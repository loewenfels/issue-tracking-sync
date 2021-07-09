package ch.loewenfels.issuetrackingsync.executor.fields.skipping

import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient
import ch.loewenfels.issuetrackingsync.syncconfig.FieldSkippingEvaluatorDefinition

open class SkipIfNull(fieldSkippingEvaluatorDefinition: FieldSkippingEvaluatorDefinition) :
    FieldSkippingEvaluator(fieldSkippingEvaluatorDefinition) {
    override fun <T> hasFieldToBeSkipped(
        issueClient: IssueTrackingClient<in T>,
        issueBuilder: Any,
        issue: Issue,
        fieldname: String,
        sourceValue: Any?
    ) = null == sourceValue

}