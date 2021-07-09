package ch.loewenfels.issuetrackingsync.notification

import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.app.NotificationChannelProperties
import ch.loewenfels.issuetrackingsync.executor.SyncActionName
import ch.loewenfels.issuetrackingsync.executor.actions.SimpleSynchronizationAction
import ch.loewenfels.issuetrackingsync.executor.actions.SynchronizationAction
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import java.time.LocalDateTime

internal class CsvProtocolTest {
    @Test
    fun onSuccessfulSync_writeToCsvSuccess_noException() {
        // arrange
        val properties = defaultProperties()
        val testee = CsvProtocol(properties)
        val syncActions = createSyncActions()
        val issue = Issue("MK-1", "JIRA", LocalDateTime.now())
        // act
        testee.onSuccessfulSync(issue, syncActions)
        // assert
        assertCsvEntry(issue, properties)
    }

    private fun defaultProperties(): NotificationChannelProperties {
        val properties = NotificationChannelProperties()
        properties.classname = "ch.loewenfels.issuetrackingsync.notification.CsvProtocol"
        properties.endpoint = File.createTempFile("protocol", ".csv").absolutePath
        return properties
    }

    private fun createSyncActions(): Map<SyncActionName, SynchronizationAction> =
        mapOf<SyncActionName, SynchronizationAction>(
            "SynchronizeTimeJiraToRtc" to SimpleSynchronizationAction("CSV")
        )

    private fun assertCsvEntry(issue: Issue, properties: NotificationChannelProperties) {
        val file = File(properties.endpoint)
        val lastLine = file.readLines()
            .findLast { line -> line.contains(issue.key) }
        assertThat(lastLine, containsString("SynchronizeTimeJiraToRtc"))
    }
}
