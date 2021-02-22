package ch.loewenfels.issuetrackingsync.executor.preactions

import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient
import ch.loewenfels.issuetrackingsync.syncconfig.PreActionDefinition

class MultipleTargetIssuesIgnoreWhenFieldsPreAction(definition: PreActionDefinition) : PreAction {
    private var fields: List<Map<String, String>> = emptyList()

    override fun execute(event: PreActionEvent) {
        val targetClient = event.targetClient
        val issue = event.issue
        if (issue.keyFieldMapping != null) {
            val keyFieldMapping = issue.keyFieldMapping!!
            val fieldName = keyFieldMapping.getTargetFieldname()
            val fieldValue = keyFieldMapping.getKeyForTargetIssue().toString()
            val issues = targetClient.searchProprietaryIssues(fieldName, fieldValue)


            val availableIssues = issues.filter {
                fields.size == fields.filter { obj ->
                    val objField = obj["field"] ?: error("Property field must be set")
                    val objValues = obj["value"] ?: error("Property value must be set")
                    objValues.split(",").contains(getValue(targetClient, it, objField)).not()
                }.size
            }

            if (issues.size > 1 && availableIssues.size == 1)
                issue.proprietaryTargetInstance = availableIssues[0]
        }
    }

    init {
        val parameters = definition.parameters
        @Suppress("UNCHECKED_CAST")
        fields = parameters["fields"] as List<Map<String, String>>
    }

    private fun getValue(client: IssueTrackingClient<Any>, issue: Any, fieldName: String): Any? {
        return when (fieldName) {
            "state" -> client.getState(issue)
            else -> client.getValue(issue, fieldName)
        }
    }
}