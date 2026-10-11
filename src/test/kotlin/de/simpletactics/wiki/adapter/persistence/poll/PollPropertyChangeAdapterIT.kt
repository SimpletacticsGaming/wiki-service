package de.simpletactics.wiki.adapter.persistence.poll

import de.simpletactics.wiki.services.port.PollPort
import de.simpletactics.wiki.adapter.FunSpecIT
import io.kotest.matchers.shouldBe
import org.springframework.jdbc.core.JdbcTemplate

class PollPropertyChangeAdapterIT(
    private val pollPort: PollPort,
    jdbcTemplate: JdbcTemplate,
) : FunSpecIT(jdbcTemplate, {

    context("poll property change") {

        beforeTest {
            executeSql("wiki_poll.sql")
        }

        test("a poll past its end date counts as closed even before the sweep runs") {
            // 3: no end date, open    4: end date 2099, open
            pollPort.isPollOpen(3) shouldBe true
            pollPort.isPollOpen(4) shouldBe true
            // 6: flagged ended        2 and 5: end date 2023, not yet flagged
            pollPort.isPollOpen(6) shouldBe false
            pollPort.isPollOpen(2) shouldBe false
            pollPort.isPollOpen(5) shouldBe false
        }

        test("close all open expired polls flags them in the database") {
            val closed = pollPort.closeExpiredOpenPolls()

            closed shouldBe 2
            pollPort.getPoll(2)?.ended shouldBe true
            pollPort.getPoll(5)?.ended shouldBe true
            pollPort.getPoll(3)?.ended shouldBe false
            pollPort.getPoll(4)?.ended shouldBe false
        }
    }
})
