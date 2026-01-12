package dev.sidequestlab.backend.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SessionCardDto(
	@NotNull
	Long cardId,

	@NotBlank
	@Size(max = 2000)
	String front,

	@NotBlank
	@Size(max = 10000)
	String back,

	@NotNull
	@Min(1)
	@Max(7)
	Integer box) {}
