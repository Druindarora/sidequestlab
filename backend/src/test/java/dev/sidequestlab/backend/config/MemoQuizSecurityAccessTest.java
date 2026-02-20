package dev.sidequestlab.backend.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sidequestlab.backend.auth.api.controller.AuthController;
import dev.sidequestlab.backend.auth.persistence.entity.UserEntity;
import dev.sidequestlab.backend.auth.persistence.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(controllers = {TestMemoQuizController.class, AuthController.class})
@Import(SecurityConfig.class)
class MemoQuizSecurityAccessTest {

    private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() { };

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private UserRepository userRepository;

    @Test
    void unauthenticatedRequestToMemoQuizDashboardTodayReturns401() throws Exception {
        mockMvc.perform(get("/api/memoquiz/dashboard/today"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedRequestToMemoQuizDashboardTodayReturns200WhenPasswordAlreadyChanged() throws Exception {
        stubSuccessfulAuthentication("admin@example.com");
        when(userRepository.findByEmailIgnoreCase("admin@example.com"))
            .thenReturn(Optional.of(user("admin@example.com", false, "encoded-password")));

        AuthSession session = login("admin@example.com", "admin");

        mockMvc.perform(get("/api/memoquiz/dashboard/today")
                .session(session.session()))
            .andExpect(status().isOk());
    }

    @Test
    void authenticatedRequestToMemoQuizDashboardTodayReturns403WhenPasswordChangeRequired() throws Exception {
        stubSuccessfulAuthentication("admin@example.com");
        when(userRepository.findByEmailIgnoreCase("admin@example.com"))
            .thenReturn(Optional.of(user("admin@example.com", true, "encoded-password")));

        AuthSession session = login("admin@example.com", "admin");

        mockMvc.perform(get("/api/memoquiz/dashboard/today")
                .session(session.session()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("Password change required"));
    }

    @Test
    void unauthenticatedRequestToAuthMeReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedRequestToAuthMeReturns200() throws Exception {
        stubSuccessfulAuthentication("admin@example.com");
        when(userRepository.findByEmailIgnoreCase("admin@example.com"))
            .thenReturn(Optional.of(user("admin@example.com", false, "encoded-password")));

        AuthSession session = login("admin@example.com", "admin");

        mockMvc.perform(get("/api/auth/me")
                .session(session.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("admin@example.com"))
            .andExpect(jsonPath("$.mustChangePassword").value(false));
    }

    @Test
    void authenticatedChangePasswordReturns200AndDisablesPasswordChange() throws Exception {
        String currentPassword = "current-password";
        String newPassword = "new-password";
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        stubSuccessfulAuthentication("admin@example.com");

        UserEntity user = user("admin@example.com", true, encoder.encode(currentPassword));
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(user));

        AuthSession session = login("admin@example.com", "admin");

        mockMvc.perform(post("/api/auth/change-password")
                .session(session.session())
                .header("X-XSRF-TOKEN", session.xsrfToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"" + currentPassword + "\",\"newPassword\":\"" + newPassword + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Password changed"));

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());

        UserEntity savedUser = userCaptor.getValue();
        assertThat(savedUser.isMustChangePassword()).isFalse();
        assertThat(encoder.matches(newPassword, savedUser.getPasswordHash())).isTrue();
    }

    @Test
    void authenticatedChangePasswordReturns400WhenCurrentPasswordIsInvalid() throws Exception {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        stubSuccessfulAuthentication("admin@example.com");
        when(userRepository.findByEmailIgnoreCase("admin@example.com"))
            .thenReturn(Optional.of(user("admin@example.com", true, encoder.encode("actual-password"))));

        AuthSession session = login("admin@example.com", "admin");

        mockMvc.perform(post("/api/auth/change-password")
                .session(session.session())
                .header("X-XSRF-TOKEN", session.xsrfToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"wrong-password\",\"newPassword\":\"new-password\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Current password is invalid"));
    }

    @Test
    void csrfBootstrapReturns200WithToken() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isString());
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
        stubSuccessfulAuthentication("admin@example.com");
        CsrfBootstrap csrf = bootstrapCsrf();

        mockMvc.perform(loginRequest(csrf.session(), csrf.token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("admin@example.com"));
    }

    private void stubSuccessfulAuthentication(String email) {
        Authentication authenticatedUser = UsernamePasswordAuthenticationToken.authenticated(email, "n/a", List.of());
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticatedUser);
    }

    private AuthSession login(String email, String password) throws Exception {
        CsrfBootstrap csrf = bootstrapCsrf();

        MvcResult loginResult = mockMvc.perform(loginRequest(csrf.session(), csrf.token(), email, password))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(email))
            .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session)
            .withFailMessage("Expected authenticated session to be created")
            .isNotNull();

        return new AuthSession(session, csrf.token());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder loginRequest(
        MockHttpSession session,
        String xsrfToken
    ) {
        return loginRequest(session, xsrfToken, "admin@example.com", "admin");
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder loginRequest(
        MockHttpSession session,
        String xsrfToken,
        String email,
        String password
    ) {
        return post("/api/auth/login")
            .session(session)
            .header("X-XSRF-TOKEN", xsrfToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
    }

    private CsrfBootstrap bootstrapCsrf() throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isString())
            .andReturn();

        MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession(false);
        assertThat(session)
            .withFailMessage("Expected CSRF bootstrap to create a session")
            .isNotNull();

        Map<String, String> response = objectMapper.readValue(csrfResult.getResponse().getContentAsString(), STRING_MAP);
        String token = response.get("token");
        assertThat(token).isNotBlank();
        return new CsrfBootstrap(session, token);
    }

    private UserEntity user(String email, boolean mustChangePassword, String passwordHash) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setMustChangePassword(mustChangePassword);
        user.setPasswordHash(passwordHash);
        return user;
    }

    private record AuthSession(
        MockHttpSession session,
        String xsrfToken
    ) {
    }

    private record CsrfBootstrap(MockHttpSession session, String token) {
    }
}
