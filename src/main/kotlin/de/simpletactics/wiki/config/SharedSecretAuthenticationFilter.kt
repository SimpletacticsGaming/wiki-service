package de.simpletactics.wiki.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import java.security.MessageDigest

/**
 * Authenticates service-to-service calls by comparing a shared secret sent in a
 * request header. The comparison is constant time so a wrong token leaks no
 * information about how much of it was correct.
 */
class SharedSecretAuthenticationFilter(
    private val headerName: String,
    private val expectedToken: String,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val presented = request.getHeader(headerName)

        if (presented != null && matches(presented)) {
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken(
                    SITA_BACKEND_PRINCIPAL,
                    null,
                    listOf(SimpleGrantedAuthority("ROLE_SERVICE")),
                )
        }

        filterChain.doFilter(request, response)
    }

    private fun matches(presented: String): Boolean = MessageDigest.isEqual(
        presented.toByteArray(Charsets.UTF_8),
        expectedToken.toByteArray(Charsets.UTF_8),
    )

    companion object {
        const val SITA_BACKEND_PRINCIPAL = "sita-backend"
    }
}
