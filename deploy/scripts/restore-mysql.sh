#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
ENV_FILE="${PROJECT_ROOT}/.env"

if [[ $# -ne 2 || "${2}" != "--apply" ]]; then
  echo "Usage: $0 <backup-file.sql.gz> --apply" >&2
  echo "The target database will be dropped and recreated before restore." >&2
  exit 1
fi

BACKUP_FILE="${1}"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Missing .env file: ${ENV_FILE}" >&2
  exit 1
fi

if [[ ! -f "${BACKUP_FILE}" ]]; then
  echo "Backup file does not exist: ${BACKUP_FILE}" >&2
  exit 1
fi

set -a
source "${ENV_FILE}"
set +a

if [[ -z "${MYSQL_ROOT_PASSWORD:-}" || -z "${MYSQL_DATABASE:-}" ]]; then
  echo "MYSQL_ROOT_PASSWORD and MYSQL_DATABASE must be set in .env" >&2
  exit 1
fi

if [[ ! "${MYSQL_DATABASE}" =~ ^[A-Za-z0-9_]+$ ]]; then
  echo "MYSQL_DATABASE may contain only letters, numbers, and underscores" >&2
  exit 1
fi

command -v docker >/dev/null
command -v gzip >/dev/null

gzip -t "${BACKUP_FILE}"

echo "Restoring ${BACKUP_FILE} into database ${MYSQL_DATABASE}"

docker compose -f "${PROJECT_ROOT}/docker-compose.yml" exec -T \
  -e MYSQL_PWD="${MYSQL_ROOT_PASSWORD}" \
  mysql \
  mysql \
  --user=root \
  --execute="DROP DATABASE IF EXISTS \`${MYSQL_DATABASE}\`;"

gzip -dc "${BACKUP_FILE}" |
  docker compose -f "${PROJECT_ROOT}/docker-compose.yml" exec -T \
    -e MYSQL_PWD="${MYSQL_ROOT_PASSWORD}" \
    mysql \
    mysql \
    --user=root

echo "MySQL restore completed: ${MYSQL_DATABASE}"