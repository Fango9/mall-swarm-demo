#!/usr/bin/env bash
set -euo pipefail

cache_state="${1:?请传入 cold 或 warm}"

if [[ "$cache_state" != "cold" && "$cache_state" != "warm" ]]; then
  echo "CACHE_STATE 只能是 cold 或 warm。" >&2
  exit 1
fi

project_root="$(cd "$(dirname "$0")/../../.." && pwd)"
cd "$project_root"

set -a
source docs/performance/.env.performance
set +a

if [[ "$PERF13_BASE_URL" != *":8088" ]]; then
  echo "PERF13_BASE_URL 必须指向 Nginx 的 8088 入口，当前不执行压测。" >&2
  exit 1
fi

result_dir="docs/performance/results/${PERF13_BATCH_ID}/s1-product-browse-${cache_state}"

if [[ -e "$result_dir" ]]; then
  echo "结果目录已存在：${result_dir}；为避免覆盖真实结果，本次终止。" >&2
  exit 1
fi

mkdir -p "$result_dir"

k6 run \
  --summary-export "${result_dir}/k6-summary.json" \
  -e "BASE_URL=${PERF13_BASE_URL}" \
  -e "CATEGORY_ID=${PERF13_PRODUCT_CATEGORY_ID}" \
  -e "PRODUCT_ID=${PERF13_PRODUCT_ID}" \
  -e "CACHE_STATE=${cache_state}" \
  -e "VUS=3" \
  -e "DURATION=30s" \
  -e "THINK_TIME_SECONDS=1" \
  docs/performance/scenarios/s1-product-browse.js \
  | tee "${result_dir}/k6-console.txt"