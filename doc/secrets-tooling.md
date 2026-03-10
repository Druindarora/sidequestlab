# Local secrets tooling baseline (Gitleaks)

## Scope of this slice

- Advisory-only and local-only.
- Uses Gitleaks as an external CLI tool via `scripts/secrets-report.sh`.
- No CI automation, no blocking thresholds, and no `./scripts/check.sh` changes.
- Focused on repository history scanning (`gitleaks git`) for an initial leak baseline.

## Install Gitleaks locally

Install Gitleaks on your machine using the official instructions:

- https://github.com/gitleaks/gitleaks#installing

## Run the local report

```bash
./scripts/secrets-report.sh
```

If Gitleaks is missing, the script exits with a clear install message and a non-success code.

## Report outputs

Reports are written to `reports/security/`:

- `gitleaks-git-<timestamp>.json`
  - Machine-readable source-of-truth report.
- `gitleaks-git-<timestamp>.summary.txt`
  - Concise human-readable summary with total findings and grouped breakdowns.
- `gitleaks-git-latest.json` and `gitleaks-git-latest.summary.txt`
  - Convenience copies of the most recent run.

## Current scan coverage

- Scanner: `gitleaks git`
- Target: repository git history
- Notes:
  - No custom rules in this slice.
  - No baseline suppression in this slice.
  - Findings are non-blocking in this local script.
