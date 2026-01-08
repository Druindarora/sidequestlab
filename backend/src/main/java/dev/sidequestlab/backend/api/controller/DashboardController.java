package dev.sidequestlab.backend.api.controller;

import dev.sidequestlab.backend.api.dto.TodayDashboardDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/memoquiz")
public class DashboardController {

    @GetMapping("/dashboard/today")
    public ResponseEntity<TodayDashboardDto> today() {
        return ResponseEntity.ok(new TodayDashboardDto(5, 120));
    }
}
