#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

# By default, forbid ANY modifications in generated OpenAPI client folder.
# If you intentionally regenerated it, run:
#   ALLOW_GENERATED_API_CHANGES=1 ./scripts/check.sh
ALLOW="${ALLOW_GENERATED_API_CHANGES:-0}"
PATTERN='^frontend/src/app/api/'

# Get modified files (staged + unstaged) compared to HEAD
CHANGED_FILES="$(
  { git diff --name-only --diff-filter=ACMRTUXB HEAD || true; \
    git diff --name-only --cached --diff-filter=ACMRTUXB HEAD || true; } \
  | sort -u
)"

if echo "$CHANGED_FILES" | grep -qE "$PATTERN"; then
  if [[ "$ALLOW" != "1" ]]; then
    echo "❌ Guardrail: modifications detected in generated OpenAPI client:"
    echo ""
    echo "$CHANGED_FILES" | grep -E "$PATTERN" || true
    echo ""
    echo "This folder is generated and must not be edited manually:"
    echo "  frontend/src/app/api/**"
    echo ""
    echo "If you intentionally regenerated it, rerun with:"
    echo "  ALLOW_GENERATED_API_CHANGES=1 ./scripts/check.sh"
    exit 2
  else
    echo "⚠️ Guardrail override: generated API changes allowed (ALLOW_GENERATED_API_CHANGES=1)."
  fi
fi
