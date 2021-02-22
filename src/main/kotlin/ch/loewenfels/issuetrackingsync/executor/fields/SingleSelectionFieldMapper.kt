package ch.loewenfels.issuetrackingsync.executor.fields

import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient
import ch.loewenfels.issuetrackingsync.syncconfig.FieldMappingDefinition

/**
 *  This class matches properties of a single-select field from the source client to the target client
 *  via the [associations]. The internal value will be the *source value*, mapped to a target value
 *  only when writing out.
 *
 *  [associations] contains the source property-name as key and the target property-name as value and must be
 *  configured for both clients. This implies that both client-configuration are a mirrored version of each other.
 */
class SingleSelectionFieldMapper(fieldMappingDefinition: FieldMappingDefinition) : FieldMapper {
    private val keyFallback = "*"
    private val associations: Map<String, String> = fieldMappingDefinition.associations

    override fun <T> getValue(
        proprietaryIssue: T,
        fieldname: String,
        issueTrackingClient: IssueTrackingClient<in T>
    ): Any? = issueTrackingClient.getValue(proprietaryIssue, fieldname)

    override fun <T> setValue(
        proprietaryIssueBuilder: Any,
        fieldname: String,
        issue: Issue,
        issueTrackingClient: IssueTrackingClient<in T>,
        value: Any?
    ) {
        val associationKey = value ?: "null"
        val result = computeValue(associationKey as String)
        if (null !== result) {
            issueTrackingClient.setValue(proprietaryIssueBuilder, issue, fieldname, result)
        } else {
            issue.workLog.add("Cannot update $fieldname, there is not association entry for $associationKey")
        }
    }

    private fun computeValue(key: String): String? {
        if (associations.containsKey(key)) {
            return associations[key]
        }
        if (associations.containsKey(keyFallback)) {
            return associations[keyFallback]
        }

        return null
    }
}