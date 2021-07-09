package ch.loewenfels.issuetrackingsync.executor.fields

import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient
import ch.loewenfels.issuetrackingsync.syncconfig.FieldMappingDefinition

/**
 *  This class matches properties of a multi-select field from the source client to the target client
 *  via the [associations].
 *
 *  [associations] contains the source property-name as key and the target property-name as value and must be
 *  configured for both clients. This implies that both client-configuration are a mirrored version of each other.
 */

open class MultiSelectionFieldMapper(fieldMappingDefinition: FieldMappingDefinition) : FieldMapper {
    private val associations: Map<String, String> = fieldMappingDefinition.associations

    override fun <T> getValue(
        proprietaryIssue: T,
        fieldname: String,
        issueTrackingClient: IssueTrackingClient<in T>
    ): Any? {
        return getValue(proprietaryIssue, fieldname, issueTrackingClient, associations)
    }

    fun <T> getValue(
        proprietaryIssue: T,
        fieldname: String,
        issueTrackingClient: IssueTrackingClient<in T>,
        association: Map<String, String>
    ): List<String> {
        val values = issueTrackingClient.getMultiSelectValues(proprietaryIssue, fieldname)
        return mapAssociations(values, association)
    }

    private fun mapAssociations(value: List<String>, association: Map<String, String>): List<String> {
        val containsOneToOneAssociation = association.containsKey("*") && association.getValue("*") == "*"
        val containsOneToSomethingAssociation = association.containsKey("*") && !containsOneToOneAssociation
        return value
            .filter { association.containsKey(it) || containsOneToOneAssociation || containsOneToSomethingAssociation }
            .map {
                when {
                    association.containsKey(it) -> association.getValue(it)
                    containsOneToOneAssociation -> it
                    else -> association.getValue("*")
                }
            }
    }

    override fun <T> setValue(
        proprietaryIssueBuilder: Any,
        fieldname: String,
        issue: Issue,
        issueTrackingClient: IssueTrackingClient<in T>,
        value: Any?
    ) {
        issueTrackingClient.setValue(proprietaryIssueBuilder, issue, fieldname, value)
    }
}