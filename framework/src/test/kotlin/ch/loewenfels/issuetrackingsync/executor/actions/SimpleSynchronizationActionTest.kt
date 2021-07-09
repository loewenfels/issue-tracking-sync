package ch.loewenfels.issuetrackingsync.executor.actions

import ch.loewenfels.issuetrackingsync.AbstractSpringTest
import ch.loewenfels.issuetrackingsync.syncclient.ClientFactory
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildFieldMappingList
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildIssueTrackingApplication
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildIssueTrackingClient
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired

internal class SimpleSynchronizationActionTest : AbstractSpringTest() {
    @Autowired
    private lateinit var clientFactory: ClientFactory

    @Test
    fun execute() {
        // arrange
        val sourceClient = buildIssueTrackingClient(buildIssueTrackingApplication("JiraClient"), clientFactory)
        val targetClient = buildIssueTrackingClient(buildIssueTrackingApplication("RtcClient"), clientFactory)
        val fieldMappings = buildFieldMappingList()
        val issue = sourceClient.getIssue("MK-1") ?: throw IllegalArgumentException("Unknown key")
        val testee = SimpleSynchronizationAction("foobar")
        issue.proprietarySourceInstance = issue
        // act
        testee.execute(sourceClient, targetClient, issue, fieldMappings, null)
        // assert
        assertNotNull(issue.proprietarySourceInstance)
        verify(targetClient).createOrUpdateTargetIssue(issue, null)
    }
}