package ch.loewenfels.issuetrackingsync.client.jira

import ch.loewenfels.issuetrackingsync.client.IssueTrackingClient
import ch.loewenfels.issuetrackingsync.dto.Issue
import ch.loewenfels.issuetrackingsync.settings.IssueTrackingApplication
import java.time.LocalDateTime

class JiraClient(val setup: IssueTrackingApplication) : IssueTrackingClient {
    override fun changedIssuesSince(lastPollingTimestamp: LocalDateTime): Collection<Issue> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
