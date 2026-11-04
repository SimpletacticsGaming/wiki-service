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

        return "${environmentLabel()}:\n$alertMessage"
    }

    /**
     * The label names the profile the service actually runs under. It used to
     * fall back to PROD, which meant any run without a profile - a local start
     * that forgot `-Dspring.profiles.active=local` above all - announced itself
     * as production. An unlabelled environment is [UNKNOWN_ENVIRONMENT] instead:
     * for PROD to appear, the deployment has to say so.
     */
    private fun environmentLabel(): String {
        val activeProfiles = environment.activeProfiles
        return ENVIRONMENT_PROFILES.firstOrNull { it in activeProfiles }?.uppercase()
            ?: UNKNOWN_ENVIRONMENT
    }

    companion object {
        private val log = LoggerFactory.getLogger(AlertService::class.java)

        /** In precedence order, so `local,secrets` reads as LOCAL rather than both. */
        private val ENVIRONMENT_PROFILES = listOf("local", "testing", "dev", "prod")
        private const val UNKNOWN_ENVIRONMENT = "UNKNOWN"
    }
}
