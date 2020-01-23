package ch.loewenfels.issuetrackingsync.testcontext

import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.executor.IssueFilter
import ch.loewenfels.issuetrackingsync.executor.SynchronizationFlowTest
import ch.loewenfels.issuetrackingsync.executor.actions.SimpleSynchronizationAction
import ch.loewenfels.issuetrackingsync.executor.fields.*
import ch.loewenfels.issuetrackingsync.notification.NotificationObserver
import ch.loewenfels.issuetrackingsync.syncclient.ClientFactory
import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient
import ch.loewenfels.issuetrackingsync.syncconfig.*
import org.mockito.Mockito.spy
import java.time.LocalDateTime

object TestObjects {
    private const val simpleActionName = "foobar"

    fun buildSyncFlowDefinition(source: TrackingApplicationName, target: TrackingApplicationName): SyncFlowDefinition {
        val syncFlowDefinition = SyncFlowDefinition()
        syncFlowDefinition.source = source
        syncFlowDefinition.target = target
        syncFlowDefinition.actions = mutableListOf(simpleActionName)
        syncFlowDefinition.defaultsForNewIssue = buildDefaultsForNewIssue()
        syncFlowDefinition.keyFieldMappingDefinition = buildKeyFieldMappingDefinition()
        syncFlowDefinition.writeBackFieldMappingDefinition = buildWriteBackFieldMappingDefinition()
        return syncFlowDefinition
    }

    fun buildSyncActionDefinition(): SyncActionDefinition {
        val syncActionDefinition = SyncActionDefinition()
        syncActionDefinition.name = simpleActionName
        syncActionDefinition.classname = SimpleSynchronizationAction::class.qualifiedName ?: ""
        syncActionDefinition.fieldMappingDefinitions = buildFieldMappingDefinitionList()
        return syncActionDefinition
    }

    private fun buildKeyFieldMappingDefinition(): FieldMappingDefinition =
        FieldMappingDefinition(
            "id",
            "custom_field_10244"
        )

    private fun buildWriteBackFieldMappingDefinition(): FieldMappingDefinition =
        FieldMappingDefinition(
            "key",
            "ch.foobar.team.workitem.attribute.external_refid"
        )

    private fun buildFieldMappingDefinitionList(): MutableList<FieldMappingDefinition> =
        mutableListOf(buildFieldMappingDefinition())

    private fun buildFieldMappingDefinition(): FieldMappingDefinition =
        FieldMappingDefinition("title", "summary")

    fun buildFieldMappingList(): MutableList<FieldMapping> = mutableListOf(buildFieldMapping())

    private fun buildFieldMapping(): FieldMapping =
        FieldMapping(
            "title",
            "summary",
            DirectFieldMapper()
        )

    private fun buildDefaultsForNewIssue(): DefaultsForNewIssue = DefaultsForNewIssue("task", "BUG")

    fun buildIssueTrackingApplication(simpleClassName: String): IssueTrackingApplication {
        val issueTrackingApplication = IssueTrackingApplication()
        issueTrackingApplication.className = simpleClassName
        issueTrackingApplication.name = simpleClassName.toUpperCase()
        return issueTrackingApplication
    }

    fun buildIssueTrackingClient(
        issueTrackingApplication: IssueTrackingApplication,
        clientFactory: ClientFactory
    ): IssueTrackingClient<Any> =
        spy(clientFactory.getClient(issueTrackingApplication))

    fun buildNotificationObserver(): NotificationObserver {
        val observer = NotificationObserver()
        observer.addChannel(SynchronizationFlowTest)
        return observer
    }

    fun buildIssue(key: String = "MK-1") =
        Issue(key, "", LocalDateTime.now())
}

class AlwaysFalseIssueFilter : IssueFilter {
    override fun test(client: IssueTrackingClient<out Any>, issue: Issue): Boolean = false
}