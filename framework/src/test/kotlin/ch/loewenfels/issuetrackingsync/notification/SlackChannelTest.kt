package ch.loewenfels.issuetrackingsync.notification

import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.app.NotificationChannelProperties
import ch.loewenfels.issuetrackingsync.executor.SyncActionName
import ch.loewenfels.issuetrackingsync.executor.actions.SimpleSynchronizationAction
import ch.loewenfels.issuetrackingsync.executor.actions.SynchronizationAction
import org.apache.commons.io.IOUtils
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.conn.HttpHostConnectException
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*
import java.time.LocalDateTime

internal class SlackChannelTest {
    @Test
    fun onSuccessfulSync_httpClientSuccess_noException() {
        // arrange
        val httpClient = mock(CloseableHttpClient::class.java)
        val httpResponse = mock(CloseableHttpResponse::class.java)
        `when`(httpClient.execute(any())).thenReturn(httpResponse)
        val properties = defaultProperties()
        val testee = SlackChannel(properties)
        testee.injectedHttpClient = httpClient
        val syncActions = createSyncActions()
        // act
        val issue = Issue("MK-1", "JIRA", LocalDateTime.now())
        issue.hasChanges = true
        testee.onSuccessfulSync(issue, syncActions)
        // assert
        val captor = ArgumentCaptor.forClass(HttpPost::class.java)
        verify(httpClient).execute(captor.capture())
        val post = captor.value
        val postEntity = post.entity as StringEntity
        val postEntityContent = IOUtils.toString(postEntity.content)
        assertThat(postEntityContent, containsString("MK-1"))
    }

    @Test
    fun onSuccessfulSync_httpClientError_noException() {
        // arrange
        val httpClient = mock(CloseableHttpClient::class.java)
        `when`(httpClient.execute(any())).thenThrow(HttpHostConnectException::class.java)
        val properties = defaultProperties()
        val testee = SlackChannel(properties)
        testee.injectedHttpClient = httpClient
        val syncActions = createSyncActions()
        // act
        val issue = Issue("MK-1", "JIRA", LocalDateTime.now())
        issue.hasChanges = true
        testee.onSuccessfulSync(issue, syncActions)
        // assert
        verify(httpClient).execute(any(HttpPost::class.java))
    }

    private fun defaultProperties(): NotificationChannelProperties {
        val properties = NotificationChannelProperties()
        properties.endpoint = "http://localhost:8080/slack"
        return properties
    }

    private fun createSyncActions(): Map<SyncActionName, SynchronizationAction> {
        return mapOf<SyncActionName, SynchronizationAction>(
            "SynchronizeTimeJiraToRtc" to SimpleSynchronizationAction("Slack")
        )
    }
}