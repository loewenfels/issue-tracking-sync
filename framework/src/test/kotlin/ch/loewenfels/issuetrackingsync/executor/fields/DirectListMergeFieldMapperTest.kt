package ch.loewenfels.issuetrackingsync.executor.fields

import ch.loewenfels.issuetrackingsync.AbstractSpringTest
import ch.loewenfels.issuetrackingsync.Issue
import ch.loewenfels.issuetrackingsync.any
import ch.loewenfels.issuetrackingsync.syncclient.ClientFactory
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildIssueTrackingApplication
import ch.loewenfels.issuetrackingsync.testcontext.TestObjects.buildIssueTrackingClient
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.empty
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

internal class DirectListMergeFieldMapperTest : AbstractSpringTest() {
    @Autowired
    private lateinit var clientFactory: ClientFactory
    private val mockJiraValuesForArrayField = listOf("a", "b")

    @Test
    fun setValue_getValueShouldReturnOneValueInsteadOfAList_valueGetsWrittenWithOnePlusNewElemnts() {
        // arrange
        val issueTrackingApplication = buildIssueTrackingApplication("JiraClient")
        val issueTrackingClient = buildIssueTrackingClient(issueTrackingApplication, clientFactory)
        val issue = Issue("", "", LocalDateTime.now())
        issue.proprietaryTargetInstance = issue
        val expected = listOf("foobar", "c", "d")
        // act
        getTestee().setValue("", "oneStringField", issue, issueTrackingClient, listOf("c", "d"))
        // assert
        val argumentCaptor = ArgumentCaptor.forClass(List::class.java)
        verify(issueTrackingClient).setValue(any(String::class.java), any(), any(), argumentCaptor.capture())
        assert(argumentCaptor.value.containsAll(expected)) {
            "value writte does not match expected values to be written:\nexpected: $expected\nactual: ${argumentCaptor.value}"
        }
    }


    @ParameterizedTest
    @MethodSource("writeValues")
    fun setValue_differentListOfArgument_mergedVersionContainingOldAndNewValue(valuesToWrite: List<String>) {
        // arrange
        val issueTrackingApplication = buildIssueTrackingApplication("JiraClient")
        val issueTrackingClient = buildIssueTrackingClient(issueTrackingApplication, clientFactory)
        val issue = Issue("", "", LocalDateTime.now())
        issue.proprietaryTargetInstance = issue
        val expected = mockJiraValuesForArrayField.toMutableSet()
        expected.addAll(valuesToWrite)
        // act
        getTestee().setValue("", "arrayField", issue, issueTrackingClient, valuesToWrite)
        // assert
        val argumentCaptor = ArgumentCaptor.forClass(List::class.java)
        verify(issueTrackingClient).setValue(any(String::class.java), any(), any(), argumentCaptor.capture())
        assert(argumentCaptor.value.containsAll(expected)) {
            "value writte does not match expected values to be written:\nexpected: $expected\nactual: ${argumentCaptor.value}"
        }
        val potentiallyEmptyList = argumentCaptor.value.toMutableList()
        potentiallyEmptyList.removeAll(expected)
        assertThat(potentiallyEmptyList, `is`(empty()))
    }

    companion object {
        @JvmStatic
        fun writeValues() = listOf(
            Arguments.of(listOf("a", "b")),
            Arguments.of(listOf("c", "d")),
            Arguments.of(listOf("a", "c", "d")),
            Arguments.of(listOf("d")),
            Arguments.of(emptyList<String>())
        )
    }

    private fun getTestee() = DirectListMergeFieldMapper()
}