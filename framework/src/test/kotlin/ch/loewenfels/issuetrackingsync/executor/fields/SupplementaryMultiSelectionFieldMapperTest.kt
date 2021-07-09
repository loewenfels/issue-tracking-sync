package ch.loewenfels.issuetrackingsync.executor.fields

import ch.loewenfels.issuetrackingsync.AbstractSpringTest
import ch.loewenfels.issuetrackingsync.syncclient.ClientFactory
import ch.loewenfels.issuetrackingsync.syncconfig.FieldMappingDefinition
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildIssue
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildIssueTrackingApplication
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildIssueTrackingClient
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired

internal class SupplementaryMultiSelectionFieldMapperTest : AbstractSpringTest() {
    @Autowired
    private lateinit var clientFactory: ClientFactory

    private val jiraFieldname = "multiSelectCustomFieldJira"
    private val rtcFieldname = "multiSelectCustomFieldRtc"

    @Test
    fun setValue() {
        // arrange
        val testee = buildTestee()
        val issue = buildIssue("MK-1")
        issue.proprietaryTargetInstance = issue
        val targetClient = buildIssueTrackingClient(buildIssueTrackingApplication("JiraClient"), clientFactory)
        val value = listOf("congress")
        // act
        testee.setValue(issue, jiraFieldname, issue, targetClient, value)
        // assert
        verify(targetClient)
            .setValue(issue, issue, jiraFieldname, arrayListOf("fooJira", "barJira", "congress"))
    }

    private fun buildTestee(): SupplementaryMultiSelectionFieldMapper = SupplementaryMultiSelectionFieldMapper(
        FieldMappingDefinition(rtcFieldname, jiraFieldname)
    )

}