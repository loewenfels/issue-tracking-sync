package ch.loewenfels.issuetrackingsync.executor.actions

import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.executor.fields.FieldMapping
import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient
import ch.loewenfels.issuetrackingsync.syncconfig.AdditionalProperties
import ch.loewenfels.issuetrackingsync.syncconfig.DefaultsForNewIssue

@Suppress("LongParameterList")
interface SynchronizationAction {
    fun execute(
        sourceClient: IssueTrackingClient<Any>,
        targetClient: IssueTrackingClient<Any>,
        issue: Issue,
        fieldMappings: List<FieldMapping>,
        defaultsForNewIssue: DefaultsForNewIssue?
    )

    fun execute(
        sourceClient: IssueTrackingClient<Any>,
        targetClient: IssueTrackingClient<Any>,
        issue: Issue,
        fieldMappings: List<FieldMapping>,
        defaultsForNewIssue: DefaultsForNewIssue?,
        additionalProperties: AdditionalProperties?
    ) = execute(sourceClient, targetClient, issue, fieldMappings, defaultsForNewIssue)
}