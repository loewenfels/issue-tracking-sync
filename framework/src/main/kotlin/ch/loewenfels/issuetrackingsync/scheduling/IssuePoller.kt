package ch.loewenfels.issuetrackingsync.scheduling

import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.Logging
import ch.loewenfels.issuetrackingsync.app.AppState
import ch.loewenfels.issuetrackingsync.app.SyncApplicationProperties
import ch.loewenfels.issuetrackingsync.executor.SynchronizationFlowFactory
import ch.loewenfels.issuetrackingsync.logger
import ch.loewenfels.issuetrackingsync.syncclient.ClientFactory
import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient
import ch.loewenfels.issuetrackingsync.syncconfig.IssueTrackingApplication
import ch.loewenfels.issuetrackingsync.syncconfig.Settings
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import javax.annotation.PostConstruct

@Component
class IssuePoller @Autowired constructor(
    private val settings: Settings,
    private val appState: AppState,
    private val syncApplicationProperties: SyncApplicationProperties,
    private val objectMapper: ObjectMapper,
    private val syncRequestProducer: SyncRequestProducer,
    private val clientFactory: ClientFactory,
    private val synchronizationFlowFactory: SynchronizationFlowFactory
) : Logging {
    private val batchSize = 50
    private val issueTrackingAppUsers = settings.trackingApplications.map { it.username }

    @PostConstruct
    fun afterPropertiesSet() {
        logger().info("Polling set for {}", syncApplicationProperties.pollingCron)
        settings.earliestSyncDate?.let {
            // we want to fail hard here as assuming a different "earliest" date might have
            // unwanted effects
            val earliestSyncDate = LocalDateTime.parse(it)
            appState.lastPollingTimestamp = maxOf(earliestSyncDate, appState.lastPollingTimestamp ?: earliestSyncDate)
        }
    }

    @Scheduled(cron = "\${sync.pollingCron:}")
    fun checkForUpdatedIssues() {
        val polledIssues = pollChangedIssuesFromTrackingApps()
        updateLastPollingTimestamp()
        resolveConflicts(polledIssues)
        queueChangedIssues(polledIssues)
    }

    private fun pollChangedIssuesFromTrackingApps(): Map<IssueTrackingApplication, MutableList<Issue>> =
        settings.trackingApplications
            .filter { it.polling }
            .associateWith { trackingApp ->
                logger().info("Checking for issues for {}", trackingApp.name)
                clientFactory.getClient(trackingApp).use { pollIssuesInBatches(it) }
            }

    private fun pollIssuesInBatches(
        issueTrackingClient: IssueTrackingClient<Any>
    ): MutableList<Issue> {
        val allChangedIssues = mutableListOf<Issue>()
        var offset = 0
        do {
            val changedIssues: Collection<Issue> = try {
                val timestamp = appState.lastPollingTimestamp ?: LocalDateTime.now()
                issueTrackingClient.changedIssuesSince(timestamp, batchSize, offset)
                    .filter { it.lastUpdatedBy !in issueTrackingAppUsers }
            } catch (e: Exception) {
                logger().error(
                    "Could not load issues or polling. One common problem could be your " +
                            "authentication or authorisation.\nException was: ${e.message}"
                )
                emptyList()
            }
            allChangedIssues.addAll(changedIssues)
            offset += batchSize
        } while (changedIssues.isNotEmpty())
        return allChangedIssues
    }

    private fun updateLastPollingTimestamp() {
        appState.lastPollingTimestamp = LocalDateTime.now()
        appState.persist(objectMapper)
    }

    private fun resolveConflicts(polledIssues: Map<IssueTrackingApplication, MutableList<Issue>>) {
        val allIssues = polledIssues.values.flatten()
        val sourceKeys = allIssues.map { it.key }
        val allConflictingIssues = allIssues.filter { sourceKeys.contains(it.targetKey) }
        val outdatedIssues = allConflictingIssues.map { issue ->
            val relatedIssue = allConflictingIssues.find { it.key == issue.targetKey }
            listOf(issue, relatedIssue!!).minByOrNull(Issue::lastUpdated)
        }
        polledIssues.values.forEach { issuesPerTrackingApp ->
            issuesPerTrackingApp.removeAll { outdatedIssues.contains(it) }
        }
    }

    private fun queueChangedIssues(polledIssues: Map<IssueTrackingApplication, MutableList<Issue>>) =
        polledIssues.forEach { (trackingApp, issues) ->
            issues.filter { synchronizationFlowFactory.getSynchronizationFlow(trackingApp.name, it) != null }
                .forEach { issue -> syncRequestProducer.queue(issue) }
        }
}