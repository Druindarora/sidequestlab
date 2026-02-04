package dev.sidequestlab.backend.memoquiz.persistence.repository;

import dev.sidequestlab.backend.memoquiz.persistence.entity.MemoQuizReviewLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoQuizReviewLogRepository extends JpaRepository<MemoQuizReviewLogEntity, Long> {
}
