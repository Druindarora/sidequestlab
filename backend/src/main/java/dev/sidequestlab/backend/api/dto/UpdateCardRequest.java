package dev.sidequestlab.backend.api.dto;

import dev.sidequestlab.backend.api.enums.CardStatus;

public record UpdateCardRequest(String front, String back, CardStatus status, Integer box) {}
