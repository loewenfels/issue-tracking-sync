package ch.loewenfels.issuetrackingsync.executor.fields

import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient
import ch.loewenfels.issuetrackingsync.syncconfig.FieldMappingDefinition

class KeyFieldMapping(
    sourceName: String,
    targetName: String,
    mapper: FieldMapper,
    callback: FieldMappingDefinition? = null
) : FieldMapping(sourceName, targetName, mapper) {
    private var callback = callback?.let { FieldMappingFactory.getKeyMapping(it) }
    fun getSourceFieldname(): String = sourceName
    fun getKeyForTargetIssue(): Any? = sourceValue
    fun getTargetFieldname(): String = targetName
    fun getCallback(): KeyFieldMapping? = callback
    fun invertMapping(): KeyFieldMapping {
        val inverted = KeyFieldMapping(targetName, sourceName, mapper)
        inverted.callback = callback?.invertMapping()
        return inverted
    }

    fun <T> loadSourceValueWithCallback(issue: Issue, issueTrackingClient: IssueTrackingClient<in T>) {
        loadSourceValue(issue, issueTrackingClient)
        callback?.loadSourceValueWithCallback(issue, issueTrackingClient)
    }
}