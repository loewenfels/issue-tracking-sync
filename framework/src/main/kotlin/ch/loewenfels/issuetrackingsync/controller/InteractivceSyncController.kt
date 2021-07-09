package ch.loewenfels.issuetrackingsync.controller

import ch.loewenfels.issuetrackingsync.HTTP_PARAMNAME_ISSUEKEY
import ch.loewenfels.issuetrackingsync.HTTP_PARAMNAME_RESPONSEMESSAGE
import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.app.AppState
import ch.loewenfels.issuetrackingsync.app.SyncApplicationProperties
import ch.loewenfels.issuetrackingsync.scheduling.SyncRequestProducer
import ch.loewenfels.issuetrackingsync.syncclient.ClientFactory
import ch.loewenfels.issuetrackingsync.syncconfig.IssueTrackingApplication
import ch.loewenfels.issuetrackingsync.syncconfig.Settings
import org.springframework.scheduling.support.CronSequenceGenerator
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@RestController
class InteractivceSyncController(
    private val syncRequestProducer: SyncRequestProducer,
    private val clientFactory: ClientFactory,
    private val settings: Settings,
    private val syncApplicationProperties: SyncApplicationProperties,
    private val appState: AppState
) {

    @GetMapping("/systeminfo")
    fun getSystemInfo(): Map<String, Any?> {
        return mapOf(
            "title" to syncApplicationProperties.title,
            "debug" to syncApplicationProperties.debug
        )
    }

    @GetMapping("/info")
    fun getSettings(): Map<String, String> {
        val dateFormat = "dd. MMM. yyyy HH:mm:ss"
        val cronTrigger = CronSequenceGenerator(syncApplicationProperties.pollingCron)
        val nextPollingDate = cronTrigger.next(Date())
        return mapOf(
            "Next run" to SimpleDateFormat(dateFormat).format(nextPollingDate),
            "Process issues updated after" to DateTimeFormatter.ofPattern(dateFormat)
                .format(appState.lastPollingTimestamp)
        )
    }

    @PutMapping("/manualsync")
    fun manualSync(@RequestBody body: Map<String, String>): Map<String, String> {
        val trackingApplications = settings.trackingApplications
        val issueKey = body.getValue(HTTP_PARAMNAME_ISSUEKEY)
        val trackingApplication = trackingApplications.map { it to retrieveIssue(issueKey, it) }
            .sortedByDescending { (_, issue) -> issue?.lastUpdated }
            .first()
            .first
        val resultMessage = loadAndQueueIssue(issueKey, trackingApplication)
        return mapOf(HTTP_PARAMNAME_RESPONSEMESSAGE to resultMessage)
    }

    @PutMapping("/earliestSyncDate")
    fun updateStartDateTime(@RequestBody body: Map<String, String>): Map<String, String> {
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyyHH:mm")
        val date = body.getValue("date")
        val time = body.getValue("time")

        try {
            val dateTime = LocalDateTime.parse(date + time, formatter)
            appState.lastPollingTimestamp = dateTime
        } catch (e: Exception) {
            return mapOf(HTTP_PARAMNAME_RESPONSEMESSAGE to "Could not update earliest sync date")
        }
        return mapOf(HTTP_PARAMNAME_RESPONSEMESSAGE to "Earliest sync date successfully updated")
    }

    private fun loadAndQueueIssue(key: String, trackingApplication: IssueTrackingApplication): String {
        return retrieveIssue(key, trackingApplication)?.let {
            syncRequestProducer.queue(it)
            "Issue $key has been queued for sync"
        } ?: "Failed to locate issue with key $key in ${trackingApplication.name}"
    }

    private fun retrieveIssue(key: String, trackingApplication: IssueTrackingApplication): Issue? {
        return try {
            clientFactory.getClient(trackingApplication).use { client -> client.getIssue(key) }
        } catch (ex: Exception) {
            when (ex.javaClass.simpleName) {
                "RestClientException",
                "NumberFormatException" -> null
                else -> throw ex
            }
        }
    }

    @GetMapping("/definedSystems")
    fun definedSystems(): List<String> =
        settings.trackingApplications.map { app -> app.name }.toList()
}

