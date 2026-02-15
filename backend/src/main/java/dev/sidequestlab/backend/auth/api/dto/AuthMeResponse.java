package dev.sidequestlab.backend.auth.api.dto;

public record AuthMeResponse(
    String email,
    boolean mustChangePassword
) {
}
