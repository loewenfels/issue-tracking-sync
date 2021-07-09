package ch.loewenfels.issuetrackingsync.executor.fields

import ch.loewenfels.issuetrackingsync.AbstractSpringTest
import ch.loewenfels.issuetrackingsync.syncclient.ClientFactory
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildIssue
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildIssueTrackingApplication
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildIssueTrackingClient
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired

internal class LinkToIssueFieldMapperTest : AbstractSpringTest() {
    @Autowired
    private lateinit var clientFactory: ClientFactory

    @Test
    fun setValue() {
        // arrange
        val testee = LinkToIssueFieldMapper()
        val issue = buildIssue("MK-1")
        issue.sourceUrl = "http://localhost/issues/MK-1"
        val targetClient = buildIssueTrackingClient(buildIssueTrackingApplication("JiraClient"), clientFactory)
        // act
        testee.setValue(issue, "My Link", issue, targetClient, "")
        // assert
        verify(targetClient).setValue(issue, issue, "My Link", "http://localhost/issues/MK-1")
    }
}