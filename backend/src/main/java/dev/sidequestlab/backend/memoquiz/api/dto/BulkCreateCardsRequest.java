package dev.sidequestlab.backend.memoquiz.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record BulkCreateCardsRequest(
    @NotEmpty
    @Size(max = 100)
    List<@Valid BulkCreateCardItem> cards) {}
