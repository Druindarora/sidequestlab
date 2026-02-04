package dev.sidequestlab.backend.memoquiz.service;

import dev.sidequestlab.backend.memoquiz.persistence.entity.CardEntity;
import dev.sidequestlab.backend.memoquiz.persistence.entity.MemoQuizQuizCardEntity;
import dev.sidequestlab.backend.memoquiz.persistence.entity.MemoQuizQuizEntity;
import dev.sidequestlab.backend.memoquiz.persistence.repository.CardRepository;
import dev.sidequestlab.backend.memoquiz.persistence.repository.MemoQuizQuizCardRepository;
import dev.sidequestlab.backend.memoquiz.persistence.repository.MemoQuizQuizRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    private MemoQuizQuizRepository quizRepository;

    @Mock
    private MemoQuizQuizCardRepository quizCardRepository;

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private QuizService quizService;

    @Test
    void addCardToDefaultQuizCreatesMembership() {
        MemoQuizQuizEntity quiz = new MemoQuizQuizEntity();
        quiz.setId(1L);
        quiz.setCode("default");
        quiz.setTitle("Default Quiz");

        CardEntity card = new CardEntity();
        card.setId(5L);

        when(quizRepository.findByCode("default")).thenReturn(Optional.of(quiz));
        when(cardRepository.findById(5L)).thenReturn(Optional.of(card));
        when(quizCardRepository.findByQuizIdAndCardId(1L, 5L)).thenReturn(Optional.empty());
        when(quizCardRepository.save(any(MemoQuizQuizCardEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        quizService.addCardToDefaultQuiz(5L);

        ArgumentCaptor<MemoQuizQuizCardEntity> membershipCaptor = ArgumentCaptor.forClass(MemoQuizQuizCardEntity.class);
        verify(quizCardRepository).save(membershipCaptor.capture());
        MemoQuizQuizCardEntity saved = membershipCaptor.getValue();
        assertThat(saved.getQuizId()).isEqualTo(1L);
        assertThat(saved.getCardId()).isEqualTo(5L);
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.getBox()).isEqualTo(1);
    }

    @Test
    void addCardToDefaultQuizReenablesKeepsBox() {
        MemoQuizQuizEntity quiz = new MemoQuizQuizEntity();
        quiz.setId(1L);

        CardEntity card = new CardEntity();
        card.setId(6L);

        MemoQuizQuizCardEntity membership = new MemoQuizQuizCardEntity();
        membership.setQuizId(1L);
        membership.setCardId(6L);
        membership.setEnabled(false);
        membership.setBox(5);

        when(quizRepository.findByCode("default")).thenReturn(Optional.of(quiz));
        when(cardRepository.findById(6L)).thenReturn(Optional.of(card));
        when(quizCardRepository.findByQuizIdAndCardId(1L, 6L)).thenReturn(Optional.of(membership));
        when(quizCardRepository.save(any(MemoQuizQuizCardEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        quizService.addCardToDefaultQuiz(6L);

        ArgumentCaptor<MemoQuizQuizCardEntity> membershipCaptor = ArgumentCaptor.forClass(MemoQuizQuizCardEntity.class);
        verify(quizCardRepository).save(membershipCaptor.capture());
        MemoQuizQuizCardEntity saved = membershipCaptor.getValue();
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.getBox()).isEqualTo(5);
    }

    @Test
    void addCardToDefaultQuizMissingCardThrows() {
        MemoQuizQuizEntity quiz = new MemoQuizQuizEntity();
        quiz.setId(1L);

        when(quizRepository.findByCode("default")).thenReturn(Optional.of(quiz));
        when(cardRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quizService.addCardToDefaultQuiz(7L))
            .isInstanceOf(ResponseStatusException.class)
            .extracting("statusCode")
            .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void removeCardFromDefaultQuizDisablesMembership() {
        MemoQuizQuizEntity quiz = new MemoQuizQuizEntity();
        quiz.setId(1L);

        MemoQuizQuizCardEntity membership = new MemoQuizQuizCardEntity();
        membership.setQuizId(1L);
        membership.setCardId(8L);
        membership.setEnabled(true);
        membership.setBox(2);

        when(quizRepository.findByCode("default")).thenReturn(Optional.of(quiz));
        when(quizCardRepository.findByQuizIdAndCardId(1L, 8L)).thenReturn(Optional.of(membership));
        when(quizCardRepository.save(any(MemoQuizQuizCardEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        quizService.removeCardFromDefaultQuiz(8L);

        ArgumentCaptor<MemoQuizQuizCardEntity> membershipCaptor = ArgumentCaptor.forClass(MemoQuizQuizCardEntity.class);
        verify(quizCardRepository).save(membershipCaptor.capture());
        MemoQuizQuizCardEntity saved = membershipCaptor.getValue();
        assertThat(saved.isEnabled()).isFalse();
        assertThat(saved.getBox()).isEqualTo(2);
    }

    @Test
    void removeCardFromDefaultQuizMissingMembershipThrows() {
        MemoQuizQuizEntity quiz = new MemoQuizQuizEntity();
        quiz.setId(1L);

        when(quizRepository.findByCode("default")).thenReturn(Optional.of(quiz));
        when(quizCardRepository.findByQuizIdAndCardId(1L, 9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quizService.removeCardFromDefaultQuiz(9L))
            .isInstanceOf(ResponseStatusException.class)
            .extracting("statusCode")
            .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
