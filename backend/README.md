# Backend (Spring Boot, Java 21)

## Run locally

The application reads configuration from environment variables (see repo root `.env.example`).

1. Create local env values in repo root:

   ```bash
   cp .env.example .env.local
   ```

2. Start PostgreSQL (from repo root):

   ```bash
   docker compose -f docker-compose.dev.yml up -d postgres
   ```

3. Start backend:

   ```bash
   cd backend && ./mvnw spring-boot:run
   ```

If you want `.env.local` auto-loaded, run from repo root with `./scripts/run-backend.sh`.

## Tests

```bash
cd backend
./mvnw -B test -Dspring.profiles.active=test
```

## Package

```bash
cd backend
./mvnw -B -DskipTests package
```

## Database notes

- PostgreSQL is the expected runtime database.
- Flyway migrations in `src/main/resources/db/migration` run at startup.
- Spring datasource/admin/cors settings are provided via environment variables from `.env.example`.
