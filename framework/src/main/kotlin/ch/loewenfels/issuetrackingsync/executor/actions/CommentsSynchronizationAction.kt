package ch.loewenfels.issuetrackingsync.executor.actions

import ch.loewenfels.issuetrackingsync.Comment
import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.Logging
import ch.loewenfels.issuetrackingsync.executor.fields.FieldMapping
import ch.loewenfels.issuetrackingsync.logger
import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient
import ch.loewenfels.issuetrackingsync.syncconfig.AdditionalProperties
import ch.loewenfels.issuetrackingsync.syncconfig.DefaultsForNewIssue
import java.time.format.DateTimeFormatter
import ch.loewenfels.issuetrackingsync.syncconfig.CommentFilter as configCommentFilter

class CommentsSynchronizationAction : AbstractSynchronizationAction(),
    SynchronizationAction, Logging {
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm")
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    override fun execute(
        sourceClient: IssueTrackingClient<Any>,
        targetClient: IssueTrackingClient<Any>,
        issue: Issue,
        fieldMappings: List<FieldMapping>,
        defaultsForNewIssue: DefaultsForNewIssue?
    ) = execute(sourceClient, targetClient, issue, fieldMappings, defaultsForNewIssue, AdditionalProperties())

    override fun execute(
        sourceClient: IssueTrackingClient<Any>,
        targetClient: IssueTrackingClient<Any>,
        issue: Issue,
        fieldMappings: List<FieldMapping>,
        defaultsForNewIssue: DefaultsForNewIssue?,
        additionalProperties: AdditionalProperties?
    ) {
        val internalSourceIssue = issue.proprietarySourceInstance
        val internalTargetIssue = issue.proprietaryTargetInstance
        if ((internalSourceIssue != null) && (internalTargetIssue != null)) {
            val sourceComments = sourceClient.getComments(internalSourceIssue)
            val targetComments = targetClient.getComments(internalTargetIssue)
            val commentsToSync =
                getSourceCommentsNotPresentInTarget(sourceComments, targetComments, additionalProperties?.commentFilter)
            commentsToSync
                .map { mapContentOfComment(it, additionalProperties) }
                .forEach {
                    targetClient.addComment(internalTargetIssue, it)
                    issue.workLog.add("Added comment from ${it.author} created ${it.timestamp}")
                }
            if (commentsToSync.isNotEmpty()) {
                issue.hasChanges = true
            }
        } else {
            logger().warn(
                "This action relies on a previous action loading source and target issues." +
                        " Consider configuring a SimpleSynchronizationAction without any fieldMappings prior to this action."
            )
        }
    }

    private fun mapContentOfComment(comment: Comment, additionalProperties: AdditionalProperties?): Comment {
        val preComment: String = replacePlaceholders(additionalProperties?.preComment ?: "", comment)
        val postComment: String = replacePlaceholders(additionalProperties?.postComment ?: "", comment)
        val content = listOf(preComment, comment.content, postComment)
            .filter { it.isNotEmpty() }
            .joinToString("<br/>")
        return Comment(comment.author, comment.timestamp, content, comment.internalId)
    }

    private fun replacePlaceholders(
        containingPlaceholders: String,
        comment: Comment
    ): String = containingPlaceholders
        .replace("\${author}", comment.author)
        .replace("\${id}", comment.internalId)
        .replace("\${time}", comment.timestamp.format(timeFormatter))
        .replace("\${date}", comment.timestamp.format(dateFormatter))

    private fun getSourceCommentsNotPresentInTarget(
        sourceComments: List<Comment>,
        targetComments: List<Comment>,
        commentFilters: List<configCommentFilter>?
    ): List<Comment> = sourceComments
        .filter { src -> !isSourcePresentInTarget(src, targetComments) }
        .filter(CommentFilterFactory.create(commentFilters))

    companion object {
        fun isSourcePresentInTarget(
            sourceComment: Comment,
            targetComments: List<Comment>
        ): Boolean =
            targetComments.any { targetComment ->
                sourceComment.content.contains(targetComment.content)
                        || targetComment.content.contains(sourceComment.content)
                        // if the source comment contains the internal ID of a target, it must have been sync'ed
                        // from another system, so don't sync it back anywhere
                        || sourceComment.content.contains(targetComment.internalId)
                        // a match here means the source comment was already synced to the target system
                        || targetComment.content.contains(sourceComment.internalId)
            }
    }

    class CommentFilterFactory {
        companion object : Logging {
            fun create(commentFilters: List<configCommentFilter>?): (Comment) -> Boolean {
                if (commentFilters == null) {
                    return { (_) -> true }
                }
                val someList: MutableList<(Comment) -> Boolean> =
                    getListOfCommentFilterInstances(commentFilters)
                return { comment ->
                    !someList.map { it(comment) }.contains(false)
                }
            }

            private fun getListOfCommentFilterInstances(commentFilters: List<configCommentFilter>): MutableList<(Comment) -> Boolean> {
                val someList: MutableList<(Comment) -> Boolean> = mutableListOf()
                commentFilters.forEach {
                    try {
                        someList.add(
                            (Class.forName(it.filterClassname).getDeclaredConstructor(Map::class.java)
                                .newInstance(it.filterProperties) as CommentFilter).getFilter()
                        )
                    } catch (e: Exception) {
                        logger().error("Failed to create $it\nException was: ${e.message}")
                    }
                }
                return someList
            }
        }
    }
}


