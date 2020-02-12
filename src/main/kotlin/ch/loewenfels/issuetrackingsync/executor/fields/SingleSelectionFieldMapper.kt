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
    private val associations: Map<String, String> = fieldMappingDefinition.associations

    override fun <T> getValue(
        proprietaryIssue: T,
        fieldname: String,
        issueTrackingClient: IssueTrackingClient<in T>
    ): Any? {
        return issueTrackingClient.getValue(proprietaryIssue, fieldname)
    }

    override fun <T> setValue(
        proprietaryIssueBuilder: Any,
        fieldname: String,
        issue: Issue,
        issueTrackingClient: IssueTrackingClient<in T>,
        value: Any?
    ) {
        value?.let {
            val result = associations[it as String]
            issueTrackingClient.setValue(proprietaryIssueBuilder, issue, fieldname, result)
        }
    }
}