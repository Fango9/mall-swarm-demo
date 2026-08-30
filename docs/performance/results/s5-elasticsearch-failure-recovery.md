# S5 Elasticsearch 故障与索引恢复结果

## 1. 演练信息

- 批次：`perf13-20260830-001`
- 公开搜索入口：`http://127.0.0.1:8088/search/products`
- 故障目标：仅 `mall-elasticsearch`
- Elasticsearch 数据卷：`mall-swarm_elasticsearch-data:/usr/share/elasticsearch/data`
- Search 索引：`mall_product`
- 未停止或重建 MySQL、Redis、RabbitMQ、Nacos、Gateway、Portal、Admin、Monitor，也未删除 Elasticsearch 数据卷。
- 测试商品：ID `30`，名称 `PERF13-20260830-001-product`。

## 2. 故障步骤与真实观察

1. 故障前，Elasticsearch、Search、Monitor 均健康；Monitor 中 `mall-search` 为 `UP`。
2. 故障前，`mall_product` 文档数为 2；经 Nginx → Gateway 的关键词搜索成功，业务码 `200`，总数为 1，命中商品 30。
3. 仅停止 `mall-elasticsearch`。Search、Monitor、MySQL 容器没有被停止，Elasticsearch 数据卷仍保持挂载。
4. Elasticsearch 停止后，Monitor 中 `mall-search` 变为 `DOWN`；稍后 Search Docker health 也变为 `unhealthy`。
5. 故障中，经 Nginx → Gateway 搜索返回 HTTP `500`，curl exit `0`。响应为 Spring Boot 默认错误结构：`status=500`、`error=Internal Server Error`、`path=/portal/search/products`，没有统一 `CommonResult` 业务码。
6. 故障中，测试商品 MySQL 主数据保持不变；Elasticsearch 索引查询按容器停止状态记录为跳过。
7. 启动同一个 Elasticsearch 容器，确认其恢复 healthy 且索引端口可访问。
8. 仅重启 `mall-swarm-mall-search-1`。当前代码的启动期 `ProductSearchIndexRebuildRunner` 会调用全量索引重建：删除并按 Admin 当前可索引商品重写 Elasticsearch 派生索引，不写 MySQL 商品主数据。
9. Search 重启后恢复 healthy，Monitor 中恢复为 `UP`；`mall_product` 文档数为 2，公开关键词搜索再次成功并命中商品 30，MySQL 主数据与故障前一致。

## 3. 对账

| 阶段 | Elasticsearch | Monitor 中 Search | 索引文档数 | 公开搜索 | 商品 30 MySQL 主数据 |
| --- | --- | --- | ---: | --- | --- |
| 故障前 | healthy，单节点 yellow | `UP` | 2 | 200，命中 30 | 已发布，未变化 |
| 故障中 | exited unhealthy | `DOWN` | 不可查询 | HTTP 500，默认错误 JSON | 未变化 |
| 恢复并重启 Search 后 | healthy，单节点 yellow | `UP` | 2 | 200，命中 30 | 未变化 |

单节点 `yellow` 来自副本分片未分配，不等同于 Elasticsearch 不可用。

## 4. 结论与限制

本次演练证明 Elasticsearch 停止后，Search 依赖健康状态会由 Monitor 观察为 `DOWN`，公开搜索不可用，但商品 MySQL 主数据不受影响。

恢复 Elasticsearch 并重启 Search 后，启动期全量索引重建机制按当前实现执行；Search、Monitor、索引文档数和公开搜索均恢复。由于当前实现没有额外的“重建完成”业务日志，重建结论来自启动期重建代码、Search 成功启动、索引文档数恢复和公开搜索恢复的组合证据，而不是单独的重建完成日志。

Search 依赖 Elasticsearch 异常当前返回 Spring Boot 默认 HTTP 500，而不是统一 `CommonResult`；这是一项已发现的问题，应在后续迭代中补充基础设施异常转换策略。

本次未取得 Nacos 中 Search 下线、上线的直接 API 证据：尝试的实例统计路径返回 HTTP 410，说明该路径与本机 Nacos 版本不兼容。Nacos、Gateway、Monitor 状态变化将在后续专门的服务重启场景继续验证。

## 5. 原始证据

- `docs/performance/results/perf13-20260830-001/s5-elasticsearch/before-state.md`
- `docs/performance/results/perf13-20260830-001/s5-elasticsearch/during-outage-state.md`
- `docs/performance/results/perf13-20260830-001/s5-elasticsearch/after-recovery-state.md`
- 故障中公开搜索：HTTP `500`，默认错误 JSON 的字段已在本报告第 2 节记录。