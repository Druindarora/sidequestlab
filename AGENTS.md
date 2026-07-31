# AGENTS — SideQuestLab (Monorepo)

## Scope & intent

Deliver small, reviewable changes with CI green. Prefer minimal diffs, no refactors outside scope.

## Repo layout

- Public Astro site: `apps/public-site/` (follow its local README/AGENTS when present)
- Private Angular app rules: `frontend/AGENTS.md`
- Backend rules: `backend/AGENTS.md`
  If you modify files under `apps/public-site/`, `frontend/`, or `backend/`, you must follow the applicable local guidance.

## Branching & PR

- Branch creation, commit, push, and PR creation may be handled by an external orchestrator.
- Do not create branches, commit, push, or open PRs unless explicitly requested by the user prompt.
- If publishing is explicitly requested, never commit on `main` or `dev`; start from `dev`, create a `work/<topic>` branch, and open a PR targeting `dev`.

## Work loop (mandatory)

1. Plan (max 5 bullets): what will change + files impacted + checks to run.
2. Implement minimal changes.
3. Run `./scripts/check.sh`.
4. If red: fix once and rerun.

## Definition of Done (required before final message)

- `./scripts/check.sh` is green.
- No forbidden changes (see sub-agents).
- If publishing was explicitly requested, PR is small and focused.

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
- commit message
- commit hash + PR link, only when publishing was explicitly requested
