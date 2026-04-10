# VisionHub Backend

VisionHub Backend 是暖阳智视项目的服务端部分，负责事件接收、药品识别接口、缓存、消息投递与持久化。

## 技术栈

- Go
- Fiber v2
- GORM
- PostgreSQL
- Redis
- Kafka / Redpanda

## 当前能力

- `GET /healthz` 健康检查
- `POST /api/v1/event/report` 跌倒事件上报
- `POST /api/v1/recognize/medicine` 药品识别占位接口
- Redis 结果缓存
- Kafka 事件写入与消费者持久化 PostgreSQL

## 目录结构

- `backend/main.go` — 服务入口、依赖初始化、HTTP 启动与优雅退出
- `backend/config/` — 环境变量加载
- `backend/handler/` — HTTP 处理器
- `backend/service/` — 业务逻辑
- `backend/infra/` — PostgreSQL / Redis / Kafka 基础设施封装
- `backend/consumer/` — Kafka 消费者
- `backend/model/` — GORM 模型
- `backend/route/` — 路由注册

## 环境变量

复制示例配置：

```bash
cp backend/.env.example backend/.env
```

关键变量：

- `APP_PORT`：HTTP 端口，默认 `3000`
- `DATABASE_URL`：PostgreSQL 连接串，必填
- `REDIS_ADDR`：Redis 地址，默认 `localhost:6379`
- `REDIS_PASSWORD`：Redis 密码，可空
- `REDIS_DB`：Redis DB 编号，默认 `0`
- `KAFKA_BROKERS`：Broker 列表，默认 `localhost:9092`
- `KAFKA_TOPIC`：事件 Topic，默认 `sensor_events`

## 本地启动

```bash
cd backend
go mod tidy
go run ./main.go
```

启动后默认监听：

- `http://localhost:3000`
- 健康检查：`GET /healthz`

## Docker 部署

推荐在仓库根目录使用 `docker compose` 启动 PostgreSQL、Redis、Redpanda 和 backend 服务。部署示例见主仓库文档中的 `DEPLOYMENT.md`。

## 接口概览

### 健康检查

```http
GET /healthz
```

返回：

```json
{"status":"ok"}
```

### 事件上报

```http
POST /api/v1/event/report
```

用于接收设备/客户端上报的事件数据，并写入 Kafka / PostgreSQL 流程。

### 药品识别

```http
POST /api/v1/recognize/medicine
```

当前为占位实现，包含缓存与消息发布链路。

## 开发说明

- 服务启动时会自动连接 PostgreSQL、Redis、Kafka
- PostgreSQL 启动后会自动执行 `Device` 和 `EventLog` 模型迁移
- Kafka 消费者会在后台 goroutine 中持续消费 `sensor_events`

## 相关分支

- `android`：Android 客户端主线
- `backend`：Go 后端主线
