# Current State

Snapshot date: 2026-08-12.

## Implemented Features

- Astro public site at `www.imaginecodebuild.dev` with routes `/`, `/profil`, `/portfolio`, `/demo-memoquiz`, and `/confidentialite`.
- Public-site shared layout, header/footer navigation, per-page metadata, sitemap/favicons, and production-only GoatCounter analytics.
- Angular private app at `app.imaginecodebuild.dev` with root login entry and guarded `/memo-quiz/**` routes.
- Session auth with backend cookies and CSRF:
  - CSRF bootstrap
  - login/logout
  - session restore
  - `mustChangePassword` gate before MemoQuiz access
  - password-change dialog flow
- MemoQuiz dashboard:
  - current day index and scheduled boxes
  - due count and active card totals
  - latest session summary including duration when completed
  - box overview
- Card management:
  - create, edit, archive through status update, activate
  - client-side table filter/sort over a fetched page
  - JSON bulk card import with client and backend validation
- Quiz admin for the default quiz:
  - list current memberships
  - add cards to the default quiz
  - remove cards from the default quiz
- Daily session flow:
  - get/create today's session
  - block a second session after one has been started today
  - reveal answer and self-evaluate good/bad
  - persist review logs and update default-quiz Leitner boxes
  - complete session and store `endedAt` plus non-negative duration seconds

## Main User Flows

1. Visitor browses the Astro public site and can open the MemoQuiz demo or privacy page.
2. Visitor follows the private-app link to `app.imaginecodebuild.dev`.
3. User logs in through the Angular app; CSRF and session cookies are handled by the backend auth flow.
4. If password change is required, private MemoQuiz access is blocked until the password is changed.
5. Authenticated user manages cards, bulk imports cards, manages default quiz membership, views the dashboard, and runs the daily session.

## API Summary

Source: generated client under `frontend/src/app/api/api/*.service.ts`, derived from backend Swagger/OpenAPI.

- Root/web:
  - `GET /`
  - `GET /favicon.ico`
- Health/profile:
  - `GET /api/health`
  - `GET /api/profile/me`
- Auth:
  - `GET /api/auth/csrf`
  - `POST /api/auth/login`
  - `GET /api/auth/me`
  - `POST /api/auth/change-password`
- MemoQuiz dashboard/session:
  - `GET /api/memoquiz/dashboard/today`
  - `GET /api/memoquiz/session/today`
  - `POST /api/memoquiz/session/answer`
  - `POST /api/memoquiz/session/complete`
- MemoQuiz cards:
  - `GET /api/memoquiz/cards` with `q`, `status`, `box`, `page`, `size`, and `sort`
  - `POST /api/memoquiz/cards`
  - `POST /api/memoquiz/cards/bulk`
  - `PUT /api/memoquiz/cards/{id}`
  - `POST /api/memoquiz/cards/{id}/activate`
- MemoQuiz quizzes:
  - `GET /api/memoquiz/quiz`
  - `GET /api/memoquiz/quiz/overview`
  - `GET /api/memoquiz/quizzes/default/cards`
  - `POST /api/memoquiz/quizzes/default/cards/{cardId}`
  - `DELETE /api/memoquiz/quizzes/default/cards/{cardId}`

Auth endpoints are generated, but the app's auth flow currently uses `AuthService` with manual `HttpClient` calls. MemoQuiz pages use generated services.

## Known Limits

- MemoQuiz cards, sessions, review logs, and quiz membership are global; there is no per-user ownership model.
- Only the seeded `default` quiz is operationally managed; there is no create/edit/delete quiz workflow.
- No hard-delete card endpoint; UI deletion archives cards by setting status to `ARCHIVED`.
- Card and quiz admin screens fetch up to 200 cards and paginate/filter client-side.
- MemoQuiz page components still contain Blob-response parsing fallbacks around generated client responses.
- The public profile page is static and does not consume `/api/profile/me`.
- Backend password-change validation only requires non-blank passwords; stronger password requirements are not enforced by the backend DTO.
