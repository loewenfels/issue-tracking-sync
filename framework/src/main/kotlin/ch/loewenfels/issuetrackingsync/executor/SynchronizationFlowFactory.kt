package ch.loewenfels.issuetrackingsync.executor

import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.Logging
import ch.loewenfels.issuetrackingsync.logger
import ch.loewenfels.issuetrackingsync.notification.NotificationObserver
import ch.loewenfels.issuetrackingsync.syncclient.ClientFactory
import ch.loewenfels.issuetrackingsync.syncconfig.Settings
import ch.loewenfels.issuetrackingsync.syncconfig.TrackingApplicationName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class SynchronizationFlowFactory @Autowired constructor(
    private val settings: Settings,
    private val clientFactory: ClientFactory,
    private val notificationObserver: NotificationObserver
) : Logging {
    private lateinit var definedFlows: List<SynchronizationFlow>

    @PostConstruct
    fun loadSyncFlows() {
        definedFlows = settings.syncFlowDefinitions.map {
            val sourceClientSettings = settings.toTrackingApplication(it.source)
                ?: throw IllegalArgumentException("No application configured for ${it.source}")
            val targetClientSettings = settings.toTrackingApplication(it.target)
                ?: throw IllegalArgumentException("No application configured for ${it.target}")
            SynchronizationFlow(
                it,
                settings.actionDefinitions,
                clientFactory.getClient(sourceClientSettings),
                clientFactory.getClient(targetClientSettings),
                notificationObserver
            )
        }
    }

    fun getSynchronizationFlow(source: TrackingApplicationName, issue: Issue): SynchronizationFlow? {
        val applicableFlows = definedFlows.filter { it.applies(source, issue) }.toList()
        return when (applicableFlows.size) {
            1 -> applicableFlows.first()
            0 -> {
                logger().info("No applicable flow found for $issue, probably didn't pass filter")
                null
            }
            else -> {
                logger().error("Found multiple flows for issue $issue from $source. Consider defining a filter")
                null
            }
        }
    }
}