package ch.loewenfels.issuetrackingsync.custom

import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.executor.fields.FieldMapper
import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient
import ch.loewenfels.issuetrackingsync.syncclient.rtc.RtcClient
import ch.loewenfels.issuetrackingsync.syncconfig.FieldMappingDefinition
import com.ibm.team.repository.common.IContributorHandle
import com.ibm.team.repository.common.IItemHandle
import com.ibm.team.repository.common.model.impl.ItemImpl
import com.ibm.team.workitem.common.model.IWorkItem

class RtcSyncContributorTargetFieldToTargetMapper(fieldMappingDefinition: FieldMappingDefinition) : FieldMapper {
    private var sourceTargetField: String? = null

    override fun <T> getValue(
            proprietaryIssue: T,
            fieldname: String,
            issueTrackingClient: IssueTrackingClient<in T>
    ): Any? {
        sourceTargetField = fieldname

        return null
    }

    override fun <T> setValue(proprietaryIssueBuilder: Any, fieldname: String, issue: Issue, issueTrackingClient: IssueTrackingClient<in T>, value: Any?) {
        val trackingClient = issueTrackingClient as RtcClient
        @Suppress("UNCHECKED_CAST")
        val propIssue = issue.proprietaryTargetInstance as? IWorkItem
        if (null != propIssue) {
            val contributor = sourceTargetField?.let { trackingClient.getNonConvertedValue(propIssue, it) }
            contributor?.let {
                check(contributor is IContributorHandle) {
                    "Failed to change ${fieldname}. Source filed must be of type ${IContributorHandle::class}";
                }
                val oldValue = trackingClient.getNonConvertedValue(propIssue, fieldname)
                check(oldValue == null || oldValue is IContributorHandle) {
                    "Failed to change ${fieldname}. Target filed must be of type ${IContributorHandle::class}";
                }
                if (null == oldValue || (oldValue as IContributorHandle).sameItemId(contributor as IItemHandle?).not()) {
                    trackingClient.setValue(proprietaryIssueBuilder, issue, fieldname, it)
                }
            }
        }
    }
}