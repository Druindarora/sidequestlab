# SideQuestLab Overview

## Product Scope

- Public Imagine Code & Build site with profile, portfolio, MemoQuiz demo, and privacy pages.
- Private MemoQuiz V1 application with session authentication, card management, quiz membership, and daily review sessions.

## Architecture

- Public website: Astro (`apps/public-site/`), deployed at `https://www.imaginecodebuild.dev`.
- Private app: Angular 21 standalone app (`frontend/`), Angular Material UI, generated OpenAPI TypeScript client (`frontend/src/app/api/**`), deployed at `https://app.imaginecodebuild.dev`.
- API: Spring Boot / Java 21 (`backend/`), REST controllers, Spring Security session auth with CSRF, Springdoc Swagger/OpenAPI, deployed at `https://api.imaginecodebuild.dev`.
- Data: PostgreSQL with Flyway migrations under `backend/src/main/resources/db/migration`.

## Route Ownership

Astro owns public routes:

- `/`
- `/profil`
- `/portfolio`
- `/demo-memoquiz`
- `/confidentialite`

Angular owns the private app entry at `/` on the app domain and protected `/memo-quiz/**` routes. MemoQuiz routes require authentication and are blocked while `mustChangePassword` is true.

Backend API groups:

- `/api/auth/*`
- `/api/memoquiz/*`
- `/api/health`
- `/api/profile/me`
- `/favicon.ico` and `/`

## Public Site Responsibilities

The Astro site is intentionally small. Pages use shared layouts, header/footer navigation from `src/navigation.ts`, common metadata components, and site defaults from `src/config/site.ts`. Public metadata responsibilities include per-page titles/descriptions, canonical URLs, robots tags, Open Graph basics, Twitter card type, sitemap link, and favicons.

GoatCounter analytics is loaded only in production on `www.imaginecodebuild.dev`.

## MemoQuiz Capabilities

- Dashboard with current schedule day, boxes due today/tomorrow, active card totals, box overview, and latest session summary.
- Card management with create, edit, archive, activate, filtering/sorting, and JSON bulk import of up to 100 cards.
- Default quiz management with current memberships and add/remove operations for the default quiz.
- Daily session flow with get/create today's session, answer reveal, good/bad self-evaluation, Leitner box updates, review logs, and explicit session completion with duration.

MemoQuiz schedule data is loaded from `backend/src/main/resources/memoquiz/study-schedule-64.json`.

## Integration Points

- API base URL comes from `frontend/src/environments/*`; the frontend expects the `/api` prefix.
- Astro links to the private app through `PUBLIC_APP_URL`.
- `backendAuthInterceptor` sends credentials and adds `X-XSRF-TOKEN` for mutating backend requests when a token is available.
- Auth controller endpoints are present in the generated OpenAPI client except logout. `POST /api/auth/logout` is provided by Spring Security configuration, and the Angular app currently uses the manual `AuthService`/`HttpClient` flow for all auth operations.
- MemoQuiz domain endpoints are consumed through generated OpenAPI services.
- OpenAPI generation command: `cd frontend && npm run generate:api` with the backend serving `/v3/api-docs`.

## Quality Gates

- Main repository validation: `./scripts/check.sh`.
- Generated API guardrail: do not edit `frontend/src/app/api/**` by hand.
- Public-site slices must also follow `apps/public-site/AGENTS.md`.
- Backend and frontend implementation slices must follow their local `AGENTS.md` files.
