package ch.loewenfels.issuetrackingsync.notification

import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.executor.SyncActionName
import ch.loewenfels.issuetrackingsync.executor.actions.SynchronizationAction
import java.util.*

class NotificationObserver {
    private val channels = ArrayList<NotificationChannel>()
    fun addChannel(channel: NotificationChannel) {
        this.channels.add(channel)
    }

    fun removeChannel(channel: NotificationChannel) {
        this.channels.remove(channel)
    }

    fun notifySuccessfulSync(
        issue: Issue,
        syncActions: Map<SyncActionName, SynchronizationAction>
    ) {
        this.channels.forEach { it.onSuccessfulSync(issue, syncActions) }
    }

    fun notifyException(
        issue: Issue,
        ex: Exception,
        syncActions: Map<SyncActionName, SynchronizationAction>
    ) {
        this.channels.forEach { it.onException(issue, ex, syncActions) }
    }
}