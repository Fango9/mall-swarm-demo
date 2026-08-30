#!/usr/bin/env bash
set -euo pipefail

phase="${1:?请传入审计阶段，例如 before、during-restart 或 after-recovery}"

if [[ ! "$phase" =~ ^[A-Za-z0-9_-]+$ ]]; then
  echo "审计阶段只能包含字母、数字、下划线和连字符。" >&2
  exit 1
fi

project_root="$(cd "$(dirname "$0")/../../.." && pwd)"
cd "$project_root"

set -a
source .env
source docs/performance/.env.performance
set +a

: "${NACOS_USERNAME:?缺少 NACOS_USERNAME}"
: "${NACOS_PASSWORD:?缺少 NACOS_PASSWORD}"
: "${MONITOR_USERNAME:?缺少 MONITOR_USERNAME}"
: "${MONITOR_PASSWORD:?缺少 MONITOR_PASSWORD}"
: "${PERF13_BASE_URL:?缺少 PERF13_BASE_URL}"
: "${PERF13_BATCH_ID:?缺少 PERF13_BATCH_ID}"

if [[ "$PERF13_BASE_URL" != *":8088" ]]; then
  echo "PERF13_BASE_URL 必须指向 Nginx 的 8088 入口。" >&2
  exit 1
fi

container_state() {
  docker inspect \
    -f '{{.State.Status}} {{if .State.Health}}{{.State.Health.Status}}{{end}}' \
    "$1"
}

nacos_demo_instance_counts() {
  local login_response
  local access_token
  local response_file
  local http_status
  local curl_exit
  local instance_response
  local nacos_code

  login_response="$(
    curl --silent --show-error --fail-with-body --max-time 10 \
      --request POST \
      "${NACOS_SERVER_URL:-http://127.0.0.1:8848/nacos}/v3/auth/user/login" \
      --data-urlencode "username=${NACOS_USERNAME}" \
      --data-urlencode "password=${NACOS_PASSWORD}"
  )"

  access_token="$(jq -r '.accessToken // .data.accessToken // empty' <<< "$login_response")"

  if [[ -z "$access_token" ]]; then
    echo "nacos_demo_instances=AUTH_FAILED"
    return
  fi

  response_file="$(mktemp)"

  set +e
  http_status="$(
    curl --silent --show-error --max-time 10 \
      --output "$response_file" \
      --write-out '%{http_code}' \
      --get \
      "${NACOS_CONSOLE_URL:-http://127.0.0.1:8080}/v3/console/ns/instance/list" \
      --header "accessToken: ${access_token}" \
      --data-urlencode "pageNo=1" \
      --data-urlencode "pageSize=10" \
      --data-urlencode "serviceName=mall-demo" \
      --data-urlencode "groupName=DEFAULT_GROUP" \
      --data-urlencode "namespaceId=public"
  )"
  curl_exit=$?
  set -e

  if [[ "$curl_exit" -ne 0 ]]; then
    rm -f "$response_file"
    echo "nacos_demo_instances=QUERY_FAILED"
    return
  fi

  if [[ "$http_status" == "404" ]]; then
    rm -f "$response_file"
    echo "nacos_demo_healthy_instances=0"
    echo "nacos_demo_enabled_instances=0"
    return
  fi

  if [[ "$http_status" != "200" ]]; then
    rm -f "$response_file"
    echo "nacos_demo_instances=QUERY_FAILED"
    return
  fi

  instance_response="$(<"$response_file")"
  rm -f "$response_file"

  nacos_code="$(jq -r '.code // empty' <<< "$instance_response")"

  if [[ "$nacos_code" != "0" ]]; then
    echo "nacos_demo_instances=QUERY_FAILED"
    return
  fi

  jq -r '
    "nacos_demo_healthy_instances="
      + ([.data.pageItems[]? | select(.healthy == true)] | length | tostring),
    "nacos_demo_enabled_instances="
      + ([.data.pageItems[]? | select(.enabled == true)] | length | tostring)
  ' <<< "$instance_response"
}

monitor_demo_status() {
  local response

  response="$(
    docker exec \
      -e PERF13_MONITOR_USERNAME="${MONITOR_USERNAME}" \
      -e PERF13_MONITOR_PASSWORD="${MONITOR_PASSWORD}" \
      mall-swarm-mall-monitor-1 \
      sh -c '
        curl --silent --show-error --max-time 10 \
          --user "$PERF13_MONITOR_USERNAME:$PERF13_MONITOR_PASSWORD" \
          --header "Accept: application/json" \
          http://127.0.0.1:8206/monitor/instances
      '
  )"

  jq -r '
    [.[] | select(.registration.name == "mall-demo") | .statusInfo.status]
    | if length == 0 then "monitor_demo_status=NOT_REGISTERED"
      else "monitor_demo_status=" + join(",")
      end
  ' <<< "$response"
}

gateway_demo_probe() {
  local response_file
  local http_status
  local curl_exit

  response_file="$(mktemp)"

  set +e
  http_status="$(
    curl --silent --show-error --max-time 10 \
      --output "$response_file" \
      --write-out '%{http_code}' \
      "${PERF13_BASE_URL}/demo/ping"
  )"
  curl_exit=$?
  set -e

  echo "gateway_demo_http_status=${http_status:-000}"
  echo "gateway_demo_curl_exit=${curl_exit}"

  if jq -e 'type == "object"' "$response_file" >/dev/null 2>&1; then
    jq -r '
      "gateway_demo_business_code=" + (.code | tostring),
      "gateway_demo_service=" + (.data.service // "NULL"),
      "gateway_demo_status=" + (.data.status // "NULL")
    ' "$response_file"
  else
    echo "gateway_demo_business_code=NON_JSON_OR_EMPTY"
  fi

  rm -f "$response_file"
}

echo "# S6 Demo 服务重启审计快照：${phase}"
echo
echo "- 批次：\`${PERF13_BATCH_ID}\`"
echo "- 时间：\`$(date '+%Y-%m-%d %H:%M:%S %Z')\`"
echo
echo "## 容器、Nacos 与 Monitor"
echo
echo '```text'
echo "mall-swarm-mall-demo-1=$(container_state mall-swarm-mall-demo-1)"
echo "mall-swarm-mall-gateway-1=$(container_state mall-swarm-mall-gateway-1)"
echo "mall-swarm-mall-monitor-1=$(container_state mall-swarm-mall-monitor-1)"
echo "mall-nacos=$(container_state mall-nacos)"
nacos_demo_instance_counts
monitor_demo_status
echo '```'
echo
echo "## Nginx → Gateway → Demo"
echo
echo '```text'
gateway_demo_probe
echo '```'