package dev.sidequestlab.backend.memoquiz.persistence.entity;

import java.io.Serializable;
import java.util.Objects;

public class MemoQuizSessionItemId implements Serializable {

    private Long sessionId;
    private Long cardId;

    public MemoQuizSessionItemId() {
    }

    public MemoQuizSessionItemId(Long sessionId, Long cardId) {
        this.sessionId = sessionId;
        this.cardId = cardId;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MemoQuizSessionItemId that = (MemoQuizSessionItemId) o;
        return Objects.equals(sessionId, that.sessionId) && Objects.equals(cardId, that.cardId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId, cardId);
    }
}
