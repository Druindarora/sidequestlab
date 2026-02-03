# Agent Rules (V0) — SideQuestLab

## Goal

Produce small, reviewable PRs and keep CI green.

## Scope rules (guardrails)

- Never work directly on `main` or `dev`. Work only on a `work/<topic>` branch.
- No broad refactors, no massive renames, and no architecture changes unless explicitly requested.
- Do not touch `infra/` or `.github/` unless explicitly requested.
- OpenAPI generated code: `frontend/src/app/api/**`
  - Never modify it manually.
  - If a change is needed, regenerate it (`npm run generate:api`) and review the diff.

## Definition of Done (DoD)

- `scripts/check.sh` must be green before proposing a PR.
- The PR must include a summary: goals, touched files, executed commands, and manual verification points.

## Backend (tests)

- CI tests run without a DB using the `test` profile (`-Dspring.profiles.active=test`).
- Do not skip Maven tests in CI.

## Frontend (standards)

- Follow the agreed Angular standards (inject(), @if/@for, etc.).
- Lint via ESLint (same as CI).

## Safety & Logging

### Logging policy (V1)

- Do not add "business logs" in services/controllers (e.g., "creating card...", "answer received...").
- Log only unexpected errors (5xx / unhandled exceptions) in the global exception handler.
- Avoid logging stack traces for expected client errors (4xx) unless explicitly requested.

### Destructive actions policy

- Never run destructive commands (e.g., `rm -rf`, recursive deletes) or perform mass deletions.
- Never delete or rename files/directories outside the explicitly requested scope.
- If deletion is truly necessary, explain why, list exactly what will be deleted, and wait for explicit confirmation before proceeding.
