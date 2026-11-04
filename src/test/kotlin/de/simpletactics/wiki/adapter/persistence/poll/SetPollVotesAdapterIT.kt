package de.simpletactics.wiki.adapter.persistence.poll

import de.simpletactics.wiki.adapter.FunSpecIT
import de.simpletactics.wiki.adapter.dto.poll.PollVoteEnum
import de.simpletactics.wiki.adapter.dto.poll.VoteSubmission
import de.simpletactics.wiki.services.port.PollPort
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.springframework.jdbc.core.JdbcTemplate

private const val OPTION_ONE = "ddb11436-bdc8-4488-87f6-fsdfsd"
private const val OPTION_TWO = "aaa1436-bdc8-4488-87f6-fsdfsd"

class SetPollVotesAdapterIT(
    private val pollPort: PollPort,
    jdbcTemplate: JdbcTemplate,
) : FunSpecIT(jdbcTemplate, {

    context("Setting poll votes") {
        val pollId = 2

        beforeTest {
            executeSql("wiki_poll.sql")
        }

        test("set first poll votes") {
            pollPort.setPollVotes(
                pollId,
                1,
                listOf(
                    VoteSubmission(OPTION_ONE, PollVoteEnum.TRUE),
                    VoteSubmission(OPTION_TWO, PollVoteEnum.TRUE),
                ),
            )

            val poll = pollPort.getPoll(pollId)
            requireNotNull(poll)
            with(poll) {
                question shouldBe "This is a test"
                description shouldBe "Test description"
                pollEntries shouldHaveSize 2
                pollEntries.forEach {
                    it.votes shouldHaveSize 1
                    it.votes.first().userId shouldBe 1
                    it.votes.first().option shouldBe PollVoteEnum.TRUE
                }
            }
        }

        test("does not touch options the submission leaves out") {
            pollPort.setPollVotes(pollId, 1, listOf(VoteSubmission(OPTION_ONE, PollVoteEnum.TRUE)))
            pollPort.setPollVotes(pollId, 1, listOf(VoteSubmission(OPTION_TWO, PollVoteEnum.FALSE)))

            val poll = pollPort.getPoll(pollId)
            requireNotNull(poll)
            with(poll.pollEntries.first { it.pollOption.uuid == OPTION_ONE }) {
                votes shouldHaveSize 1
                votes.first().option shouldBe PollVoteEnum.TRUE
            }
            with(poll.pollEntries.first { it.pollOption.uuid == OPTION_TWO }) {
                votes shouldHaveSize 1
                votes.first().option shouldBe PollVoteEnum.FALSE
            }
        }

        test("does not override other users votes") {
            pollPort.setPollVotes(pollId, 3, listOf(VoteSubmission(OPTION_ONE, PollVoteEnum.TRUE)))
            pollPort.setPollVotes(pollId, 1, listOf(VoteSubmission(OPTION_ONE, PollVoteEnum.FALSE)))

            val poll = pollPort.getPoll(pollId)
            requireNotNull(poll)
            with(poll.pollEntries.first { it.pollOption.uuid == OPTION_ONE }) {
                votes shouldHaveSize 2
                votes.first { it.userId == 3 }.option shouldBe PollVoteEnum.TRUE
                votes.first { it.userId == 1 }.option shouldBe PollVoteEnum.FALSE
            }
        }

        test("replaces a users own earlier vote instead of appending") {
            pollPort.setPollVotes(pollId, 3, listOf(VoteSubmission(OPTION_ONE, PollVoteEnum.FALSE)))
            pollPort.setPollVotes(pollId, 3, listOf(VoteSubmission(OPTION_ONE, PollVoteEnum.TRUE)))

            val poll = pollPort.getPoll(pollId)
            requireNotNull(poll)
            with(poll.pollEntries.first { it.pollOption.uuid == OPTION_ONE }) {
                votes shouldHaveSize 1
                votes.first().userId shouldBe 3
                votes.first().option shouldBe PollVoteEnum.TRUE
            }
        }

        test("option text containing quotes and sql survives a vote round trip") {
            val hostile = """O'Brien "x" '); DROP TABLE wiki_poll; --"""
            val updated = pollPort.updatePoll(
                de.simpletactics.wiki.adapter.dto.poll.PollUpdateEntity(
                    id = pollId,
                    question = hostile,
                    description = hostile,
                    endDate = null,
                    pollEntries = listOf(
                        de.simpletactics.wiki.adapter.dto.poll.PollEntryEntity(
                            de.simpletactics.wiki.adapter.dto.poll.PollOption(OPTION_ONE, hostile)
                        )
                    ),
                )
            )
            updated.pollEntries.first().pollOption.text shouldBe hostile

            pollPort.setPollVotes(pollId, 7, listOf(VoteSubmission(OPTION_ONE, PollVoteEnum.TRUE)))

            val poll = pollPort.getPoll(pollId)
            requireNotNull(poll)
            poll.question shouldBe hostile
            poll.pollEntries.first().pollOption.text shouldBe hostile
            poll.pollEntries.first().votes shouldHaveSize 1
        }
    }
})
