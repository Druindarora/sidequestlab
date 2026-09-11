package dev.sidequestlab.backend.memoquiz.persistence.entity;

import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemoQuizQuizEntityTest {

    @Test
    void storesAndReturnsQuizProperties() {
        MemoQuizQuizEntity quiz = new MemoQuizQuizEntity();
        Instant createdAt = Instant.parse("2026-09-11T10:00:00Z");

        quiz.setId(42L);
        quiz.setCode("java-basics");
        quiz.setTitle("Java Basics");
        quiz.setCreatedAt(createdAt);

        assertThat(quiz.getId()).isEqualTo(42L);
        assertThat(quiz.getCode()).isEqualTo("java-basics");
        assertThat(quiz.getTitle()).isEqualTo("Java Basics");
        assertThat(quiz.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void prePersistInitializesMissingCreatedAt() {
        MemoQuizQuizEntity quiz = new MemoQuizQuizEntity();
        Instant before = Instant.now();

        quiz.prePersist();

        assertThat(quiz.getCreatedAt()).isBetween(before, Instant.now());
    }

    @Test
    void prePersistPreservesExistingCreatedAt() {
        MemoQuizQuizEntity quiz = new MemoQuizQuizEntity();
        Instant createdAt = Instant.parse("2026-09-11T10:00:00Z");
        quiz.setCreatedAt(createdAt);

        quiz.prePersist();

        assertThat(quiz.getCreatedAt()).isEqualTo(createdAt);
    }
}
