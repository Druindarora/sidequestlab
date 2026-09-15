package dev.sidequestlab.backend.config;

import dev.sidequestlab.backend.auth.persistence.entity.UserEntity;
import dev.sidequestlab.backend.auth.persistence.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ForcePasswordChangeFilterTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final ForcePasswordChangeFilter filter = new ForcePasswordChangeFilter(userRepository);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void bypassesNonMemoQuizRequestsWithoutLookingUpUser() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        authenticate("admin@example.com");

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
        verify(userRepository, never()).findByEmailIgnoreCase("admin@example.com");
    }

    @Test
    void bypassesAnonymousMemoQuizRequestsWithoutLookingUpUser() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/memoquiz/dashboard/today");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
            "key",
            "anonymousUser",
            List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        ));

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
        verify(userRepository, never()).findByEmailIgnoreCase("anonymousUser");
    }

    @Test
    void allowsMemoQuizRequestWhenPasswordChangeIsNotRequired() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/memoquiz/dashboard/today");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        authenticate("admin@example.com");
        when(userRepository.findByEmailIgnoreCase("admin@example.com"))
            .thenReturn(Optional.of(user(false)));

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void blocksMemoQuizRequestWhenUserMustChangePassword() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sidequest/api/memoquiz/dashboard/today");
        request.setContextPath("/sidequest");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        authenticate("admin@example.com");
        when(userRepository.findByEmailIgnoreCase("admin@example.com"))
            .thenReturn(Optional.of(user(true)));

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
        assertThat(response.getContentAsString()).isEqualTo("{\"error\":\"Password change required\"}");
    }

    @Test
    void blocksMemoQuizRequestWhenAuthenticatedUserIsMissingFromRepository() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/memoquiz/dashboard/today");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        authenticate("admin@example.com");
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.empty());

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).isEqualTo("{\"error\":\"Password change required\"}");
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(email, "n/a", List.of())
        );
    }

    private UserEntity user(boolean mustChangePassword) {
        UserEntity user = new UserEntity();
        user.setMustChangePassword(mustChangePassword);
        return user;
    }
}
