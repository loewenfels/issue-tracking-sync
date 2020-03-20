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
import com.atlassian.jira.rest.client.api.RestClientException
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.http.HttpStatus
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.random.Random

open class MockJiraClient(private val setup: IssueTrackingApplication) : IssueTrackingClient<Issue> {
    private val testIssues = mutableListOf(
        Issue("MK-1", setup.name, LocalDateTime.now().minusHours(2)),//
        Issue("MK-2", setup.name, LocalDateTime.now().minusHours(3)),//
        Issue("MK-3", setup.name, LocalDateTime.now().minusHours(4))
    )

    override fun getIssue(key: String): Issue? {
        return testIssues.find { it.key == key }
    }

    override fun getIssueFromWebhookBody(body: JsonNode): Issue {
        val formatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS][xxx][xx][X]")
        return Issue(
            body.get("issue")?.get("key")?.asText() ?: "",
            setup.name,
            OffsetDateTime.parse(
                body.get("issue")?.get("fields")?.get("updated")?.asText() ?: "",
                formatter
            ).toLocalDateTime()
        )
    }

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
        "${setup.endpoint}/browse/${internalIssue.key}".replace("//", "/")

    override fun getHtmlValue(internalIssue: Issue, fieldName: String): String? {
        return when (fieldName) {
            "fromOne" -> "text should have no title\n<h4>text2</h4>\n Some Solution\n<h4>text3</h4>\n Some Solution"
            else -> "<h4>someOtherStuff</h4>\n hot other stuff"
        }
    }

    override fun getValue(internalIssue: Issue, fieldName: String): Any? {
        return when (fieldName) {
            "priorityId" -> "12"
            "comments" -> "h4. Important stuff"
            "multiSelectCustomFieldJira" -> arrayOf("fooJira", "barJira")
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
        val targetIssue = Issue("MK-" + (abs(Random.nextInt()) % 10000).toString(), "JIRA", LocalDateTime.now())
        testIssues.add(targetIssue)
        issue.proprietaryTargetInstance = targetIssue
        testIssues.add(issue)
    }

    override fun changedIssuesSince(
        lastPollingTimestamp: LocalDateTime,
        batchSize: Int,
        offset: Int
    ): Collection<Issue> {
        return if (offset < testIssues.size) testIssues else emptyList()
    }

    override fun getComments(internalIssue: Issue): List<Comment> {
        return listOf(
            Comment(
                "Quiet Mary",
                LocalDateTime.now().minusHours(36),
                "I'll have to take a closer look at the logic here",
                "4321"
            ),
            Comment("Happy Joe", LocalDateTime.now().minusHours(24), "Mary, could this be related to BUG-1234?", "2345")
        )
    }

    override fun addComment(internalIssue: Issue, comment: Comment) {
        // no-op
    }

    override fun getAttachments(internalIssue: Issue): List<Attachment> =
        listOf(
            Attachment(
                "explanation.docx",
                "some content here".toByteArray()
            )
        )

    override fun addAttachment(internalIssue: Issue, attachment: Attachment) {
        // no-op
    }

    override fun getMultiSelectValues(internalIssue: Issue, fieldName: String): List<String> {
        return (getValue(internalIssue, fieldName) as List<*>).filterIsInstance<String>()
    }

    override fun getTimeValueInMinutes(internalIssue: Any, fieldName: String): Number {
        return 16
    }

    override fun setTimeValue(internalIssueBuilder: Any, issue: Issue, fieldName: String, timeInMinutes: Number?) {
        // no-op
    }

    override fun getState(internalIssue: Issue): String {
        return "In Work"
    }

    override fun getStateHistory(internalIssue: Issue): List<StateHistory> {
        return listOf(
            StateHistory(LocalDateTime.now().minusHours(5), "Open", "Authorized"),
            StateHistory(LocalDateTime.now(), "Authorized", "In Work")
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
    ) {
        val statusCode = (exception as RestClientException).statusCode.or(0)
        val responseMessage = HttpStatus.valueOf(statusCode).reasonPhrase
        val errorMessage = "Jira: $responseMessage ($statusCode)"
        notificationObserver.notifyException(issue, Exception(errorMessage), syncActions)
    }
}