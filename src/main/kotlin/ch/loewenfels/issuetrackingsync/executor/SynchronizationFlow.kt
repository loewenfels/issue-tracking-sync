package ch.loewenfels.issuetrackingsync.executor

import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.Logging
import ch.loewenfels.issuetrackingsync.SynchronizationAbortedException
import ch.loewenfels.issuetrackingsync.executor.actions.SimpleSynchronizationAction
import ch.loewenfels.issuetrackingsync.executor.actions.SynchronizationAction
import ch.loewenfels.issuetrackingsync.executor.fields.FieldMappingFactory
import ch.loewenfels.issuetrackingsync.notification.NotificationObserver
import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient
import ch.loewenfels.issuetrackingsync.syncconfig.DefaultsForNewIssue
import ch.loewenfels.issuetrackingsync.syncconfig.SyncActionDefinition
import ch.loewenfels.issuetrackingsync.syncconfig.SyncFlowDefinition
import ch.loewenfels.issuetrackingsync.syncconfig.TrackingApplicationName
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.math.abs

typealias SyncActionName = String

/**
 * This class is the main work horse of issue synchronization. The internal behavior derives from the configured
 * [SyncFlowDefinition], delegating the work to a list of [SynchronizationAction].
 *
 * The internal state of this class must be immutable, as the [execute] method is called from multiple threads
 * and for multiple issues.
 */
class SynchronizationFlow(
    private val syncFlowDefinition: SyncFlowDefinition,
    private val actionDefinitions: List<SyncActionDefinition>,
    private val sourceClient: IssueTrackingClient<Any>,
    private val targetClient: IssueTrackingClient<Any>,
    private val notificationObserver: NotificationObserver
) : Logging {
    private val sourceApplication: TrackingApplicationName = syncFlowDefinition.source
    private val syncActions: Map<SyncActionName, SynchronizationAction>
    private val issueFilter: IssueFilter?
    private val defaultsForNewIssue: DefaultsForNewIssue?

    companion object {
        const val syncAbortThreshold = 5
    }

    init {
        syncActions = syncFlowDefinition.actions.associateBy({ it }, { buildSyncAction(it, actionDefinitions) })
        issueFilter = syncFlowDefinition.filterClassname?.let {
            @Suppress("UNCHECKED_CAST")
            val filterClass = Class.forName(it) as Class<IssueFilter>
            filterClass.getDeclaredConstructor().newInstance()
        }
        defaultsForNewIssue = syncFlowDefinition.defaultsForNewIssue
    }

    @Suppress("UNCHECKED_CAST")
    private fun buildSyncAction(
        actionName: String,
        actionDefinitions: List<SyncActionDefinition>
    ): SynchronizationAction {
        val actionDefinition = actionDefinitions.first { it.name.equals(actionName, ignoreCase = true) }
        val actionClass = Class.forName(actionDefinition.classname) as Class<SynchronizationAction>
        return try {
            actionClass.getConstructor(String::class.java).newInstance(actionName)
        } catch (ignore: Exception) {
            // no ctor taking a FieldMappingDefinition, so look for empty ctor
            try {
                actionClass.getDeclaredConstructor().newInstance()
            } catch (ignore: Exception) {
                throw IllegalArgumentException(
                    "Failed to instantiate action class ${actionDefinition.classname}",
                    ignore
                )
            }
        }
    }

    fun applies(source: TrackingApplicationName, issue: Issue): Boolean {
        return Objects.equals(sourceApplication, source) &&
                issueFilter?.test(sourceClient, issue, syncFlowDefinition) ?: true
    }

    fun execute(issue: Issue) {
        try {
            loadInternalSourceIssue(issue)
            syncActions.forEach { execute(it, issue) }
            notificationObserver.notifySuccessfulSync(issue, syncActions)
        } catch (ex: Exception) {
            sourceClient.logException(issue, ex, notificationObserver, syncActions)
        } finally {
            writeBackKeyReference(issue)
        }
    }

    private fun loadInternalSourceIssue(issue: Issue, checkLastUpdatedTimestamp: Boolean = true) {
        val sourceIssue =
            sourceClient.getProprietaryIssue(issue.key) ?: throw IllegalArgumentException("No source issue found")
        if (checkLastUpdatedTimestamp) {
            val gap = issue.lastUpdated.until(sourceClient.getLastUpdated(sourceIssue), ChronoUnit.SECONDS)
            if (abs(gap) > syncAbortThreshold) {
                throw SynchronizationAbortedException("Issues have been updated since synchronization request")
            }
        }
        issue.proprietarySourceInstance = sourceIssue
        issue.sourceUrl = sourceClient.getIssueUrl(sourceIssue)
        val keyFieldMapping = FieldMappingFactory.getKeyMapping(syncFlowDefinition.keyFieldMappingDefinition)
        keyFieldMapping.loadSourceValue(issue, sourceClient)
        issue.keyFieldMapping = keyFieldMapping
    }

    private fun execute(syncActionEntry: Map.Entry<SyncActionName, SynchronizationAction>, issue: Issue) {
        val actionDefinition = actionDefinitions.first { it.name.equals(syncActionEntry.key, ignoreCase = true) }
        val fieldMappings = actionDefinition.fieldMappingDefinitions.map { FieldMappingFactory.getMapping(it) }.toList()
        syncActionEntry.value.execute(
            sourceClient,
            targetClient,
            issue,
            fieldMappings,
            defaultsForNewIssue,
            actionDefinition.additionalProperties
        )
        issue.fieldMappings.clear()
    }

    private fun writeBackKeyReference(issue: Issue) {
        try {
            updateKeyReferenceOnTarget(issue)
            updateKeyReferenceOnSource(issue)
        } catch (ex: Exception) {
            sourceClient.logException(issue, ex, notificationObserver, syncActions)
        }
    }

    private fun updateKeyReferenceOnTarget(issue: Issue) {
        issue.keyFieldMapping?.let {
            val fieldMapping = listOf(it)
            // use a clone here so we don't attempt to re-update all previously mapped fields
            val issueClone = Issue(issue.key, "", issue.lastUpdated)
            loadInternalSourceIssue(issueClone, false)
            SimpleSynchronizationAction("SourceReferenceOnTarget").execute(
                sourceClient,
                targetClient,
                issueClone,
                fieldMapping,
                defaultsForNewIssue
            )
        }
        issue.fieldMappings.clear()
    }

    private fun updateKeyReferenceOnSource(issue: Issue) {
        if (issue.proprietaryTargetInstance != null) {
            issue.targetKey = issue.proprietaryTargetInstance?.let { targetClient.getKey(it) }
            syncFlowDefinition.writeBackFieldMappingDefinition?.let { writeBack ->
                val invertedIssue = Issue(issue.targetKey!!, "", issue.lastUpdated)
                invertedIssue.proprietarySourceInstance = issue.proprietaryTargetInstance
                invertedIssue.proprietaryTargetInstance = issue.proprietarySourceInstance
                val invertedKeyMapping = FieldMappingFactory.getKeyMapping(writeBack)
                invertedKeyMapping.loadSourceValue(invertedIssue, targetClient)
                invertedIssue.keyFieldMapping = invertedKeyMapping
                val fieldMappings = listOf(invertedKeyMapping)
                SimpleSynchronizationAction("ReferenceWriteBack").execute(
                    targetClient,
                    sourceClient,
                    invertedIssue,
                    fieldMappings,
                    defaultsForNewIssue
                )
            }
        }
    }
}