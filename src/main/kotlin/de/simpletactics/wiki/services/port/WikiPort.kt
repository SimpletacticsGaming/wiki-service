package de.simpletactics.wiki.services.port

import de.simpletactics.wiki.adapter.dto.EntryEntity
import de.simpletactics.wiki.adapter.dto.EntryInsertEntity
import de.simpletactics.wiki.adapter.dto.TopicEntity
import de.simpletactics.wiki.adapter.dto.TopicInsertEntity
import de.simpletactics.wiki.adapter.dto.WikiNodeEntity
import de.simpletactics.wiki.model.WikiType

interface WikiPort {

    fun getTopic(id: Int): TopicEntity?

    fun createTopic(parentId: Int, wikiTopicEntity: TopicInsertEntity): Int

    fun updateTopic(id: Int, topic: String): Int

    fun deleteTopic(id: Int)

    fun getEntry(id: Int): EntryEntity?

    fun createEntry(topicId: Int, wikiEntryEntity: EntryInsertEntity): Int

    fun updateEntry(id: Int, headline: String, body: String): Int

    fun deleteEntry(id: Int)

    fun getWikiType(id: Int): WikiType?

    fun getTopicForChild(childId: Int): TopicEntity?

    fun getAuthor(id: Int): String

    /** [id] and everything beneath it, in one query. Empty when [id] does not exist. */
    fun getDescendants(id: Int): List<WikiNodeEntity>
}
