package ch.loewenfels.issuetrackingsync.executor.fields

import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient
import ch.loewenfels.issuetrackingsync.syncconfig.FieldMappingDefinition

class DirectListMergeFieldRegexTransformMapper(fieldMappingDefinition: FieldMappingDefinition) :
    DirectListMergeFieldMapper() {
    private val onlyLast = fieldMappingDefinition.associations["onlyLast"]
    private val toUpperCase = fieldMappingDefinition.associations["toUpperCase"]
    private val toLowerCase = fieldMappingDefinition.associations["toLowerCase"]

    @Suppress("UNCHECKED_CAST")
    override fun <T> setValue(
        proprietaryIssueBuilder: Any,
        fieldname: String,
        issue: Issue,
        issueTrackingClient: IssueTrackingClient<in T>,
        value: Any?
    ) {
        var newValues = mutableSetOf<Any>()
        addToSet(newValues, value)
        toUpperCase?.let {
            newValues = newValues.map { newValue ->
                var result = newValue
                it.toRegex().find(newValue as String)?.value?.let { found ->
                    result = newValue.replace(found, found.uppercase())
                }
                result
            }.toMutableSet()
        }
        toLowerCase?.let {
            newValues = newValues.map { newValue ->
                var result = newValue
                it.toRegex().find(newValue as String)?.value?.let { found ->
                    result = newValue.replace(found, found.lowercase())
                }
                result
            }.toMutableSet()
        }

        super.setValue(
            proprietaryIssueBuilder,
            fieldname,
            issue,
            issueTrackingClient,
            newValues.toList()
        )
    }

    override fun addToSet(setNewValues: MutableSet<Any>, toMergeIntoNewValues: Any?) {
        toMergeIntoNewValues?.let {
            if (toMergeIntoNewValues is Collection<*>) {
                val toAdd = toMergeIntoNewValues.filterNotNull()
                toAdd.forEach {
                    removeIfOnlyLastEnabled(setNewValues, it)
                }
                setNewValues.addAll(toAdd)
            } else {
                removeIfOnlyLastEnabled(setNewValues, toMergeIntoNewValues)
                setNewValues.add(toMergeIntoNewValues)
            }
        }
    }

    private fun removeIfOnlyLastEnabled(setNewValues: MutableSet<Any>, toAdd: Any) {
        onlyLast?.let { regexExpr ->
            if (regexExpr.toRegex().find(toAdd as String)?.value != null) {
                setNewValues.removeIf { regexExpr.toRegex().find(it as String)?.value != null }
            }
        }
    }
}