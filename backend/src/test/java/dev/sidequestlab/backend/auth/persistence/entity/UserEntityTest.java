package dev.sidequestlab.backend.auth.persistence.entity;

import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserEntityTest {

    @Test
    void storesAndReturnsUserProperties() {
        UserEntity user = new UserEntity();
        Instant createdAt = Instant.parse("2026-09-11T10:00:00Z");

        user.setId(42L);
        user.setEmail("admin@example.com");
        user.setPasswordHash("hashed-password");
        user.setMustChangePassword(false);
        user.setCreatedAt(createdAt);

        assertThat(user.getId()).isEqualTo(42L);
        assertThat(user.getEmail()).isEqualTo("admin@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(user.isMustChangePassword()).isFalse();
        assertThat(user.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void mustChangePasswordDefaultsToTrue() {
        UserEntity user = new UserEntity();

        assertThat(user.isMustChangePassword()).isTrue();
    }

    @Test
    void prePersistInitializesMissingCreatedAt() {
        UserEntity user = new UserEntity();
        Instant before = Instant.now();

        user.prePersist();

        assertThat(user.getCreatedAt()).isBetween(before, Instant.now());
    }

    @Test
    void prePersistPreservesExistingCreatedAt() {
        UserEntity user = new UserEntity();
        Instant createdAt = Instant.parse("2026-09-11T10:00:00Z");
        user.setCreatedAt(createdAt);

        user.prePersist();

        assertThat(user.getCreatedAt()).isEqualTo(createdAt);
    }
}
