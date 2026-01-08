package dev.sidequestlab.backend.api.dto;

import java.time.Instant;

public record AnswerResponse(boolean correct, Instant nextReview) {}
