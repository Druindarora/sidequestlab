# AGENTS — SideQuestLab (Monorepo)

## Scope & intent

Deliver small, reviewable changes with CI green. Prefer minimal diffs, no refactors outside scope.

## Repo layout

- Frontend rules: `frontend/AGENTS.md`
- Backend rules: `backend/AGENTS.md`
  If you modify files under `frontend/`, you must follow frontend rules. Same for `backend/`.

## Branching & PR (mandatory)

- Never commit on `main` or `dev`.
- Start from `dev` and create `work/<topic>` branch.
- Open a PR targeting `dev`.

## Work loop (mandatory)

1. Plan (max 5 bullets): what will change + files impacted + checks to run.
2. Implement minimal changes.
3. Run `./scripts/check.sh`.
4. If red: fix and rerun until green.
5. Commit, push, and open PR.

## Definition of Done (required before final message)

- `./scripts/check.sh` is green.
- No forbidden changes (see sub-agents).
- PR is small and focused.

## Safety guardrails

- Do not touch `infra/` or `.github/` unless explicitly requested.
- No destructive commands (e.g. `rm -rf`), mass deletions, or broad renames.
- If deletion is necessary, list exact files and wait for explicit confirmation.

## Logging policy (default)

- No "business logs" in services/controllers.
- Log unexpected errors (5xx/unhandled) in global error handler only.

## Final report (mandatory)

At the end, report:

- `./scripts/check.sh` result
- files modified
- commands executed
- manual verification steps (UI if relevant)
- commit hash + PR link
