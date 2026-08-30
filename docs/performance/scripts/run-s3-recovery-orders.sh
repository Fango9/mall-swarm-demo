#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "$0")/../../.." && pwd)"
cd "$project_root"

set -a
source docs/performance/.env.performance
set +a

if [[ "$PERF13_BASE_URL" != *":8088" ]]; then
  echo "PERF13_BASE_URL 必须指向 Nginx 的 8088 入口。" >&2
  exit 1
fi

rabbit_state="$(docker inspect -f '{{.State.Status}}' mall-rabbitmq)"

if [[ "$rabbit_state" == "running" ]]; then
  echo "RabbitMQ 仍在运行；为避免误做正常链路下单，本次停止。" >&2
  exit 1
fi

result_dir="docs/performance/results/${PERF13_BATCH_ID}/s3-recovery"

if [[ -e "${result_dir}/k6-summary.json" ]]; then
  echo "恢复下单结果已存在；同一批次不可重放下单，停止。" >&2
  exit 1
fi

if [[ ! -f "${result_dir}/before-state.md" ]]; then
  echo "缺少故障前快照，停止。" >&2
  exit 1
fi

k6 run \
  --summary-export "${result_dir}/k6-summary.json" \
  -e "BASE_URL=${PERF13_BASE_URL}" \
  -e "BATCH_ID=${PERF13_BATCH_ID}" \
  -e "MEMBER_PASSWORD=${PERF13_MEMBER_PASSWORD}" \
  -e "PRODUCT_ID=${PERF13_PRODUCT_ID}" \
  -e "RECOVERY_SKU_ID=${PERF13_RECOVERY_SKU_ID}" \
  docs/performance/scenarios/s3-recovery-orders.js \
  | tee "${result_dir}/k6-console.txt"