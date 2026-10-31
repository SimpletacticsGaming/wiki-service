package de.simpletactics.wiki.adapter.persistence

import de.simpletactics.wiki.adapter.dto.poll.PollEntity
import de.simpletactics.wiki.adapter.dto.poll.PollEntryEntity
import de.simpletactics.wiki.adapter.dto.poll.PollInsertEntity
import de.simpletactics.wiki.adapter.dto.poll.PollUpdateEntity
import de.simpletactics.wiki.adapter.dto.poll.Vote
import de.simpletactics.wiki.adapter.dto.poll.VoteSubmission
import de.simpletactics.wiki.adapter.persistence.mapper.IdMapper
import de.simpletactics.wiki.adapter.persistence.mapper.PollMapper
import de.simpletactics.wiki.model.WikiNotFoundException
import de.simpletactics.wiki.util.jsonObjectMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * All poll state lives in the `wiki_poll.data` JSONB column. Rather than editing
 * that document with generated `jsonb_set`/`jsonb_path_query` SQL - which meant
 * concatenating user-supplied option text and uuids straight into a statement -
 * every mutation reads the document, changes it in Kotlin and writes it back
 * through a single bound parameter. Nothing user-supplied reaches the SQL text.
 */
@Component
class PollSqlAdapter(
    private val jdbc: JdbcTemplate,
) {

    fun savePoll(poll: PollInsertEntity): Int {
        return jdbc.query(
            """
            INSERT INTO wiki_poll (question, description, end_date, ended, data, author)
            VALUES (?, ?, ?, false, ?::jsonb, ?)
            RETURNING id
            """.trimIndent(),
            IdMapper(),
            poll.question,
            poll.description,
            poll.endDate,
            jsonObjectMapper.writeValueAsString(poll.pollEntries),
            poll.author,
        ).firstOrNull() ?: throw IllegalStateException("Insert of poll '${poll.question}' returned no id")
    }

    fun getPoll(id: Int): PollEntity? =
        jdbc.query("SELECT * FROM wiki_poll WHERE id = ?", PollMapper(), id).firstOrNull()

    /**
     * Applies the scalar fields and reconciles the option list: options that
     * disappeared are dropped, new ones are appended, renamed ones keep the
     * votes already cast on them.
     */
    fun updatePoll(update: PollUpdateEntity): PollEntity {
        val existing = requirePoll(update.id)

        val byUuid = existing.pollEntries.associateBy { it.pollOption.uuid }
        val merged = update.pollEntries.map { updated ->
            val previous = byUuid[updated.pollOption.uuid]
            if (previous == null) updated
            else PollEntryEntity(updated.pollOption, previous.votes.toMutableList())
        }

        jdbc.update(
            """
            UPDATE wiki_poll
            SET question = ?, description = ?, end_date = ?, data = ?::jsonb
            WHERE id = ?
            """.trimIndent(),
            update.question,
            update.description,
            update.endDate,
            jsonObjectMapper.writeValueAsString(merged),
            update.id,
        )

        return requirePoll(update.id)
    }

    /**
     * Records [userId]'s choices. A user has at most one vote per option, so an
     * existing vote is replaced rather than appended.
     */
    fun setPollVotes(pollId: Int, userId: Int, votes: List<VoteSubmission>): PollEntity {
        val poll = requirePoll(pollId)
        val submitted = votes.associate { it.optionUuid to it.option }
        val now = OffsetDateTime.now()

        val updated = poll.pollEntries.map { entry ->
            val choice = submitted[entry.pollOption.uuid] ?: return@map entry
            val remaining = entry.votes.filterNot { it.userId == userId }.toMutableList()
            remaining.add(Vote(userId, now, choice))
            PollEntryEntity(entry.pollOption, remaining)
        }

        jdbc.update(
            "UPDATE wiki_poll SET data = ?::jsonb WHERE id = ?",
            jsonObjectMapper.writeValueAsString(updated),
            pollId,
        )

        return requirePoll(pollId)
    }

    fun deletePoll(id: Int): Int = jdbc.update("DELETE FROM wiki_poll WHERE id = ?", id)

    /** Closes the poll without touching `end_date` - the original deadline is history worth keeping. */
    fun endPoll(id: Int) {
        val affected = jdbc.update("UPDATE wiki_poll SET ended = true WHERE id = ?", id)
        if (affected != 1) throw WikiNotFoundException("No poll found to close with id $id")
    }

    /**
     * Reopens a closed poll. A null [endDate] leaves the stored deadline alone;
     * clearing it would silently turn a dated poll into an open-ended one.
     */
    fun reopenPoll(id: Int, endDate: LocalDate?) {
        val affected = if (endDate == null) {
            jdbc.update("UPDATE wiki_poll SET ended = false WHERE id = ?", id)
        } else {
            jdbc.update("UPDATE wiki_poll SET ended = false, end_date = ? WHERE id = ?", endDate, id)
        }
        if (affected != 1) throw WikiNotFoundException("No poll found to reopen with id $id")
    }

    /**
     * A poll is open while it is not flagged ended and its deadline has not
     * passed. Reading only the `ended` flag would let votes land on a poll whose
     * end date is long gone but which the scheduler has not swept up yet.
     */
    fun isPollOpen(id: Int): Boolean {
        val open = jdbc.query(
            """
            SELECT (ended = false AND (end_date IS NULL OR end_date >= CURRENT_DATE)) AS is_open
            FROM wiki_poll WHERE id = ?
            """.trimIndent(),
            { rs, _ -> rs.getBoolean("is_open") },
            id,
        ).firstOrNull()

        return open ?: throw WikiNotFoundException("No poll found with id $id")
    }

    fun closeExpiredOpenPolls(): Int = jdbc.update(
        "UPDATE wiki_poll SET ended = true WHERE ended = false AND end_date IS NOT NULL AND end_date < CURRENT_DATE"
    )

    private fun requirePoll(id: Int): PollEntity =
        getPoll(id) ?: throw WikiNotFoundException("No poll found with id $id")
}
