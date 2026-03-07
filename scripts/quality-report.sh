#!/usr/bin/env bash
set -u
set -o pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND_DIR="$ROOT_DIR/frontend"
BACKEND_DIR="$ROOT_DIR/backend"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

timestamp="$(date -u '+%Y-%m-%d %H:%M:%S UTC')"

print_header() {
  local title="$1"
  echo
  echo "== $title =="
}

print_note() {
  local text="$1"
  echo "  - $text"
}

run_cmd() {
  local stdout_file="$1"
  local stderr_file="$2"
  shift 2
  "$@" >"$stdout_file" 2>"$stderr_file"
  return $?
}

parse_npm_outdated() {
  local json_file="$1"
  local package_json="$2"
  node - "$json_file" "$package_json" <<'NODE'
const fs = require("fs");
const jsonFile = process.argv[2];
const packageJsonFile = process.argv[3];

function readJson(path) {
  try {
    const raw = fs.readFileSync(path, "utf8").trim();
    return raw ? JSON.parse(raw) : {};
  } catch (_e) {
    return null;
  }
}

const outdated = readJson(jsonFile);
const pkg = readJson(packageJsonFile);
if (outdated === null || pkg === null) {
  console.log("PARSE_ERROR");
  process.exit(0);
}

const runtime = new Set(Object.keys(pkg.dependencies || {}));
const dev = new Set(Object.keys(pkg.devDependencies || {}));
const rows = Object.entries(outdated).map(([name, data]) => {
  let bucket = "other";
  if (runtime.has(name)) bucket = "runtime";
  if (dev.has(name)) bucket = "dev";
  return {
    bucket,
    name,
    current: data.current || "missing",
    wanted: data.wanted || "n/a",
    latest: data.latest || "n/a",
  };
});

rows.sort((a, b) => a.name.localeCompare(b.name));
const runtimeCount = rows.filter((r) => r.bucket === "runtime").length;
const devCount = rows.filter((r) => r.bucket === "dev").length;
const otherCount = rows.filter((r) => r.bucket === "other").length;

console.log(`SUMMARY ${runtimeCount} ${devCount} ${otherCount} ${rows.length}`);
for (const row of rows) {
  console.log(`${row.bucket}|${row.name}|${row.current}|${row.wanted}|${row.latest}`);
}
NODE
}

parse_npm_audit_counts() {
  local json_file="$1"
  node - "$json_file" <<'NODE'
const fs = require("fs");
const jsonFile = process.argv[2];
const fields = ["info", "low", "moderate", "high", "critical", "total"];

let data = {};
try {
  const raw = fs.readFileSync(jsonFile, "utf8").trim();
  data = raw ? JSON.parse(raw) : {};
} catch (_e) {
  console.log("0 0 0 0 0 0");
  process.exit(0);
}

const vulns = (data.metadata && data.metadata.vulnerabilities) || {};
const values = fields.map((field) => Number(vulns[field] || 0));
console.log(values.join(" "));
NODE
}

extract_maven_goal_block() {
  local mvn_log="$1"
  local goal="$2"
  awk -v goal="$goal" '
    $0 ~ ("\\[INFO\\] --- [^ ]+:[0-9.]+:" goal " ") {capture = 1; next}
    capture && $0 ~ /^\[INFO\] --- / {exit}
    capture {print}
  ' "$mvn_log" | sed 's/^\[INFO\] //'
}

print_maven_goal_summary() {
  local title="$1"
  local block_file="$2"

  echo "  - $title:"
  if [[ ! -s "$block_file" ]]; then
    echo "      no output (goal may not have run)"
    return
  fi

  if grep -Eiq 'up to date|no .*updates|latest versions|version specified' "$block_file"; then
    grep -Ei 'up to date|no .*updates|latest versions|version specified' "$block_file" | sed 's/^/      /'
    return
  fi

  grep -E -- '->|\$\{|^[A-Za-z0-9._-]+:' "$block_file" | sed 's/^/      /' || true
}

safe_subtract() {
  local left="$1"
  local right="$2"
  local result=$((left - right))
  if (( result < 0 )); then
    echo 0
  else
    echo "$result"
  fi
}

echo "== SideQuestLab :: local quality report =="
echo "Generated at: $timestamp"
echo "Mode: advisory only (non-blocking)"

print_header "1) Frontend direct dependency freshness"
if ! command -v npm >/dev/null 2>&1; then
  print_note "npm not found; skipping frontend freshness."
else
  outdated_json="$TMP_DIR/frontend-outdated.json"
  outdated_err="$TMP_DIR/frontend-outdated.err"
  (
    cd "$FRONTEND_DIR" || exit 1
    run_cmd "$outdated_json" "$outdated_err" npm outdated --json
  )
  outdated_exit=$?

  if [[ $outdated_exit -gt 1 ]]; then
    print_note "Could not run 'npm outdated --json'."
    sed -n '1,5p' "$outdated_err" | sed 's/^/    /'
  else
    parsed_outdated="$(parse_npm_outdated "$outdated_json" "$FRONTEND_DIR/package.json")"
    if [[ "$parsed_outdated" == "PARSE_ERROR" ]]; then
      print_note "Could not parse npm outdated output."
      sed -n '1,5p' "$outdated_err" | sed 's/^/    /'
    else
      summary_line="$(printf '%s\n' "$parsed_outdated" | sed -n '1p')"
      runtime_updates="$(printf '%s\n' "$summary_line" | awk '{print $2}')"
      dev_updates="$(printf '%s\n' "$summary_line" | awk '{print $3}')"
      other_updates="$(printf '%s\n' "$summary_line" | awk '{print $4}')"
      total_updates="$(printf '%s\n' "$summary_line" | awk '{print $5}')"

      print_note "Outdated direct dependencies: $total_updates (runtime=$runtime_updates, dev=$dev_updates, other=$other_updates)"
      if [[ "$total_updates" == "0" ]]; then
        print_note "All direct frontend dependencies are up to date."
      else
        printf '%s\n' "$parsed_outdated" | sed -n '2,$p' | while IFS='|' read -r bucket name current wanted latest; do
          echo "    - [$bucket] $name: $current -> $latest (wanted: $wanted)"
        done
      fi
    fi
  fi
fi

print_header "2) Backend parent/property/plugin freshness"
if [[ ! -x "$BACKEND_DIR/mvnw" ]]; then
  print_note "backend/mvnw not found or not executable; skipping backend freshness."
else
  mvn_log="$TMP_DIR/backend-versions.log"
  mvn_err="$TMP_DIR/backend-versions.err"
  (
    cd "$BACKEND_DIR" || exit 1
    run_cmd "$mvn_log" "$mvn_err" ./mvnw -B \
      -DgenerateBackupPoms=false \
      org.codehaus.mojo:versions-maven-plugin:2.18.0:display-parent-updates \
      org.codehaus.mojo:versions-maven-plugin:2.18.0:display-property-updates \
      org.codehaus.mojo:versions-maven-plugin:2.18.0:display-plugin-updates
  )
  mvn_exit=$?

  if [[ $mvn_exit -ne 0 ]]; then
    print_note "Could not collect backend freshness via versions-maven-plugin."
    sed -n '1,8p' "$mvn_err" | sed 's/^/    /'
  else
    parent_block="$TMP_DIR/parent-block.log"
    prop_block="$TMP_DIR/property-block.log"
    plugin_block="$TMP_DIR/plugin-block.log"

    extract_maven_goal_block "$mvn_log" "display-parent-updates" >"$parent_block"
    extract_maven_goal_block "$mvn_log" "display-property-updates" >"$prop_block"
    extract_maven_goal_block "$mvn_log" "display-plugin-updates" >"$plugin_block"

    print_maven_goal_summary "Parent updates" "$parent_block"
    print_maven_goal_summary "Property updates" "$prop_block"
    print_maven_goal_summary "Plugin updates" "$plugin_block"
  fi
fi

print_header "3) Security findings (runtime-relevant vs informational/dev)"
if ! command -v npm >/dev/null 2>&1; then
  print_note "npm not found; skipping frontend security audit."
else
  full_audit_json="$TMP_DIR/frontend-audit-full.json"
  full_audit_err="$TMP_DIR/frontend-audit-full.err"
  runtime_audit_json="$TMP_DIR/frontend-audit-runtime.json"
  runtime_audit_err="$TMP_DIR/frontend-audit-runtime.err"

  (
    cd "$FRONTEND_DIR" || exit 1
    run_cmd "$full_audit_json" "$full_audit_err" npm audit --json
  )
  full_audit_exit=$?

  (
    cd "$FRONTEND_DIR" || exit 1
    run_cmd "$runtime_audit_json" "$runtime_audit_err" npm audit --omit=dev --json
  )
  runtime_audit_exit=$?

  if [[ $full_audit_exit -gt 1 || $runtime_audit_exit -gt 1 ]]; then
    print_note "Could not complete npm audit."
    sed -n '1,8p' "$full_audit_err" | sed 's/^/    /'
    sed -n '1,8p' "$runtime_audit_err" | sed 's/^/    /'
  else
    read -r full_info full_low full_moderate full_high full_critical full_total \
      <<<"$(parse_npm_audit_counts "$full_audit_json")"
    read -r runtime_info runtime_low runtime_moderate runtime_high runtime_critical runtime_total \
      <<<"$(parse_npm_audit_counts "$runtime_audit_json")"

    dev_info="$(safe_subtract "$full_info" "$runtime_info")"
    dev_low="$(safe_subtract "$full_low" "$runtime_low")"
    dev_moderate="$(safe_subtract "$full_moderate" "$runtime_moderate")"
    dev_high="$(safe_subtract "$full_high" "$runtime_high")"
    dev_critical="$(safe_subtract "$full_critical" "$runtime_critical")"
    dev_total="$(safe_subtract "$full_total" "$runtime_total")"

    print_note "Runtime-relevant (frontend prod deps): total=$runtime_total critical=$runtime_critical high=$runtime_high moderate=$runtime_moderate low=$runtime_low info=$runtime_info"
    print_note "Informational/dev-only (frontend dev deps delta): total=$dev_total critical=$dev_critical high=$dev_high moderate=$dev_moderate low=$dev_low info=$dev_info"
  fi
fi

if command -v trivy >/dev/null 2>&1; then
  trivy_json="$TMP_DIR/trivy-backend.json"
  trivy_err="$TMP_DIR/trivy-backend.err"
  run_cmd "$trivy_json" "$trivy_err" trivy fs --scanners vuln --format json "$BACKEND_DIR"
  trivy_exit=$?
  if [[ $trivy_exit -ne 0 ]]; then
    print_note "Trivy is installed but backend scan failed."
    sed -n '1,8p' "$trivy_err" | sed 's/^/    /'
  else
    backend_totals="$(node - "$trivy_json" <<'NODE'
const fs = require("fs");
const file = process.argv[2];
const totals = {CRITICAL: 0, HIGH: 0, MEDIUM: 0, LOW: 0, UNKNOWN: 0};
try {
  const data = JSON.parse(fs.readFileSync(file, "utf8"));
  const results = Array.isArray(data.Results) ? data.Results : [];
  for (const result of results) {
    const vulns = Array.isArray(result.Vulnerabilities) ? result.Vulnerabilities : [];
    for (const v of vulns) {
      const sev = (v.Severity || "UNKNOWN").toUpperCase();
      totals[sev] = (totals[sev] || 0) + 1;
    }
  }
} catch (_e) {}
console.log(`${totals.CRITICAL} ${totals.HIGH} ${totals.MEDIUM} ${totals.LOW} ${totals.UNKNOWN}`);
NODE
)"
    read -r backend_critical backend_high backend_medium backend_low backend_unknown <<<"$backend_totals"
    print_note "Backend vulnerabilities via Trivy: critical=$backend_critical high=$backend_high medium=$backend_medium low=$backend_low unknown=$backend_unknown"
    print_note "Backend runtime/dev scope split is not inferred in this baseline."
  fi
else
  print_note "Trivy not installed; backend vulnerability scan skipped (recommended next step)."
fi

print_header "4) Future quality checks (placeholders, not blocking)"
print_note "Coverage: add JaCoCo for backend and Angular coverage report for frontend."
print_note "Static/dead code: add PMD + SpotBugs for backend and Knip for frontend."
print_note "Container/dependency security: add Trivy report artifacts and trend over time."

echo
echo "Done. This report is informational and does not change CI behavior."
