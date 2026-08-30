# S6 Demo 服务重启下的注册、路由与监控恢复结果

## 1. 演练信息

- 批次：`perf13-20260830-001`
- 演练对象：`mall-swarm-mall-demo-1`
- 验证入口：`http://127.0.0.1:8088/demo/ping`
- Gateway 路由：`/demo/** → lb://mall-demo`，并使用 `StripPrefix=1`。
- 本次未停止或重建 MySQL、Redis、RabbitMQ、Elasticsearch、Nacos、Gateway、Portal、Admin、Search 或 Monitor；未删除任何数据卷，也没有业务写入操作。

## 2. 故障与恢复步骤

1. 重启前记录 Demo 容器、Nacos 实例、Monitor 和 Nginx → Gateway → Demo 入口状态。
2. 仅停止 `mall-swarm-mall-demo-1`，记录服务下线期间的状态。
3. 启动同一个 `mall-swarm-mall-demo-1` 容器。
4. 容器恢复后，重新记录 Nacos、Monitor 与真实入口状态。

## 3. 真实观察与对账

| 阶段 | Demo 容器 | Nacos healthy/enabled | Monitor | Nginx → Gateway → Demo |
| --- | --- | --- | --- | --- |
| 重启前 | running healthy | 1 / 1 | `UP` | HTTP 200，业务码 200，`mall-demo` / `pong` |
| 停止期间 | exited unhealthy | 0 / 0 | `NOT_REGISTERED` | HTTP 503 |
| 恢复后 | running healthy | 1 / 1 | `UP` | HTTP 200，业务码 200，`mall-demo` / `pong` |

停止期间，Gateway 的 HTTP 503 说明路由没有把请求转发到已摘除的 Demo 实例。恢复后，不能只依据 Docker 容器为 running 判定成功；本次同时确认了 Nacos 重新注册、Monitor 恢复健康，以及经 Nginx 真实入口的请求恢复。

## 4. 结论与限制

本次演练证明，在当前单机 Docker Compose 环境中，Demo 服务停止后可从 Nacos 实例列表摘除，Monitor 随之不再注册该服务，Gateway 真实入口返回 503；同一容器启动后，实例重新注册为健康且启用，Monitor 恢复为 `UP`，Gateway 路由恢复可用。

这只是单个无业务写入 Demo 服务的重启恢复验证，不等同于订单、库存等有状态服务的故障恢复承诺，也不能外推为生产环境的高可用能力。

## 5. 原始证据

- `docs/performance/results/perf13-20260830-001/s6-demo-restart/before-state.md`
- `docs/performance/results/perf13-20260830-001/s6-demo-restart/during-restart-state.md`
- `docs/performance/results/perf13-20260830-001/s6-demo-restart/after-recovery-state.md`