#!/usr/bin/env bash
set -u

print_label() {
  printf '%-14s %s\n' "$1" "$2"
}

print_cmd_version() {
  local label="$1"
  local cmd="$2"
  local args="$3"

  if command -v "$cmd" >/dev/null 2>&1; then
    # shellcheck disable=SC2086
    print_label "$label" "$($cmd $args 2>&1 | head -n 1)"
  else
    print_label "$label" "not found"
  fi
}

print_label "OS" "$(uname -a)"
print_cmd_version "git" "git" "--version"
print_cmd_version "node" "node" "--version"
print_cmd_version "npm" "npm" "--version"
print_cmd_version "java" "java" "-version"

printf '\nUseful commands:\n'
printf '  - %s\n' "./scripts/check.sh"
printf '  - %s\n' "cd frontend && npm ci"
printf '  - %s\n' "cd backend && ./mvnw -B test -Dspring.profiles.active=test"

exit 0
