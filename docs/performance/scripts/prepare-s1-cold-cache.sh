#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
PERFORMANCE_ENV_FILE="${PROJECT_ROOT}/docs/performance/.env.performance"
PROJECT_ENV_FILE="${PROJECT_ROOT}/.env"

if [[ ! -f "${PERFORMANCE_ENV_FILE}" || ! -f "${PROJECT_ENV_FILE}" ]]; then
  echo "缺少 .env 或 docs/performance/.env.performance。" >&2
  exit 1
fi

set -a
source "${PROJECT_ENV_FILE}"
source "${PERFORMANCE_ENV_FILE}"
set +a

: "${REDIS_PASSWORD:?缺少 REDIS_PASSWORD}"
: "${PERF13_PRODUCT_CATEGORY_ID:?缺少 PERF13_PRODUCT_CATEGORY_ID}"
: "${PERF13_PRODUCT_ID:?缺少 PERF13_PRODUCT_ID}"

if [[ ! "${PERF13_PRODUCT_CATEGORY_ID}" =~ ^[1-9][0-9]*$ ]] ||
   [[ ! "${PERF13_PRODUCT_ID}" =~ ^[1-9][0-9]*$ ]]; then
  echo "分类 ID 或商品 ID 非法。" >&2
  exit 1
fi

redis_cli() {
  docker exec \
    -e "REDISCLI_AUTH=${REDIS_PASSWORD}" \
    mall-redis \
    redis-cli --raw "$@"
}

category_key="mall:portal:product:categories"
category_list_key="mall:portal:product:list:category:${PERF13_PRODUCT_CATEGORY_ID}"
detail_key="mall:portal:product:detail:${PERF13_PRODUCT_ID}"
detail_null_key="mall:portal:product:detail:null:${PERF13_PRODUCT_ID}"
detail_lock_key="mall:portal:product:detail:lock:${PERF13_PRODUCT_ID}"

lock_exists="$(redis_cli EXISTS "${detail_lock_key}")"
if [[ "${lock_exists}" != "0" ]]; then
  echo "专用商品详情互斥锁存在，停止删除缓存以避免干扰正在进行的缓存重建。" >&2
  exit 1
fi

deleted_count="$(
  redis_cli DEL \
    "${category_key}" \
    "${category_list_key}" \
    "${detail_key}" \
    "${detail_null_key}"
)"

for cache_key in \
  "${category_key}" \
  "${category_list_key}" \
  "${detail_key}" \
  "${detail_null_key}"; do
  exists="$(redis_cli EXISTS "${cache_key}")"

  if [[ "${exists}" != "0" ]]; then
    echo "缓存键删除后仍存在：${cache_key}" >&2
    exit 1
  fi
done

printf '冷缓存准备完成：删除了 %s 个目标缓存键。\\n' "${deleted_count}"