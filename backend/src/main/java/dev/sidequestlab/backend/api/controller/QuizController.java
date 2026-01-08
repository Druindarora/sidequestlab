package dev.sidequestlab.backend.api.controller;

import dev.sidequestlab.backend.api.dto.QuizDto;
import dev.sidequestlab.backend.api.dto.QuizOverviewDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/memoquiz")
public class QuizController {

    @GetMapping("/quiz")
    public ResponseEntity<List<QuizDto>> listQuizzes() {
        var q = List.of(new QuizDto(1L, "Demo Quiz", 10));
        return ResponseEntity.ok(q);
    }

    @GetMapping("/quiz/overview")
    public ResponseEntity<QuizOverviewDto> overview() {
        return ResponseEntity.ok(new QuizOverviewDto(1, 10));
    }
}
