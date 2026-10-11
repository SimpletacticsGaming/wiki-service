package de.simpletactics.wiki.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule

/**
 * Used for the `wiki_poll.data` JSONB column. Dates are written as ISO-8601
 * strings so the stored JSON stays readable and parseable outside the JVM.
 */
val jsonObjectMapper: ObjectMapper = ObjectMapper()
    .registerModule(kotlinModule())
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
