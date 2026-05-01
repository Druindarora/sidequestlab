package dev.sidequestlab.backend.memoquiz.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BulkCreateCardItem(
    @NotBlank
    @Size(max = 2000)
    String front,

    @NotBlank
    @Size(max = 10000)
    String back) {}
