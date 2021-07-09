package ch.loewenfels.issuetrackingsync.controller

import ch.loewenfels.issuetrackingsync.AbstractSpringTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

internal class WebhookControllerTest : AbstractSpringTest() {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun triggerSyncRequest_sourceNotSupportingWebhook_shouldHaveStatusError() {
        // arrange
        val payload = WebhookControllerTest::class.java.getResource("/jira-webhook-payload.json").readText()
        // act
        mockMvc.perform(
            MockMvcRequestBuilders.post("/webhook/rtc")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .accept(MediaType.APPLICATION_JSON)
        )
            // assert
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
    }

    @Test
    fun triggerSyncRequest_sourceSupportingWebhook_shouldHaveStatusOk() {
        // arrange
        val payload = WebhookControllerTest::class.java.getResource("/jira-webhook-payload.json").readText()
        // act
        mockMvc.perform(
            MockMvcRequestBuilders.post("/webhook/jira")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .accept(MediaType.APPLICATION_JSON)
        )
            // assert
            .andExpect(MockMvcResultMatchers.status().isOk)
    }
}