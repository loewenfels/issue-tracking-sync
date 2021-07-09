package ch.loewenfels.issuetrackingsync.scheduling

import ch.loewenfels.issuetrackingsync.INTERNAL_QUEUE_NAME
import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.Logging
import ch.loewenfels.issuetrackingsync.logger
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.jms.core.JmsTemplate
import org.springframework.stereotype.Component

@Component
class SyncRequestProducer(
    private val jmsTemplate: JmsTemplate,
    private val objectMapper: ObjectMapper
) : Logging {
    fun queue(issue: Issue) {
        logger().debug("Queueing {}", issue)
        val syncRequestAsJson = objectMapper.writeValueAsString(
            SyncRequest(
                issue.key,
                issue.clientSourceName,
                issue.lastUpdated
            )
        )
        jmsTemplate.send(INTERNAL_QUEUE_NAME) { session ->
            session.createTextMessage(syncRequestAsJson)
        }
    }
}