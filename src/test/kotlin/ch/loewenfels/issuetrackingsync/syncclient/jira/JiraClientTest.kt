package ch.loewenfels.issuetrackingsync.syncclient.jira

import ch.loewenfels.issuetrackingsync.AbstractSpringTest
import ch.loewenfels.issuetrackingsync.syncconfig.IssueTrackingApplication
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildIssue
import com.atlassian.jira.rest.client.api.domain.Issue
import com.atlassian.jira.rest.client.api.domain.IssueFieldId
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.time.LocalDateTime

/**
 * These tests rely on a valid JIRA setup. To run, remove the @Disabled and edit buildSetup()
 */
@Disabled
internal class JiraClientTest : AbstractSpringTest() {
    @Test
    fun getIssue_validKey_issueFound() {
        // arrange
        val testee = JiraClient(buildSetup())
        verifySetup(testee)
        // act
        val issue = testee.use { client -> client.getIssue("DEV-44692") }
        // assert
        assertNotNull(issue)
        assertEquals("DEV-44692", issue?.key)
    }

    @Test
    fun getValue_descriptionAsHtml_issueFound() {
        // arrange
        val testee = JiraClient(buildSetup())
        verifySetup(testee)
        val issue = testee.getProprietaryIssue("DEV-44692") ?: throw IllegalArgumentException("Unknown key")
        // act
        val html = testee.use { client -> client.getHtmlValue(issue, "description") }
        // assert
        assertNotNull(issue)
        assertThat(html, containsString("<h4>"))
    }

    @Test
    fun changedIssuesSince_updatedTwoDaysAgo_issuesCollectionNotNull() {
        // arrange
        val testee = JiraClient(buildSetup())
        verifySetup(testee)
        val lastUpdated = LocalDateTime.now().minusDays(2)
        // act
        val issues = testee.use { client -> client.changedIssuesSince(lastUpdated, 0, 50) }
        // assert
        assertNotNull(issues)
    }

    @Test
    fun listFields() {
        // arrange
        val testee = JiraClient(buildSetup())
        verifySetup(testee)
        // act
        testee.use { client -> client.listFields() }
    }

    @Test
    fun getComments_validKey_commentsLoaded() {
        // arrange
        val testee = JiraClient(buildSetup())
        val issue = testee.getProprietaryIssue("DEV-44692") ?: throw IllegalArgumentException("Unknown key")
        // act
        val comments = testee.use { client -> client.getComments(issue) }
        // assert
        assertNotNull(comments)
        assertEquals(2, comments.size)
    }

    @Test
    fun setValue_listOfLabels_setLabelsFieldCalled() {
        // arrange
        val sourceIssue = buildIssue("MK-1")
        val targetIssue = mock(Issue::class.java)
        sourceIssue.proprietaryTargetInstance = targetIssue
        val issueInputBuilder = mock(IssueInputBuilder::class.java)
        val listOfLabels = arrayListOf("nextSprint", "bug")
        val testee = JiraClient(buildSetup())
        // act
        testee.use { client -> client.setValue(issueInputBuilder, sourceIssue, "labels", listOfLabels) }
        // assert
        verify(issueInputBuilder).setFieldValue(IssueFieldId.LABELS_FIELD.id, listOfLabels)
    }

    @Test
    fun setValue_emptyLabel_setLabelsFieldNotCalled() {
        // arrange
        val sourceIssue = buildIssue("MK-1")
        val targetIssue = mock(Issue::class.java)
        sourceIssue.proprietaryTargetInstance = targetIssue
        val issueInputBuilder = mock(IssueInputBuilder::class.java)
        val testee = JiraClient(buildSetup())
        // act
        testee.use { client -> client.setValue(issueInputBuilder, sourceIssue, "labels", arrayListOf<String>()) }
        // assert
        verifyNoInteractions(issueInputBuilder)
    }

    private fun buildSetup(): IssueTrackingApplication {
        return IssueTrackingApplication(
            "ch.loewenfels.issuetrackingsync.client.jira.JiraClient",
            "JIRA",
            "myusername",
            "mysecret",
            "https://jira.foobar.com/jira",
            "",
            false
        )
    }

    private fun verifySetup(client: JiraClient) {
        try {
            val greeting = client.verifySetup()
            assumeTrue(greeting.isNotEmpty())
        } catch (ex: Exception) {
            assumeTrue(false)
        }
    }
}