# AGENTS — Backend (Spring Boot)

## Coding conventions

- Target: Java 21 / Spring Boot (repo), use current patterns.

## Tests / profiles

- CI tests run without DB using profile `test`.
- Do NOT skip Maven tests in CI.
- Keep DB-dependent controllers excluded from `test` if needed (e.g. `@Profile("!test")`).
- If you add or change business logic, add/update unit tests accordingly. New services must include unit tests.

## Flyway / DB

- All schema changes via Flyway migrations.
- Keep migrations small and targeted.

## Commands (reference)

- `cd backend && ./mvnw -B test -Dspring.profiles.active=test`
- `cd backend && ./mvnw -B -DskipTests package`
- Final gate remains: `./scripts/check.sh` (repo root)
