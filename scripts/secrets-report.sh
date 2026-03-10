#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPORT_DIR="$ROOT_DIR/reports/security"
TIMESTAMP="$(date '+%Y%m%d-%H%M%S')"

JSON_REPORT="$REPORT_DIR/gitleaks-git-${TIMESTAMP}.json"
SUMMARY_REPORT="$REPORT_DIR/gitleaks-git-${TIMESTAMP}.summary.txt"
LATEST_JSON="$REPORT_DIR/gitleaks-git-latest.json"
LATEST_SUMMARY="$REPORT_DIR/gitleaks-git-latest.summary.txt"

if ! command -v gitleaks >/dev/null 2>&1; then
  echo "Gitleaks CLI is not installed."
  echo "Install Gitleaks locally, then rerun: ./scripts/secrets-report.sh"
  echo "Installation guide: https://github.com/gitleaks/gitleaks#installing"
  exit 127
fi

mkdir -p "$REPORT_DIR"

echo "== SideQuestLab :: secrets baseline (Gitleaks, advisory-only) =="
echo "Output directory: $REPORT_DIR"
echo ""

pushd "$ROOT_DIR" >/dev/null

GITLEAKS_CMD=(
  gitleaks
  git
  --no-banner
  --redact
  --report-format json
  --report-path "$JSON_REPORT"
)

echo "Running: ${GITLEAKS_CMD[*]}"

set +e
"${GITLEAKS_CMD[@]}" >/dev/null
SCAN_STATUS=$?
set -e

if [[ "$SCAN_STATUS" -ne 0 && "$SCAN_STATUS" -ne 1 ]]; then
  echo "Gitleaks scan failed with exit code: $SCAN_STATUS"
  echo "No advisory report generated."
  popd >/dev/null
  exit "$SCAN_STATUS"
fi

cp "$JSON_REPORT" "$LATEST_JSON"

if command -v jq >/dev/null 2>&1; then
  FINDINGS_COUNT="$(jq 'length' "$JSON_REPORT")"
  RULE_SUMMARY="$(jq -r '
    sort_by(.RuleID // "unknown")
    | group_by(.RuleID // "unknown")
    | map({id:(.[0].RuleID // "unknown"), count:length})
    | sort_by(-.count, .id)
    | .[]
    | "- \(.id): \(.count)"
  ' "$JSON_REPORT")"
  FILE_SUMMARY="$(jq -r '
    sort_by(.File // "unknown")
    | group_by(.File // "unknown")
    | map({file:(.[0].File // "unknown"), count:length})
    | sort_by(-.count, .file)
    | .[:10]
    | .[]
    | "- \(.file): \(.count)"
  ' "$JSON_REPORT")"
  SUMMARY_MODE="exact (jq)"
else
  FINDINGS_COUNT="$(grep -c '"RuleID"' "$JSON_REPORT" || true)"
  RULE_SUMMARY="(Install jq for grouped rule summary.)"
  FILE_SUMMARY="(Install jq for grouped file summary.)"
  SUMMARY_MODE="approximate (grep fallback)"
fi

{
  echo "SideQuestLab Gitleaks local baseline (advisory only)"
  echo "Timestamp: $TIMESTAMP"
  echo "Root: $ROOT_DIR"
  echo "Scan command: ${GITLEAKS_CMD[*]}"
  echo "JSON report: $JSON_REPORT"
  echo "Total findings: $FINDINGS_COUNT [$SUMMARY_MODE]"
  if [[ "$SCAN_STATUS" -eq 0 ]]; then
    echo "Gitleaks exit status: 0 (no findings)"
  else
    echo "Gitleaks exit status: 1 (findings detected, non-blocking in this script)"
  fi
  echo ""
  echo "Top findings by rule:"
  if [[ -n "$RULE_SUMMARY" ]]; then
    echo "$RULE_SUMMARY"
  else
    echo "- none"
  fi
  echo ""
  echo "Top findings by file (first 10):"
  if [[ -n "$FILE_SUMMARY" ]]; then
    echo "$FILE_SUMMARY"
  else
    echo "- none"
  fi
} > "$SUMMARY_REPORT"

cp "$SUMMARY_REPORT" "$LATEST_SUMMARY"

popd >/dev/null

echo "Saved JSON report: $JSON_REPORT"
echo "Saved summary: $SUMMARY_REPORT"
echo "Latest JSON: $LATEST_JSON"
echo "Latest summary: $LATEST_SUMMARY"
echo ""
echo "Completed. This script is local/advisory only and does not block CI."
