package dev.sidequestlab.backend.memoquiz.persistence.repository;

import dev.sidequestlab.backend.memoquiz.persistence.entity.MemoQuizSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoQuizSessionRepository extends JpaRepository<MemoQuizSessionEntity, Long> {
}
