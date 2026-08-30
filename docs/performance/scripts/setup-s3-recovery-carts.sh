#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "$0")/../../.." && pwd)"
cd "$project_root"

set -a
source docs/performance/.env.performance
set +a

: "${PERF13_BASE_URL:?缺少 PERF13_BASE_URL}"
: "${PERF13_BATCH_ID:?缺少 PERF13_BATCH_ID}"
: "${PERF13_PRODUCT_ID:?缺少 PERF13_PRODUCT_ID}"
: "${PERF13_RECOVERY_SKU_ID:?缺少 PERF13_RECOVERY_SKU_ID}"
: "${PERF13_MEMBER_PASSWORD:?缺少 PERF13_MEMBER_PASSWORD}"

if [[ "$PERF13_BASE_URL" != *":8088" ]]; then
  echo "PERF13_BASE_URL 必须指向 Nginx 的 8088 入口。" >&2
  exit 1
fi

printf '| 用户名 | cartItemId | 数量 | 准备证据 |\n'
printf '| --- | ---: | ---: | --- |\n'

for member_suffix in 01 02; do
  username="${PERF13_BATCH_ID}-m${member_suffix}"

  login_response="$(
    curl --silent --show-error --fail-with-body \
      --request POST \
      "${PERF13_BASE_URL}/auth/login" \
      --header "Content-Type: application/json" \
      --data "$(jq -nc \
        --arg username "${username}" \
        --arg password "${PERF13_MEMBER_PASSWORD}" \
        '{username: $username, password: $password}')"
  )"

  if ! jq -e \
    --arg username "${username}" \
    '.code == 200
      and .data.username == $username
      and .data.role == "MEMBER"
      and (.data.token | type == "string")' \
    <<<"${login_response}" >/dev/null; then
    jq '{code, message}' <<<"${login_response}" >&2
    exit 1
  fi

  token="$(jq -r '.data.token' <<<"${login_response}")"

  cart_response="$(
    curl --silent --show-error --fail-with-body \
      "${PERF13_BASE_URL}/portal/cart/items" \
      --header "Authorization: Bearer ${token}"
  )"

  if ! jq -e '.code == 200 and (.data | type == "array")' \
    <<<"${cart_response}" >/dev/null; then
    jq '{code, message}' <<<"${cart_response}" >&2
    exit 1
  fi

  matched_count="$(
    jq --argjson sku_id "${PERF13_RECOVERY_SKU_ID}" \
      '[.data[] | select(.skuId == $sku_id)] | length' \
      <<<"${cart_response}"
  )"

  if [[ "$matched_count" == "0" ]]; then
    add_response="$(
      curl --silent --show-error --fail-with-body \
        --request POST \
        "${PERF13_BASE_URL}/portal/cart/items" \
        --header "Authorization: Bearer ${token}" \
        --header "Content-Type: application/json" \
        --data "$(jq -nc \
          --argjson product_id "${PERF13_PRODUCT_ID}" \
          --argjson sku_id "${PERF13_RECOVERY_SKU_ID}" \
          '{productId: $product_id, skuId: $sku_id, quantity: 1}')"
    )"

    if ! jq -e '.code == 200 and (.data | type == "number")' \
      <<<"${add_response}" >/dev/null; then
      jq '{code, message}' <<<"${add_response}" >&2
      exit 1
    fi

    evidence="POST /portal/cart/items 成功"
  elif [[ "$matched_count" == "1" ]]; then
    evidence="已有恢复专用购物车项，安全复用"
  else
    echo "${username} 存在多条恢复 SKU 购物车项，停止。" >&2
    exit 1
  fi

  verify_response="$(
    curl --silent --show-error --fail-with-body \
      "${PERF13_BASE_URL}/portal/cart/items" \
      --header "Authorization: Bearer ${token}"
  )"

  if ! jq -e \
    --argjson product_id "${PERF13_PRODUCT_ID}" \
    --argjson sku_id "${PERF13_RECOVERY_SKU_ID}" \
    '[.data[]
      | select(
          .productId == $product_id
          and .skuId == $sku_id
          and .quantity == 1
        )
    ] | length == 1' \
    <<<"${verify_response}" >/dev/null; then
    jq '{code, message, data}' <<<"${verify_response}" >&2
    exit 1
  fi

  cart_item_id="$(
    jq -r --argjson sku_id "${PERF13_RECOVERY_SKU_ID}" \
      '.data[] | select(.skuId == $sku_id) | .id' \
      <<<"${verify_response}"
  )"

  printf '| `%s` | `%s` | `1` | %s；查询已验证 |\n' \
    "${username}" \
    "${cart_item_id}" \
    "${evidence}"
done