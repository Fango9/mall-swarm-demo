#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
ENV_FILE="${PROJECT_ROOT}/.env"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "缺少 .env，无法读取 Nacos 登录配置。" >&2
  exit 1
fi

set -a
source "${ENV_FILE}"
set +a

: "${NACOS_USERNAME:?缺少 NACOS_USERNAME}"
: "${NACOS_PASSWORD:?缺少 NACOS_PASSWORD}"

NACOS_CONSOLE_URL="${NACOS_CONSOLE_URL:-http://127.0.0.1:8080}"
NACOS_SERVER_URL="${NACOS_SERVER_URL:-http://127.0.0.1:8848/nacos}"
NACOS_NAMESPACE_ID="${NACOS_NAMESPACE_ID:-public}"
NACOS_GROUP_NAME="${NACOS_GROUP_NAME:-DEFAULT_GROUP}"

CONFIGS=(
  "mall-gateway-dev.yml"
  "mall-auth-dev.yml"
  "mall-admin-dev.yml"
  "mall-portal-dev.yml"
  "mall-search-dev.yml"
  "mall-monitor-dev.yml"
  "mall-demo-dev.yml"
)

login_response="$(
  curl --fail-with-body --silent --show-error \
    --request POST "${NACOS_SERVER_URL}/v3/auth/user/login" \
    --data-urlencode "username=${NACOS_USERNAME}" \
    --data-urlencode "password=${NACOS_PASSWORD}"
)"

access_token="$(jq -r '.accessToken // .data.accessToken // empty' <<<"${login_response}")"

if [[ -z "${access_token}" ]]; then
  echo "Nacos 登录未返回 accessToken。" >&2
  exit 1
fi

format_modify_time() {
  local modify_time_ms="$1"
  date -r "$((modify_time_ms / 1000))" '+%Y-%m-%d %H:%M:%S %Z'
}

printf '| Data ID | Group | 修改时间 | MD5 |\n'
printf '| --- | --- | --- | --- |\n'

for data_id in "${CONFIGS[@]}"; do
  response="$(
    curl --fail-with-body --silent --show-error \
      --get "${NACOS_CONSOLE_URL}/v3/console/cs/config" \
      --header "accessToken: ${access_token}" \
      --data-urlencode "dataId=${data_id}" \
      --data-urlencode "groupName=${NACOS_GROUP_NAME}" \
      --data-urlencode "namespaceId=${NACOS_NAMESPACE_ID}"
  )"

  data_id_result="$(jq -r '.data.dataId // empty' <<<"${response}")"
  modify_time="$(jq -r '.data.modifyTime // empty' <<<"${response}")"
  md5="$(jq -r '.data.md5 // empty' <<<"${response}")"

  if [[ -z "${data_id_result}" || -z "${modify_time}" || -z "${md5}" ]]; then
    echo "无法读取 ${data_id} 的 Nacos 元数据。" >&2
    exit 1
  fi

  printf '| `%s` | `%s` | %s | `%s` |\n' \
    "${data_id_result}" \
    "${NACOS_GROUP_NAME}" \
    "$(format_modify_time "${modify_time}")" \
    "${md5}"
done