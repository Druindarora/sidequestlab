package dev.sidequestlab.backend.memoquiz.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "memoquiz_session_item")
@IdClass(MemoQuizSessionItemId.class)
public class MemoQuizSessionItemEntity {

    @Id
    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Id
    @Column(name = "card_id", nullable = false)
    private Long cardId;

    @Column(nullable = false)
    private int box;

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getCardId() {
        return cardId;
    }

    public void setCardId(Long cardId) {
        this.cardId = cardId;
    }

    public int getBox() {
        return box;
    }

    public void setBox(int box) {
        this.box = box;
    }
}
