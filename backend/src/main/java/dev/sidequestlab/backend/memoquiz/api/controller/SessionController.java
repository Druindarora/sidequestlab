package dev.sidequestlab.backend.memoquiz.api.controller;

import dev.sidequestlab.backend.memoquiz.api.dto.AnswerRequest;
import dev.sidequestlab.backend.memoquiz.api.dto.AnswerResponse;
import dev.sidequestlab.backend.memoquiz.api.dto.SessionDto;
import dev.sidequestlab.backend.memoquiz.service.SessionService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/memoquiz")
@Validated
@Profile("!test")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping("/session/today")
    public ResponseEntity<SessionDto> todaySession() {
        return ResponseEntity.ok(sessionService.getTodaySession());
    }

    @PostMapping("/session/answer")
    public ResponseEntity<AnswerResponse> answer(@Valid @RequestBody AnswerRequest req) {
        return ResponseEntity.ok(sessionService.answer(req));
    }
}
