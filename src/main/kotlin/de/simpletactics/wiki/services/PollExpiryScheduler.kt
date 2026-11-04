package de.simpletactics.wiki.services

import de.simpletactics.wiki.services.port.PollPort
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * `wiki_poll.ended` is the flag the rest of the system reads, so polls whose
 * deadline has passed have to be flipped eventually. Voting is already refused
 * on an expired poll, this only keeps the stored state honest.
 */
@Component
@ConditionalOnProperty(
    prefix = "sita.wiki.poll.auto-close",
    name = ["enabled"],
    havingValue = "true",
)
class PollExpiryScheduler(
    private val pollPort: PollPort,
) {

    /**
     * No inline default for the cron: both `enabled` and `cron` live in
     * `application.properties`, so a missing or misspelled property should make
     * the context fail rather than silently install a schedule nobody chose.
     */
    @Scheduled(cron = "\${sita.wiki.poll.auto-close.cron}")
    fun closeExpiredPolls() {
        val closed = pollPort.closeExpiredOpenPolls()
        if (closed > 0) log.info("Closed $closed expired poll(s)")
    }

    companion object {
        private val log = LoggerFactory.getLogger(PollExpiryScheduler::class.java)
    }
}
