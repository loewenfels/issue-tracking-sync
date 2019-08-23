package ch.loewenfels.issuetrackingsync.scheduled

import ch.loewenfels.issuetrackingsync.Logging
import ch.loewenfels.issuetrackingsync.app.AppState
import ch.loewenfels.issuetrackingsync.client.ClientFactory
import ch.loewenfels.issuetrackingsync.dto.Issue
import ch.loewenfels.issuetrackingsync.logger
import ch.loewenfels.issuetrackingsync.settings.Settings
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import javax.annotation.PostConstruct

@Component
class IssuePoller @Autowired constructor(
    private val settings: Settings,
    private val appState: AppState,
    private val objectMapper: ObjectMapper,
    private val syncRequestProducer: SyncRequestProducer,
    private val clientFactory: ClientFactory
) : Logging {
    @Value("\${sync.pollingCron}")
    lateinit var pollingCronExpression: String;

    @PostConstruct
    fun afterPropertiesSet() {
        logger().info("Polling set for {}", pollingCronExpression)
    }

    @Scheduled(cron = "\${sync.pollingCron:}")
    fun checkForUpdatedIssues() {
        settings.trackingApplications.filter { it.polling }.forEach {
            logger().info("Checking for issues for {}", it.name)
            val issueTrackingClient = clientFactory.getClient(it)
            issueTrackingClient.changedIssuesSince(appState.lastPollingTimestamp ?: LocalDateTime.now())
                .forEach { scheduleSync(it) }
        }
        appState.lastPollingTimestamp = LocalDateTime.now()
        appState.persist(objectMapper)
    }

    private fun scheduleSync(issue: Issue) = syncRequestProducer.queue(issue)
}