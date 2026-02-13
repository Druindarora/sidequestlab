package dev.sidequestlab.backend.config;

import dev.sidequestlab.backend.auth.api.controller.AuthController;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {TestMemoQuizController.class, AuthController.class})
@Import(SecurityConfig.class)
class MemoQuizSecurityAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationManager authenticationManager;

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

    @Test
    void unauthenticatedRequestToAuthMeReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@example.com")
    void authenticatedRequestToAuthMeReturns200() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("admin@example.com"));
    }

    @Test
    void csrfBootstrapReturns204AndSetsXsrfCookie() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
            .andExpect(status().isNoContent())
            .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    void loginWithoutCsrfReturns403() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@example.com\",\"password\":\"admin\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void loginWithCsrfReturns200() throws Exception {
        Authentication authenticatedUser = UsernamePasswordAuthenticationToken.authenticated(
            "admin@example.com",
            "n/a",
            List.of()
        );
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticatedUser);

        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf"))
            .andExpect(status().isNoContent())
            .andReturn();
        Cookie csrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(csrfCookie);

        mockMvc.perform(post("/api/auth/login")
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@example.com\",\"password\":\"admin\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("admin@example.com"));
    }
}
