package ch.loewenfels.issuetrackingsync.notification

import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.executor.SyncActionName
import ch.loewenfels.issuetrackingsync.executor.actions.SynchronizationAction

interface NotificationChannel {
    fun onSuccessfulSync(
        issue: Issue,
        syncActions: Map<SyncActionName, SynchronizationAction>
    )

    fun onException(
        issue: Issue,
        ex: Exception,
        syncActions: Map<SyncActionName, SynchronizationAction>
    )
}