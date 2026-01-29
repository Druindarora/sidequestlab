package dev.sidequestlab.backend.card;

import dev.sidequestlab.backend.api.dto.CreateCardRequest;
import dev.sidequestlab.backend.api.dto.UpdateCardRequest;
import dev.sidequestlab.backend.api.enums.CardStatus;
import java.time.Instant;
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
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private CardService cardService;

    @Test
    void createDefaultsStatusAndBox() {
        CreateCardRequest req = new CreateCardRequest("Front", "Back", null);

        when(cardRepository.save(any(CardEntity.class))).thenAnswer(invocation -> {
            CardEntity saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        var dto = cardService.createCard(req);

        ArgumentCaptor<CardEntity> captor = ArgumentCaptor.forClass(CardEntity.class);
        verify(cardRepository).save(captor.capture());

        CardEntity entity = captor.getValue();
        assertThat(entity.getStatus()).isEqualTo(CardStatus.INACTIVE);
        assertThat(entity.getProgress()).isNotNull();
        assertThat(entity.getProgress().getBox()).isEqualTo(1);
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(dto.status()).isEqualTo(CardStatus.INACTIVE);
        assertThat(dto.box()).isEqualTo(1);
    }

    @Test
    void updateDoesNotReplaceNullFields() {
        CardEntity existing = new CardEntity();
        existing.setId(1L);
        existing.setFront("Old front");
        existing.setBack("Old back");
        existing.setStatus(CardStatus.INACTIVE);
        existing.setCreatedAt(Instant.now());
        existing.setUpdatedAt(Instant.now());

        CardProgressEntity progress = new CardProgressEntity();
        progress.setBox(3);
        progress.setUpdatedAt(Instant.now());
        existing.setProgress(progress);

        when(cardRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(cardRepository.save(any(CardEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateCardRequest req = new UpdateCardRequest(null, "New back", null, null);

        var dto = cardService.updateCard(1L, req);

        assertThat(existing.getFront()).isEqualTo("Old front");
        assertThat(existing.getBack()).isEqualTo("New back");
        assertThat(existing.getStatus()).isEqualTo(CardStatus.INACTIVE);
        assertThat(existing.getProgress().getBox()).isEqualTo(3);
        assertThat(dto.front()).isEqualTo("Old front");
        assertThat(dto.back()).isEqualTo("New back");
    }

    @Test
    void activateSetsStatusActive() {
        CardEntity existing = new CardEntity();
        existing.setId(2L);
        existing.setFront("Front");
        existing.setBack("Back");
        existing.setStatus(CardStatus.INACTIVE);
        existing.setCreatedAt(Instant.now());
        existing.setUpdatedAt(Instant.now());

        CardProgressEntity progress = new CardProgressEntity();
        progress.setBox(2);
        progress.setUpdatedAt(Instant.now());
        existing.setProgress(progress);

        when(cardRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(cardRepository.save(any(CardEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var dto = cardService.activateCard(2L);

        assertThat(existing.getStatus()).isEqualTo(CardStatus.ACTIVE);
        assertThat(dto.status()).isEqualTo(CardStatus.ACTIVE);
        assertThat(dto.box()).isEqualTo(2);
    }

    @Test
    void updateNotFoundThrows() {
        when(cardRepository.findById(99L)).thenReturn(Optional.empty());

        UpdateCardRequest req = new UpdateCardRequest("Front", null, null, null);

        assertThatThrownBy(() -> cardService.updateCard(99L, req))
            .isInstanceOf(ResponseStatusException.class)
            .extracting("statusCode")
            .isEqualTo(HttpStatus.NOT_FOUND);

        verify(cardRepository, never()).save(any(CardEntity.class));
    }

    @Test
    void activateNotFoundThrows() {
        when(cardRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.activateCard(100L))
            .isInstanceOf(ResponseStatusException.class)
            .extracting("statusCode")
            .isEqualTo(HttpStatus.NOT_FOUND);

        verify(cardRepository, never()).save(any(CardEntity.class));
    }
}
