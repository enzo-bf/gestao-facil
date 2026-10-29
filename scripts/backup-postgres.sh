#!/usr/bin/env bash
set -euo pipefail
umask 077

backup_dir="${BACKUP_DIR:-./backups}"
retention_days="${BACKUP_RETENTION_DAYS:-14}"
timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
backup_file="${backup_dir}/gestao-facil-${timestamp}.dump"

if ! [[ "$retention_days" =~ ^[0-9]+$ ]] || [ "$retention_days" -lt 1 ]; then
  echo "BACKUP_RETENTION_DAYS must be a positive integer." >&2
  exit 2
fi

mkdir -p "$backup_dir"
chmod 700 "$backup_dir"
trap 'status=$?; if [ "$status" -ne 0 ]; then rm -f -- "$backup_file" "${backup_file}.sha256"; fi; exit "$status"' EXIT

docker compose -f docker-compose.prod.yml exec -T postgres \
  sh -c 'pg_dump --format=custom --no-owner --username="$POSTGRES_USER" --dbname="$POSTGRES_DB"' \
  > "$backup_file"
sha256sum "$backup_file" > "${backup_file}.sha256"
find "$backup_dir" -type f -name 'gestao-facil-*.dump*' -mtime "+${retention_days}" -delete

echo "Database backup created: $backup_file"
