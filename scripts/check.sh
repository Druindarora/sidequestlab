#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "== SideQuestLab :: check =="
"$ROOT_DIR/scripts/guardrails.sh"

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
