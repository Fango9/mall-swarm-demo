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

result_dir="docs/performance/results/${PERF13_BATCH_ID}/s2-member-rate-limit"

if [[ -e "$result_dir" ]]; then
  echo "结果目录已存在：${result_dir}；为避免覆盖真实结果，本次终止。" >&2
  exit 1
fi

mkdir -p "$result_dir"

k6 run \
  --summary-export "${result_dir}/k6-summary.json" \
  -e "BASE_URL=${PERF13_BASE_URL}" \
  -e "BATCH_ID=${PERF13_BATCH_ID}" \
  -e "MEMBER_PASSWORD=${PERF13_MEMBER_PASSWORD}" \
  -e "MEMBER_COUNT=12" \
  -e "REQUESTS_PER_MEMBER=20" \
  docs/performance/scenarios/s2-member-rate-limit.js \
  | tee "${result_dir}/k6-console.txt"