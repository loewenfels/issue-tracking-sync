package ch.loewenfels.issuetrackingsync.executor.fields

import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient
import ch.loewenfels.issuetrackingsync.syncconfig.FieldMappingDefinition

/**
 *  This class checks current value of field which needs to be updated and if new value is not present than append it.
 */
class AppendValueFieldMapper(fieldMappingDefinition: FieldMappingDefinition): FieldMapper {
    val associations = fieldMappingDefinition.associations

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
        val oldValue = getValueFromTarget(issue, fieldname, issueTrackingClient)
        val newValue = value.toString()
        var result = "${if (oldValue == null || oldValue.isEmpty()) "" else "$oldValue;"}${newValue}"
        oldValue?.let {
            result = when {
                getRegexPatter() != null -> {
                    val pattern = getRegexPatter()!!
                    val fResult = pattern.toRegex().find(oldValue)
                    if (fResult?.value != null) {
                        oldValue.replace(fResult.value, newValue)
                    } else {
                        result
                    }
                }
                else -> result
            }
        }
        issueTrackingClient.setValue(proprietaryIssueBuilder, issue, fieldname, result)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getValueFromTarget(
            issue: Issue,
            fieldname: String,
            issueTrackingClient: IssueTrackingClient<in T>
    ): String? {
        return getValue(
                issue.proprietaryTargetInstance as T,
                fieldname,
                issueTrackingClient
        )?.toString()
    }

    private fun getRegexPatter(): String? {
        return associations["pattern"]
    }

}