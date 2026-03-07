# Current State (Implemented)

Snapshot date: 2026-03-06.

## Implemented features

- Public frontend pages: home, profile (static content), portfolio, MemoQuiz screenshot demo.
- Session auth with backend cookies + CSRF:
  - login/logout/session restore
  - `mustChangePassword` enforcement before MemoQuiz access
  - change-password dialog flow
- MemoQuiz dashboard:
  - today index/boxes, due count (capped to 20), active card totals
  - latest session summary + box overview
- Card management:
  - create, edit, archive (soft delete by status), activate
  - filter/sort in UI table (client side)
- Quiz admin (default quiz only):
  - list current memberships
  - activate inactive cards into default quiz
- Daily session flow:
  - create/get today session
  - reveal answer, self-evaluate good/bad
  - answer persists review log and updates Leitner box progression

## Main user flows available

1. User opens site and can browse public content or MemoQuiz demo screenshots.
2. User logs in (CSRF bootstrap + credentials) and session is restored on app load.
3. If password change is required, MemoQuiz routes are blocked until password is changed.
4. Authenticated user opens MemoQuiz dashboard, navigates to cards/quiz/session pages.
5. User creates or updates cards, activates cards in default quiz, then runs the daily session.

## API summary (from current Swagger/OpenAPI surface)

Source used: generated client under `frontend/src/app/api/api/*.service.ts` (derived from backend Swagger `/v3/api-docs`).

- Health/Profile
  - `GET /api/health`
  - `GET /api/profile/me`
- MemoQuiz dashboard/session
  - `GET /api/memoquiz/dashboard/today`
  - `GET /api/memoquiz/session/today`
  - `POST /api/memoquiz/session/answer`
- MemoQuiz cards
  - `GET /api/memoquiz/cards` (`q`, `status`, `box`, `page`, `size`, `sort`)
  - `POST /api/memoquiz/cards`
  - `PUT /api/memoquiz/cards/{id}`
  - `POST /api/memoquiz/cards/{id}/activate`
- MemoQuiz quizzes
  - `GET /api/memoquiz/quiz`
  - `GET /api/memoquiz/quiz/overview`
  - `GET /api/memoquiz/quizzes/default/cards`
  - `POST /api/memoquiz/quizzes/default/cards/{cardId}`
  - `DELETE /api/memoquiz/quizzes/default/cards/{cardId}`

Related implemented API (used by frontend, but currently outside generated OpenAPI client):

- `GET /api/auth/csrf`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `POST /api/auth/change-password`

## Known limits / missing parts

- MemoQuiz data is global (no user ownership on sessions/review logs/quiz membership).
- Only the `default` quiz is operationally managed; no create/edit/delete quiz workflow.
- No hard-delete card endpoint; UI “delete” archives cards.
- Frontend card/quiz admin screens fetch up to 200 items and paginate client-side.
- Several frontend components include Blob-response parsing fallbacks around generated client calls.
- Profile page UI is static and does not consume `/api/profile/me`.
- Backend password policy is stricter in UI (min length 8) than in backend DTO validation.
