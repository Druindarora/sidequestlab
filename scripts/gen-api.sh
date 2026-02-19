#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WAIT_SCRIPT="$ROOT_DIR/scripts/wait-http.sh"
API_URL="http://localhost:8080/v3/api-docs"

if ! "$WAIT_SCRIPT" "$API_URL"; then
  echo "Backend not reachable at $API_URL." >&2
  echo "Start it first (VS Code \"Sidequestlab - Full Stack (dev)\" or ./scripts/run-backend.sh), then retry." >&2
  exit 1
fi

cd "$ROOT_DIR/frontend"

npm run generate:api
