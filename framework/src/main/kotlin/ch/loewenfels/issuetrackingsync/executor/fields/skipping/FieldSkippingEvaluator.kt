package ch.loewenfels.issuetrackingsync.executor.fields.skipping

import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient
import ch.loewenfels.issuetrackingsync.syncconfig.FieldSkippingEvaluatorDefinition

/*
 * Due to the Possibility that a field can be non existing or unchangeable on certain occasions there has to be the
 * ability to skip one single FieldMapper.
 */
abstract class FieldSkippingEvaluator(var fieldSkippingEvaluatorDefinition: FieldSkippingEvaluatorDefinition) {
    abstract fun <T> hasFieldToBeSkipped(
        issueClient: IssueTrackingClient<in T>,
        issueBuilder: Any,
        issue: Issue,
        fieldname: String,
        sourceValue: Any?
    ): Boolean
}