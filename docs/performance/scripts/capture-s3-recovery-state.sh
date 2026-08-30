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
: "${PERF13_RECOVERY_SKU_ID:?缺少 PERF13_RECOVERY_SKU_ID}"

if [[ ! "$PERF13_BATCH_ID" =~ ^[A-Za-z0-9-]+$ ]]; then
  echo "PERF13_BATCH_ID 格式不安全，终止查询。" >&2
  exit 1
fi

if [[ ! "$PERF13_RECOVERY_SKU_ID" =~ ^[0-9]+$ ]]; then
  echo "PERF13_RECOVERY_SKU_ID 必须是数字。" >&2
  exit 1
fi

idempotency_prefix="${PERF13_BATCH_ID}-s3r-"

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

echo "# S3-R 审计快照：${phase}"
echo
echo "- 批次：\`${PERF13_BATCH_ID}\`"
echo "- 幂等键前缀：\`${idempotency_prefix}\`"
echo "- 时间：\`$(date '+%Y-%m-%d %H:%M:%S %Z')\`"
echo
echo "## 恢复专用 SKU"
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
WHERE id = ${PERF13_RECOVERY_SKU_ID};
"
echo '```'
echo
echo "## 专用订单与预占"
echo
echo '```text'
mysql_query "
SELECT CONCAT(
  'order_id=', o.id,
  ', order_sn=', o.order_sn,
  ', member_id=', o.member_id,
  ', idempotency_key=', o.idempotency_key,
  ', order_status=', o.status,
  ', reservation_status=', COALESCE(r.status, 'NULL'),
  ', expire_at=', COALESCE(DATE_FORMAT(r.expire_at, '%Y-%m-%d %H:%i:%s'), 'NULL')
)
FROM oms_order o
LEFT JOIN pms_stock_reservation r ON r.reservation_no = o.order_sn
WHERE o.idempotency_key LIKE '${idempotency_prefix}%'
ORDER BY o.id;
"
echo '```'
echo
echo "## Outbox 与幂等消费"
echo
echo '```text'
mysql_query "
SELECT CONCAT(
  'event_id=', e.event_id,
  ', order_id=', e.aggregate_id,
  ', outbox_status=', e.status,
  ', retry_count=', e.retry_count,
  ', published_at=', COALESCE(DATE_FORMAT(e.published_at, '%Y-%m-%d %H:%i:%s'), 'NULL'),
  ', consumer=', COALESCE(c.consumer, 'NULL')
)
FROM oms_outbox_event e
INNER JOIN oms_order o ON o.id = e.aggregate_id
LEFT JOIN pms_event_consume_log c ON c.event_id = e.event_id
WHERE e.aggregate_type = 'ORDER'
  AND e.event_type = 'ORDER_CREATED'
  AND o.idempotency_key LIKE '${idempotency_prefix}%'
ORDER BY e.id;
"
echo '```'
echo
echo "## RabbitMQ 状态与相关队列"
echo
echo '```text'
rabbit_state="$(docker inspect -f '{{.State.Status}} {{if .State.Health}}{{.State.Health.Status}}{{end}}' mall-rabbitmq)"
echo "container_state=${rabbit_state}"

if [[ "$rabbit_state" == running* ]]; then
  docker exec mall-rabbitmq rabbitmqctl list_queues \
    name \
    messages \
    messages_ready \
    messages_unacknowledged \
    | awk '
      NR == 1
      || $1 == "mall.order.created.queue"
      || $1 == "mall.order.created.dlq"
    '
else
  echo "queue_query=SKIPPED_RABBITMQ_NOT_RUNNING"
fi
echo '```'