package dev.sidequestlab.backend.memoquiz.persistence.entity;

import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CardProgressEntityTest {

    @Test
    void storesAndReturnsCardProgressProperties() {
        CardProgressEntity progress = new CardProgressEntity();
        CardEntity card = new CardEntity();
        Instant updatedAt = Instant.parse("2026-09-15T10:00:00Z");

        progress.setCardId(9L);
        progress.setCard(card);
        progress.setBox(4);
        progress.setUpdatedAt(updatedAt);

        assertThat(progress.getCardId()).isEqualTo(9L);
        assertThat(progress.getCard()).isSameAs(card);
        assertThat(card.getProgress()).isSameAs(progress);
        assertThat(progress.getBox()).isEqualTo(4);
        assertThat(progress.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void setCardAcceptsNullAndAlreadyLinkedCard() {
        CardProgressEntity progress = new CardProgressEntity();
        CardEntity card = new CardEntity();
        card.setProgress(progress);

        progress.setCard(card);
        assertThat(progress.getCard()).isSameAs(card);

        progress.setCard(null);
        assertThat(progress.getCard()).isNull();
        assertThat(card.getProgress()).isSameAs(progress);
    }

    @Test
    void prePersistInitializesMissingUpdatedAt() {
        CardProgressEntity progress = new CardProgressEntity();
        Instant before = Instant.now();

        progress.prePersist();

        assertThat(progress.getUpdatedAt()).isBetween(before, Instant.now());
    }

    @Test
    void prePersistPreservesExistingUpdatedAt() {
        CardProgressEntity progress = new CardProgressEntity();
        Instant updatedAt = Instant.parse("2026-09-15T10:00:00Z");
        progress.setUpdatedAt(updatedAt);

        progress.prePersist();

        assertThat(progress.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void preUpdateRefreshesUpdatedAt() {
        CardProgressEntity progress = new CardProgressEntity();
        progress.setUpdatedAt(Instant.parse("2026-09-15T10:00:00Z"));
        Instant before = Instant.now();

        progress.preUpdate();

        assertThat(progress.getUpdatedAt()).isBetween(before, Instant.now());
    }
}
