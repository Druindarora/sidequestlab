package dev.sidequestlab.backend.memoquiz.api.dto;

import dev.sidequestlab.backend.memoquiz.api.enums.CardStatus;
import java.time.Instant;

public record CardDto(Long id, String front, String back, CardStatus status, int box, Instant createdAt) {}
