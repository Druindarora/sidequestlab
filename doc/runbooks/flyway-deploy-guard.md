# Flyway Deploy Guard

## Purpose

`./scripts/check.sh` includes a Flyway guard to make DB-related changes visible and to protect migration history from risky edits.

## What the guard enforces

Allowed (non-blocking warning):
- Adding a new migration file under `backend/src/main/resources/db/migration`.
- `check.sh` prints a high-visibility warning and lists the new files.

Blocked (fails `check.sh`):
- Modifying or deleting an existing migration file that already exists on the base branch.
- Historical Flyway migrations are treated as immutable.

## Base branch detection

The guard compares against:
1. `origin/dev` when available.
2. Local `dev` when `origin/dev` is unavailable.
3. A safe fallback (`HEAD~1`, then `HEAD`) when neither ref exists.

`check.sh` prints which base ref is used so behavior is explicit locally and in CI.

## DB deploy reminder when migrations are present

When new Flyway migrations are detected:
- Perform a manual backup before DB-related deploy.
- Deploy only after checks are green.
- Rollback principle: restore backup + redeploy a compatible app version.
