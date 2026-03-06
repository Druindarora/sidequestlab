# Next Slices (Immediate)

## 1) Frontend dependency alignment and refresh

Priority: High

- Align OpenAPI generation config with current Angular major (`generate:api` still uses `ngVersion=17`).
- Update frontend dependencies to latest safe patch/minor within Angular 21 toolchain.
- Regenerate API client once after alignment and verify compile/runtime behavior.

Acceptance criteria:

- `npm ci`, `npm run lint`, `npm run build`, `npm test -- --watch=false`, and `./scripts/check.sh` are green.
- Generated client compiles without manual edits under `frontend/src/app/api/**`.
- No unexpected UI/API regressions on MemoQuiz main flows.

## 2) API client normalization in frontend

Priority: High

- Add a thin frontend API adapter layer to normalize generated client responses (JSON-first).
- Remove repeated Blob parsing logic from MemoQuiz pages.
- Centralize API error mapping for user-facing messages.

Acceptance criteria:

- No Blob parsing code remains in page components.
- Cards, quiz admin, dashboard, and session pages still load successfully.
- Existing specs pass and at least one spec covers adapter behavior.

## 3) Dev quality gate tightening

Priority: Medium

- Add explicit format check to regular local/CI flow (Prettier check step).
- Ensure frontend lint/build/test commands are deterministic and non-interactive in CI/local.
- Document the exact gate commands in one short canonical section.

Acceptance criteria:

- Formatting violations fail fast in checks.
- `./scripts/check.sh` remains the single reliable project gate.
- No new flaky behavior in CI execution.

## 4) Frontend auth/session test coverage boost

Priority: Medium

- Add focused unit tests for `AuthService`, auth guard, and password-change gating behavior.
- Cover CSRF bootstrap + login/logout state transitions.

Acceptance criteria:

- Tests validate: unauthenticated block, forced password change block, and successful unlock path.
- Frontend test suite stays green in `./scripts/check.sh`.

## Not now

- New product modules beyond MemoQuiz V1.
- Infra/workflow redesign outside direct quality or dependency improvements.
