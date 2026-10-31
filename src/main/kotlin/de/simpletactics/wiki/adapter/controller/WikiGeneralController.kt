package de.simpletactics.wiki.adapter.controller

import de.simpletactics.wiki.api.WikiNodesApi
import de.simpletactics.wiki.api.dto.WikiAuthorResponse
import de.simpletactics.wiki.api.dto.WikiButtonResponse
import de.simpletactics.wiki.api.dto.WikiNodeTypeResponse
import de.simpletactics.wiki.model.WikiNotFoundException
import de.simpletactics.wiki.services.PanelSide
import de.simpletactics.wiki.services.WikiService
import de.simpletactics.wiki.services.port.WikiPort
import de.simpletactics.wiki.services.toWire
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Wiki nodes")
@RestController
class WikiGeneralController(
    private val wikiService: WikiService,
    private val wikiPort: WikiPort,
) : WikiNodesApi {

    override fun getWikiType(id: Int): ResponseEntity<WikiNodeTypeResponse> {
        val type = wikiPort.getWikiType(id)
            ?: throw WikiNotFoundException("No wiki node found with id $id")

        return ResponseEntity.ok(WikiNodeTypeResponse(id, type.toWire()))
    }

    override fun getWikiAuthor(id: Int): ResponseEntity<WikiAuthorResponse> =
        ResponseEntity.ok(WikiAuthorResponse(id, wikiPort.getAuthor(id)))

    /**
     * [id] and every node beneath it, as a flat list.
     *
     * Exists for sita-backend's permission check, which grants access to a topic
     * when any node under it is permitted. That check runs on every wiki request,
     * so it needs the subtree in one call rather than one request per node. A
     * 404 rather than an empty list for an unknown id, so a caller can tell
     * "no such node" from "a leaf".
     */
    override fun getDescendants(id: Int): ResponseEntity<List<WikiNodeTypeResponse>> {
        val descendants = wikiPort.getDescendants(id)
        if (descendants.isEmpty()) throw WikiNotFoundException("No wiki node found with id $id")

        return ResponseEntity.ok(descendants.map { WikiNodeTypeResponse(it.id, it.type.toWire()) })
    }

    /**
     * Sidebar for the node the user currently has open: the containing topic
     * followed by its relevant children. [id] is any wiki node, not necessarily
     * a topic.
     */
    override fun getSidebar(id: Int): ResponseEntity<List<WikiButtonResponse>> {
        val topic = wikiService.getTopicForChild(id)
            ?: throw WikiNotFoundException("No parent topic found for wiki node $id")

        return ResponseEntity.ok(
            wikiService.getWikiButtonsForPanel(topic.id, PanelSide.LEFT, wikiPort.getWikiType(id))
        )
    }
}
