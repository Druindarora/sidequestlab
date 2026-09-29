package dev.sidequestlab.backend.memoquiz.persistence.entity;

import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemoQuizQuizCardEntityTest {

    @Test
    void storesAndReturnsQuizCardProperties() {
        MemoQuizQuizCardEntity quizCard = new MemoQuizQuizCardEntity();
        MemoQuizQuizEntity quiz = new MemoQuizQuizEntity();
        CardEntity card = new CardEntity();
        Instant addedAt = Instant.parse("2026-09-15T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-09-15T10:05:00Z");
        quiz.setId(4L);
        card.setId(5L);

        quizCard.setQuizId(1L);
        quizCard.setCardId(2L);
        quizCard.setQuiz(quiz);
        quizCard.setCard(card);
        quizCard.setEnabled(false);
        quizCard.setBox(3);
        quizCard.setAddedAt(addedAt);
        quizCard.setUpdatedAt(updatedAt);

        assertThat(quizCard.getQuizId()).isEqualTo(4L);
        assertThat(quizCard.getCardId()).isEqualTo(5L);
        assertThat(quizCard.getQuiz()).isSameAs(quiz);
        assertThat(quizCard.getCard()).isSameAs(card);
        assertThat(quizCard.isEnabled()).isFalse();
        assertThat(quizCard.getBox()).isEqualTo(3);
        assertThat(quizCard.getAddedAt()).isEqualTo(addedAt);
        assertThat(quizCard.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void newQuizCardIsEnabledByDefault() {
        assertThat(new MemoQuizQuizCardEntity().isEnabled()).isTrue();
    }

    @Test
    void nullAssociationsDoNotOverwriteIds() {
        MemoQuizQuizCardEntity quizCard = new MemoQuizQuizCardEntity();
        quizCard.setQuizId(1L);
        quizCard.setCardId(2L);

        quizCard.setQuiz(null);
        quizCard.setCard(null);

        assertThat(quizCard.getQuiz()).isNull();
        assertThat(quizCard.getCard()).isNull();
        assertThat(quizCard.getQuizId()).isEqualTo(1L);
        assertThat(quizCard.getCardId()).isEqualTo(2L);
    }

    @Test
    void prePersistInitializesMissingTimestamps() {
        MemoQuizQuizCardEntity quizCard = new MemoQuizQuizCardEntity();
        Instant before = Instant.now();

        quizCard.prePersist();

        Instant after = Instant.now();
        assertThat(quizCard.getAddedAt()).isBetween(before, after);
        assertThat(quizCard.getUpdatedAt()).isBetween(before, after);
    }

    @Test
    void prePersistPreservesExistingTimestamps() {
        MemoQuizQuizCardEntity quizCard = new MemoQuizQuizCardEntity();
        Instant addedAt = Instant.parse("2026-09-15T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-09-15T10:05:00Z");
        quizCard.setAddedAt(addedAt);
        quizCard.setUpdatedAt(updatedAt);

        quizCard.prePersist();

        assertThat(quizCard.getAddedAt()).isEqualTo(addedAt);
        assertThat(quizCard.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void preUpdateRefreshesUpdatedAt() {
        MemoQuizQuizCardEntity quizCard = new MemoQuizQuizCardEntity();
        quizCard.setUpdatedAt(Instant.parse("2026-09-15T10:00:00Z"));
        Instant before = Instant.now();

        quizCard.preUpdate();

        assertThat(quizCard.getUpdatedAt()).isBetween(before, Instant.now());
    }
}
