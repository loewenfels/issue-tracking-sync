package ch.loewenfels.issuetrackingsync

import ch.loewenfels.issuetrackingsync.app.IssueTrackingSyncApp
import ch.loewenfels.issuetrackingsync.testcontext.TestSpringConfiguration
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.jms.core.JmsTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [TestSpringConfiguration::class, IssueTrackingSyncApp::class])
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class AbstractSpringTest {
    @MockBean
    protected lateinit var jmsTemplate: JmsTemplate
}

