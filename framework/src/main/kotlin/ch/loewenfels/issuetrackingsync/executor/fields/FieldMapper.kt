package ch.loewenfels.issuetrackingsync.executor.fields

import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient

/**
 * A [FieldMapper] takes care of synchronizing a single data instance. Often this will be a simple String field-to-field
 * mapping, but there are cases where one tracking application has multiple fields requiring synchronization into
 * a single field in the target tracking application.
 *
 * All implementations must be state-less and thread-safe, as instances might be re-used.
 */
interface FieldMapper {
    /**
     * Read a value from the [proprietaryIssue], which is of whatever type the [issueTrackingClient] returns
     * when loading an issue.
     */
    fun <T> getValue(proprietaryIssue: T, fieldname: String, issueTrackingClient: IssueTrackingClient<in T>): Any?

    /**
     * Set a value on the [proprietaryIssueBuilder], which is whatever object the [issueTrackingClient] uses
     * to "build" an issue create/update before submitting it. The current [issue] is provided for context.
     */
    fun <T> setValue(
        proprietaryIssueBuilder: Any,
        fieldname: String,
        issue: Issue,
        issueTrackingClient: IssueTrackingClient<in T>,
        value: Any?
    )
}