package de.simpletactics.wiki.adapter.persistence.poll

import de.simpletactics.wiki.adapter.FunSpecIT
import de.simpletactics.wiki.adapter.dto.poll.PollEntity
import de.simpletactics.wiki.adapter.dto.poll.PollEntryEntity
import de.simpletactics.wiki.adapter.dto.poll.PollOption
import de.simpletactics.wiki.adapter.dto.poll.PollUpdateEntity
import de.simpletactics.wiki.services.port.PollPort
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate
import java.util.UUID

private const val POLL_ID = 3
private const val EXISTING_OPTION = "ddb11436-bdc8-4488-87f6-fsdfsd"

private fun PollEntity.toUpdate(
    question: String = this.question,
    description: String = this.description,
    endDate: LocalDate? = this.endDate,
    pollEntries: List<PollEntryEntity> = this.pollEntries,
) = PollUpdateEntity(id, question, description, endDate, pollEntries)

class UpdatePollAdapterIT(
    private val pollPort: PollPort,
    jdbcTemplate: JdbcTemplate,
) : FunSpecIT(jdbcTemplate, {

    fun currentPoll() = pollPort.getPoll(POLL_ID)!!

    context("Update polls") {

        beforeTest {
            executeSql("wiki_poll.sql")
        }

        test("update question and description") {
            val updated = pollPort.updatePoll(
                currentPoll().toUpdate(question = "Updated question", description = "Updated description")
            )

            with(updated) {
                id shouldBe POLL_ID
                question shouldBe "Updated question"
                description shouldBe "Updated description"
            }
        }

        test("update end date") {
            val updated = pollPort.updatePoll(currentPoll().toUpdate(endDate = LocalDate.of(2099, 1, 2)))

            updated.endDate shouldBe LocalDate.of(2099, 1, 2)
        }

        test("clear end date") {
            pollPort.updatePoll(currentPoll().toUpdate(endDate = LocalDate.of(2099, 1, 2)))

            pollPort.updatePoll(currentPoll().toUpdate(endDate = null)).endDate shouldBe null
        }

        test("insert a new poll entry") {
            val entries = currentPoll().pollEntries +
                PollEntryEntity(PollOption(UUID.randomUUID().toString(), "New Option"))

            val updated = pollPort.updatePoll(currentPoll().toUpdate(pollEntries = entries))

            updated.pollEntries shouldHaveSize 2
            updated.pollEntries.last().pollOption.text shouldBe "New Option"
            updated.pollEntries.last().votes shouldHaveSize 0
        }

        test("renaming an option keeps the votes already cast on it") {
            val entries = currentPoll().pollEntries.map {
                PollEntryEntity(PollOption(it.pollOption.uuid, "Updated entry"), it.votes)
            }

            val updated = pollPort.updatePoll(currentPoll().toUpdate(pollEntries = entries))

            updated.pollEntries shouldHaveSize 1
            with(updated.pollEntries.first()) {
                pollOption.uuid shouldBe EXISTING_OPTION
                pollOption.text shouldBe "Updated entry"
                votes shouldHaveSize 1
                votes.first().userId shouldBe 1
            }
        }

        test("a submitted option cannot smuggle in votes for someone else") {
            val entries = listOf(PollEntryEntity(PollOption(EXISTING_OPTION, "testCase")))

            val updated = pollPort.updatePoll(currentPoll().toUpdate(pollEntries = entries))

            // votes come from the stored poll, never from the update payload
            updated.pollEntries.first().votes shouldHaveSize 1
            updated.pollEntries.first().votes.first().userId shouldBe 1
        }

        test("delete a poll entry") {
            val updated = pollPort.updatePoll(currentPoll().toUpdate(pollEntries = emptyList()))

            updated.pollEntries shouldHaveSize 0
        }
    }
})
