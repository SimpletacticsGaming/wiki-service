package de.simpletactics.wiki.adapter.dto.poll

/** A single user's choice for one option, as submitted through the API. */
data class VoteSubmission(
    val optionUuid: String,
    val option: PollVoteEnum,
)
