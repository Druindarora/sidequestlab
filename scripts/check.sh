#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "== SideQuestLab :: check =="
"$ROOT_DIR/scripts/guardrails.sh"

# ---------- Flyway migration guard ----------
MIGRATION_DIR="backend/src/main/resources/db/migration"

if [[ -d "$ROOT_DIR/$MIGRATION_DIR" ]]; then
  BASE_REF=""
  BASE_REF_REASON=""
  if git rev-parse --verify --quiet "origin/dev^{commit}" >/dev/null; then
    BASE_REF="origin/dev"
    BASE_REF_REASON="using remote tracking branch"
  elif git rev-parse --verify --quiet "dev^{commit}" >/dev/null; then
    BASE_REF="dev"
    BASE_REF_REASON="origin/dev missing; using local dev branch"
  elif git rev-parse --verify --quiet "HEAD~1^{commit}" >/dev/null; then
    BASE_REF="HEAD~1"
    BASE_REF_REASON="origin/dev and dev missing; using previous commit fallback"
  else
    BASE_REF="HEAD"
    BASE_REF_REASON="origin/dev and dev missing; repository has a single commit"
  fi

  echo ""
  echo "== Flyway guard :: base ref '$BASE_REF' ($BASE_REF_REASON) =="

  declare -A BASE_MIGRATIONS=()
  declare -A NEW_MIGRATIONS=()
  declare -A BLOCKED_MIGRATIONS=()
  declare -A INVALID_NEW_MIGRATIONS=()

  while IFS= read -r path; do
    [[ -z "$path" ]] && continue
    BASE_MIGRATIONS["$path"]=1
  done < <(git ls-tree -r --name-only "$BASE_REF" -- "$MIGRATION_DIR" 2>/dev/null || true)

  while IFS=$'\t' read -r status path1 path2; do
    [[ -z "${status:-}" ]] && continue

    case "$status" in
      A*)
        if [[ "$path1" == *.sql ]] && [[ -z "${BASE_MIGRATIONS[$path1]:-}" ]]; then
          NEW_MIGRATIONS["$path1"]=1
        fi
        ;;
      M*|D*|T*|U*|X*|B*)
        if [[ "$path1" == *.sql ]] && [[ -n "${BASE_MIGRATIONS[$path1]:-}" ]]; then
          BLOCKED_MIGRATIONS["$path1"]=1
        fi
        ;;
      R*|C*)
        if [[ "$path1" == *.sql ]] && [[ -n "${BASE_MIGRATIONS[$path1]:-}" ]]; then
          BLOCKED_MIGRATIONS["$path1"]=1
        fi
        if [[ "$path2" == *.sql ]] && [[ -n "${BASE_MIGRATIONS[$path2]:-}" ]]; then
          BLOCKED_MIGRATIONS["$path2"]=1
        fi
        ;;
    esac
  done < <(git diff --name-status --find-renames "$BASE_REF" -- "$MIGRATION_DIR" || true)

  while IFS= read -r path; do
    [[ -z "$path" ]] && continue
    if [[ "$path" == *.sql ]] && [[ -z "${BASE_MIGRATIONS[$path]:-}" ]]; then
      NEW_MIGRATIONS["$path"]=1
    fi
  done < <(git ls-files --others --exclude-standard -- "$MIGRATION_DIR" || true)

  while IFS= read -r path; do
    [[ -z "$path" ]] && continue
    filename="$(basename "$path")"
    if [[ ! "$filename" =~ ^V[0-9]+__[a-z0-9_]+\.sql$ ]]; then
      INVALID_NEW_MIGRATIONS["$path"]=1
    fi
  done < <(printf '%s\n' "${!NEW_MIGRATIONS[@]}" | sort)

  if (( ${#INVALID_NEW_MIGRATIONS[@]} > 0 )); then
    echo "❌ Flyway guard: invalid new migration filename(s)."
    echo "Expected pattern used in this repo: V<version>__<description>.sql"
    printf '%s\n' "${!INVALID_NEW_MIGRATIONS[@]}" | sort | sed 's/^/  - /'
    exit 2
  fi

  if (( ${#BLOCKED_MIGRATIONS[@]} > 0 )); then
    echo "❌ Flyway guard: historical migrations are immutable."
    echo "Modified or deleted migration files already present on '$BASE_REF':"
    printf '%s\n' "${!BLOCKED_MIGRATIONS[@]}" | sort | sed 's/^/  - /'
    echo "Create a new migration file instead of editing/removing existing ones."
    exit 2
  fi

  if (( ${#NEW_MIGRATIONS[@]} > 0 )); then
    echo ""
    echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
    echo "!! WARNING: NEW FLYWAY MIGRATION FILES DETECTED (NON-BLOCKING)"
    echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
    printf '%s\n' "${!NEW_MIGRATIONS[@]}" | sort | sed 's/^/  - /'
    echo ""
    echo "Reminder:"
    echo "  - Manual backup is required before any DB-related deploy."
    echo "  - Deploy only after all checks are green."
    echo "  - Rollback principle: restore backup + redeploy matching app version."
    echo "Runbook: doc/runbooks/flyway-deploy-guard.md"
    echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
  fi
else
  echo ""
  echo "== Flyway guard :: migration directory not found, skipping =="
fi

# ---------- Backend (Maven) ----------
echo ""
echo "== Backend :: mvn test (profile=test) =="
pushd "$ROOT_DIR/backend" >/dev/null

chmod +x ./mvnw
./mvnw -B test -Dspring.profiles.active=test

echo ""
echo "== Backend :: mvn package (skipTests) =="
./mvnw -B -DskipTests package

popd >/dev/null

# ---------- Frontend (Angular) ----------
echo ""
echo "== Frontend :: npm ci =="
pushd "$ROOT_DIR/frontend" >/dev/null

npm ci

echo ""
echo "== Frontend :: lint =="
# CI logic: if script exists use it, else fallback to ng lint
if npm run | grep -qE '^\s*lint\b'; then
  npm run lint
else
  npx ng lint
fi

echo ""
echo "== Frontend :: build =="
# CI logic: if script exists use it, else fallback to ng build production
if npm run | grep -qE '^\s*build\b'; then
  npm run build --if-present
else
  npx ng build --configuration production
fi

echo ""
echo "== Frontend :: test (if script exists) =="
# CI logic: only run if test script exists
if npm run | grep -qE '^\s*test\b'; then
  npm test -- --watch=false
else
  echo "No test script, skipping"
fi

popd >/dev/null

echo ""
echo "✅ check OK"
