package de.simpletactics.wiki.adapter.controller

import de.simpletactics.wiki.api.WikiEntriesApi
import de.simpletactics.wiki.api.dto.DeletedResponse
import de.simpletactics.wiki.api.dto.EntryResponse
import de.simpletactics.wiki.api.dto.UpdateEntryRequest
import de.simpletactics.wiki.model.WikiNotFoundException
import de.simpletactics.wiki.services.WikiService
import de.simpletactics.wiki.services.port.WikiPort
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Wiki entries")
@RestController
class WikiEntryController(
    private val wikiPort: WikiPort,
    private val wikiService: WikiService,
) : WikiEntriesApi {

    override fun getEntry(id: Int): ResponseEntity<EntryResponse> {
        val entry = wikiPort.getEntry(id)
            ?: throw WikiNotFoundException("No entry found with id $id")

        return ResponseEntity.ok(
            EntryResponse(
                entry.id,
                entry.headline,
                entry.htmlEntry,
                entry.author,
                entry.createdAt,
                entry.updatedAt,
            )
        )
    }

    override fun updateEntry(
        id: Int,
        updateEntryRequest: UpdateEntryRequest,
    ): ResponseEntity<EntryResponse> {
        wikiPort.updateEntry(id, updateEntryRequest.headline, updateEntryRequest.content ?: "")
        return getEntry(id)
    }

    override fun deleteEntry(id: Int): ResponseEntity<DeletedResponse> =
        ResponseEntity.ok(DeletedResponse(id, wikiService.deleteWikiEntry(id)))
}
