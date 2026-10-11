package de.simpletactics.wiki.adapter.dto

import java.time.OffsetDateTime

data class TopicEntity(
    val id: Int,
    val topic: String,
    val childIds: List<Int>,
    val author: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

data class TopicInsertEntity(
    val topic: String,
    val author: String,
)
