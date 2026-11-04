package de.simpletactics.wiki.adapter.persistence

import de.simpletactics.wiki.adapter.FunSpecIT
import de.simpletactics.wiki.adapter.WikiAdapter
import de.simpletactics.wiki.adapter.dto.EntryEntity
import de.simpletactics.wiki.adapter.dto.EntryInsertEntity
import de.simpletactics.wiki.adapter.dto.TopicEntity
import de.simpletactics.wiki.adapter.dto.TopicInsertEntity
import de.simpletactics.wiki.model.WikiNotFoundException
import de.simpletactics.wiki.model.WikiType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.springframework.jdbc.core.JdbcTemplate
import java.lang.Thread.sleep
import java.time.OffsetDateTime

class WikiAdapterIT(
    private val wikiAdapter: WikiAdapter,
    jdbcTemplate: JdbcTemplate,
) : FunSpecIT(jdbcTemplate, {

    context("wiki type") {
        beforeTest {
            executeSql("wiki_type.sql")
        }

        test("get") {
            wikiAdapter.getWikiType(1) shouldBe WikiType.TOPIC
            wikiAdapter.getWikiType(2) shouldBe WikiType.ENTRY
            wikiAdapter.getWikiType(3) shouldBe WikiType.POLL
        }

        test("get no type") {
            wikiAdapter.getWikiType(99) shouldBe null
        }
    }

    context("topic") {

        beforeTest {
            executeSql("wiki_topic.sql")
        }

        test("get") {
            with(wikiAdapter.getTopic(6)) {
                requireNotNull(this)
                this.id shouldBe 6
                this.topic shouldBe "Thema 3"
                this.childIds shouldBe listOf(5)
                this.author shouldBe "Author"
                this.createdAt.shouldBeTypeOf<OffsetDateTime>()
                this.updatedAt.shouldBeTypeOf<OffsetDateTime>()
            }
            wikiAdapter.getTopic(99) shouldBe null
        }

        test("create") {
            val wikiId = wikiAdapter.createTopic(11, TopicInsertEntity("New Topic 1", "testUser"))

            wikiAdapter.getWikiType(wikiId) shouldBe WikiType.TOPIC
            val topic = wikiAdapter.getTopic(11)
            requireNotNull(topic)
            topic shouldBe TopicEntity(
                11,
                "Thema 4",
                listOf(15, wikiId),
                "unknown_user",
                topic.createdAt,
                topic.updatedAt
            )
            val newTopic = wikiAdapter.getTopic(wikiId)
            requireNotNull(newTopic)
            newTopic shouldBe TopicEntity(
                wikiId,
                "New Topic 1",
                listOf(),
                "testUser",
                newTopic.createdAt,
                newTopic.updatedAt
            )
        }

        test("update") {
            val wikiId = 9
            val originalTopic = wikiAdapter.getTopic(wikiId)
            requireNotNull(originalTopic)
            sleep(1000) // Sleep for 1 second to ensure updatedAt is after createdAt
            wikiAdapter.updateTopic(wikiId, "Updated Topic 2")
            with(wikiAdapter.getTopic(wikiId)) {
                requireNotNull(this)
                this.id shouldBe originalTopic.id
                this.topic shouldBe "Updated Topic 2"
                this.childIds shouldBe listOf(12)
                this.author shouldBe originalTopic.author
                this.createdAt.shouldBeTypeOf<OffsetDateTime>()
                this.updatedAt.shouldBeTypeOf<OffsetDateTime>()

                this.createdAt shouldBe originalTopic.createdAt
                this.updatedAt shouldBeAfter originalTopic.updatedAt
            }
        }

        test("delete") {
            val wikiId = 7
            val parentId = 8
            val topic = wikiAdapter.getTopic(wikiId)

            sleep(1000) // Sleep for 1 second to ensure updatedAt is after createdAt
            wikiAdapter.deleteTopic(wikiId)

            with(wikiAdapter.getTopic(parentId)) {
                requireNotNull(this)
                this.id shouldBe parentId
                this.topic shouldBe "Topic to delete 2"
                this.childIds shouldBe listOf(2)
                this.author shouldBe "unknown_author"
                this.createdAt.shouldBeTypeOf<OffsetDateTime>()
                this.updatedAt.shouldBeTypeOf<OffsetDateTime>()

                this.updatedAt shouldBeAfter this.createdAt
            }

            wikiAdapter.getWikiType(wikiId) shouldBe null
            wikiAdapter.getTopic(wikiId) shouldBe null

            requireNotNull(topic)
            with(topic) {
                childIds shouldHaveSize 2
                childIds.forEach { wikiAdapter.getEntry(it) shouldBe null }
            }
        }
    }

    context("entry") {

        beforeTest {
            executeSql("wiki_entry.sql")
        }

        test("get") {
            wikiAdapter.getWikiType(14) shouldBe WikiType.ENTRY
            with(wikiAdapter.getEntry(14)) {
                requireNotNull(this)
                id shouldBe 14
                headline shouldBe "Eintrag 1"
                htmlEntry shouldBe "<p>Test</p>"
                author shouldBe "Author"
                createdAt.shouldBeTypeOf<OffsetDateTime>()
                updatedAt.shouldBeTypeOf<OffsetDateTime>()
            }

            wikiAdapter.getEntry(99) shouldBe null
        }

        test("create") {
            val wikiId = wikiAdapter.createEntry(
                11,
                EntryInsertEntity("New Entry 1", "My html body", "Author")
            )
            wikiAdapter.getWikiType(wikiId) shouldBe WikiType.ENTRY
            with(wikiAdapter.getTopic(11)) {
                requireNotNull(this)
                id shouldBe 11
                topic shouldBe "Thema 1"
                childIds shouldBe listOf(14, wikiId)
                author shouldBe "Author"
                createdAt.shouldBeTypeOf<OffsetDateTime>()
                updatedAt.shouldBeTypeOf<OffsetDateTime>()
            }
            with(wikiAdapter.getEntry(wikiId)) {
                requireNotNull(this)
                id shouldBe wikiId
                headline shouldBe "New Entry 1"
                htmlEntry shouldBe "My html body"
                author shouldBe "Author"
                createdAt.shouldBeTypeOf<OffsetDateTime>()
                updatedAt.shouldBeTypeOf<OffsetDateTime>()
            }
        }

        test("update") {
            val wikiId = 15
            val originalEntry = wikiAdapter.getEntry(wikiId)
            requireNotNull(originalEntry)
            sleep(1000)
            wikiAdapter.updateEntry(wikiId, "Updated Entry 1", "My html body")
            wikiAdapter.getWikiType(wikiId) shouldBe WikiType.ENTRY
            with(wikiAdapter.getEntry(wikiId)) {
                requireNotNull(this)
                id shouldBe wikiId
                headline shouldBe "Updated Entry 1"
                htmlEntry shouldBe "My html body"
                author shouldBe originalEntry.author
                createdAt shouldBe originalEntry.createdAt
                updatedAt shouldBeAfter originalEntry.updatedAt
            }
        }

        test("delete") {
            val wikiId = 7
            val parentId = 8
            wikiAdapter.deleteEntry(wikiId)
            wikiAdapter.getWikiType(wikiId) shouldBe null
            with(wikiAdapter.getTopic(parentId)) {
                requireNotNull(this)
                id shouldBe parentId
                topic shouldBe "Topic with child delete"
                childIds shouldBe listOf()
                author shouldBe "Author"
                createdAt.shouldBeTypeOf<OffsetDateTime>()
                updatedAt.shouldBeTypeOf<OffsetDateTime>()
            }
            wikiAdapter.getEntry(wikiId) shouldBe null
        }
    }

    context("exception") {
        beforeTest {
            executeSql("wiki_exception.sql")
        }

        test("delete entry with topic id") {
            val topicId = 11
            shouldThrow<WikiNotFoundException> {
                wikiAdapter.deleteEntry(topicId)
            } shouldBe WikiNotFoundException("No entry found to delete with id $topicId")
        }

        test("delete topic with wrong topic id") {
            val topicId = 19
            shouldThrow<WikiNotFoundException> {
                wikiAdapter.deleteTopic(topicId)
            } shouldBe WikiNotFoundException("No topic found to delete with id $topicId")
        }

        test("update entry") {
            val entryId = 999
            shouldThrow<WikiNotFoundException> {
                wikiAdapter.updateEntry(entryId, "Updated Entry 1", "My html body")
            } shouldBe WikiNotFoundException("No entry found to update with id $entryId")
        }

        test("update topic") {
            val topicId = 999
            shouldThrow<WikiNotFoundException> {
                wikiAdapter.updateTopic(topicId, "Updated Topic 2")
            } shouldBe WikiNotFoundException("No topic found to update with id $topicId")
        }
    }
})
