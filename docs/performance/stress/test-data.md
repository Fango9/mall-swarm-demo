# 流程 14：专用测试数据清单

## 1. 批次与隔离规则

| 项目 | 值 |
| --- | --- |
| 批次标识 | `PERF14-20260831-001` |
| 商品、SKU、关键词前缀 | `PERF14-20260831-001-` |
| MEMBER 用户名前缀 | `PERF14-20260831-001-` |
| 下单幂等键前缀 | `PERF14-20260831-001-` |
| 数据创建入口 | `http://127.0.0.1:8088`，经 Nginx 与 Gateway |
| 清理范围 | 仅清理本文件回填登记且带本批次前缀的数据 |

不在 Git 中记录密码、Bearer Token、`.env.performance` 或原始业务响应。不得通过数据库直接创建压测业务数据。

## 2. 计划创建的数据

| 用途 | 计划名称或值 | 创建后必须回填 |
| --- | --- | --- |
| P1、P2、P3、P4 商品 | `PERF14-20260831-001-catalog-product` | productId、productSn、分类 ID、品牌 ID |
| P1、P2、P3、P4 SKU | `PERF14-20260831-001-catalog-sku` | skuId、SKU 编码、初始 `stock`、初始 `lock_stock` |
| P2 搜索关键词 | `20260831` | Elasticsearch 索引确认时间与查询结果 |
| P5 热点下单商品 | `PERF14-20260831-001-order-product` | productId、productSn、分类 ID、品牌 ID |
| P5 热点下单 SKU | `PERF14-20260831-001-order-sku` | skuId、SKU 编码、初始 `stock=100`、初始 `lock_stock=0` |
| P5B 热点下单 SKU | `PERF14-20260831-001-p5b-order-sku` | skuId=`35`、基线 `stock=300`、初始 `lock_stock=0` |
| P5D 重复性验证 SKU | `PERF14-20260831-001-p5d-order-sku` | skuId=`36`、初始 `stock=300`、初始 `lock_stock=0` |
| P5E 强度提升 SKU | `PERF14-20260831-001-p5e-order-sku` | skuId=`37`、初始 `stock=300`、初始 `lock_stock=0` |
| P5F 上限探测 SKU | `PERF14-20260831-001-p5f-order-sku` | skuId=`38`、初始 `stock=300`、初始 `lock_stock=0` |
| P5G 延迟阈值探测 SKU | `PERF14-20260831-001-p5g-order-sku` | skuId=`39`、初始 `stock=300`、初始 `lock_stock=0` |
| P5H 千 RPS 探测 SKU | `PERF14-20260831-001-p5h-order-sku` | skuId=`40`、初始 `stock=300`、初始 `lock_stock=0` |
| P4、P5 MEMBER | `PERF14-20260831-001-m01` 至 `PERF14-20260831-001-m100` | memberId、用户名、角色 |
| P5 已执行尝试数 | `100` | 100 个唯一幂等键，均已创建订单 |

## 3. 场景数据边界

- P1 使用 catalog product 的详情接口；正式测量前先完成一次单请求预热，使详情缓存成为热缓存。
- P2 使用关键词 `20260831`；必须等待 Search 索引包含本批次商品后才能开始测量。
- P3 在启动前只删除本批次 catalog product 的详情正常缓存与空值缓存；若详情互斥锁存在则中止，不能使用模糊匹配或全库删除。P3 标记为“冷启动回源恢复”：只有启动窗口为回源阶段，缓存回填后的后续请求不应被表述为持续冷缓存能力。
- P4 只使用本批次 MEMBER 和 catalog SKU；每轮开始与结束都记录购物车状态。
- P5B 只使用专用 order SKU；本轮用户购物车中该 SKU 的数量按 `1,2,3` 循环分配，且使用唯一 `Idempotency-Key`。
- P5 仅测有限库存下的最大安全突发能力，不命名为最大可持续下单 TPS。
- P7 使用现有 dev Gateway 限流配置，不创建或修改业务数据。

## 4. 创建后回填表

| 类型 | 名称 | 主键 | 初始 stock | 初始 lock_stock | 创建证据 |
| --- | --- | --- | --- | --- | --- |
| 商品 | `PERF14-20260831-001-catalog-product` | `32` | 不适用 | 不适用 | `POST /admin/products` 成功；Admin 商品详情已验证为上架 |
| SKU | `PERF14-20260831-001-catalog-sku` | `33` | `1000` | `0` | `POST /admin/products/32/skus` 成功；Admin SKU 详情已验证 |
| 商品 | `PERF14-20260831-001-order-product` | `33` | 不适用 | 不适用 | `POST /admin/products` 成功；Admin 商品详情已验证为上架 |
| SKU | `PERF14-20260831-001-order-sku` | `34` | `100` | `0` | `POST /admin/products/33/skus` 成功；Admin SKU 详情已验证 |
| SKU | `PERF14-20260831-001-p5b-order-sku` | `35` | `300` | `0` | 初始创建成功后通过 `PUT /admin/products/33/skus/35` 调整库存 |
| SKU | `PERF14-20260831-001-p5d-order-sku` | `36` | `300` | `0` | `POST /admin/products/33/skus` 成功；Admin SKU 详情已验证 |
| SKU | `PERF14-20260831-001-p5e-order-sku` | `37` | `300` | `0` | `POST /admin/products/33/skus` 成功；Admin SKU 详情已验证 |
| SKU | `PERF14-20260831-001-p5f-order-sku` | `38` | `300` | `0` | `POST /admin/products/33/skus` 成功；Admin SKU 详情已验证 |
| SKU | `PERF14-20260831-001-p5g-order-sku` | `39` | `300` | `0` | `POST /admin/products/33/skus` 成功；Admin SKU 详情已验证 |
| SKU | `PERF14-20260831-001-p5h-order-sku` | `40` | `300` | `0` | `POST /admin/products/33/skus` 成功；Admin SKU 详情已验证 |
| P2 搜索关键词 | `20260831` | 不适用 | 不适用 | 不适用 | 经 `GET /search/products?keyword=20260831&pageNum=1&pageSize=10` 验证命中商品 `32`、`33` |
| MEMBER | `PERF14-20260831-001-m01` 至 `PERF14-20260831-001-m100` | `19` 至 `118` | 不适用 | 不适用 | 100 个账号均已注册并登录验证为启用的 `MEMBER` |

## 5. 清理前提

仅在所有结果、MySQL/Redis/Outbox/消费一致性核对和故障恢复记录归档完成后，才允许清理本批次数据。清理前必须按本文件的主键和前缀复核范围；不得按模糊名称或全表删除。
