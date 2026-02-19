package dev.sidequestlab.backend.memoquiz.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "card_progress")
public class CardProgressEntity {

    @Id
    @Column(name = "card_id")
    private Long cardId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "card_id")
    private CardEntity card;

    @Column(nullable = false)
    private int box;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getCardId() {
        return cardId;
    }

    public void setCardId(Long cardId) {
        this.cardId = cardId;
    }

    public CardEntity getCard() {
        return card;
    }

    public void setCard(CardEntity card) {
        this.card = card;
        if (card != null && card.getProgress() != this) {
            card.setProgress(this);
        }
    }

    public int getBox() {
        return box;
    }

    public void setBox(int box) {
        this.box = box;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
