package de.simpletactics.wiki.adapter.dto.poll

import de.simpletactics.wiki.api.dto.CreatePollRequest
import de.simpletactics.wiki.api.dto.PollOptionRequest
import de.simpletactics.wiki.api.dto.SubmitVotesRequest
import de.simpletactics.wiki.api.dto.UpdatePollRequest
import de.simpletactics.wiki.api.dto.VoteRequest
import de.simpletactics.wiki.api.dto.PollVoteEnum as WirePollVoteEnum
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import java.time.OffsetDateTime
import java.time.ZoneOffset

private val FIXED_TIME = OffsetDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC)

private fun pollWith(vararg entries: PollEntryEntity) = PollEntity(
    id = 1,
    question = "q",
    pollEntries = entries.toList(),
    description = "d",
    ended = false,
    endDate = null,
    author = "a",
    createdAt = FIXED_TIME,
    updatedAt = FIXED_TIME,
)

class PollMappersTest : FunSpec({

    test("response carries vote counts but never other users identities") {
        val poll = pollWith(
            PollEntryEntity(
                PollOption("uuid-1", "Option 1"),
                mutableListOf(
                    Vote(1, FIXED_TIME, PollVoteEnum.TRUE),
                    Vote(2, FIXED_TIME, PollVoteEnum.TRUE),
                    Vote(3, FIXED_TIME, PollVoteEnum.FALSE),
                ),
            )
        )

        val response = poll.toResponse(userId = 2)

        response.options shouldHaveSize 1
        with(response.options.first()) {
            uuid shouldBe "uuid-1"
            text shouldBe "Option 1"
            yesVotes shouldBe 2
            noVotes shouldBe 1
            ownVote shouldBe WirePollVoteEnum.TRUE
        }
        // the projection exposes no userId at all
        response.toString().contains("userId") shouldBe false
    }

    test("a user who has not voted gets FALSE for that option") {
        val poll = pollWith(
            PollEntryEntity(PollOption("uuid-1", "Option 1"), mutableListOf(Vote(1, FIXED_TIME, PollVoteEnum.TRUE)))
        )

        poll.toResponse(userId = 99).options.first().ownVote shouldBe WirePollVoteEnum.FALSE
    }

    test("create request generates a uuid for options that arrive without one") {
        val request = CreatePollRequest(
            question = "q",
            options = listOf(PollOptionRequest(text = "no uuid"), PollOptionRequest(uuid = "given", text = "has uuid")),
            author = "a",
        )

        val entity = request.toInsertEntity()

        entity.pollEntries shouldHaveSize 2
        entity.pollEntries[0].pollOption.uuid.shouldNotBeBlank()
        entity.pollEntries[1].pollOption.uuid shouldBe "given"
    }

    test("duplicate option uuids are rejected") {
        val request = UpdatePollRequest(
            question = "q",
            options = listOf(
                PollOptionRequest(uuid = "same", text = "a"),
                PollOptionRequest(uuid = "same", text = "b"),
            ),
        )

        shouldThrow<IllegalArgumentException> { request.toUpdateEntity(1) }
    }

    test("duplicate votes for one option are rejected") {
        val request = SubmitVotesRequest(
            listOf(
                VoteRequest("uuid-1", WirePollVoteEnum.TRUE),
                VoteRequest("uuid-1", WirePollVoteEnum.FALSE),
            )
        )

        shouldThrow<IllegalArgumentException> { request.toSubmissions() }
    }

    test("submitted options never carry votes into the update entity") {
        val entity = UpdatePollRequest(
            question = "q",
            options = listOf(PollOptionRequest(uuid = "uuid-1", text = "Option 1")),
        ).toUpdateEntity(1)

        entity.pollEntries.first().votes shouldHaveSize 0
    }
})
