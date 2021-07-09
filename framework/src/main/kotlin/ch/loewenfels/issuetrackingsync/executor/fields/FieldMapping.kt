package ch.loewenfels.issuetrackingsync.executor.fields

import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.executor.fields.skipping.FieldSkippingEvaluator
import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient

open class FieldMapping(
    protected val sourceName: String,
    val targetName: String,
    protected val mapper: FieldMapper,
    private val fieldSkipEvaluators: List<FieldSkippingEvaluator> = mutableListOf()
) {
    protected var sourceValue: Any? = null

    @Suppress("UNCHECKED_CAST")
    fun <T> loadSourceValue(issue: Issue, issueTrackingClient: IssueTrackingClient<in T>) {
        if (issue.proprietarySourceInstance == null) {
            error("Internal source issue needs to be loaded first")
        }
        sourceValue = mapper.getValue(issue.proprietarySourceInstance as T, sourceName, issueTrackingClient)
    }

    fun <T> setTargetValue(issueBuilder: Any, issue: Issue, targetClient: IssueTrackingClient<in T>) {
        if (!fieldSkipEvaluators.any {
                it.hasFieldToBeSkipped(
                    targetClient,
                    issueBuilder,
                    issue,
                    targetName,
                    sourceValue
                )
            }) {
            mapper.setValue(issueBuilder, targetName, issue, targetClient, sourceValue)
        }
    }
}