package de.simpletactics.wiki.adapter.controller

import de.simpletactics.wiki.adapter.dto.EntryInsertEntity
import de.simpletactics.wiki.adapter.dto.TopicInsertEntity
import de.simpletactics.wiki.adapter.dto.poll.toInsertEntity
import de.simpletactics.wiki.api.WikiTopicsApi
import de.simpletactics.wiki.api.dto.CreateEntryRequest
import de.simpletactics.wiki.api.dto.CreatePollRequest
import de.simpletactics.wiki.api.dto.CreateTopicRequest
import de.simpletactics.wiki.api.dto.CreatedResponse
import de.simpletactics.wiki.api.dto.DeletedResponse
import de.simpletactics.wiki.api.dto.TopicResponse
import de.simpletactics.wiki.api.dto.UpdateTopicRequest
import de.simpletactics.wiki.api.dto.WikiButtonResponse
import de.simpletactics.wiki.model.WikiNotFoundException
import de.simpletactics.wiki.services.PanelSide
import de.simpletactics.wiki.services.WikiService
import de.simpletactics.wiki.services.port.PollPort
import de.simpletactics.wiki.services.port.WikiPort
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

/**
 * Routes and payload types come from [WikiTopicsApi], generated from
 * `openapi/wiki-api.yaml`. Keep the mappings out of this class: declaring them
 * here as well would stack a second prefix onto every path.
 */
@Tag(name = "Wiki topics")
@RestController
class WikiTopicController(
    private val wikiPort: WikiPort,
    private val pollPort: PollPort,
    private val wikiService: WikiService,
) : WikiTopicsApi {

    override fun getTopic(id: Int): ResponseEntity<TopicResponse> {
        val topic = wikiPort.getTopic(id)
            ?: throw WikiNotFoundException("No topic found with id $id")

        return ResponseEntity.ok(
            TopicResponse(topic.id, topic.topic, topic.author, topic.createdAt, topic.updatedAt)
        )
    }

    override fun createChildTopic(
        parentId: Int,
        createTopicRequest: CreateTopicRequest,
    ): ResponseEntity<CreatedResponse> = ResponseEntity.status(HttpStatus.CREATED).body(
        CreatedResponse(
            wikiPort.createTopic(
                parentId,
                TopicInsertEntity(createTopicRequest.topic, createTopicRequest.author),
            )
        )
    )

    override fun createEntry(
        id: Int,
        createEntryRequest: CreateEntryRequest,
    ): ResponseEntity<CreatedResponse> = ResponseEntity.status(HttpStatus.CREATED).body(
        CreatedResponse(
            wikiPort.createEntry(
                id,
                EntryInsertEntity(
                    createEntryRequest.headline,
                    createEntryRequest.content ?: "",
                    createEntryRequest.author,
                ),
            )
        )
    )

    override fun createPoll(
        id: Int,
        createPollRequest: CreatePollRequest,
    ): ResponseEntity<CreatedResponse> = ResponseEntity.status(HttpStatus.CREATED)
        .body(CreatedResponse(pollPort.savePoll(id, createPollRequest.toInsertEntity())))

    override fun updateTopic(
        id: Int,
        updateTopicRequest: UpdateTopicRequest,
    ): ResponseEntity<TopicResponse> {
        wikiPort.updateTopic(id, updateTopicRequest.topic)
        return getTopic(id)
    }

    override fun deleteTopic(id: Int): ResponseEntity<DeletedResponse> =
        ResponseEntity.ok(DeletedResponse(id, wikiService.deleteWikiTopic(id)))

    /** Children of this topic, for the main panel. The sidebar lives on /api/wiki/nodes. */
    override fun getButtons(id: Int): ResponseEntity<List<WikiButtonResponse>> =
        ResponseEntity.ok(wikiService.getWikiButtonsForPanel(id, PanelSide.MID))
}
