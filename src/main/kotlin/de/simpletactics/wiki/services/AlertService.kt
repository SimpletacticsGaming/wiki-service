package de.simpletactics.wiki.services

import de.simpletactics.wiki.config.SitaBotProperties
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class AlertService(
    private val webClient: WebClient,
    private val environment: Environment,
    private val sitaBotProperties: SitaBotProperties,
) {

    /**
     * Fire and forget. Alerting is a side channel; blocking a request thread on
     * it - as this used to with `.block()` - makes a bot outage look like a wiki
     * outage.
     */
    fun alert(errorMessage: String) {
        if (!sitaBotProperties.enabled) return

        webClient.post()
            .body(BodyInserters.fromValue(createAlertMessage(errorMessage)))
            .retrieve()
            .bodyToMono(String::class.java)
            .subscribe({ }, { error -> log.error("Alerting failed", error) })
    }

    private fun createAlertMessage(errorMessage: String): String {
        val alertMessage = "Alert am ${
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        }\n$errorMessage"

        val profiles = environment.activeProfiles
        return when {
            profiles.contains("local") -> "LOCAL:\n$alertMessage"
            profiles.contains("dev") -> "DEV:\n$alertMessage"
            else -> "PROD:\n$alertMessage"
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(AlertService::class.java)
    }
}
