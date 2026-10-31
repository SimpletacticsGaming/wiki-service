package de.simpletactics.wiki.adapter

import de.simpletactics.wiki.adapter.dto.EntryEntity
import de.simpletactics.wiki.adapter.dto.EntryInsertEntity
import de.simpletactics.wiki.adapter.dto.TopicEntity
import de.simpletactics.wiki.adapter.dto.TopicInsertEntity
import de.simpletactics.wiki.adapter.dto.WikiNodeEntity
import de.simpletactics.wiki.adapter.persistence.PollSqlAdapter
import de.simpletactics.wiki.adapter.persistence.WikiSqlAdapter
import de.simpletactics.wiki.model.WikiConflictException
import de.simpletactics.wiki.model.WikiNotFoundException
import de.simpletactics.wiki.model.WikiType
import de.simpletactics.wiki.services.port.WikiPort
import de.simpletactics.wiki.util.verify
import org.springframework.stereotype.Component

@Component
class WikiAdapter(
    private val wikiSqlAdapter: WikiSqlAdapter,
    private val pollSqlAdapter: PollSqlAdapter,
) : WikiPort {

    override fun getTopic(id: Int): TopicEntity? = wikiSqlAdapter.getTopic(id)

    @SitaTransactional
    override fun createTopic(parentId: Int, wikiTopicEntity: TopicInsertEntity): Int {
        val parent = requireTopic(parentId) { "No topic found to create a child topic with parentId $parentId" }

        val id = wikiSqlAdapter.createTopic(wikiTopicEntity)
        wikiSqlAdapter.updateTopic(parent.copy(childIds = parent.childIds + id))

        return id
    }

    @SitaTransactional
    override fun updateTopic(id: Int, topic: String): Int {
        val topicEntity = requireTopic(id) { "No topic found to update with id $id" }
        return wikiSqlAdapter.updateTopic(topicEntity.copy(topic = topic))
    }

    @SitaTransactional
    override fun deleteTopic(id: Int) {
        val topic = requireTopic(id) { "No topic found to delete with id $id" }

        if (wikiSqlAdapter.getTopicForChild(id) == null) {
            throw WikiConflictException("Topic $id has no parent and looks like the root topic; refusing to delete it")
        }

        deleteSubtree(topic)
        detachFromParent(id)
        wikiSqlAdapter.deleteTopic(id)
    }

    override fun getEntry(id: Int): EntryEntity? = wikiSqlAdapter.getEntry(id)

    @SitaTransactional
    override fun createEntry(topicId: Int, wikiEntryEntity: EntryInsertEntity): Int {
        val parent = requireTopic(topicId) { "Try to create entry for an undefined topic with topicId $topicId" }

        val entryId = wikiSqlAdapter.createEntry(wikiEntryEntity)
        wikiSqlAdapter.updateTopic(parent.copy(childIds = parent.childIds + entryId))

        return entryId
    }

    @SitaTransactional
    override fun updateEntry(id: Int, headline: String, body: String): Int {
        val wikiType = wikiSqlAdapter.getWikiType(id)
        val entity = wikiSqlAdapter.getEntry(id)

        verify(wikiType, WikiType.ENTRY, entity) { "No entry found to update with id $id" }

        return wikiSqlAdapter.updateEntry(entity.copy(headline = headline, htmlEntry = body))
    }

    @SitaTransactional
    override fun deleteEntry(id: Int) {
        val wikiType = wikiSqlAdapter.getWikiType(id)
        val entry = wikiSqlAdapter.getEntry(id)

        verify(wikiType, WikiType.ENTRY, entry) { "No entry found to delete with id $id" }

        detachFromParent(id)
        wikiSqlAdapter.deleteEntry(id)
    }

    override fun getWikiType(id: Int): WikiType? = wikiSqlAdapter.getWikiType(id)

    override fun getTopicForChild(childId: Int): TopicEntity? = wikiSqlAdapter.getTopicForChild(childId)

    override fun getAuthor(id: Int): String =
        wikiSqlAdapter.getAuthor(id) ?: throw WikiNotFoundException("Author not found for id $id")

    override fun getDescendants(id: Int): List<WikiNodeEntity> = wikiSqlAdapter.getDescendants(id)

    /**
     * Deletes every descendant of [topic]. Polls used to be skipped here, which
     * left rows in `wiki_poll`/`wiki` that no topic referenced any more.
     */
    private fun deleteSubtree(topic: TopicEntity) {
        topic.childIds.forEach { childId ->
            when (wikiSqlAdapter.getWikiType(childId)) {
                WikiType.TOPIC -> wikiSqlAdapter.getTopic(childId)?.let {
                    deleteSubtree(it)
                    wikiSqlAdapter.deleteTopic(childId)
                }

                WikiType.ENTRY -> wikiSqlAdapter.deleteEntry(childId)
                WikiType.POLL -> pollSqlAdapter.deletePoll(childId)
                null -> Unit
            }
        }
    }

    private fun detachFromParent(childId: Int) {
        val parent = wikiSqlAdapter.getTopicForChild(childId) ?: return
        wikiSqlAdapter.updateTopic(parent.copy(childIds = parent.childIds - childId))
    }

    private inline fun requireTopic(id: Int, errorMessage: () -> String): TopicEntity {
        val wikiType = wikiSqlAdapter.getWikiType(id)
        val topic = wikiSqlAdapter.getTopic(id)
        verify(wikiType, WikiType.TOPIC, topic, errorMessage)
        return topic
    }
}
