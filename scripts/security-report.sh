#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPORT_DIR="$ROOT_DIR/reports/security"
TIMESTAMP="$(date '+%Y%m%d-%H%M%S')"

TABLE_REPORT="$REPORT_DIR/trivy-repo-${TIMESTAMP}.table.txt"
JSON_REPORT="$REPORT_DIR/trivy-repo-${TIMESTAMP}.json"
LATEST_TABLE="$REPORT_DIR/trivy-repo-latest.table.txt"
LATEST_JSON="$REPORT_DIR/trivy-repo-latest.json"

if ! command -v trivy >/dev/null 2>&1; then
  echo "Trivy CLI is not installed."
  echo "Install Trivy locally, then rerun: ./scripts/security-report.sh"
  echo "Installation guide: https://trivy.dev/latest/getting-started/installation/"
  exit 127
fi

mkdir -p "$REPORT_DIR"

echo "== SideQuestLab :: security baseline (Trivy, advisory-only) =="
echo "Output directory: $REPORT_DIR"
echo ""

pushd "$ROOT_DIR" >/dev/null

COMMON_ARGS=(
  fs
  --scanners vuln
  --no-progress
  --skip-dirs frontend/node_modules
  --skip-dirs frontend/dist
  --skip-dirs backend/target
  --skip-dirs reports
  .
)

echo "1) High-signal summary (HIGH,CRITICAL) -> table"
trivy "${COMMON_ARGS[@]}" \
  --severity HIGH,CRITICAL \
  --format table \
  --output "$TABLE_REPORT"
cp "$TABLE_REPORT" "$LATEST_TABLE"
echo "   saved: $TABLE_REPORT"
echo "   latest: $LATEST_TABLE"
echo ""

echo "2) Full vulnerability baseline (all severities) -> json"
trivy "${COMMON_ARGS[@]}" \
  --format json \
  --output "$JSON_REPORT"
cp "$JSON_REPORT" "$LATEST_JSON"
echo "   saved: $JSON_REPORT"
echo "   latest: $LATEST_JSON"
echo ""

popd >/dev/null

echo "Completed. This script is local/advisory only and does not change CI gates."
