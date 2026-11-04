package de.simpletactics.wiki.services

import io.kotest.matchers.collections.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.scheduling.config.ScheduledTaskHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource

/**
 * The cron has no inline default, so an unresolvable
 * `sita.wiki.poll.auto-close.cron` fails the context start instead of installing
 * a schedule nobody configured. Starting the context with auto-close on is the
 * assertion; the task lookup only pins down that the schedule is registered.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("testing")
@TestPropertySource(properties = ["sita.wiki.poll.auto-close.enabled=true"])
class PollExpirySchedulerIT(
    @Autowired val scheduler: PollExpiryScheduler,
    @Autowired val taskHolders: List<ScheduledTaskHolder>,
) {

    @Test
    fun `auto-close is scheduled from the configured cron`() {
        taskHolders.flatMap { it.scheduledTasks }.shouldNotBeEmpty()
    }
}
