#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
CONFIG_DIR="${PROJECT_ROOT}/deploy/nacos/dev"

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

declare -a ALL_CONFIGS=(
  "mall-gateway-dev.yml:text"
  "mall-auth-dev.yml:yaml"
  "mall-admin-dev.yml:yaml"
  "mall-portal-dev.yml:yaml"
  "mall-search-dev.yml:yaml"
  "mall-monitor-dev.yml:yaml"
  "mall-demo-dev.yml:text"
)

declare -a CONFIGS=("${ALL_CONFIGS[@]}")
if [[ "$#" == 2 && "$1" == --apply && "$2" == --only-admin ]]; then
  CONFIGS=("mall-admin-dev.yml:yaml")
elif [[ "$#" == 2 && "$1" == --apply && "$2" == --only-gateway ]]; then
  CONFIGS=("mall-gateway-dev.yml:text")
elif [[ "$#" == 2 && "$1" == --apply && "$2" == --only-portal ]]; then
  CONFIGS=("mall-portal-dev.yml:yaml")
elif [[ "$#" == 1 && "$1" == --apply ]]; then
  :
elif [[ "$#" == 0 ]]; then
  echo "Dry run: the following Nacos configurations would be imported:"
  printf '%s\n' "${ALL_CONFIGS[@]%%:*}"
  echo "No Nacos configuration was changed. Re-run with --apply [--only-admin|--only-portal] to import."
  exit 0
else
  echo '用法: import-nacos-dev.sh [--apply [--only-admin|--only-gateway|--only-portal]]' >&2
  exit 2
fi

command -v curl >/dev/null
command -v jq >/dev/null

if [[ -z "${NACOS_USERNAME:-}" || -z "${NACOS_PASSWORD:-}" ]]; then
  echo "NACOS_USERNAME and NACOS_PASSWORD must be set in .env" >&2
  exit 1
fi

login_response="$(
  curl --fail --silent --show-error --max-time 15 \
    --request POST "${NACOS_SERVER_URL}/v3/auth/user/login" \
    --data-urlencode "username=${NACOS_USERNAME}" \
    --data-urlencode "password=${NACOS_PASSWORD}"
)"

access_token="$(jq -r '.accessToken // .data.accessToken // empty' <<<"${login_response}")"

if [[ -z "${access_token}" ]]; then
  echo "Nacos login did not return an access token" >&2
  exit 1
fi

for config in "${CONFIGS[@]}"; do
  file_name="${config%%:*}"
  config_type="${config##*:}"
  file_path="${CONFIG_DIR}/${file_name}"

  if [[ ! -s "${file_path}" ]]; then
    echo "Missing or empty configuration file: ${file_path}" >&2
    exit 1
  fi

  response="$(
    curl --fail --silent --show-error --max-time 15 \
      --request POST "${NACOS_CONSOLE_URL}/v3/console/cs/config" \
      --header "accessToken: ${access_token}" \
      --data-urlencode "dataId=${file_name}" \
      --data-urlencode "groupName=${NACOS_GROUP_NAME}" \
      --data-urlencode "namespaceId=${NACOS_NAMESPACE_ID}" \
      --data-urlencode "type=${config_type}" \
      --data-urlencode "content@${file_path}"
  )"

  if ! jq -e '.code == 0 and .data == true' <<<"${response}" >/dev/null; then
    echo "Nacos import failed for ${file_name}" >&2
    exit 1
  fi

  # 成功响应后再读回内容，防止导错 Data ID/namespace 或只依据本地 YAML 判断。
  published="$(curl --fail --silent --show-error --max-time 15 --get \
    "${NACOS_CONSOLE_URL}/v3/console/cs/config" \
    --header "accessToken: ${access_token}" \
    --data-urlencode "dataId=${file_name}" \
    --data-urlencode "groupName=${NACOS_GROUP_NAME}" \
    --data-urlencode "namespaceId=${NACOS_NAMESPACE_ID}")"
  remote_content="$(jq -er 'select(.code == 0) | .data.content | select(type == "string")' <<< "$published")"
  local_content="$(cat "$file_path")"
  [[ "$remote_content" == "$local_content" ]] || { echo "Nacos readback mismatch: $file_name" >&2; exit 1; }
  echo "Imported and verified ${file_name} namespace=${NACOS_NAMESPACE_ID} group=${NACOS_GROUP_NAME}"
done
