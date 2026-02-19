package dev.sidequestlab.backend.memoquiz.api.controller;

import dev.sidequestlab.backend.memoquiz.api.dto.AnswerRequest;
import dev.sidequestlab.backend.memoquiz.api.dto.AnswerResponse;
import dev.sidequestlab.backend.memoquiz.api.dto.SessionDto;
import dev.sidequestlab.backend.memoquiz.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
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

    @Operation(summary = "Get today's memoquiz session")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Today's session",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = SessionDto.class)
            )
        )
    })
    @GetMapping(value = "/session/today", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SessionDto> todaySession() {
        return ResponseEntity.ok(sessionService.getTodaySession());
    }

    @PostMapping("/session/answer")
    public ResponseEntity<AnswerResponse> answer(@Valid @RequestBody AnswerRequest req) {
        return ResponseEntity.ok(sessionService.answer(req));
    }
}
