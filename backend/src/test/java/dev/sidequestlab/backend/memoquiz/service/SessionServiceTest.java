package dev.sidequestlab.backend.memoquiz.service;

import dev.sidequestlab.backend.memoquiz.api.dto.AnswerRequest;
import dev.sidequestlab.backend.memoquiz.persistence.entity.CardEntity;
import dev.sidequestlab.backend.memoquiz.persistence.entity.CardProgressEntity;
import dev.sidequestlab.backend.memoquiz.persistence.entity.MemoQuizReviewLogEntity;
import dev.sidequestlab.backend.memoquiz.persistence.entity.MemoQuizSessionEntity;
import dev.sidequestlab.backend.memoquiz.persistence.entity.MemoQuizSessionItemEntity;
import dev.sidequestlab.backend.memoquiz.persistence.repository.CardRepository;
import dev.sidequestlab.backend.memoquiz.persistence.repository.MemoQuizReviewLogRepository;
import dev.sidequestlab.backend.memoquiz.persistence.repository.MemoQuizSessionItemRepository;
import dev.sidequestlab.backend.memoquiz.persistence.repository.MemoQuizSessionRepository;
import dev.sidequestlab.backend.memoquiz.persistence.repository.MemoQuizSettingsRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
        CardProgressEntity progress = new CardProgressEntity();
        progress.setBox(3);
        card.setProgress(progress);

        when(sessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(sessionItemRepository.findBySessionIdAndCardId(10L, 5L)).thenReturn(Optional.of(item));
        when(cardRepository.findById(5L)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(CardEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewLogRepository.save(any(MemoQuizReviewLogEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AnswerRequest req = new AnswerRequest(10L, 5L, " paris ");
        var resp = sessionService.answer(req);

        ArgumentCaptor<CardEntity> cardCaptor = ArgumentCaptor.forClass(CardEntity.class);
        verify(cardRepository).save(cardCaptor.capture());
        assertThat(cardCaptor.getValue().getProgress().getBox()).isEqualTo(4);

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
        CardProgressEntity progress = new CardProgressEntity();
        progress.setBox(4);
        card.setProgress(progress);

        when(sessionRepository.findById(11L)).thenReturn(Optional.of(session));
        when(sessionItemRepository.findBySessionIdAndCardId(11L, 6L)).thenReturn(Optional.of(item));
        when(cardRepository.findById(6L)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(CardEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewLogRepository.save(any(MemoQuizReviewLogEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AnswerRequest req = new AnswerRequest(11L, 6L, "wrong");
        var resp = sessionService.answer(req);

        ArgumentCaptor<CardEntity> cardCaptor = ArgumentCaptor.forClass(CardEntity.class);
        verify(cardRepository).save(cardCaptor.capture());
        assertThat(cardCaptor.getValue().getProgress().getBox()).isEqualTo(1);

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
        CardProgressEntity progress = new CardProgressEntity();
        progress.setBox(7);
        card.setProgress(progress);

        when(sessionRepository.findById(30L)).thenReturn(Optional.of(session));
        when(sessionItemRepository.findBySessionIdAndCardId(30L, 40L)).thenReturn(Optional.of(item));
        when(cardRepository.findById(40L)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(CardEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewLogRepository.save(any(MemoQuizReviewLogEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // answer matches after trim+lowercase
        AnswerRequest req = new AnswerRequest(30L, 40L, " paris ");
        var resp = sessionService.answer(req);

        ArgumentCaptor<CardEntity> cardCaptor = ArgumentCaptor.forClass(CardEntity.class);
        verify(cardRepository).save(cardCaptor.capture());
        assertThat(cardCaptor.getValue().getProgress().getBox()).isEqualTo(7);

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
        CardProgressEntity progress = new CardProgressEntity();
        progress.setBox(7);
        card.setProgress(progress);

        when(sessionRepository.findById(31L)).thenReturn(Optional.of(session));
        when(sessionItemRepository.findBySessionIdAndCardId(31L, 41L)).thenReturn(Optional.of(item));
        when(cardRepository.findById(41L)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(CardEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewLogRepository.save(any(MemoQuizReviewLogEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AnswerRequest req = new AnswerRequest(31L, 41L, "wrong answer");
        var resp = sessionService.answer(req);

        ArgumentCaptor<CardEntity> cardCaptor = ArgumentCaptor.forClass(CardEntity.class);
        verify(cardRepository).save(cardCaptor.capture());
        assertThat(cardCaptor.getValue().getProgress().getBox()).isEqualTo(1);

        ArgumentCaptor<MemoQuizReviewLogEntity> logCaptor = ArgumentCaptor.forClass(MemoQuizReviewLogEntity.class);
        verify(reviewLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getPreviousBox()).isEqualTo(7);
        assertThat(logCaptor.getValue().getNextBox()).isEqualTo(1);
        assertThat(logCaptor.getValue().isCorrect()).isFalse();

        assertThat(resp.correct()).isFalse();
        assertThat(resp.nextReview()).isNotNull();
    }
}
