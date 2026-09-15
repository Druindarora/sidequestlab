package dev.sidequestlab.backend.memoquiz.persistence.entity;

import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemoQuizSessionEntityTest {

    @Test
    void storesAndReturnsSessionProperties() {
        MemoQuizSessionEntity session = new MemoQuizSessionEntity();
        Instant startedAt = Instant.parse("2026-09-15T10:00:00Z");
        Instant endedAt = Instant.parse("2026-09-15T10:05:00Z");

        session.setId(7L);
        session.setStartedAt(startedAt);
        session.setEndedAt(endedAt);
        session.setDurationSeconds(300);
        session.setDayIndex(12);

        assertThat(session.getId()).isEqualTo(7L);
        assertThat(session.getStartedAt()).isEqualTo(startedAt);
        assertThat(session.getEndedAt()).isEqualTo(endedAt);
        assertThat(session.getDurationSeconds()).isEqualTo(300);
        assertThat(session.getDayIndex()).isEqualTo(12);
    }

    @Test
    void prePersistInitializesMissingStartedAt() {
        MemoQuizSessionEntity session = new MemoQuizSessionEntity();
        Instant before = Instant.now();

        session.prePersist();

        assertThat(session.getStartedAt()).isBetween(before, Instant.now());
    }

    @Test
    void prePersistPreservesExistingStartedAt() {
        MemoQuizSessionEntity session = new MemoQuizSessionEntity();
        Instant startedAt = Instant.parse("2026-09-15T10:00:00Z");
        session.setStartedAt(startedAt);

        session.prePersist();

        assertThat(session.getStartedAt()).isEqualTo(startedAt);
    }
}
