package dev.sidequestlab.backend.memoquiz.api.controller;

import dev.sidequestlab.backend.memoquiz.api.dto.BulkCreateCardItem;
import dev.sidequestlab.backend.memoquiz.api.dto.BulkCreateCardsRequest;
import dev.sidequestlab.backend.memoquiz.api.dto.BulkCreateCardsResponse;
import dev.sidequestlab.backend.memoquiz.api.dto.CardDto;
import dev.sidequestlab.backend.memoquiz.api.dto.CreateCardRequest;
import dev.sidequestlab.backend.memoquiz.api.dto.UpdateCardRequest;
import dev.sidequestlab.backend.memoquiz.api.enums.CardStatus;
import dev.sidequestlab.backend.memoquiz.service.CardService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CardControllerTest {

    private final StubCardService cardService = new StubCardService();
    private final CardController controller = new CardController(cardService);

    @Test
    void listCardsReturnsOkAndDelegatesToService() {
        String q = "java";
        CardStatus status = CardStatus.ACTIVE;
        Integer box = 2;
        int page = 1;
        int size = 10;
        String sort = "createdAt,desc";

        List<CardDto> expected = List.of(
            new CardDto(1L, "front", "back", CardStatus.ACTIVE, 2, Instant.parse("2025-01-01T00:00:00Z"))
        );
        cardService.listCardsResult = expected;

        ResponseEntity<List<CardDto>> response = controller.listCards(q, status, box, page, size, sort);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(expected);
        assertThat(cardService.listCardsCall).isEqualTo(new ListCardsCall(q, status, box, page, size, sort));
    }

    @Test
    void createCardReturnsOkAndDelegatesToService() {
        CreateCardRequest req = new CreateCardRequest("Question", "Answer", 3);
        CardDto expected = new CardDto(5L, "Question", "Answer", CardStatus.INACTIVE, 3, Instant.parse("2025-01-02T00:00:00Z"));
        cardService.createCardResult = expected;

        ResponseEntity<CardDto> response = controller.createCard(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        assertThat(cardService.createCardRequestArg).isEqualTo(req);
    }

    @Test
    void bulkCreateCardsReturnsOkAndDelegatesToService() {
        BulkCreateCardsRequest req = new BulkCreateCardsRequest(List.of(
            new BulkCreateCardItem("Question 1", "Answer 1"),
            new BulkCreateCardItem("Question 2", "Answer 2")
        ));
        BulkCreateCardsResponse expected = new BulkCreateCardsResponse(2, 2);
        cardService.bulkCreateCardsResult = expected;

        ResponseEntity<BulkCreateCardsResponse> response = controller.bulkCreateCards(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        assertThat(cardService.bulkCreateCardsRequestArg).isEqualTo(req);
    }

    @Test
    void updateCardReturnsOkAndDelegatesToService() {
        long id = 8L;
        UpdateCardRequest req = new UpdateCardRequest("Updated front", "Updated back", CardStatus.ARCHIVED, 4);
        CardDto expected = new CardDto(id, "Updated front", "Updated back", CardStatus.ARCHIVED, 4, Instant.parse("2025-01-03T00:00:00Z"));
        cardService.updateCardResult = expected;

        ResponseEntity<CardDto> response = controller.updateCard(id, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        assertThat(cardService.updateCardIdArg).isEqualTo(id);
        assertThat(cardService.updateCardRequestArg).isEqualTo(req);
    }

    @Test
    void activateCardReturnsOkAndDelegatesToService() {
        long id = 13L;
        CardDto expected = new CardDto(id, "f", "b", CardStatus.ACTIVE, 1, Instant.parse("2025-01-04T00:00:00Z"));
        cardService.activateCardResult = expected;

        ResponseEntity<CardDto> response = controller.activateCard(id);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        assertThat(cardService.activateCardIdArg).isEqualTo(id);
    }

    @Test
    void activateCardPropagatesServiceExceptionBecauseControllerDoesNotHandleIt() {
        long id = 21L;
        RuntimeException failure = new RuntimeException("service failure");
        cardService.activateCardException = failure;

        assertThatThrownBy(() -> controller.activateCard(id)).isSameAs(failure);
        assertThat(cardService.activateCardIdArg).isEqualTo(id);
    }

    private record ListCardsCall(String q, CardStatus status, Integer box, int page, int size, String sort) {}

    private static final class StubCardService extends CardService {
        private ListCardsCall listCardsCall;
        private List<CardDto> listCardsResult;

        private CreateCardRequest createCardRequestArg;
        private CardDto createCardResult;

        private BulkCreateCardsRequest bulkCreateCardsRequestArg;
        private BulkCreateCardsResponse bulkCreateCardsResult;

        private Long updateCardIdArg;
        private UpdateCardRequest updateCardRequestArg;
        private CardDto updateCardResult;

        private Long activateCardIdArg;
        private CardDto activateCardResult;
        private RuntimeException activateCardException;

        private StubCardService() {
            super(null);
        }

        @Override
        public List<CardDto> listCards(String q, CardStatus status, Integer box, int page, int size, String sort) {
            this.listCardsCall = new ListCardsCall(q, status, box, page, size, sort);
            return listCardsResult;
        }

        @Override
        public CardDto createCard(CreateCardRequest req) {
            this.createCardRequestArg = req;
            return createCardResult;
        }

        @Override
        public BulkCreateCardsResponse bulkCreateCards(BulkCreateCardsRequest req) {
            this.bulkCreateCardsRequestArg = req;
            return bulkCreateCardsResult;
        }

        @Override
        public CardDto updateCard(Long id, UpdateCardRequest req) {
            this.updateCardIdArg = id;
            this.updateCardRequestArg = req;
            return updateCardResult;
        }

        @Override
        public CardDto activateCard(Long id) {
            this.activateCardIdArg = id;
            if (activateCardException != null) {
                throw activateCardException;
            }
            return activateCardResult;
        }
    }
}
