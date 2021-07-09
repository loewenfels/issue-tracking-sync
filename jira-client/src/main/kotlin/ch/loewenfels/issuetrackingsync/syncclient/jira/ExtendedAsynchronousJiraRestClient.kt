package ch.loewenfels.issuetrackingsync.syncclient.jira

import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClient
import com.atlassian.jira.rest.client.internal.async.DisposableHttpClient
import java.net.URI
import javax.ws.rs.core.UriBuilder

class ExtendedAsynchronousJiraRestClient(serverUri: URI, httpClient: DisposableHttpClient) :
    AsynchronousJiraRestClient(serverUri, httpClient) {
    private val delegateForHtmlRendering: AsynchronousHtmlRenderingRestClient

    init {
        val baseUri = UriBuilder.fromUri(serverUri).path("/rest/api/latest").build()
        delegateForHtmlRendering = AsynchronousHtmlRenderingRestClient(baseUri, httpClient)
    }

    fun getHtmlRenderingRestClient(): HtmlRenderingRestClient = delegateForHtmlRendering
}