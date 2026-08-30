#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "$0")/../../.." && pwd)"
cd "$project_root"

set -a
source docs/performance/.env.performance
set +a

if [[ "$PERF13_BASE_URL" != *":8088" ]]; then
  echo "PERF13_BASE_URL 必须指向 Nginx 的 8088 入口，当前不执行压测。" >&2
  exit 1
fi

result_dir="docs/performance/results/${PERF13_BATCH_ID}/s3-hot-stock-concurrency"

if [[ -e "${result_dir}/k6-summary.json" ]]; then
  echo "S3 结果已存在；同一批次不可重放下单，停止以避免产生重复测试订单。" >&2
  exit 1
fi

if [[ ! -f "${result_dir}/before-state.md" ]]; then
  echo "缺少下单前审计快照，停止。" >&2
  exit 1
fi

k6 run \
  --summary-export "${result_dir}/k6-summary.json" \
  -e "BASE_URL=${PERF13_BASE_URL}" \
  -e "BATCH_ID=${PERF13_BATCH_ID}" \
  -e "MEMBER_PASSWORD=${PERF13_MEMBER_PASSWORD}" \
  -e "PRODUCT_ID=${PERF13_PRODUCT_ID}" \
  -e "SKU_ID=${PERF13_SKU_ID}" \
  -e "MEMBER_COUNT=12" \
  -e "EXPECTED_SUCCESS_COUNT=10" \
  docs/performance/scenarios/s3-hot-stock-order.js \
  | tee "${result_dir}/k6-console.txt"