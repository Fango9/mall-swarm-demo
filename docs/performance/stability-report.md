# 流程 13：单机容量与稳定性验收报告

## 1. 报告范围

- 批次：`perf13-20260830-001`
- 环境冻结时间：`2026-08-30 15:00:49 CST`
- Git 分支与提交：`13-Pressure-testing` / `86b6404ae487`
- 压测入口：`k6 → Nginx → Gateway → 微服务`
- 工具：k6 `v1.7.1`
- 环境：macOS 26.5.1、Apple M4 Pro（14 个逻辑核）、24 GiB 内存、Docker Engine 29.4.0、Temurin JDK 17.0.19+10。
- 容器资源限制：本次环境未显式设置 CPU 或内存限制。

本报告仅描述上述本机、单节点 Docker Compose 环境在本批次中的真实观测结果；不构成生产容量、高可用性或吞吐承诺。

## 2. 验收总览

| 场景 | 真实结果 | 验收结论 |
| --- | --- | --- |
| S1 商品浏览 | 冷缓存 261/261 成功，8.62 QPS，p95 27.50 ms；热缓存 261/261 成功，8.68 QPS，p95 21.60 ms | 通过 |
| S2 公开浏览 IP 限流 | 241 请求中 120 业务成功、121 Gateway 429；429 统一业务码为 `42901` | 通过 |
| S2 MEMBER 限流 | 12 个专用会员共 240 次购物车读取：120 成功、120 Gateway 429 | 通过 |
| S3 热点库存并发 | 初始库存 10，12 并发下单：10 成功、2 受控预占拒绝；MySQL、Redis、订单、预占、Outbox 与消费记录一致 | 通过 |
| S3-R RabbitMQ 恢复 | RabbitMQ 故障期间订单和 Outbox 持久化；恢复后 Outbox 发布；迟到消息未重新锁库存并进入 DLQ | 通过，有人工处理限制 |
| S4 Redis 故障恢复 | Redis 完全不可用时真实入口超时；恢复后入口与状态恢复，业务数据无变化 | 已记录降级边界 |
| S5 Elasticsearch 故障恢复 | Elasticsearch 故障时搜索 HTTP 500、Monitor 中 Search 为 DOWN；恢复并重启 Search 后索引和搜索恢复 | 通过，有错误响应问题 |
| S6 Demo 服务重启 | 下线时 Nacos 0 实例、Monitor 未注册、入口 503；恢复后 Nacos 1 实例、Monitor UP、入口 200 | 通过 |

## 3. 容量结论

商品浏览在本批次的固定 3 VU、约 8.6 QPS 负载下保持 100% 成功。热缓存相对冷缓存的平均延迟降低约 16.3%，p95 降低约 21.5%，p99 降低约 34.0%。

这些数字仅是当前机器、当前数据量、当前 Docker 资源未限制条件下的端到端基线，不应外推为更高并发、更多商品、更大订单数据量或生产环境的容量结论。

## 4. 热点库存一致性结论

S3 使用初始可售库存为 10 的专用 SKU 30，发起 12 个独立 MEMBER、独立购物车项、独立幂等键的并发下单请求。结果为 10 笔订单创建成功，2 笔返回 Portal 业务码 `54011`“库存预占失败”，没有 Gateway 限流、认证/授权失败或系统错误。

下单后对账结果如下：

| 对账对象 | 实测结果 |
| --- | --- |
| MySQL `stock / lock_stock / 可售库存` | `10 / 10 / 0` |
| Redis 热点可预占库存 | `0` |
| 专用 `PENDING_PAYMENT` 订单与订单明细 | 10 / 10 |
| 库存预占 | 10 条 `CONFIRMED`，无 `LOCKED`、无 `RELEASED` |
| `ORDER_CREATED` Outbox | 10 条 `PUBLISHED`，重试次数合计为 0 |
| Admin 幂等消费记录 | 10 条，与已发布事件一一对应 |

成功数没有超过初始库存。Redis 热点键只是 MySQL `stock - lock_stock` 的快速过滤投影；MySQL 条件更新仍是防超卖的最终裁决。

`54011` 是 Portal 对 Admin 预占异常的统一映射，响应本身不能单独证明一定是库存不足。本次将两个拒绝归因于库存耗尽，依据是下单前后库存、成功订单数、确认预占数、Redis 投影和 Outbox 消费结果的完整对账。

## 5. 故障恢复结论

### 5.1 RabbitMQ

RabbitMQ 停止期间，专用 SKU 31 的两笔订单、两条 `LOCKED` 预占和两条 `PENDING` Outbox 均已持久化。预占在 15 分钟到期后由 5 秒周期任务释放，库存从 `2 / 2 / 0` 恢复为 `2 / 0 / 2`。RabbitMQ 恢复后，两条 Outbox 最终为 `PUBLISHED`。

迟到的 `ORDER_CREATED` 消息尝试确认已经 `RELEASED` 的预占时被拒绝，进入 DLQ；库存没有被重新锁定。DLQ 目前没有自动消费者，需要人工排查和受控处理。该机制是本地事务加 Outbox 的最终一致性，不是分布式强一致事务。

### 5.2 Redis

Redis 完全停止后，Gateway 的 Redis 令牌桶和 MEMBER 会话均受影响；公开商品详情与已登录购物车读取经 Nginx 均在 10 秒超时，没有得到统一业务 JSON。Redis 恢复后，Gateway、Portal、Admin 及两个入口探针恢复正常，MySQL 库存、订单、预占记录与故障前一致。

因此，当前工程不能承诺 Redis 故障时端到端下单仍可用。Portal Cache Aside 回源和 Admin 热点库存 Redis 异常回退 MySQL 的代码路径存在，但本次未取得它们穿过 Gateway 后的端到端验证证据。

### 5.3 Elasticsearch

Elasticsearch 停止后，Monitor 中 `mall-search` 为 `DOWN`，公开搜索经 Nginx → Gateway 返回 HTTP `500`，且为 Spring Boot 默认错误 JSON；商品 MySQL 主数据未发生变化。

恢复 Elasticsearch 并重启 Search 后，`mall_product` 索引文档数恢复为 2，Monitor 中 Search 回到 `UP`，公开关键词搜索再次命中测试商品。当前 Elasticsearch 依赖异常未转换为统一 `CommonResult`，是后续应修复的用户体验问题。

### 5.4 Nacos、Gateway 与 Monitor

Demo 服务停止时，Nacos 中健康/启用实例均为 0，Monitor 为 `NOT_REGISTERED`，Nginx → Gateway → Demo 返回 HTTP `503`。启动同一容器后，Nacos 中健康/启用实例恢复为 1，Monitor 为 `UP`，真实入口恢复 HTTP `200`、业务码 `200`。

该验证仅覆盖无业务写入职责的 Demo 服务，不覆盖订单、库存等有状态服务的重启恢复。

## 6. 已发现问题、未覆盖风险与建议

1. Redis 是当前 Gateway 限流和 MEMBER 会话的关键依赖。Redis 完全故障会使真实入口超时；后续应先明确业务可用性目标，再设计会话、限流与降级策略。
2. Elasticsearch 故障时，搜索接口返回 Spring Boot 默认 HTTP 500，而非统一 `CommonResult`。后续应将基础设施异常转换为明确、可重试且符合统一响应约定的业务错误。
3. RabbitMQ 迟到消息进入 DLQ 后没有自动消费者。后续应建立受控的 DLQ 审计、告警和人工重放流程；不得直接自动重放已释放预占对应的消息。
4. S1 证明了缓存状态与端到端延迟的关联，但没有采集 Portal 缓存命中率、Gateway 限流计数器或 MySQL 指标，不能量化缓存命中带来的数据库压力变化。
5. 本批次只覆盖固定并发、单机 Docker Compose、少量测试商品和会员。未覆盖长时间压测、较大数据量、网络抖动、磁盘满、MySQL 故障、有状态服务重启、跨节点部署或多副本高可用。
6. 所有压测数据均使用 `PERF13-20260830-001` 可识别前缀或专用测试资产；本次没有删除订单、商品、数据库或数据卷。后续清理必须先按 `docs/performance/test-data.md` 的范围核对，再执行受控清理。

## 7. 可重复执行资产

- 测试计划与环境冻结：`docs/performance/test-plan.md`、`docs/performance/environment.md`
- 测试数据边界：`docs/performance/test-data.md`
- k6 场景：`docs/performance/scenarios/`
- 数据准备、执行与状态采集脚本：`docs/performance/scripts/`
- 场景结果：`docs/performance/results/s1-product-browse.md` 至 `docs/performance/results/s6-demo-restart-recovery.md`
- 批次原始摘要：`docs/performance/results/perf13-20260830-001/`

执行前必须重新核对环境冻结信息、Docker 容器状态、Nacos 实际配置与专用测试数据；不得复用过期 Token，不得绕过 Nginx 和 Gateway 直接调用业务服务。

## 8. 构建与自动化测试验证

执行时间：`2026-08-30`。

已在项目根目录执行：

```bash
mvn clean test
```

Surefire 本轮生成 16 份 XML 测试报告，合计 41 个测试，`0 failures`、`0 errors`、`2 skipped`。

跳过的 2 项均属于 `ProductSearchRepositoryIntegrationTest`，其跳过原因是未设置 `RUN_ELASTICSEARCH_INTEGRATION_TESTS`。这不是构建失败；但它意味着 Elasticsearch Repository 的自动化集成测试未纳入本轮 Maven 验证，相关真实环境行为以 S5 故障与恢复演练证据为准。

## 9. 最终结论

在冻结的本机单节点 Docker Compose 环境中，本项目已获得以下真实证据：

- 商品浏览在冷、热缓存基线下均成功，且热缓存端到端延迟更低；
- IP 与 MEMBER 维度的 Gateway Redis 令牌桶均实际返回符合约定的 HTTP 429 和业务码 `42901`；
- 热点 SKU 并发下单没有超卖，MySQL、Redis 投影、订单、库存预占、Outbox 和幂等消费记录完成一致性对账；
- RabbitMQ、Redis、Elasticsearch 三类依赖故障的实际行为及恢复结果均已归档；
- Demo 服务下线和恢复时，已同时验证 Nacos 实例状态、Gateway 真实路由和 Monitor 状态变化。

因此，本批次满足流程 13 的单机工程验收目标；但不应将该结果表述为生产容量或生产高可用承诺。本批次 Maven 全量测试已执行并通过，结果已在本报告第 8 节归档。
