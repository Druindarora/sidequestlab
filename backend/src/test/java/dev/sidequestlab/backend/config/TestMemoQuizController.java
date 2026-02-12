package dev.sidequestlab.backend.config;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class TestMemoQuizController {

    @GetMapping("/api/memoquiz/dashboard/today")
    Map<String, String> today() {
        return Map.of("status", "ok");
    }
}
