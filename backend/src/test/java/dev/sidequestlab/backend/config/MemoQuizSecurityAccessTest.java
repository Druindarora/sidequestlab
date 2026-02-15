package dev.sidequestlab.backend.config;

import dev.sidequestlab.backend.auth.api.controller.AuthController;
import dev.sidequestlab.backend.auth.persistence.entity.UserEntity;
import dev.sidequestlab.backend.auth.persistence.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
    @WithMockUser(username = "admin@example.com")
    void authenticatedRequestToMemoQuizDashboardTodayReturns200WhenPasswordAlreadyChanged() throws Exception {
        when(userRepository.findByEmailIgnoreCase("admin@example.com"))
            .thenReturn(Optional.of(user("admin@example.com", false, "encoded-password")));

        mockMvc.perform(get("/api/memoquiz/dashboard/today"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@example.com")
    void authenticatedRequestToMemoQuizDashboardTodayReturns403WhenPasswordChangeRequired() throws Exception {
        when(userRepository.findByEmailIgnoreCase("admin@example.com"))
            .thenReturn(Optional.of(user("admin@example.com", true, "encoded-password")));

        mockMvc.perform(get("/api/memoquiz/dashboard/today"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("Password change required"));
    }

    @Test
    void unauthenticatedRequestToAuthMeReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@example.com")
    void authenticatedRequestToAuthMeReturns200() throws Exception {
        when(userRepository.findByEmailIgnoreCase("admin@example.com"))
            .thenReturn(Optional.of(user("admin@example.com", false, "encoded-password")));

        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("admin@example.com"))
            .andExpect(jsonPath("$.mustChangePassword").value(false));
    }

    @Test
    @WithMockUser(username = "admin@example.com")
    void authenticatedChangePasswordReturns200AndDisablesPasswordChange() throws Exception {
        String currentPassword = "current-password";
        String newPassword = "new-password";
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        UserEntity user = user("admin@example.com", true, encoder.encode(currentPassword));
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/auth/change-password")
                .with(csrf())
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
    @WithMockUser(username = "admin@example.com")
    void authenticatedChangePasswordReturns400WhenCurrentPasswordIsInvalid() throws Exception {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        when(userRepository.findByEmailIgnoreCase("admin@example.com"))
            .thenReturn(Optional.of(user("admin@example.com", true, encoder.encode("actual-password"))));

        mockMvc.perform(post("/api/auth/change-password")
                .with(csrf())
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
        Authentication authenticatedUser = UsernamePasswordAuthenticationToken.authenticated(
            "admin@example.com",
            "n/a",
            List.of()
        );
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticatedUser);

        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@example.com\",\"password\":\"admin\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("admin@example.com"));
    }

    private UserEntity user(String email, boolean mustChangePassword, String passwordHash) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setMustChangePassword(mustChangePassword);
        user.setPasswordHash(passwordHash);
        return user;
    }
}
