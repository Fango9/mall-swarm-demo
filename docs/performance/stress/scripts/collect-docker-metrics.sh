#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
RESULTS_ROOT="${PROJECT_ROOT}/docs/performance/stress/results"

result_dir="${1:?用法: bash collect-docker-metrics.sh <result-dir> <duration-seconds> [interval-seconds]}"
duration_seconds="${2:?用法: bash collect-docker-metrics.sh <result-dir> <duration-seconds> [interval-seconds]}"
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

command -v docker >/dev/null

containers=(
  mall-mysql
  mall-redis
  mall-nacos
  mall-rabbitmq
  mall-elasticsearch
  mall-swarm-mall-nginx-1
  mall-swarm-mall-gateway-1
  mall-swarm-mall-auth-1
  mall-swarm-mall-admin-1
  mall-swarm-mall-portal-1
  mall-swarm-mall-search-1
  mall-swarm-mall-demo-1
  mall-swarm-mall-monitor-1
)

for container in "${containers[@]}"; do
  if ! docker inspect "${container}" >/dev/null 2>&1; then
    echo "缺少目标容器：${container}" >&2
    exit 1
  fi
done

stats_file="${result_dir}/docker-stats.csv"
limits_file="${result_dir}/docker-resource-limits.txt"
start_status_file="${result_dir}/docker-ps-start.txt"
end_status_file="${result_dir}/docker-ps-end.txt"

printf 'timestamp,name,cpu_percent,memory_usage,memory_percent,network_io,block_io,pids\n' \
  > "${stats_file}"

docker ps --format '{{.Names}}\t{{.Status}}\t{{.Image}}' \
  > "${start_status_file}"

for container in "${containers[@]}"; do
  docker inspect \
    --format '{{.Name}} cpus={{.HostConfig.NanoCpus}} memory={{.HostConfig.Memory}} memory_reservation={{.HostConfig.MemoryReservation}}' \
    "${container}"
done > "${limits_file}"

elapsed_seconds=0

while (( elapsed_seconds < duration_seconds )); do
  timestamp="$(date '+%Y-%m-%dT%H:%M:%S%z')"

  docker stats --no-stream \
    --format '{{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}\t{{.NetIO}}\t{{.BlockIO}}\t{{.PIDs}}' \
    "${containers[@]}" \
    | awk -F '\t' -v timestamp="${timestamp}" \
      '{print timestamp "," $1 "," $2 "," $3 "," $4 "," $5 "," $6 "," $7}' \
    >> "${stats_file}"

  sleep "${interval_seconds}"
  elapsed_seconds=$((elapsed_seconds + interval_seconds))
done

docker ps --format '{{.Names}}\t{{.Status}}\t{{.Image}}' \
  > "${end_status_file}"