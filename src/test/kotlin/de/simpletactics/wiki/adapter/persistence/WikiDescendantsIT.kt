package de.simpletactics.wiki.adapter.persistence

import de.simpletactics.wiki.adapter.FunSpecIT
import de.simpletactics.wiki.adapter.WikiAdapter
import de.simpletactics.wiki.adapter.dto.WikiNodeEntity
import de.simpletactics.wiki.model.WikiType
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.springframework.jdbc.core.JdbcTemplate

/**
 * The subtree query backs sita-backend's permission check, which runs on every
 * wiki request - so it has to be one statement, and it has to survive the data
 * actually in the table, cycles included.
 */
class WikiDescendantsIT(
    private val wikiAdapter: WikiAdapter,
    jdbcTemplate: JdbcTemplate,
) : FunSpecIT(jdbcTemplate, {

    beforeTest {
        executeSql("wiki_topic.sql")
    }

    test("returns the node itself plus every node beneath it, at any depth") {
        // 8 -> [2, 7]; 7 -> [16, 17]
        wikiAdapter.getDescendants(8) shouldContainExactlyInAnyOrder listOf(
            WikiNodeEntity(8, WikiType.TOPIC),
            WikiNodeEntity(2, WikiType.TOPIC),
            WikiNodeEntity(7, WikiType.TOPIC),
            WikiNodeEntity(16, WikiType.ENTRY),
            WikiNodeEntity(17, WikiType.ENTRY),
        )
    }

    test("a leaf is just itself") {
        wikiAdapter.getDescendants(16) shouldContainExactlyInAnyOrder listOf(
            WikiNodeEntity(16, WikiType.ENTRY)
        )
    }

    test("a childless topic is just itself") {
        wikiAdapter.getDescendants(3) shouldContainExactlyInAnyOrder listOf(
            WikiNodeEntity(3, WikiType.TOPIC)
        )
    }

    /** Topic 5 lists itself as its own child; UNION is what stops that looping. */
    test("a cycle terminates instead of recursing forever") {
        wikiAdapter.getDescendants(5) shouldContainExactlyInAnyOrder listOf(
            WikiNodeEntity(5, WikiType.TOPIC)
        )
    }

    test("an unknown id yields nothing, so the caller can report 404") {
        wikiAdapter.getDescendants(9999) shouldBe emptyList()
    }
})
