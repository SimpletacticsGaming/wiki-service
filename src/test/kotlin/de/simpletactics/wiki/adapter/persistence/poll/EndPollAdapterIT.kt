package de.simpletactics.wiki.adapter.persistence.poll

import de.simpletactics.wiki.adapter.FunSpecIT
import de.simpletactics.wiki.services.port.PollPort
import io.kotest.matchers.shouldBe
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate

class EndPollAdapterIT(
    private val pollPort: PollPort,
    jdbcTemplate: JdbcTemplate,
) : FunSpecIT(jdbcTemplate, {

    context("poll") {

        beforeTest {
            executeSql("wiki_poll.sql")
        }

        test("closing a poll keeps its original end date") {
            val pollId = 5
            val before = pollPort.getPoll(pollId)
            requireNotNull(before)
            before.endDate shouldBe LocalDate.of(2023, 10, 21)

            pollPort.endPoll(pollId)

            val endedPoll = pollPort.getPoll(pollId)
            requireNotNull(endedPoll)
            with(endedPoll) {
                ended shouldBe true
                endDate shouldBe LocalDate.of(2023, 10, 21)
            }
        }

        test("closing a poll without an end date leaves it null") {
            pollPort.endPoll(3)

            val endedPoll = pollPort.getPoll(3)
            requireNotNull(endedPoll)
            endedPoll.ended shouldBe true
            endedPoll.endDate shouldBe null
        }
    }
})
