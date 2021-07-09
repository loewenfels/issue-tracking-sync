package ch.loewenfels.issuetrackingsync.scheduling

import ch.loewenfels.issuetrackingsync.AbstractSpringTest
import ch.loewenfels.issuetrackingsync.app.AppState
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

internal class IssuePollerTest : AbstractSpringTest() {
    @Autowired
    lateinit var issuePoller: IssuePoller

    @Autowired
    lateinit var appState: AppState

    @Test
    fun checkForUpdatedIssues_validSettings_appStateUpdated() {
        //arrange
        appState.lastPollingTimestamp = LocalDateTime.now().minusDays(1)
        //act
        issuePoller.checkForUpdatedIssues()
        //assert
        val millisSinceLastPoll = LocalDateTime.now().until(appState.lastPollingTimestamp, ChronoUnit.MILLIS)
        assertTrue(millisSinceLastPoll < 5000, "Last pooling timestamp must be recent")
    }
}