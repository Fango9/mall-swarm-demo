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
: "${PERF13_BATCH_ID:?缺少 PERF13_BATCH_ID}"
: "${PERF13_SKU_ID:?缺少 PERF13_SKU_ID}"

if [[ ! "$PERF13_BATCH_ID" =~ ^[A-Za-z0-9-]+$ ]]; then
  echo "PERF13_BATCH_ID 格式不安全，终止查询。" >&2
  exit 1
fi

if [[ ! "$PERF13_SKU_ID" =~ ^[0-9]+$ ]]; then
  echo "PERF13_SKU_ID 必须是数字。" >&2
  exit 1
fi

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

container_state() {
  local container_name="$1"

  docker inspect \
    -f '{{.State.Status}} {{if .State.Health}}{{.State.Health.Status}}{{end}}' \
    "$container_name"
}

echo "# S4 Redis 审计快照：${phase}"
echo
echo "- 批次：\`${PERF13_BATCH_ID}\`"
echo "- 时间：\`$(date '+%Y-%m-%d %H:%M:%S %Z')\`"
echo "- 入口：\`${PERF13_BASE_URL}\`"
echo
echo "## 容器状态"
echo
echo '```text'
for container_name in \
  mall-redis \
  mall-swarm-mall-gateway-1 \
  mall-swarm-mall-portal-1 \
  mall-swarm-mall-admin-1 \
  mall-swarm-mall-monitor-1; do
  echo "${container_name}=$(container_state "${container_name}")"
done
echo '```'
echo
echo "## 热点 SKU 的 MySQL 库存"
echo
echo '```text'
mysql_query "
SELECT CONCAT(
  'sku_id=', id,
  ', stock=', stock,
  ', lock_stock=', lock_stock,
  ', mysql_available=', stock - lock_stock
)
FROM pms_sku_stock
WHERE id = ${PERF13_SKU_ID};
"
echo '```'
echo
echo "## 本批次订单与预占汇总"
echo
echo '```text'
mysql_query "
SELECT CONCAT('orders=', COUNT(*))
FROM oms_order
WHERE idempotency_key LIKE '${PERF13_BATCH_ID}%';

SELECT CONCAT(
  'reservation_status=', r.status,
  ', count=', COUNT(*)
)
FROM pms_stock_reservation r
INNER JOIN oms_order o ON o.order_sn = r.reservation_no
WHERE o.idempotency_key LIKE '${PERF13_BATCH_ID}%'
GROUP BY r.status
ORDER BY r.status;
"
echo '```'
echo
echo "## Redis 热点库存键"
echo
echo '```text'
redis_state="$(container_state mall-redis)"
echo "container_state=${redis_state}"

if [[ "$redis_state" == running* ]]; then
  redis_cli=(docker exec mall-redis redis-cli --raw)

  if [[ -n "${REDIS_PASSWORD:-}" ]]; then
    redis_cli+=(--no-auth-warning -a "${REDIS_PASSWORD}")
  fi

  echo "key=mall:admin:hot-sku:available:${PERF13_SKU_ID}"
  echo "value=$("${redis_cli[@]}" GET "mall:admin:hot-sku:available:${PERF13_SKU_ID}")"
else
  echo "value=SKIPPED_REDIS_NOT_RUNNING"
fi
echo '```'