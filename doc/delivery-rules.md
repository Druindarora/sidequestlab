# Delivery Rules (Prompting Pack)

Source of truth: `AGENTS.md` at repo root (plus `frontend/AGENTS.md` or `backend/AGENTS.md` when touching those folders).

## Non-negotiables

- Branch creation, commit, push, and PR creation may be handled by an external orchestrator.
- Do not create branches, commit, push, or open PRs unless explicitly requested by the user prompt.
- If publishing is explicitly requested, work from `dev` on a `work/<topic>` branch and never commit on `main` or `dev`.
- Keep diffs small and focused; no refactor outside explicit scope.
- Do not touch `infra/` or `.github/` unless explicitly requested.
- Never edit generated client code under `frontend/src/app/api/**` manually.
- No destructive operations (`rm -rf`, mass deletions, broad renames).
- If deletion is required, list exact files and wait for explicit confirmation.

## Required loop

1. Plan (max 5 bullets): scope, files, checks.
2. Implement minimal change set.
3. Run `./scripts/check.sh`.
4. If failing, fix once and rerun.

## Done criteria

- `./scripts/check.sh` is green.
- No forbidden paths/patterns changed.
- If publishing was explicitly requested, PR is small and reviewable.
- Final report includes: check result, modified files, commands run, manual verification, and, only when publishing was explicitly requested, commit hash and PR link.
