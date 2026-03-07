# Local quality tooling baseline

## Purpose

`scripts/quality-report.sh` gives a local, non-blocking baseline for dependency freshness and security signals. It is designed for manual review and follow-up prompts, not CI enforcement.

## Signals in the report

1. Freshness
   - Frontend: direct dependency freshness from `npm outdated --json` (no transitive noise).
   - Backend: parent/property/plugin freshness from Maven versions plugin goals:
     - `display-parent-updates`
     - `display-property-updates`
     - `display-plugin-updates`

2. Security
   - Runtime-relevant signal: frontend production dependencies from `npm audit --omit=dev --json`.
   - Informational/dev signal: frontend dev-only delta (`npm audit --json` minus runtime counts).
   - Optional backend signal: Trivy (`trivy fs`) if installed locally.

3. Dead code / static analysis
   - Placeholder only in this baseline (not blocking yet).

4. Coverage
   - Placeholder only in this baseline (not blocking yet).

## Recommended next tools (future, non-blocking first)

- Trivy: backend/frontend vulnerability scanning with persisted artifacts.
- Knip: frontend dead code and unused exports/dependencies.
- PMD: backend static rules for maintainability and code smells.
- SpotBugs: backend bug-pattern detection on bytecode.
- JaCoCo / Angular coverage: baseline test coverage for backend/frontend.

## Run locally

```bash
./scripts/quality-report.sh
```

If a required external tool is missing, the report prints a clear skip note and continues.
