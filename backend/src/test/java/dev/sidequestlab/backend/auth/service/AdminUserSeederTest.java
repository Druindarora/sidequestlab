package dev.sidequestlab.backend.auth.service;

import dev.sidequestlab.backend.auth.config.AdminSeedProperties;
import dev.sidequestlab.backend.auth.persistence.entity.UserEntity;
import dev.sidequestlab.backend.auth.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserSeederTest {

    @Mock
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private AdminSeedProperties adminSeedProperties;
    private AdminUserSeeder adminUserSeeder;

    @BeforeEach
    void setUp() {
        adminSeedProperties = new AdminSeedProperties();
        adminUserSeeder = new AdminUserSeeder(userRepository, passwordEncoder, adminSeedProperties);
    }

    @Test
    void createsAdminUserWhenUsersTableIsEmptyAndCredentialsArePresent() throws Exception {
        when(userRepository.count()).thenReturn(0L);
        adminSeedProperties.setEmail(" Admin@Example.com ");
        adminSeedProperties.setPassword("my-secret-password");

        adminUserSeeder.run(new DefaultApplicationArguments(new String[0]));

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());

        UserEntity savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("admin@example.com");
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("my-secret-password");
        assertThat(passwordEncoder.matches("my-secret-password", savedUser.getPasswordHash())).isTrue();
    }

    @Test
    void doesNotCreateAdminUserWhenUsersAlreadyExist() throws Exception {
        when(userRepository.count()).thenReturn(2L);
        adminSeedProperties.setEmail("admin@example.com");
        adminSeedProperties.setPassword("my-secret-password");

        adminUserSeeder.run(new DefaultApplicationArguments(new String[0]));

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(UserEntity.class));
    }

    @Test
    void doesNotCreateAdminUserWhenCredentialsAreMissing() throws Exception {
        when(userRepository.count()).thenReturn(0L);
        adminSeedProperties.setEmail(" ");
        adminSeedProperties.setPassword(null);

        adminUserSeeder.run(new DefaultApplicationArguments(new String[0]));

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(UserEntity.class));
    }
}
