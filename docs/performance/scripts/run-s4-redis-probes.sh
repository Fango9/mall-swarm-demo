#!/usr/bin/env bash
set -euo pipefail

phase="${1:?请传入阶段，例如 during-outage 或 after-recovery}"

if [[ ! "$phase" =~ ^[A-Za-z0-9_-]+$ ]]; then
  echo "阶段只能包含字母、数字、下划线和连字符。" >&2
  exit 1
fi

project_root="$(cd "$(dirname "$0")/../../.." && pwd)"
cd "$project_root"

set -a
source docs/performance/.env.performance
set +a

: "${PERF13_BASE_URL:?缺少 PERF13_BASE_URL}"
: "${PERF13_BATCH_ID:?缺少 PERF13_BATCH_ID}"
: "${PERF13_PRODUCT_ID:?缺少 PERF13_PRODUCT_ID}"

if [[ "$PERF13_BASE_URL" != *":8088" ]]; then
  echo "PERF13_BASE_URL 必须指向 Nginx 的 8088 入口，当前终止。" >&2
  exit 1
fi

if [[ ! "$PERF13_BATCH_ID" =~ ^[A-Za-z0-9-]+$ ]]; then
  echo "PERF13_BATCH_ID 格式不安全，终止。" >&2
  exit 1
fi

if [[ ! "$PERF13_PRODUCT_ID" =~ ^[0-9]+$ ]]; then
  echo "PERF13_PRODUCT_ID 必须是数字。" >&2
  exit 1
fi

result_dir="docs/performance/results/${PERF13_BATCH_ID}/s4-redis"
token_file="${result_dir}/member-token.txt"
result_file="${result_dir}/${phase}-probes.md"

if [[ ! -r "$token_file" ]]; then
  echo "缺少可读的临时 MEMBER Token 文件：${token_file}" >&2
  exit 1
fi

if [[ -e "$result_file" ]]; then
  echo "结果文件已存在：${result_file}；为避免覆盖真实证据，当前终止。" >&2
  exit 1
fi

member_token="$(<"$token_file")"

if [[ -z "$member_token" ]]; then
  echo "临时 MEMBER Token 为空，当前终止。" >&2
  exit 1
fi

probe() {
  local name="$1"
  local url="$2"
  local authorization="$3"
  local response_file
  local http_status
  local curl_exit
  local business_code
  local message

  response_file="$(mktemp)"

  set +e
  if [[ -n "$authorization" ]]; then
    http_status="$(
      curl \
        --silent \
        --show-error \
        --max-time 10 \
        --output "$response_file" \
        --write-out '%{http_code}' \
        --header "Authorization: Bearer ${authorization}" \
        "$url" \
        2>/dev/null
    )"
  else
    http_status="$(
      curl \
        --silent \
        --show-error \
        --max-time 10 \
        --output "$response_file" \
        --write-out '%{http_code}' \
        "$url" \
        2>/dev/null
    )"
  fi
  curl_exit=$?
  set -e

  if jq -e 'type == "object" and has("code")' "$response_file" >/dev/null 2>&1; then
    business_code="$(jq -r '.code' "$response_file")"
    message="$(jq -r '.message // "NULL"' "$response_file" | tr '\n' ' ')"
  else
    business_code="NON_JSON_OR_NO_CODE"
    message="NON_JSON_OR_NO_MESSAGE"
  fi

  rm -f "$response_file"

  echo "- ${name}: http_status=${http_status:-000}, curl_exit=${curl_exit}, business_code=${business_code}, message=${message}"
}

{
  echo "# S4 Redis 请求探针：${phase}"
  echo
  echo "- 批次：\`${PERF13_BATCH_ID}\`"
  echo "- 时间：\`$(date '+%Y-%m-%d %H:%M:%S %Z')\`"
  echo "- 入口：\`${PERF13_BASE_URL}\`"
  echo "- 说明：仅执行两个 GET 请求；不输出 Token，也不执行写操作。"
  echo
  echo "## 结果"
  echo
  probe \
    "public-product-detail" \
    "${PERF13_BASE_URL}/portal/products/${PERF13_PRODUCT_ID}" \
    ""
  probe \
    "member-cart-read" \
    "${PERF13_BASE_URL}/portal/cart/items" \
    "${member_token}"
} > "$result_file"

echo "result_file=${result_file}"