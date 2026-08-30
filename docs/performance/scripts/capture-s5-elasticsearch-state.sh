#!/usr/bin/env bash
set -euo pipefail

phase="${1:?请传入审计阶段，例如 before、during-outage 或 after-recovery}"

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

: "${MYSQL_ROOT_PASSWORD:?缺少 MYSQL_ROOT_PASSWORD}"
: "${MYSQL_DATABASE:?缺少 MYSQL_DATABASE}"
: "${MONITOR_USERNAME:?缺少 MONITOR_USERNAME}"
: "${MONITOR_PASSWORD:?缺少 MONITOR_PASSWORD}"
: "${PERF13_BATCH_ID:?缺少 PERF13_BATCH_ID}"
: "${PERF13_PRODUCT_ID:?缺少 PERF13_PRODUCT_ID}"

if [[ ! "$PERF13_BATCH_ID" =~ ^[A-Za-z0-9-]+$ ]]; then
  echo "PERF13_BATCH_ID 格式不安全，终止查询。" >&2
  exit 1
fi

if [[ ! "$PERF13_PRODUCT_ID" =~ ^[0-9]+$ ]]; then
  echo "PERF13_PRODUCT_ID 必须是数字。" >&2
  exit 1
fi

container_state() {
  docker inspect \
    -f '{{.State.Status}} {{if .State.Health}}{{.State.Health.Status}}{{end}}' \
    "$1"
}

mysql_query() {
  docker exec \
    -e MYSQL_PWD="${MYSQL_ROOT_PASSWORD}" \
    mall-mysql \
    mysql \
    --batch \
    --raw \
    --skip-column-names \
    -uroot \
    "${MYSQL_DATABASE}" \
    -e "$1"
}

monitor_search_status() {
  local response
  local exit_code

  set +e
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
  exit_code=$?
  set -e

  if [[ "$exit_code" -ne 0 ]]; then
    echo "monitor_search_status=QUERY_FAILED"
    return
  fi

  jq -r '
    [.[] | select(.registration.name == "mall-search") | .statusInfo.status]
    | if length == 0 then "NOT_REGISTERED" else join(",") end
  ' <<< "$response" \
    | sed 's/^/monitor_search_status=/'
}

echo "# S5 Elasticsearch 审计快照：${phase}"
echo
echo "- 批次：\`${PERF13_BATCH_ID}\`"
echo "- 时间：\`$(date '+%Y-%m-%d %H:%M:%S %Z')\`"
echo
echo "## 容器与 Monitor 状态"
echo
echo '```text'
echo "mall-elasticsearch=$(container_state mall-elasticsearch)"
echo "mall-swarm-mall-search-1=$(container_state mall-swarm-mall-search-1)"
echo "mall-swarm-mall-monitor-1=$(container_state mall-swarm-mall-monitor-1)"
monitor_search_status
echo '```'
echo
echo "## Elasticsearch 派生索引"
echo
echo '```text'
elasticsearch_state="$(container_state mall-elasticsearch)"

if [[ "$elasticsearch_state" == running* ]]; then
  set +e
  cluster_health="$(
    curl --silent --show-error --max-time 10 \
      'http://127.0.0.1:9200/_cluster/health/mall_product?filter_path=status,number_of_nodes,active_shards,unassigned_shards'
  )"
  cluster_exit=$?

  document_count="$(
    curl --silent --show-error --max-time 10 \
      'http://127.0.0.1:9200/mall_product/_count?filter_path=count'
  )"
  count_exit=$?
  set -e

  echo "cluster_health_exit=${cluster_exit}"
  echo "cluster_health=${cluster_health:-EMPTY}"
  echo "document_count_exit=${count_exit}"
  echo "document_count=${document_count:-EMPTY}"
else
  echo "cluster_health=SKIPPED_ELASTICSEARCH_NOT_RUNNING"
  echo "document_count=SKIPPED_ELASTICSEARCH_NOT_RUNNING"
fi
echo '```'
echo
echo "## 测试商品 MySQL 主数据"
echo
echo '```text'
mysql_query "
SELECT CONCAT(
  'product_id=', id,
  ', product_name=', name,
  ', product_sn=', product_sn,
  ', publish_status=', publish_status
)
FROM pms_product
WHERE id = ${PERF13_PRODUCT_ID};
"
echo '```'