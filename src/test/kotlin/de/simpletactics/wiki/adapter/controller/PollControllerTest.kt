package de.simpletactics.wiki.adapter.controller

import com.ninjasquad.springmockk.MockkBean
import de.simpletactics.wiki.adapter.dto.poll.PollEntity
import de.simpletactics.wiki.adapter.dto.poll.PollEntryEntity
import de.simpletactics.wiki.adapter.dto.poll.PollOption
import de.simpletactics.wiki.adapter.dto.poll.PollVoteEnum
import de.simpletactics.wiki.adapter.dto.poll.Vote
import de.simpletactics.wiki.services.WikiService
import de.simpletactics.wiki.services.port.PollPort
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

private val TIME = OffsetDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC)

/** Security is exercised separately; this slice is about status codes and payload shape. */
@WebMvcTest(PollController::class)
@AutoConfigureMockMvc(addFilters = false)
class PollControllerTest(@Autowired val mockMvc: MockMvc) {

    @MockkBean
    lateinit var pollPort: PollPort

    @MockkBean
    lateinit var wikiService: WikiService

    private fun poll(ended: Boolean = false) = PollEntity(
        id = 1,
        question = "Question",
        pollEntries = listOf(
            PollEntryEntity(
                PollOption("uuid-1", "Option 1"),
                mutableListOf(
                    Vote(7, TIME, PollVoteEnum.TRUE),
                    Vote(8, TIME, PollVoteEnum.FALSE),
                ),
            )
        ),
        description = "Description",
        ended = ended,
        endDate = LocalDate.of(2099, 1, 1),
        author = "author",
        createdAt = TIME,
        updatedAt = TIME,
    )

    @Test
    fun `get poll returns counts and the callers own vote, never other user ids`() {
        every { pollPort.getPoll(1) } returns poll()

        mockMvc.perform(get("/api/wiki/polls/1").param("userId", "7"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.question").value("Question"))
            .andExpect(jsonPath("$.endDate").value("2099-01-01"))
            .andExpect(jsonPath("$.options[0].yesVotes").value(1))
            .andExpect(jsonPath("$.options[0].noVotes").value(1))
            .andExpect(jsonPath("$.options[0].ownVote").value("TRUE"))
            .andExpect(jsonPath("$.options[0].votes").doesNotExist())
    }

    @Test
    fun `a closed poll is still readable`() {
        every { pollPort.getPoll(1) } returns poll(ended = true)

        mockMvc.perform(get("/api/wiki/polls/1").param("userId", "7"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.ended").value(true))
    }

    @Test
    fun `an unknown poll is a 404 with a problem body, not a 500`() {
        every { pollPort.getPoll(999) } returns null

        mockMvc.perform(get("/api/wiki/polls/999").param("userId", "7"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.title").value("Wiki resource not found"))
            .andExpect(jsonPath("$.detail").value("No poll found with id 999"))
    }

    @Test
    fun `voting on a closed poll is a 409`() {
        every { pollPort.getPoll(1) } returns poll(ended = true)
        every { pollPort.isPollOpen(1) } returns false

        mockMvc.perform(
            post("/api/wiki/polls/1/votes")
                .param("userId", "7")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"votes":[{"optionUuid":"uuid-1","option":"TRUE"}]}""")
        ).andExpect(status().isConflict)
    }

    @Test
    fun `an empty vote list is rejected as a 400`() {
        every { pollPort.getPoll(1) } returns poll()
        every { pollPort.isPollOpen(1) } returns true

        mockMvc.perform(
            post("/api/wiki/polls/1/votes")
                .param("userId", "7")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"votes":[]}""")
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `delete returns the parent id to navigate to`() {
        every { wikiService.deletePoll(1) } returns 42

        mockMvc.perform(delete("/api/wiki/polls/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.parentId").value(42))
    }

    @Test
    fun `reopening an open poll is a 409`() {
        every { pollPort.getPoll(1) } returns poll()
        every { pollPort.isPollOpen(1) } returns true

        mockMvc.perform(delete("/api/wiki/polls/1/closure"))
            .andExpect(status().isConflict)
    }
}
