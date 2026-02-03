package dev.sidequestlab.backend.memoquiz.persistence.repository;

import dev.sidequestlab.backend.memoquiz.persistence.entity.CardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CardRepository extends JpaRepository<CardEntity, Long>, JpaSpecificationExecutor<CardEntity> {
}
