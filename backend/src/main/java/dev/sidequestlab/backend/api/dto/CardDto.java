package dev.sidequestlab.backend.api.dto;

import dev.sidequestlab.backend.api.enums.CardStatus;
import java.time.Instant;

public record CardDto(Long id, String front, String back, CardStatus status, int box, Instant createdAt) {}
