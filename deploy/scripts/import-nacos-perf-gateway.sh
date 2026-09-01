#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
CONFIG_FILE="${PROJECT_ROOT}/deploy/nacos/perf/mall-gateway-perf.yml"
DATA_ID="mall-gateway-perf.yml"
CONFIG_TYPE="text"

ENV_FILE="${PROJECT_ROOT}/.env"

if [[ -f "${ENV_FILE}" ]]; then
  set -a
  source "${ENV_FILE}"
  set +a
fi

NACOS_CONSOLE_URL="${NACOS_CONSOLE_URL:-http://127.0.0.1:8080}"
NACOS_SERVER_URL="${NACOS_SERVER_URL:-http://127.0.0.1:8848/nacos}"
NACOS_NAMESPACE_ID="${NACOS_NAMESPACE_ID:-public}"
NACOS_GROUP_NAME="${NACOS_GROUP_NAME:-DEFAULT_GROUP}"

if [[ "${1:-}" != "--apply" ]]; then
  echo "Dry run: the following Nacos configuration would be imported:"
  echo "${DATA_ID}"
  echo "No Nacos configuration was changed. Re-run with --apply to import."
  exit 0
fi

command -v curl >/dev/null
command -v jq >/dev/null

if [[ ! -s "${CONFIG_FILE}" ]]; then
  echo "Missing or empty configuration file: ${CONFIG_FILE}" >&2
  exit 1
fi

if [[ -z "${NACOS_USERNAME:-}" || -z "${NACOS_PASSWORD:-}" ]]; then
  echo "NACOS_USERNAME and NACOS_PASSWORD must be set in .env" >&2
  exit 1
fi

login_response="$(
  curl --fail-with-body --silent --show-error \
    --request POST "${NACOS_SERVER_URL}/v3/auth/user/login" \
    --data-urlencode "username=${NACOS_USERNAME}" \
    --data-urlencode "password=${NACOS_PASSWORD}"
)"

access_token="$(jq -r '.accessToken // .data.accessToken // empty' <<<"${login_response}")"

if [[ -z "${access_token}" ]]; then
  echo "Nacos login did not return an access token" >&2
  exit 1
fi

response="$(
  curl --fail-with-body --silent --show-error \
    --request POST "${NACOS_CONSOLE_URL}/v3/console/cs/config" \
    --header "accessToken: ${access_token}" \
    --data-urlencode "dataId=${DATA_ID}" \
    --data-urlencode "groupName=${NACOS_GROUP_NAME}" \
    --data-urlencode "namespaceId=${NACOS_NAMESPACE_ID}" \
    --data-urlencode "type=${CONFIG_TYPE}" \
    --data-urlencode "content@${CONFIG_FILE}"
)"

if ! jq -e '.code == 0 and .data == true' <<<"${response}" >/dev/null; then
  echo "Nacos import failed for ${DATA_ID}" >&2
  exit 1
fi

echo "Imported ${DATA_ID}"