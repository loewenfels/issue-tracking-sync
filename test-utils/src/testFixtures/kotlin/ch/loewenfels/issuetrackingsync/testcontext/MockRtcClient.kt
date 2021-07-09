package ch.loewenfels.issuetrackingsync.testcontext

import ch.loewenfels.issuetrackingsync.Attachment
import ch.loewenfels.issuetrackingsync.Comment
import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.StateHistory
import ch.loewenfels.issuetrackingsync.executor.SyncActionName
import ch.loewenfels.issuetrackingsync.executor.actions.SynchronizationAction
import ch.loewenfels.issuetrackingsync.notification.NotificationObserver
import ch.loewenfels.issuetrackingsync.syncclient.IssueTrackingClient
import ch.loewenfels.issuetrackingsync.syncconfig.DefaultsForNewIssue
import ch.loewenfels.issuetrackingsync.syncconfig.IssueTrackingApplication
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.http.HttpStatus
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.random.Random

open class MockRtcClient(private val setup: IssueTrackingApplication) : IssueTrackingClient<Issue> {
    private val testIssues = mutableListOf(
        Issue("1234", setup.name, LocalDateTime.now().minusHours(2)),
        Issue("2345", setup.name, LocalDateTime.now().minusHours(3)),
        Issue("3456", setup.name, LocalDateTime.now().minusHours(4))
    )

    override fun getIssue(key: String): Issue? {
        return testIssues.find { it.key == key }
    }

    override fun getIssueFromWebhookBody(body: JsonNode): Issue =
        throw UnsupportedOperationException("RTC does not support webhooks")

    override fun getProprietaryIssue(issueKey: String): Issue? {
        return getIssue(issueKey)
    }

    override fun getProprietaryIssue(fieldName: String, fieldValue: String): Issue? {
        return getIssue(fieldValue)
    }

    override fun getLastUpdated(internalIssue: Issue): LocalDateTime {
        return internalIssue.lastUpdated
    }

    override fun getKey(internalIssue: Issue): String =
        internalIssue.key

    override fun getIssueUrl(internalIssue: Issue): String =
        "${setup.endpoint}/web/projects/${setup.project}#action=com.ibm.team.workitem.viewWorkItem&id=${internalIssue.key}"
            .replace("//", "/")

    override fun getHtmlValue(internalIssue: Issue, fieldName: String): String? =
        getValue(internalIssue, fieldName)?.toString()

    override fun getValue(internalIssue: Issue, fieldName: String): Any? {
        return when (fieldName) {
            "severity" -> "com.ibm.team.workitem.common.model.ISeverity:severity.s2"
            "priority" -> "com.ibm.team.workitem.common.model.IPriority:priority.literal.I12"
            "comments" -> "<b>Important stuff</b>"
            "text" -> "text should have no title"
            "multiSelectCustomFieldRtc" -> listOf("fooRtc", "barRtc")
            "singleSelectCustomFieldRtc" -> "fooRtc"
            "geplantFuer" -> "I2000.3 - 3.66"
            else -> "foobar"
        }
    }

    override fun setValue(
        internalIssueBuilder: Any,
        issue: Issue,
        fieldName: String,
        value: Any?
    ) {
        // no-op
    }

    override fun setHtmlValue(internalIssueBuilder: Any, issue: Issue, fieldName: String, htmlString: String) {
        // no-op
    }

    override fun createOrUpdateTargetIssue(
        issue: Issue,
        defaultsForNewIssue: DefaultsForNewIssue?
    ) {
        val targetIssue = Issue((abs(Random.nextInt()) % 10000).toString(), "RTC", LocalDateTime.now())
        testIssues.add(targetIssue)
        issue.proprietaryTargetInstance = targetIssue
    }

    override fun changedIssuesSince(
        lastPollingTimestamp: LocalDateTime,
        batchSize: Int,
        offset: Int
    ): Collection<Issue> {
        if (offset < 200) return testIssues else return emptyList()
    }

    override fun getComments(internalIssue: Issue): List<Comment> {
        return listOf(
            Comment(
                "Quiet Mary",
                LocalDateTime.now().minusHours(36),
                "I'll have to take a closer look at the logic here",
                "4444"
            ),
            Comment("Impatient Rick", LocalDateTime.now().minusHours(24), "Mary, this is rather urgent!", "5555")
        )
    }

    override fun addComment(internalIssue: Issue, comment: Comment) {
        // no-op
    }

    override fun getAttachments(internalIssue: Issue): List<Attachment> =
        listOf(
            Attachment(
                "explanation.docx",
                "some other content here".toByteArray()
            )
        )

    override fun addAttachment(internalIssue: Issue, attachment: Attachment) {
        // no-op
    }

    override fun getMultiSelectValues(internalIssue: Issue, fieldName: String): List<String> {
        return when (val value = getValue(internalIssue, fieldName)) {
            is String -> listOf(value)
            is List<*> -> value.filterIsInstance<String>()
            else -> listOf()
        }
    }

    override fun setTimeValue(internalIssueBuilder: Any, issue: Issue, fieldName: String, timeInMinutes: Number?) {
        // no-op
    }

    override fun getTimeValueInMinutes(internalIssue: Any, fieldName: String): Number {
        return 15
    }

    override fun getState(internalIssue: Issue): String {
        return "In Umsetzung"
    }

    override fun getStateHistory(internalIssue: Issue): List<StateHistory> {
        return listOf(
            StateHistory(LocalDateTime.now().minusHours(5), "Neu", "In Abklärung"),
            StateHistory(LocalDateTime.now().minusHours(3), "In Abklärung", "Bereit zur Umsetzung"),
            StateHistory(LocalDateTime.now(), "Bereit zur Umsetzung", "In Umsetzung")
        )
    }

    override fun setState(internalIssue: Issue, targetState: String) {
        // no-op
    }

    override fun logException(
        issue: Issue,
        exception: Exception,
        notificationObserver: NotificationObserver,
        syncActions: Map<SyncActionName, SynchronizationAction>
    ): Boolean {
        val errorMessage = HttpStatus.UNAUTHORIZED.reasonPhrase
        notificationObserver.notifyException(issue, Exception(errorMessage), syncActions)
        return true
    }

    override fun close() {
    }

    override fun getProprietaryIssue(issue: Issue): Issue? {
        TODO("Not yet implemented")
    }

    override fun searchProprietaryIssues(fieldName: String, fieldValue: String): List<Issue> {
        TODO("Not yet implemented")
    }

    override fun prepareHtmlValue(htmlString: String): String {
        TODO("Not yet implemented")
    }
}