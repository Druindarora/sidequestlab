package dev.sidequestlab.backend.memoquiz.api.controller;


import dev.sidequestlab.backend.memoquiz.api.dto.TodayDashboardDto;
import dev.sidequestlab.backend.memoquiz.service.DashboardService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@RestController
@Validated
@RequestMapping("/api/memoquiz")
@Profile("!test")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard/today")
    public ResponseEntity<TodayDashboardDto> today() {
        return ResponseEntity.ok(dashboardService.today());
    }
}
