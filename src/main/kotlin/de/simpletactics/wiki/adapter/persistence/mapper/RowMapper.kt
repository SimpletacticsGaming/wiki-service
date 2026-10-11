package de.simpletactics.wiki.adapter.persistence.mapper

import com.fasterxml.jackson.module.kotlin.readValue
import de.simpletactics.wiki.adapter.dto.EntryEntity
import de.simpletactics.wiki.adapter.dto.TopicEntity
import de.simpletactics.wiki.adapter.dto.WikiNodeEntity
import de.simpletactics.wiki.adapter.dto.poll.PollEntity
import de.simpletactics.wiki.adapter.dto.poll.PollEntryEntity
import de.simpletactics.wiki.model.WikiType
import de.simpletactics.wiki.util.jsonObjectMapper
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet
import java.time.LocalDate
import java.time.OffsetDateTime

private fun ResultSet.offsetDateTime(column: String): OffsetDateTime =
    getObject(column, OffsetDateTime::class.java)

class TopicRowMapper : RowMapper<TopicEntity> {

    override fun mapRow(resultSet: ResultSet, rowNum: Int): TopicEntity = with(resultSet) {
        TopicEntity(
            getInt("id"),
            getString("topic"),
            childIds(this),
            getString("author"),
            offsetDateTime("created_at"),
            offsetDateTime("updated_at"),
        )
    }

    private fun childIds(resultSet: ResultSet): List<Int> {
        val array = resultSet.getArray("child_id") ?: return emptyList()
        @Suppress("UNCHECKED_CAST")
        return (array.array as Array<Integer>).map { it.toInt() }
    }
}

class EntryMapper : RowMapper<EntryEntity> {

    override fun mapRow(resultSet: ResultSet, rowNum: Int): EntryEntity = with(resultSet) {
        EntryEntity(
            getInt("id"),
            getString("headline"),
            getString("body"),
            getString("author"),
            offsetDateTime("created_at"),
            offsetDateTime("updated_at"),
        )
    }
}

class WikiNodeMapper : RowMapper<WikiNodeEntity> {

    override fun mapRow(resultSet: ResultSet, rowNum: Int): WikiNodeEntity = WikiNodeEntity(
        resultSet.getInt("id"),
        WikiType.valueOf(resultSet.getString("type")),
    )
}

class IdMapper : RowMapper<Int> {
    override fun mapRow(resultSet: ResultSet, rowNum: Int): Int = resultSet.getInt("id")
}

class PollMapper : RowMapper<PollEntity> {
    override fun mapRow(resultSet: ResultSet, rowNum: Int): PollEntity = with(resultSet) {
        PollEntity(
            getInt("id"),
            getString("question"),
            parsePollEntries(getString("data")),
            getString("description") ?: "",
            getBoolean("ended"),
            getObject("end_date", LocalDate::class.java),
            getString("author"),
            offsetDateTime("created_at"),
            offsetDateTime("updated_at"),
        )
    }

    private fun parsePollEntries(json: String?): List<PollEntryEntity> =
        jsonObjectMapper.readValue(json ?: "[]")
}
