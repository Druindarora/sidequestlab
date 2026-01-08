package dev.sidequestlab.backend.api.controller;

import dev.sidequestlab.backend.api.dto.*;
import dev.sidequestlab.backend.api.enums.CardStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/memoquiz")
public class SessionController {

    @GetMapping("/session/today")
    public ResponseEntity<SessionDto> todaySession() {
        var cards = List.of(new SessionCardDto(1L, "Front A", "Back A", 1));
        var session = new SessionDto(100L, Instant.now(), cards);
        return ResponseEntity.ok(session);
    }

    @PostMapping("/session/answer")
    public ResponseEntity<AnswerResponse> answer(@RequestBody AnswerRequest req) {
        var next = Instant.now().plusSeconds(60 * 60 * 24);
        var resp = new AnswerResponse(true, next);
        return ResponseEntity.ok(resp);
    }
}
