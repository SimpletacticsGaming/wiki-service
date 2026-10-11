package de.simpletactics.wiki.adapter.dto.poll

import java.time.OffsetDateTime

data class Vote(
    val userId: Int,
    val date: OffsetDateTime,
    val option: PollVoteEnum,
)
