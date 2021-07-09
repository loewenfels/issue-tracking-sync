package ch.loewenfels.issuetrackingsync.app

import ch.loewenfels.issuetrackingsync.AbstractSpringTest
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class IssueTrackingSyncAppTest : AbstractSpringTest() {

    @Autowired
    lateinit var syncApplicationProperties: SyncApplicationProperties

    @Test
    fun contextLoads() {
        // Test fails if 'IllegalStateException: Failed to load ApplicationContext'
        assertNotNull(syncApplicationProperties)
    }
}