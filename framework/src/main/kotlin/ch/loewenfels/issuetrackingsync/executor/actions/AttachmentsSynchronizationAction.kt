package ch.loewenfels.issuetrackingsync.executor.actions

import ch.loewenfels.issuetrackingsync.Attachment
import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.Logging
import ch.loewenfels.issuetrackingsync.executor.fields.FieldMapping
import ch.loewenfels.issuetrackingsync.logger
import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient
import ch.loewenfels.issuetrackingsync.syncconfig.DefaultsForNewIssue

class AttachmentsSynchronizationAction : AbstractSynchronizationAction(),
    SynchronizationAction, Logging {
    override fun execute(
        sourceClient: IssueTrackingClient<Any>,
        targetClient: IssueTrackingClient<Any>,
        issue: Issue,
        fieldMappings: List<FieldMapping>,
        defaultsForNewIssue: DefaultsForNewIssue?
    ) {
        val internalSourceIssue = issue.proprietarySourceInstance
        val internalTargetIssue = issue.proprietaryTargetInstance
        if ((internalSourceIssue != null) && (internalTargetIssue != null)) {
            val sourceAttachments = sourceClient.getAttachments(internalSourceIssue)
            val targetAttachments = targetClient.getAttachments(internalTargetIssue)
            val attachmentsToSync = getSourceAttachmentsNotPresentInTarget(sourceAttachments, targetAttachments)
            attachmentsToSync.forEach {
                targetClient.addAttachment(internalTargetIssue, it)
                issue.workLog.add("Added attachment ${it.filename}")
            }
            if (attachmentsToSync.isNotEmpty()) {
                issue.hasChanges = true
            }
        } else {
            logger().warn(
                "This action relies on a previous action loading source and target issues." +
                        " Consider configuring a SimpleSynchronizationAction without any fieldMappings prior to this action."
            )
        }
    }

    private fun getSourceAttachmentsNotPresentInTarget(
        sourceAttachments: List<Attachment>,
        targetAttachments: List<Attachment>
    ): List<Attachment> =
        sourceAttachments.filter { src -> !targetAttachments.contains(src) }.toList()
}