package dev.sidequestlab.backend.memoquiz.api.controller;

import dev.sidequestlab.backend.memoquiz.api.dto.CardDto;
import dev.sidequestlab.backend.memoquiz.api.dto.CreateCardRequest;
import dev.sidequestlab.backend.memoquiz.api.dto.UpdateCardRequest;
import dev.sidequestlab.backend.memoquiz.api.enums.CardStatus;
import dev.sidequestlab.backend.memoquiz.service.CardService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/memoquiz")
@Profile("!test")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping("/cards")
    public ResponseEntity<List<CardDto>> listCards(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) @Min(1) Integer box,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            @RequestParam(required = false) String sort
    ) {
        return ResponseEntity.ok(cardService.listCards(q, status, box, page, size, sort));
    }

    @PostMapping("/cards")
    public ResponseEntity<CardDto> createCard(@Valid @RequestBody CreateCardRequest req) {
        return ResponseEntity.ok(cardService.createCard(req));
    }

    @PutMapping("/cards/{id}")
    public ResponseEntity<CardDto> updateCard(@PathVariable @Min(1) Long id, @Valid @RequestBody UpdateCardRequest req) {
        return ResponseEntity.ok(cardService.updateCard(id, req));
    }

    @PostMapping("/cards/{id}/activate")
    public ResponseEntity<CardDto> activateCard(@PathVariable @Min(1) Long id) {
        return ResponseEntity.ok(cardService.activateCard(id));
    }
}
