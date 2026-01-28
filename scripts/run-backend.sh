#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [[ -f "$ROOT_DIR/.env.local" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ROOT_DIR/.env.local"
  set +a
fi

echo "Starting backend (Spring Boot) on port 8080..."
cd "$ROOT_DIR/backend"
chmod +x ./mvnw
./mvnw spring-boot:run
