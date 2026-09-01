#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
ENV_FILE="${PROJECT_ROOT}/docs/performance/.env.performance"

target_rps="${1:?用法: bash run-p1-product-detail.sh <rps> <duration> <run-id>}"
duration="${2:?用法: bash run-p1-product-detail.sh <rps> <duration> <run-id>}"
run_id="${3:?用法: bash run-p1-product-detail.sh <rps> <duration> <run-id>}"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "缺少 docs/performance/.env.performance。" >&2
  exit 1
fi

set -a
source "${ENV_FILE}"
set +a

: "${PERF14_BASE_URL:?缺少 PERF14_BASE_URL}"
: "${PERF14_BATCH_ID:?缺少 PERF14_BATCH_ID}"
: "${PERF14_CATALOG_PRODUCT_ID:?缺少 PERF14_CATALOG_PRODUCT_ID}"

if [[ "${PERF14_BASE_URL}" != *":8088" ]]; then
  echo "PERF14_BASE_URL 必须指向 Nginx 的 8088 入口。" >&2
  exit 1
fi

if [[ ! "${target_rps}" =~ ^[1-9][0-9]*$ ]]; then
  echo "rps 必须是大于 0 的整数。" >&2
  exit 1
fi

if [[ ! "${duration}" =~ ^[1-9][0-9]*[smh]$ ]]; then
  echo "duration 必须是如 5m、10m 或 30s 的正时间段。" >&2
  exit 1
fi

if [[ ! "${run_id}" =~ ^stress-[0-9]{8}-[0-9]{6}$ ]]; then
  echo "run-id 必须是 stress-YYYYMMDD-HHMMSS。" >&2
  exit 1
fi

duration_to_seconds() {
  local duration_value="$1"
  local number="${duration_value%?}"
  local unit="${duration_value: -1}"

  case "${unit}" in
    s) echo "${number}" ;;
    m) echo $((number * 60)) ;;
    h) echo $((number * 3600)) ;;
    *)
      echo "不支持的时间单位：${unit}" >&2
      exit 1
      ;;
  esac
}

duration_seconds="$(duration_to_seconds "${duration}")"
metrics_duration_seconds=$((duration_seconds + 30))

command -v curl >/dev/null
command -v jq >/dev/null
command -v k6 >/dev/null

result_dir="${PROJECT_ROOT}/docs/performance/stress/results/${run_id}/p1-product-detail-${target_rps}rps"

if [[ -e "${result_dir}" ]]; then
  echo "结果目录已存在：${result_dir}；为避免覆盖真实结果，本次终止。" >&2
  exit 1
fi

# 单次请求预热专用商品详情缓存；所有请求仍经过 Nginx 和 Gateway。
prewarm_response="$(
  curl --silent --show-error --fail-with-body \
    "${PERF14_BASE_URL}/portal/products/${PERF14_CATALOG_PRODUCT_ID}"
)"

if ! jq -e \
  --argjson product_id "${PERF14_CATALOG_PRODUCT_ID}" \
  '.code == 200 and .data.id == $product_id' \
  <<<"${prewarm_response}" >/dev/null; then
  jq '{code, message}' <<<"${prewarm_response}" >&2
  exit 1
fi

mkdir -p "${result_dir}"

jq '{code, message, productId: .data.id, productName: .data.name}' \
  <<<"${prewarm_response}" \
  > "${result_dir}/prewarm.json"

{
  echo "git_commit=$(git -C "${PROJECT_ROOT}" rev-parse HEAD)"
  echo "k6_version=$(k6 version)"
  echo "target_rps=${target_rps}"
  echo "duration=${duration}"
  echo "product_id=${PERF14_CATALOG_PRODUCT_ID}"
  echo "base_url=${PERF14_BASE_URL}"
} > "${result_dir}/run-metadata.txt"

bash "${PROJECT_ROOT}/docs/performance/scripts/capture-nacos-metadata.sh" \
  > "${result_dir}/nacos-metadata.md"

set +e
bash "${PROJECT_ROOT}/docs/performance/stress/scripts/collect-docker-metrics.sh" \
  "${result_dir}" \
  "${metrics_duration_seconds}" \
  "${METRICS_INTERVAL_SECONDS:-5}" \
  > "${result_dir}/docker-metrics-console.txt" 2>&1 &

docker_metrics_pid=$!

bash "${PROJECT_ROOT}/docs/performance/stress/scripts/collect-jvm-metrics.sh" \
  "${result_dir}" \
  "${metrics_duration_seconds}" \
  "${METRICS_INTERVAL_SECONDS:-5}" \
  > "${result_dir}/jvm-metrics-console.txt" 2>&1 &

jvm_metrics_pid=$!

bash "${PROJECT_ROOT}/docs/performance/stress/scripts/collect-mysql-redis-metrics.sh" \
  "${result_dir}" \
  "${metrics_duration_seconds}" \
  "${METRICS_INTERVAL_SECONDS:-5}" \
  > "${result_dir}/mysql-redis-metrics-console.txt" 2>&1 &

mysql_redis_metrics_pid=$!

bash "${PROJECT_ROOT}/docs/performance/stress/scripts/collect-rabbitmq-elasticsearch-metrics.sh" \
  "${result_dir}" \
  "${metrics_duration_seconds}" \
  "${METRICS_INTERVAL_SECONDS:-5}" \
  > "${result_dir}/rabbitmq-elasticsearch-metrics-console.txt" 2>&1 &

rabbitmq_elasticsearch_metrics_pid=$!

k6 run \
  --summary-export "${result_dir}/k6-summary.json" \
  --tag "run_id=${run_id}" \
  -e "BASE_URL=${PERF14_BASE_URL}" \
  -e "PRODUCT_ID=${PERF14_CATALOG_PRODUCT_ID}" \
  -e "TARGET_RPS=${target_rps}" \
  -e "DURATION=${duration}" \
  -e "PRE_ALLOCATED_VUS=${PRE_ALLOCATED_VUS:-10}" \
  -e "MAX_VUS=${MAX_VUS:-100}" \
  "${PROJECT_ROOT}/docs/performance/stress/scenarios/p1-product-detail.js" \
  | tee "${result_dir}/k6-console.txt"

k6_exit="${PIPESTATUS[0]}"

wait "${docker_metrics_pid}"
docker_metrics_exit="$?"

wait "${jvm_metrics_pid}"
jvm_metrics_exit="$?"

wait "${mysql_redis_metrics_pid}"
mysql_redis_metrics_exit="$?"

wait "${rabbitmq_elasticsearch_metrics_pid}"
rabbitmq_elasticsearch_metrics_exit="$?"
set -e

if (( docker_metrics_exit != 0 )); then
  echo "Docker 指标采集失败，请保留结果目录并排查。" >&2
  exit "${docker_metrics_exit}"
fi

if (( jvm_metrics_exit != 0 )); then
  echo "JVM 指标采集失败，请保留结果目录并排查。" >&2
  exit "${jvm_metrics_exit}"
fi

if (( mysql_redis_metrics_exit != 0 )); then
  echo "MySQL/Redis 指标采集失败，请保留结果目录并排查。" >&2
  exit "${mysql_redis_metrics_exit}"
fi

if (( rabbitmq_elasticsearch_metrics_exit != 0 )); then
  echo "RabbitMQ/Elasticsearch 指标采集失败，请保留结果目录并排查。" >&2
  exit "${rabbitmq_elasticsearch_metrics_exit}"
fi

if (( k6_exit != 0 )); then
  echo "k6 未通过阈值或执行失败，请保留结果目录并排查。" >&2
  exit "${k6_exit}"
fi
