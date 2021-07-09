package ch.loewenfels.issuetrackingsync.executor.preactions

import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient
import ch.loewenfels.issuetrackingsync.syncconfig.PreActionDefinition

class MultipleSourceIssuesIgnoreWhenFieldsPreAction(definition: PreActionDefinition) : PreAction {
    private var externalFieldName: String? = null
    private var fields: List<Map<String, String>> = emptyList()

    override fun execute(event: PreActionEvent) {
        val client = event.sourceClient
        val issue = event.issue
        if (issue.keyFieldMapping != null) {
            issue.keyFieldMapping!!.getCallback()?.let {
                val fieldName = it.getSourceFieldname()
                val fieldValue = it.getKeyForTargetIssue().toString()
                val issues = client.searchProprietaryIssues(fieldName, fieldValue)
                if (issues.size > 1) {
                    issue.proprietarySourceInstance?.let { sourceIssue ->
                        val validFields = fields.filter { obj ->
                            val objField = obj["field"] ?: error("Property field must be set")
                            val objValues = obj["value"] ?: error("Property value must be set")
                            objValues.split(",").contains(getValue(client, sourceIssue, objField))
                        }
                        if (fields.size == validFields.size) {
                            event.stopSynchronization()
                        }
                    }
                }
            }
            externalFieldName?.let {
                issue.proprietarySourceInstance?.let { sourceIssue ->
                    val fieldValue = client.getValue(sourceIssue, it).toString()
                    val issues = client.searchProprietaryIssues(it, fieldValue)
                    if (issues.size > 1) {
                        val validFields = fields.filter { obj ->
                            val objField = obj["field"] ?: error("Property field must be set")
                            val objValues = obj["value"] ?: error("Property value must be set")
                            objValues.split(",").contains(getValue(client, sourceIssue, objField))
                        }
                        if (fields.size == validFields.size) {
                            event.stopSynchronization()
                        }
                    }
                }
            }
        }
    }

    init {
        val parameters = definition.parameters
        @Suppress("UNCHECKED_CAST")
        fields = parameters["fields"] as List<Map<String, String>>
        if (parameters["externalRefFieldName"] != null) {
            externalFieldName = parameters["externalRefFieldName"].toString()
        }
    }

    private fun getValue(client: IssueTrackingClient<Any>, issue: Any, fieldName: String): Any? {
        return when (fieldName) {
            "resolution" -> client.getValue(issue, fieldName)
            "state" -> client.getState(issue)
            else -> client.getValue(issue, fieldName)
        }
    }
}