#!/usr/bin/env bash
set -u
set -o pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND_DIR="$ROOT_DIR/frontend"
BACKEND_DIR="$ROOT_DIR/backend"
REPORT_DIR="$ROOT_DIR/reports/static-analysis"

TIMESTAMP="$(date -u '+%Y-%m-%d %H:%M:%S UTC')"
KNIP_VERSION="5.86.0"

mkdir -p "$REPORT_DIR"

STATUS_KNIP="not-run"
STATUS_PMD="not-run"
STATUS_SPOTBUGS="not-run"

print_header() {
  local title="$1"
  echo
  echo "== $title =="
}

print_note() {
  local message="$1"
  echo "  - $message"
}

count_with_grep() {
  local pattern="$1"
  local file="$2"
  grep -o "$pattern" "$file" 2>/dev/null | wc -l | tr -d ' '
}

run_knip() {
  local knip_json="$REPORT_DIR/frontend-knip.json"
  local knip_stderr="$REPORT_DIR/frontend-knip.stderr.log"
  local knip_parser_err="$REPORT_DIR/frontend-knip-parser.stderr.log"
  local knip_cmd_label=""

  if [[ ! -d "$FRONTEND_DIR" ]]; then
    STATUS_KNIP="failed"
    print_note "Frontend directory not found at $FRONTEND_DIR"
    return
  fi

  if ! command -v npm >/dev/null 2>&1; then
    STATUS_KNIP="failed"
    print_note "npm not found. Knip scan skipped."
    return
  fi

  if [[ -x "$FRONTEND_DIR/node_modules/.bin/knip" ]]; then
    knip_cmd_label="./node_modules/.bin/knip"
    (
      cd "$FRONTEND_DIR" || exit 1
      ./node_modules/.bin/knip \
        --config knip.json \
        --include files,dependencies,exports,unlisted,unresolved \
        --reporter json \
        --no-progress \
        --no-exit-code \
        >"$knip_json" 2>"$knip_stderr"
    )
  else
    if ! command -v npx >/dev/null 2>&1; then
      STATUS_KNIP="failed"
      print_note "npx not found. Knip scan skipped."
      return
    fi

    knip_cmd_label="npx --yes knip@${KNIP_VERSION}"
    (
      cd "$FRONTEND_DIR" || exit 1
      npx --yes "knip@${KNIP_VERSION}" \
        --config knip.json \
        --include files,dependencies,exports,unlisted,unresolved \
        --reporter json \
        --no-progress \
        --no-exit-code \
        >"$knip_json" 2>"$knip_stderr"
    )
  fi

  local knip_exit=$?
  if (( knip_exit != 0 )); then
    STATUS_KNIP="failed"
    print_note "Knip command failed (exit=$knip_exit). See $knip_stderr"
    return
  fi

  if [[ ! -s "$knip_json" ]]; then
    STATUS_KNIP="failed"
    print_note "Knip did not produce JSON output at $knip_json"
    return
  fi

  if ! command -v node >/dev/null 2>&1; then
    STATUS_KNIP="ok"
    print_note "Knip completed via '$knip_cmd_label'."
    print_note "Node is unavailable for summary parsing. Raw report: $knip_json"
    return
  fi

  local parsed
  parsed="$(
    node - "$knip_json" <<'NODE' 2>"$knip_parser_err"
const fs = require("fs");
const file = process.argv[2];

function asArray(value) {
  return Array.isArray(value) ? value : [];
}

const report = JSON.parse(fs.readFileSync(file, "utf8"));
const files = asArray(report.files).length;

let deps = 0;
let unlisted = 0;
let unresolved = 0;
let exportsCount = 0;
let issueFiles = 0;

for (const issue of asArray(report.issues)) {
  issueFiles += 1;
  deps += asArray(issue.dependencies).length;
  deps += asArray(issue.devDependencies).length;
  deps += asArray(issue.optionalPeerDependencies).length;
  unlisted += asArray(issue.unlisted).length;
  unresolved += asArray(issue.unresolved).length;
  exportsCount += asArray(issue.exports).length;
}

console.log(`${files} ${deps} ${unlisted} ${unresolved} ${exportsCount} ${issueFiles}`);
NODE
  )"

  if [[ $? -ne 0 || -z "$parsed" ]]; then
    STATUS_KNIP="failed"
    print_note "Knip report parsing failed. See $knip_parser_err"
    return
  fi

  local files_count deps_count unlisted_count unresolved_count exports_count issue_files_count
  read -r files_count deps_count unlisted_count unresolved_count exports_count issue_files_count <<<"$parsed"

  STATUS_KNIP="ok"
  print_note "Knip command: $knip_cmd_label"
  print_note "Knip findings: files=$files_count deps=$deps_count unlisted=$unlisted_count unresolved=$unresolved_count exports=$exports_count (issueFiles=$issue_files_count)"
  print_note "Knip raw output: $knip_json"
  if [[ -s "$knip_stderr" ]]; then
    print_note "Knip stderr log: $knip_stderr"
  fi
}

run_pmd() {
  local pmd_log="$REPORT_DIR/backend-pmd.stdout.log"
  local pmd_err="$REPORT_DIR/backend-pmd.stderr.log"
  local pmd_xml_src="$BACKEND_DIR/target/pmd.xml"
  local pmd_xml_dst="$REPORT_DIR/backend-pmd.xml"

  if [[ ! -x "$BACKEND_DIR/mvnw" ]]; then
    STATUS_PMD="failed"
    print_note "backend/mvnw not found or not executable. PMD scan skipped."
    return
  fi

  (
    cd "$BACKEND_DIR" || exit 1
    ./mvnw -B -DskipTests pmd:pmd >"$pmd_log" 2>"$pmd_err"
  )
  local pmd_exit=$?

  if (( pmd_exit != 0 )); then
    STATUS_PMD="failed"
    print_note "PMD command failed (exit=$pmd_exit). See $pmd_log and $pmd_err"
    return
  fi

  if [[ ! -f "$pmd_xml_src" ]]; then
    STATUS_PMD="failed"
    print_note "PMD finished but report file is missing at $pmd_xml_src"
    return
  fi

  cp "$pmd_xml_src" "$pmd_xml_dst"

  local total rule_summary
  total="$(count_with_grep "<violation " "$pmd_xml_dst")"
  rule_summary="$(
    grep -oE "rule=['\"][^'\"]*['\"]" "$pmd_xml_dst" \
      | sed -E "s/rule=['\"]([^'\"]*)['\"]/\\1/" \
      | sort \
      | uniq -c \
      | sort -nr \
      | head -5 || true
  )"

  STATUS_PMD="ok"
  print_note "PMD command: ./mvnw -B -DskipTests pmd:pmd"
  print_note "PMD violations: $total"
  if [[ -n "$rule_summary" ]]; then
    print_note "Top PMD rule categories:"
    while IFS= read -r line; do
      [[ -n "$line" ]] && echo "    $line"
    done <<<"$rule_summary"
  fi
  print_note "PMD raw output: $pmd_xml_dst"
}

run_spotbugs() {
  local spotbugs_log="$REPORT_DIR/backend-spotbugs.stdout.log"
  local spotbugs_err="$REPORT_DIR/backend-spotbugs.stderr.log"
  local spotbugs_xml_src="$BACKEND_DIR/target/spotbugsXml.xml"
  local spotbugs_xml_dst="$REPORT_DIR/backend-spotbugs.xml"

  if [[ ! -x "$BACKEND_DIR/mvnw" ]]; then
    STATUS_SPOTBUGS="failed"
    print_note "backend/mvnw not found or not executable. SpotBugs scan skipped."
    return
  fi

  (
    cd "$BACKEND_DIR" || exit 1
    ./mvnw -B -DskipTests compile spotbugs:spotbugs >"$spotbugs_log" 2>"$spotbugs_err"
  )
  local spotbugs_exit=$?

  if (( spotbugs_exit != 0 )); then
    STATUS_SPOTBUGS="failed"
    print_note "SpotBugs command failed (exit=$spotbugs_exit). See $spotbugs_log and $spotbugs_err"
    return
  fi

  if [[ ! -f "$spotbugs_xml_src" ]]; then
    STATUS_SPOTBUGS="failed"
    print_note "SpotBugs finished but report file is missing at $spotbugs_xml_src"
    return
  fi

  cp "$spotbugs_xml_src" "$spotbugs_xml_dst"

  local total category_summary
  total="$(count_with_grep "<BugInstance " "$spotbugs_xml_dst")"
  category_summary="$(
    grep -oE "<BugInstance [^>]*>" "$spotbugs_xml_dst" \
      | grep -oE "category=['\"][^'\"]*['\"]" \
      | sed -E "s/category=['\"]([^'\"]*)['\"]/\\1/" \
      | sort \
      | uniq -c \
      | sort -nr \
      | head -5 || true
  )"

  STATUS_SPOTBUGS="ok"
  print_note "SpotBugs command: ./mvnw -B -DskipTests compile spotbugs:spotbugs"
  print_note "SpotBugs findings: $total"
  if [[ -n "$category_summary" ]]; then
    print_note "Top SpotBugs categories:"
    while IFS= read -r line; do
      [[ -n "$line" ]] && echo "    $line"
    done <<<"$category_summary"
  fi
  print_note "SpotBugs raw output: $spotbugs_xml_dst"
}

echo "== SideQuestLab :: static analysis report =="
echo "Generated at: $TIMESTAMP"
echo "Mode: advisory only (non-blocking findings)"
echo "Report directory: $REPORT_DIR"

print_header "1) Frontend dead-code scan (Knip)"
run_knip

print_header "2) Backend unused-code scan (PMD)"
run_pmd

print_header "3) Backend bug-pattern scan (SpotBugs)"
run_spotbugs

echo
echo "== Summary =="
echo "  - Knip: $STATUS_KNIP"
echo "  - PMD: $STATUS_PMD"
echo "  - SpotBugs: $STATUS_SPOTBUGS"
echo "  - Raw reports: $REPORT_DIR"

if [[ "$STATUS_KNIP" != "ok" || "$STATUS_PMD" != "ok" || "$STATUS_SPOTBUGS" != "ok" ]]; then
  echo "  - Result: INCOMPLETE (at least one scan failed or was unavailable)"
  exit 1
fi

echo "  - Result: COMPLETE"
