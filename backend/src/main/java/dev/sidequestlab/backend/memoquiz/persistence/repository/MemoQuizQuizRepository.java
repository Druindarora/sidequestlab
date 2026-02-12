package dev.sidequestlab.backend.memoquiz.persistence.repository;

import dev.sidequestlab.backend.memoquiz.persistence.entity.MemoQuizQuizEntity;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemoQuizQuizRepository extends JpaRepository<MemoQuizQuizEntity, Long> {

    Optional<MemoQuizQuizEntity> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from MemoQuizQuizEntity q where q.code = :code")
    Optional<MemoQuizQuizEntity> findByCodeForUpdate(@Param("code") String code);
}
