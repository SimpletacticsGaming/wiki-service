package de.simpletactics.wiki.services

import de.simpletactics.wiki.adapter.SitaTransactional
import de.simpletactics.wiki.adapter.dto.TopicEntity
import de.simpletactics.wiki.api.dto.WikiButtonResponse
import de.simpletactics.wiki.model.WikiNotFoundException
import de.simpletactics.wiki.model.WikiType
import de.simpletactics.wiki.services.port.PollPort
import de.simpletactics.wiki.services.port.WikiPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import de.simpletactics.wiki.api.dto.WikiType as WireWikiType

@Service
class WikiService(
    private val wikiPort: WikiPort,
    private val pollPort: PollPort,
    private val alertService: AlertService,
) {

    fun getTopicForChild(id: Int): TopicEntity? = wikiPort.getTopicForChild(id)

    /**
     * Buttons for one panel of the wiki UI.
     *
     * The mid panel lists every child of [topicId]. The left panel is a
     * navigation sidebar: it leads with the topic itself and then, when the node
     * the user selected is itself a topic ([selectedWikiType] == [WikiType.TOPIC]),
     * lists only sibling topics - drilling into a topic should not dump all of
     * its neighbours' entries into the sidebar. For any other selection the full
     * child list is shown.
     */
    fun getWikiButtonsForPanel(
        topicId: Int,
        panelSide: PanelSide,
        selectedWikiType: WikiType? = null,
    ): List<WikiButtonResponse> {
        val topic = wikiPort.getTopic(topicId)
            ?: throw WikiNotFoundException("No topic found for id $topicId")

        val buttons = mutableListOf<WikiButtonResponse>()
        if (panelSide == PanelSide.LEFT) {
            buttons.add(WikiButtonResponse(topic.id, topic.topic, WireWikiType.TOPIC))
        }

        topic.childIds.forEach { childId ->
            val wikiType = wikiPort.getWikiType(childId)
            if (wikiType == null) {
                alert("Could not find wiki type with id $childId")
                return@forEach
            }
            if (!isVisibleIn(panelSide, wikiType, selectedWikiType)) return@forEach

            buttonFor(wikiType, childId)?.let { buttons.add(it) }
        }

        return buttons
    }

    @SitaTransactional
    fun deleteWikiTopic(id: Int): Int? {
        val parentId = wikiPort.getTopicForChild(id)?.id
        wikiPort.deleteTopic(id)
        return parentId
    }

    @SitaTransactional
    fun deleteWikiEntry(id: Int): Int? {
        val parentId = wikiPort.getTopicForChild(id)?.id
        wikiPort.deleteEntry(id)
        return parentId
    }

    @SitaTransactional
    fun deletePoll(id: Int): Int? {
        val parentId = wikiPort.getTopicForChild(id)?.id
        pollPort.deletePoll(id)
        return parentId
    }

    private fun isVisibleIn(
        panelSide: PanelSide,
        wikiType: WikiType,
        selectedWikiType: WikiType?,
    ): Boolean =
        panelSide == PanelSide.MID || wikiType == WikiType.TOPIC || selectedWikiType != WikiType.TOPIC

    private fun buttonFor(wikiType: WikiType, childId: Int): WikiButtonResponse? = when (wikiType) {
        WikiType.TOPIC -> wikiPort.getTopic(childId)
            ?.let { WikiButtonResponse(childId, it.topic, WireWikiType.TOPIC) }
            ?: missing("topic", childId)

        WikiType.ENTRY -> wikiPort.getEntry(childId)
            ?.let { WikiButtonResponse(childId, it.headline, WireWikiType.ENTRY) }
            ?: missing("entry", childId)

        WikiType.POLL -> pollPort.getPoll(childId)
            ?.let { WikiButtonResponse(childId, it.question, WireWikiType.POLL) }
            ?: missing("poll", childId)
    }

    /** A child id with no row behind it means the tree is inconsistent - alert, but keep serving. */
    private fun missing(kind: String, childId: Int): WikiButtonResponse? {
        alert("Could not find wiki $kind with id $childId")
        return null
    }

    private fun alert(errorMessage: String) {
        log.error(errorMessage)
        alertService.alert(errorMessage)
    }

    companion object {
        private val log = LoggerFactory.getLogger(WikiService::class.java)
    }
}

enum class PanelSide {
    LEFT,
    MID,
}

/**
 * The domain enum and the generated wire enum are kept apart so the persistence
 * layer does not depend on the API contract. Same constants, so map by name.
 */
fun WikiType.toWire(): WireWikiType = WireWikiType.valueOf(name)
