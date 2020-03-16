package ch.loewenfels.issuetrackingsync.executor.fields

import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient
import ch.loewenfels.issuetrackingsync.syncconfig.FieldMappingDefinition


/**
 *  This class matches properties of a multi-select field from the source client to the target client
 *  via the [combinedAssociations], wich is a combination of [associations] and [additionalAssociations] (if present).
 *
 *  [combinedAssociations] contains the source property-name as key and the target property-name as value and must be
 *  configured for both clients. This implies that both client-configuration are a mirrored version of each other.
 */

class MultiSelectionFieldMapper(fieldMappingDefinition: FieldMappingDefinition) : FieldMapper {
    private val associations: Map<String, String> = fieldMappingDefinition.associations
    private val additionalAssociations: Map<String, String> = fieldMappingDefinition.additionalAssociations

    override fun <T> getValue(
        proprietaryIssue: T,
        fieldname: String,
        issueTrackingClient: IssueTrackingClient<in T>
    ): Any? {
        val values = issueTrackingClient.getMultiSelectValues(proprietaryIssue, fieldname)
        val combinedAssociations = getCombinedAssociations()
        return values.filter { combinedAssociations.containsKey(it) }
    }

    override fun <T> setValue(
        proprietaryIssueBuilder: Any,
        fieldname: String,
        issue: Issue,
        issueTrackingClient: IssueTrackingClient<in T>,
        value: Any?
    ) {
        val combinedAssociations = getCombinedAssociations()
        val result = (value as ArrayList<*>).filterIsInstance<String>()
            .filter { combinedAssociations.containsKey(it) }//
            .map { combinedAssociations.getValue(it) }//
            .distinct()
        issueTrackingClient.setValue(proprietaryIssueBuilder, issue, fieldname, result)
    }

    private fun getCombinedAssociations(): MutableMap<String, String> {
        val combinedAssociations = additionalAssociations.toMutableMap()
        combinedAssociations.putAll(associations)
        return combinedAssociations
    }
}