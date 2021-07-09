package ch.loewenfels.issuetrackingsync.executor.fields

import ch.loewenfels.issuetrackingsync.AbstractSpringTest
import ch.loewenfels.issuetrackingsync.syncclient.ClientFactory
import ch.loewenfels.issuetrackingsync.syncconfig.FieldMappingDefinition
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildIssue
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildIssueTrackingApplication
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildIssueTrackingClient
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired

@Suppress("UNCHECKED_CAST")
internal class PriorityAndSeverityFieldMapperTest : AbstractSpringTest() {
    @Autowired
    private lateinit var clientFactory: ClientFactory

    @Test
    fun getValue() {
        // arrange
        val testee = buildTestee()
        val issue = buildIssue("MK-1")
        val sourceClient = buildIssueTrackingClient(buildIssueTrackingApplication("RtcClient"), clientFactory)
        // act
        val result = testee.getValue(issue, "priority,severity", sourceClient)
        // assert
        assertNotNull(result)
        assertTrue(result is Pair<*, *>)
        assertEquals(
            "com.ibm.team.workitem.common.model.IPriority:priority.literal.I12",
            (result as Pair<String, String>).first
        )
        assertEquals(
            "com.ibm.team.workitem.common.model.ISeverity:severity.s2",
            result.second
        )
    }

    @Test
    fun setValue() {
        // arrange
        val testee = buildTestee()
        val issue = buildIssue("MK-1")
        val targetClient = buildIssueTrackingClient(buildIssueTrackingApplication("JiraClient"), clientFactory)
        val value = "com.ibm.team.workitem.common.model.IPriority:priority.literal.I12" to
                "com.ibm.team.workitem.common.model.ISeverity:severity.s2"

        // act
        testee.setValue(issue, "priorityId", issue, targetClient, value)
        // assert
        verify(targetClient).setValue(issue, issue, "priorityId", "Hoch")
    }

    @Test
    fun setValue_fallback() {
        // arrange
        val testee = buildTestee()
        val issue = buildIssue("MK-1")
        val targetClient = buildIssueTrackingClient(buildIssueTrackingApplication("JiraClient"), clientFactory)
        val value = "weird_value" to "another_weird_value"

        // act
        testee.setValue(issue, "priorityId", issue, targetClient, value)
        // assert
        verify(targetClient).setValue(issue, issue, "priorityId", "Normal")
    }

    private fun buildTestee(): PriorityAndSeverityFieldMapper {
        val iPriority = "com.ibm.team.workitem.common.model.IPriority:priority.literal.I12"
        val iSeverity = "com.ibm.team.workitem.common.model.ISeverity:severity.s2"
        val fieldDefinition = FieldMappingDefinition(
            "priority,severity", "priorityId",
            PriorityAndSeverityFieldMapper::class.toString(),
            associations = mutableMapOf(
                "$iPriority,$iSeverity" to "Hoch",
                "Hoch" to "$iPriority,$iSeverity",
                "*,*" to "Normal"
            )
        )
        return PriorityAndSeverityFieldMapper(fieldDefinition)
    }
}