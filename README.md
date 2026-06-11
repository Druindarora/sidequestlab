# SideQuestLab

Monorepo for the Astro public website, Angular private MemoQuiz app, and Spring Boot API.

## Tech stack

- Public website: Astro (`apps/public-site`)
- Private app: Angular 21 + Angular Material (`frontend`)
- API: Spring Boot (Java 21) (`backend`)
- Database: PostgreSQL + Flyway migrations
- Local infra: Docker Compose (`docker-compose.dev.yml`)

## Repository structure

```text
.
├── apps/
│   └── public-site/        # Astro public website
├── frontend/               # Angular private app
├── backend/                # Spring Boot API
├── docker-compose.dev.yml  # Local Postgres + optional full stack services
├── .env.example            # Environment variables template
└── scripts/                # Dev/check helper scripts
```

## Quickstart (local development)

1. Create local environment values:

   ```bash
   cp .env.example .env.local
   ```

2. Start the database:

   ```bash
   docker compose -f docker-compose.dev.yml up -d
   ```

3. Start the API:

   ```bash
   cd backend && ./mvnw spring-boot:run
   ```

   The backend expects datasource/auth/cors env vars from `.env.example`. If you use `.env.local`, you can run from repo root with `./scripts/run-backend.sh` (it sources `.env.local` automatically).

4. Start the private Angular app:

   ```bash
   cd frontend && npm ci && npm start
   ```

5. Start the public Astro site:

   ```bash
   cd apps/public-site && npm ci && npm run dev
   ```

- Public site: `http://localhost:4321`
- Private app: `http://localhost:4200`
- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui`

## OpenAPI client generation

When backend is running on `http://localhost:8080`:

```bash
cd frontend && npm run generate:api
```

Do not edit generated files in `frontend/src/app/api/**` by hand.

## CI and audits

- `.github/workflows/ci.yml`: on push and pull request, runs backend test/package and frontend install/lint/build/test.
- `.github/workflows/nightly-audit.yml`: weekly `npm audit` report + backend Maven dependency tree artifact.
- `.github/dependabot.yml`: weekly npm (`/frontend`) and Maven (`/backend`) update PRs.

## Local checks

Primary repo gate:

```bash
./scripts/check.sh
```

This validates the Spring Boot API, Angular private app, and Astro public site.

Useful direct commands:

```bash
cd backend && ./mvnw -B test -Dspring.profiles.active=test
cd backend && ./mvnw -B -DskipTests package
cd frontend && npm ci
cd frontend && npm run lint
cd frontend && npm run build
cd frontend && npm test -- --watch=false
cd apps/public-site && npm ci
cd apps/public-site && npm run build
```
