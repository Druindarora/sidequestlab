#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND_DIR="$ROOT_DIR/frontend"
BACKEND_DIR="$ROOT_DIR/backend"

frontend_lcov=""
frontend_html_report=""
backend_jacoco_xml="$BACKEND_DIR/target/site/jacoco/jacoco.xml"
backend_html_report="$BACKEND_DIR/target/site/jacoco/index.html"
coverage_summary_json="$ROOT_DIR/coverage-summary.json"
coverage_summary_markdown="$ROOT_DIR/coverage-summary.md"

fail() {
  echo "ERROR: $1" >&2
  exit 1
}

format_percent() {
  local covered="$1"
  local total="$2"

  if [[ "$total" -eq 0 ]]; then
    echo "n/a"
    return
  fi

  awk -v c="$covered" -v t="$total" 'BEGIN { printf "%.2f", (c / t) * 100 }'
}

extract_lcov_totals() {
  local metric_total="$1"
  local metric_covered="$2"
  local file="$3"

  local total
  local covered

  total=$(awk -F: -v key="$metric_total" '$1 == key { sum += $2 } END { print sum + 0 }' "$file")
  covered=$(awk -F: -v key="$metric_covered" '$1 == key { sum += $2 } END { print sum + 0 }' "$file")

  echo "$total $covered"
}

extract_jacoco_totals() {
  local counter_type="$1"
  local xml_file="$2"

  local line
  local missed
  local covered

  line=$(grep -o "<counter type=\"$counter_type\" missed=\"[0-9]*\" covered=\"[0-9]*\"/>" "$xml_file" | tail -n1 || true)
  if [[ -z "$line" ]]; then
    echo "0 0"
    return
  fi

  missed=$(echo "$line" | sed -E 's/.*missed="([0-9]+)".*/\1/')
  covered=$(echo "$line" | sed -E 's/.*covered="([0-9]+)".*/\1/')
  echo "$missed $covered"
}

echo "== Frontend :: coverage =="
pushd "$FRONTEND_DIR" >/dev/null
npm ci
npm run test:coverage
popd >/dev/null

frontend_lcov=$(find "$FRONTEND_DIR/coverage" -type f -name "lcov.info" | head -n1 || true)
[[ -n "$frontend_lcov" ]] || fail "frontend lcov.info not found under $FRONTEND_DIR/coverage"

frontend_html_report=$(find "$(dirname "$frontend_lcov")" -maxdepth 3 -type f -name "index.html" | head -n1 || true)
[[ -n "$frontend_html_report" ]] || fail "frontend HTML coverage report not found near $frontend_lcov"

echo ""
echo "== Backend :: coverage =="
pushd "$BACKEND_DIR" >/dev/null
chmod +x ./mvnw
./mvnw -B test -Dspring.profiles.active=test
popd >/dev/null

[[ -f "$backend_jacoco_xml" ]] || fail "JaCoCo XML report not found at $backend_jacoco_xml"
[[ -f "$backend_html_report" ]] || fail "JaCoCo HTML report not found at $backend_html_report"

read -r frontend_line_total frontend_line_covered < <(extract_lcov_totals "LF" "LH" "$frontend_lcov")
read -r frontend_function_total frontend_function_covered < <(extract_lcov_totals "FNF" "FNH" "$frontend_lcov")
read -r frontend_branch_total frontend_branch_covered < <(extract_lcov_totals "BRF" "BRH" "$frontend_lcov")

frontend_line_percent=$(format_percent "$frontend_line_covered" "$frontend_line_total")
frontend_function_percent=$(format_percent "$frontend_function_covered" "$frontend_function_total")
frontend_branch_percent=$(format_percent "$frontend_branch_covered" "$frontend_branch_total")

read -r backend_line_missed backend_line_covered < <(extract_jacoco_totals "LINE" "$backend_jacoco_xml")
read -r backend_branch_missed backend_branch_covered < <(extract_jacoco_totals "BRANCH" "$backend_jacoco_xml")
read -r backend_instruction_missed backend_instruction_covered < <(extract_jacoco_totals "INSTRUCTION" "$backend_jacoco_xml")

backend_line_total=$((backend_line_missed + backend_line_covered))
backend_branch_total=$((backend_branch_missed + backend_branch_covered))
backend_instruction_total=$((backend_instruction_missed + backend_instruction_covered))

backend_line_percent=$(format_percent "$backend_line_covered" "$backend_line_total")
backend_branch_percent=$(format_percent "$backend_branch_covered" "$backend_branch_total")
backend_instruction_percent=$(format_percent "$backend_instruction_covered" "$backend_instruction_total")

echo ""
echo "== Coverage Summary =="
echo "Frontend (Angular/lcov)"
echo "  - Lines: ${frontend_line_percent}% (${frontend_line_covered}/${frontend_line_total})"
echo "  - Functions: ${frontend_function_percent}% (${frontend_function_covered}/${frontend_function_total})"
echo "  - Branches: ${frontend_branch_percent}% (${frontend_branch_covered}/${frontend_branch_total})"
echo "Backend (JaCoCo)"
echo "  - Lines: ${backend_line_percent}% (${backend_line_covered}/${backend_line_total})"
echo "  - Branches: ${backend_branch_percent}% (${backend_branch_covered}/${backend_branch_total})"
echo "  - Instructions: ${backend_instruction_percent}% (${backend_instruction_covered}/${backend_instruction_total})"
echo ""

python3 "$ROOT_DIR/scripts/coverage-summary.py" \
  --frontend-lcov "$frontend_lcov" \
  --backend-jacoco "$backend_jacoco_xml" \
  --root-dir "$ROOT_DIR" \
  --json-output "$coverage_summary_json" \
  --markdown-output "$coverage_summary_markdown" \
  --top-limit 5

echo "Generated compact markdown summary: $coverage_summary_markdown"
echo "Generated machine summary JSON:   $coverage_summary_json"
echo ""
echo "Frontend HTML report: $frontend_html_report"
echo "Backend HTML report:  $backend_html_report"
