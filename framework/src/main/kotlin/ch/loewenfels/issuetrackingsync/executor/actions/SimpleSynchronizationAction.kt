package ch.loewenfels.issuetrackingsync.executor.actions

import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.executor.fields.FieldMapping
import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient
import ch.loewenfels.issuetrackingsync.syncconfig.DefaultsForNewIssue

open class SimpleSynchronizationAction(private val actionName: String) : AbstractSynchronizationAction(),
    SynchronizationAction {
    override fun execute(
        sourceClient: IssueTrackingClient<Any>,
        targetClient: IssueTrackingClient<Any>,
        issue: Issue,
        fieldMappings: List<FieldMapping>,
        defaultsForNewIssue: DefaultsForNewIssue?
    ) {
        buildTargetIssueValues(sourceClient, issue, fieldMappings)
        createOrUpdateTargetIssue(targetClient, issue, defaultsForNewIssue)
        issue.workLog.add("Synchronized fields for ${issue.key} using $actionName")
    }
}