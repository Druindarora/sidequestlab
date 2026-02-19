package dev.sidequestlab.backend.memoquiz.service;

import dev.sidequestlab.backend.memoquiz.api.enums.CardStatus;
import dev.sidequestlab.backend.memoquiz.persistence.entity.MemoQuizSessionEntity;
import dev.sidequestlab.backend.memoquiz.persistence.projection.BoxOverviewProjection;
import dev.sidequestlab.backend.memoquiz.persistence.repository.MemoQuizQuizCardRepository;
import dev.sidequestlab.backend.memoquiz.persistence.repository.MemoQuizReviewLogRepository;
import dev.sidequestlab.backend.memoquiz.persistence.repository.MemoQuizSessionItemRepository;
import dev.sidequestlab.backend.memoquiz.persistence.repository.MemoQuizSessionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private MemoQuizSessionRepository sessionRepository;

    @Mock
    private MemoQuizSessionItemRepository sessionItemRepository;

    @Mock
    private MemoQuizReviewLogRepository reviewLogRepository;

    @Mock
    private MemoQuizQuizCardRepository quizCardRepository;

    @Mock
    private ScheduleProvider scheduleProvider;

    @Mock
    private QuizService quizService;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void usesTodaySessionDayIndexAndDisablesStart() {
        MemoQuizSessionEntity todaySession = new MemoQuizSessionEntity();
        todaySession.setId(12L);
        todaySession.setStartedAt(Instant.parse("2026-02-12T09:00:00Z"));
        todaySession.setDayIndex(7);

        when(sessionRepository.findTopByStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtDescIdDesc(any(), any()))
            .thenReturn(Optional.of(todaySession));
        when(sessionRepository.findTopByOrderByStartedAtDescIdDesc()).thenReturn(Optional.of(todaySession));
        when(scheduleProvider.boxesForDay(7)).thenReturn(List.of(1, 4));
        when(quizService.getDefaultQuizId()).thenReturn(1L);
        when(quizCardRepository.countEligibleForSession(eq(1L), anyCollection(), eq(CardStatus.ACTIVE))).thenReturn(5L);
        when(quizCardRepository.countEnabledByQuizIdAndCardStatus(1L, CardStatus.ACTIVE)).thenReturn(17L);
        when(reviewLogRepository.countBySessionId(12L)).thenReturn(4L);
        when(reviewLogRepository.countBySessionIdAndCorrectTrue(12L)).thenReturn(3L);
        when(quizCardRepository.findEnabledActiveBoxOverview(1L, CardStatus.ACTIVE))
            .thenReturn(List.of(
                new BoxOverviewProjection(1, 11L),
                new BoxOverviewProjection(2, 5L),
                new BoxOverviewProjection(4, 1L)
            ));

        var dashboard = dashboardService.today();

        assertThat(dashboard.dayIndex()).isEqualTo(7);
        assertThat(dashboard.canStartSession()).isFalse();
        assertThat(dashboard.boxesToday()).containsExactly(1, 4);
        assertThat(dashboard.dueToday()).isEqualTo(5);
        assertThat(dashboard.totalCards()).isEqualTo(17);
        assertThat(dashboard.lastSessionSummary()).isNotNull();
        assertThat(dashboard.lastSessionSummary().reviewedCards()).isEqualTo(4);
        assertThat(dashboard.lastSessionSummary().goodAnswers()).isEqualTo(3);
        assertThat(dashboard.lastSessionSummary().successRate()).isEqualTo(75.0);
        assertThat(dashboard.boxesOverview())
            .extracting(box -> box.boxNumber(), box -> box.isToday())
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(1, true),
                org.assertj.core.groups.Tuple.tuple(2, false),
                org.assertj.core.groups.Tuple.tuple(4, true)
            );

        verify(scheduleProvider, never()).scheduleLength();
    }

    @Test
    void computesNextDayIndexWhenNoSessionToday() {
        MemoQuizSessionEntity lastSession = new MemoQuizSessionEntity();
        lastSession.setId(20L);
        lastSession.setStartedAt(Instant.parse("2026-02-11T09:00:00Z"));
        lastSession.setDayIndex(12);

        when(sessionRepository.findTopByStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtDescIdDesc(any(), any()))
            .thenReturn(Optional.empty());
        when(sessionRepository.findTopByOrderByStartedAtDescIdDesc()).thenReturn(Optional.of(lastSession));
        when(scheduleProvider.scheduleLength()).thenReturn(64);
        when(scheduleProvider.boxesForDay(13)).thenReturn(List.of(2, 1));
        when(quizService.getDefaultQuizId()).thenReturn(1L);
        when(quizCardRepository.countEligibleForSession(eq(1L), anyCollection(), eq(CardStatus.ACTIVE))).thenReturn(9L);
        when(quizCardRepository.countEnabledByQuizIdAndCardStatus(1L, CardStatus.ACTIVE)).thenReturn(31L);
        when(reviewLogRepository.countBySessionId(20L)).thenReturn(0L);
        when(reviewLogRepository.countBySessionIdAndCorrectTrue(20L)).thenReturn(0L);
        when(sessionItemRepository.countBySessionId(20L)).thenReturn(0L);
        when(quizCardRepository.findEnabledActiveBoxOverview(1L, CardStatus.ACTIVE)).thenReturn(List.of());

        var dashboard = dashboardService.today();

        assertThat(dashboard.dayIndex()).isEqualTo(13);
        assertThat(dashboard.canStartSession()).isTrue();
        assertThat(dashboard.boxesToday()).containsExactly(2, 1);
        assertThat(dashboard.dueToday()).isEqualTo(9);
        verify(scheduleProvider).scheduleLength();
    }

    @Test
    void wrapsDayIndexUsingScheduleLengthFromProvider() {
        MemoQuizSessionEntity lastSession = new MemoQuizSessionEntity();
        lastSession.setId(33L);
        lastSession.setStartedAt(Instant.parse("2026-02-11T10:00:00Z"));
        lastSession.setDayIndex(3);

        when(sessionRepository.findTopByStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtDescIdDesc(any(), any()))
            .thenReturn(Optional.empty());
        when(sessionRepository.findTopByOrderByStartedAtDescIdDesc()).thenReturn(Optional.of(lastSession));
        when(scheduleProvider.scheduleLength()).thenReturn(3);
        when(scheduleProvider.boxesForDay(1)).thenReturn(List.of(1));
        when(quizService.getDefaultQuizId()).thenReturn(1L);
        when(quizCardRepository.countEligibleForSession(eq(1L), anyCollection(), eq(CardStatus.ACTIVE))).thenReturn(25L);
        when(quizCardRepository.countEnabledByQuizIdAndCardStatus(1L, CardStatus.ACTIVE)).thenReturn(40L);
        when(reviewLogRepository.countBySessionId(33L)).thenReturn(0L);
        when(reviewLogRepository.countBySessionIdAndCorrectTrue(33L)).thenReturn(0L);
        when(sessionItemRepository.countBySessionId(33L)).thenReturn(7L);
        when(quizCardRepository.findEnabledActiveBoxOverview(1L, CardStatus.ACTIVE))
            .thenReturn(List.of(new BoxOverviewProjection(1, 2L)));

        var dashboard = dashboardService.today();

        assertThat(dashboard.dayIndex()).isEqualTo(1);
        assertThat(dashboard.canStartSession()).isTrue();
        assertThat(dashboard.dueToday()).isEqualTo(20);
        assertThat(dashboard.lastSessionSummary()).isNotNull();
        assertThat(dashboard.lastSessionSummary().reviewedCards()).isEqualTo(7);
        assertThat(dashboard.lastSessionSummary().goodAnswers()).isEqualTo(0);
        verify(scheduleProvider).boxesForDay(1);
    }
}
