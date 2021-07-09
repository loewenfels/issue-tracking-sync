package ch.loewenfels.issuetrackingsync.executor.actions

import ch.loewenfels.issuetrackingsync.AbstractSpringTest
import ch.loewenfels.issuetrackingsync.Comment
import ch.loewenfels.issuetrackingsync.any
import ch.loewenfels.issuetrackingsync.safeEq
import ch.loewenfels.issuetrackingsync.syncclient.ClientFactory
import ch.loewenfels.issuetrackingsync.syncconfig.AdditionalProperties
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildFieldMappingList
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildIssueTrackingApplication
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildIssueTrackingClient
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import ch.loewenfels.issuetrackingsync.syncconfig.CommentFilter as configCommentFilter

internal class CommentsSynchronizationActionTest : AbstractSpringTest() {
    @Autowired
    private lateinit var clientFactory: ClientFactory


    @Test
    fun execute_someCommentsHaveContentFilterFiltersEverything_NoSync() {
        // arrange
        val sourceClient = buildIssueTrackingClient(buildIssueTrackingApplication("JiraClient"), clientFactory)
        val targetClient = buildIssueTrackingClient(buildIssueTrackingApplication("RtcClient"), clientFactory)
        val fieldMappings = buildFieldMappingList()
        val issue = sourceClient.getIssue("MK-1") ?: throw IllegalArgumentException("Unknown key")
        val targetIssue = targetClient.getIssue("1234") ?: throw IllegalArgumentException("Unknown key")
        val additionalProperties = AdditionalProperties()
        additionalProperties.commentFilter =
            listOf(configCommentFilter("ch.loewenfels.issuetrackingsync.executor.actions.CommentFilterAlwaysFalseStub"))
        issue.proprietarySourceInstance = issue
        issue.proprietaryTargetInstance = targetIssue
        val testee = CommentsSynchronizationAction()
        // act
        testee.execute(sourceClient, targetClient, issue, fieldMappings, null, additionalProperties)
        // assert
        verify(targetClient, never()).addComment(safeEq(targetIssue), any(Comment::class.java))
    }

    @Test
    fun execute_someCommentsHaveContentFilterFiltersNothing_CommentsShouldGetSynced() {
        // arrange
        val sourceClient = buildIssueTrackingClient(buildIssueTrackingApplication("JiraClient"), clientFactory)
        val targetClient = buildIssueTrackingClient(buildIssueTrackingApplication("RtcClient"), clientFactory)
        val fieldMappings = buildFieldMappingList()
        val issue = sourceClient.getIssue("MK-1") ?: throw IllegalArgumentException("Unknown key")
        val targetIssue = targetClient.getIssue("1234") ?: throw IllegalArgumentException("Unknown key")
        val additionalProperties = AdditionalProperties()
        additionalProperties.commentFilter =
            listOf(configCommentFilter("ch.loewenfels.issuetrackingsync.executor.actions.CommentFilterAlwaysTrueStub"))
        issue.proprietarySourceInstance = issue
        issue.proprietaryTargetInstance = targetIssue
        val testee = CommentsSynchronizationAction()
        // act
        testee.execute(sourceClient, targetClient, issue, fieldMappings, null, additionalProperties)
        // assert
        verify(targetClient).addComment(safeEq(targetIssue), any(Comment::class.java))
    }

    // TODO move to Custom-mapper, or check setup
    fun execute_commentsAreBeforeCreatedAfterOfFilter_NoSync() {
        // arrange
        val sourceClient = buildIssueTrackingClient(buildIssueTrackingApplication("JiraClient"), clientFactory)
        val targetClient = buildIssueTrackingClient(buildIssueTrackingApplication("RtcClient"), clientFactory)
        val fieldMappings = buildFieldMappingList()
        val issue = sourceClient.getIssue("MK-1") ?: throw IllegalArgumentException("Unknown key")
        val targetIssue = targetClient.getIssue("1234") ?: throw IllegalArgumentException("Unknown key")
        val additionalProperties = AdditionalProperties()
        additionalProperties.commentFilter =
            listOf(
                configCommentFilter(
                    "ch.loewenfels.issuetrackingsync.custom.CreatedBeforeCommentFilter", mapOf(
                        "createdBefore" to LocalDateTime.now().toString()
                    )
                )
            )
        issue.proprietarySourceInstance = issue
        issue.proprietaryTargetInstance = targetIssue
        val testee = CommentsSynchronizationAction()
        // act
        testee.execute(sourceClient, targetClient, issue, fieldMappings, null, additionalProperties)
        // assert
        verify(targetClient, never()).addComment(safeEq(targetIssue), any(Comment::class.java))
    }

    @Test
    fun execute_commentsAreAfterCreatedAfterOfFilter_CommentsShouldGetSynced() {
        // arrange
        val sourceClient = buildIssueTrackingClient(buildIssueTrackingApplication("JiraClient"), clientFactory)
        val targetClient = buildIssueTrackingClient(buildIssueTrackingApplication("RtcClient"), clientFactory)
        val fieldMappings = buildFieldMappingList()
        val issue = sourceClient.getIssue("MK-1") ?: throw IllegalArgumentException("Unknown key")
        val targetIssue = targetClient.getIssue("1234") ?: throw IllegalArgumentException("Unknown key")
        val additionalProperties = AdditionalProperties()
        additionalProperties.commentFilter =
            listOf(
                configCommentFilter(
                    "ch.loewenfels.issuetrackingsync.custom.CreatedBeforeCommentFilter", mapOf(
                        "createdBefore" to LocalDateTime.now().minusHours(48).toString()
                    )
                )
            )
        issue.proprietarySourceInstance = issue
        issue.proprietaryTargetInstance = targetIssue
        val testee = CommentsSynchronizationAction()
        // act
        testee.execute(sourceClient, targetClient, issue, fieldMappings, null, additionalProperties)
        // assert
        verify(targetClient).addComment(safeEq(targetIssue), any(Comment::class.java))
    }

    @Test
    fun execute_someCommentsHaveContentMatch() {
        // arrange
        val sourceClient = buildIssueTrackingClient(buildIssueTrackingApplication("JiraClient"), clientFactory)
        val targetClient = buildIssueTrackingClient(buildIssueTrackingApplication("RtcClient"), clientFactory)
        val fieldMappings = buildFieldMappingList()
        val issue = sourceClient.getIssue("MK-1") ?: throw IllegalArgumentException("Unknown key")
        val targetIssue = targetClient.getIssue("1234") ?: throw IllegalArgumentException("Unknown key")
        issue.proprietarySourceInstance = issue
        issue.proprietaryTargetInstance = targetIssue
        val testee = CommentsSynchronizationAction()
        // act
        testee.execute(sourceClient, targetClient, issue, fieldMappings, null)
        // assert
        verify(targetClient).addComment(safeEq(targetIssue), any(Comment::class.java))
    }

    @Test
    fun execute_targetContainsSyncedComment_nothingSynced() {
        // arrange
        val sourceClient = buildIssueTrackingClient(buildIssueTrackingApplication("JiraClient"), clientFactory)
        val targetClient = buildIssueTrackingClient(buildIssueTrackingApplication("RtcClient"), clientFactory)
        val fieldMappings = buildFieldMappingList()
        val issue = sourceClient.getIssue("MK-1") ?: throw IllegalArgumentException("Unknown key")
        val targetIssue = targetClient.getIssue("1234") ?: throw IllegalArgumentException("Unknown key")
        issue.proprietarySourceInstance = issue
        issue.proprietaryTargetInstance = targetIssue
        `when`(sourceClient.getComments(issue)).thenReturn(listOf(getCommentOriginal("1234")))
        `when`(targetClient.getComments(targetIssue)).thenReturn(listOf(getCommentSynced("1234", "9999")))
        val testee = CommentsSynchronizationAction()
        // act
        testee.execute(sourceClient, targetClient, issue, fieldMappings, null)
        // assert
        verify(targetClient, never()).addComment(safeEq(targetIssue), any(Comment::class.java))
    }

    @Test
    fun execute_sourceContainsSyncedComment_nothingSynced() {
        // arrange
        val sourceClient = buildIssueTrackingClient(buildIssueTrackingApplication("JiraClient"), clientFactory)
        val targetClient = buildIssueTrackingClient(buildIssueTrackingApplication("RtcClient"), clientFactory)
        val fieldMappings = buildFieldMappingList()
        val issue = sourceClient.getIssue("MK-1") ?: throw IllegalArgumentException("Unknown key")
        val targetIssue = targetClient.getIssue("1234") ?: throw IllegalArgumentException("Unknown key")
        issue.proprietarySourceInstance = issue
        issue.proprietaryTargetInstance = targetIssue
        `when`(sourceClient.getComments(issue)).thenReturn(listOf(getCommentSynced("1234", "9999")))
        `when`(targetClient.getComments(targetIssue)).thenReturn(listOf(getCommentOriginal("1234")))
        val testee = CommentsSynchronizationAction()
        // act
        testee.execute(sourceClient, targetClient, issue, fieldMappings, null)
        // assert
        verify(targetClient, never()).addComment(safeEq(targetIssue), any(Comment::class.java))
    }

    private fun getCommentOriginal(internalId: String): Comment =
        createComment("*Have* fun", internalId)

    private fun getCommentSynced(sourceInternalId: String, internalId: String): Comment =
        createComment("[from $sourceInternalId]: \n<b>Have</b> fun", internalId)

    private fun createComment(content: String, internalId: String): Comment =
        Comment("junit", LocalDateTime.now(), content, internalId)

}

class CommentFilterAlwaysFalseStub(val filterProperties: Map<String, String> = emptyMap()) : CommentFilter {
    override fun getFilter(): (Comment) -> Boolean = { (_) -> false }

}

class CommentFilterAlwaysTrueStub(val filterProperties: Map<String, String> = emptyMap()) : CommentFilter {
    override fun getFilter(): (Comment) -> Boolean = { (_) -> true }

}
