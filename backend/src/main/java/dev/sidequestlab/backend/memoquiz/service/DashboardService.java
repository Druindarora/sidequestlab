package dev.sidequestlab.backend.memoquiz.service;

import dev.sidequestlab.backend.memoquiz.api.dto.TodayDashboardDto;
import dev.sidequestlab.backend.memoquiz.api.enums.CardStatus;
import dev.sidequestlab.backend.memoquiz.persistence.entity.MemoQuizSessionEntity;
import dev.sidequestlab.backend.memoquiz.persistence.projection.BoxOverviewProjection;
import dev.sidequestlab.backend.memoquiz.persistence.repository.MemoQuizQuizCardRepository;
import dev.sidequestlab.backend.memoquiz.persistence.repository.MemoQuizReviewLogRepository;
import dev.sidequestlab.backend.memoquiz.persistence.repository.MemoQuizSessionItemRepository;
import dev.sidequestlab.backend.memoquiz.persistence.repository.MemoQuizSessionRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!test")
public class DashboardService {

    private final MemoQuizSessionRepository sessionRepository;
    private final MemoQuizSessionItemRepository sessionItemRepository;
    private final MemoQuizReviewLogRepository reviewLogRepository;
    private final MemoQuizQuizCardRepository quizCardRepository;
    private final ScheduleProvider scheduleProvider;
    private final QuizService quizService;

    public DashboardService(
        MemoQuizSessionRepository sessionRepository,
        MemoQuizSessionItemRepository sessionItemRepository,
        MemoQuizReviewLogRepository reviewLogRepository,
        MemoQuizQuizCardRepository quizCardRepository,
        ScheduleProvider scheduleProvider,
        QuizService quizService
    ) {
        this.sessionRepository = sessionRepository;
        this.sessionItemRepository = sessionItemRepository;
        this.reviewLogRepository = reviewLogRepository;
        this.quizCardRepository = quizCardRepository;
        this.scheduleProvider = scheduleProvider;
        this.quizService = quizService;
    }

    public TodayDashboardDto today() {
        LocalDate todayDate = LocalDate.now();
        Instant startOfDay = todayDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant startOfNextDay = todayDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        Optional<MemoQuizSessionEntity> todaySession = sessionRepository
            .findTopByStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtDescIdDesc(startOfDay, startOfNextDay);

        int dayIndex;
        boolean canStartSession;
        if (todaySession.isPresent()) {
            dayIndex = todaySession.get().getDayIndex();
            canStartSession = false;
        } else {
            Integer lastDayIndex = sessionRepository.findTopByOrderByStartedAtDescIdDesc()
                .map(MemoQuizSessionEntity::getDayIndex)
                .orElse(null);
            dayIndex = SessionService.nextDayIndex(lastDayIndex, scheduleProvider.scheduleLength());
            canStartSession = true;
        }

        List<Integer> boxesToday = scheduleProvider.boxesForDay(dayIndex);
        Set<Integer> boxesTodaySet = Set.copyOf(boxesToday);

        Long quizId = quizService.getDefaultQuizId();
        long dueCount = quizCardRepository.countEligibleForSession(quizId, boxesToday, CardStatus.ACTIVE);
        // Dashboard due count mirrors session creation by applying the same per-session card cap.
        int dueToday = Math.toIntExact(Math.min(dueCount, SessionService.SESSION_CARD_LIMIT));
        int totalCards = Math.toIntExact(quizCardRepository.countEnabledByQuizIdAndCardStatus(quizId, CardStatus.ACTIVE));

        TodayDashboardDto.LastSessionSummary lastSessionSummary = sessionRepository.findTopByOrderByStartedAtDescIdDesc()
            .map(this::toLastSessionSummary)
            .orElse(null);

        List<TodayDashboardDto.BoxesOverviewItem> boxesOverview = quizCardRepository
            .findEnabledActiveBoxOverview(quizId, CardStatus.ACTIVE)
            .stream()
            .map(box -> toBoxOverviewItem(box, boxesTodaySet))
            .toList();

        return new TodayDashboardDto(
            todayDate,
            dayIndex,
            canStartSession,
            boxesToday,
            dueToday,
            totalCards,
            lastSessionSummary,
            boxesOverview
        );
    }

    private TodayDashboardDto.LastSessionSummary toLastSessionSummary(MemoQuizSessionEntity session) {
        long reviewedCardsCount = reviewLogRepository.countBySessionId(session.getId());
        long goodAnswersCount = reviewLogRepository.countBySessionIdAndCorrectTrue(session.getId());
        if (reviewedCardsCount == 0) {
            reviewedCardsCount = sessionItemRepository.countBySessionId(session.getId());
            goodAnswersCount = 0;
        }

        int reviewedCards = Math.toIntExact(reviewedCardsCount);
        int goodAnswers = Math.toIntExact(Math.min(goodAnswersCount, reviewedCardsCount));
        double successRate = reviewedCards == 0 ? 0.0 : (goodAnswers * 100.0) / reviewedCards;

        return new TodayDashboardDto.LastSessionSummary(
            reviewedCards,
            goodAnswers,
            successRate,
            session.getStartedAt(),
            session.getDayIndex()
        );
    }

    private TodayDashboardDto.BoxesOverviewItem toBoxOverviewItem(BoxOverviewProjection box, Set<Integer> boxesToday) {
        return new TodayDashboardDto.BoxesOverviewItem(
            box.boxNumber(),
            Math.toIntExact(box.cardCount()),
            boxesToday.contains(box.boxNumber())
        );
    }
}
