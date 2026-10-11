# Wathb — Backup and Restore

## Overview

This document covers the database backup procedure for the Wathb production Django/PostgreSQL backend. Run all commands on the production server (or a secure bastion host with PostgreSQL access). Never perform these operations against the development database.

---

## 1. Prerequisites

- `pg_dump` and `psql` installed (comes with PostgreSQL client)
- Access to production `.env` (read DB_NAME, DB_USER, DB_HOST, DB_PORT)
- A secure, off-server backup destination (S3, object storage, encrypted NAS)
- `PGPASSWORD` environment variable or a `.pgpass` file — do not pass passwords on the command line

---

## 2. Creating a Full Backup

```bash
# Set credentials from .env (never echo them to console)
source .env  # or export manually

BACKUP_DIR="/var/backups/wathb"
TIMESTAMP=$(date +%Y%m%dT%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/wathb_${TIMESTAMP}.dump"

mkdir -p "$BACKUP_DIR"

PGPASSWORD="$DB_PASSWORD" pg_dump \
  --host="$DB_HOST" \
  --port="$DB_PORT" \
  --username="$DB_USER" \
  --format=custom \
  --compress=9 \
  --file="$BACKUP_FILE" \
  "$DB_NAME"

echo "Backup written to: $BACKUP_FILE"
```

The `--format=custom` format is compressed and supports selective restoration.

---

## 3. Verifying a Backup

```bash
pg_restore --list "$BACKUP_FILE" | head -20
```

If `pg_restore --list` exits 0 and prints table names, the backup file is structurally valid.

---

## 4. Automated Daily Backups (cron)

Add to root/deploy user crontab (`crontab -e`):

```
0 2 * * * /opt/wathb/scripts/backup.sh >> /var/log/wathb-backup.log 2>&1
```

Rotate and transfer backups off-server daily. Keep at least 7 daily + 4 weekly copies.

---

## 5. Restoring from Backup

**STOP the application server before restoring** to avoid concurrent writes.

```bash
# Stop Gunicorn / uWSGI
sudo systemctl stop wathb-gunicorn

# Drop and recreate the target database
PGPASSWORD="$DB_PASSWORD" psql \
  --host="$DB_HOST" --port="$DB_PORT" \
  --username="$DB_USER" \
  --command="DROP DATABASE IF EXISTS ${DB_NAME}_restore; CREATE DATABASE ${DB_NAME}_restore;"

# Restore
PGPASSWORD="$DB_PASSWORD" pg_restore \
  --host="$DB_HOST" \
  --port="$DB_PORT" \
  --username="$DB_USER" \
  --dbname="${DB_NAME}_restore" \
  --no-owner \
  --role="$DB_USER" \
  "$BACKUP_FILE"

echo "Restore complete. Verify data, then rename or swap databases."
```

**Verify** the restored database with the application before cutting traffic over.

---

## 6. Media Files Backup

User-uploaded payment evidence files live in `MEDIA_ROOT`. Back them up separately:

```bash
MEDIA_BACKUP="/var/backups/wathb/media_${TIMESTAMP}.tar.gz"
tar -czf "$MEDIA_BACKUP" -C "$(dirname $MEDIA_ROOT)" "$(basename $MEDIA_ROOT)"
```

Sync both the `.dump` and `.tar.gz` to off-server storage immediately after creation.

---

## 7. Recovery Time Objective (Pilot)

During the Gaza pilot, target:
- **RPO (Recovery Point Objective):** 24 hours (daily backups)
- **RTO (Recovery Time Objective):** 4 hours

These are conservative targets for a pilot phase. Increase backup frequency to hourly before general availability.

---

## 8. Secret Rotation After a Compromise

If `DJANGO_SECRET_KEY` or database credentials are suspected compromised:

1. Generate a new `DJANGO_SECRET_KEY` (use `python -c "import secrets; print(secrets.token_hex(50))"`)
2. Update `.env` on the production server
3. Restart the application — all existing sessions and JWT tokens will be invalidated
4. Rotate the database password in PostgreSQL: `ALTER USER wathb_user PASSWORD 'new-password';`
5. Update `.env` with the new DB_PASSWORD
6. Restart again
