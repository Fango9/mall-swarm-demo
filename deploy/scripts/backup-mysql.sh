#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
ENV_FILE="${PROJECT_ROOT}/.env"
BACKUP_DIR="${1:-${PROJECT_ROOT}/backups/mysql}"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Missing .env file: ${ENV_FILE}" >&2
  exit 1
fi

set -a
source "${ENV_FILE}"
set +a

if [[ -z "${MYSQL_ROOT_PASSWORD:-}" || -z "${MYSQL_DATABASE:-}" ]]; then
  echo "MYSQL_ROOT_PASSWORD and MYSQL_DATABASE must be set in .env" >&2
  exit 1
fi

command -v docker >/dev/null
command -v gzip >/dev/null

mkdir -p "${BACKUP_DIR}"

timestamp="$(date +%Y%m%d-%H%M%S)"
backup_file="${BACKUP_DIR}/${MYSQL_DATABASE}-${timestamp}.sql.gz"
temporary_file="$(mktemp "${BACKUP_DIR}/.${MYSQL_DATABASE}-${timestamp}.XXXXXX")"

cleanup() {
  rm -f "${temporary_file}"
}

trap cleanup ERR

docker compose -f "${PROJECT_ROOT}/docker-compose.yml" exec -T \
  -e MYSQL_PWD="${MYSQL_ROOT_PASSWORD}" \
  mysql \
  mysqldump \
  --single-transaction \
  --routines \
  --events \
  --triggers \
  --set-gtid-purged=OFF \
  --databases "${MYSQL_DATABASE}" |
  gzip -c > "${temporary_file}"

mv "${temporary_file}" "${backup_file}"
trap - ERR

echo "MySQL backup created: ${backup_file}"