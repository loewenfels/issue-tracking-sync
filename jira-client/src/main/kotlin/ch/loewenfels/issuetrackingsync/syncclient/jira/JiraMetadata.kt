package ch.loewenfels.issuetrackingsync.syncclient.jira

import ch.loewenfels.issuetrackingsync.syncclient.IssueClientException
import com.atlassian.jira.rest.client.api.JiraRestClient
import com.atlassian.jira.rest.client.api.domain.Version

class JiraMetadata {
    companion object {
        private val issueTypes: MutableMap<String, Long> = mutableMapOf()
        private val priorities: MutableMap<String, Long> = mutableMapOf()
        private val fieldTypes: MutableMap<String, String> = mutableMapOf()
        private val fieldCustom: MutableMap<String, String> = mutableMapOf()
        private val projectVersions: MutableMap<String, Version> = mutableMapOf()

        fun getIssueTypeId(name: String, jiraRestClient: JiraRestClient): Long =
            getId(issueTypes, "issue type", name, jiraRestClient)

        fun getPriorityId(name: String, jiraRestClient: JiraRestClient): Long =
            getId(priorities, "priority", name, jiraRestClient)

        fun getVersionEntity(name: List<*>, jiraRestClient: JiraRestClient, projectKey: String?): List<Version> {
            return name.mapNotNull {
                projectVersions[it] ?: run {
                    projectKey?.run {
                        loadVersions(jiraRestClient, projectKey)
                        projectVersions[it] ?: throw IssueClientException("Unknown version $name")
                    }
                }
            }
        }

        private fun getId(
            collection: MutableMap<String, Long>,
            property: String,
            name: String,
            jiraRestClient: JiraRestClient
        ): Long {
            return name.toLongOrNull()
                ?: collection[name]
                ?: run {
                    loadIssueTypes(jiraRestClient)
                    loadPriorities(jiraRestClient)
                    name.toLongOrNull()
                        ?: collection[name]
                        ?: throw IssueClientException("Unknown $property $name")
                }
        }

        fun getFieldCustom(internalId: String, jiraRestClient: JiraRestClient): String =
            getName(fieldCustom, "customfield", internalId, null, jiraRestClient)

        fun getPriorityName(internalId: Long, jiraRestClient: JiraRestClient): String =
            getName(priorities, "priority", null, internalId, jiraRestClient)

        fun getFieldType(internalId: String, jiraRestClient: JiraRestClient): String =
            getName(fieldTypes, "field", internalId, null, jiraRestClient)

        private fun getName(
            collection: MutableMap<*, *>,
            property: String,
            internalName: String?,
            internalId: Long?,
            jiraRestClient: JiraRestClient
        ): String {
            return collection[internalName] as String?
                ?: collection.filterValues { it == internalId }.keys.firstOrNull() as String?
                ?: run {
                    loadPriorities(jiraRestClient)
                    loadFields(jiraRestClient)
                    collection.filterValues { it == internalId }.keys.firstOrNull() as String?
                        ?: collection[internalName] as String?
                        ?: throw IssueClientException("Unknown $property ${internalId ?: internalName}")
                }
        }

        @kotlin.jvm.Synchronized
        private fun loadVersions(jiraRestClient: JiraRestClient, projectKey: String) {
            jiraRestClient.projectClient.getProject(projectKey).claim().versions.forEach {
                projectVersions[it.name] = it
            }
        }


        @kotlin.jvm.Synchronized
        private fun loadIssueTypes(jiraRestClient: JiraRestClient) {
            jiraRestClient.metadataClient.issueTypes.claim().forEach {
                issueTypes[it.name] = it.id
            }
        }

        @kotlin.jvm.Synchronized
        private fun loadPriorities(jiraRestClient: JiraRestClient) {
            jiraRestClient.metadataClient.priorities.claim().forEach {
                priorities[it.name] = it.id ?: 0L
            }
        }

        @kotlin.jvm.Synchronized
        private fun loadFields(jiraRestClient: JiraRestClient) {
            jiraRestClient.metadataClient.fields.claim().forEach { f ->
                fieldTypes[f.id] = f.schema?.type ?: ""
                fieldCustom[f.id] = f.schema?.custom ?: ""
            }
        }
    }

}