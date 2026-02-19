package dev.sidequestlab.backend.memoquiz.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AnswerRequest(
	@NotNull
	Long sessionId,

	@NotNull
	Long cardId,

	@NotBlank
	@Size(max = 10000)
	String answer) {}
