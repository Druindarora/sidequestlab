# Next Slices

Immediate pending work only.

## 1. API Response and Client Normalization

Priority: High

MemoQuiz page components still contain Blob-response parsing fallbacks for generated-client calls.

Acceptance criteria:

- Add a thin frontend API adapter or normalization layer for generated-client responses used by MemoQuiz.
- Remove Blob parsing from MemoQuiz page components.
- Cards, quiz admin, dashboard, and session pages still load successfully.
- Existing specs pass, with focused coverage for the normalization behavior.

## 2. Focused AuthService Coverage

Priority: Medium

There is surrounding coverage for login UI, private entry guard, headers, and password-change prompting. `AuthService` itself still lacks focused unit coverage for its state transitions and HTTP sequencing.

Acceptance criteria:

- Add `AuthService` specs for CSRF bootstrap, login, logout, session restore success/failure, password change, and prompt state.
- Keep existing guard/header/login specs green.
- `./scripts/check.sh` remains green.

## Not Now

- Repository housekeeping such as renaming `frontend` to `apps/private-app` or `backend` to `services/api`.
- New product modules beyond MemoQuiz V1.
- Infra/workflow redesign outside direct quality or dependency work.
