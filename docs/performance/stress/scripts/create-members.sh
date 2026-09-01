#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
ENV_FILE="${PROJECT_ROOT}/docs/performance/.env.performance"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "缺少 docs/performance/.env.performance。" >&2
  exit 1
fi

set -a
source "${ENV_FILE}"
set +a

: "${PERF14_BASE_URL:?缺少 PERF14_BASE_URL}"
: "${PERF14_BATCH_ID:?缺少 PERF14_BATCH_ID}"
: "${PERF14_MEMBER_PASSWORD:?缺少 PERF14_MEMBER_PASSWORD}"

if [[ "${PERF14_BASE_URL}" != *":8088" ]]; then
  echo "PERF14_BASE_URL 必须指向 Nginx 的 8088 入口。" >&2
  exit 1
fi

command -v curl >/dev/null
command -v jq >/dev/null

printf '| 用户名 | memberId | 角色 | 创建证据 |\n'
printf '| --- | --- | --- | --- |\n'

for sequence in $(seq -w 1 10); do
  username="${PERF14_BATCH_ID}-m${sequence}"

  register_response="$(
    curl --silent --show-error --fail-with-body \
      --request POST "${PERF14_BASE_URL}/auth/register" \
      --header "Content-Type: application/json" \
      --data "$(jq -nc \
        --arg username "${username}" \
        --arg password "${PERF14_MEMBER_PASSWORD}" \
        '{username: $username, password: $password}')"
  )"

  register_code="$(jq -r '.code // empty' <<<"${register_response}")"

  if [[ "${register_code}" == "200" ]]; then
    create_evidence="POST /auth/register 成功"
  elif [[ "${register_code}" == "40013" ]]; then
    create_evidence="账号已存在，重复执行后复用"
  else
    jq '{code, message}' <<<"${register_response}" >&2
    exit 1
  fi

  login_response="$(
    curl --silent --show-error --fail-with-body \
      --request POST "${PERF14_BASE_URL}/auth/login" \
      --header "Content-Type: application/json" \
      --data "$(jq -nc \
        --arg username "${username}" \
        --arg password "${PERF14_MEMBER_PASSWORD}" \
        '{username: $username, password: $password}')"
  )"

  if ! jq -e \
    --arg username "${username}" \
    '.code == 200 and .data.username == $username and .data.role == "MEMBER" and (.data.memberId | type == "number")' \
    <<<"${login_response}" >/dev/null; then
    jq '{code, message}' <<<"${login_response}" >&2
    exit 1
  fi

  member_id="$(jq -r '.data.memberId' <<<"${login_response}")"

  printf '| `%s` | `%s` | `MEMBER` | %s；`POST /auth/login` 已验证 |\n' \
    "${username}" \
    "${member_id}" \
    "${create_evidence}"
done