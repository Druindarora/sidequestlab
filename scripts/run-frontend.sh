#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "Starting frontend (Angular) on port 4200..."
cd "$ROOT_DIR/frontend"

npm run start
