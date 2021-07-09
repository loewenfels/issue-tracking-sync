package ch.loewenfels.issuetrackingsync.syncclient.jira

import com.atlassian.jira.rest.client.api.AuthenticationHandler
import com.atlassian.jira.rest.client.auth.BasicHttpAuthenticationHandler
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory
import java.net.URI

class ExtendedAsynchronousJiraRestClientFactory : AsynchronousJiraRestClientFactory() {
    override fun create(
        serverUri: URI,
        authenticationHandler: AuthenticationHandler
    ): ExtendedAsynchronousJiraRestClient {
        val httpClient = ExtendedAsynchronousHttpClientFactory()
            .createClient(serverUri, authenticationHandler)
        return ExtendedAsynchronousJiraRestClient(serverUri, httpClient)
    }

    override fun createWithBasicHttpAuthentication(
        serverUri: URI,
        username: String,
        password: String
    ): ExtendedAsynchronousJiraRestClient =
        create(serverUri, BasicHttpAuthenticationHandler(username, password))

    fun extendedCreateWithBasicHttpAuthentication(
        serverUri: URI,
        username: String,
        password: String,
        socketTimeout: Int?
    ): ExtendedAsynchronousJiraRestClient {
        return socketTimeout?.let {
            val httpClient = ExtendedAsynchronousHttpClientFactory()
                .createClient(serverUri, BasicHttpAuthenticationHandler(username, password), socketTimeout)
            ExtendedAsynchronousJiraRestClient(serverUri, httpClient)
        } ?: create(serverUri, BasicHttpAuthenticationHandler(username, password))
    }

}