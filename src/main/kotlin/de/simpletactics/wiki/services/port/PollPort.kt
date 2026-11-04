package de.simpletactics.wiki.services.port

import de.simpletactics.wiki.adapter.dto.poll.PollEntity
import de.simpletactics.wiki.adapter.dto.poll.PollInsertEntity
import de.simpletactics.wiki.adapter.dto.poll.PollUpdateEntity
import de.simpletactics.wiki.adapter.dto.poll.VoteSubmission
import java.time.LocalDate

interface PollPort {

    fun savePoll(topicId: Int, poll: PollInsertEntity): Int

    fun getPoll(id: Int): PollEntity?

    fun updatePoll(poll: PollUpdateEntity): PollEntity

    fun setPollVotes(pollId: Int, userId: Int, votes: List<VoteSubmission>): PollEntity

    fun deletePoll(id: Int): Int

    fun endPoll(id: Int)

    fun reopenPoll(id: Int, endDate: LocalDate?)

    fun isPollOpen(id: Int): Boolean

    fun closeExpiredOpenPolls(): Int
}
