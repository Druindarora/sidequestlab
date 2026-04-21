package dev.sidequestlab.backend.auth.service;

import dev.sidequestlab.backend.auth.persistence.entity.UserEntity;
import dev.sidequestlab.backend.auth.persistence.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    private DatabaseUserDetailsService databaseUserDetailsService;

    @BeforeEach
    void setUp() {
        databaseUserDetailsService = new DatabaseUserDetailsService(userRepository);
    }

    @Test
    void loadUserByUsernameReturnsAdminUserDetailsWhenUserExists() {
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail("Admin@Example.com");
        userEntity.setPasswordHash("$2a$10$testHash");
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(userEntity));

        UserDetails userDetails = databaseUserDetailsService.loadUserByUsername("admin@example.com");

        assertThat(userDetails.getUsername()).isEqualTo("Admin@Example.com");
        assertThat(userDetails.getPassword()).isEqualTo("$2a$10$testHash");
        assertThat(userDetails.getAuthorities())
            .extracting("authority")
            .containsExactly("ROLE_ADMIN");
        verify(userRepository).findByEmailIgnoreCase("admin@example.com");
    }

    @Test
    void loadUserByUsernameThrowsWhenUserDoesNotExist() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> databaseUserDetailsService.loadUserByUsername("missing@example.com"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessage("User not found");
        verify(userRepository).findByEmailIgnoreCase("missing@example.com");
    }
}
