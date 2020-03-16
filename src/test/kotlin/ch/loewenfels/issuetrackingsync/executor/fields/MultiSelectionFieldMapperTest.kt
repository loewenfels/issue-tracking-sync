package ch.loewenfels.issuetrackingsync.executor.fields

import ch.loewenfels.issuetrackingsync.AbstractSpringTest
import ch.loewenfels.issuetrackingsync.syncclient.ClientFactory
import ch.loewenfels.issuetrackingsync.syncconfig.FieldMappingDefinition
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired

class MultiSelectionFieldMapperTest : AbstractSpringTest() {
    @Autowired
    private lateinit var clientFactory: ClientFactory

    private val jiraFieldname = "multiSelectCustomFieldJira"
    private val rtcFieldname = "multiSelectCustomFieldRtc"
    private val associations =
        mutableMapOf(
            "fooRtc" to "fooJira",
            "barRtc" to "barJira"
        )
    private val additionalAssociations =
        mutableMapOf(
            "fobarRtc" to "fooJira",
            "barRtc" to "barJira"
        )


    @Test
    fun getValue() {
        // arrange
        val testee = buildTestee(associations)
        val issue = TestObjects.buildIssue("MK-1")
        val sourceClient =
            TestObjects.buildIssueTrackingClient(TestObjects.buildIssueTrackingApplication("RtcClient"), clientFactory)
        // act
        val result = testee.getValue(issue, rtcFieldname, sourceClient)
        // assert
        assertNotNull(result)
        assertEquals(
            arrayListOf("fooRtc", "barRtc"),
            (result as ArrayList<*>)
        )
    }

    @Test
    fun setValue() {
        // arrange
        val testee = buildTestee(associations)
        val issue = TestObjects.buildIssue("MK-1")
        val targetClient =
            TestObjects.buildIssueTrackingClient(TestObjects.buildIssueTrackingApplication("JiraClient"), clientFactory)
        val value = arrayListOf("fooRtc", "barRtc")
        // act
        testee.setValue(issue, jiraFieldname, issue, targetClient, value)
        // assert
        verify(targetClient).setValue(issue, issue, jiraFieldname, arrayListOf("fooJira", "barJira"))
    }

    @Test
    fun setValue_oneKnownAndOneUnknownValue_onlyKnownValueSet() {
        // arrange
        val testee = buildTestee(associations)
        val issue = TestObjects.buildIssue("MK-1")
        val targetClient =
            TestObjects.buildIssueTrackingClient(TestObjects.buildIssueTrackingApplication("JiraClient"), clientFactory)
        val value = arrayListOf("fooRtc", "foobar")
        // act
        testee.setValue(issue, jiraFieldname, issue, targetClient, value)
        // assert
        verify(targetClient).setValue(issue, issue, jiraFieldname, arrayListOf("fooJira"))
    }

    @Test
    fun setValue_oneUnknownValue_noValueSet() {
        // arrange
        val testee = buildTestee(associations)
        val issue = TestObjects.buildIssue("MK-1")
        val targetClient =
            TestObjects.buildIssueTrackingClient(TestObjects.buildIssueTrackingApplication("JiraClient"), clientFactory)
        val value = arrayListOf("foobar")
        // act
        testee.setValue(issue, jiraFieldname, issue, targetClient, value)
        // assert
        verify(targetClient).setValue(issue, issue, jiraFieldname, arrayListOf<String>())
    }

    @Test
    fun setValue_additionalAssociationsWithDuplicatedKeys_onlyKnownValueSet() {
        // arrange
        val testee = buildTestee(associations, additionalAssociations)
        val issue = TestObjects.buildIssue("MK-1")
        val targetClient =
            TestObjects.buildIssueTrackingClient(TestObjects.buildIssueTrackingApplication("JiraClient"), clientFactory)
        val value = arrayListOf("fobarRtc")
        // act
        testee.setValue(issue, jiraFieldname, issue, targetClient, value)
        // assert
        verify(targetClient).setValue(issue, issue, jiraFieldname, arrayListOf("fooJira"))
    }

    private fun buildTestee(
        associations: MutableMap<String, String>,
        additionalAssociations: MutableMap<String, String> = mutableMapOf()
    ): MultiSelectionFieldMapper {
        val fieldDefinition = FieldMappingDefinition(
            rtcFieldname, jiraFieldname,
            MultiSelectionFieldMapper::class.toString(), associations, additionalAssociations
        )
        return MultiSelectionFieldMapper(fieldDefinition)
    }
}