package ch.loewenfels.issuetrackingsync.syncclient

import ch.loewenfels.issuetrackingsync.Attachment
import ch.loewenfels.issuetrackingsync.Comment
import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.StateHistory
import ch.loewenfels.issuetrackingsync.executor.SyncActionName
import ch.loewenfels.issuetrackingsync.executor.actions.SynchronizationAction
import ch.loewenfels.issuetrackingsync.notification.NotificationObserver
import ch.loewenfels.issuetrackingsync.syncconfig.DefaultsForNewIssue
import com.fasterxml.jackson.databind.JsonNode
import com.ibm.team.workitem.common.model.IWorkItem
import java.time.LocalDateTime

@Suppress("ComplexInterface", "LongParameterList", "TooManyFunctions")
interface IssueTrackingClient<T> {
    /**
     * Attempt to load an issue by unique key. Returns absent value if no issue is found, or if the client
     * is not authorized to load the issue
     */
    fun getIssue(key: String): Issue?

    /**
     * Extract an issue from a webhook payload. If a issue tracking application cannot issue webhooks, the
     * client implementation should throw an [UnsupportedOperationException]
     */
    fun getIssueFromWebhookBody(body: JsonNode): Issue

    /**
     * Get the issue instance as used by the implementation. For JIRA, this is a [com.atlassian.jira.rest.client.api.domain.Issue],
     * whereas for RTC it will be [IWorkItem]
     */
    fun getProprietaryIssue(issueKey: String): T?

    /**
     * Search for an issue based on a field value. If the search finds more than 1 issue, an exception is thrown
     */
    fun getProprietaryIssue(fieldName: String, fieldValue: String): T?

    fun getLastUpdated(internalIssue: T): LocalDateTime

    fun getKey(internalIssue: T): String

    fun getIssueUrl(internalIssue: T): String

    fun getValue(internalIssue: T, fieldName: String): Any?

    fun setValue(
        internalIssueBuilder: Any,
        issue: Issue,
        fieldName: String,
        value: Any?
    )

    fun getComments(internalIssue: T): List<Comment>

    fun addComment(internalIssue: T, comment: Comment)

    fun getAttachments(internalIssue: T): List<Attachment>

    fun addAttachment(internalIssue: T, attachment: Attachment)

    fun getMultiSelectValues(internalIssue: T, fieldName: String): List<String>

    /**
     * Get the human-readable string representation of the current [internalIssue] state
     */
    fun getState(internalIssue: T): String

    /**
     * Get all state transitions. The last entry in the result will contain the transition to the
     * current [getState]
     */
    fun getStateHistory(internalIssue: T): List<StateHistory>

    /**
     * Set the state on the [internalIssue]. It is in the callers responsibility to ensure that the [targetState]
     * is available as transition from the current state of the [internalIssue]. If multiple transitions are
     * required, call this method multiple times
     */
    fun setState(internalIssue: T, targetState: String)

    fun createOrUpdateTargetIssue(issue: Issue, defaultsForNewIssue: DefaultsForNewIssue?)

    /**
     * Get all issues which:
     * - were created since [lastPollingTimestamp]
     * - were updated since [lastPollingTimestamp] and have a reference to another tracking application
     * - were updated since [lastPollingTimestamp] and [settings.json] defines no reference fields
     *     for this tracking application
     */
    fun changedIssuesSince(
        lastPollingTimestamp: LocalDateTime,
        batchSize: Int,
        offset: Int
    ): Collection<Issue>

    fun getHtmlValue(internalIssue: T, fieldName: String): String?

    fun setHtmlValue(
        internalIssueBuilder: Any,
        issue: Issue,
        fieldName: String,
        htmlString: String
    )

    fun getTimeValueInMinutes(internalIssue: Any, fieldName: String): Number

    fun setTimeValue(internalIssueBuilder: Any, issue: Issue, fieldName: String, timeInMinutes: Number?)

    fun logException(issue: Issue, exception: Exception, notificationObserver: NotificationObserver, syncActions: Map<SyncActionName, SynchronizationAction>)
}
