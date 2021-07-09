package ch.loewenfels.issuetrackingsync.executor.fields

import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient
import ch.loewenfels.issuetrackingsync.syncconfig.FieldMappingDefinition

open class FieldValueRegexTransformationMapper(fieldMappingDefinition: FieldMappingDefinition) : FieldMapper {
    val associations = fieldMappingDefinition.associations

    override fun <T> getValue(
        proprietaryIssue: T,
        fieldname: String,
        issueTrackingClient: IssueTrackingClient<in T>
    ) = getValue(proprietaryIssue, fieldname, issueTrackingClient, associations)

    protected fun <T> getValue(
        proprietaryIssue: T,
        fieldname: String,
        issueTrackingClient: IssueTrackingClient<in T>,
        association: Map<String, String>
    ): Any? {
        return when (val value = issueTrackingClient.getValue(proprietaryIssue, fieldname)) {
            is String -> transFromString(value, association, fieldname)
            is List<*> -> issueTrackingClient
                .getMultiSelectValues(proprietaryIssue, fieldname)
                .map { transFromString(it, association, fieldname) }
            else -> value
        }
    }

    private fun transFromString(value: String, association: Map<String, String>, fieldname: String): String? {
        val transformedString = association.map {
            it.key.toRegex().find(value)?.value?.replace(it.key.toRegex(), it.value)
        }.filterNotNull().toList()
        check(transformedString.isNotEmpty()) {
            "Found non transformable value \"$value\" for field $fieldname."
        }
        return transformedString.firstOrNull(String::isNotEmpty)
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