package dev.sidequestlab.backend.memoquiz.service;

import dev.sidequestlab.backend.memoquiz.api.dto.QuizDto;
import dev.sidequestlab.backend.memoquiz.api.dto.QuizOverviewDto;
import dev.sidequestlab.backend.memoquiz.api.dto.SessionCardDto;
import dev.sidequestlab.backend.memoquiz.persistence.entity.CardEntity;
import dev.sidequestlab.backend.memoquiz.persistence.entity.MemoQuizQuizCardEntity;
import dev.sidequestlab.backend.memoquiz.persistence.entity.MemoQuizQuizEntity;
import dev.sidequestlab.backend.memoquiz.persistence.repository.CardRepository;
import dev.sidequestlab.backend.memoquiz.persistence.repository.MemoQuizQuizCardRepository;
import dev.sidequestlab.backend.memoquiz.persistence.repository.MemoQuizQuizRepository;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Profile("!test")
public class QuizService {

    private static final String DEFAULT_QUIZ_CODE = "default";

    private final MemoQuizQuizRepository quizRepository;
    private final MemoQuizQuizCardRepository quizCardRepository;
    private final CardRepository cardRepository;

    public QuizService(
        MemoQuizQuizRepository quizRepository,
        MemoQuizQuizCardRepository quizCardRepository,
        CardRepository cardRepository
    ) {
        this.quizRepository = quizRepository;
        this.quizCardRepository = quizCardRepository;
        this.cardRepository = cardRepository;
    }

    public Long getDefaultQuizId() {
        return getDefaultQuiz().getId();
    }

    public List<QuizDto> listQuizzes() {
        List<MemoQuizQuizEntity> quizzes = quizRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        return quizzes.stream()
            .map(quiz -> new QuizDto(
                quiz.getId(),
                quiz.getTitle(),
                (int) quizCardRepository.countByQuizIdAndEnabledTrue(quiz.getId())
            ))
            .toList();
    }

    public QuizOverviewDto overview() {
        int totalQuizzes = Math.toIntExact(quizRepository.count());
        int totalCards = Math.toIntExact(quizCardRepository.countByEnabledTrue());
        return new QuizOverviewDto(totalQuizzes, totalCards);
    }

    public List<SessionCardDto> listDefaultQuizCards() {
        Long quizId = getDefaultQuizId();
        return quizCardRepository.findEnabledSessionCardsByQuizId(quizId);
    }

    @Transactional
    public void addCardToDefaultQuiz(Long cardId) {
        MemoQuizQuizEntity quiz = getDefaultQuiz();
        CardEntity card = cardRepository.findById(cardId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"));

        MemoQuizQuizCardEntity membership = quizCardRepository.findByQuizIdAndCardId(quiz.getId(), cardId)
            .orElse(null);

        if (membership == null) {
            MemoQuizQuizCardEntity created = new MemoQuizQuizCardEntity();
            created.setQuizId(quiz.getId());
            created.setCardId(card.getId());
            created.setEnabled(true);
            created.setBox(1);
            quizCardRepository.save(created);
            return;
        }

        if (!membership.isEnabled()) {
            membership.setEnabled(true);
        }
        quizCardRepository.save(membership);
    }

    @Transactional
    public void removeCardFromDefaultQuiz(Long cardId) {
        MemoQuizQuizEntity quiz = getDefaultQuiz();
        MemoQuizQuizCardEntity membership = quizCardRepository.findByQuizIdAndCardId(quiz.getId(), cardId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz membership not found"));

        if (membership.isEnabled()) {
            membership.setEnabled(false);
            quizCardRepository.save(membership);
        }
    }

    private MemoQuizQuizEntity getDefaultQuiz() {
        return quizRepository.findByCode(DEFAULT_QUIZ_CODE)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Default quiz not found"));
    }
}
