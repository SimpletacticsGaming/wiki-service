package de.simpletactics.wiki.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Inbound authentication for calls from sita-backend.
 *
 * [authToken] is the shared secret sita-backend has to send in the header named
 * [authTokenHeaderName]. When [enabled] is false the filter is not registered at
 * all - only acceptable for local development.
 *
 * [enabled] and [authTokenHeaderName] carry no default on purpose. They are set
 * in `application.properties`, so a renamed or unresolvable property fails the
 * context start instead of silently binding a fallback - which for [enabled]
 * would decide whether the service is authenticated at all.
 */
@ConfigurationProperties("sita.backend.http")
data class SitaBackendHttpProperties(
    val enabled: Boolean,
    val authTokenHeaderName: String,
    val authToken: String? = null,
)
