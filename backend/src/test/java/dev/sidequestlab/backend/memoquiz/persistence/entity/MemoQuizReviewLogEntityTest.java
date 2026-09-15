package dev.sidequestlab.backend.memoquiz.persistence.entity;

import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemoQuizReviewLogEntityTest {

    @Test
    void storesAndReturnsReviewLogProperties() {
        MemoQuizReviewLogEntity log = new MemoQuizReviewLogEntity();
        Instant answeredAt = Instant.parse("2026-09-15T10:00:00Z");

        log.setId(11L);
        log.setSessionId(22L);
        log.setCardId(33L);
        log.setAnsweredAt(answeredAt);
        log.setAnswerText("Paris");
        log.setCorrect(true);
        log.setPreviousBox(2);
        log.setNextBox(3);

        assertThat(log.getId()).isEqualTo(11L);
        assertThat(log.getSessionId()).isEqualTo(22L);
        assertThat(log.getCardId()).isEqualTo(33L);
        assertThat(log.getAnsweredAt()).isEqualTo(answeredAt);
        assertThat(log.getAnswerText()).isEqualTo("Paris");
        assertThat(log.isCorrect()).isTrue();
        assertThat(log.getPreviousBox()).isEqualTo(2);
        assertThat(log.getNextBox()).isEqualTo(3);
    }

    @Test
    void prePersistInitializesMissingAnsweredAt() {
        MemoQuizReviewLogEntity log = new MemoQuizReviewLogEntity();
        Instant before = Instant.now();

        log.prePersist();

        assertThat(log.getAnsweredAt()).isBetween(before, Instant.now());
    }

    @Test
    void prePersistPreservesExistingAnsweredAt() {
        MemoQuizReviewLogEntity log = new MemoQuizReviewLogEntity();
        Instant answeredAt = Instant.parse("2026-09-15T10:00:00Z");
        log.setAnsweredAt(answeredAt);

        log.prePersist();

        assertThat(log.getAnsweredAt()).isEqualTo(answeredAt);
    }
}
