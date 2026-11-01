package de.simpletactics.wiki.adapter.dto.poll

import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * `end_date` is a DATE column, so it is a [LocalDate]: a deadline is a calendar
 * day, not an instant. `created_at`/`updated_at` are TIMESTAMPTZ and keep their
 * offset all the way to the API.
 */
data class PollEntity(
    val id: Int,
    val question: String,
    val pollEntries: List<PollEntryEntity>,
    val author: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val description: String = "",
    val ended: Boolean = false,
    val endDate: LocalDate? = null,
)

data class PollInsertEntity(
    val question: String,
    val pollEntries: List<PollEntryEntity>,
    val author: String,
    val description: String = "",
    val endDate: LocalDate? = null,
)

/** The fields [PollSqlAdapter.updatePoll] is allowed to change. */
data class PollUpdateEntity(
    val id: Int,
    val question: String,
    val description: String,
    val endDate: LocalDate?,
    val pollEntries: List<PollEntryEntity>,
)
