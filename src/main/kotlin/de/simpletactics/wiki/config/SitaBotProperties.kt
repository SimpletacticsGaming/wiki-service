package de.simpletactics.wiki.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("sita.bot.alerting")
data class SitaBotProperties(
    val enabled: Boolean = true,
    val url: String?,
    val channelId: Long?,
)
