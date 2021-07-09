package ch.loewenfels.issuetrackingsync.executor.fields

import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.syncclient.IssueClientException
import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient
import ch.loewenfels.issuetrackingsync.syncconfig.FieldMappingDefinition

/**
 * This mapper splits or merges priority and severity. The direction is defined by the [fieldname], allowed
 * values are:
 * - priority [, severity]
 *
 * This mapper works with [Pair] values
 */
open class PriorityAndSeverityFieldMapper(fieldMappingDefinition: FieldMappingDefinition) : FieldMapper {
    private val keyFallback = "*,*"
    private val associations: Map<String, String> = fieldMappingDefinition.associations

    override fun <T> getValue(
        proprietaryIssue: T,
        fieldname: String,
        issueTrackingClient: IssueTrackingClient<in T>
    ): Any? {
        val properties = fieldname.split(",")
        return when (properties.size) {
            1 -> Pair(issueTrackingClient.getValue(proprietaryIssue, properties[0]), null)
            2 -> Pair(
                issueTrackingClient.getValue(proprietaryIssue, properties[0]),
                issueTrackingClient.getValue(proprietaryIssue, properties[1])
            )
            else -> throw IssueClientException("Don't know how to process $fieldname")
        }
    }

    override fun <T> setValue(
        proprietaryIssueBuilder: Any,
        fieldname: String,
        issue: Issue,
        issueTrackingClient: IssueTrackingClient<in T>,
        value: Any?
    ) {
        if (value !is Pair<*, *>) {
            throw IssueClientException("This mapper can only set Pair values, got $value instead")
        }
        val properties = fieldname.split(",")
        when {
            properties.size == 1 && value.second == null -> issueTrackingClient.setValue(
                proprietaryIssueBuilder,
                issue,
                properties[0],
                value.first
            )
            properties.size == 1 && value.second != null -> issueTrackingClient.setValue(
                proprietaryIssueBuilder,
                issue,
                properties[0],
                merge(value)
            )
            properties.size == 2 && value.second != null -> {
                issueTrackingClient.setValue(proprietaryIssueBuilder, issue, properties[0], value.first)
                issueTrackingClient.setValue(proprietaryIssueBuilder, issue, properties[1], value.second)
            }
            properties.size == 2 && value.second == null -> {
                val values = split(value)
                issueTrackingClient.setValue(proprietaryIssueBuilder, issue, properties[0], values[0])
                issueTrackingClient.setValue(proprietaryIssueBuilder, issue, properties[1], values[1])
            }
            else -> throw IssueClientException("Found inconsistent mapper state mapping $value to $fieldname")
        }
    }

    private fun merge(priorityAndSeverity: Pair<*, *>): Any? {
        val keyFirstSecond = "${priorityAndSeverity.first},${priorityAndSeverity.second}"
        val keySecondFirst = "${priorityAndSeverity.second},${priorityAndSeverity.first}"
        return associations[keyFirstSecond] ?: associations[keySecondFirst] ?: associations[keyFallback]
        ?: throw IssueClientException("No association found for $priorityAndSeverity")
    }

    private fun split(priorityAndSeverity: Pair<*, *>): List<String> {
        val keyFirst = "${priorityAndSeverity.first}"
        return (associations[keyFirst] ?: associations["*"])?.split(",")
            ?: throw IssueClientException("No association found for $keyFirst")
    }
}