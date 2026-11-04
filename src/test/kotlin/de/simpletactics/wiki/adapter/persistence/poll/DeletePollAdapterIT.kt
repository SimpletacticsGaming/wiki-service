package de.simpletactics.wiki.adapter.persistence.poll

import de.simpletactics.wiki.adapter.FunSpecIT
import de.simpletactics.wiki.services.port.PollPort
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.jdbc.core.JdbcTemplate

class DeletePollAdapterIT(
    private val pollPort: PollPort,
    jdbcTemplate: JdbcTemplate,
) : FunSpecIT(jdbcTemplate, {

    context("poll") {

        beforeTest {
            executeSql("wiki_poll.sql")
        }

        test("delete poll") {
            val pollIdToDelete = 6
            val fetchedPoll = pollPort.getPoll(pollIdToDelete)
            pollPort.deletePoll(pollIdToDelete)

            fetchedPoll shouldNotBe null
            pollPort.getPoll(pollIdToDelete) shouldBe null
        }

        test("deleting a poll detaches it from its parent topic") {
            pollPort.deletePoll(6)

            jdbcTemplate.queryForObject(
                "SELECT array_to_string(child_id, ',') FROM wiki_topic WHERE id = 1",
                String::class.java,
            ) shouldBe "2"
        }
    }
})
