package dev.sidequestlab.backend.memoquiz.service;

import dev.sidequestlab.backend.memoquiz.api.dto.AnswerRequest;
import dev.sidequestlab.backend.memoquiz.api.enums.CardStatus;
import dev.sidequestlab.backend.memoquiz.persistence.entity.CardEntity;
import dev.sidequestlab.backend.memoquiz.persistence.entity.MemoQuizQuizCardEntity;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private MemoQuizSettingsRepository settingsRepository;

    @Mock
    private MemoQuizSessionRepository sessionRepository;

    @Mock
    private MemoQuizSessionItemRepository sessionItemRepository;

    @Mock
    private MemoQuizReviewLogRepository reviewLogRepository;

    @Mock
    private MemoQuizQuizCardRepository quizCardRepository;

    @Mock
    private QuizService quizService;

    @Mock
    private ScheduleProvider scheduleProvider;

    @InjectMocks
    private SessionService sessionService;

    @Test
    void computeDayIndexWrapsAtCycleLength() {
        LocalDate start = LocalDate.of(2026, 1, 1);

        assertThat(SessionService.computeDayIndex(start, start)).isEqualTo(1);
        assertThat(SessionService.computeDayIndex(start, start.plusDays(1))).isEqualTo(2);
        assertThat(SessionService.computeDayIndex(start, start.plusDays(64))).isEqualTo(1);
    }

    @Test
    void computeDayIndexWithToday() {
        LocalDate today = LocalDate.now();

        assertThat(SessionService.computeDayIndex(today, today)).isEqualTo(1);
        assertThat(SessionService.computeDayIndex(today.minusDays(1), today)).isEqualTo(2);
        assertThat(SessionService.computeDayIndex(today.minusDays(64), today)).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void todaySessionUsesQuizMembershipBoxes() {
        LocalDate today = LocalDate.now();
        MemoQuizSettingsEntity settings = new MemoQuizSettingsEntity();
        settings.setStartDate(today);

        CardEntity card = new CardEntity();
        card.setId(5L);
        card.setFront("Front");
        card.setBack("Back");
        card.setStatus(CardStatus.ACTIVE);

        MemoQuizQuizCardEntity membership = new MemoQuizQuizCardEntity();
        membership.setQuizId(1L);
        membership.setCard(card);
        membership.setEnabled(true);
        membership.setBox(4);

        when(settingsRepository.findTopByOrderByIdAsc()).thenReturn(Optional.of(settings));
        when(scheduleProvider.boxesForDay(anyInt())).thenReturn(List.of(4));
        when(quizService.getDefaultQuizId()).thenReturn(1L);
        when(quizCardRepository.findEnabledForSession(eq(1L), anyCollection(), eq(CardStatus.ACTIVE), any(Pageable.class)))
            .thenReturn(List.of(membership));
        when(sessionRepository.save(any(MemoQuizSessionEntity.class))).thenAnswer(invocation -> {
            MemoQuizSessionEntity saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });
        when(sessionItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var session = sessionService.getTodaySession();

        assertThat(session.cards()).hasSize(1);
        assertThat(session.cards().get(0).box()).isEqualTo(4);

        ArgumentCaptor<List<MemoQuizSessionItemEntity>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(sessionItemRepository).saveAll(itemsCaptor.capture());
        assertThat(itemsCaptor.getValue()).hasSize(1);
        assertThat(itemsCaptor.getValue().get(0).getBox()).isEqualTo(4);
    }

    @Test
    void answerCorrectAdvancesBox() {
        MemoQuizSessionEntity session = new MemoQuizSessionEntity();
        session.setId(10L);

        MemoQuizSessionItemEntity item = new MemoQuizSessionItemEntity();
        item.setSessionId(10L);
        item.setCardId(5L);
        item.setBox(3);

        CardEntity card = new CardEntity();
        card.setId(5L);
        card.setFront("Front");
        card.setBack("Paris");

        MemoQuizQuizCardEntity membership = new MemoQuizQuizCardEntity();
        membership.setQuizId(1L);
        membership.setCard(card);
        membership.setEnabled(true);
        membership.setBox(3);

        when(sessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(sessionItemRepository.findBySessionIdAndCardId(10L, 5L)).thenReturn(Optional.of(item));
        when(cardRepository.findById(5L)).thenReturn(Optional.of(card));
        when(quizService.getDefaultQuizId()).thenReturn(1L);
        when(quizCardRepository.findByQuizIdAndCardId(1L, 5L)).thenReturn(Optional.of(membership));
        when(quizCardRepository.save(any(MemoQuizQuizCardEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewLogRepository.save(any(MemoQuizReviewLogEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AnswerRequest req = new AnswerRequest(10L, 5L, " paris ");
        var resp = sessionService.answer(req);

        ArgumentCaptor<MemoQuizQuizCardEntity> membershipCaptor = ArgumentCaptor.forClass(MemoQuizQuizCardEntity.class);
        verify(quizCardRepository).save(membershipCaptor.capture());
        assertThat(membershipCaptor.getValue().getBox()).isEqualTo(4);

        ArgumentCaptor<MemoQuizReviewLogEntity> logCaptor = ArgumentCaptor.forClass(MemoQuizReviewLogEntity.class);
        verify(reviewLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getPreviousBox()).isEqualTo(3);
        assertThat(logCaptor.getValue().getNextBox()).isEqualTo(4);
        assertThat(logCaptor.getValue().isCorrect()).isTrue();

        assertThat(resp.correct()).isTrue();
        assertThat(resp.nextReview()).isNotNull();
    }

    @Test
    void answerIncorrectResetsBox() {
        MemoQuizSessionEntity session = new MemoQuizSessionEntity();
        session.setId(11L);

        MemoQuizSessionItemEntity item = new MemoQuizSessionItemEntity();
        item.setSessionId(11L);
        item.setCardId(6L);
        item.setBox(4);

        CardEntity card = new CardEntity();
        card.setId(6L);
        card.setFront("Front");
        card.setBack("Paris");

        MemoQuizQuizCardEntity membership = new MemoQuizQuizCardEntity();
        membership.setQuizId(1L);
        membership.setCard(card);
        membership.setEnabled(true);
        membership.setBox(4);

        when(sessionRepository.findById(11L)).thenReturn(Optional.of(session));
        when(sessionItemRepository.findBySessionIdAndCardId(11L, 6L)).thenReturn(Optional.of(item));
        when(cardRepository.findById(6L)).thenReturn(Optional.of(card));
        when(quizService.getDefaultQuizId()).thenReturn(1L);
        when(quizCardRepository.findByQuizIdAndCardId(1L, 6L)).thenReturn(Optional.of(membership));
        when(quizCardRepository.save(any(MemoQuizQuizCardEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewLogRepository.save(any(MemoQuizReviewLogEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AnswerRequest req = new AnswerRequest(11L, 6L, "wrong");
        var resp = sessionService.answer(req);

        ArgumentCaptor<MemoQuizQuizCardEntity> membershipCaptor = ArgumentCaptor.forClass(MemoQuizQuizCardEntity.class);
        verify(quizCardRepository).save(membershipCaptor.capture());
        assertThat(membershipCaptor.getValue().getBox()).isEqualTo(1);

        ArgumentCaptor<MemoQuizReviewLogEntity> logCaptor = ArgumentCaptor.forClass(MemoQuizReviewLogEntity.class);
        verify(reviewLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getPreviousBox()).isEqualTo(4);
        assertThat(logCaptor.getValue().getNextBox()).isEqualTo(1);
        assertThat(logCaptor.getValue().isCorrect()).isFalse();

        assertThat(resp.correct()).isFalse();
        assertThat(resp.nextReview()).isNotNull();
    }

    @Test
    void answerMissingSessionThrows() {
        when(sessionRepository.findById(20L)).thenReturn(Optional.empty());

        AnswerRequest req = new AnswerRequest(20L, 30L, "answer");

        assertThatThrownBy(() -> sessionService.answer(req))
            .isInstanceOf(ResponseStatusException.class)
            .extracting("statusCode")
            .isEqualTo(HttpStatus.NOT_FOUND);

        verify(sessionItemRepository, never()).findBySessionIdAndCardId(any(Long.class), any(Long.class));
        verify(cardRepository, never()).findById(any(Long.class));
    }

    @Test
    void answerMissingCardThrows() {
        MemoQuizSessionEntity session = new MemoQuizSessionEntity();
        session.setId(21L);

        MemoQuizSessionItemEntity item = new MemoQuizSessionItemEntity();
        item.setSessionId(21L);
        item.setCardId(31L);

        when(sessionRepository.findById(21L)).thenReturn(Optional.of(session));
        when(sessionItemRepository.findBySessionIdAndCardId(21L, 31L)).thenReturn(Optional.of(item));
        when(cardRepository.findById(31L)).thenReturn(Optional.empty());

        AnswerRequest req = new AnswerRequest(21L, 31L, "answer");

        assertThatThrownBy(() -> sessionService.answer(req))
            .isInstanceOf(ResponseStatusException.class)
            .extracting("statusCode")
            .isEqualTo(HttpStatus.NOT_FOUND);

        verify(reviewLogRepository, never()).save(any(MemoQuizReviewLogEntity.class));
    }

    @Test
    void answerMissingMembershipThrows() {
        MemoQuizSessionEntity session = new MemoQuizSessionEntity();
        session.setId(22L);

        MemoQuizSessionItemEntity item = new MemoQuizSessionItemEntity();
        item.setSessionId(22L);
        item.setCardId(32L);

        CardEntity card = new CardEntity();
        card.setId(32L);
        card.setBack("Paris");

        when(sessionRepository.findById(22L)).thenReturn(Optional.of(session));
        when(sessionItemRepository.findBySessionIdAndCardId(22L, 32L)).thenReturn(Optional.of(item));
        when(cardRepository.findById(32L)).thenReturn(Optional.of(card));
        when(quizService.getDefaultQuizId()).thenReturn(1L);
        when(quizCardRepository.findByQuizIdAndCardId(1L, 32L)).thenReturn(Optional.empty());

        AnswerRequest req = new AnswerRequest(22L, 32L, "answer");

        assertThatThrownBy(() -> sessionService.answer(req))
            .isInstanceOf(ResponseStatusException.class)
            .extracting("statusCode")
            .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void answerKeepsBoxSevenWhenCorrect() {
        MemoQuizSessionEntity session = new MemoQuizSessionEntity();
        session.setId(30L);

        MemoQuizSessionItemEntity item = new MemoQuizSessionItemEntity();
        item.setSessionId(30L);
        item.setCardId(40L);
        item.setBox(7);

        CardEntity card = new CardEntity();
        card.setId(40L);
        card.setFront("Front");
        card.setBack("Paris");

        MemoQuizQuizCardEntity membership = new MemoQuizQuizCardEntity();
        membership.setQuizId(1L);
        membership.setCard(card);
        membership.setEnabled(true);
        membership.setBox(7);

        when(sessionRepository.findById(30L)).thenReturn(Optional.of(session));
        when(sessionItemRepository.findBySessionIdAndCardId(30L, 40L)).thenReturn(Optional.of(item));
        when(cardRepository.findById(40L)).thenReturn(Optional.of(card));
        when(quizService.getDefaultQuizId()).thenReturn(1L);
        when(quizCardRepository.findByQuizIdAndCardId(1L, 40L)).thenReturn(Optional.of(membership));
        when(quizCardRepository.save(any(MemoQuizQuizCardEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewLogRepository.save(any(MemoQuizReviewLogEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AnswerRequest req = new AnswerRequest(30L, 40L, " paris ");
        var resp = sessionService.answer(req);

        ArgumentCaptor<MemoQuizQuizCardEntity> membershipCaptor = ArgumentCaptor.forClass(MemoQuizQuizCardEntity.class);
        verify(quizCardRepository).save(membershipCaptor.capture());
        assertThat(membershipCaptor.getValue().getBox()).isEqualTo(7);

        ArgumentCaptor<MemoQuizReviewLogEntity> logCaptor = ArgumentCaptor.forClass(MemoQuizReviewLogEntity.class);
        verify(reviewLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getPreviousBox()).isEqualTo(7);
        assertThat(logCaptor.getValue().getNextBox()).isEqualTo(7);
        assertThat(logCaptor.getValue().isCorrect()).isTrue();

        assertThat(resp.correct()).isTrue();
        assertThat(resp.nextReview()).isNotNull();
    }

    @Test
    void answerResetsBoxFromSevenWhenIncorrect() {
        MemoQuizSessionEntity session = new MemoQuizSessionEntity();
        session.setId(31L);

        MemoQuizSessionItemEntity item = new MemoQuizSessionItemEntity();
        item.setSessionId(31L);
        item.setCardId(41L);
        item.setBox(7);

        CardEntity card = new CardEntity();
        card.setId(41L);
        card.setFront("Front");
        card.setBack("Paris");

        MemoQuizQuizCardEntity membership = new MemoQuizQuizCardEntity();
        membership.setQuizId(1L);
        membership.setCard(card);
        membership.setEnabled(true);
        membership.setBox(7);

        when(sessionRepository.findById(31L)).thenReturn(Optional.of(session));
        when(sessionItemRepository.findBySessionIdAndCardId(31L, 41L)).thenReturn(Optional.of(item));
        when(cardRepository.findById(41L)).thenReturn(Optional.of(card));
        when(quizService.getDefaultQuizId()).thenReturn(1L);
        when(quizCardRepository.findByQuizIdAndCardId(1L, 41L)).thenReturn(Optional.of(membership));
        when(quizCardRepository.save(any(MemoQuizQuizCardEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewLogRepository.save(any(MemoQuizReviewLogEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AnswerRequest req = new AnswerRequest(31L, 41L, "wrong answer");
        var resp = sessionService.answer(req);

        ArgumentCaptor<MemoQuizQuizCardEntity> membershipCaptor = ArgumentCaptor.forClass(MemoQuizQuizCardEntity.class);
        verify(quizCardRepository).save(membershipCaptor.capture());
        assertThat(membershipCaptor.getValue().getBox()).isEqualTo(1);

        ArgumentCaptor<MemoQuizReviewLogEntity> logCaptor = ArgumentCaptor.forClass(MemoQuizReviewLogEntity.class);
        verify(reviewLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getPreviousBox()).isEqualTo(7);
        assertThat(logCaptor.getValue().getNextBox()).isEqualTo(1);
        assertThat(logCaptor.getValue().isCorrect()).isFalse();

        assertThat(resp.correct()).isFalse();
        assertThat(resp.nextReview()).isNotNull();
    }
}
