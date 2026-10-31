package de.simpletactics.wiki.adapter.persistence.poll

import de.simpletactics.wiki.adapter.FunSpecIT
import de.simpletactics.wiki.services.port.PollPort
import io.kotest.matchers.shouldBe
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate

class ReopenPollAdapterIT(
    val pollPort: PollPort,
    jdbcTemplate: JdbcTemplate,
) : FunSpecIT(jdbcTemplate, {

    context("Reopens poll") {

        beforeTest {
            executeSql("wiki_poll.sql")
        }

        test("reopen closed poll with a new date") {
            pollPort.reopenPoll(30, LocalDate.of(2099, 8, 9))

            with(pollPort.getPoll(30)) {
                this?.endDate shouldBe LocalDate.of(2099, 8, 9)
                this?.ended shouldBe false
            }
        }

        test("reopen without a date keeps the stored one instead of clearing it") {
            pollPort.reopenPoll(30, null)

            with(pollPort.getPoll(30)) {
                this?.endDate shouldBe LocalDate.of(2023, 10, 30)
                this?.ended shouldBe false
            }
        }
    }
})
