package ch.loewenfels.issuetrackingsync.executor

import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient
import ch.loewenfels.issuetrackingsync.syncconfig.SyncFlowDefinition

interface IssueFilter {
    fun test(
        client: IssueTrackingClient<out Any>, issue: Issue,
        syncFlowDefinition: SyncFlowDefinition
    ): Boolean

    fun defineParameters(parameters: Map<String, String>) {
        // no-op
    }
}