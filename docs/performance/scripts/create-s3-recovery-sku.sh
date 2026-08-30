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
: "${PERF13_ADMIN_TOKEN:?缺少 PERF13_ADMIN_TOKEN}"

if [[ "$PERF13_BASE_URL" != *":8088" ]]; then
  echo "PERF13_BASE_URL 必须指向 Nginx 的 8088 入口。" >&2
  exit 1
fi

recovery_sku_code="${PERF13_BATCH_ID}-s3-recovery-sku"

sale_attrs="$(
  jq -nc \
    --arg batch_id "${PERF13_BATCH_ID}" \
    '{batch: $batch_id, scenario: "s3-recovery"}'
)"

list_response="$(
  curl --silent --show-error --fail-with-body \
    "${PERF13_BASE_URL}/admin/products/${PERF13_PRODUCT_ID}/skus" \
    --header "Authorization: Bearer ${PERF13_ADMIN_TOKEN}"
)"

if ! jq -e '.code == 200 and (.data | type == "array")' \
  <<<"${list_response}" >/dev/null; then
  jq '{code, message}' <<<"${list_response}" >&2
  exit 1
fi

matching_count="$(
  jq --arg sku_code "${recovery_sku_code}" \
    '[.data[] | select(.skuCode == $sku_code)] | length' \
    <<<"${list_response}"
)"

if [[ "$matching_count" == "0" ]]; then
  create_response="$(
    curl --silent --show-error --fail-with-body \
      --request POST \
      "${PERF13_BASE_URL}/admin/products/${PERF13_PRODUCT_ID}/skus" \
      --header "Authorization: Bearer ${PERF13_ADMIN_TOKEN}" \
      --header "Content-Type: application/json" \
      --data "$(jq -nc \
        --arg sku_code "${recovery_sku_code}" \
        --arg sale_attrs "${sale_attrs}" \
        '{
          skuCode: $sku_code,
          price: 99.00,
          stock: 2,
          pic: null,
          saleAttrs: $sale_attrs
        }')"
  )"

  if ! jq -e '.code == 200 and (.data | type == "number")' \
    <<<"${create_response}" >/dev/null; then
    jq '{code, message}' <<<"${create_response}" >&2
    exit 1
  fi

  recovery_sku_id="$(jq -r '.data' <<<"${create_response}")"
  evidence="POST /admin/products/${PERF13_PRODUCT_ID}/skus 成功"
elif [[ "$matching_count" == "1" ]]; then
  recovery_sku_id="$(
    jq -r --arg sku_code "${recovery_sku_code}" \
      '.data[] | select(.skuCode == $sku_code) | .id' \
      <<<"${list_response}"
  )"
  evidence="SKU 已存在，重复执行后安全复用"
else
  echo "发现多条同编码恢复 SKU，停止以避免误用数据。" >&2
  exit 1
fi

detail_response="$(
  curl --silent --show-error --fail-with-body \
    "${PERF13_BASE_URL}/admin/products/${PERF13_PRODUCT_ID}/skus/${recovery_sku_id}" \
    --header "Authorization: Bearer ${PERF13_ADMIN_TOKEN}"
)"

if ! jq -e \
  --arg sku_code "${recovery_sku_code}" \
  '.code == 200
    and .data.skuCode == $sku_code
    and .data.stock == 2
    and .data.lockStock == 0' \
  <<<"${detail_response}" >/dev/null; then
  jq '{code, message, data}' <<<"${detail_response}" >&2
  exit 1
fi

printf 'recovery_sku_code=%s\n' "${recovery_sku_code}"
printf 'recovery_sku_id=%s\n' "${recovery_sku_id}"
printf 'evidence=%s；Admin SKU 详情已验证 stock=2、lockStock=0\n' "${evidence}"