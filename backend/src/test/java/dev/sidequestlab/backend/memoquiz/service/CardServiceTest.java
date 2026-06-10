package dev.sidequestlab.backend.memoquiz.service;

import dev.sidequestlab.backend.memoquiz.api.dto.BulkCreateCardItem;
import dev.sidequestlab.backend.memoquiz.api.dto.BulkCreateCardsRequest;
import dev.sidequestlab.backend.memoquiz.api.dto.CreateCardRequest;
import dev.sidequestlab.backend.memoquiz.api.dto.UpdateCardRequest;
import dev.sidequestlab.backend.memoquiz.api.enums.CardStatus;
import dev.sidequestlab.backend.memoquiz.persistence.entity.CardEntity;
import dev.sidequestlab.backend.memoquiz.persistence.entity.CardProgressEntity;
import dev.sidequestlab.backend.memoquiz.persistence.repository.CardRepository;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
class CardServiceTest {

    @Test
    void createDefaultsStatusAndBox() {
        RepositoryStub repository = new RepositoryStub();
        CardService cardService = new CardService(repository.repository());
        CreateCardRequest req = new CreateCardRequest("Front", "Back", null);

        repository.onSave(entity -> {
            entity.setId(10L);
            return entity;
        });

        var dto = cardService.createCard(req);
        CardEntity entity = repository.lastSavedEntity;
        assertThat(entity.getStatus()).isEqualTo(CardStatus.INACTIVE);
        assertThat(entity.getProgress()).isNotNull();
        assertThat(entity.getProgress().getBox()).isEqualTo(1);
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(dto.status()).isEqualTo(CardStatus.INACTIVE);
        assertThat(dto.box()).isEqualTo(1);
    }

    @Test
    void createUsesProvidedBoxWhenPresent() {
        RepositoryStub repository = new RepositoryStub();
        CardService cardService = new CardService(repository.repository());
        CreateCardRequest req = new CreateCardRequest("Front", "Back", 4);

        repository.onSave(entity -> {
            entity.setId(11L);
            return entity;
        });

        var dto = cardService.createCard(req);
        CardEntity entity = repository.lastSavedEntity;

        assertThat(entity.getProgress()).isNotNull();
        assertThat(entity.getProgress().getBox()).isEqualTo(4);
        assertThat(dto.box()).isEqualTo(4);
    }

    @Test
    void bulkCreateCardsCreatesInactiveCardsWithDefaultBox() {
        RepositoryStub repository = new RepositoryStub();
        CardService cardService = new CardService(repository.repository());
        BulkCreateCardsRequest req = new BulkCreateCardsRequest(List.of(
            new BulkCreateCardItem("Front 1", "Back 1"),
            new BulkCreateCardItem("Front 2", "Back 2")
        ));

        repository.onSaveAll(entities -> {
            long id = 100L;
            for (CardEntity entity : entities) {
                entity.setId(id++);
            }
            return entities;
        });

        var response = cardService.bulkCreateCards(req);

        assertThat(response.detectedCount()).isEqualTo(2);
        assertThat(response.savedCount()).isEqualTo(2);
        assertThat(repository.saveAllCalls).isEqualTo(1);
        assertThat(repository.lastSavedEntities).hasSize(2);
        assertThat(repository.lastSavedEntities)
            .allSatisfy(entity -> {
                assertThat(entity.getStatus()).isEqualTo(CardStatus.INACTIVE);
                assertThat(entity.getProgress()).isNotNull();
                assertThat(entity.getProgress().getBox()).isEqualTo(1);
                assertThat(entity.getCreatedAt()).isNotNull();
            });
    }

    @Test
    void bulkCreateCardsRejectsEmptyList() {
        RepositoryStub repository = new RepositoryStub();
        CardService cardService = new CardService(repository.repository());

        assertThatThrownBy(() -> cardService.bulkCreateCards(new BulkCreateCardsRequest(List.of())))
            .isInstanceOf(ResponseStatusException.class)
            .extracting("statusCode")
            .isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(repository.saveAllCalls).isZero();
    }

    @Test
    void bulkCreateCardsRejectsMoreThanOneHundredCards() {
        RepositoryStub repository = new RepositoryStub();
        CardService cardService = new CardService(repository.repository());
        List<BulkCreateCardItem> cards = Stream.generate(() -> new BulkCreateCardItem("Front", "Back"))
            .limit(101)
            .toList();

        assertThatThrownBy(() -> cardService.bulkCreateCards(new BulkCreateCardsRequest(cards)))
            .isInstanceOf(ResponseStatusException.class)
            .extracting("statusCode")
            .isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(repository.saveAllCalls).isZero();
    }

    @ParameterizedTest
    @MethodSource("invalidBulkCardCases")
    void bulkCreateCardsRejectsBlankFrontOrBack(BulkCreateCardItem invalidCard) {
        RepositoryStub repository = new RepositoryStub();
        CardService cardService = new CardService(repository.repository());

        assertThatThrownBy(() -> cardService.bulkCreateCards(new BulkCreateCardsRequest(List.of(invalidCard))))
            .isInstanceOf(ResponseStatusException.class)
            .extracting("statusCode")
            .isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(repository.saveAllCalls).isZero();
    }

    @Test
    void bulkCreateCardsCreatesNothingWhenAnyItemIsInvalid() {
        RepositoryStub repository = new RepositoryStub();
        CardService cardService = new CardService(repository.repository());
        BulkCreateCardsRequest req = new BulkCreateCardsRequest(List.of(
            new BulkCreateCardItem("Front", "Back"),
            new BulkCreateCardItem(" ", "Back 2")
        ));

        assertThatThrownBy(() -> cardService.bulkCreateCards(req))
            .isInstanceOf(ResponseStatusException.class)
            .extracting("statusCode")
            .isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(repository.saveAllCalls).isZero();
        assertThat(repository.saveCalls).isZero();
    }

    @Test
    void listCardsMapsEntityWithNullProgressToDefaultBox() {
        RepositoryStub repository = new RepositoryStub();
        CardService cardService = new CardService(repository.repository());
        CardEntity entity = new CardEntity();
        entity.setId(30L);
        entity.setFront("Front");
        entity.setBack("Back");
        entity.setStatus(CardStatus.ACTIVE);
        entity.setCreatedAt(Instant.parse("2026-01-01T10:00:00Z"));
        entity.setUpdatedAt(Instant.parse("2026-01-01T10:00:00Z"));
        entity.setProgress(null);

        repository.onFindAll((spec, pageable) -> new PageImpl<>(List.of(entity)));

        var cards = cardService.listCards("  front  ", CardStatus.ACTIVE, 2, 0, 20, "createdAt,desc");

        assertThat(cards).hasSize(1);
        assertThat(cards.getFirst().id()).isEqualTo(30L);
        assertThat(cards.getFirst().status()).isEqualTo(CardStatus.ACTIVE);
        assertThat(cards.getFirst().box()).isEqualTo(1);
        assertThat(repository.findAllCalls).isEqualTo(1);
        assertThat(repository.lastSpecification).isNotNull();
    }

    @ParameterizedTest
    @MethodSource("sortCases")
    void listCardsUsesExpectedSort(String sort, Sort.Direction expectedDirection, String expectedProperty) {
        RepositoryStub repository = new RepositoryStub();
        CardService cardService = new CardService(repository.repository());
        CardEntity entity = new CardEntity();
        entity.setId(40L);
        entity.setFront("Front");
        entity.setBack("Back");
        entity.setStatus(CardStatus.INACTIVE);
        entity.setCreatedAt(Instant.parse("2026-01-01T10:00:00Z"));
        entity.setUpdatedAt(Instant.parse("2026-01-01T10:00:00Z"));

        CardProgressEntity progress = new CardProgressEntity();
        progress.setBox(3);
        progress.setUpdatedAt(Instant.parse("2026-01-01T10:00:00Z"));
        entity.setProgress(progress);

        repository.onFindAll((spec, pageable) -> new PageImpl<>(List.of(entity)));

        cardService.listCards(null, null, null, 2, 15, sort);

        Pageable pageable = repository.lastPageable;
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(15);
        Sort.Order order = pageable.getSort().getOrderFor(expectedProperty);
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(expectedDirection);
    }

    @Test
    void updateDoesNotReplaceNullFields() {
        RepositoryStub repository = new RepositoryStub();
        CardService cardService = new CardService(repository.repository());
        CardEntity existing = new CardEntity();
        existing.setId(1L);
        existing.setFront("Old front");
        existing.setBack("Old back");
        existing.setStatus(CardStatus.INACTIVE);
        existing.setCreatedAt(Instant.now());
        existing.setUpdatedAt(Instant.now());

        CardProgressEntity progress = new CardProgressEntity();
        progress.setBox(3);
        progress.setUpdatedAt(Instant.now());
        existing.setProgress(progress);

        repository.onFindById(id -> Optional.of(existing));
        repository.onSave(entity -> entity);

        UpdateCardRequest req = new UpdateCardRequest(null, "New back", null, null);

        var dto = cardService.updateCard(1L, req);

        assertThat(existing.getFront()).isEqualTo("Old front");
        assertThat(existing.getBack()).isEqualTo("New back");
        assertThat(existing.getStatus()).isEqualTo(CardStatus.INACTIVE);
        assertThat(existing.getProgress().getBox()).isEqualTo(3);
        assertThat(dto.front()).isEqualTo("Old front");
        assertThat(dto.back()).isEqualTo("New back");
    }

    @Test
    void updateCreatesProgressWhenMissingAndBoxProvided() {
        RepositoryStub repository = new RepositoryStub();
        CardService cardService = new CardService(repository.repository());
        CardEntity existing = new CardEntity();
        existing.setId(3L);
        existing.setFront("Old front");
        existing.setBack("Old back");
        existing.setStatus(CardStatus.INACTIVE);
        existing.setCreatedAt(Instant.parse("2026-01-01T10:00:00Z"));
        existing.setUpdatedAt(Instant.parse("2026-01-01T10:00:00Z"));
        existing.setProgress(null);

        repository.onFindById(id -> Optional.of(existing));
        repository.onSave(entity -> entity);

        UpdateCardRequest req = new UpdateCardRequest("New front", "New back", CardStatus.ACTIVE, 5);

        var dto = cardService.updateCard(3L, req);

        assertThat(existing.getFront()).isEqualTo("New front");
        assertThat(existing.getBack()).isEqualTo("New back");
        assertThat(existing.getStatus()).isEqualTo(CardStatus.ACTIVE);
        assertThat(existing.getProgress()).isNotNull();
        assertThat(existing.getProgress().getBox()).isEqualTo(5);
        assertThat(existing.getProgress().getUpdatedAt()).isNotNull();
        assertThat(dto.box()).isEqualTo(5);
    }

    @Test
    void updateReusesExistingProgressWhenBoxProvided() {
        RepositoryStub repository = new RepositoryStub();
        CardService cardService = new CardService(repository.repository());
        CardEntity existing = new CardEntity();
        existing.setId(4L);
        existing.setFront("Front");
        existing.setBack("Back");
        existing.setStatus(CardStatus.INACTIVE);
        existing.setCreatedAt(Instant.parse("2026-01-01T10:00:00Z"));
        existing.setUpdatedAt(Instant.parse("2026-01-01T10:00:00Z"));

        CardProgressEntity progress = new CardProgressEntity();
        progress.setBox(2);
        progress.setUpdatedAt(Instant.parse("2026-01-01T10:00:00Z"));
        existing.setProgress(progress);

        repository.onFindById(id -> Optional.of(existing));
        repository.onSave(entity -> entity);

        cardService.updateCard(4L, new UpdateCardRequest(null, null, null, 6));

        assertThat(existing.getProgress()).isSameAs(progress);
        assertThat(existing.getProgress().getBox()).isEqualTo(6);
    }

    @Test
    void activateSetsStatusActive() {
        RepositoryStub repository = new RepositoryStub();
        CardService cardService = new CardService(repository.repository());
        CardEntity existing = new CardEntity();
        existing.setId(2L);
        existing.setFront("Front");
        existing.setBack("Back");
        existing.setStatus(CardStatus.INACTIVE);
        existing.setCreatedAt(Instant.now());
        existing.setUpdatedAt(Instant.now());

        CardProgressEntity progress = new CardProgressEntity();
        progress.setBox(2);
        progress.setUpdatedAt(Instant.now());
        existing.setProgress(progress);

        repository.onFindById(id -> Optional.of(existing));
        repository.onSave(entity -> entity);

        var dto = cardService.activateCard(2L);

        assertThat(existing.getStatus()).isEqualTo(CardStatus.ACTIVE);
        assertThat(dto.status()).isEqualTo(CardStatus.ACTIVE);
        assertThat(dto.box()).isEqualTo(2);
    }

    @Test
    void updateNotFoundThrows() {
        RepositoryStub repository = new RepositoryStub();
        CardService cardService = new CardService(repository.repository());
        repository.onFindById(id -> Optional.empty());

        UpdateCardRequest req = new UpdateCardRequest("Front", null, null, null);

        assertThatThrownBy(() -> cardService.updateCard(99L, req))
            .isInstanceOf(ResponseStatusException.class)
            .extracting("statusCode")
            .isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(repository.saveCalls).isZero();
    }

    @Test
    void activateNotFoundThrows() {
        RepositoryStub repository = new RepositoryStub();
        CardService cardService = new CardService(repository.repository());
        repository.onFindById(id -> Optional.empty());

        assertThatThrownBy(() -> cardService.activateCard(100L))
            .isInstanceOf(ResponseStatusException.class)
            .extracting("statusCode")
            .isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(repository.saveCalls).isZero();
    }

    private static Stream<Arguments> sortCases() {
        return Stream.of(
            Arguments.of(null, Sort.Direction.ASC, "id"),
            Arguments.of(" ", Sort.Direction.ASC, "id"),
            Arguments.of("createdAt,desc", Sort.Direction.DESC, "createdAt"),
            Arguments.of("createdAt,asc", Sort.Direction.ASC, "createdAt"),
            Arguments.of("id,desc", Sort.Direction.DESC, "id"),
            Arguments.of("id,asc", Sort.Direction.ASC, "id"),
            Arguments.of("unsupported", Sort.Direction.ASC, "id")
        );
    }

    private static Stream<Arguments> invalidBulkCardCases() {
        return Stream.of(
            Arguments.of(new BulkCreateCardItem("", "Back")),
            Arguments.of(new BulkCreateCardItem("   ", "Back")),
            Arguments.of(new BulkCreateCardItem("Front", "")),
            Arguments.of(new BulkCreateCardItem("Front", "   "))
        );
    }

    private static final class RepositoryStub implements InvocationHandler {
        private java.util.function.Function<CardEntity, CardEntity> saveBehavior = entity -> entity;
        private java.util.function.Function<List<CardEntity>, List<CardEntity>> saveAllBehavior = entities -> entities;
        private java.util.function.Function<Long, Optional<CardEntity>> findByIdBehavior = id -> Optional.empty();
        private java.util.function.BiFunction<Specification<CardEntity>, Pageable, Page<CardEntity>> findAllBehavior =
            (spec, pageable) -> Page.empty(pageable);

        private int saveCalls;
        private CardEntity lastSavedEntity;
        private int saveAllCalls;
        private List<CardEntity> lastSavedEntities = List.of();
        private int findAllCalls;
        private Specification<CardEntity> lastSpecification;
        private Pageable lastPageable;

        private CardRepository repository() {
            return (CardRepository) Proxy.newProxyInstance(
                CardRepository.class.getClassLoader(),
                new Class<?>[] {CardRepository.class},
                this
            );
        }

        private void onSave(java.util.function.Function<CardEntity, CardEntity> behavior) {
            this.saveBehavior = behavior;
        }

        private void onSaveAll(java.util.function.Function<List<CardEntity>, List<CardEntity>> behavior) {
            this.saveAllBehavior = behavior;
        }

        private void onFindById(java.util.function.Function<Long, Optional<CardEntity>> behavior) {
            this.findByIdBehavior = behavior;
        }

        private void onFindAll(java.util.function.BiFunction<Specification<CardEntity>, Pageable, Page<CardEntity>> behavior) {
            this.findAllBehavior = behavior;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();

            if ("save".equals(name) && args != null && args.length == 1 && args[0] instanceof CardEntity entity) {
                saveCalls++;
                lastSavedEntity = entity;
                return saveBehavior.apply(entity);
            }

            if ("saveAll".equals(name) && args != null && args.length == 1 && args[0] instanceof Iterable<?> entities) {
                saveAllCalls++;
                lastSavedEntities = new java.util.ArrayList<>();
                for (Object entity : entities) {
                    if (!(entity instanceof CardEntity cardEntity)) {
                        throw new UnsupportedOperationException("Unsupported saveAll entity: " + entity);
                    }
                    lastSavedEntities.add(cardEntity);
                }
                return saveAllBehavior.apply(lastSavedEntities);
            }

            if ("findById".equals(name) && args != null && args.length == 1 && args[0] instanceof Long id) {
                return findByIdBehavior.apply(id);
            }

            if ("findAll".equals(name) && args != null && args.length == 2 && args[1] instanceof Pageable pageable) {
                if (!(args[0] instanceof Specification<?> specification)) {
                    throw new UnsupportedOperationException("Unsupported findAll signature");
                }
                findAllCalls++;
                @SuppressWarnings("unchecked")
                Specification<CardEntity> typedSpecification = (Specification<CardEntity>) specification;
                lastSpecification = typedSpecification;
                lastPageable = pageable;
                return findAllBehavior.apply(typedSpecification, pageable);
            }

            if ("toString".equals(name)) {
                return "RepositoryStub";
            }

            if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            }

            if ("equals".equals(name) && args != null && args.length == 1) {
                return proxy == args[0];
            }

            throw new UnsupportedOperationException("Method not supported in RepositoryStub: " + method);
        }
    }
}
