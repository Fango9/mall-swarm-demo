# S4 Redis 故障与恢复结果

## 1. 演练信息

- 批次：`perf13-20260830-001`
- 入口：`http://127.0.0.1:8088`（Nginx → Gateway → 微服务）
- 故障目标：仅 `mall-redis` 容器
- Redis 数据卷：`mall-swarm_redis-data:/data`
- 未停止或重建 MySQL、Gateway、Portal、Admin、Nacos、RabbitMQ、Elasticsearch，也未删除 Redis 数据卷或清空 Redis 键。
- Gateway 实际限流配置：
    - 公开商品浏览：Redis 令牌桶，IP 维度，`20/s`，burst `40`；
    - 购物车、订单：Redis 令牌桶，MEMBER 维度，`5/s`，burst `10`。

## 2. 故障步骤与真实观察

1. 故障前，商品详情和 MEMBER 购物车读取均经 Nginx 成功：HTTP `200`、业务码 `200`。
2. 故障前快照确认：SKU 30 为 `stock=10`、`lock_stock=10`、MySQL 可预占库存 `0`；本批次有 12 笔订单，其中 10 条预占为 `CONFIRMED`、2 条为 `RELEASED`；Redis 热点库存键值为 `0`。
3. 仅停止 `mall-redis`。Redis 数据卷仍挂载为 `mall-swarm_redis-data:/data`；MySQL 保持 healthy。
4. Redis 停止后，Gateway、Portal、Admin、Monitor 进程仍运行；约数分钟后 Gateway 与 Portal 健康状态变为 `unhealthy`。
5. Redis 故障中，经 Nginx 访问公开商品详情和携带既有 MEMBER Token 的购物车读取接口，两个请求均在 10 秒超时：HTTP `000`、curl exit `28`，未得到统一业务 JSON。
6. Redis 故障中快照确认 SKU 30 的 MySQL 库存、订单数、预占状态均与故障前一致；Redis 热点键查询按容器停止状态记录为 `SKIPPED_REDIS_NOT_RUNNING`。
7. 启动同一个 `mall-redis` 容器后，Redis、Gateway、Portal、Admin、MySQL 均恢复 healthy；Nginx 商品详情入口恢复 HTTP `200`。
8. 恢复后两个只读入口探针均再次成功：HTTP `200`、业务码 `200`。恢复后 MySQL 库存、订单数、预占状态与故障前一致，Redis 热点库存键恢复为 `0`。

## 3. 一致性对账

| 阶段 | Redis | Gateway / Portal | SKU 30：stock / lock_stock / 可预占 | 本批次订单 | 预占状态 |
| --- | --- | --- | --- | ---: | --- |
| 故障前 | healthy，热点键为 0 | healthy | 10 / 10 / 0 | 12 | 10 `CONFIRMED`，2 `RELEASED` |
| Redis 停止后 | exited unhealthy | 最终均 unhealthy | 10 / 10 / 0 | 12 | 10 `CONFIRMED`，2 `RELEASED` |
| Redis 恢复后 | healthy，热点键为 0 | healthy | 10 / 10 / 0 | 12 | 10 `CONFIRMED`，2 `RELEASED` |

本场景未创建订单、购物车项、库存预占或 Outbox，因此没有引入新的业务数据。

## 4. 结论与限制

Redis 完全不可用时，本机当前真实入口不可用：Gateway 的 Redis 令牌桶和 MEMBER 会话均依赖 Redis，公开浏览和已登录购物车读取请求均在 Nginx 入口超时。不能据此环境承诺 Redis 故障下端到端下单仍可用。

Portal 商品 Cache Aside 的代码对 Redis 缓存读写异常会回源 MySQL；但本次请求未能稳定穿过 Gateway Redis 限流路径，因此未获得“Portal 缓存降级已在真实入口生效”的端到端证据。

Admin 热点 SKU 库存过滤的代码在 Redis 异常时会返回 `NOT_APPLIED`，继续由 MySQL 条件更新作最终库存裁决；但由于 Gateway 限流与 MEMBER 会话先受 Redis 故障影响，本次未能从真实入口触发该库存回退路径。该实现能力不能等同于 Redis 故障下端到端下单可用。

Redis 恢复后，入口、健康状态与热点库存键均恢复，且 MySQL 库存、订单和预占记录没有意外变化。

## 5. 原始证据位置

```text

docs/performance/results/perf13-20260830-001/s4-redis/
├── before-state.md
├── before-probes.md
├── during-outage-state.md
├── during-outage-probes.md
├── after-recovery-state.md
└── after-recovery-probes.md

```
