package dev.sidequestlab.backend.memoquiz.persistence.repository;

import dev.sidequestlab.backend.memoquiz.persistence.entity.MemoQuizQuizEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoQuizQuizRepository extends JpaRepository<MemoQuizQuizEntity, Long> {

    Optional<MemoQuizQuizEntity> findByCode(String code);
}
