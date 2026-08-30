# 流程 13：压测环境冻结记录

> 本文件记录单机 Docker Compose 基线，不构成生产容量承诺。
> 正式压测开始前必须复核本文件；若任一冻结项变化，创建新的结果批次。

## 1. 本次冻结

| 项目 | 值 |
| --- | --- |
| 冻结时间 | 2026-08-30 15:00:49 CST |
| Git 分支 | `13-Pressure-testing` |
| Git 提交 | `86b6404ae487` |
| 业务入口 | `http://127.0.0.1:8088` |
| 请求链路 | `k6 → Nginx → Gateway → 微服务` |
| 操作系统 | macOS 26.5.1 |
| CPU / 内存 | Apple M4 Pro，14 个逻辑核 / 24 GiB |
| Docker Engine | 29.4.0 |
| JDK | Temurin 17.0.19+10 |
| 压测工具 | k6 v1.7.1（darwin/arm64） |
| 容器 CPU / 内存限制 | 当前均未显式限制 |

## 2. 容器镜像

| 服务 | 镜像 |
| --- | --- |
| Nginx | `nginx:1.27.5-alpine` |
| Gateway、Auth、Admin、Portal、Search、Monitor、Demo | `cn.fango.mall/mall-*:1.0.0` |
| Nacos | `nacos/nacos-server:v3.0.3` |
| MySQL | `mysql:8.0.32` |
| Redis | `redis:6.2` |
| RabbitMQ | `rabbitmq:4.3.5-management` |
| Elasticsearch | `docker.elastic.co/elasticsearch/elasticsearch:8.18.1` |

## 3. 配置文件指纹

| 文件 | SHA-256 |
| --- | --- |
| `docker-compose.yml` | `1d9cd8b7b981cca5cb891be5e005d780a1333f5348b7f20773ae75701e64cf72` |
| `docker-compose.app.yml` | `e85b400bee7a57806d1f526b2ab24bd688ed9cfd40fae9ff7726df61acb82aa4` |
| `deploy/nginx/default.conf` | `e26c7060dfea663b13d763485e8af7918cb79b3ff6afd3ac65010abd0d4961cf` |
| `deploy/nacos/dev/mall-gateway-dev.yml` | `abd5800b747ee5486c283fb065d17bc116ba66fc61a89034e93be0ff6f52d06c` |
| `deploy/nacos/dev/mall-admin-dev.yml` | `616039453afb952204ee8b9bfad90f5f926a6fdc2de0a9e4e951a3ff48a9acb2` |

## 4. 运行配置摘要

- Nginx 仅监听宿主机 `127.0.0.1:8088`，转发到 Gateway `8200`。
- 公开商品浏览按客户端 IP 限流：20 请求/秒，突发容量 40。
- 购物车、下单按 MEMBER 限流：5 请求/秒，突发容量 10。
- 热点 SKU：1；热点库存 Redis 路径异常时，回退 MySQL 条件更新。
- 当前容器健康状态已在冻结前检查；正式压测前重新检查并在结果批次中记录。

## 5. Nacos 实际版本

不得填写密码、Token 或配置原文。下一步仅记录下表中的 Data ID、修改时间与 MD5：

| Data ID | Group | 修改时间 | MD5 |
| --- | --- | --- | --- |
| `mall-gateway-dev.yml` | `DEFAULT_GROUP` | 2026-08-29 14:45:47 CST | `69d6c84a3cf6ad2ce29beb99ee5f4a45` |
| `mall-auth-dev.yml` | `DEFAULT_GROUP` | 2026-08-29 14:45:47 CST | `a17be40fc0ffa71c9d1a8d2d058cef57` |
| `mall-admin-dev.yml` | `DEFAULT_GROUP` | 2026-08-30 15:00:49 CST | `3a5176c448f6cd966740bf1c8c8b4af5` |
| `mall-portal-dev.yml` | `DEFAULT_GROUP` | 2026-08-29 14:45:47 CST | `c10a575d2c9d2e50b29b8c2a3c7ef5e4` |
| `mall-search-dev.yml` | `DEFAULT_GROUP` | 2026-08-29 14:45:47 CST | `b80afbf2b42ba927ff29f026959f8731` |
| `mall-monitor-dev.yml` | `DEFAULT_GROUP` | 2026-08-29 14:45:47 CST | `0df557526792e6a205720b5878a6e5fd` |
| `mall-demo-dev.yml` | `DEFAULT_GROUP` | 2026-08-29 14:45:47 CST | `9261dbc28397288a99c306a152bff1a1` |