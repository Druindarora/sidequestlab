package dev.sidequestlab.backend.memoquiz.persistence.repository;

import dev.sidequestlab.backend.memoquiz.persistence.entity.MemoQuizSessionEntity;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoQuizSessionRepository extends JpaRepository<MemoQuizSessionEntity, Long> {

    boolean existsByStartedAtGreaterThanEqualAndStartedAtLessThan(Instant startedAt, Instant endAt);

    Optional<MemoQuizSessionEntity> findTopByOrderByStartedAtDescIdDesc();
}
