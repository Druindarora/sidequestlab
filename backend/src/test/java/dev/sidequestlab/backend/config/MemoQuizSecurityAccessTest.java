package dev.sidequestlab.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TestMemoQuizController.class)
@Import(SecurityConfig.class)
class MemoQuizSecurityAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedRequestToMemoQuizDashboardTodayReturns401() throws Exception {
        mockMvc.perform(get("/api/memoquiz/dashboard/today"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@example.com")
    void authenticatedRequestToMemoQuizDashboardTodayReturns200() throws Exception {
        mockMvc.perform(get("/api/memoquiz/dashboard/today"))
            .andExpect(status().isOk());
    }
}
