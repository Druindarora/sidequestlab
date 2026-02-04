package dev.sidequestlab.backend.memoquiz.api.controller;

import dev.sidequestlab.backend.memoquiz.api.dto.QuizDto;
import dev.sidequestlab.backend.memoquiz.api.dto.QuizOverviewDto;
import dev.sidequestlab.backend.memoquiz.api.dto.SessionCardDto;
import dev.sidequestlab.backend.memoquiz.service.QuizService;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/memoquiz")
@Profile("!test")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping("/quiz")
    public ResponseEntity<List<QuizDto>> listQuizzes() {
        return ResponseEntity.ok(quizService.listQuizzes());
    }

    @GetMapping("/quiz/overview")
    public ResponseEntity<QuizOverviewDto> overview() {
        return ResponseEntity.ok(quizService.overview());
    }

    @GetMapping("/quizzes/default/cards")
    public ResponseEntity<List<SessionCardDto>> listDefaultQuizCards() {
        return ResponseEntity.ok(quizService.listDefaultQuizCards());
    }

    @PostMapping("/quizzes/default/cards/{cardId}")
    public ResponseEntity<Void> addCardToDefaultQuiz(@PathVariable Long cardId) {
        quizService.addCardToDefaultQuiz(cardId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/quizzes/default/cards/{cardId}")
    public ResponseEntity<Void> removeCardFromDefaultQuiz(@PathVariable Long cardId) {
        quizService.removeCardFromDefaultQuiz(cardId);
        return ResponseEntity.noContent().build();
    }
}
