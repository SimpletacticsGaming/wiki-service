package de.simpletactics.wiki.adapter.controller

import de.simpletactics.wiki.model.WikiAccessDeniedException
import de.simpletactics.wiki.model.WikiConflictException
import de.simpletactics.wiki.model.WikiNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.util.UUID

/**
 * Single place that turns exceptions into status codes, so sita-backend gets a
 * predictable RFC 7807 body instead of a stack trace and a blanket 500.
 *
 * Extending [ResponseEntityExceptionHandler] matters: it keeps Spring's own MVC
 * exceptions (unknown route, wrong method, unreadable body) mapped to their
 * proper statuses. Without it the catch-all below would report them all as 500.
 */
@RestControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(WikiNotFoundException::class)
    fun handleNotFound(exception: WikiNotFoundException): ProblemDetail =
        problem(HttpStatus.NOT_FOUND, "Wiki resource not found", exception.message)

    @ExceptionHandler(WikiAccessDeniedException::class)
    fun handleAccessDenied(exception: WikiAccessDeniedException): ProblemDetail =
        problem(HttpStatus.FORBIDDEN, "Access denied", exception.message)

    @ExceptionHandler(WikiConflictException::class)
    fun handleConflict(exception: WikiConflictException): ProblemDetail =
        problem(HttpStatus.CONFLICT, "Conflicting wiki state", exception.message)

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(exception: IllegalArgumentException): ProblemDetail =
        problem(HttpStatus.BAD_REQUEST, "Invalid request", exception.message)

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(exception: Exception): ProblemDetail {
        val errorId = UUID.randomUUID().toString()
        log.error("Unhandled exception [errorId=$errorId]", exception)
        return problem(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal server error",
            "Unexpected error. Reference this id when reporting it: $errorId",
        )
    }

    /** Spring's default body says nothing about which field failed; ours does. */
    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> {
        val details = ex.bindingResult.fieldErrors
            .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
            .ifBlank { "Request body is invalid" }

        return ResponseEntity.badRequest()
            .body(problem(HttpStatus.BAD_REQUEST, "Invalid request", details))
    }

    private fun problem(status: HttpStatus, title: String, detail: String?): ProblemDetail =
        ProblemDetail.forStatusAndDetail(status, detail ?: title).apply {
            this.title = title
        }

    companion object {
        private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }
}
