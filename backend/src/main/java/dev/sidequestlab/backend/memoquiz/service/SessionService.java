package dev.sidequestlab.backend.memoquiz.service;

import dev.sidequestlab.backend.memoquiz.api.dto.AnswerRequest;
import dev.sidequestlab.backend.memoquiz.api.dto.AnswerResponse;
import dev.sidequestlab.backend.memoquiz.api.dto.SessionCardDto;
import dev.sidequestlab.backend.memoquiz.api.dto.SessionDto;
import dev.sidequestlab.backend.memoquiz.api.enums.CardStatus;
import dev.sidequestlab.backend.memoquiz.persistence.entity.CardEntity;
import dev.sidequestlab.backend.memoquiz.persistence.entity.CardProgressEntity;
import dev.sidequestlab.backend.memoquiz.persistence.entity.MemoQuizReviewLogEntity;
import dev.sidequestlab.backend.memoquiz.persistence.entity.MemoQuizSessionEntity;
import dev.sidequestlab.backend.memoquiz.persistence.entity.MemoQuizSessionItemEntity;
import dev.sidequestlab.backend.memoquiz.persistence.entity.MemoQuizSettingsEntity;
import dev.sidequestlab.backend.memoquiz.persistence.repository.CardRepository;
import dev.sidequestlab.backend.memoquiz.persistence.repository.MemoQuizReviewLogRepository;
import dev.sidequestlab.backend.memoquiz.persistence.repository.MemoQuizSessionItemRepository;
import dev.sidequestlab.backend.memoquiz.persistence.repository.MemoQuizSessionRepository;
import dev.sidequestlab.backend.memoquiz.persistence.repository.MemoQuizSettingsRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

    public SessionService(
        CardRepository cardRepository,
        MemoQuizSettingsRepository settingsRepository,
        MemoQuizSessionRepository sessionRepository,
        MemoQuizSessionItemRepository sessionItemRepository,
        MemoQuizReviewLogRepository reviewLogRepository,
        ScheduleProvider scheduleProvider
    ) {
        this.cardRepository = cardRepository;
        this.settingsRepository = settingsRepository;
        this.sessionRepository = sessionRepository;
        this.sessionItemRepository = sessionItemRepository;
        this.reviewLogRepository = reviewLogRepository;
        this.scheduleProvider = scheduleProvider;
    }

    @Transactional
    public SessionDto getTodaySession() {
        LocalDate today = LocalDate.now();
        MemoQuizSettingsEntity settings = getOrCreateSettings(today);
        int dayIndex = computeDayIndex(settings.getStartDate(), today);
        List<Integer> boxesToday = scheduleProvider.boxesForDay(dayIndex);

        Pageable pageable = PageRequest.of(0, SESSION_CARD_LIMIT, Sort.by(Sort.Direction.ASC, "id"));
        List<CardEntity> cards = cardRepository.findAll(activeCardsInBoxes(boxesToday), pageable).getContent();

        Instant now = Instant.now();
        MemoQuizSessionEntity session = new MemoQuizSessionEntity();
        session.setStartedAt(now);
        session.setDayIndex(dayIndex);
        MemoQuizSessionEntity savedSession = sessionRepository.save(session);

        List<MemoQuizSessionItemEntity> items = new ArrayList<>();
        for (CardEntity card : cards) {
            int box = card.getProgress() == null ? 1 : card.getProgress().getBox();
            MemoQuizSessionItemEntity item = new MemoQuizSessionItemEntity();
            item.setSessionId(savedSession.getId());
            item.setCardId(card.getId());
            item.setBox(box);
            items.add(item);
        }
        if (!items.isEmpty()) {
            sessionItemRepository.saveAll(items);
        }

        List<SessionCardDto> cardDtos = cards.stream()
            .map(card -> new SessionCardDto(
                card.getId(),
                card.getFront(),
                card.getBack(),
                card.getProgress() == null ? 1 : card.getProgress().getBox()
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

        CardProgressEntity progress = card.getProgress();
        if (progress == null) {
            progress = new CardProgressEntity();
            progress.setBox(1);
            card.setProgress(progress);
        }

        int previousBox = progress.getBox();
        boolean correct = normalize(req.answer()).equals(normalize(card.getBack()));
        int nextBox = correct ? Math.min(previousBox + 1, MAX_BOX) : 1;

        Instant now = Instant.now();
        progress.setBox(nextBox);
        progress.setUpdatedAt(now);
        card.setUpdatedAt(now);
        cardRepository.save(card);

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

    private Specification<CardEntity> activeCardsInBoxes(List<Integer> boxes) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), CardStatus.ACTIVE));
            predicates.add(root.join("progress", JoinType.INNER).get("box").in(boxes));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase();
    }
}
