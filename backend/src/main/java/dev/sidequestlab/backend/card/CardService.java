package dev.sidequestlab.backend.card;

import dev.sidequestlab.backend.api.dto.CardDto;
import dev.sidequestlab.backend.api.dto.CreateCardRequest;
import dev.sidequestlab.backend.api.dto.UpdateCardRequest;
import dev.sidequestlab.backend.api.enums.CardStatus;
import jakarta.persistence.criteria.JoinType;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Profile("!test")
public class CardService {

    private final CardRepository cardRepository;

    public CardService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    public List<CardDto> listCards(String q, CardStatus status, Integer box, int page, int size, String sort) {
        Specification<CardEntity> spec = (root, query, cb) -> cb.conjunction();

        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("front")), like),
                cb.like(cb.lower(root.get("back")), like)
            ));
        }

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        if (box != null) {
            spec = spec.and((root, query, cb) -> cb.equal(
                root.join("progress", JoinType.INNER).get("box"),
                box
            ));
        }

        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        return cardRepository.findAll(spec, pageable).map(this::toDto).toList();
    }

    public CardDto createCard(CreateCardRequest req) {
        Instant now = Instant.now();
        int box = req.box() == null ? 1 : req.box();

        CardEntity entity = new CardEntity();
        entity.setFront(req.front());
        entity.setBack(req.back());
        entity.setStatus(CardStatus.INACTIVE);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        CardProgressEntity progress = new CardProgressEntity();
        progress.setBox(box);
        progress.setUpdatedAt(now);
        entity.setProgress(progress);

        CardEntity saved = cardRepository.save(entity);
        return toDto(saved);
    }

    public CardDto updateCard(Long id, UpdateCardRequest req) {
        CardEntity entity = getCardOrThrow(id);

        if (req.front() != null) {
            entity.setFront(req.front());
        }
        if (req.back() != null) {
            entity.setBack(req.back());
        }
        if (req.status() != null) {
            entity.setStatus(req.status());
        }
        if (req.box() != null) {
            CardProgressEntity progress = entity.getProgress();
            if (progress == null) {
                progress = new CardProgressEntity();
                entity.setProgress(progress);
            }
            progress.setBox(req.box());
            progress.setUpdatedAt(Instant.now());
        }

        entity.setUpdatedAt(Instant.now());

        CardEntity saved = cardRepository.save(entity);
        return toDto(saved);
    }

    public CardDto activateCard(Long id) {
        CardEntity entity = getCardOrThrow(id);
        entity.setStatus(CardStatus.ACTIVE);
        entity.setUpdatedAt(Instant.now());
        CardEntity saved = cardRepository.save(entity);
        return toDto(saved);
    }

    private CardEntity getCardOrThrow(Long id) {
        return cardRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"));
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.ASC, "id");
        }
        return switch (sort.trim()) {
            case "createdAt,desc" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "createdAt,asc" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "id,desc" -> Sort.by(Sort.Direction.DESC, "id");
            case "id,asc" -> Sort.by(Sort.Direction.ASC, "id");
            default -> Sort.by(Sort.Direction.ASC, "id");
        };
    }

    private CardDto toDto(CardEntity entity) {
        int box = 1;
        CardProgressEntity progress = entity.getProgress();
        if (progress != null) {
            box = progress.getBox();
        }
        return new CardDto(
            entity.getId(),
            entity.getFront(),
            entity.getBack(),
            entity.getStatus(),
            box,
            entity.getCreatedAt()
        );
    }
}
