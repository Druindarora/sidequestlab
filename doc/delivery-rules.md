# Delivery Rules (Prompting Pack)

Source of truth: `AGENTS.md` at repo root (plus `frontend/AGENTS.md` or `backend/AGENTS.md` when touching those folders).

## Non-negotiables

- Work from `dev` on a `work/<topic>` branch. Never commit on `main` or `dev`.
- Keep diffs small and focused; no refactor outside explicit scope.
- Do not touch `infra/` or `.github/` unless explicitly requested.
- Never edit generated client code under `frontend/src/app/api/**` manually.
- No destructive operations (`rm -rf`, mass deletions, broad renames).
- If deletion is required, list exact files and wait for explicit confirmation.

## Required loop

1. Plan (max 5 bullets): scope, files, checks.
2. Implement minimal change set.
3. Run `./scripts/check.sh`.
4. If failing, fix and rerun until green.
5. Commit, push branch, open PR to `dev`.

## Done criteria

- `./scripts/check.sh` is green.
- No forbidden paths/patterns changed.
- PR is small and reviewable.
- Final report includes: check result, modified files, commands run, manual verification, commit hash, PR link.
