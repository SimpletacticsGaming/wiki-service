package de.simpletactics.wiki.adapter.dto.poll

/** One option of a poll plus the votes cast on it. Serialized into `wiki_poll.data`. */
data class PollEntryEntity(
    val pollOption: PollOption,
    val votes: MutableList<Vote> = mutableListOf(),
)
