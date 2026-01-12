package dev.sidequestlab.backend.api.dto;

import dev.sidequestlab.backend.api.enums.CardStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateCardRequest(
	@Size(max = 2000)
	String front,

	@Size(max = 10000)
	String back,

	CardStatus status,

	@Min(1)
	@Max(7)
	Integer box) {}
