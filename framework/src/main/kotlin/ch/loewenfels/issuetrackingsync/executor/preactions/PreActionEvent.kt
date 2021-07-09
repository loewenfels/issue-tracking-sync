package ch.loewenfels.issuetrackingsync.executor.preactions

import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient

class PreActionEvent(
    val sourceClient: IssueTrackingClient<Any>,
    val targetClient: IssueTrackingClient<Any>,
    val issue: Issue
) {
    var isStopSynchronization = false
    var isStopPropagation = false

    fun stopSynchronization() {
        isStopSynchronization = true
    }

    fun stopPropagation() {
        isStopPropagation = true
    }
}