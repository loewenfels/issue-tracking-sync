package ch.loewenfels.issuetrackingsync.executor.fields.skipping

import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient
import ch.loewenfels.issuetrackingsync.syncconfig.FieldSkippingEvaluatorDefinition

class SkipFieldIfSameAsTargetField(fieldSkippingEvaluatorDefinition: FieldSkippingEvaluatorDefinition) :
        FieldSkippingEvaluator(fieldSkippingEvaluatorDefinition) {
    val properties: Map<String, Any> = fieldSkippingEvaluatorDefinition.properties
    val associations: Map<String, Any> = fieldSkippingEvaluatorDefinition.associations

    override fun <T> hasFieldToBeSkipped(
            issueClient: IssueTrackingClient<in T>,
            issueBuilder: Any,
            issue: Issue,
            fieldname: String,
            sourceValue: Any?
    ): Boolean {
        @Suppress("UNCHECKED_CAST")
        val propIssue = issue.proprietaryTargetInstance as? T
        val field = properties["field"]

        var valueToCompare = sourceValue
        val associationKey = sourceValue ?: "null"
        if (associations.containsKey(associationKey)) {
            valueToCompare = associations[associationKey as String]
        }

        if (field is String) {
            val fieldValue: Any? = propIssue?.let { issueClient.getValue(it, field) }

            return valueToCompare == fieldValue
        }
        error("This class expects a property 'field' as a compare target field")
    }
}