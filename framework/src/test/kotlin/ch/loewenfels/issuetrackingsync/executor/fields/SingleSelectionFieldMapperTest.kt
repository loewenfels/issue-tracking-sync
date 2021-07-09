package ch.loewenfels.issuetrackingsync.executor.fields

import ch.loewenfels.issuetrackingsync.AbstractSpringTest
import ch.loewenfels.issuetrackingsync.syncclient.ClientFactory
import ch.loewenfels.issuetrackingsync.syncconfig.FieldMappingDefinition
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildIssue
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildIssueTrackingApplication
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildIssueTrackingClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired

class SingleSelectionFieldMapperTest : AbstractSpringTest() {
    @Autowired
    private lateinit var clientFactory: ClientFactory

    private val jiraFieldname = "singleSelectCustomFieldJira"
    private val rtcFieldname = "singleSelectCustomFieldRtc"

    @Test
    fun getValue() {
        // arrange
        val testee = buildTestee()
        val issue = buildIssue("MK-1")
        val sourceClient = buildIssueTrackingClient(buildIssueTrackingApplication("RtcClient"), clientFactory)
        // act
        val result = testee.getValue(issue, rtcFieldname, sourceClient)
        // assert
        assertNotNull(result)
        assertEquals("fooRtc", result)
    }

    @Test
    fun setValue() {
        // arrange
        val testee = buildTestee()
        val issue = buildIssue("MK-1")
        val targetClient = buildIssueTrackingClient(buildIssueTrackingApplication("JiraClient"), clientFactory)
        val value = "fooRtc"
        // act
        testee.setValue(issue, jiraFieldname, issue, targetClient, value)
        // assert
        verify(targetClient).setValue(issue, issue, jiraFieldname, "fooJira")
    }

    @Test
    internal fun setValue_nullValue_noInteraction() {
        // arrange
        val testee = buildTestee()
        val issue = buildIssue("MK-1")
        val targetClient = buildIssueTrackingClient(buildIssueTrackingApplication("JiraClient"), clientFactory)
        // act
        testee.setValue(issue, jiraFieldname, issue, targetClient, null)
        // assert
        verifyNoInteractions(targetClient)
    }

    private fun buildTestee(): SingleSelectionFieldMapper {

        val fieldDefinition = FieldMappingDefinition(
            rtcFieldname, jiraFieldname,
            SingleSelectionFieldMapper::class.toString(),
            associations = mutableMapOf(
                "fooRtc" to "fooJira",
                "barRtc" to "barJira"
            )
        )
        return SingleSelectionFieldMapper(fieldDefinition)
    }
}