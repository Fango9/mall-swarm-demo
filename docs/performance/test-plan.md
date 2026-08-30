# 流程 13：容量与稳定性验收测试计划

## 1. 目标与边界

所有压测必须经过：

`压测客户端 → http://127.0.0.1:8088 → Nginx → Gateway → 微服务`

本计划只验证本机单节点 Docker Compose 环境，不推导生产容量。不停止 MySQL、不删除数据卷，也不删除既有业务数据。

## 2. 环境冻结

| 项目 | 本次基线 |
| --- | --- |
| Git 提交 | `86b6404ae487` |
| 操作系统 | macOS 26.5.1 |
| CPU / 内存 | Apple M4 Pro，14 个逻辑核 / 24 GiB |
| Docker Engine | 29.4.0 |
| JDK | Temurin 17.0.19+10 |
| 压测工具 | k6 v1.7.1 |
| 统一入口 | `http://127.0.0.1:8088` |
| 容器资源限制 | 未显式限制 CPU、内存 |
| Nacos 配置来源 | `deploy/nacos/dev/`；正式执行前记录实际 Data ID 修改时间与 MD5 |
| Gateway 浏览限流 | IP 维度：20/s，burst 40 |
| Gateway 会员写限流 | MEMBER 维度：5/s，burst 10 |
| 热点库存 | 当前实际热点 SKU：30；Redis 异常时 Admin 库存预扣会回退 MySQL 条件更新，但端到端请求还受 Gateway Redis 限流与 Sa-Token Redis 会话影响 |

正式执行前如环境、镜像、配置或 Git 提交变化，建立新的结果批次，禁止与旧数据横向比较。

## 3. 测试数据隔离

- 商品、SKU：`PERF13-` 前缀
- 测试会员：`perf13-` 前缀
- Idempotency-Key：`perf13-` 前缀
- 结果批次：`perf13-YYYYMMDD-HHMMSS`
- 仅清理带上述标识的数据；清理 SQL 与主键范围后续归档到 `results/`。

## 4. 最小场景

### S1：商品浏览冷、热缓存

测试 `/portal/categories`、`/portal/products`、`/portal/products/{productId}` 的冷缓存与热缓存。

记录总请求、成功、失败、QPS、平均延迟、p50、p95、p99、429 数、Portal 缓存命中证据与 MySQL 压力。两轮必须使用相同参数。

### S2：Gateway 限流

验证公开浏览按固定客户端 IP 限流，以及购物车、下单按 MEMBER 限流。

结果必须区分：业务成功、HTTP 429 限流、库存不足、认证或授权失败、系统错误。429 必须是统一 JSON 响应。

### S3：热点库存并发

为 `PERF13-` 专用热点 SKU 设置初始库存 `N`，发起大于 `N` 的并发下单；每个请求必须使用唯一 Idempotency-Key。

验证成功数不超过 `N`，并核对 MySQL `stock` / `lock_stock`、Redis 热点库存键、预占状态、订单与明细、Outbox、RabbitMQ 消费结果。释放或超时恢复后，库存必须回到预期值；MySQL 条件更新是最终库存边界。

#### S3-R：RabbitMQ 不可用下的预占超时释放与迟到消息

使用独立的 `PERF13-` 恢复专用 SKU，初始 `stock=2`、`lock_stock=0`，并使用两个专用 MEMBER；不得复用已 `CONFIRMED` 的 SKU 30 订单。

执行前必须在 `test-data.md` 登记新 SKU、两个会员、购物车项和初始状态，并保存只读基线快照。

故障步骤与预期如下：

1. 仅停止 RabbitMQ 容器；不停止 MySQL、不删除队列、不删除数据卷。
2. 经 Nginx → Gateway 创建两个带唯一幂等键的测试订单。
3. 在 RabbitMQ 停止期间，确认订单、本地 Outbox 与 `LOCKED` 预占已持久化；预期 MySQL `lock_stock=2`，可售库存为 0。
4. 保持 RabbitMQ 不可用至少 15 分钟，并覆盖一次 5 秒的过期释放扫描周期；确认两条预占变为 `RELEASED`，`lock_stock` 回到 0，MySQL 可售库存回到 2。
5. 恢复同一个 RabbitMQ 容器；确认 Outbox 最终发布。迟到的 `ORDER_CREATED` 消息不得将已 `RELEASED` 的预占重新确认或重新锁定；若消费者按当前设计将该类迟到消息转入死信队列，必须如实记录为预期保护行为。
6. 记录 Nacos、Gateway、Monitor、RabbitMQ 队列和死信队列状态变化，以及 MySQL、Redis（如该 SKU 启用热点键）、Outbox、预占记录的最终状态。

停止 RabbitMQ 前必须单独复查容器名、精确恢复命令、观察项与恢复验证；任何不符合预期的状态均立即停止后续写入，并先恢复 RabbitMQ。

### S4：Redis 不可用下的入口降级与库存边界

本场景仅停止并恢复同一个 Redis 容器；不删除 Redis 数据卷，不清空键，不修改业务代码或 Nacos 配置。所有 HTTP 请求仍经过 Nginx → Gateway。

执行前保存 Redis、Gateway、Portal、Admin、Monitor 的健康状态，以及测试 SKU、订单、预占记录和 Redis 热点库存键的只读快照。

故障步骤与观察项如下：

1. 仅停止 Redis 容器，记录 Gateway、Portal、Admin、Monitor 的健康状态变化。
2. 以公开商品详情请求验证真实入口行为，记录 HTTP 状态、统一响应、Gateway 是否仍可完成 Redis 令牌桶处理，以及 Portal 是否收到请求。
3. 使用 Redis 故障前获取的 MEMBER Token 请求购物车读取接口，记录 Sa-Token Redis 会话不可用时的真实响应；不得把认证失败或系统错误误记为业务成功。
4. 不在 Redis 不可用期间创建新的订单。原因是 Gateway 限流和 MEMBER 会话均依赖 Redis，端到端请求可能无法到达 Admin；此时应如实记录“无法经真实入口验证 Admin 热点库存回退路径”。
5. 从源码核对并在报告中说明：Admin 热点库存 Redis 预扣未应用或 Redis 异常时，仍继续由 MySQL 条件更新作最终库存裁决；这不等同于 Redis 故障下端到端下单必然可用。
6. 恢复同一个 Redis 容器后，确认容器健康、Gateway 路由恢复、公开浏览恢复，并复查订单数、预占记录、MySQL `stock` / `lock_stock` 未被本场景意外改变。
7. 归档故障前、故障中、恢复后的状态快照和脱敏结果摘要；明确缓存降级、限流、会话、热点库存四条路径分别观察到的事实与未覆盖项。

停止 Redis 前必须再次确认容器名与恢复命令。若发现 MySQL 库存、预占或非 `PERF13-` 数据出现意外变化，立即恢复 Redis 并停止后续写请求。

### S5：Elasticsearch 不可用与索引恢复

本场景仅停止并恢复同一个 `mall-elasticsearch` 容器；不删除或重建 Elasticsearch 数据卷，不修改 MySQL 商品主数据，不创建订单或库存预占。所有公开业务验证仍经 Nginx → Gateway。

执行前保存 Elasticsearch 容器状态、Search 的健康状态、Monitor 中 Search 应用状态、索引文档数量，以及一次经 Nginx 的关键词搜索基线。

故障步骤与观察项如下：

1. 仅停止 Elasticsearch 容器；不停止 MySQL、Redis、RabbitMQ、Nacos、Gateway、Portal、Admin 或 Search。
2. 记录 Elasticsearch 容器停止后 Search 的健康状态变化，以及 Monitor 中 Search 依赖健康状态变化。
3. 经 Nginx → Gateway 请求同一个关键词搜索接口，记录 HTTP 状态、统一响应或超时；不得将搜索故障误记为商品 MySQL 主数据不可用。
4. 恢复同一个 Elasticsearch 容器，确认其健康和索引服务可连接。
5. 仅重启 `mall-swarm-mall-search-1`，触发 Search 启动时的全量索引重建。该重建只删除并重写 Elasticsearch 派生索引，不会写入商品 MySQL 主数据。
6. 记录 Search 在 Nacos 与 Monitor 中的下线、恢复上线和健康状态变化；确认重建后索引文档数量恢复，并经 Nginx → Gateway 用同一关键词再次搜索成功。
7. 对账故障前后商品 MySQL 数据不变；归档 Elasticsearch 容器、Search、Monitor、Nacos、搜索接口和索引文档数量的状态证据。

停止 Elasticsearch 前必须再次确认容器名、数据卷挂载和精确恢复命令。任何非预期的 MySQL 或非 `PERF13-` 数据变化均应立即停止后续写操作、先恢复 Elasticsearch。

### S6：服务重启下的 Nacos、Gateway 与 Monitor 恢复

本场景只重启无业务写入职责的 `mall-swarm-mall-demo-1`。不停止 MySQL、Redis、RabbitMQ、Elasticsearch、Nacos、Gateway、Portal、Admin、Search，也不删除任何数据卷。

验证入口为：

`http://127.0.0.1:8088/demo/ping`

该请求经过 Nginx → Gateway → `lb://mall-demo`。Nacos 实例状态使用本机 Nacos 3 控制台 API 的 `/v3/console/ns/instance/list` 读取；Monitor 状态使用 Spring Boot Admin 的受保护实例 API 读取。

故障步骤与观察项如下：

1. 重启前保存 `mall-demo` 的 Docker 状态、Nacos 健康/启用实例数、Monitor 状态，以及经 Nginx 的 `/demo/ping` 成功响应。
2. 仅重启 `mall-swarm-mall-demo-1`；记录容器短暂不可用期间 Nacos 实例数、Monitor 状态与 Gateway 入口响应。
3. 容器恢复后，确认 Nacos 恢复为 1 个健康且启用的 `mall-demo` 实例，Monitor 恢复为 `UP`。
4. 再次经 Nginx → Gateway 调用 `/demo/ping`，确认响应恢复为 HTTP `200`、业务码 `200`，且内容显示 `service=mall-demo`、`status=pong`。
5. 归档故障前、重启中、恢复后的状态；不把 Docker 容器“running”单独视为服务已恢复，必须同时满足 Nacos、Monitor 和 Gateway 入口证据。

重启前必须再次确认精确容器名和恢复命令。若 Gateway、Nacos 或 Monitor 在容器恢复后未收敛，停止后续操作并保留现场证据。

## 5. 归档规则

每次执行建立独立目录：

`docs/performance/results/perf13-YYYYMMDD-HHMMSS/`

只提交脱敏后的脚本、参数和结果摘要；不提交 Token、密码、个人网络地址、敏感日志或大体积原始结果。未执行的指标必须标记“未测”。
