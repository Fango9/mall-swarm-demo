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

gateway_metrics=(
  "p5.gateway.order.duration"
)

gateway_timed_stages=(
  "p5.gateway.order.downstream.duration|request_to_response_headers"
  "p5.gateway.order.downstream.duration|request_to_response_complete"
)

portal_metrics=(
  "p5.portal.order.duration"
  "p5.portal.order.stock_reservation.duration"
  "p5.portal.order.local_transaction.duration"
  "hikaricp.connections.active"
  "hikaricp.connections.idle"
  "hikaricp.connections.pending"
  "hikaricp.connections.max"
  "hikaricp.connections.timeout"
)

portal_timed_stages=(
  "p5.portal.order.stage.duration|current_member"
  "p5.portal.order.stage.duration|request_validation"
  "p5.portal.order.stage.duration|idempotency_lookup"
  "p5.portal.order.stage.duration|cart_load"
  "p5.portal.order.stage.duration|reservation_request_build"
  "p5.portal.order.stage.duration|hot_reservation_http"
  "p5.portal.order.stage.duration|local_transaction_call"
  "p5.portal.order.stage.duration|failure_event_lock_select"
  "p5.portal.order.stage.duration|order_insert"
  "p5.portal.order.stage.duration|order_item_insert"
  "p5.portal.order.stage.duration|outbox_payload_serialize"
  "p5.portal.order.stage.duration|outbox_insert"
  "p5.portal.order.stage.duration|cart_delete"
  "p5.portal.order.stage.duration|order_detail_select"
  "p5.portal.order.stage.duration|order_items_select"
  "p5.portal.order.jdbc.duration|tx_connection_acquire"
  "p5.portal.order.jdbc.duration|failure_event_lock_select"
  "p5.portal.order.jdbc.duration|idempotency_lookup"
  "p5.portal.order.jdbc.duration|cart_load"
  "p5.portal.order.jdbc.duration|order_insert"
  "p5.portal.order.jdbc.duration|order_item_insert"
  "p5.portal.order.jdbc.duration|outbox_insert"
  "p5.portal.order.jdbc.duration|cart_delete"
  "p5.portal.order.jdbc.duration|order_detail_select"
  "p5.portal.order.jdbc.duration|order_items_select"
  "p5.portal.order.jdbc.duration|tx_commit"
  "p5.portal.order.jdbc.duration|tx_rollback"
  "p5.portal.order.jdbc.duration|unattributed_sql"
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

    service_metrics=("${metrics[@]}")
    if [[ "${service_name}" == "gateway" ]]; then
      service_metrics+=("${gateway_metrics[@]}")
    elif [[ "${service_name}" == "portal" ]]; then
      service_metrics+=("${portal_metrics[@]}")
    fi

    for metric_name in "${service_metrics[@]}"; do
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

    if [[ "${service_name}" == "portal" ]]; then
      for timed_metric in "${portal_timed_stages[@]}"; do
        IFS='|' read -r metric_name stage <<< "${timed_metric}"
        if response="$(
          docker exec "${container_name}" \
            curl --silent --show-error --fail \
            --user "${ACTUATOR_USERNAME}:${ACTUATOR_PASSWORD}" \
            "http://127.0.0.1:${actuator_port}/actuator/metrics/${metric_name}?tag=stage:${stage}"
        )"; then
          jq -cn \
            --arg timestamp "${timestamp}" \
            --arg service "${service_name}" \
            --arg metric "${metric_name}" \
            --arg stage "${stage}" \
            --argjson payload "${response}" \
            '{timestamp: $timestamp, service: $service, metric: $metric, stage: $stage, payload: $payload}' \
            >> "${metrics_file}"
        else
          jq -cn \
            --arg timestamp "${timestamp}" \
            --arg service "${service_name}" \
            --arg metric "${metric_name}" \
            --arg stage "${stage}" \
            '{timestamp: $timestamp, service: $service, metric: $metric, stage: $stage, error: "ACTUATOR_QUERY_FAILED"}' \
            >> "${metrics_file}"
        fi
      done
    fi

    if [[ "${service_name}" == "gateway" ]]; then
      for timed_metric in "${gateway_timed_stages[@]}"; do
        IFS='|' read -r metric_name stage <<< "${timed_metric}"
        if response="$(
          docker exec "${container_name}" \
            curl --silent --show-error --fail \
            --user "${ACTUATOR_USERNAME}:${ACTUATOR_PASSWORD}" \
            "http://127.0.0.1:${actuator_port}/actuator/metrics/${metric_name}?tag=stage:${stage}"
        )"; then
          jq -cn \
            --arg timestamp "${timestamp}" \
            --arg service "${service_name}" \
            --arg metric "${metric_name}" \
            --arg stage "${stage}" \
            --argjson payload "${response}" \
            '{timestamp: $timestamp, service: $service, metric: $metric, stage: $stage, payload: $payload}' \
            >> "${metrics_file}"
        else
          jq -cn \
            --arg timestamp "${timestamp}" \
            --arg service "${service_name}" \
            --arg metric "${metric_name}" \
            --arg stage "${stage}" \
            '{timestamp: $timestamp, service: $service, metric: $metric, stage: $stage, error: "ACTUATOR_QUERY_FAILED"}' \
            >> "${metrics_file}"
        fi
      done
    fi
  done

  sleep "${interval_seconds}"
  elapsed_seconds=$((elapsed_seconds + interval_seconds))
done
