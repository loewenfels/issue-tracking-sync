package ch.loewenfels.issuetrackingsync.syncclient.jira

import ch.loewenfels.issuetrackingsync.genericMock
import com.atlassian.jira.rest.client.api.JiraRestClient
import com.atlassian.jira.rest.client.api.MetadataRestClient
import com.atlassian.jira.rest.client.api.domain.IssueType
import com.atlassian.jira.rest.client.api.domain.Priority
import io.atlassian.util.concurrent.Promise
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.net.URI

internal class JiraMetadataTest {
    private val someUri = URI.create("")

    @Test
    fun getIssueTypeId() {
        // arrange
        val issueType = listOf(IssueType(someUri, 123L, "Defect", false, "", someUri))
        val metadataClient = buildMetadataClient(issueType)
        val mockClient = mock(JiraRestClient::class.java)
        `when`(mockClient.metadataClient).thenReturn(metadataClient)
        // act
        val result = JiraMetadata.getIssueTypeId("Defect", mockClient)
        // assert
        assertEquals(123L, result)
    }

    @Test
    fun getPriorityId() {
        // arrange
        val priorities = listOf(Priority(someUri, 123L, "Hoch", "Orange", "", someUri))
        val metadataClient = buildMetadataClient(priorities)
        val mockClient = mock(JiraRestClient::class.java)
        `when`(mockClient.metadataClient).thenReturn(metadataClient)
        // act
        val result = JiraMetadata.getPriorityId("Hoch", mockClient)
        // assert
        assertEquals(123L, result)
    }

    @Test
    fun getPriorityName() {
        // arrange
        val priorities = listOf(Priority(someUri, 123L, "Hoch", "Orange", "", someUri))
        val metadataClient = buildMetadataClient(priorities)
        val mockClient = mock(JiraRestClient::class.java)
        `when`(mockClient.metadataClient).thenReturn(metadataClient)
        // act
        val result = JiraMetadata.getPriorityName(123L, mockClient)
        // assert
        assertEquals("Hoch", result)
    }

    private fun buildMetadataClient(collection: List<*>): MetadataRestClient? {
        val issueTypesPromise: Promise<Iterable<IssueType>> = genericMock()
        val prioritiesPromise: Promise<Iterable<Priority>> = genericMock()
        val issueTypes = collection.filterIsInstance(IssueType::class.java)
        val priorities = collection.filterIsInstance(Priority::class.java)
        `when`(issueTypesPromise.claim()).thenReturn(issueTypes)
        `when`(prioritiesPromise.claim()).thenReturn(priorities)
        val metadataClient = mock(MetadataRestClient::class.java)
        `when`(metadataClient.issueTypes).thenReturn(issueTypesPromise)
        `when`(metadataClient.priorities).thenReturn(prioritiesPromise)
        return metadataClient
    }
}

