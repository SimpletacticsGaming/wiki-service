package de.simpletactics.wiki.adapter.persistence

import de.simpletactics.wiki.adapter.dto.EntryEntity
import de.simpletactics.wiki.adapter.dto.EntryInsertEntity
import de.simpletactics.wiki.adapter.dto.TopicEntity
import de.simpletactics.wiki.adapter.dto.TopicInsertEntity
import de.simpletactics.wiki.adapter.dto.WikiNodeEntity
import de.simpletactics.wiki.adapter.persistence.mapper.EntryMapper
import de.simpletactics.wiki.adapter.persistence.mapper.IdMapper
import de.simpletactics.wiki.adapter.persistence.mapper.TopicRowMapper
import de.simpletactics.wiki.adapter.persistence.mapper.WikiNodeMapper
import de.simpletactics.wiki.model.WikiNotFoundException
import de.simpletactics.wiki.model.WikiType
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class WikiSqlAdapter(
    private val jdbc: JdbcTemplate,
) {

    fun getWikiType(id: Int): WikiType? {
        val raw = jdbc.query(
            "SELECT type FROM wiki WHERE id = ?",
            { rs, _ -> rs.getString("type") },
            id,
        ).firstOrNull() ?: return null

        val type = WikiType.entries.firstOrNull { it.name == raw }
        if (type == null) log.error("Unknown wiki type '$raw' stored for id $id")
        return type
    }

    /**
     * The node itself plus every node beneath it, resolved in one round trip.
     *
     * Callers need the whole subtree to answer a single question (sita-backend
     * asks it on every authorization check), so walking it child-by-child would
     * be one query per node. UNION rather than UNION ALL: a `child_id` cycle
     * would otherwise recurse forever.
     */
    fun getDescendants(id: Int): List<WikiNodeEntity> = jdbc.query(
        """
        WITH RECURSIVE subtree(id) AS (
            SELECT CAST(? AS integer)
            UNION
            SELECT child.id
            FROM subtree
            JOIN wiki_topic ON wiki_topic.id = subtree.id
            CROSS JOIN LATERAL unnest(wiki_topic.child_id) AS child(id)
        )
        SELECT subtree.id, wiki.type
        FROM subtree
        JOIN wiki ON wiki.id = subtree.id
        """.trimIndent(),
        WikiNodeMapper(),
        id,
    )

    fun getTopic(id: Int): TopicEntity? =
        jdbc.query("SELECT * FROM wiki_topic WHERE id = ?", TopicRowMapper(), id).firstOrNull()

    fun createTopic(topicEntity: TopicInsertEntity): Int =
        jdbc.query(
            "INSERT INTO wiki_topic (topic, author) VALUES (?, ?) RETURNING id",
            IdMapper(),
            topicEntity.topic,
            topicEntity.author,
        ).firstOrNull() ?: throw IllegalStateException("Insert of topic '${topicEntity.topic}' returned no id")

    fun updateTopic(topicEntity: TopicEntity): Int {
        val affectedRows = jdbc.update(
            "UPDATE wiki_topic SET topic = ?, child_id = ? WHERE id = ?",
            topicEntity.topic,
            topicEntity.childIds.toTypedArray(),
            topicEntity.id,
        )
        if (affectedRows != 1) {
            throw WikiNotFoundException(
                "Update topic affected $affectedRows rows instead of 1 for id ${topicEntity.id}"
            )
        }
        return affectedRows
    }

    fun deleteTopic(id: Int) {
        jdbc.update("DELETE FROM wiki_topic WHERE id = ?", id)
    }

    fun getEntry(id: Int): EntryEntity? =
        jdbc.query("SELECT * FROM wiki_entry WHERE id = ?", EntryMapper(), id).firstOrNull()

    fun createEntry(entryEntity: EntryInsertEntity): Int =
        jdbc.query(
            "INSERT INTO wiki_entry (headline, body, author) VALUES (?, ?, ?) RETURNING id",
            IdMapper(),
            entryEntity.headline,
            entryEntity.htmlEntry,
            entryEntity.author,
        ).firstOrNull() ?: throw IllegalStateException("Insert of entry '${entryEntity.headline}' returned no id")

    fun updateEntry(entryEntity: EntryEntity): Int {
        val affectedRows = jdbc.update(
            "UPDATE wiki_entry SET headline = ?, body = ? WHERE id = ?",
            entryEntity.headline,
            entryEntity.htmlEntry,
            entryEntity.id,
        )
        if (affectedRows != 1) {
            throw WikiNotFoundException(
                "Update entry affected $affectedRows rows instead of 1 for id ${entryEntity.id}"
            )
        }
        return affectedRows
    }

    fun deleteEntry(id: Int) {
        jdbc.update("DELETE FROM wiki_entry WHERE id = ?", id)
    }

    fun getTopicForChild(childId: Int): TopicEntity? =
        jdbc.query("SELECT * FROM wiki_topic WHERE ? = ANY(child_id)", TopicRowMapper(), childId)
            .firstOrNull()

    fun getAuthor(id: Int): String? = when (getWikiType(id)) {
        WikiType.TOPIC -> queryAuthor("wiki_topic", id)
        WikiType.ENTRY -> queryAuthor("wiki_entry", id)
        WikiType.POLL -> queryAuthor("wiki_poll", id)
        null -> null
    }

    private fun queryAuthor(table: String, id: Int): String? =
        jdbc.query("SELECT author FROM $table WHERE id = ?", { rs, _ -> rs.getString("author") }, id)
            .firstOrNull()

    companion object {
        private val log = LoggerFactory.getLogger(WikiSqlAdapter::class.java)
    }
}
