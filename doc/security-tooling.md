# Local security tooling baseline (Trivy)

## Scope of this slice

- Advisory-only and local-only.
- Uses Trivy as an external CLI tool (not an application dependency).
- No CI automation, no blocking thresholds, and no `./scripts/check.sh` changes.
- Focused on vulnerability scanning for practical signal.

## Install Trivy locally

Install Trivy on your machine using the official instructions:

- https://trivy.dev/latest/getting-started/installation/

## Run the baseline report

```bash
./scripts/security-report.sh
```

If Trivy is missing, the script exits with a clear install message and a non-success code.

## Report outputs

Reports are written to `reports/security/`:

- `trivy-repo-<timestamp>.table.txt`:
  - Human-readable summary, filtered to `HIGH` and `CRITICAL`.
- `trivy-repo-<timestamp>.json`:
  - Machine-readable JSON baseline (all severities).
- `trivy-repo-latest.table.txt` and `trivy-repo-latest.json`:
  - Convenience copies of the most recent run.

## Current scan coverage

- Scanner: `trivy fs --scanners vuln`
- Target: repository filesystem
- Excludes for noise/performance: `frontend/node_modules`, `frontend/dist`, `backend/target`, `reports`
- Out of scope in this slice: secret scanning, misconfiguration scanning, Gitleaks
