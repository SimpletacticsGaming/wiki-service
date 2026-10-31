package de.simpletactics.wiki.adapter.persistence.poll

import de.simpletactics.wiki.adapter.FunSpecIT
import de.simpletactics.wiki.adapter.dto.poll.PollEntryEntity
import de.simpletactics.wiki.adapter.dto.poll.PollInsertEntity
import de.simpletactics.wiki.adapter.dto.poll.PollOption
import de.simpletactics.wiki.adapter.dto.poll.PollVoteEnum
import de.simpletactics.wiki.adapter.dto.poll.Vote
import de.simpletactics.wiki.services.port.PollPort
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

class SavePollAdapterIT(
    val pollPort: PollPort,
    jdbcTemplate: JdbcTemplate,
) : FunSpecIT(jdbcTemplate, {

    val notPresentPollId = 999
    val presentPollId = 5

    context("Get and save poll information") {

        beforeTest {
            executeSql("wiki_poll.sql")
        }

        test("save poll under a topic") {
            val generatedId = pollPort.savePoll(
                24,
                PollInsertEntity(
                    question = "This is a test",
                    pollEntries = listOf(
                        PollEntryEntity(
                            PollOption("ddb11436-bdc8-4488-87f6-fsdfsd", "testCase"),
                            mutableListOf(
                                Vote(
                                    1,
                                    OffsetDateTime.of(2023, 10, 21, 0, 0, 0, 0, ZoneOffset.UTC),
                                    PollVoteEnum.FALSE,
                                )
                            ),
                        )
                    ),
                    description = "",
                    endDate = null,
                    author = "testAuthor",
                ),
            )

            val poll = pollPort.getPoll(generatedId)

            requireNotNull(poll)
            with(poll) {
                id shouldBe generatedId
                description shouldBe ""
                endDate shouldBe null
                ended shouldBe false
                pollEntries shouldHaveSize 1
                with(pollEntries.first()) {
                    pollOption.text shouldBe "testCase"
                    pollOption.uuid shouldBe "ddb11436-bdc8-4488-87f6-fsdfsd"
                    votes shouldHaveSize 1
                    with(votes.first()) {
                        userId shouldBe 1
                        option shouldBe PollVoteEnum.FALSE
                    }
                }
            }
        }

        test("saved poll is registered as a child of its topic") {
            val generatedId = pollPort.savePoll(
                24,
                PollInsertEntity("Child check", emptyList(), "", null, "testAuthor"),
            )

            jdbcTemplate.queryForObject(
                "SELECT array_to_string(child_id, ',') FROM wiki_topic WHERE id = 24",
                String::class.java,
            ) shouldBe generatedId.toString()
        }

        test("get present poll") {
            val entity = pollPort.getPoll(presentPollId)

            requireNotNull(entity)
            with(entity) {
                id shouldBe presentPollId
                question shouldBe "Frage 1"
                description shouldBe "Beschreibung 1"
                ended shouldBe false
                author shouldBe "testAuthor"
                endDate shouldBe LocalDate.of(2023, 10, 21)
                pollEntries shouldHaveSize 2
                createdAt.toLocalDate() shouldBe LocalDate.of(2023, 10, 21)
                updatedAt.toLocalDate() shouldBe LocalDate.of(2023, 10, 21)
            }
        }

        test("get not present poll") {
            pollPort.getPoll(notPresentPollId) shouldBe null
        }
    }
})
