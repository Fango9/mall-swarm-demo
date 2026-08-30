# S1 商品浏览压测结果

## 1. 执行信息

- 批次：`perf13-20260830-001`
- 工具：k6 `v1.7.1`
- 入口：`http://127.0.0.1:8088`（Nginx → Gateway → Portal）
- 限流配置：公开浏览按客户端 IP，`20/s`，突发容量 `40`
- 压测脚本：`docs/performance/scenarios/s1-product-browse.js`
- 压测参数：3 VU，持续 30 秒，每轮停顿 1 秒；每轮依次请求分类、商品列表和商品详情
- 冷缓存执行时间：`2026-08-30 15:31:44 CST`
- 热缓存执行时间：`2026-08-30 15:33:42 CST`

## 2. 实测结果

| 缓存状态 | 总请求数 | 成功数 | 失败数 | QPS | 平均延迟 | p50 | p95 | p99 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 冷缓存 | 261 | 261 | 0 | 8.62 | 14.08 ms | 11.63 ms | 27.50 ms | 39.10 ms |
| 热缓存 | 261 | 261 | 0 | 8.68 | 11.78 ms | 10.35 ms | 21.60 ms | 25.82 ms |

两次执行均为 `100%` 检查通过，HTTP 失败率为 `0%`，未触发 Gateway 限流。

## 3. 缓存状态证据与解释

冷缓存执行前，脚本 `prepare-s1-cold-cache.sh` 已验证以下四个精确键均不存在：

- `mall:portal:product:categories`
- `mall:portal:product:list:category:<categoryId>`
- `mall:portal:product:detail:<productId>`
- `mall:portal:product:detail:null:<productId>`

冷缓存执行后，分类、列表和详情三个正常缓存键已回填；详情空值缓存键不存在，符合该商品存在的预期。

在相同负载参数下，热缓存平均延迟较冷缓存低约 `16.3%`，p95 低约 `21.5%`，p99 低约 `34.0%`。这证明当前本机环境下缓存状态与端到端延迟存在可重复的关联；但这组数据尚不能单独量化 Portal 缓存命中率或 MySQL 压力，后续需要补充对应的可观测证据。

## 4. 原始结果位置

本机原始 k6 汇总与控制台输出位于：

```text
docs/performance/results/perf13-20260830-001/