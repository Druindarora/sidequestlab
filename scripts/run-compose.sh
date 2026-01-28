#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "Starting docker compose dev stack in detached mode..."

docker compose -f "$ROOT_DIR/docker-compose.dev.yml" up -d
