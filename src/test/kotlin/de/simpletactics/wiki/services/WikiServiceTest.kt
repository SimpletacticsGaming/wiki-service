package de.simpletactics.wiki.services

import de.simpletactics.wiki.adapter.dto.EntryEntity
import de.simpletactics.wiki.adapter.dto.TopicEntity
import de.simpletactics.wiki.adapter.dto.poll.PollEntity
import de.simpletactics.wiki.model.WikiNotFoundException
import de.simpletactics.wiki.model.WikiType
import de.simpletactics.wiki.api.dto.WikiType as WireWikiType
import de.simpletactics.wiki.services.port.PollPort
import de.simpletactics.wiki.services.port.WikiPort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.OffsetDateTime
import java.time.ZoneOffset

private val TIME = OffsetDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC)

private fun topic(id: Int, name: String, children: List<Int> = emptyList()) =
    TopicEntity(id, name, children, "author", TIME, TIME)

class WikiServiceTest : FunSpec({

    lateinit var wikiPort: WikiPort
    lateinit var pollPort: PollPort
    lateinit var alertService: AlertService
    lateinit var service: WikiService

    beforeTest {
        wikiPort = mockk()
        pollPort = mockk()
        alertService = mockk(relaxed = true)
        service = WikiService(wikiPort, pollPort, alertService)
    }

    test("unknown topic is reported as not found") {
        every { wikiPort.getTopic(42) } returns null

        shouldThrow<WikiNotFoundException> { service.getWikiButtonsForPanel(42, PanelSide.MID) }
    }

    test("main panel lists every child regardless of type") {
        every { wikiPort.getTopic(1) } returns topic(1, "Root", listOf(2, 3, 4))
        every { wikiPort.getWikiType(2) } returns WikiType.TOPIC
        every { wikiPort.getWikiType(3) } returns WikiType.ENTRY
        every { wikiPort.getWikiType(4) } returns WikiType.POLL
        every { wikiPort.getTopic(2) } returns topic(2, "Child topic")
        every { wikiPort.getEntry(3) } returns EntryEntity(3, "Headline", "<p/>", "a", TIME, TIME)
        every { pollPort.getPoll(4) } returns mockk<PollEntity> { every { question } returns "Question" }

        val buttons = service.getWikiButtonsForPanel(1, PanelSide.MID)

        buttons.map { it.title } shouldContainExactly listOf("Child topic", "Headline", "Question")
        buttons.map { it.type } shouldContainExactly
            listOf(WireWikiType.TOPIC, WireWikiType.ENTRY, WireWikiType.POLL)
    }

    test("sidebar leads with the topic itself and hides non-topic siblings of a selected topic") {
        every { wikiPort.getTopic(1) } returns topic(1, "Root", listOf(2, 3))
        every { wikiPort.getWikiType(2) } returns WikiType.TOPIC
        every { wikiPort.getWikiType(3) } returns WikiType.ENTRY
        every { wikiPort.getTopic(2) } returns topic(2, "Child topic")

        val buttons = service.getWikiButtonsForPanel(1, PanelSide.LEFT, selectedWikiType = WikiType.TOPIC)

        buttons.map { it.title } shouldContainExactly listOf("Root", "Child topic")
    }

    test("sidebar shows every sibling when the selected node is not a topic") {
        every { wikiPort.getTopic(1) } returns topic(1, "Root", listOf(2, 3))
        every { wikiPort.getWikiType(2) } returns WikiType.TOPIC
        every { wikiPort.getWikiType(3) } returns WikiType.ENTRY
        every { wikiPort.getTopic(2) } returns topic(2, "Child topic")
        every { wikiPort.getEntry(3) } returns EntryEntity(3, "Headline", "<p/>", "a", TIME, TIME)

        val buttons = service.getWikiButtonsForPanel(1, PanelSide.LEFT, selectedWikiType = WikiType.ENTRY)

        buttons.map { it.title } shouldContainExactly listOf("Root", "Child topic", "Headline")
    }

    test("a dangling child id is alerted and skipped instead of failing the request") {
        every { wikiPort.getTopic(1) } returns topic(1, "Root", listOf(2, 3))
        every { wikiPort.getWikiType(2) } returns null
        every { wikiPort.getWikiType(3) } returns WikiType.ENTRY
        every { wikiPort.getEntry(3) } returns null

        service.getWikiButtonsForPanel(1, PanelSide.MID).size shouldBe 0

        verify { alertService.alert("Could not find wiki type with id 2") }
        verify { alertService.alert("Could not find wiki entry with id 3") }
    }

    test("delete returns the parent id the caller should navigate to") {
        every { wikiPort.getTopicForChild(5) } returns topic(9, "Parent")
        every { wikiPort.deleteEntry(5) } returns Unit

        service.deleteWikiEntry(5) shouldBe 9
    }
})
