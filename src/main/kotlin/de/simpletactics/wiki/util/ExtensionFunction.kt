package de.simpletactics.wiki.util

import de.simpletactics.wiki.model.WikiNotFoundException
import de.simpletactics.wiki.model.WikiType
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun verify(
    actualWikiType: WikiType?,
    shouldBeWikiType: WikiType,
    shouldNotBeNull: Any?,
    errorMessage: () -> String
) {

    contract {
        returns() implies (shouldNotBeNull != null)
    }

    if (actualWikiType != shouldBeWikiType || shouldNotBeNull == null) {
        throw WikiNotFoundException(errorMessage.invoke())
    }
}
