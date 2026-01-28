#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 || $# -gt 2 ]]; then
  echo "Usage: $0 <url> [timeout_seconds]" >&2
  exit 1
fi

URL="$1"
TIMEOUT_SECONDS="${2:-60}"

START_TS=$(date +%s)

while true; do
  HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$URL" || true)
  if [[ "$HTTP_CODE" =~ ^[0-9]+$ ]] && (( HTTP_CODE >= 200 && HTTP_CODE < 400 )); then
    exit 0
  fi

  NOW_TS=$(date +%s)
  ELAPSED=$((NOW_TS - START_TS))
  if (( ELAPSED >= TIMEOUT_SECONDS )); then
    echo "Timeout waiting for $URL after ${TIMEOUT_SECONDS}s" >&2
    exit 1
  fi

  sleep 1
done
