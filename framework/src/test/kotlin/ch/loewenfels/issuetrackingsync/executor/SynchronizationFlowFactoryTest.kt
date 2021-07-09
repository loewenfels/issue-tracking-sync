package ch.loewenfels.issuetrackingsync.executor

import ch.loewenfels.issuetrackingsync.AbstractSpringTest
import ch.loewenfels.issuetrackingsync.Issue
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

internal class SynchronizationFlowFactoryTest : AbstractSpringTest() {
    @Autowired
    lateinit var synchronizationFlowFactory: SynchronizationFlowFactory

    @Test
    fun getSynchronizationFlow_matchingSourceAndIssue_flowFound() {
        // arrange
        val sourceApp = "RTC"
        val issue = Issue("123456", "RTC", LocalDateTime.now())
        // act
        val result = synchronizationFlowFactory.getSynchronizationFlow(sourceApp, issue)
        // assert
        assertNotNull(result)
    }

    @Test
    fun getSynchronizationFlow_unknownSource_returnsNull() {
        // arrange
        val sourceApp = "FOOBAR"
        val issue = Issue("", "JIRA", LocalDateTime.now())
        // act
        val result = synchronizationFlowFactory.getSynchronizationFlow(sourceApp, issue)
        // assert
        assertNull(result)
    }
}