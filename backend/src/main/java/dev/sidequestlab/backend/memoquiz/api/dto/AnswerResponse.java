package dev.sidequestlab.backend.memoquiz.api.dto;

import java.time.Instant;

public record AnswerResponse(boolean correct, Instant nextReview) {}
