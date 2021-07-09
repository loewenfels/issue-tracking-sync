package ch.loewenfels.issuetrackingsync.executor

import ch.loewenfels.issuetrackingsync.AbstractSpringTest
import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.any
import ch.loewenfels.issuetrackingsync.executor.actions.SynchronizationAction
import ch.loewenfels.issuetrackingsync.notification.NotificationChannel
import ch.loewenfels.issuetrackingsync.notification.NotificationObserver
import ch.loewenfels.issuetrackingsync.syncclient.ClientFactory
import ch.loewenfels.issuetrackingsync.syncconfig.DefaultsForNewIssue
import ch.loewenfels.issuetrackingsync.testcontext.AlwaysFalseIssueFilter
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildIssue
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildIssueTrackingApplication
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildIssueTrackingClient
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildSyncActionDefinition
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildSyncFlowDefinition
import org.hamcrest.CoreMatchers.hasItem
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus

internal class SynchronizationFlowTest : AbstractSpringTest() {
    @Autowired
    private lateinit var clientFactory: ClientFactory

    companion object TestNotificationChannel : NotificationChannel {
        val successfulIssueKeys: MutableList<String> = mutableListOf()
        val erroneousIssueKeys: MutableList<String> = mutableListOf()
        val erroneousMessages: MutableList<String> = mutableListOf()
        override fun onSuccessfulSync(
            issue: Issue,
            syncActions: Map<SyncActionName, SynchronizationAction>
        ) {
            successfulIssueKeys.add(issue.key)
        }

        override fun onException(
            issue: Issue,
            ex: Exception,
            syncActions: Map<SyncActionName, SynchronizationAction>
        ) {
            erroneousIssueKeys.add(issue.key)
            ex.message?.let { erroneousMessages.add(it) }
        }
    }

    @Test
    fun applies_validClientName_true() {
        // arrange
        val syncFlowDefinition = buildSyncFlowDefinition("JIRACLIENT", "RTCCLIENT")
        val actionDefinitions = listOf(buildSyncActionDefinition())
        val sourceClient = buildIssueTrackingClient(buildIssueTrackingApplication("JiraClient"), clientFactory)
        val targetClient = buildIssueTrackingClient(buildIssueTrackingApplication("RtcClient"), clientFactory)
        val notificationObserver = buildNotificationObserver()
        val testee =
            SynchronizationFlow(syncFlowDefinition, actionDefinitions, sourceClient, targetClient, notificationObserver)
        val issue = buildIssue()
        // act
        val result = testee.use { flow -> flow.applies(syncFlowDefinition.source, issue) }
        // assert
        assertTrue(result)
    }

    @Test
    fun applies_validClientNameFilterNotMatched_false() {
        // arrange
        val syncFlowDefinition = buildSyncFlowDefinition("JIRACLIENT", "RTCCLIENT")
        val actionDefinitions = listOf(buildSyncActionDefinition())
        syncFlowDefinition.filterClassname = AlwaysFalseIssueFilter::class.qualifiedName ?: ""
        val sourceClient = buildIssueTrackingClient(buildIssueTrackingApplication("JiraClient"), clientFactory)
        val targetClient = buildIssueTrackingClient(buildIssueTrackingApplication("RtcClient"), clientFactory)
        val notificationObserver = buildNotificationObserver()
        val testee =
            SynchronizationFlow(syncFlowDefinition, actionDefinitions, sourceClient, targetClient, notificationObserver)
        val issue = buildIssue()
        // act
        val result = testee.use { flow -> flow.applies(syncFlowDefinition.source, issue) }
        // assert
        assertFalse(result)
    }

    @Test
    fun applies_invalidClientName_false() {
        // arrange
        val syncFlowDefinition = buildSyncFlowDefinition("JIRACLIENT", "RTCCLIENT")
        val actionDefinitions = listOf(buildSyncActionDefinition())
        val sourceClient = buildIssueTrackingClient(buildIssueTrackingApplication("JiraClient"), clientFactory)
        val targetClient = buildIssueTrackingClient(buildIssueTrackingApplication("RtcClient"), clientFactory)
        val notificationObserver = buildNotificationObserver()
        val testee =
            SynchronizationFlow(syncFlowDefinition, actionDefinitions, sourceClient, targetClient, notificationObserver)
        val issue = buildIssue()
        // act
        val result = testee.use { flow -> flow.applies("foobar", issue) }
        // assert
        assertFalse(result)
    }

    @Test
    fun execute_issueUpdatedInTheMeantime_notifiedAsError() {
        // arrange
        val syncFlowDefinition = buildSyncFlowDefinition("JIRACLIENT", "RTCCLIENT")
        val actionDefinitions = listOf(buildSyncActionDefinition())
        val sourceClient = buildIssueTrackingClient(buildIssueTrackingApplication("JiraClient"), clientFactory)
        val targetClient = buildIssueTrackingClient(buildIssueTrackingApplication("RtcClient"), clientFactory)
        val notificationObserver = buildNotificationObserver()
        val testee =
            SynchronizationFlow(syncFlowDefinition, actionDefinitions, sourceClient, targetClient, notificationObserver)
        val issue = buildIssue("MK-1")
        // act
        testee.use { flow -> flow.execute(issue) }
        // assert
        assertEquals(1, erroneousIssueKeys.distinct().size)
    }


    @Test
    fun execute_jiraLoginFailureSourceIsRtc_notifiedAsError() {
        // arrange
        val error = HttpStatus.UNAUTHORIZED
        val exception = IllegalStateException(error.value().toString(), Throwable(error.reasonPhrase))
        val syncFlowDefinition = buildSyncFlowDefinition("RTCCLIENT", "JIRACLIENT")
        val actionDefinitions = listOf(buildSyncActionDefinition())
        val sourceClient = buildIssueTrackingClient(buildIssueTrackingApplication("RtcClient"), clientFactory)
        val targetClient = buildIssueTrackingClient(buildIssueTrackingApplication("JiraClient"), clientFactory)
        val notificationObserver = buildNotificationObserver()
        val testee =
            SynchronizationFlow(syncFlowDefinition, actionDefinitions, sourceClient, targetClient, notificationObserver)
        val issue = buildIssue("MK-1")
        doAnswer { throw exception }.`when`(sourceClient).getProprietaryIssue(anyString())
        // act
        testee.use { flow -> flow.execute(issue) }
        // assert
        assertThat(erroneousMessages, hasItem(error.reasonPhrase))
    }

    @Test
    fun execute_rtcLoginFailureSourceIsJira_notifiedAsError() {
        // arrange
        val error = HttpStatus.UNAUTHORIZED
        val exception = IllegalStateException(error.value().toString(), Throwable(error.reasonPhrase))
        val syncFlowDefinition = buildSyncFlowDefinition("JIRACLIENT", "RTCCLIENT")
        val actionDefinitions = listOf(buildSyncActionDefinition())
        val sourceClient = buildIssueTrackingClient(buildIssueTrackingApplication("JiraClient"), clientFactory)
        val targetClient = buildIssueTrackingClient(buildIssueTrackingApplication("RtcClient"), clientFactory)
        val notificationObserver = buildNotificationObserver()
        val testee =
            SynchronizationFlow(syncFlowDefinition, actionDefinitions, sourceClient, targetClient, notificationObserver)
        val issue = buildIssue("MK-1")
        doAnswer { throw exception }.`when`(sourceClient).getProprietaryIssue(anyString())
        // act
        testee.use { flow -> flow.execute(issue) }
        // assert
        assertThat(erroneousMessages, hasItem(error.reasonPhrase))
    }

    @Test
    fun execute_validKey_syncNotifiedSuccessful() {
        // arrange
        val syncFlowDefinition = buildSyncFlowDefinition("JIRACLIENT", "RTCCLIENT")
        val actionDefinitions = listOf(buildSyncActionDefinition())
        val sourceClient = buildIssueTrackingClient(buildIssueTrackingApplication("JiraClient"), clientFactory)
        val targetClient = buildIssueTrackingClient(buildIssueTrackingApplication("RtcClient"), clientFactory)
        val notificationObserver = buildNotificationObserver()
        val testee =
            SynchronizationFlow(syncFlowDefinition, actionDefinitions, sourceClient, targetClient, notificationObserver)
        val issue = sourceClient.getIssue("MK-1") ?: throw IllegalArgumentException("Unknown key")
        // act
        testee.use { flow -> flow.execute(issue) }
        // assert
        assertEquals(1, successfulIssueKeys.size)
        verify(sourceClient, times(1))
            .createOrUpdateTargetIssue(any(Issue::class.java), any(DefaultsForNewIssue::class.java))
        verify(targetClient, times(2))
            .createOrUpdateTargetIssue(any(Issue::class.java), any(DefaultsForNewIssue::class.java))
    }

    private fun buildNotificationObserver(): NotificationObserver {
        val observer = NotificationObserver()
        observer.addChannel(SynchronizationFlowTest)
        return observer
    }

}

