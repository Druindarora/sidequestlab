package dev.sidequestlab.backend.memoquiz.api.controller;

import dev.sidequestlab.backend.memoquiz.api.dto.QuizDto;
import dev.sidequestlab.backend.memoquiz.api.dto.QuizOverviewDto;
import dev.sidequestlab.backend.memoquiz.api.dto.SessionCardDto;
import dev.sidequestlab.backend.memoquiz.service.QuizService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuizControllerTest {

    private final StubQuizService quizService = new StubQuizService();
    private final QuizController controller = new QuizController(quizService);

    @Test
    void listQuizzesReturnsOkAndDelegatesToService() {
        List<QuizDto> expected = List.of(
            new QuizDto(1L, "Default", 12),
            new QuizDto(2L, "Algorithms", 7)
        );
        quizService.listQuizzesResult = expected;

        ResponseEntity<List<QuizDto>> response = controller.listQuizzes();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(expected);
        assertThat(quizService.listQuizzesCallCount).isEqualTo(1);
    }

    @Test
    void overviewReturnsOkAndDelegatesToService() {
        QuizOverviewDto expected = new QuizOverviewDto(3, 24);
        quizService.overviewResult = expected;

        ResponseEntity<QuizOverviewDto> response = controller.overview();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(expected);
        assertThat(quizService.overviewCallCount).isEqualTo(1);
    }

    @Test
    void listDefaultQuizCardsReturnsOkAndDelegatesToService() {
        List<SessionCardDto> expected = List.of(
            new SessionCardDto(11L, "Front A", "Back A", 1),
            new SessionCardDto(12L, "Front B", "Back B", 3)
        );
        quizService.listDefaultQuizCardsResult = expected;

        ResponseEntity<List<SessionCardDto>> response = controller.listDefaultQuizCards();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(expected);
        assertThat(quizService.listDefaultQuizCardsCallCount).isEqualTo(1);
    }

    @Test
    void addCardToDefaultQuizReturnsOkAndDelegatesToService() {
        long cardId = 42L;

        ResponseEntity<Void> response = controller.addCardToDefaultQuiz(cardId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNull();
        assertThat(quizService.addCardToDefaultQuizArg).isEqualTo(cardId);
    }

    @Test
    void removeCardFromDefaultQuizReturnsNoContentAndDelegatesToService() {
        long cardId = 73L;

        ResponseEntity<Void> response = controller.removeCardFromDefaultQuiz(cardId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        assertThat(quizService.removeCardFromDefaultQuizArg).isEqualTo(cardId);
    }

    @Test
    void addCardToDefaultQuizPropagatesServiceExceptionBecauseControllerDoesNotHandleIt() {
        long cardId = 99L;
        RuntimeException failure = new RuntimeException("service failure");
        quizService.addCardToDefaultQuizException = failure;

        assertThatThrownBy(() -> controller.addCardToDefaultQuiz(cardId)).isSameAs(failure);
        assertThat(quizService.addCardToDefaultQuizArg).isEqualTo(cardId);
    }

    private static final class StubQuizService extends QuizService {
        private int listQuizzesCallCount;
        private List<QuizDto> listQuizzesResult;

        private int overviewCallCount;
        private QuizOverviewDto overviewResult;

        private int listDefaultQuizCardsCallCount;
        private List<SessionCardDto> listDefaultQuizCardsResult;

        private Long addCardToDefaultQuizArg;
        private RuntimeException addCardToDefaultQuizException;

        private Long removeCardFromDefaultQuizArg;

        private StubQuizService() {
            super(null, null, null);
        }

        @Override
        public List<QuizDto> listQuizzes() {
            listQuizzesCallCount++;
            return listQuizzesResult;
        }

        @Override
        public QuizOverviewDto overview() {
            overviewCallCount++;
            return overviewResult;
        }

        @Override
        public List<SessionCardDto> listDefaultQuizCards() {
            listDefaultQuizCardsCallCount++;
            return listDefaultQuizCardsResult;
        }

        @Override
        public void addCardToDefaultQuiz(Long cardId) {
            addCardToDefaultQuizArg = cardId;
            if (addCardToDefaultQuizException != null) {
                throw addCardToDefaultQuizException;
            }
        }

        @Override
        public void removeCardFromDefaultQuiz(Long cardId) {
            removeCardFromDefaultQuizArg = cardId;
        }
    }
}
