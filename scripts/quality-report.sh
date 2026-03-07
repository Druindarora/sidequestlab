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

function parseMajor(version) {
  const normalized = String(version || "")
    .trim()
    .replace(/^[^0-9]*/, "");
  const major = Number(normalized.split(".")[0]);
  return Number.isFinite(major) ? major : null;
}

const runtime = new Set(Object.keys(pkg.dependencies || {}));
const dev = new Set(Object.keys(pkg.devDependencies || {}));
const rows = Object.entries(outdated).map(([name, data]) => {
  let bucket = "other";
  if (runtime.has(name)) bucket = "runtime";
  if (dev.has(name)) bucket = "dev";

  const current = data.current || "missing";
  const wanted = data.wanted || "n/a";
  const latest = data.latest || "n/a";

  const actionable = wanted !== "n/a" && current !== wanted;
  const majorBase = wanted !== "n/a" ? wanted : current;
  const latestMajor = parseMajor(latest);
  const majorBaseMajor = parseMajor(majorBase);
  const latestMajorHigher =
    latestMajor !== null && majorBaseMajor !== null && latestMajor > majorBaseMajor;

  return {
    bucket,
    name,
    current,
    wanted,
    latest,
    actionable,
    latestMajorHigher,
  };
});

rows.sort((a, b) => a.name.localeCompare(b.name));

const countBy = (bucket, actionable) =>
  rows.filter((r) => r.bucket === bucket && r.actionable === actionable).length;

const runtimeActionable = countBy("runtime", true);
const devActionable = countBy("dev", true);
const otherActionable = countBy("other", true);
const runtimeInfo = countBy("runtime", false);
const devInfo = countBy("dev", false);
const otherInfo = countBy("other", false);

console.log(
  `SUMMARY ${runtimeActionable} ${devActionable} ${otherActionable} ${runtimeInfo} ${devInfo} ${otherInfo} ${rows.length}`
);
for (const row of rows) {
  const kind = row.actionable ? "actionable" : "informational";
  const majorFlag = row.latestMajorHigher ? "yes" : "no";
  console.log(
    `${row.bucket}|${row.name}|${row.current}|${row.wanted}|${row.latest}|${kind}|${majorFlag}`
  );
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

extract_major() {
  local version="$1"
  local cleaned
  cleaned="$(printf '%s' "$version" | sed -E 's/^[^0-9]*//')"
  local major="${cleaned%%.*}"
  if [[ "$major" =~ ^[0-9]+$ ]]; then
    echo "$major"
  fi
}

is_prerelease_version() {
  local version="$1"
  [[ "$version" =~ -[Mm][0-9]*($|[^0-9]) ]] || \
  [[ "$version" =~ -[Rr][Cc][0-9]*($|[^0-9]) ]] || \
  [[ "$version" =~ -[Ss][Nn][Aa][Pp][Ss][Hh][Oo][Tt] ]] || \
  [[ "$version" =~ [Aa]lpha ]] || \
  [[ "$version" =~ [Bb]eta ]]
}

extract_target_version() {
  local line="$1"
  printf '%s\n' "$line" | sed -E 's/.*-> *([^[:space:]]+).*/\1/'
}

extract_spring_boot_parent_version() {
  awk '
    /<parent>/ {in_parent = 1}
    in_parent && /<artifactId>spring-boot-starter-parent<\/artifactId>/ {is_boot_parent = 1}
    in_parent && is_boot_parent && /<version>/ {
      line = $0
      sub(/^.*<version>/, "", line)
      sub(/<\/version>.*$/, "", line)
      print line
      exit
    }
    /<\/parent>/ {
      in_parent = 0
      is_boot_parent = 0
    }
  ' "$BACKEND_DIR/pom.xml"
}

backend_policy_watch_reason() {
  local line="$1"
  local boot_major="$2"
  local target_version
  target_version="$(extract_target_version "$line")"
  local target_major
  target_major="$(extract_major "$target_version")"

  if [[ "$line" == *"spring-boot-starter-parent"* ]] && [[ -n "$boot_major" ]] && [[ -n "$target_major" ]] && (( target_major > boot_major )); then
    echo "major deferred by Spring Boot ${boot_major}.x policy"
    return
  fi

  if [[ "$line" == *"springdoc"* ]] && [[ "$boot_major" == "3" ]] && [[ -n "$target_major" ]] && (( target_major >= 3 )); then
    echo "springdoc ${target_major}.x deferred while staying on Spring Boot 3.x"
    return
  fi
}

collect_update_records() {
  local goal_label="$1"
  local block_file="$2"
  local out_file="$3"
  if [[ -s "$block_file" ]]; then
    grep -E -- '->' "$block_file" | sed 's/^[[:space:]]*//' | while IFS= read -r line; do
      [[ -n "$line" ]] && printf '%s|%s\n' "$goal_label" "$line" >>"$out_file"
    done
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
      runtime_actionable="$(printf '%s\n' "$summary_line" | awk '{print $2}')"
      dev_actionable="$(printf '%s\n' "$summary_line" | awk '{print $3}')"
      other_actionable="$(printf '%s\n' "$summary_line" | awk '{print $4}')"
      runtime_info="$(printf '%s\n' "$summary_line" | awk '{print $5}')"
      dev_info="$(printf '%s\n' "$summary_line" | awk '{print $6}')"
      other_info="$(printf '%s\n' "$summary_line" | awk '{print $7}')"
      total_updates="$(printf '%s\n' "$summary_line" | awk '{print $8}')"

      total_actionable=$((runtime_actionable + dev_actionable + other_actionable))
      total_info=$((runtime_info + dev_info + other_info))

      print_note "Direct dependency signals: total=$total_updates actionable=$total_actionable informational=$total_info"
      print_note "Actionable now by scope: runtime=$runtime_actionable dev=$dev_actionable other=$other_actionable"
      if [[ "$total_updates" == "0" ]]; then
        print_note "All direct frontend dependencies are up to date."
      else
        printf '%s\n' "$parsed_outdated" | sed -n '2,$p' | while IFS='|' read -r bucket name current wanted latest classification latest_major_higher; do
          if [[ "$classification" == "actionable" ]]; then
            note="actionable now"
            if [[ "$latest_major_higher" == "yes" ]]; then
              note="$note; latest major $latest ignored"
            elif [[ "$latest" != "$wanted" && "$latest" != "n/a" ]]; then
              note="$note; latest $latest informational only"
            fi
            echo "    - [$bucket] $name: $current -> $wanted ($note)"
          else
            if [[ "$wanted" == "$current" && "$latest" != "$current" && "$latest" != "n/a" ]]; then
              if [[ "$latest_major_higher" == "yes" ]]; then
                echo "    - [$bucket] $name: $current (informational only; pinned at $wanted, major $latest deferred)"
              else
                echo "    - [$bucket] $name: $current (informational only; pinned at $wanted, latest $latest available)"
              fi
            else
              echo "    - [$bucket] $name: $current (informational only)"
            fi
          fi
        done
      fi
    fi
  fi
fi

print_header "2) Backend parent/property/plugin freshness"
if [[ ! -x "$BACKEND_DIR/mvnw" ]]; then
  print_note "backend/mvnw not found or not executable; skipping backend freshness."
else
  spring_boot_parent_version="$(extract_spring_boot_parent_version)"
  spring_boot_major="$(extract_major "$spring_boot_parent_version")"
  prerelease_ignore_regex='.*-[mM][0-9]*,.*-[rR][cC][0-9]*,.*-[sS][nN][aA][pP][sS][hH][oO][tT].*,.*[aA]lpha.*,.*[bB]eta.*'

  mvn_actionable_log="$TMP_DIR/backend-versions-actionable.log"
  mvn_actionable_err="$TMP_DIR/backend-versions-actionable.err"
  mvn_full_log="$TMP_DIR/backend-versions-full.log"
  mvn_full_err="$TMP_DIR/backend-versions-full.err"

  (
    cd "$BACKEND_DIR" || exit 1
    run_cmd "$mvn_actionable_log" "$mvn_actionable_err" ./mvnw -B \
      -DgenerateBackupPoms=false \
      "-Dmaven.version.ignore=$prerelease_ignore_regex" \
      org.codehaus.mojo:versions-maven-plugin:2.18.0:display-parent-updates \
      org.codehaus.mojo:versions-maven-plugin:2.18.0:display-property-updates \
      org.codehaus.mojo:versions-maven-plugin:2.18.0:display-plugin-updates
  )
  mvn_actionable_exit=$?

  (
    cd "$BACKEND_DIR" || exit 1
    run_cmd "$mvn_full_log" "$mvn_full_err" ./mvnw -B \
      -DgenerateBackupPoms=false \
      org.codehaus.mojo:versions-maven-plugin:2.18.0:display-parent-updates \
      org.codehaus.mojo:versions-maven-plugin:2.18.0:display-property-updates \
      org.codehaus.mojo:versions-maven-plugin:2.18.0:display-plugin-updates
  )
  mvn_full_exit=$?

  if [[ $mvn_actionable_exit -ne 0 && $mvn_full_exit -ne 0 ]]; then
    print_note "Could not collect backend freshness via versions-maven-plugin."
    sed -n '1,8p' "$mvn_actionable_err" | sed 's/^/    /'
    sed -n '1,8p' "$mvn_full_err" | sed 's/^/    /'
  else
    actionable_parent="$TMP_DIR/actionable-parent.log"
    actionable_prop="$TMP_DIR/actionable-property.log"
    actionable_plugin="$TMP_DIR/actionable-plugin.log"

    full_parent="$TMP_DIR/full-parent.log"
    full_prop="$TMP_DIR/full-property.log"
    full_plugin="$TMP_DIR/full-plugin.log"

    extract_maven_goal_block "$mvn_actionable_log" "display-parent-updates" >"$actionable_parent"
    extract_maven_goal_block "$mvn_actionable_log" "display-property-updates" >"$actionable_prop"
    extract_maven_goal_block "$mvn_actionable_log" "display-plugin-updates" >"$actionable_plugin"

    extract_maven_goal_block "$mvn_full_log" "display-parent-updates" >"$full_parent"
    extract_maven_goal_block "$mvn_full_log" "display-property-updates" >"$full_prop"
    extract_maven_goal_block "$mvn_full_log" "display-plugin-updates" >"$full_plugin"

    actionable_raw="$TMP_DIR/backend-actionable-raw.txt"
    full_raw="$TMP_DIR/backend-full-raw.txt"
    : >"$actionable_raw"
    : >"$full_raw"

    collect_update_records "parent" "$actionable_parent" "$actionable_raw"
    collect_update_records "property" "$actionable_prop" "$actionable_raw"
    collect_update_records "plugin" "$actionable_plugin" "$actionable_raw"

    collect_update_records "parent" "$full_parent" "$full_raw"
    collect_update_records "property" "$full_prop" "$full_raw"
    collect_update_records "plugin" "$full_plugin" "$full_raw"

    actionable_final="$TMP_DIR/backend-actionable-final.txt"
    actionable_keys="$TMP_DIR/backend-actionable-keys.txt"
    watchlist_final="$TMP_DIR/backend-watchlist-final.txt"
    watchlist_keys="$TMP_DIR/backend-watchlist-keys.txt"
    : >"$actionable_final"
    : >"$actionable_keys"
    : >"$watchlist_final"
    : >"$watchlist_keys"

    while IFS='|' read -r goal line; do
      [[ -z "${goal:-}" || -z "${line:-}" ]] && continue
      key="$goal|$line"
      reason="$(backend_policy_watch_reason "$line" "$spring_boot_major")"
      if [[ -n "$reason" ]]; then
        if ! grep -Fqx "$key" "$watchlist_keys"; then
          printf '%s\n' "$key" >>"$watchlist_keys"
          printf '%s|%s|%s\n' "$goal" "$line" "$reason" >>"$watchlist_final"
        fi
      else
        if ! grep -Fqx "$key" "$actionable_keys"; then
          printf '%s\n' "$key" >>"$actionable_keys"
          printf '%s|%s\n' "$goal" "$line" >>"$actionable_final"
        fi
      fi
    done <"$actionable_raw"

    while IFS='|' read -r goal line; do
      [[ -z "${goal:-}" || -z "${line:-}" ]] && continue
      key="$goal|$line"
      if grep -Fqx "$key" "$actionable_keys" || grep -Fqx "$key" "$watchlist_keys"; then
        continue
      fi

      target_version="$(extract_target_version "$line")"
      reason=""
      if is_prerelease_version "$target_version"; then
        reason="prerelease/preview ignored in actionable section"
      else
        reason="$(backend_policy_watch_reason "$line" "$spring_boot_major")"
        [[ -z "$reason" ]] && reason="informational strategic tracking"
      fi

      printf '%s\n' "$key" >>"$watchlist_keys"
      printf '%s|%s|%s\n' "$goal" "$line" "$reason" >>"$watchlist_final"
    done <"$full_raw"

    print_note "Spring Boot baseline detected: ${spring_boot_parent_version:-unknown} (major ${spring_boot_major:-unknown})"
    print_note "A) Actionable on current branch/policy"
    if [[ ! -s "$actionable_final" ]]; then
      echo "    - none under current policy"
    else
      while IFS='|' read -r goal line; do
        echo "    - [$goal] $line (actionable now)"
      done <"$actionable_final"
    fi

    print_note "B) Strategic watchlist"
    if [[ ! -s "$watchlist_final" ]]; then
      echo "    - none"
    else
      while IFS='|' read -r goal line reason; do
        echo "    - [$goal] $line ($reason)"
      done <"$watchlist_final"
    fi
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
