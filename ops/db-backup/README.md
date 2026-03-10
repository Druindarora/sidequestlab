# PostgreSQL Backup Cron Service (Railway)

This service runs a scheduled PostgreSQL backup and uploads the dump to a Railway S3-compatible bucket.

## What this service does

- Runs `pg_dump -Fc --no-owner --no-privileges "$DATABASE_URL"` to a temporary dump file.
- Names the file as `<database>_YYYY-MM-DDTHH-MM-SSZ.dump`.
- Uploads the dump to `s3://$BUCKET/postgres/` using the Railway bucket credentials.
- Keeps only the 5 most recent dump files in `postgres/` and deletes older files.
- Handles retention in-script (bucket lifecycle is intentionally not used).

## Required variables

From PostgreSQL service:

- `DATABASE_URL`

From Railway bucket credentials:

- `ENDPOINT`
- `ACCESS_KEY_ID`
- `SECRET_ACCESS_KEY`
- `BUCKET`

Optional:

- `AWS_DEFAULT_REGION` (defaults to `us-east-1`)

## Railway setup

1. Create a dedicated Railway service for this folder (`ops/db-backup/`).
2. Ensure it builds from the provided `Dockerfile`.
3. Add all required variables listed above.
4. Configure the service as a Cron Job with schedule:
   - `0 2 * * *` (UTC)

## Manual trigger (optional)

From repo root, run:

```bash
cd ops/db-backup
railway run --service <backup-service-name> -- ./backup.sh
```

This runs one backup immediately using the service environment variables.

## Restore principle

1. Download the desired dump from `postgres/` in the bucket.
2. Restore with `pg_restore` into the target database.

Example:

```bash
pg_restore --clean --if-exists --no-owner --no-privileges -d "$DATABASE_URL" ./mydb_2026-03-10T02-00-00Z.dump
```
