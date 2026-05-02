package dev.sidequestlab.backend.memoquiz.api.dto;

import jakarta.validation.constraints.NotNull;

public record CompleteSessionRequest(
    @NotNull
    Long sessionId
) {}
