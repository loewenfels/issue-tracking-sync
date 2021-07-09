package ch.loewenfels.issuetrackingsync.executor.fields.skipping

import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient
import ch.loewenfels.issuetrackingsync.syncconfig.FieldSkippingEvaluatorDefinition

open class SkipFieldOnSpecificTargetValue(fieldSkippingEvaluatorDefinition: FieldSkippingEvaluatorDefinition) :
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
        val fieldValue: String = (propIssue?.let { issueClient.getValue(it, fieldname) } ?: "") as String
        if (fieldValue == "") {
            return false
        }
        val fieldValues = fieldSkippingEvaluatorDefinition.properties["fieldValues"]
        if (fieldValues is Map<*, *>) {
            return fieldValues[sourceValue]?.let { (it as List<*>).contains(fieldValue) } ?: false
        }
        error("This class expects a property 'allowedStates' as a list of state IDs")
    }
}