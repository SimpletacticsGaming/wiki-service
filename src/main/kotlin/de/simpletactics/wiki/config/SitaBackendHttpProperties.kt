package de.simpletactics.wiki.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Inbound authentication for calls from sita-backend.
 *
 * [authToken] is the shared secret sita-backend has to send in the header named
 * [authTokenHeaderName]. When [enabled] is false the filter is not registered at
 * all - only acceptable for local development.
 */
@ConfigurationProperties("sita.backend.http")
data class SitaBackendHttpProperties(
    val enabled: Boolean = true,
    val authTokenHeaderName: String = "authentication",
    val authToken: String? = null,
)
