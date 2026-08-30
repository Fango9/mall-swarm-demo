#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
ENV_FILE="${PROJECT_ROOT}/docs/performance/.env.performance"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "缺少 docs/performance/.env.performance。" >&2
  exit 1
fi

set -a
source "${ENV_FILE}"
set +a

: "${PERF13_BASE_URL:?缺少 PERF13_BASE_URL}"
: "${PERF13_BATCH_ID:?缺少 PERF13_BATCH_ID}"
: "${PERF13_MEMBER_PASSWORD:?缺少 PERF13_MEMBER_PASSWORD}"

printf '| 用户名 | memberId | 角色 | 创建证据 |\n'
printf '| --- | --- | --- | --- |\n'

for sequence in $(seq -w 1 12); do
  username="${PERF13_BATCH_ID}-m${sequence}"

  register_response="$(
    curl --silent --show-error --fail-with-body \
      --request POST "${PERF13_BASE_URL}/auth/register" \
      --header "Content-Type: application/json" \
      --data "$(jq -nc \
        --arg username "${username}" \
        --arg password "${PERF13_MEMBER_PASSWORD}" \
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
      --request POST "${PERF13_BASE_URL}/auth/login" \
      --header "Content-Type: application/json" \
      --data "$(jq -nc \
        --arg username "${username}" \
        --arg password "${PERF13_MEMBER_PASSWORD}" \
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