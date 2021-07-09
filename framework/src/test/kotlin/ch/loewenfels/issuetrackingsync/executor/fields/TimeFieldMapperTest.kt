package ch.loewenfels.issuetrackingsync.executor.fields

import ch.loewenfels.issuetrackingsync.AbstractSpringTest
import ch.loewenfels.issuetrackingsync.safeEq
import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

internal class TimeFieldMapperTest : AbstractSpringTest() {

    @Test
    fun setValue_jira() {
        // arrange
        val testee = TimeFieldMapper()
        val issue = TestObjects.buildIssue("MK-1")
        issue.sourceUrl = "http://localhost/issues/MK-1"
        val targetClient = mock(IssueTrackingClient::class.java)
        val value = 0 as Number
        val fieldName = "timeTracking"
        // act
        testee.setValue(issue, fieldName, issue, targetClient, value)
        // assert
        verify(targetClient).setTimeValue(safeEq(issue), safeEq(issue), safeEq(fieldName), safeEq(value))
    }
}