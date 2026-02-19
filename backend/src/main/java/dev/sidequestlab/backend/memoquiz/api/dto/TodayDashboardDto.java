package dev.sidequestlab.backend.memoquiz.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TodayDashboardDto(
    LocalDate todayDate,
    int dayIndex,
    boolean canStartSession,
    List<Integer> boxesToday,
    int dueToday,
    int totalCards,
    LastSessionSummary lastSessionSummary,
    List<BoxesOverviewItem> boxesOverview
) {

    public record LastSessionSummary(
        int reviewedCards,
        int goodAnswers,
        double successRate,
        Instant startedAt,
        int dayIndex
    ) {}

    public record BoxesOverviewItem(
        int boxNumber,
        int cardCount,
        boolean isToday
    ) {}
}
