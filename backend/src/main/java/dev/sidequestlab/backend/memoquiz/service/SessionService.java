package dev.sidequestlab.backend.memoquiz.service;

import dev.sidequestlab.backend.memoquiz.api.dto.AnswerRequest;
import dev.sidequestlab.backend.memoquiz.api.dto.AnswerResponse;
import dev.sidequestlab.backend.memoquiz.api.dto.SessionCardDto;
import dev.sidequestlab.backend.memoquiz.api.dto.SessionDto;
import dev.sidequestlab.backend.memoquiz.api.enums.CardStatus;
import dev.sidequestlab.backend.memoquiz.persistence.entity.CardEntity;
import dev.sidequestlab.backend.memoquiz.persistence.entity.MemoQuizQuizCardEntity;
import dev.sidequestlab.backend.memoquiz.persistence.projection.SessionCardProjection;
import dev.sidequestlab.backend.memoquiz.persistence.entity.MemoQuizReviewLogEntity;
import dev.sidequestlab.backend.memoquiz.persistence.entity.MemoQuizSessionEntity;
import dev.sidequestlab.backend.memoquiz.persistence.entity.MemoQuizSessionItemEntity;
import dev.sidequestlab.backend.memoquiz.persistence.entity.MemoQuizSettingsEntity;
import dev.sidequestlab.backend.memoquiz.persistence.repository.CardRepository;
import dev.sidequestlab.backend.memoquiz.persistence.repository.MemoQuizQuizCardRepository;
import dev.sidequestlab.backend.memoquiz.persistence.repository.MemoQuizReviewLogRepository;
import dev.sidequestlab.backend.memoquiz.persistence.repository.MemoQuizSessionItemRepository;
import dev.sidequestlab.backend.memoquiz.persistence.repository.MemoQuizSessionRepository;
import dev.sidequestlab.backend.memoquiz.persistence.repository.MemoQuizSettingsRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Profile("!test")
public class SessionService {

    static final int CYCLE_DAYS = 64;
    private static final int SESSION_CARD_LIMIT = 20;
    private static final int MAX_BOX = 7;

    private final CardRepository cardRepository;
    private final MemoQuizSettingsRepository settingsRepository;
    private final MemoQuizSessionRepository sessionRepository;
    private final MemoQuizSessionItemRepository sessionItemRepository;
    private final MemoQuizReviewLogRepository reviewLogRepository;
    private final ScheduleProvider scheduleProvider;
    private final MemoQuizQuizCardRepository quizCardRepository;
    private final QuizService quizService;

    public SessionService(
        CardRepository cardRepository,
        MemoQuizSettingsRepository settingsRepository,
        MemoQuizSessionRepository sessionRepository,
        MemoQuizSessionItemRepository sessionItemRepository,
        MemoQuizReviewLogRepository reviewLogRepository,
        ScheduleProvider scheduleProvider,
        MemoQuizQuizCardRepository quizCardRepository,
        QuizService quizService
    ) {
        this.cardRepository = cardRepository;
        this.settingsRepository = settingsRepository;
        this.sessionRepository = sessionRepository;
        this.sessionItemRepository = sessionItemRepository;
        this.reviewLogRepository = reviewLogRepository;
        this.scheduleProvider = scheduleProvider;
        this.quizCardRepository = quizCardRepository;
        this.quizService = quizService;
    }

    @Transactional
    public SessionDto getTodaySession() {
        LocalDate today = LocalDate.now();
        MemoQuizSettingsEntity settings = getOrCreateSettings(today);
        int dayIndex = computeDayIndex(settings.getStartDate(), today);
        List<Integer> boxesToday = scheduleProvider.boxesForDay(dayIndex);

        Long quizId = quizService.getDefaultQuizId();
        Pageable pageable = PageRequest.of(0, SESSION_CARD_LIMIT, Sort.by(Sort.Direction.ASC, "cardId"));

        // If there are no boxes scheduled today, create an empty session and return
        if (boxesToday == null || boxesToday.isEmpty()) {
            Instant now = Instant.now();
            MemoQuizSessionEntity session = new MemoQuizSessionEntity();
            session.setStartedAt(now);
            session.setDayIndex(dayIndex);
            MemoQuizSessionEntity savedSession = sessionRepository.save(session);
            return new SessionDto(savedSession.getId(), savedSession.getStartedAt(), List.of());
        }

        List<SessionCardProjection> memberships = quizCardRepository.findEnabledForSession(
            quizId,
            boxesToday,
            CardStatus.ACTIVE,
            pageable
        );

        Instant now = Instant.now();
        MemoQuizSessionEntity session = new MemoQuizSessionEntity();
        session.setStartedAt(now);
        session.setDayIndex(dayIndex);
        MemoQuizSessionEntity savedSession = sessionRepository.save(session);

        List<MemoQuizSessionItemEntity> items = new ArrayList<>();
        for (SessionCardProjection membership : memberships) {
            int box = membership.box();
            MemoQuizSessionItemEntity item = new MemoQuizSessionItemEntity();
            item.setSessionId(savedSession.getId());
            item.setCardId(membership.cardId());
            item.setBox(box);
            items.add(item);
        }
        if (!items.isEmpty()) {
            sessionItemRepository.saveAll(items);
        }

        List<SessionCardDto> cardDtos = memberships.stream()
            .map(membership -> new SessionCardDto(
                membership.cardId(),
                membership.front(),
                membership.back(),
                membership.box()
            ))
            .toList();

        return new SessionDto(savedSession.getId(), savedSession.getStartedAt(), cardDtos);
    }

    @Transactional
    public AnswerResponse answer(AnswerRequest req) {
        MemoQuizSessionEntity session = sessionRepository.findById(req.sessionId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        sessionItemRepository.findBySessionIdAndCardId(req.sessionId(), req.cardId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session item not found"));

        CardEntity card = cardRepository.findById(req.cardId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"));

        Long quizId = quizService.getDefaultQuizId();
        MemoQuizQuizCardEntity membership = quizCardRepository.findByQuizIdAndCardId(quizId, req.cardId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz membership not found"));
        if (!membership.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz membership not found");
        }

        int previousBox = membership.getBox();
        boolean correct = normalize(req.answer()).equals(normalize(card.getBack()));
        int nextBox = correct ? Math.min(previousBox + 1, MAX_BOX) : 1;

        Instant now = Instant.now();
        membership.setBox(nextBox);
        quizCardRepository.save(membership);

        MemoQuizReviewLogEntity log = new MemoQuizReviewLogEntity();
        log.setSessionId(session.getId());
        log.setCardId(card.getId());
        log.setAnsweredAt(now);
        log.setAnswerText(req.answer());
        log.setCorrect(correct);
        log.setPreviousBox(previousBox);
        log.setNextBox(nextBox);
        reviewLogRepository.save(log);

        return new AnswerResponse(correct, now.plus(1, ChronoUnit.DAYS));
    }

    static int computeDayIndex(LocalDate startDate, LocalDate today) {
        long daysBetween = ChronoUnit.DAYS.between(startDate, today);
        long normalized = Math.floorMod(daysBetween, CYCLE_DAYS);
        return (int) normalized + 1;
    }

    private MemoQuizSettingsEntity getOrCreateSettings(LocalDate today) {
        MemoQuizSettingsEntity settings = settingsRepository.findTopByOrderByIdAsc()
            .orElseGet(() -> {
                MemoQuizSettingsEntity created = new MemoQuizSettingsEntity();
                created.setStartDate(today);
                return settingsRepository.save(created);
            });

        if (settings.getStartDate() == null) {
            settings.setStartDate(today);
            settings = settingsRepository.save(settings);
        }

        return settings;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase();
    }
}
