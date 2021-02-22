package ch.loewenfels.issuetrackingsync.custom

import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.executor.fields.FieldMapper
import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient
import com.atlassian.jira.rest.client.api.domain.TimeTracking

class TimeSynchronizationFromRtcToJiraMapper : FieldMapper {
    private val sourceNames = listOf("duration", "correctedEstimate")
    private val targetNames = listOf(
        "timeTracking.originalEstimateMinutes",
        "timeTracking.timeSpentMinutes",
        "timeTracking.remainingEstimateMinutes"
    )

    override fun <T> getValue(
        proprietaryIssue: T,
        fieldname: String,
        issueTrackingClient: IssueTrackingClient<in T>
    ): List<Number?> = sourceNames.map { issueTrackingClient.getTimeValueInMinutes(proprietaryIssue as Any, it) }

    override fun <T> setValue(
        proprietaryIssueBuilder: Any,
        fieldname: String,
        issue: Issue,
        issueTrackingClient: IssueTrackingClient<in T>,
        value: Any?
    ) {
        @Suppress("UNCHECKED_CAST")
        val newEstimatedTime = value as List<Number?>
        val times = targetNames.map { name ->
            issue.proprietaryTargetInstance?.let {
                issueTrackingClient.getTimeValueInMinutes(it, name)
            } ?: 0
        }
        val fullTimeEstimated = getEstimatedTime(newEstimatedTime)
        val estimatedTime = newEstimatedTime[0]?.toInt() ?: 0
        val correctionTime = newEstimatedTime[1]?.toInt() ?: 0
        val timeSpent = times[1].toInt()

        var newRemaining = if (timeSpent in 1 until fullTimeEstimated) fullTimeEstimated - timeSpent else null
        if (null == newRemaining && correctionTime > estimatedTime) {
            newRemaining = correctionTime
        }

        val newTimes = TimeTracking(newEstimatedTime[0]?.toInt(), newRemaining, timeSpent)
        issueTrackingClient.setValue(
            proprietaryIssueBuilder,
            issue,
            targetNames[1],
            newTimes
        )
    }

    private fun getEstimatedTime(estimatedTime: List<Number?>): Int {
        val originalTime = estimatedTime[0]?.toInt() ?: 0
        val correctionTime = estimatedTime[1]?.toInt() ?: 0

        return if (correctionTime > originalTime) correctionTime else originalTime
    }
}