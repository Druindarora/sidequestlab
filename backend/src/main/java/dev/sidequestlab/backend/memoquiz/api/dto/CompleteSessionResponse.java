package dev.sidequestlab.backend.memoquiz.api.dto;

import java.time.Instant;

public record CompleteSessionResponse(
    Long sessionId,
    Instant endedAt,
    Integer durationSeconds
) {}
