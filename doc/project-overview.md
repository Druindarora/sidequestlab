# SideQuestLab Overview

## Product scope (current)

- Public portfolio site (home/profile/portfolio pages).
- MemoQuiz V1 module (authenticated), plus a public screenshot demo page.

## Architecture snapshot

- Frontend: Angular 21 standalone app (`frontend/`), Angular Material UI, generated OpenAPI TypeScript client (`frontend/src/app/api/**`).
- Backend: Spring Boot 3.5 / Java 21 (`backend/`), REST controllers, Spring Security (session cookie + CSRF), Springdoc Swagger.
- Data: PostgreSQL + Flyway migrations (`backend/src/main/resources/db/migration`).

## Frontend/backend structure

- Frontend routing:
  - Public: `/`, `/profil`, `/portfolio`, `/demo-memoquiz`
  - Protected: `/memo-quiz/**` (guarded by auth + forced password change rule)
- Backend API groups:
  - `api/auth/*` (session auth + password change)
  - `api/memoquiz/*` (dashboard, cards, quizzes, session)
  - `api/health`, `api/profile/me`

## Main integration points

- API base URL from `frontend/src/environments/*` (`/api` prefix expected).
- `backendAuthInterceptor` forces `withCredentials` and injects `X-XSRF-TOKEN` for mutating backend calls.
- Auth is consumed with manual `HttpClient` calls; MemoQuiz domain endpoints are consumed via generated OpenAPI services.
- OpenAPI generation command: `cd frontend && npm run generate:api` (requires backend `/v3/api-docs`).

## Technical constraints to keep in mind

- Guardrail: generated API folder is protected by `scripts/guardrails.sh`.
- Main quality gate is `./scripts/check.sh` (backend tests/package + frontend ci/lint/build/test).
- MemoQuiz schedule is fixed by `backend/src/main/resources/memoquiz/study-schedule-64.json` (64-day cycle).
- Non-test MemoQuiz controllers/services run under profile `!test`.
