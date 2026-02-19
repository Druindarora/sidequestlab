package dev.sidequestlab.backend.memoquiz.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public record SessionDto(
	@NotNull
	Long id,

	@NotNull
	Instant startedAt,

	@NotNull
	@Size(min = 0)
	List<SessionCardDto> cards) {}
