package dev.sidequestlab.backend.memoquiz.persistence.entity;

import java.io.Serializable;
import java.util.Objects;

public class MemoQuizQuizCardId implements Serializable {

    private Long quizId;
    private Long cardId;

    public MemoQuizQuizCardId() {
    }

    public MemoQuizQuizCardId(Long quizId, Long cardId) {
        this.quizId = quizId;
        this.cardId = cardId;
    }

    public Long getQuizId() {
        return quizId;
    }

    public void setQuizId(Long quizId) {
        this.quizId = quizId;
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
        MemoQuizQuizCardId that = (MemoQuizQuizCardId) o;
        return Objects.equals(quizId, that.quizId) && Objects.equals(cardId, that.cardId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(quizId, cardId);
    }
}
