#!/usr/bin/env bash
set -Eeuo pipefail

PREFIX="postgres/"
KEEP_COUNT=5

required_vars=(
  DATABASE_URL
  ENDPOINT
  ACCESS_KEY_ID
  SECRET_ACCESS_KEY
  BUCKET
)

for var_name in "${required_vars[@]}"; do
  if [[ -z "${!var_name:-}" ]]; then
    echo "ERROR: missing required environment variable: ${var_name}" >&2
    exit 1
  fi
done

export AWS_ACCESS_KEY_ID="$ACCESS_KEY_ID"
export AWS_SECRET_ACCESS_KEY="$SECRET_ACCESS_KEY"
export AWS_DEFAULT_REGION="${AWS_DEFAULT_REGION:-us-east-1}"
export AWS_PAGER=""

db_name="${DATABASE_URL##*/}"
db_name="${db_name%%\?*}"

if [[ -z "$db_name" ]]; then
  echo "ERROR: could not determine database name from DATABASE_URL" >&2
  exit 1
fi

timestamp="$(date -u +"%Y-%m-%dT%H-%M-%SZ")"
filename="${db_name}_${timestamp}.dump"
dump_path="/tmp/${filename}"
object_key="${PREFIX}${filename}"

cleanup() {
  rm -f "$dump_path"
}
trap cleanup EXIT

on_error() {
  local exit_code=$?
  echo "Backup failed (exit ${exit_code})" >&2
  exit "$exit_code"
}
trap on_error ERR

echo "Starting PostgreSQL backup for database: ${db_name}"
pg_dump -Fc --no-owner --no-privileges "$DATABASE_URL" -f "$dump_path"

echo "Uploading backup to s3://${BUCKET}/${object_key}"
aws s3 cp "$dump_path" "s3://${BUCKET}/${object_key}" --endpoint-url "$ENDPOINT"

echo "Applying retention policy: keeping ${KEEP_COUNT} most recent backups in ${PREFIX}"
keys_raw="$(
  aws s3api list-objects-v2 \
    --bucket "$BUCKET" \
    --prefix "$PREFIX" \
    --endpoint-url "$ENDPOINT" \
    --query 'sort_by(Contents,&LastModified)[].Key' \
    --output text
)"

filtered_keys="$(printf '%s\n' "$keys_raw" | tr '\t' '\n' | sed '/^$/d;/^None$/d')"
keys=()
if [[ -n "$filtered_keys" ]]; then
  mapfile -t keys <<<"$filtered_keys"
fi

if (( ${#keys[@]} > KEEP_COUNT )); then
  delete_count=$(( ${#keys[@]} - KEEP_COUNT ))
  for ((i=0; i<delete_count; i++)); do
    old_key="${keys[$i]}"
    echo "Deleting old backup: ${old_key}"
    aws s3 rm "s3://${BUCKET}/${old_key}" --endpoint-url "$ENDPOINT"
  done
fi

echo "Backup completed successfully: ${object_key}"
exit 0
