package de.simpletactics.wiki.model

/**
 * Unchecked on purpose: Spring only rolls a transaction back automatically for
 * unchecked exceptions, and every throw site here means "abandon this unit of
 * work".
 */
class WikiNotFoundException(message: String) : RuntimeException(message)

class WikiAccessDeniedException(message: String) : RuntimeException(message)

class WikiConflictException(message: String) : RuntimeException(message)
