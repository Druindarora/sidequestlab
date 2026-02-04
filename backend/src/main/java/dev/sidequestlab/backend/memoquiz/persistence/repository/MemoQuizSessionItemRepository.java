package dev.sidequestlab.backend.memoquiz.persistence.repository;

import dev.sidequestlab.backend.memoquiz.persistence.entity.MemoQuizSessionItemEntity;
import dev.sidequestlab.backend.memoquiz.persistence.entity.MemoQuizSessionItemId;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoQuizSessionItemRepository extends JpaRepository<MemoQuizSessionItemEntity, MemoQuizSessionItemId> {
    Optional<MemoQuizSessionItemEntity> findBySessionIdAndCardId(Long sessionId, Long cardId);
}
