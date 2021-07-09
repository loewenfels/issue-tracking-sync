package ch.loewenfels.issuetrackingsync.controller

import ch.loewenfels.issuetrackingsync.*
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath

internal class InteractivceSyncControllerTest : AbstractSpringTest() {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    @WithMockUser(roles = ["USER"])
    fun definedSystems_defaultGet_shouldHaveStatusOk() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/definedSystems")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @WithMockUser(roles = ["USER"])
    fun manualsync_unknownIssueKey_shouldHaveStatusOkWithErrorMessage() {
        val requestBody = JsonNodeFactory.instance.objectNode()
        requestBody.put(HTTP_PARAMNAME_TRACKINGSYSTEM, "JIRA")
        requestBody.put(HTTP_PARAMNAME_ISSUEKEY, "DEV-123456")
        mockMvc.perform(
            MockMvcRequestBuilders.put("/manualsync")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody.toString())
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath(HTTP_PARAMNAME_RESPONSEMESSAGE, containsString("Failed to locate issue")))
    }

    @Test
    @WithMockUser(roles = ["USER"])
    fun manualsync_validIssueKey_shouldHaveStatusOkWithStatus() {
        val requestBody = JsonNodeFactory.instance.objectNode()
        requestBody.put(HTTP_PARAMNAME_TRACKINGSYSTEM, "JIRA")
        requestBody.put(HTTP_PARAMNAME_ISSUEKEY, "MK-1")
        mockMvc.perform(
            MockMvcRequestBuilders.put("/manualsync")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody.toString())
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath(HTTP_PARAMNAME_RESPONSEMESSAGE, Matchers.`is`("Issue MK-1 has been queued for sync")))
    }
}