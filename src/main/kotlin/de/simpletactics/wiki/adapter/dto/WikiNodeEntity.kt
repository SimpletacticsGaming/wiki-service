package de.simpletactics.wiki.adapter.dto

import de.simpletactics.wiki.model.WikiType

/** A wiki node reduced to the two things a caller needs to reason about the tree. */
data class WikiNodeEntity(
    val id: Int,
    val type: WikiType,
)
