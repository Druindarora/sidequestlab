package dev.sidequestlab.backend.memoquiz.persistence.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemoQuizSessionItemIdTest {

    @Test
    void constructorStoresSessionAndCardIds() {
        MemoQuizSessionItemId id = new MemoQuizSessionItemId(10L, 20L);

        assertThat(id.getSessionId()).isEqualTo(10L);
        assertThat(id.getCardId()).isEqualTo(20L);
    }

    @Test
    void settersUpdateSessionAndCardIds() {
        MemoQuizSessionItemId id = new MemoQuizSessionItemId();

        id.setSessionId(11L);
        id.setCardId(21L);

        assertThat(id.getSessionId()).isEqualTo(11L);
        assertThat(id.getCardId()).isEqualTo(21L);
    }

    @Test
    void equalsUsesSessionAndCardIds() {
        MemoQuizSessionItemId id = new MemoQuizSessionItemId(12L, 22L);

        assertThat(id)
            .isEqualTo(id)
            .isEqualTo(new MemoQuizSessionItemId(12L, 22L))
            .isNotEqualTo(new MemoQuizSessionItemId(13L, 22L))
            .isNotEqualTo(new MemoQuizSessionItemId(12L, 23L))
            .isNotEqualTo(null)
            .isNotEqualTo("12:22");
    }

    @Test
    void hashCodeUsesSessionAndCardIds() {
        MemoQuizSessionItemId id = new MemoQuizSessionItemId(14L, 24L);

        assertThat(id).hasSameHashCodeAs(new MemoQuizSessionItemId(14L, 24L));
    }
}
