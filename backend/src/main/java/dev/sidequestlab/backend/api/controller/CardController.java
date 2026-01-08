package dev.sidequestlab.backend.api.controller;

import dev.sidequestlab.backend.api.dto.CardDto;
import dev.sidequestlab.backend.api.dto.CreateCardRequest;
import dev.sidequestlab.backend.api.dto.UpdateCardRequest;
import dev.sidequestlab.backend.api.enums.CardStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/memoquiz")
public class CardController {

    @GetMapping("/cards")
    public ResponseEntity<List<CardDto>> listCards(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) Integer box,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        var now = Instant.now();
        var sample = List.of(new CardDto(1L, "Front 1", "Back 1", CardStatus.ACTIVE, 1, now));
        return ResponseEntity.ok(sample);
    }

    @PostMapping("/cards")
    public ResponseEntity<CardDto> createCard(@RequestBody CreateCardRequest req) {
        var now = Instant.now();
        var dto = new CardDto(42L, req.front(), req.back(), CardStatus.INACTIVE, req.box() == null ? 1 : req.box(), now);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/cards/{id}")
    public ResponseEntity<CardDto> updateCard(@PathVariable Long id, @RequestBody UpdateCardRequest req) {
        var now = Instant.now();
        var status = req.status() == null ? CardStatus.INACTIVE : req.status();
        var dto = new CardDto(id, req.front(), req.back(), status, req.box() == null ? 1 : req.box(), now);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/cards/{id}/activate")
    public ResponseEntity<CardDto> activateCard(@PathVariable Long id) {
        var now = Instant.now();
        var dto = new CardDto(id, "Activated front", "Activated back", CardStatus.ACTIVE, 1, now);
        return ResponseEntity.ok(dto);
    }
}
