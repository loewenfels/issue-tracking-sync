package ch.loewenfels.issuetrackingsync.controller

import ch.loewenfels.issuetrackingsync.HTTP_PARAMNAME_RESPONSEMESSAGE
import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.scheduling.SyncRequestProducer
import ch.loewenfels.issuetrackingsync.syncclient.ClientFactory
import ch.loewenfels.issuetrackingsync.syncconfig.IssueTrackingApplication
import ch.loewenfels.issuetrackingsync.syncconfig.Settings
import ch.loewenfels.issuetrackingsync.syncconfig.TrackingApplicationName
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * Receive sync requests by webhooks, eg. (https://developer.atlassian.com/server/jira/platform/webhooks/)
 */
@RestController
class WebhookController(
    private val syncRequestProducer: SyncRequestProducer,
    private val clientFactory: ClientFactory,
    private val settings: Settings
) {
    @PostMapping("/webhook/{sourceSystem}")
    fun triggerSyncRequest(@PathVariable sourceSystem: TrackingApplicationName, @RequestBody body: JsonNode): Map<String, String> {
        val trackingApp: IssueTrackingApplication =
            settings.trackingApplications.find { it.name.equals(sourceSystem, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown client $sourceSystem")
        val resultMessage = trackingApp.let { loadAndQueueIssue(body, it) }
        return mapOf(HTTP_PARAMNAME_RESPONSEMESSAGE to resultMessage)
    }

    private fun loadAndQueueIssue(body: JsonNode, trackingApplication: IssueTrackingApplication): String {
        return retrieveIssue(body, trackingApplication).let {
            syncRequestProducer.queue(it)
            "Issue ${it.key} has been queued for sync"
        }
    }

    private fun retrieveIssue(body: JsonNode, trackingApplication: IssueTrackingApplication): Issue =
        clientFactory.getClient(trackingApplication).use { client -> client.getIssueFromWebhookBody(body) }

    @ExceptionHandler(UnsupportedOperationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST, reason = "Invalid webhook call")
    fun onUnsupportedOperationException() {
        // no implementation, response is defined by annotation
    }
}