package de.simpletactics.wiki.adapter.dto.poll

import de.simpletactics.wiki.api.dto.CreatePollRequest
import de.simpletactics.wiki.api.dto.PollOptionRequest
import de.simpletactics.wiki.api.dto.PollOptionResponse
import de.simpletactics.wiki.api.dto.PollOptionVotesResponse
import de.simpletactics.wiki.api.dto.PollResponse
import de.simpletactics.wiki.api.dto.PollVoteResponse
import de.simpletactics.wiki.api.dto.SubmitVotesRequest
import de.simpletactics.wiki.api.dto.UpdatePollRequest
import java.util.UUID
import de.simpletactics.wiki.api.dto.PollVoteEnum as WirePollVoteEnum

/**
 * The stored vote enum and the wire vote enum are separate on purpose: one is
 * part of the jsonb written into `wiki_poll.data` and must stay stable for data
 * already on disk, the other is generated from the contract. They happen to
 * have the same constants, so the mapping is by name.
 */
private fun PollVoteEnum.toWire() = WirePollVoteEnum.valueOf(name)

private fun WirePollVoteEnum.toStored() = PollVoteEnum.valueOf(name)

/**
 * Projects a stored poll for one caller: option texts, aggregate counts and
 * that caller's own vote. No other user's identity leaves the service.
 */
fun PollEntity.toResponse(userId: Int): PollResponse = PollResponse(
    id = id,
    question = question,
    description = description,
    options = pollEntries.map { it.toResponse(userId) },
    endDate = endDate,
    ended = ended,
    author = author,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun PollEntryEntity.toResponse(userId: Int) = PollOptionResponse(
    uuid = pollOption.uuid,
    text = pollOption.text,
    yesVotes = votes.count { it.option == PollVoteEnum.TRUE },
    noVotes = votes.count { it.option == PollVoteEnum.FALSE },
    ownVote = (votes.firstOrNull { it.userId == userId }?.option ?: PollVoteEnum.FALSE).toWire(),
)

/**
 * The unaggregated counterpart of [toResponse], for the evaluation view. Votes
 * are ordered oldest first so the caller does not have to sort to show a
 * timeline.
 */
fun PollEntity.toVotesResponse(): List<PollOptionVotesResponse> = pollEntries.map { entry ->
    PollOptionVotesResponse(
        optionUuid = entry.pollOption.uuid,
        text = entry.pollOption.text,
        votes = entry.votes
            .sortedBy { it.date }
            .map { PollVoteResponse(it.userId, it.option.toWire(), it.date) },
    )
}

fun CreatePollRequest.toInsertEntity() = PollInsertEntity(
    question = question,
    pollEntries = options.toPollEntries(),
    description = description ?: "",
    endDate = endDate,
    author = author,
)

fun UpdatePollRequest.toUpdateEntity(id: Int) = PollUpdateEntity(
    id = id,
    question = question,
    description = description ?: "",
    endDate = endDate,
    pollEntries = options.toPollEntries(),
)

fun SubmitVotesRequest.toSubmissions(): List<VoteSubmission> {
    val duplicates = votes.groupingBy { it.optionUuid }.eachCount().filterValues { it > 1 }.keys
    require(duplicates.isEmpty()) { "Duplicate votes for option(s): ${duplicates.joinToString()}" }
    return votes.map { VoteSubmission(it.optionUuid, it.option.toStored()) }
}

private fun List<PollOptionRequest>.toPollEntries(): List<PollEntryEntity> {
    val entries = map {
        PollEntryEntity(PollOption(it.uuid ?: UUID.randomUUID().toString(), it.text))
    }
    val duplicates = entries.groupingBy { it.pollOption.uuid }.eachCount().filterValues { it > 1 }.keys
    require(duplicates.isEmpty()) { "Duplicate option uuid(s): ${duplicates.joinToString()}" }
    return entries
}
