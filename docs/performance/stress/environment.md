# 流程 14：环境冻结记录

## 1. 当前基线

| 项目 | 值 |
| --- | --- |
| Git commit | `24d9a42` |
| 主机 | MacBook Pro，Apple M4 Pro，14 核，24 GB 内存 |
| 操作系统 | macOS 26.5.1 |
| Docker Engine | 29.4.0 |
| Docker Compose | v5.1.2 |
| Docker VM 配额 | 14 CPU，12,600,381,440 Bytes 内存 |
| k6 | v1.7.1，darwin/arm64 |
| 压测机位置 | 本机，与被测 Docker 环境共用物理主机 |
| 结论限制 | 单机下限基线，不能外推为生产容量 |

## 2. 镜像与容器健康

记录时间：`2026-08-31`

| 服务 | 镜像 | 当前状态 |
| --- | --- | --- |
| MySQL | `mysql:8.0.32` | healthy |
| Redis | `redis:6.2` | healthy |
| Nacos | `nacos/nacos-server:v3.0.3` | running，无 healthcheck |
| RabbitMQ | `rabbitmq:4.3.5-management` | healthy |
| Elasticsearch | `docker.elastic.co/elasticsearch/elasticsearch:8.18.1` | healthy |
| Gateway | `cn.fango.mall/mall-gateway:1.0.0` | healthy |
| Auth | `cn.fango.mall/mall-auth:1.0.0` | healthy |
| Admin | `cn.fango.mall/mall-admin:1.0.0` | healthy |
| Portal | `cn.fango.mall/mall-portal:1.0.0` | healthy |
| Search | `cn.fango.mall/mall-search:1.0.0` | healthy |
| Demo | `cn.fango.mall/mall-demo:1.0.0` | healthy |
| Monitor | `cn.fango.mall/mall-monitor:1.0.0` | healthy |
| Nginx | `nginx:1.27.5-alpine` | running |

## 3. Compose 资源冻结配置

资源覆盖文件：

`docker-compose.perf.yml`

后续固定使用以下 Compose 文件顺序：

    docker-compose.yml
    docker-compose.app.yml
    docker-compose.perf.yml

已于 2026-08-31 使用上述 Compose 文件顺序受控重建容器，并通过 Docker inspect 核对实际 CPU/内存限制。

Gateway 与 Admin 当前 profile 均为 `perf`，分别加载 Nacos 中的 `mall-gateway-perf.yml` 与 `mall-admin-perf.yml`；Auth、Demo、Monitor、Portal、Search 保持 `dev` profile。所有容器已恢复运行，具有 healthcheck 的容器均为 healthy；Nacos 和 Nginx 无 healthcheck，状态为 running。

Nacos 的 perf 资源基线为 `0.5 CPU / 768MiB`，JVM 参数固定为 `-Xms256m -Xmx256m -Xmn128m`。该配置避免镜像默认 `1GiB` 堆与容器限额冲突，并为堆外内存、线程栈和系统缓存保留余量。

Elasticsearch 的 perf 资源基线为 `1.5 CPU / 1536MiB`，JVM 堆保持 `-Xms512m -Xmx512m`。该配置为 Metaspace、Direct Memory 和本地内存保留余量，避免 `1GiB` 容器限额下的常驻内存贴线。

## 4. 当前 Nacos dev 配置指纹

| Data ID | Group | 修改时间 | MD5 |
| --- | --- | --- | --- |
| `mall-gateway-dev.yml` | `DEFAULT_GROUP` | 2026-08-29 14:45:47 CST | `69d6c84a3cf6ad2ce29beb99ee5f4a45` |
| `mall-auth-dev.yml` | `DEFAULT_GROUP` | 2026-08-29 14:45:47 CST | `a17be40fc0ffa71c9d1a8d2d058cef57` |
| `mall-admin-dev.yml` | `DEFAULT_GROUP` | 2026-08-30 15:00:49 CST | `3a5176c448f6cd966740bf1c8c8b4af5` |
| `mall-portal-dev.yml` | `DEFAULT_GROUP` | 2026-08-29 14:45:47 CST | `c10a575d2c9d2e50b29b8c2a3c7ef5e4` |
| `mall-search-dev.yml` | `DEFAULT_GROUP` | 2026-08-29 14:45:47 CST | `b80afbf2b42ba927ff29f026959f8731` |
| `mall-monitor-dev.yml` | `DEFAULT_GROUP` | 2026-08-29 14:45:47 CST | `0df557526792e6a205720b5878a6e5fd` |
| `mall-demo-dev.yml` | `DEFAULT_GROUP` | 2026-08-29 14:45:47 CST | `9261dbc28397288a99c306a152bff1a1` |
| `mall-gateway-perf.yml` | `DEFAULT_GROUP` | 2026-08-31 00:35:18 CST | `5b88695fbce931ab8be542b6b46965d1` |
| `mall-admin-perf.yml` | `DEFAULT_GROUP` | 2026-08-31 01:44:07 CST | `f8929b0d1c5911d9e9479b216119c353` |

## 5. 测试数据与结果批次

- `PERF14-` catalog 商品/SKU 已创建：productId=`32`、skuId=`33`；P5 order 商品/SKU 已创建：productId=`33`、skuId=`34`；专用 MEMBER 已创建：memberId=`19` 至 `118`（共 100 个）；
- 调整前 P1 热缓存 10 RPS、5 分钟对照：`docs/performance/stress/results/stress-20260831-154900/p1-product-detail-10rps/`；该结果使用 Nacos `512MiB` 容器限额；
- 调整后 P1 热缓存 10 RPS、5 分钟正式基线：`docs/performance/stress/results/stress-20260831-161000/p1-product-detail-10rps/`；该结果使用当前 Nacos `768MiB` 容器限额与 `256MiB` JVM 堆；
- P2 商品搜索 10 RPS、5 分钟调整前对照：`docs/performance/stress/results/stress-20260831-163000/p2-product-search-10rps/`；经 Gateway 转发至 Portal、Search 与 Elasticsearch，零错误通过，但该结果使用 Elasticsearch `1GiB` 容器限额，内存峰值为 `91.54%`；
- P2 商品搜索 10 RPS、5 分钟正式基线：`docs/performance/stress/results/stress-20260831-165000/p2-product-search-10rps/`；使用当前 Elasticsearch `1536MiB` 容器限额，零错误通过，Elasticsearch 内存峰值为 `60.09%`；
- P3 冷启动回源恢复 10 RPS、5 分钟非正式对照：`docs/performance/stress/results/stress-20260831-170000/p3-cold-product-detail-10rps/`；k6 零错误通过，但缺少运行后缓存状态文件，不能作为正式基线；
- P3 冷启动回源恢复 10 RPS、5 分钟正式基线：`docs/performance/stress/results/stress-20260831-171000/p3-cold-product-detail-10rps/`；启动前商品 `32` 的详情正常缓存、空值缓存和互斥锁均不存在，运行后仅详情正常缓存回填，k6 零错误通过；
- P4 已登录购物车读写 10 workflow RPS、5 分钟正式基线：`docs/performance/stress/results/stress-20260831-173000/p4-cart-read-write-10workflow-rps/`；10 个 PERF14 成员各有一个 SKU `33` 购物车项，完成 3000 个读写工作流、6000 个 HTTP 请求，零错误通过；
- P5 热点库存突发验证：`docs/performance/stress/results/stress-20260831-174000/p5-hot-stock-burst/`；100 次独立下单全部成功，SKU `34` 的 100 件专用库存全部由已确认预占占用，订单、Outbox 与消费记录对账一致。该轮未采集完整资源指标，只作为库存安全与一致性验证，不能作为完整性能基线；后续 P5 轮次已补入完整资源采集链；
- P5 幂等重试补测：`docs/performance/stress/results/stress-20260831-175000/p5-idempotency-retry/`；同一会员、同一购物车项、同一幂等键的 1 次重试返回原订单，P95=`9.71ms`，资源指标已采集，一致性快照前后完全相同；
- P5 库存耗尽补测：`docs/performance/stress/results/stress-20260831-180000/p5-stock-exhausted-reject/`；SKU `34` 可售库存为 0 时，1 次新幂等键下单按预期返回业务码 `54011`，P95=`26.47ms`，一致性快照前后完全相同，临时购物车项已清理；
- P5 幂等并发补测：`docs/performance/stress/results/stress-20260831-181000/p5-idempotency-retry/`；10 VU 对同一订单执行 100 次同键重试，100/100 返回原订单，P95=`37.94ms`，一致性快照前后完全相同；
- P5 库存耗尽并发补测：`docs/performance/stress/results/stress-20260831-182000/p5-stock-exhausted-reject/`；10 VU 使用 100 个新幂等键请求耗尽 SKU，100/100 返回 `54011`，P95=`69.39ms`；订单数仍为 100，临时购物车项已清理，一致性快照前后完全相同；
- P6 浏览与搜索混合稳定性基线：`docs/performance/stress/results/stress-20260831-184000/p6-browse-search-mix-10rps/`；10 RPS、30 分钟、70% 商品详情与 30% 搜索，完成 18,001 次请求，HTTP/业务错误率均为 0，整体 P95=`16.44ms`、P99=`18.91ms`；完整资源指标已归档。该结果为单机下限基线；
- P6 浏览与搜索混合稳定性第二阶段：`docs/performance/stress/results/stress-20260831-185000/p6-browse-search-mix-20rps/`；20 RPS、30 分钟、70% 商品详情与 30% 搜索，完成 36,001 次请求，HTTP/业务错误率均为 0，整体 P95=`14.36ms`、P99=`17.14ms`；完整资源指标已归档。该结果为单机下限基线；
- P6 浏览与搜索混合稳定性第三阶段：`docs/performance/stress/results/stress-20260831-190000/p6-browse-search-mix-40rps/`；40 RPS、30 分钟、70% 商品详情与 30% 搜索，完成 72,000 次请求，HTTP/业务错误率均为 0，整体 P95=`11.65ms`、P99=`14.05ms`；完整资源指标已归档。该结果为单机下限基线；
- P6 浏览与搜索混合稳定性第四阶段：`docs/performance/stress/results/stress-20260831-191000/p6-browse-search-mix-80rps/`；80 RPS、30 分钟、70% 商品详情与 30% 搜索，完成 143,999 次请求，HTTP/业务错误率均为 0，整体 P95=`8.64ms`、P99=`10.52ms`，但出现 1 个 `dropped_iterations`（计划 144,000 次）。本轮业务 SLO 通过，但到达率完整性待复测，不作为正式 80 RPS 稳定性基线；
- P6 浏览与搜索混合 160 RPS 探索：`docs/performance/stress/results/stress-20260831-192000/p6-browse-search-mix-160rps/`；160 RPS、20 分钟、70% 商品详情与 30% 搜索，完成 192,000 次请求，无 dropped iterations，HTTP/业务错误率均为 0，整体 P95=`6.09ms`、P99=`8.44ms`；完整资源指标已归档。该轮为单机 20 分钟探索性结果，不替代 30 分钟稳定性基线；
- P6 浏览与搜索混合 320 RPS 探索：`docs/performance/stress/results/stress-20260831-193000/p6-browse-search-mix-320rps/`；320 RPS、10 分钟、70% 商品详情与 30% 搜索，HTTP/业务错误率均为 0，整体 P95=`3.17ms`、P99=`5.05ms`，完成 191,996 次、丢弃 5 次；完成数与丢弃数之和为 192,001，存在 k6 离散调度的边界迭代，不能单独归因于服务容量；
- P6 浏览与搜索混合 320 RPS 复测：`docs/performance/stress/results/stress-20260831-194000/p6-browse-search-mix-320rps/`；320 RPS、10 分钟，HTTP/业务错误率均为 0，整体 P95=`3.16ms`、P99=`4.98ms`，完成 191,998 次、丢弃 3 次；完成数与丢弃数之和同为 192,001。预分配 10 VU，运行峰值 6 VU；
- P6 浏览与搜索混合 320 RPS 收尾与预分配修正复测：`docs/performance/stress/results/stress-20260831-195000/p6-browse-search-mix-320rps/`；320 RPS、10 分钟、预分配 20 VU、1 分钟 graceful stop，HTTP/业务错误率均为 0，整体 P95=`3.17ms`、P99=`5.00ms`，完成 191,999 次、丢弃 2 次；完成数与丢弃数之和仍为 192,001，VU 峰值仅 3。该少量边界丢弃不作为服务容量失败依据，320 RPS 可进入 30 分钟确认；
- P6 浏览与搜索混合 640 RPS 探索：`docs/performance/stress/results/stress-20260831-200000/p6-browse-search-mix-640rps/`；640 RPS、10 分钟、预分配 20 VU、1 分钟 graceful stop，HTTP/业务错误率均为 0，整体 P95=`2.45ms`、P99=`3.62ms`，完成 383,982 次、丢弃 19 次；完成数与丢弃数之和为 384,001（`640 × 600 + 1`），无服务错误或延迟退化，作为单机 10 分钟探索通过；
- P6 浏览与搜索混合 2,000 RPS 探索：`docs/performance/stress/results/stress-20260831-203000/p6-browse-search-mix-2000rps/`；2,000 RPS、10 分钟、预分配 50 VU、最大 500 VU，HTTP/业务错误率为 0，但 P95=`3.36s`、P99=`6.38s`，达到 500 VU 上限，完成 438,294 次（约 729 RPS）、丢弃 761,707 次迭代。该轮为明确性能与发压饱和失败点，不进入确认；
- P6 浏览与搜索混合 1,280 RPS 二分：`docs/performance/stress/results/stress-20260901-204000/p6-browse-search-mix-1280rps/`；1,280 RPS、10 分钟，达到 500 VU 上限，完成 508,405 次（约 847 RPS）、丢弃 259,596 次迭代，并有 1 次业务检查失败；整体 P95=`2.05s`、P99=`5.28s`，最长请求接近 60 秒。该轮为明确失败点，不进入确认；
- P6 浏览与搜索混合 960 RPS 二分：`docs/performance/stress/results/stress-20260901-205000/p6-browse-search-mix-960rps/`；960 RPS、10 分钟，HTTP/业务错误率均为 0、P95=`134.56ms`，但达到 500 VU 上限，完成 508,930 次（约 848 RPS）、丢弃 67,071 次迭代，P99=`2.67s`。未能维持目标到达率，不作为 960 RPS 通过；
- 当前 Gateway 使用 perf 限流配置；dev 配置保持不变，保留给 P7 Gateway 限流保护回归。

任何镜像、Nacos 配置、容器资源、测试数据或压测机变化，均必须新建结果批次。
