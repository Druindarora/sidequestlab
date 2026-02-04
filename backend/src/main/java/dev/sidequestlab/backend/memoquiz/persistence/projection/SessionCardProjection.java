package dev.sidequestlab.backend.memoquiz.persistence.projection;

public record SessionCardProjection(Long cardId, String front, String back, Integer box) {

}
