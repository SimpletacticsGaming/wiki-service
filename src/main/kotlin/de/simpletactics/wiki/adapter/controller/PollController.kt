package de.simpletactics.wiki.adapter.controller

import de.simpletactics.wiki.adapter.dto.poll.toResponse
import de.simpletactics.wiki.adapter.dto.poll.toSubmissions
import de.simpletactics.wiki.adapter.dto.poll.toUpdateEntity
import de.simpletactics.wiki.adapter.dto.poll.toVotesResponse
import de.simpletactics.wiki.api.WikiPollsApi
import de.simpletactics.wiki.api.dto.DeletedResponse
import de.simpletactics.wiki.api.dto.PollOptionVotesResponse
import de.simpletactics.wiki.api.dto.PollResponse
import de.simpletactics.wiki.api.dto.SubmitVotesRequest
import de.simpletactics.wiki.api.dto.UpdatePollRequest
import de.simpletactics.wiki.model.WikiConflictException
import de.simpletactics.wiki.model.WikiNotFoundException
import de.simpletactics.wiki.services.WikiService
import de.simpletactics.wiki.services.port.PollPort
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

/**
 * Polls are created under their topic (`POST /api/wiki/topics/{id}/polls`); every
 * other operation addresses the poll directly.
 *
 * `userId` identifies the calling end user so the response can carry that user's
 * own vote. It is a plain request parameter because sita-backend owns user
 * identity - this service only stores the id.
 */
@Tag(name = "Wiki polls")
@RestController
class PollController(
    private val pollPort: PollPort,
    private val wikiService: WikiService,
) : WikiPollsApi {

    /** Works for open and closed polls alike - results stay readable after the poll ends. */
    override fun getPoll(id: Int, userId: Int): ResponseEntity<PollResponse> =
        ResponseEntity.ok(requirePoll(id).toResponse(userId))

    /**
     * Every vote on the poll, voter ids included - unlike [getPoll], which only
     * aggregates. Restricted information: the caller is responsible for checking
     * that the end user may see who voted what (sita-backend requires
     * WIKI_EVALUATE). Names are the caller's to resolve; this service has none.
     */
    override fun getPollVotes(id: Int): ResponseEntity<List<PollOptionVotesResponse>> =
        ResponseEntity.ok(requirePoll(id).toVotesResponse())

    override fun updatePoll(
        id: Int,
        userId: Int,
        updatePollRequest: UpdatePollRequest,
    ): ResponseEntity<PollResponse> {
        requireOpen(id)
        return ResponseEntity.ok(
            pollPort.updatePoll(updatePollRequest.toUpdateEntity(id)).toResponse(userId)
        )
    }

    override fun submitVotes(
        id: Int,
        userId: Int,
        submitVotesRequest: SubmitVotesRequest,
    ): ResponseEntity<PollResponse> {
        requireOpen(id)
        return ResponseEntity.ok(
            pollPort.setPollVotes(id, userId, submitVotesRequest.toSubmissions()).toResponse(userId)
        )
    }

    override fun closePoll(id: Int): ResponseEntity<Unit> {
        requirePoll(id)
        pollPort.endPoll(id)
        return ResponseEntity.noContent().build()
    }

    /**
     * Reopens a closed poll. Without a new [endDate] the stored deadline is kept
     * - which means a poll that closed because its deadline passed can only be
     * reopened by supplying a new one.
     */
    override fun reopenPoll(id: Int, endDate: LocalDate?): ResponseEntity<Unit> {
        val poll = requirePoll(id)
        if (pollPort.isPollOpen(id)) throw WikiConflictException("Poll $id is not closed")

        val today = LocalDate.now()
        require(endDate == null || !endDate.isBefore(today)) { "End date $endDate is in the past" }

        if (endDate == null && poll.endDate?.isBefore(today) == true) {
            throw WikiConflictException(
                "Poll $id ended on ${poll.endDate}; supply a new endDate to reopen it"
            )
        }

        pollPort.reopenPoll(id, endDate)
        return ResponseEntity.noContent().build()
    }

    override fun deletePoll(id: Int): ResponseEntity<DeletedResponse> =
        ResponseEntity.ok(DeletedResponse(id, wikiService.deletePoll(id)))

    private fun requirePoll(id: Int) =
        pollPort.getPoll(id) ?: throw WikiNotFoundException("No poll found with id $id")

    private fun requireOpen(id: Int) {
        requirePoll(id)
        if (!pollPort.isPollOpen(id)) throw WikiConflictException("Poll $id is closed")
    }
}
