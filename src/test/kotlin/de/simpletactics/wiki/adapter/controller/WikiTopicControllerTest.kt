package de.simpletactics.wiki.adapter.controller

import com.ninjasquad.springmockk.MockkBean
import de.simpletactics.wiki.adapter.dto.TopicEntity
import de.simpletactics.wiki.adapter.dto.TopicInsertEntity
import de.simpletactics.wiki.model.WikiNotFoundException
import de.simpletactics.wiki.services.WikiService
import de.simpletactics.wiki.services.port.PollPort
import de.simpletactics.wiki.services.port.WikiPort
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
import java.time.OffsetDateTime
import java.time.ZoneOffset

private val TIME = OffsetDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC)

@WebMvcTest(WikiTopicController::class)
@AutoConfigureMockMvc(addFilters = false)
class WikiTopicControllerTest(@Autowired val mockMvc: MockMvc) {

    @MockkBean
    lateinit var wikiPort: WikiPort

    @MockkBean
    lateinit var pollPort: PollPort

    @MockkBean
    lateinit var wikiService: WikiService

    @Test
    fun `get topic serializes timestamps with their offset`() {
        every { wikiPort.getTopic(1) } returns TopicEntity(1, "Root", listOf(2), "author", TIME, TIME)

        mockMvc.perform(get("/api/wiki/topics/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.topic").value("Root"))
            .andExpect(jsonPath("$.author").value("author"))
            .andExpect(jsonPath("$.createdAt").value("2024-01-01T12:00:00Z"))
    }

    @Test
    fun `unknown topic is a 404`() {
        every { wikiPort.getTopic(99) } returns null

        mockMvc.perform(get("/api/wiki/topics/99"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.detail").value("No topic found with id 99"))
    }

    @Test
    fun `create child topic answers 201 with the new id`() {
        every { wikiPort.createTopic(1, TopicInsertEntity("Child", "author")) } returns 7

        mockMvc.perform(
            post("/api/wiki/topics/1/topics")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"topic":"Child","author":"author"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(7))
    }

    @Test
    fun `a blank topic is rejected with 400 before it reaches the port`() {
        mockMvc.perform(
            post("/api/wiki/topics/1/topics")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"topic":"   ","author":"author"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.title").value("Invalid request"))
    }

    /**
     * Regression: a catch-all @ExceptionHandler(Exception) swallows Spring's own
     * MVC exceptions and reports an unknown URL or a wrong verb as 500.
     */
    @Test
    fun `an unknown route is a 404 and a wrong method a 405, not 500`() {
        mockMvc.perform(get("/api/wiki/definitely-not-a-route"))
            .andExpect(status().isNotFound)

        mockMvc.perform(post("/api/wiki/topics/1"))
            .andExpect(status().isMethodNotAllowed)
    }

    @Test
    fun `a failing delete surfaces as 404 rather than 500`() {
        every { wikiService.deleteWikiTopic(5) } throws WikiNotFoundException("No topic found to delete with id 5")

        mockMvc.perform(delete("/api/wiki/topics/5"))
            .andExpect(status().isNotFound)
    }
}
