package ch.loewenfels.issuetrackingsync.executor.actions

import ch.loewenfels.issuetrackingsync.AbstractSpringTest
import ch.loewenfels.issuetrackingsync.Attachment
import ch.loewenfels.issuetrackingsync.any
import ch.loewenfels.issuetrackingsync.safeEq
import ch.loewenfels.issuetrackingsync.syncclient.ClientFactory
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildFieldMappingList
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildIssueTrackingApplication
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildIssueTrackingClient
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired

internal class AttachmentsSynchronizationActionTest : AbstractSpringTest() {
    @Autowired
    private lateinit var clientFactory: ClientFactory

    @Test
    fun execute() {
        // arrange
        val sourceClient = buildIssueTrackingClient(buildIssueTrackingApplication("JiraClient"), clientFactory)
        val targetClient = buildIssueTrackingClient(buildIssueTrackingApplication("RtcClient"), clientFactory)
        val fieldMappings = buildFieldMappingList()
        val issue = sourceClient.getIssue("MK-1") ?: throw IllegalArgumentException("Unknown key")
        val targetIssue = targetClient.getIssue("1234") ?: throw IllegalArgumentException("Unknown key")
        issue.proprietarySourceInstance = issue
        issue.proprietaryTargetInstance = targetIssue
        val testee = AttachmentsSynchronizationAction()
        // act
        testee.execute(sourceClient, targetClient, issue, fieldMappings, null)
        // assert
        verify(targetClient).addAttachment(safeEq(targetIssue), any(Attachment::class.java))
    }
}