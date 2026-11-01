package de.simpletactics.wiki.adapter.dto

import de.simpletactics.wiki.api.dto.EntryResponse
import de.simpletactics.wiki.api.dto.TopicResponse

/**
 * Entity to contract projections, kept next to the entities rather than in the
 * controllers - same split as [de.simpletactics.wiki.adapter.dto.poll.toResponse].
 */
fun EntryEntity.toResponse() = EntryResponse(
    id = id,
    headline = headline,
    content = htmlEntry,
    author = author,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun TopicEntity.toResponse() = TopicResponse(
    id = id,
    topic = topic,
    author = author,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
