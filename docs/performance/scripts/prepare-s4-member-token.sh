#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "$0")/../../.." && pwd)"
cd "$project_root"

set -a
source docs/performance/.env.performance
set +a

: "${PERF13_BASE_URL:?缺少 PERF13_BASE_URL}"
: "${PERF13_BATCH_ID:?缺少 PERF13_BATCH_ID}"
: "${PERF13_MEMBER_PASSWORD:?缺少 PERF13_MEMBER_PASSWORD}"

if [[ "$PERF13_BASE_URL" != *":8088" ]]; then
  echo "PERF13_BASE_URL 必须指向 Nginx 的 8088 入口，当前终止。" >&2
  exit 1
fi

if [[ ! "$PERF13_BATCH_ID" =~ ^[A-Za-z0-9-]+$ ]]; then
  echo "PERF13_BATCH_ID 格式不安全，终止。" >&2
  exit 1
fi

result_dir="docs/performance/results/${PERF13_BATCH_ID}/s4-redis"
token_file="${result_dir}/member-token.txt"
username="${PERF13_BATCH_ID}-m01"

if [[ -e "$token_file" ]]; then
  echo "Token 文件已存在：${token_file}；为避免覆盖，当前终止。" >&2
  exit 1
fi

mkdir -p "$result_dir"

login_response="$(
  curl --fail-with-body --silent --show-error \
    --max-time 10 \
    --request POST \
    --header 'Content-Type: application/json' \
    --data "$(jq -nc \
      --arg username "$username" \
      --arg password "$PERF13_MEMBER_PASSWORD" \
      '{username: $username, password: $password}')" \
    "${PERF13_BASE_URL}/auth/login"
)"

token="$(jq -er \
  --arg username "$username" \
  '
    select(
      .code == 200
      and .data.username == $username
      and .data.role == "MEMBER"
      and (.data.token | type == "string" and length > 0)
    )
    | .data.token
  ' <<< "$login_response")"

umask 077
printf '%s\n' "$token" > "$token_file"

echo "member_username=${username}"
echo "token_file=${token_file}"
echo "token_saved=yes"