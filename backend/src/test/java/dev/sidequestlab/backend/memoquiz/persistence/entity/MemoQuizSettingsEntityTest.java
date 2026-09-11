package dev.sidequestlab.backend.memoquiz.persistence.entity;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemoQuizSettingsEntityTest {

    @Test
    void storesAndReturnsSettingsProperties() {
        MemoQuizSettingsEntity settings = new MemoQuizSettingsEntity();
        LocalDate startDate = LocalDate.of(2026, 9, 11);
        Instant createdAt = Instant.parse("2026-09-11T10:00:00Z");

        settings.setId(42L);
        settings.setStartDate(startDate);
        settings.setCreatedAt(createdAt);

        assertThat(settings.getId()).isEqualTo(42L);
        assertThat(settings.getStartDate()).isEqualTo(startDate);
        assertThat(settings.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void prePersistInitializesMissingCreatedAt() {
        MemoQuizSettingsEntity settings = new MemoQuizSettingsEntity();
        Instant before = Instant.now();

        settings.prePersist();

        assertThat(settings.getCreatedAt()).isBetween(before, Instant.now());
    }

    @Test
    void prePersistPreservesExistingCreatedAt() {
        MemoQuizSettingsEntity settings = new MemoQuizSettingsEntity();
        Instant createdAt = Instant.parse("2026-09-11T10:00:00Z");
        settings.setCreatedAt(createdAt);

        settings.prePersist();

        assertThat(settings.getCreatedAt()).isEqualTo(createdAt);
    }
}
