package de.simpletactics.wiki.adapter.dto

import java.time.OffsetDateTime

data class EntryEntity(
    val id: Int,
    val headline: String,
    val htmlEntry: String,
    val author: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

data class EntryInsertEntity(
    val headline: String,
    val htmlEntry: String,
    val author: String,
)
