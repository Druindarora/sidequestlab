package dev.sidequestlab.backend.api.dto;

public record AnswerRequest(Long sessionId, Long cardId, String answer) {}
