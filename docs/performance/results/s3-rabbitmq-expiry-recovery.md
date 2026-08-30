# S3-R RabbitMQ 故障与预占超时释放结果

## 1. 演练信息

- 批次：`perf13-20260830-001`
- 入口：`http://127.0.0.1:8088`（Nginx → Gateway → Portal）
- 恢复专用 SKU：`31`，初始 `stock=2`、`lock_stock=0`
- 恢复专用会员：m01、m02；各自一条 SKU 31 购物车项，数量均为 1
- 预占过期策略：15 分钟
- 过期扫描周期：5 秒
- Outbox 扫描周期：5 秒
- RabbitMQ 容器：`mall-rabbitmq`
- 未停止 MySQL、Redis、Nacos、Gateway 或任何数据卷

## 2. 故障步骤与真实观察

1. 故障前快照确认：SKU 31 为 `2 / 0 / 2`，无恢复订单、预占或 Outbox，订单主队列与 DLQ 均为空。
2. 仅停止 RabbitMQ；Gateway 与 Monitor 保持健康，Portal、Admin、Search 因 RabbitMQ 依赖健康检查变为 `unhealthy`。
3. RabbitMQ 停止期间，经 Nginx → Gateway 创建两笔带唯一幂等键的订单；k6 检查 `6/6` 通过。
4. 故障期间：两笔订单已持久化，SKU 31 为 `stock=2、lock_stock=2、可售=0`，两条预占均为 `LOCKED`，两条 Outbox 均为 `PENDING`。
5. 保持 RabbitMQ 停止直至预占过期；两条预占自动变为 `RELEASED`，SKU 31 回到 `2 / 0 / 2`，Outbox 仍为 `PENDING`。
6. 启动同一个 RabbitMQ 容器；RabbitMQ、Portal、Admin、Search 全部恢复 `healthy`。
7. Outbox 最终均为 `PUBLISHED`；主队列回到 0，DLQ 中保留 2 条迟到 `ORDER_CREATED` 消息。

## 3. 一致性对账

| 阶段 | MySQL stock | MySQL lock_stock | 可售库存 | 预占状态 | Outbox 状态 |
| --- | ---: | ---: | ---: | --- | --- |
| 故障前 | 2 | 0 | 2 | 无 | 无 |
| RabbitMQ 停止后下单 | 2 | 2 | 0 | 2 条 `LOCKED` | 2 条 `PENDING` |
| 超时释放后 | 2 | 0 | 2 | 2 条 `RELEASED` | 2 条 `PENDING` |
| RabbitMQ 恢复后 | 2 | 0 | 2 | 2 条 `RELEASED` | 2 条 `PUBLISHED` |

最终状态：

- 两笔订单均仍为 `PENDING_PAYMENT`；
- 两条预占保持 `RELEASED`，未被迟到消息重新确认或重新锁定；
- 两条 Outbox 均已发布；
- `pms_event_consume_log` 中没有这两条事件记录；
- `mall.order.created.queue` 为 0；
- `mall.order.created.dlq` 为 2。

## 4. 结论与限制

Outbox 保证的是本地订单事务提交后可恢复发布，不是分布式强一致事务。本次演练证明 RabbitMQ 不可用期间订单、Outbox 和库存预占可持久化；服务恢复后 Outbox 能发布迟到消息。

迟到消息尝试确认已 `RELEASED` 预占时被拒绝并进入 DLQ，避免库存被重新锁定。由于确认失败会回滚消费者本地事务，消费幂等记录也不会落库；DLQ 当前没有自动消费者，需人工排查和受控处理，不能自动重放。

## 5. 原始证据位置

```text
docs/performance/results/perf13-20260830-001/s3-recovery/
