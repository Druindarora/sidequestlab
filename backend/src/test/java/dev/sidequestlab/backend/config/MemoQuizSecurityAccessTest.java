package dev.sidequestlab.backend.config;

import dev.sidequestlab.backend.auth.api.controller.AuthController;
import dev.sidequestlab.backend.auth.persistence.entity.UserEntity;
import dev.sidequestlab.backend.auth.persistence.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import java.util.List;
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

    @Autowired
    private MockMvc mockMvc;

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
                .session(session.session())
                .cookie(session.sessionCookie()))
            .andExpect(status().isOk());
    }

    @Test
    void authenticatedRequestToMemoQuizDashboardTodayReturns403WhenPasswordChangeRequired() throws Exception {
        stubSuccessfulAuthentication("admin@example.com");
        when(userRepository.findByEmailIgnoreCase("admin@example.com"))
            .thenReturn(Optional.of(user("admin@example.com", true, "encoded-password")));

        AuthSession session = login("admin@example.com", "admin");

        mockMvc.perform(get("/api/memoquiz/dashboard/today")
                .session(session.session())
                .cookie(session.sessionCookie()))
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
                .session(session.session())
                .cookie(session.sessionCookie()))
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
                .cookie(session.sessionCookie(), session.xsrfCookie())
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
                .cookie(session.sessionCookie(), session.xsrfCookie())
                .header("X-XSRF-TOKEN", session.xsrfToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"wrong-password\",\"newPassword\":\"new-password\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Current password is invalid"));
    }

    @Test
    void csrfBootstrapReturns204() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
            .andExpect(status().isNoContent());
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
        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf"))
            .andExpect(status().isNoContent())
            .andReturn();
        Cookie xsrfCookie = requiredCookie(csrfResult, "XSRF-TOKEN");

        mockMvc.perform(loginRequest(xsrfCookie, xsrfCookie.getValue()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("admin@example.com"));
    }

    private void stubSuccessfulAuthentication(String email) {
        Authentication authenticatedUser = UsernamePasswordAuthenticationToken.authenticated(email, "n/a", List.of());
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticatedUser);
    }

    private AuthSession login(String email, String password) throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf"))
            .andExpect(status().isNoContent())
            .andReturn();
        Cookie xsrfCookie = requiredCookie(csrfResult, "XSRF-TOKEN");
        String xsrfToken = xsrfCookie.getValue();

        MvcResult loginResult = mockMvc.perform(loginRequest(xsrfCookie, xsrfToken, email, password))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(email))
            .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session)
            .withFailMessage("Expected authenticated session to be created")
            .isNotNull();
        Cookie sessionCookie = new Cookie("JSESSIONID", session.getId());
        sessionCookie.setPath("/");

        Cookie refreshedXsrfCookie = loginResult.getResponse().getCookie("XSRF-TOKEN");
        if (refreshedXsrfCookie != null) {
            xsrfCookie = refreshedXsrfCookie;
            xsrfToken = refreshedXsrfCookie.getValue();
        }

        return new AuthSession(session, sessionCookie, xsrfCookie, xsrfToken);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder loginRequest(
        Cookie xsrfCookie,
        String xsrfToken
    ) {
        return loginRequest(xsrfCookie, xsrfToken, "admin@example.com", "admin");
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder loginRequest(
        Cookie xsrfCookie,
        String xsrfToken,
        String email,
        String password
    ) {
        return post("/api/auth/login")
            .cookie(xsrfCookie)
            .header("X-XSRF-TOKEN", xsrfToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
    }

    private Cookie requiredCookie(MvcResult result, String name) {
        Cookie cookie = result.getResponse().getCookie(name);
        assertThat(cookie)
            .withFailMessage("Expected cookie '%s' in response", name)
            .isNotNull();
        return cookie;
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
        Cookie sessionCookie,
        Cookie xsrfCookie,
        String xsrfToken
    ) {
    }
}
