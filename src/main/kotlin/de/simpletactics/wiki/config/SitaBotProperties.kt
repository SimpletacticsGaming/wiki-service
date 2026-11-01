package de.simpletactics.wiki.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Outbound alerting to the sita bot. Nothing is defaulted: a typo in a property
 * name or a missing deployment value has to fail the context start rather than
 * quietly fall back to "alerting on" against an unreachable url.
 */
@ConfigurationProperties("sita.bot.alerting")
data class SitaBotProperties(
    val enabled: Boolean,
    val url: String?,
    val channelId: Long?,
)
