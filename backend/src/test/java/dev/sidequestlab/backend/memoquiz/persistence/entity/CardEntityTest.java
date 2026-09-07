package dev.sidequestlab.backend.memoquiz.persistence.entity;

import dev.sidequestlab.backend.memoquiz.api.enums.CardStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CardEntityTest {

    @Test
    void storesAndReturnsCardProperties() {
        CardEntity card = new CardEntity();
        Instant createdAt = Instant.parse("2026-08-28T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-08-28T11:00:00Z");

        card.setId(42L);
        card.setFront("Question");
        card.setBack("Answer");
        card.setStatus(CardStatus.ACTIVE);
        card.setCreatedAt(createdAt);
        card.setUpdatedAt(updatedAt);

        assertThat(card.getId()).isEqualTo(42L);
        assertThat(card.getFront()).isEqualTo("Question");
        assertThat(card.getBack()).isEqualTo("Answer");
        assertThat(card.getStatus()).isEqualTo(CardStatus.ACTIVE);
        assertThat(card.getCreatedAt()).isEqualTo(createdAt);
        assertThat(card.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void prePersistInitializesMissingTimestamps() {
        CardEntity card = new CardEntity();
        Instant before = Instant.now();

        card.prePersist();

        Instant after = Instant.now();
        assertThat(card.getCreatedAt()).isBetween(before, after);
        assertThat(card.getUpdatedAt()).isEqualTo(card.getCreatedAt());
    }

    @Test
    void prePersistPreservesExistingTimestamps() {
        CardEntity card = new CardEntity();
        Instant createdAt = Instant.parse("2026-08-28T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-08-28T11:00:00Z");
        card.setCreatedAt(createdAt);
        card.setUpdatedAt(updatedAt);

        card.prePersist();

        assertThat(card.getCreatedAt()).isEqualTo(createdAt);
        assertThat(card.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void preUpdateRefreshesUpdatedAt() {
        CardEntity card = new CardEntity();
        card.setUpdatedAt(Instant.parse("2026-08-28T10:00:00Z"));
        Instant before = Instant.now();

        card.preUpdate();

        assertThat(card.getUpdatedAt()).isBetween(before, Instant.now());
    }

    @Test
    void setProgressKeepsBidirectionalAssociationInSync() {
        CardEntity card = new CardEntity();
        CardProgressEntity progress = new CardProgressEntity();

        card.setProgress(progress);

        assertThat(card.getProgress()).isSameAs(progress);
        assertThat(progress.getCard()).isSameAs(card);
    }

    @Test
    void setProgressAcceptsNullAndAlreadyLinkedProgress() {
        CardEntity card = new CardEntity();
        CardProgressEntity progress = new CardProgressEntity();
        progress.setCard(card);

        card.setProgress(progress);
        assertThat(card.getProgress()).isSameAs(progress);

        card.setProgress(null);
        assertThat(card.getProgress()).isNull();
        assertThat(progress.getCard()).isSameAs(card);
    }
}
