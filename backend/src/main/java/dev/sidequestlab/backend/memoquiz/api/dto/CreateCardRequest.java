package dev.sidequestlab.backend.memoquiz.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCardRequest(
	@NotBlank
	@Size(max = 2000)
	String front,

	@NotBlank
	@Size(max = 10000)
	String back,

	@Min(1)
	@Max(7)
	Integer box) {}
