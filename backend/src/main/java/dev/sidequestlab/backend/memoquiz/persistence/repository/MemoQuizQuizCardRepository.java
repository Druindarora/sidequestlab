package dev.sidequestlab.backend.memoquiz.persistence.repository;

import dev.sidequestlab.backend.memoquiz.api.dto.SessionCardDto;
import dev.sidequestlab.backend.memoquiz.api.enums.CardStatus;
import dev.sidequestlab.backend.memoquiz.persistence.entity.MemoQuizQuizCardEntity;
import dev.sidequestlab.backend.memoquiz.persistence.entity.MemoQuizQuizCardId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemoQuizQuizCardRepository extends JpaRepository<MemoQuizQuizCardEntity, MemoQuizQuizCardId> {

    Optional<MemoQuizQuizCardEntity> findByQuizIdAndCardId(Long quizId, Long cardId);

    long countByQuizIdAndEnabledTrue(Long quizId);

    long countByEnabledTrue();

    @Query("""
        select new dev.sidequestlab.backend.memoquiz.api.dto.SessionCardDto(
            c.id,
            c.front,
            c.back,
            qc.box
        )
        from MemoQuizQuizCardEntity qc
        join qc.card c
        where qc.quizId = :quizId
          and qc.enabled = true
        order by c.id asc
        """)
    List<SessionCardDto> findEnabledSessionCardsByQuizId(@Param("quizId") Long quizId);

    @Query("""
                select new dev.sidequestlab.backend.memoquiz.persistence.projection.SessionCardProjection(
                    qc.cardId, c.front, c.back, qc.box
                )
                from MemoQuizQuizCardEntity qc
                join qc.card c
                where qc.quizId = :quizId
                    and qc.enabled = true
                    and qc.box in :boxes
                    and c.status = :status
                order by qc.cardId asc
                """)
        List<dev.sidequestlab.backend.memoquiz.persistence.projection.SessionCardProjection> findEnabledForSession(
        @Param("quizId") Long quizId,
        @Param("boxes") Collection<Integer> boxes,
        @Param("status") CardStatus status,
        Pageable pageable
    );
}
