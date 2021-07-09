package ch.loewenfels.issuetrackingsync.executor.fields

import ch.loewenfels.issuetrackingsync.AbstractSpringTest
import ch.loewenfels.issuetrackingsync.safeEq
import ch.loewenfels.issuetrackingsync.syncclient.ClientFactory
import ch.loewenfels.issuetrackingsync.syncconfig.FieldMappingDefinition
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildIssue
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildIssueTrackingApplication
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildIssueTrackingClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired

class MultiSelectionFieldMapperTest : AbstractSpringTest() {
    @Autowired
    private lateinit var clientFactory: ClientFactory

    private val jiraFieldname = "multiSelectCustomFieldJira"
    private val rtcFieldname = "multiSelectCustomFieldRtc"

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
        assertEquals(
            arrayListOf("fooJira", "barJira"),
            (result as ArrayList<*>)
        )
    }

    @Test
    fun setValue() {
        // arrange
        val testee = buildTestee()
        val issue = buildIssue("MK-1")
        val targetClient = buildIssueTrackingClient(buildIssueTrackingApplication("JiraClient"), clientFactory)
        val sourceClient = buildIssueTrackingClient(buildIssueTrackingApplication("RtcClient"), clientFactory)
        val value = testee.getValue(issue, rtcFieldname, sourceClient)
        // act
        testee.setValue(issue, jiraFieldname, issue, targetClient, value)
        // assert
        verify(targetClient).setValue(issue, issue, jiraFieldname, arrayListOf("fooJira", "barJira"))
    }

    @Test
    fun getValue_oneKnownAndOneUnknownValue_onlyKnownValueSet() {
        // arrange
        val testee = buildTestee()
        val issue = buildIssue("MK-1")
        val sourceClient = buildIssueTrackingClient(buildIssueTrackingApplication("RtcClient"), clientFactory)
        `when`(sourceClient.getMultiSelectValues(safeEq(issue), safeEq(rtcFieldname))).thenReturn(
            listOf(
                "fooRtc",
                "foobar"
            )
        )
        // act
        val result = testee.getValue(issue, rtcFieldname, sourceClient)
        // assert
        assertEquals(
            arrayListOf("fooJira"),
            (result as ArrayList<*>)
        )
    }

    @Test
    fun getValue_oneUnknownValue_noValueReturned() {
        // arrange
        val testee = buildTestee()
        val issue = buildIssue("MK-1")
        val sourceClient = buildIssueTrackingClient(buildIssueTrackingApplication("RtcClient"), clientFactory)
        `when`(sourceClient.getMultiSelectValues(safeEq(issue), safeEq(rtcFieldname))).thenReturn(listOf("foobar"))
        // act
        val result = testee.getValue(issue, rtcFieldname, sourceClient)
        // assert
        assertEquals(
            emptyList<String>(),
            (result as ArrayList<*>)
        )
    }

    @Test
    fun getValue_oneUnknownValueWithWildcardConfig_valueShouldBeReturned() {
        // arrange
        val testee = buildTestee("*" to "*")
        val issue = buildIssue("MK-1")
        val sourceClient = buildIssueTrackingClient(buildIssueTrackingApplication("RtcClient"), clientFactory)
        `when`(sourceClient.getMultiSelectValues(safeEq(issue), safeEq(rtcFieldname))).thenReturn(listOf("foobar"))
        // act
        val result = testee.getValue(issue, rtcFieldname, sourceClient)
        // assert
        assertEquals(
            listOf("foobar"),
            (result as ArrayList<*>)
        )
    }

    private fun buildTestee(additionalPair: Pair<String, String>? = null): MultiSelectionFieldMapper {
        val associations =
            mutableMapOf(
                "fooRtc" to "fooJira",
                "barRtc" to "barJira"
            )
        additionalPair?.let { associations[it.first] = it.second }
        val fieldDefinition = FieldMappingDefinition(
            rtcFieldname, jiraFieldname,
            MultiSelectionFieldMapper::class.toString(),
            associations = associations
        )
        return MultiSelectionFieldMapper(fieldDefinition)
    }
}