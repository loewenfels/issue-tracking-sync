package ch.loewenfels.issuetrackingsync.executor.fields.skipping

import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient
import ch.loewenfels.issuetrackingsync.syncconfig.FieldSkippingEvaluatorDefinition

open class SkipUnlessAllowedState(fieldSkippingEvaluatorDefinition: FieldSkippingEvaluatorDefinition) :
    FieldSkippingEvaluator(fieldSkippingEvaluatorDefinition) {
    override fun <T> hasFieldToBeSkipped(
        issueClient: IssueTrackingClient<in T>,
        issueBuilder: Any,
        issue: Issue,
        fieldname: String,
        sourceValue: Any?
    ): Boolean {
        @Suppress("UNCHECKED_CAST")
        val propIssue = issue.proprietaryTargetInstance as? T
        val status = propIssue?.let { issueClient.getState(it) } ?: ""
        if (status == "") {
            return false
        }
        val allowedStates = fieldSkippingEvaluatorDefinition.properties["allowedStates"]
        if (allowedStates is List<*>) {
            return !allowedStates.contains(status)
        }
        error("This class expects a property 'allowedStates' as a list of state IDs")
    }
}