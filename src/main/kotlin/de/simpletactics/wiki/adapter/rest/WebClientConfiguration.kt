package de.simpletactics.wiki.adapter.rest

import de.simpletactics.wiki.config.SitaBotProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfiguration(
    private val sitaBotProperties: SitaBotProperties,
) {

    /**
     * When alerting is disabled the client still has to exist for injection, but
     * it points nowhere - [de.simpletactics.wiki.services.AlertService] never
     * calls it in that case.
     */
    @Bean
    fun sitaBotWebClient(): WebClient {
        if (!sitaBotProperties.enabled) return WebClient.create("http://alerting.disabled.invalid")

        val url = checkNotNull(sitaBotProperties.url?.takeIf { it.isNotBlank() }) {
            "No url set. Provide sita.bot.alerting.url or disable alerting"
        }
        val channelId = checkNotNull(sitaBotProperties.channelId) {
            "No channelId set. Provide sita.bot.alerting.channel-id or disable alerting"
        }
        return WebClient.create("${url.trimEnd('/')}/$channelId")
    }
}
