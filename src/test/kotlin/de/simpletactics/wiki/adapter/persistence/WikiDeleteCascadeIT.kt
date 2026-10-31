package de.simpletactics.wiki.adapter.persistence

import de.simpletactics.wiki.adapter.FunSpecIT
import de.simpletactics.wiki.adapter.WikiAdapter
import de.simpletactics.wiki.model.WikiConflictException
import de.simpletactics.wiki.services.port.PollPort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject

class WikiDeleteCascadeIT(
    private val wikiAdapter: WikiAdapter,
    private val pollPort: PollPort,
    jdbcTemplate: JdbcTemplate,
) : FunSpecIT(jdbcTemplate, {

    fun countIn(table: String, ids: String) =
        jdbcTemplate.queryForObject<Int>("SELECT count(*) FROM $table WHERE id IN ($ids)")

    context("delete cascade") {

        beforeTest {
            executeSql("wiki_cascade.sql")
        }

        test("deleting a topic removes its whole subtree, polls included") {
            wikiAdapter.deleteTopic(101)

            // polls used to be skipped here, leaving orphan wiki_poll/wiki rows
            pollPort.getPoll(104) shouldBe null
            countIn("wiki_poll", "104") shouldBe 0
            countIn("wiki_topic", "101, 102") shouldBe 0
            countIn("wiki_entry", "103, 105") shouldBe 0

            // and nothing is left dangling in the shared wiki table
            countIn("wiki", "101, 102, 103, 104, 105") shouldBe 0
        }

        test("the deleted topic is detached from its parent") {
            wikiAdapter.deleteTopic(101)

            wikiAdapter.getTopic(100)?.childIds shouldBe emptyList()
        }

        test("a topic without a parent is refused rather than silently orphaning the tree") {
            shouldThrow<WikiConflictException> { wikiAdapter.deleteTopic(100) }

            wikiAdapter.getTopic(100)?.childIds shouldBe listOf(101)
            countIn("wiki_topic", "100, 101, 102") shouldBe 3
        }
    }
})
