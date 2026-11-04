package de.simpletactics.wiki.adapter

import de.simpletactics.wiki.adapter.dto.poll.PollEntity
import de.simpletactics.wiki.adapter.dto.poll.PollInsertEntity
import de.simpletactics.wiki.adapter.dto.poll.PollUpdateEntity
import de.simpletactics.wiki.adapter.dto.poll.VoteSubmission
import de.simpletactics.wiki.adapter.persistence.PollSqlAdapter
import de.simpletactics.wiki.adapter.persistence.WikiSqlAdapter
import de.simpletactics.wiki.model.WikiType
import de.simpletactics.wiki.services.port.PollPort
import de.simpletactics.wiki.util.verify
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class WikiPollAdapter(
    private val wikiSqlAdapter: WikiSqlAdapter,
    private val pollSqlAdapter: PollSqlAdapter,
) : PollPort {

    @SitaTransactional
    override fun savePoll(topicId: Int, poll: PollInsertEntity): Int {
        val wikiType = wikiSqlAdapter.getWikiType(topicId)
        val topic = wikiSqlAdapter.getTopic(topicId)

        verify(wikiType, WikiType.TOPIC, topic) { "No topic found with id $topicId" }

        val id = pollSqlAdapter.savePoll(poll)
        wikiSqlAdapter.updateTopic(topic.copy(childIds = topic.childIds + id))
        return id
    }

    /** Only returns a poll when the id really is of type POLL. */
    override fun getPoll(id: Int): PollEntity? =
        if (wikiSqlAdapter.getWikiType(id) == WikiType.POLL) pollSqlAdapter.getPoll(id) else null

    @SitaTransactional
    override fun updatePoll(poll: PollUpdateEntity): PollEntity = pollSqlAdapter.updatePoll(poll)

    @SitaTransactional
    override fun setPollVotes(pollId: Int, userId: Int, votes: List<VoteSubmission>): PollEntity =
        pollSqlAdapter.setPollVotes(pollId, userId, votes)

    @SitaTransactional
    override fun deletePoll(id: Int): Int {
        val wikiType = wikiSqlAdapter.getWikiType(id)
        val poll = pollSqlAdapter.getPoll(id)

        verify(wikiType, WikiType.POLL, poll) { "No poll found to delete with id $id" }

        wikiSqlAdapter.getTopicForChild(id)?.let { parent ->
            wikiSqlAdapter.updateTopic(parent.copy(childIds = parent.childIds - id))
        }

        return pollSqlAdapter.deletePoll(id)
    }

    @SitaTransactional
    override fun endPoll(id: Int) = pollSqlAdapter.endPoll(id)

    @SitaTransactional
    override fun reopenPoll(id: Int, endDate: LocalDate?) = pollSqlAdapter.reopenPoll(id, endDate)

    override fun isPollOpen(id: Int): Boolean = pollSqlAdapter.isPollOpen(id)

    @SitaTransactional
    override fun closeExpiredOpenPolls(): Int = pollSqlAdapter.closeExpiredOpenPolls()
}
