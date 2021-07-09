package ch.loewenfels.issuetrackingsync.controller

import ch.loewenfels.issuetrackingsync.AbstractSpringTest
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

internal class StatisticsControllerTest : AbstractSpringTest() {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    @WithMockUser(roles = ["USER"])
    fun statistics_defaultGet_shouldHaveStatusOk() {
        mockMvc.perform(
            get("/statistics")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("queuename").isNotEmpty)
            .andExpect { content().string(containsString("")) }
    }
}