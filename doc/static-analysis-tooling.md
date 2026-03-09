# Local static-analysis tooling baseline

## Purpose

`scripts/static-analysis-report.sh` provides a local, advisory-only baseline for dead-code and bug-pattern signals. It is not wired into CI and does not change `./scripts/check.sh`.

## Tools in this slice

- Knip (frontend): unused dependencies/files/exports signal for Angular sources.
- PMD (backend): unused-oriented Java findings only (unused private fields/methods/local variables/formal parameters).
- SpotBugs (backend): bytecode bug-pattern scan for likely defects.

## Run

```bash
./scripts/static-analysis-report.sh
```

## Outputs

Reports are written under `reports/static-analysis/`:

- `frontend-knip.json`
- `backend-pmd.xml`
- `backend-spotbugs.xml`
- command stderr/stdout logs for each tool

The script prints a concise terminal summary and keeps raw files for manual review.

## Notes

- This slice is non-blocking by design: findings are informational.
- If a required tool is missing/unavailable, the script prints a clear failure reason and marks the run as incomplete.
