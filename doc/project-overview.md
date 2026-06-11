# SideQuestLab Overview

## Product scope (current)

- Public portfolio site and MemoQuiz screenshot demo.
- Private MemoQuiz V1 application with session authentication.

## Architecture snapshot

- Public website: Astro (`apps/public-site/`), deployed at `https://www.imaginecodebuild.dev`.
- Private app: Angular 21 standalone app (`frontend/`), Angular Material UI, generated OpenAPI TypeScript client (`frontend/src/app/api/**`), deployed at `https://app.imaginecodebuild.dev`.
- API: Spring Boot 3.5 / Java 21 (`backend/`), REST controllers, Spring Security (session cookie + CSRF), Springdoc Swagger, deployed at `https://api.imaginecodebuild.dev`.
- Data: PostgreSQL + Flyway migrations (`backend/src/main/resources/db/migration`).

## Application ownership and routes

- Astro owns the public routes: `/`, `/profil`, `/portfolio`, `/demo-memoquiz`.
- Angular owns the private login entry at `/` and protected `/memo-quiz/**` routes. MemoQuiz routes are guarded by authentication and the forced password change rule.
- Legacy public Angular routes are not active.
- Backend API groups:
  - `api/auth/*` (session auth + password change)
  - `api/memoquiz/*` (dashboard, cards, quizzes, session)
  - `api/health`, `api/profile/me`

## Main integration points

- API base URL from `frontend/src/environments/*` (`/api` prefix expected).
- The Astro public site links users to the private app through `PUBLIC_APP_URL`.
- `backendAuthInterceptor` forces `withCredentials` and injects `X-XSRF-TOKEN` for mutating backend calls.
- Auth is consumed with manual `HttpClient` calls; MemoQuiz domain endpoints are consumed via generated OpenAPI services.
- OpenAPI generation command: `cd frontend && npm run generate:api` (requires backend `/v3/api-docs`).

## Technical constraints to keep in mind

- Guardrail: generated API folder is protected by `scripts/guardrails.sh`.
- Main quality gate is `./scripts/check.sh` (backend, Angular private app, and Astro public site).
- MemoQuiz schedule is fixed by `backend/src/main/resources/memoquiz/study-schedule-64.json` (64-day cycle).
- Non-test MemoQuiz controllers/services run under profile `!test`.
