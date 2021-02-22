package ch.loewenfels.issuetrackingsync.custom

import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.executor.fields.FieldValueRegexTransformationMapper
import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient
import ch.loewenfels.issuetrackingsync.syncclient.jira.JiraClient
import ch.loewenfels.issuetrackingsync.syncclient.rtc.RtcClient
import ch.loewenfels.issuetrackingsync.syncconfig.FieldMappingDefinition
import com.atlassian.jira.rest.client.api.domain.Version
import com.ibm.team.workitem.common.model.IWorkItem
import org.codehaus.jettison.json.JSONObject

class FieldSingleTargetVersionMapper(fieldMappingDefinition: FieldMappingDefinition) :
    FieldValueRegexTransformationMapper(fieldMappingDefinition) {

    override fun <T> setValue(
        proprietaryIssueBuilder: Any,
        fieldname: String,
        issue: Issue,
        issueTrackingClient: IssueTrackingClient<in T>,
        value: Any?
    ) {
        when (issueTrackingClient) {
            is RtcClient -> mergeLogicToRtc(proprietaryIssueBuilder, fieldname, issue, issueTrackingClient, value)
            is JiraClient -> mergeLogicToJira(proprietaryIssueBuilder, fieldname, issue, issueTrackingClient, value)
        }
    }

    private fun mergeLogicToRtc(
        proprietaryIssueBuilder: Any,
        fieldname: String,
        issue: Issue,
        issueTrackingClient: RtcClient,
        jiraVersion: Any?
    ) {
        // check if at least one jira version is set
        if (jiraVersion is JSONObject) {
            val jiraVersionName = if (jiraVersion.has("name")) jiraVersion.get("name") as String else null
            jiraVersionName?.let {
                val rtcValue: String? = super.getValue(
                    issue.proprietaryTargetInstance as IWorkItem,
                    fieldname,
                    issueTrackingClient,
                    mapOf("(.*)" to "$1")
                ) as String?
                val rtcVersion =
                    "^I\\d{4}\\.\\d+ - (\\d\\.\\d{2,3}.*)".toRegex().find(rtcValue ?: "")?.groupValues?.get(1) ?: ""
                if (it != rtcVersion) {
                    mergeToRtc(it, issue, issueTrackingClient, proprietaryIssueBuilder, fieldname)
                }
            }

        }
    }

    private fun isJiraIssueSolved(issue: Issue): Boolean {
        val jiraStatus = getJiraStatus(issue)
        return jiraStatus == "erledigt" || jiraStatus == "geschlossen"
    }

    private fun getJiraStatus(issue: Issue): String =
        (issue.proprietarySourceInstance as com.atlassian.jira.rest.client.api.domain.Issue).status.name

    private fun mergeToRtc(
        jiraVersion: String,
        issue: Issue,
        issueTrackingClient: RtcClient,
        proprietaryIssueBuilder: Any,
        fieldname: String
    ) {
        val mappedRtcVersion = issueTrackingClient.getAllDeliverables()
            .map { it.name }
            .firstOrNull { it.endsWith(jiraVersion) }
        checkNotNull(mappedRtcVersion) { "The version $jiraVersion is not yet defined for RTC." }
        super.setValue(proprietaryIssueBuilder, fieldname, issue, issueTrackingClient, mappedRtcVersion)
    }

    private fun mergeLogicToJira(
        proprietaryIssueBuilder: Any,
        fieldname: String,
        issue: Issue,
        issueTrackingClient: JiraClient,
        rtcVersion: Any?
    ) {
        if (rtcVersion is String) {
            val jiraVersions = issue.proprietaryTargetInstance?.run {
                issueTrackingClient.getMultiSelectValues(
                    issue.proprietaryTargetInstance as com.atlassian.jira.rest.client.api.domain.Issue,
                    fieldname
                )
            } ?: emptyList()
            if (jiraVersions.contains(rtcVersion).not()) {
                val regexMinorVersion = "^\\d\\.\\d{2,3}(?!\\.)".toRegex()
                val regexBugfixVersion = "\\d\\.\\d{2,3}\\.\\d*(?!\\.)".toRegex()
                val regexRemoveValue = "(Backlog-?F?C?B?)".toRegex()
                val validTransformation = associations.keys.any { it.toRegex().containsMatchIn(rtcVersion) }
                check(
                    regexMinorVersion.containsMatchIn(rtcVersion)
                            || regexBugfixVersion.containsMatchIn(rtcVersion)
                            || validTransformation
                ) {
                    "No valid version ($rtcVersion) for issue ${issue.key} found."
                }
                val valueToWrite = jiraVersions.toMutableList()
                valueToWrite.add(rtcVersion)
                if (valueToWrite.any { regexMinorVersion.containsMatchIn(it) || regexBugfixVersion.containsMatchIn(it) }
                    && !regexRemoveValue.containsMatchIn(rtcVersion)
                ) {
                    valueToWrite.removeIf { regexRemoveValue.containsMatchIn(it) }
                }
                super.setValue(
                    proprietaryIssueBuilder,
                    fieldname,
                    issue,
                    issueTrackingClient,
                    valueToWrite
                )
            }
        }
    }

    private fun createVersion(version: String): Version? {
        val versionRegex = "^(\\d)\\.(\\d+)\\.?(\\d*(?!.*\\.nil))".toRegex()
        val extractedVersions = versionRegex.find(version)?.groupValues?.mapNotNull { it.toIntOrNull() }
        extractedVersions?.let {
            if (extractedVersions.size >= 2)
                return Version(extractedVersions[0], extractedVersions[1], extractedVersions.getOrNull(2))
        }
        return null
    }

    inner class Version(val minor: Int, val major: Int, val bugfix: Int?) : Comparable<Version> {
        override fun compareTo(other: Version): Int {
            if (this.minor != other.minor) {
                return this.minor - other.minor
            } else if (this.major != other.major) {
                return this.major - other.major
            } else if (this.getBugFixOrZero() != other.getBugFixOrZero()) {
                return this.getBugFixOrZero() - other.getBugFixOrZero()
            }
            return 0
        }

        fun getBugFixOrZero() = bugfix ?: 0

        override fun toString(): String {
            val delemiter = "."
            return minor.toString() + delemiter + major.toString() + (bugfix?.let { delemiter + it } ?: "")
        }
    }

}
