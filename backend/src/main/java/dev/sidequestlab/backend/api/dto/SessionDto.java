package dev.sidequestlab.backend.api.dto;

import java.time.Instant;
import java.util.List;

public record SessionDto(Long id, Instant startedAt, List<SessionCardDto> cards) {}
