package ch.loewenfels.issuetrackingsync.executor.fields

import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient

/**
 * This class normalizes time tracking data to seconds. JIRA reports in minutes (int), RTC in milliseconds (long).
 * Furthermore, JIRA works with time spent, original estimate, and remaining estimate. RTC works
 * with time spent, duration and correction.
 *
 * - time spent (JIRA) = time spent (RTC)
 * - original estimate (JIRA) = duration (RTC)
 */
open class TimeFieldMapper : FieldMapper {

    override fun <T> getValue(
        proprietaryIssue: T,
        fieldname: String,
        issueTrackingClient: IssueTrackingClient<in T>
    ): Number? = issueTrackingClient.getTimeValueInMinutes(proprietaryIssue as Any, fieldname)

    override fun <T> setValue(
        proprietaryIssueBuilder: Any,
        fieldname: String,
        issue: Issue,
        issueTrackingClient: IssueTrackingClient<in T>,
        value: Any?
    ) {
        issueTrackingClient.setTimeValue(proprietaryIssueBuilder, issue, fieldname, value as Number)
    }
}