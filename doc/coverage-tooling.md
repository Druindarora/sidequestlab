# Local coverage baseline

## Purpose

`scripts/coverage-report.sh` provides a local, report-only coverage baseline for frontend and backend. It is non-blocking and does not add any coverage thresholds.

## What the script runs

1. Frontend: `cd frontend && npm ci && npm run test:coverage`
2. Backend: `cd backend && ./mvnw -B test -Dspring.profiles.active=test`

## Generated reports

- Frontend HTML report: `frontend/coverage/**/index.html` (generated from Angular/lcov output)
- Backend HTML report: `backend/target/site/jacoco/index.html`
- Compact markdown summary: `coverage-summary.md` (decision-oriented for GitHub step summary)
- Machine-readable summary: `coverage-summary.json` (stable shape for automation)

The script also prints a concise project-level summary in stdout using:
- Frontend lcov totals (lines, functions, branches)
- Backend JaCoCo totals (lines, branches, instructions)

`coverage-summary.json` includes:
- Frontend totals plus prioritized/weak Angular app units under `frontend/src/app/**` (excluding `frontend/src/app/api/**` and test files)
- Backend totals plus prioritized packages and weak classes from JaCoCo XML
- Deterministic ordering (branch coverage first, then line coverage)

## Policy for this slice

- Report-only baseline (non-blocking)
- `./scripts/check.sh` remains the main project gate
- No thresholds, cron integration, or mutation testing in this slice
