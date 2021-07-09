package ch.loewenfels.issuetrackingsync.executor.actions

import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.executor.fields.FieldMapping
import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient
import ch.loewenfels.issuetrackingsync.syncconfig.DefaultsForNewIssue

@Suppress("UnnecessaryAbstractClass")
abstract class AbstractSynchronizationAction {
    protected fun buildTargetIssueValues(
        sourceClient: IssueTrackingClient<Any>,
        issue: Issue,
        fieldMappings: List<FieldMapping>
    ) {
        fieldMappings.forEach {
            it.loadSourceValue(issue, sourceClient)
            issue.fieldMappings.add(it)
        }
    }

    protected fun createOrUpdateTargetIssue(
        targetClient: IssueTrackingClient<Any>,
        issue: Issue,
        defaultsForNewIssue: DefaultsForNewIssue?
    ) {
        targetClient.createOrUpdateTargetIssue(issue, defaultsForNewIssue)
    }
}