# Next Slices (Immediate)

The public/private frontend split and deployment migration are complete:

- `www.imaginecodebuild.dev` serves the Astro public site.
- `app.imaginecodebuild.dev` serves the Angular private app.
- `api.imaginecodebuild.dev` serves the Spring Boot API.

## 1) Investigate auth/session refresh behavior, if still reproducible

Priority: High

- Reproduce and diagnose any remaining login/session restore or MemoQuiz refresh issue across the deployed app and API domains.
- Confirm cookie, CSRF, CORS, and Angular route restoration behavior before changing implementation.

Acceptance criteria:

- The issue is either reproduced with a documented cause or confirmed resolved.
- Login, logout, authenticated API calls, and MemoQuiz browser refresh work across production domains.

## 2) Frontend dependency and OpenAPI alignment

Priority: High

- Align OpenAPI generation config with current Angular major (`generate:api` still uses `ngVersion=17`).
- Update frontend dependencies to latest safe patch/minor within Angular 21 toolchain.
- Regenerate API client once after alignment and verify compile/runtime behavior.

Acceptance criteria:

- `npm ci`, `npm run lint`, `npm run build`, `npm test -- --watch=false`, and `./scripts/check.sh` are green.
- Generated client compiles without manual edits under `frontend/src/app/api/**`.
- No unexpected UI/API regressions on MemoQuiz main flows.

## 3) API client normalization in frontend

Priority: High

- Add a thin frontend API adapter layer to normalize generated client responses (JSON-first).
- Remove repeated Blob parsing logic from MemoQuiz pages.
- Centralize API error mapping for user-facing messages.

Acceptance criteria:

- No Blob parsing code remains in page components.
- Cards, quiz admin, dashboard, and session pages still load successfully.
- Existing specs pass and at least one spec covers adapter behavior.

## 4) Frontend auth/session test coverage boost

Priority: Medium

- Add focused unit tests for `AuthService`, auth guard, and password-change gating behavior.
- Cover CSRF bootstrap + login/logout state transitions.

Acceptance criteria:

- Tests validate: unauthenticated block, forced password change block, and successful unlock path.
- Frontend test suite stays green in `./scripts/check.sh`.

## Not now

- Repository housekeeping: rename `frontend` to `apps/private-app` and `backend` to `services/api`. This is a future focused migration, not immediate work.
- New product modules beyond MemoQuiz V1.
- Infra/workflow redesign outside direct quality or dependency improvements.
