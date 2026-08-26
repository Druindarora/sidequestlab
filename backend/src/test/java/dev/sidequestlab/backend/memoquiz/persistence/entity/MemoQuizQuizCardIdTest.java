package dev.sidequestlab.backend.memoquiz.persistence.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemoQuizQuizCardIdTest {

    @Test
    void constructorStoresQuizAndCardIds() {
        MemoQuizQuizCardId id = new MemoQuizQuizCardId(10L, 20L);

        assertThat(id.getQuizId()).isEqualTo(10L);
        assertThat(id.getCardId()).isEqualTo(20L);
    }

    @Test
    void settersUpdateQuizAndCardIds() {
        MemoQuizQuizCardId id = new MemoQuizQuizCardId();

        id.setQuizId(11L);
        id.setCardId(21L);

        assertThat(id.getQuizId()).isEqualTo(11L);
        assertThat(id.getCardId()).isEqualTo(21L);
    }

    @Test
    void equalsUsesQuizAndCardIds() {
        MemoQuizQuizCardId id = new MemoQuizQuizCardId(12L, 22L);

        assertThat(id)
            .isEqualTo(id)
            .isEqualTo(new MemoQuizQuizCardId(12L, 22L))
            .isNotEqualTo(new MemoQuizQuizCardId(13L, 22L))
            .isNotEqualTo(new MemoQuizQuizCardId(12L, 23L))
            .isNotEqualTo(null)
            .isNotEqualTo("12:22");
    }

    @Test
    void hashCodeUsesQuizAndCardIds() {
        MemoQuizQuizCardId id = new MemoQuizQuizCardId(14L, 24L);

        assertThat(id).hasSameHashCodeAs(new MemoQuizQuizCardId(14L, 24L));
    }
}
