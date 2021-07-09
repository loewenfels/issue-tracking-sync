package ch.loewenfels.issuetrackingsync.scheduling

import ch.loewenfels.issuetrackingsync.AbstractSpringTest
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import javax.jms.TextMessage

internal class SyncRequestConsumerTest : AbstractSpringTest() {
    @Autowired
    lateinit var testee: SyncRequestConsumer

    @Test
    fun onMessage_validTextMessage_acknowledged() {
        // arrange
        val syncRequestAsJson =
            "{\"key\":\"MK-1\",\"clientSourceName\":\"JIRA\",\"lastUpdated\":\"2019-08-29T14:34:07.077\"}"
        val textMessage = mock(TextMessage::class.java)
        `when`(textMessage.text).thenReturn(syncRequestAsJson)
        // act
        testee.onMessage(textMessage)
        // assert
        verify(textMessage).acknowledge()
    }
}

