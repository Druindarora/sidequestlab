package dev.sidequestlab.backend.memoquiz.api.controller;

import dev.sidequestlab.backend.memoquiz.api.dto.AnswerRequest;
import dev.sidequestlab.backend.memoquiz.api.dto.AnswerResponse;
import dev.sidequestlab.backend.memoquiz.api.dto.SessionCardDto;
import dev.sidequestlab.backend.memoquiz.api.dto.SessionDto;
import dev.sidequestlab.backend.memoquiz.service.SessionService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionControllerTest {

    private final StubSessionService sessionService = new StubSessionService();
    private final SessionController controller = new SessionController(sessionService);

    @Test
    void todaySessionReturnsOkAndDelegatesToService() {
        SessionDto expected = new SessionDto(
            12L,
            Instant.parse("2026-04-21T09:30:00Z"),
            List.of(
                new SessionCardDto(101L, "Front A", "Back A", 1),
                new SessionCardDto(102L, "Front B", "Back B", 3)
            )
        );
        sessionService.todayResult = expected;

        ResponseEntity<SessionDto> response = controller.todaySession();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(expected);
        assertThat(sessionService.todayCallCount).isEqualTo(1);
    }

    @Test
    void answerReturnsOkAndDelegatesToService() {
        AnswerRequest req = new AnswerRequest(7L, 44L, "candidate answer");
        AnswerResponse expected = new AnswerResponse(true, Instant.parse("2026-04-22T10:00:00Z"));
        sessionService.answerResult = expected;

        ResponseEntity<AnswerResponse> response = controller.answer(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(expected);
        assertThat(sessionService.answerRequestArg).isEqualTo(req);
    }

    @Test
    void todaySessionPropagatesServiceExceptionBecauseControllerDoesNotHandleIt() {
        RuntimeException failure = new RuntimeException("service failure");
        sessionService.todayException = failure;

        assertThatThrownBy(() -> controller.todaySession()).isSameAs(failure);
        assertThat(sessionService.todayCallCount).isEqualTo(1);
    }

    @Test
    void answerPropagatesServiceExceptionBecauseControllerDoesNotHandleIt() {
        AnswerRequest req = new AnswerRequest(9L, 88L, "wrong answer");
        RuntimeException failure = new RuntimeException("service failure");
        sessionService.answerException = failure;

        assertThatThrownBy(() -> controller.answer(req)).isSameAs(failure);
        assertThat(sessionService.answerRequestArg).isEqualTo(req);
    }

    private static final class StubSessionService extends SessionService {
        private int todayCallCount;
        private SessionDto todayResult;
        private RuntimeException todayException;

        private AnswerRequest answerRequestArg;
        private AnswerResponse answerResult;
        private RuntimeException answerException;

        private StubSessionService() {
            super(null, null, null, null, null, null, null);
        }

        @Override
        public SessionDto getTodaySession() {
            todayCallCount++;
            if (todayException != null) {
                throw todayException;
            }
            return todayResult;
        }

        @Override
        public AnswerResponse answer(AnswerRequest req) {
            answerRequestArg = req;
            if (answerException != null) {
                throw answerException;
            }
            return answerResult;
        }
    }
}
