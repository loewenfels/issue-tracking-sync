package ch.loewenfels.issuetrackingsync.executor

import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.Logging
import ch.loewenfels.issuetrackingsync.SynchronizationAbortedException
import ch.loewenfels.issuetrackingsync.executor.actions.SimpleSynchronizationAction
import ch.loewenfels.issuetrackingsync.executor.actions.SynchronizationAction
import ch.loewenfels.issuetrackingsync.executor.fields.FieldMappingFactory
import ch.loewenfels.issuetrackingsync.executor.preactions.PreAction
import ch.loewenfels.issuetrackingsync.executor.preactions.PreActionEvent
import ch.loewenfels.issuetrackingsync.notification.NotificationObserver
import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient
import ch.loewenfels.issuetrackingsync.syncconfig.*
import java.io.Closeable
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
) : Closeable, Logging {
    private val sourceApplication: TrackingApplicationName = syncFlowDefinition.source
    private val syncPreActions: List<PreAction>
    private val syncActions: Map<SyncActionName, SynchronizationAction>
    private val issueFilter: IssueFilter?
    private val defaultsForNewIssue: DefaultsForNewIssue?

    companion object {
        const val syncAbortThreshold = 5
    }

    init {
        syncPreActions = syncFlowDefinition.preActions.map { buildSyncPreAction(it) }
        syncActions = syncFlowDefinition.actions.associateBy({ it }, { buildSyncAction(it, actionDefinitions) })
        issueFilter = syncFlowDefinition.filterClassname?.let {
            @Suppress("UNCHECKED_CAST")
            val filterClass = Class.forName(it) as Class<IssueFilter>
            filterClass.getDeclaredConstructor().newInstance()
        }
        issueFilter?.defineParameters(syncFlowDefinition.filterProperties)
        defaultsForNewIssue = syncFlowDefinition.defaultsForNewIssue
    }

    @Suppress("UNCHECKED_CAST")
    private fun buildSyncPreAction(preActionDefinition: PreActionDefinition): PreAction {
        val preActionClass = Class.forName(preActionDefinition.className) as Class<PreAction>

        return try {
            preActionClass.getConstructor(PreActionDefinition::class.java).newInstance(preActionDefinition)
        } catch (ignore: Exception) {
            // no ctor taking a PreActionDefinition, so look for empty ctor
            try {
                preActionClass.getDeclaredConstructor().newInstance()
            } catch (e: Exception) {
                throw IllegalArgumentException(
                    "Failed to instantiate pre action class ${preActionDefinition.className}",
                    e
                )
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun buildSyncAction(
        actionName: String,
        actionDefinitions: List<SyncActionDefinition>
    ): SynchronizationAction {
        val actionDefinition = actionDefinitions.first { it.name.equals(actionName, ignoreCase = true) }
        return try {
            val actionClass = Class.forName(actionDefinition.classname) as Class<SynchronizationAction>
            (actionClass.constructors.find { c -> c.parameterCount == 1 }?.newInstance(actionName)
                ?: actionClass.constructors.first { c -> c.parameterCount == 0 }.newInstance()) as SynchronizationAction
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to instantiate action class ${actionDefinition.classname}", e)
        }
    }

    fun applies(source: TrackingApplicationName, issue: Issue): Boolean {
        return Objects.equals(sourceApplication, source) &&
                issueFilter?.test(sourceClient, issue, syncFlowDefinition) ?: true
    }

    @Suppress("TooGenericExceptionCaught")
    fun execute(issue: Issue) {
        try {
            loadInternalSourceIssue(issue)
            val event = PreActionEvent(sourceClient, targetClient, issue)
            for (preAction in syncPreActions) {
                preAction.execute(event)
                if (event.isStopPropagation) {
                    break
                }
            }
            if (event.isStopSynchronization) {
                return
            }
            syncActions.forEach {
                try {
                    execute(it, issue)
                } catch (e: Exception) {
                    if (sourceClient.logException(issue, e, notificationObserver, syncActions))
                    else if (targetClient.logException(issue, e, notificationObserver, syncActions))
                    else notificationObserver.notifyException(issue, e, syncActions)
                }
            }
            notificationObserver.notifySuccessfulSync(issue, syncActions)
        } catch (ex: Exception) {
            if (sourceClient.logException(issue, ex, notificationObserver, syncActions))
            else if (targetClient.logException(issue, ex, notificationObserver, syncActions))
            else notificationObserver.notifyException(issue, ex, syncActions)
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
        keyFieldMapping.loadSourceValueWithCallback(issue, sourceClient)
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

    @Suppress("TooGenericExceptionCaught")
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
            val keyMapping = listOf(it)
            // use a clone here so we don't attempt to re-update all previously mapped fields
            val issueClone = Issue(issue.key, "", issue.lastUpdated)
            loadInternalSourceIssue(issueClone, false)
            SimpleSynchronizationAction("SourceReferenceOnTarget").execute(
                sourceClient,
                targetClient,
                issueClone,
                keyMapping,
                defaultsForNewIssue
            )
        }
        issue.fieldMappings.clear()
    }

    private fun updateKeyReferenceOnSource(issue: Issue) {
        if (issue.proprietaryTargetInstance != null) {
            issue.targetKey = issue.proprietaryTargetInstance?.let { targetClient.getKey(it) }
            syncFlowDefinition.writeBackFieldMappingDefinition.let { writeBack ->
                val invertedIssue = Issue(issue.targetKey!!, "", issue.lastUpdated)
                invertedIssue.proprietarySourceInstance = issue.proprietaryTargetInstance
                invertedIssue.proprietaryTargetInstance = issue.proprietarySourceInstance
                val invertedKeyFieldMapping = issue.keyFieldMapping?.invertMapping()
                val writeBackList = writeBack.map { FieldMappingFactory.getKeyMapping(it) }.toList()
                writeBackList.forEach { it.loadSourceValueWithCallback(invertedIssue, targetClient) }
                invertedIssue.keyFieldMapping = invertedKeyFieldMapping
                SimpleSynchronizationAction("ReferenceWriteBack").execute(
                    targetClient,
                    sourceClient,
                    invertedIssue,
                    writeBackList,
                    defaultsForNewIssue
                )
            }
        }
    }

    override fun close() {
        sourceClient.close()
        targetClient.close()
    }
}