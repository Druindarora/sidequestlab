package dev.sidequestlab.backend.memoquiz.persistence.repository;

import dev.sidequestlab.backend.memoquiz.persistence.entity.MemoQuizSettingsEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoQuizSettingsRepository extends JpaRepository<MemoQuizSettingsEntity, Long> {
    Optional<MemoQuizSettingsEntity> findTopByOrderByIdAsc();
}
