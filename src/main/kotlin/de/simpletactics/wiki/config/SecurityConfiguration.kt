package de.simpletactics.wiki.config

import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

/**
 * Without an explicit chain, Spring Boot installs its default one - every
 * endpoint behind form login with a generated password - which would reject
 * every call from sita-backend. This declares the intended behaviour instead.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(SitaBackendHttpProperties::class)
class SecurityConfiguration(
    private val properties: SitaBackendHttpProperties,
) {

    /**
     * This service has no user accounts - authentication is a shared secret
     * between services. Declaring an empty manager stops Spring Boot from
     * inventing a default user and logging a generated password on every start.
     */
    @Bean
    fun userDetailsService(): UserDetailsService = InMemoryUserDetailsManager()

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling { it.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)) }

        if (properties.enabled) {
            val token = checkNotNull(properties.authToken?.takeIf { it.isNotBlank() }) {
                "sita.backend.http.auth-token must be set when sita.backend.http.enabled is true"
            }
            http.addFilterBefore(
                SharedSecretAuthenticationFilter(properties.authTokenHeaderName, token),
                UsernamePasswordAuthenticationFilter::class.java,
            )
            http.authorizeHttpRequests {
                it.requestMatchers(*PUBLIC_PATHS).permitAll()
                    .anyRequest().authenticated()
            }
        } else {
            log.warn(
                "sita.backend.http.enabled=false - all endpoints are unauthenticated. " +
                    "This must never be the case outside local development."
            )
            http.authorizeHttpRequests { it.anyRequest().permitAll() }
        }

        return http.build()
    }

    companion object {
        private val log = LoggerFactory.getLogger(SecurityConfiguration::class.java)

        private val PUBLIC_PATHS = arrayOf(
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/prometheus",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
        )
    }
}
