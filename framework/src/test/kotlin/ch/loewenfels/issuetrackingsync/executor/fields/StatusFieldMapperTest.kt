package ch.loewenfels.issuetrackingsync.executor.fields

import ch.loewenfels.issuetrackingsync.AbstractSpringTest
import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.StateHistory
import ch.loewenfels.issuetrackingsync.safeEq
import ch.loewenfels.issuetrackingsync.syncclient.ClientFactory
import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient
import ch.loewenfels.issuetrackingsync.syncconfig.FieldMappingDefinition
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildIssue
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildIssueTrackingApplication
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildIssueTrackingClient
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

class StatusFieldMapperTest : AbstractSpringTest() {
    @Autowired
    private lateinit var clientFactory: ClientFactory
    private val jiraFieldname = "statusFieldJira"
    private val rtcFieldname = "statusFieldRtc"
    private lateinit var sourceClient: IssueTrackingClient<Any>
    private lateinit var targetClient: IssueTrackingClient<Any>
    private lateinit var sourceIssue: Issue
    private lateinit var targetIssue: Issue

    @BeforeEach
    fun initClients() {
        this.sourceClient = buildIssueTrackingClient(buildIssueTrackingApplication("RtcClient"), clientFactory)
        this.targetClient = buildIssueTrackingClient(buildIssueTrackingApplication("JiraClient"), clientFactory)
        this.sourceIssue = sourceClient.getIssue("1234") ?: throw IllegalArgumentException("Unknown key")
        this.targetIssue = targetClient.getIssue("MK-1") ?: throw IllegalArgumentException("Unknown key")
        sourceIssue.proprietarySourceInstance = this.sourceIssue
        sourceIssue.proprietaryTargetInstance = this.targetIssue
    }

    @Test
    fun getValue() {
        // arrange
        val testee = buildTestee()
        val issue = buildIssue("MK-1")
        // act
        val result = testee.getValue(issue, rtcFieldname, sourceClient)
        // assert
        assertNotNull(result)
        assertTrue(result is Pair<*, *>)
        assertEquals("In Umsetzung", (result as Pair<*, *>).first)
        assertTrue(result.second is List<*>)
    }

    @Test
    fun setValue_targetIssueAlreadyInSameState_noOp() {
        // arrange
        `when`(targetClient.getState(safeEq(targetIssue))).thenReturn("In Work")
        val targetValue = "In Umsetzung" to listOf(
            StateHistory(LocalDateTime.now().minusHours(5), "Neu", "In Abklärung"),
            StateHistory(LocalDateTime.now().minusHours(3), "In Abklärung", "Bereit zur Umsetzung"),
            StateHistory(LocalDateTime.now(), "Bereit zur Umsetzung", "In Umsetzung")
        )
        val testee = buildTestee()
        // act
        testee.setValue(targetIssue, jiraFieldname, sourceIssue, targetClient, targetValue)
        // assert
        verify(targetClient, Mockito.never()).setState(safeEq(targetIssue), Mockito.anyString())
    }

    @Test
    fun setValue_targetIssueStateBehind_updatedState() {
        // arrange
        `when`(targetClient.getState(safeEq(targetIssue))).thenReturn("Open")
        val targetValue = "In Umsetzung" to listOf(
            StateHistory(LocalDateTime.now().minusHours(5), "Neu", "In Abklärung"),
            StateHistory(LocalDateTime.now().minusHours(3), "In Abklärung", "Bereit zur Umsetzung"),
            StateHistory(LocalDateTime.now(), "Bereit zur Umsetzung", "In Umsetzung")
        )

        val testee = buildTestee()
        // act
        testee.setValue(targetIssue, jiraFieldname, sourceIssue, targetClient, targetValue)
        // assert
        verify(targetClient).setState(targetIssue, "Authorized")
        verify(targetClient).setState(targetIssue, "In Work")
    }

    @Test
    fun setValue_sourceIssueHasStateJumps_updatedState() {
        // arrange
        `when`(targetClient.getState(safeEq(targetIssue))).thenReturn("In Work")
        val targetValue = "In Abnahme" to listOf(
            StateHistory(LocalDateTime.now().minusHours(5), "Neu", "In Abklärung"),
            StateHistory(LocalDateTime.now().minusHours(4), "In Abklärung", "Bereit zur Umsetzung"),
            StateHistory(LocalDateTime.now().minusHours(3), "Bereit zur Umsetzung", "In Umsetzung"),
            StateHistory(LocalDateTime.now().minusHours(2), "In Umsetzung", "Bereit zur Abnahme"),
            StateHistory(LocalDateTime.now(), "Bereit zur Abnahme", "In Abnahme")
        )

        val testee = buildTestee()
        // act
        testee.setValue(targetIssue, jiraFieldname, sourceIssue, targetClient, targetValue)
        // assert
        verify(targetClient).setState(targetIssue, "In Test")
        verify(targetClient).setState(targetIssue, "Resolved")
    }

    @Test
    fun setValue_sourceIssueIsWithinStateJump_updatedState() {
        // arrange
        `when`(targetClient.getState(safeEq(targetIssue))).thenReturn("In Test")
        val targetValue = "In Abnahme" to listOf(
            StateHistory(LocalDateTime.now().minusHours(5), "Neu", "In Abklärung"),
            StateHistory(LocalDateTime.now().minusHours(4), "In Abklärung", "Bereit zur Umsetzung"),
            StateHistory(LocalDateTime.now().minusHours(3), "Bereit zur Umsetzung", "In Umsetzung"),
            StateHistory(LocalDateTime.now().minusHours(2), "In Umsetzung", "Bereit zur Abnahme"),
            StateHistory(LocalDateTime.now(), "Bereit zur Abnahme", "In Abnahme")
        )
        val testee = buildTestee()
        // act
        testee.setValue(targetIssue, jiraFieldname, sourceIssue, targetClient, targetValue)
        // assert
        verify(targetClient).setState(targetIssue, "Resolved")
    }

    private fun buildTestee(): StatusFieldMapper = StatusFieldMapper(
        FieldMappingDefinition(
            rtcFieldname, jiraFieldname,
            StatusFieldMapper::class.toString(),
            associations = mutableMapOf(
                "Neu" to "Open",
                "In Abklärung" to "Open",
                "Abklärung angehalten" to "Open",
                "Bereit zur Umsetzung" to "Authorized",
                "In Umsetzung" to "In Work",
                "In Umsetzung angehalten" to "Interrupted",
                "Bereit zur Abnahme" to "In Test,Resolved",
                "In Abnahme" to "Resolved",
                "Abnahme angehalten" to "Resolved",
                "Abgeschlossen" to "Closed"
            )
        )
    )
}

