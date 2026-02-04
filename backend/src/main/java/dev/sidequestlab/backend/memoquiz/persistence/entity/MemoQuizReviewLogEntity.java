package dev.sidequestlab.backend.memoquiz.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "memoquiz_review_log")
public class MemoQuizReviewLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "card_id", nullable = false)
    private Long cardId;

    @Column(name = "answered_at", nullable = false)
    private Instant answeredAt;

    @Column(name = "answer_text", nullable = false, length = 10000)
    private String answerText;

    @Column(nullable = false)
    private boolean correct;

    @Column(name = "previous_box", nullable = false)
    private int previousBox;

    @Column(name = "next_box", nullable = false)
    private int nextBox;

    @PrePersist
    void prePersist() {
        if (answeredAt == null) {
            answeredAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Instant getAnsweredAt() {
        return answeredAt;
    }

    public void setAnsweredAt(Instant answeredAt) {
        this.answeredAt = answeredAt;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    public int getPreviousBox() {
        return previousBox;
    }

    public void setPreviousBox(int previousBox) {
        this.previousBox = previousBox;
    }

    public int getNextBox() {
        return nextBox;
    }

    public void setNextBox(int nextBox) {
        this.nextBox = nextBox;
    }
}
