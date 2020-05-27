package ch.loewenfels.issuetrackingsync.custom

import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.executor.actions.SimpleSynchronizationAction
import ch.loewenfels.issuetrackingsync.executor.fields.FieldMapper
import ch.loewenfels.issuetrackingsync.executor.fields.FieldMapping
import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient
import ch.loewenfels.issuetrackingsync.syncconfig.DefaultsForNewIssue
import com.atlassian.jira.rest.client.api.domain.TimeTracking

class TimeSynchronizationFromRtcToJiraAction(actionName: String) : SimpleSynchronizationAction(actionName) {
    val fieldMapping: List<FieldMapping> = listOf(FieldMapping("", "", TimeSynchronizationFromRtcToJiraMapper()))

    override fun execute(
        sourceClient: IssueTrackingClient<Any>,
        targetClient: IssueTrackingClient<Any>,
        issue: Issue,
        fieldMappings: List<FieldMapping>,
        defaultsForNewIssue: DefaultsForNewIssue?
    ) {
        super.execute(sourceClient, targetClient, issue, fieldMapping, defaultsForNewIssue)
    }

    inner class TimeSynchronizationFromRtcToJiraMapper : FieldMapper {
        val sourceNames = listOf("duration", "correctedEstimate")
        val targetNames = listOf(
            "timeTracking.originalEstimateMinutes",
            "timeTracking.timeSpentMinutes",
            "timeTracking.remainingEstimateMinutes"
        )

        override fun <T> getValue(
            proprietaryIssue: T,
            fieldname: String,
            issueTrackingClient: IssueTrackingClient<in T>
        ): List<Number?> {
            return sourceNames.map { issueTrackingClient.getTimeValueInMinutes(proprietaryIssue as Any, it) }
        }


        override
        fun <T> setValue(
            proprietaryIssueBuilder: Any,
            fieldname: String,
            issue: Issue,
            issueTrackingClient: IssueTrackingClient<in T>,
            value: Any?
        ) {
            val newEstimatedTime = value as List<Number?>
            val times = targetNames.map { fieldname ->
                issue.proprietaryTargetInstance?.let {
                    issueTrackingClient.getTimeValueInMinutes(it, fieldname)
                } ?: 0
            }
            val newRemaining = newEstimatedTime[1]?.toInt()?.let {
                if (it > times[1]?.toInt()) it - times[1]?.toInt() else 0
            }
            val newTimes = TimeTracking(newEstimatedTime[0]?.toInt(), newRemaining, times[1]?.toInt())
            issueTrackingClient.setValue(
                proprietaryIssueBuilder,
                issue,
                targetNames[1],
                newTimes
            )
        }
    }
}