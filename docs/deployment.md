# mall-swarm 容器化交付说明

## 1. 适用范围

本文档描述 mall-swarm 的 Docker Compose 容器化交付基线。

该基线覆盖镜像构建、基础设施与业务服务编排、Nacos 配置导入、Nginx 统一入口、健康检查、备份恢复和版本回滚。

该基线不等同于真实生产上线；生产环境仍需补充密钥管理、HTTPS 证书、访问控制、容量评估、监控告警与备份策略。

## 2. 交付组成

- 基础设施编排：[docker-compose.yml](../docker-compose.yml)
- 应用服务与 Nginx 编排：[docker-compose.app.yml](../docker-compose.app.yml)
- 业务镜像统一构建文件：[Dockerfile](../Dockerfile)
- Nacos dev 配置模板：[deploy/nacos/dev](../deploy/nacos/dev)
- Nacos 配置导入脚本：[deploy/scripts/import-nacos-dev.sh](../deploy/scripts/import-nacos-dev.sh)
- Nginx 反向代理配置：[deploy/nginx/default.conf](../deploy/nginx/default.conf)
- 本机或部署环境私有配置：`.env`（不纳入 Git）
- 可提交的环境变量清单：[.env.example](../.env.example)

## 3. 对外入口与端口边界

业务入口为 `http://127.0.0.1:8088`，请求链路为：

`客户端 → Nginx → Gateway → 业务服务`

除 Nginx 外，Admin、Auth、Portal、Search、Monitor 和 Demo 均不向宿主机暴露业务端口。

Monitor 通过 `/monitor/**` 经 Gateway 访问；Gateway 负责 ADMIN 校验，Monitor 自身保留表单登录与 Basic Auth。

## 4. 首次部署

### 4.1 前置条件

部署主机需要具备：

- Docker Engine 与 Docker Compose v2；
- Java 17 与 Maven；
- `curl` 与 `jq`，用于导入 Nacos 配置；
- 已创建并填写的 `.env` 文件。

首次使用时复制环境变量模板：

```bash
cp .env.example .env
```

随后在 `.env` 中填写部署环境自己的密码、Token 与密钥。不要将 `.env` 提交到 Git。

### 4.2 部署顺序

在项目根目录依次执行：

```bash
mvn clean package
docker compose up -d
bash deploy/scripts/import-nacos-dev.sh --apply
docker compose -f docker-compose.app.yml build
docker compose -f docker-compose.app.yml up -d
```

顺序原因：

1. Maven 先生成七个业务服务的可执行 JAR；
2. 基础设施先创建 `mall-swarm-network` 并启动 MySQL、Redis、Nacos、RabbitMQ 与 Elasticsearch；
3. Nacos 配置在业务服务启动前导入；
4. 应用镜像完成构建后启动业务服务与 Nginx；
5. Nginx 仅在 Gateway healthcheck 成功后启动。

### 4.3 本机完整测试

在基础设施已启动、根目录 `.env` 已填写的本机环境中执行：

```bash
mvn clean test
```

Admin 与 Portal 的真实 MySQL 集成测试会在 `test` profile 下读取根目录 `.env`，并通过 `127.0.0.1` 连接 Docker 发布的 MySQL、Redis 和 RabbitMQ，不依赖 Nacos。

Search 的 Elasticsearch 仓库集成测试默认跳过。需要额外执行时：

```bash
RUN_ELASTICSEARCH_INTEGRATION_TESTS=true mvn clean test
```

## 5. 健康检查与最小 Smoke Test

### 5.1 容器健康状态

```bash
docker compose -f docker-compose.app.yml ps
```

预期 MySQL、Redis、RabbitMQ、Elasticsearch 和七个业务服务均显示为 `healthy`；Nginx 显示为 `Up`，并且只暴露 `127.0.0.1:8088->80`。

### 5.2 公开搜索

```bash
curl --silent --show-error \
  "http://127.0.0.1:8088/search/products?keyword=%E6%89%8B%E6%9C%BA&pageNum=1&pageSize=10" |
  jq '{code, message, total: .data.total}'
```

预期业务码为 `200`。即使没有命中商品，空结果也是有效响应。

### 5.3 后台与监控访问

- 使用 ADMIN 身份登录后，请求 `/admin/brands?pageNum=1&pageSize=5`，预期业务码为 `200`；
- 未携带 ADMIN 登录态访问 `/monitor/`，预期由 Gateway 返回未认证或无权限响应；
- 携带 ADMIN Bearer token 访问 `/monitor/`，预期由 Monitor 自身发起表单登录或 HTTP Basic 认证挑战。

所有业务访问都应使用 `http://127.0.0.1:8088`，不得以宿主机端口直连内部服务。

## 6. 数据持久化、备份与恢复

### 6.1 Docker 数据卷

基础设施数据保存在以下 Docker 卷中：

- `mysql-data`
- `redis-data`
- `nacos-data`
- `rabbitmq-data`
- `elasticsearch-data`

日常重启使用 `docker compose up -d` 或 `docker compose restart`。不要执行 `docker compose down -v`，除非明确决定删除全部基础设施数据。

### 6.2 MySQL 逻辑备份

执行：

```bash
bash deploy/scripts/backup-mysql.sh
```

备份会生成在 `backups/mysql/`，文件为 gzip 压缩的 SQL 转储。该目录被 Git 忽略，必须由部署环境自行保存并纳入备份策略。

### 6.3 MySQL 恢复

恢复会删除并重建 `.env` 中 `MYSQL_DATABASE` 指定的数据库。恢复前先停止业务服务，保留基础设施运行：

```bash
docker compose -f docker-compose.app.yml stop
bash deploy/scripts/restore-mysql.sh backups/mysql/<backup-file>.sql.gz --apply
docker compose -f docker-compose.app.yml up -d
```

恢复完成后执行健康检查与 smoke test。不要对不确认来源的备份文件执行恢复。

### 6.4 Nacos 配置恢复

在 Nacos 容器启动后执行：

```bash
bash deploy/scripts/import-nacos-dev.sh --apply
```

该脚本从 `deploy/nacos/dev/` 导入七个服务的 dev 配置。模板只保存环境变量占位符；实际密码、Token 与密钥仍由 `.env` 在应用启动时注入。

## 7. 发布、回滚与排障

### 7.1 镜像标签与发布

业务镜像使用 `MALL_IMAGE_TAG` 选择运行版本，默认值为 `1.0.0`，不得使用 `latest`。

构建某个明确标签的业务镜像：

```bash
MALL_IMAGE_TAG=<release-tag> \
  docker compose -f docker-compose.app.yml build
```

发布前记录当前运行的标签，并确认目标标签的七个业务镜像均已构建或已拉取到部署主机。

### 7.2 镜像版本回滚

将 `<previous-tag>` 替换为已验证可用的历史标签：

```bash
MALL_IMAGE_TAG=<previous-tag> \
  docker compose -f docker-compose.app.yml up -d --no-build --force-recreate
```

回滚只重建应用服务与 Nginx，不删除 Docker 数据卷。完成后执行容器健康检查和第 5 节 smoke test。

### 7.3 日志与 Monitor 排障入口

查看某个应用服务的最近日志：

```bash
docker compose -f docker-compose.app.yml logs --tail=200 <service-name>
```

常用服务名包括 `mall-gateway`、`mall-admin`、`mall-portal`、`mall-search` 和 `mall-monitor`。

Monitor 入口为：

```text
http://127.0.0.1:8088/monitor/
```

Monitor 需要先通过 Gateway 的 ADMIN 校验，再完成 Monitor 本地表单登录或 Basic Auth。依赖健康状态、线程转储和指标可从 Monitor 中查看。