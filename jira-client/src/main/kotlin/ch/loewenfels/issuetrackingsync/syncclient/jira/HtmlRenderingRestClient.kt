package ch.loewenfels.issuetrackingsync.syncclient.jira

import ch.loewenfels.issuetrackingsync.Comment
import com.atlassian.jira.rest.client.api.domain.Transition

interface HtmlRenderingRestClient {
    /**
     * Get HTML representation of a JIRA field, such as `description`
     */
    fun getRenderedHtml(jiraKey: String, field: String): String?

    /**
     * Get comments with comment body formatted in HTML
     */
    fun getHtmlComments(jiraKey: String): List<Comment>

    /**
     * The JIRA REST Java Client seems quite willing to offer available transitions, but not what state
     * these transitions will result in.
     */
    fun getAvailableTransitions(jiraKey: String): Map<Transition, String>
}