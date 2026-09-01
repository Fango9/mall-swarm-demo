#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
ENV_FILE="${PROJECT_ROOT}/.env"
RESULTS_ROOT="${PROJECT_ROOT}/docs/performance/stress/results"

result_dir="${1:?用法: bash collect-jvm-metrics.sh <result-dir> <duration-seconds> [interval-seconds]}"
duration_seconds="${2:?用法: bash collect-jvm-metrics.sh <result-dir> <duration-seconds> [interval-seconds]}"
interval_seconds="${3:-5}"

if [[ ! "${result_dir}" =~ ^${RESULTS_ROOT}/stress-[0-9]{8}-[0-9]{6}/ ]]; then
  echo "result-dir 必须位于 ${RESULTS_ROOT}/stress-YYYYMMDD-HHMMSS/ 下。" >&2
  exit 1
fi

if [[ ! -d "${result_dir}" ]]; then
  echo "结果目录不存在：${result_dir}" >&2
  exit 1
fi

if [[ ! "${duration_seconds}" =~ ^[1-9][0-9]*$ ]]; then
  echo "duration-seconds 必须是大于 0 的整数。" >&2
  exit 1
fi

if [[ ! "${interval_seconds}" =~ ^[1-9][0-9]*$ ]]; then
  echo "interval-seconds 必须是大于 0 的整数。" >&2
  exit 1
fi

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "缺少 .env。" >&2
  exit 1
fi

set -a
source "${ENV_FILE}"
set +a

: "${ACTUATOR_USERNAME:?缺少 ACTUATOR_USERNAME}"
: "${ACTUATOR_PASSWORD:?缺少 ACTUATOR_PASSWORD}"

command -v docker >/dev/null
command -v jq >/dev/null

services=(
  "gateway|mall-swarm-mall-gateway-1|8200"
  "portal|mall-swarm-mall-portal-1|8204"
  "admin|mall-swarm-mall-admin-1|8202"
  "search|mall-swarm-mall-search-1|8205"
)

metrics=(
  "jvm.memory.used"
  "jvm.gc.pause"
  "jvm.threads.live"
  "http.server.requests"
)

for service_spec in "${services[@]}"; do
  IFS='|' read -r service_name container_name actuator_port <<< "${service_spec}"

  if ! docker inspect "${container_name}" >/dev/null 2>&1; then
    echo "缺少目标容器：${container_name}" >&2
    exit 1
  fi
done

metrics_file="${result_dir}/jvm-metrics.ndjson"
printf '' > "${metrics_file}"

elapsed_seconds=0

while (( elapsed_seconds < duration_seconds )); do
  timestamp="$(date '+%Y-%m-%dT%H:%M:%S%z')"

  for service_spec in "${services[@]}"; do
    IFS='|' read -r service_name container_name actuator_port <<< "${service_spec}"

    for metric_name in "${metrics[@]}"; do
      if response="$(
        docker exec "${container_name}" \
          curl --silent --show-error --fail \
          --user "${ACTUATOR_USERNAME}:${ACTUATOR_PASSWORD}" \
          "http://127.0.0.1:${actuator_port}/actuator/metrics/${metric_name}"
      )"; then
        jq -cn \
          --arg timestamp "${timestamp}" \
          --arg service "${service_name}" \
          --arg metric "${metric_name}" \
          --argjson payload "${response}" \
          '{timestamp: $timestamp, service: $service, metric: $metric, payload: $payload}' \
          >> "${metrics_file}"
      else
        jq -cn \
          --arg timestamp "${timestamp}" \
          --arg service "${service_name}" \
          --arg metric "${metric_name}" \
          '{timestamp: $timestamp, service: $service, metric: $metric, error: "ACTUATOR_QUERY_FAILED"}' \
          >> "${metrics_file}"
      fi
    done
  done

  sleep "${interval_seconds}"
  elapsed_seconds=$((elapsed_seconds + interval_seconds))
done