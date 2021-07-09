package ch.loewenfels.issuetrackingsync.scheduling

import ch.loewenfels.issuetrackingsync.INTERNAL_QUEUE_NAME
import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.Logging
import ch.loewenfels.issuetrackingsync.executor.SynchronizationFlowFactory
import ch.loewenfels.issuetrackingsync.logger
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Component
import javax.jms.Message
import javax.jms.TextMessage

@Component
class SyncRequestConsumer @Autowired constructor(
    private val synchronizationFlowFactory: SynchronizationFlowFactory,
    private val objectMapper: ObjectMapper
) : Logging {
    @JmsListener(destination = INTERNAL_QUEUE_NAME)
    fun onMessage(message: Message) {
        if (message is TextMessage) {
            val syncRequest = objectMapper.readValue(message.text, SyncRequest::class.java)
            processSyncRequest(toIssue(syncRequest))
        }
        message.acknowledge()
    }

    private fun toIssue(syncRequest: SyncRequest) =
        Issue(syncRequest.key, syncRequest.clientSourceName, syncRequest.lastUpdated)

    private fun processSyncRequest(issue: Issue) {
        logger().debug("Processing issue {}", issue)
        synchronizationFlowFactory.getSynchronizationFlow(issue.clientSourceName, issue)?.execute(issue)
    }
}