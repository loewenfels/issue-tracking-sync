package ch.loewenfels.issuetrackingsync.custom

import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.executor.fields.CompoundStringFieldMapper
import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient
import ch.loewenfels.issuetrackingsync.syncconfig.FieldMappingDefinition

class KeepTitleFromDestinationCompoundStringFieldMapping(fieldMappingDefinition: FieldMappingDefinition) :
    CompoundStringFieldMapper(fieldMappingDefinition) {

    override fun <T> createSections(
        value: Any?,
        fieldname: String,
        issueTrackingClient: IssueTrackingClient<in T>,
        issue: Issue
    ): MutableMap<String, String> {
        val sections = super.createSections(value, fieldname, issueTrackingClient, issue)
        if (fieldname.split(",").size == 1) {
            sections[fieldname]?.let {
                var preparedHtml = issueTrackingClient.prepareHtmlValue(it)
                if (null != anchorOpen && null != anchorClose) {
                    preparedHtml = "$anchorOpen\r\n\r\n$preparedHtml\r\n\r\n$anchorClose"
                }
                sections[fieldname] = preparedHtml
            }
            if (issue.proprietaryTargetInstance != null) {
                val oldVal = (issueTrackingClient.getValue(issue.proprietaryTargetInstance as T, fieldname) ?: "").toString()
                if (null != anchorOpen && null != anchorClose) {
                    val indexOfOpen = oldVal.indexOf(anchorOpen)
                    val indexOfClose = oldVal.lastIndexOf(anchorClose)
                    if (indexOfOpen > -1 && indexOfClose > -1) {
                        val sectionFieldNameValue = sections[fieldname] ?: ""
                        sections[fieldname] = oldVal.replaceRange(
                                indexOfOpen,
                                indexOfClose + anchorClose.length,
                                sectionFieldNameValue
                        )
                    } else if (null != associations[""]) {
                        var preserveOldValue = oldVal
                        val catchAll = associations[""]!!
                        val indexOf = oldVal.indexOf(catchAll)
                        var oldValueComment = sections[fieldname]
                        if (indexOf >= 0) {
                            oldValueComment = oldVal.substring(0, indexOf).trim()
                            preserveOldValue = oldVal.substring(indexOf + catchAll.length)
                        }

                        sections[fieldname] = "$preserveOldValue\r\n\r\n$anchorOpen\r\n\r\n${oldValueComment}\r\n\r\n$anchorClose"
                    } else {
                        sections[fieldname] = "$oldVal\r\n\r\n$anchorOpen\r\n\r\n${sections[fieldname]}\r\n\r\n$anchorClose"
                    }
                } else {
                    associations[""]?.let {
                        val indexOf = oldVal.indexOf(it)
                        if (indexOf >= 0) {
                            sections[fieldname] =
                                    sections[fieldname] + oldVal.substring(oldVal.indexOf(it))
                        } else {
                            sections[fieldname] = sections[fieldname] + it + "\r\n\r\n"
                        }
                    }
                }
            }
        }
        return sections
    }

    override fun <T> getValue(
        proprietaryIssue: T,
        fieldname: String,
        issueTrackingClient: IssueTrackingClient<in T>
    ): Any? {
        val value = super.getValue(proprietaryIssue, fieldname, issueTrackingClient) as String
        associations[""]?.let {
            val indexOf = value.indexOf(it)
            if (indexOf >= 0) {
                return value.substring(0, indexOf)
            }
        }
        return value
    }

    override fun <T> setValue(
            proprietaryIssueBuilder: Any,
            fieldname: String,
            issue: Issue,
            issueTrackingClient: IssueTrackingClient<in T>,
            value: Any?
    ) {
        val sections: MutableMap<String, String> = createSections(value, fieldname, issueTrackingClient, issue)
        fieldname.split(",").forEach {
            issueTrackingClient.setValue(proprietaryIssueBuilder, issue, fieldname, sections[it]?.trim() ?: "")
        }
    }
}
