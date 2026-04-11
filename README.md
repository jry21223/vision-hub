# VisionHub 后端服务

> Go 后端，负责接收 Android 端上报的跌倒事件和药品识别请求，通过 Kafka 异步落库，Redis 缓存药品识别结果。

---

## 目录

- [快速启动](#快速启动)
- [系统架构](#系统架构)
- [接口文档](#接口文档)
- [数据模型](#数据模型)
- [管道详解](#管道详解)
- [环境变量](#环境变量)
- [本地开发](#本地开发)
- [目录结构](#目录结构)
- [待完成](#待完成)

---

## 快速启动

**前置条件**：Docker Desktop 已安装并运行（无需本地 Go / PostgreSQL / Redis）。

```bash
# 在仓库根目录执行
bash backend/deploy.sh
```

脚本会自动完成：

1. 检测 Docker 是否可用
2. 首次运行时从 `.env.example` 生成 `backend/.env`，并自动写入容器内网地址
3. 拉取镜像，构建 Go 服务镜像，启动全部容器（PostgreSQL → Redis → Redpanda → Backend）
4. 等待 `GET /healthz` 返回 200

预期输出（最后几行）：

```
backend 已启动。
健康检查：{"status":"ok"}
```

### 验证服务

```bash
# 健康检查
curl http://localhost:3000/healthz

# 跌倒事件上报
curl -s -X POST http://localhost:3000/api/v1/event/report \
  -H "Content-Type: application/json" \
  -d '{"device_id":"badge-001","imu_magnitude":15.3,"latitude":31.23,"longitude":121.47}' \
  | python3 -m json.tool

# 药品识别（首次无缓存）
curl -s -X POST http://localhost:3000/api/v1/recognize/medicine \
  -H "Content-Type: application/json" \
  -d '{"device_id":"badge-001","image_base64":"<base64-encoded-jpeg>"}' \
  | python3 -m json.tool
```

### 查看 PostgreSQL 落库结果

```bash
docker exec visionhub-postgres psql -U visionhub -d visionhub \
  -c "SELECT id, device_id, event_type, detail, created_at FROM event_logs ORDER BY created_at DESC LIMIT 10;"
```

### 停止服务

```bash
docker compose -f backend/docker-compose.deploy.yml --env-file backend/.env down

# 同时清除持久化数据卷
docker compose -f backend/docker-compose.deploy.yml --env-file backend/.env down -v
```

---

## 系统架构

```
Android / 其他客户端
        │  HTTP
        ▼
┌───────────────────────────────────────┐
│          Fiber v2  REST API           │
│                                       │
│  GET  /healthz                        │
│  POST /api/v1/event/report            │
│  POST /api/v1/recognize/medicine      │
└──────────┬─────────────────┬──────────┘
           │                 │
    ┌──────▼──────┐   ┌──────▼──────────┐
    │    Redis    │   │  Kafka Writer   │
    │  药品缓存   │   │ (Redpanda:9092) │
    └─────────────┘   └──────┬──────────┘
                             │  topic: sensor_events
                      ┌──────▼──────────┐
                      │  Kafka Consumer │  ← 独立 goroutine
                      │ (group: visionhub-event-consumer)
                      └──────┬──────────┘
                             │  GORM INSERT
                      ┌──────▼──────────┐
                      │   PostgreSQL    │
                      │  event_logs 表  │
                      └─────────────────┘
```

**为什么用 Kafka？**

跌倒事件是高优先级告警，HTTP 响应需要快速返回，不能等数据库写完。Kafka 作为缓冲层解耦了「接收」和「持久化」两个步骤，同时为未来的告警推送、家属通知等下游消费者预留了扩展口。

---

## 接口文档

### `GET /healthz`

健康检查，供 Docker healthcheck 和运维监控使用。

**响应 200**：
```json
{"status": "ok"}
```

---

### `POST /api/v1/event/report`

上报跌倒事件。Android 端跌倒确认后调用，事件发布到 Kafka，Consumer 异步写入 PostgreSQL。

**请求体**：
```json
{
  "device_id":     "badge-001",   // 必填，设备标识
  "imu_magnitude": 15.3,          // 必填，>0，IMU 加速度幅值（m/s²）
  "latitude":      31.23,         // 选填，GPS 纬度
  "longitude":     121.47         // 选填，GPS 经度
}
```

**响应 200**：
```json
{"success": true, "message": "fall event queued"}
```

**响应 400**（校验失败）：
```json
{"success": false, "error": "device_id is required"}
{"success": false, "error": "imu_magnitude must be positive"}
```

**响应 503**（Kafka 不可用）：
```json
{"success": false, "error": "event queue unavailable, please retry: ..."}
```

> Android 端在收到 503 时应做指数退避重试，确保跌倒事件不丢失。

---

### `POST /api/v1/recognize/medicine`

药品识别。Android 端将 YOLO 裁出的文字区域图像（base64）传入，后端走 OCR → Redis 缓存 → LLM 管线，返回可供 TTS 朗读的建议文本。

**请求体**：
```json
{
  "device_id":    "badge-001",        // 必填
  "image_base64": "<base64-jpeg>"     // 必填，JPEG 图像的 base64 编码
}
```

**响应 200**：
```json
{
  "success": true,
  "data": {
    "tts_text": "识别到药品信息：阿莫西林胶囊 0.5g 每次2粒 每日3次 饭后服用。用药建议：...",
    "cached": false   // true 表示命中 Redis 缓存
  }
}
```

**响应 400**（校验失败）：
```json
{"success": false, "error": "device_id and image_base64 are required"}
```

**缓存逻辑**：以 OCR 识别文本的 MD5 为 Redis key，TTL 30 天。同一药盒无论拍几次，只调用一次 LLM。

---

## 数据模型

### `event_logs` 表

所有事件（跌倒 / 药品识别）写入同一张表，`detail` 字段存储事件特定的 JSON。

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint PK | 自增主键 |
| `device_id` | bigint | 关联设备（暂为占位 0，待 Device 表接入） |
| `event_type` | varchar(30) | `fall` 或 `medicine_recognize` |
| `detail` | jsonb | 事件详情（见下） |
| `created_at` | timestamptz | 事件发生时间（来自 Android 端） |
| `deleted_at` | timestamptz | 软删除 |

**fall 事件 detail**：
```json
{"imu_magnitude": 15.3, "latitude": 31.23, "longitude": 121.47}
```

**medicine_recognize 事件 detail**：
```json
{
  "ocr_text": "阿莫西林胶囊 0.5g 每次2粒 每日3次",
  "tts_text": "识别到药品信息：...",
  "cached": false
}
```

### `devices` 表（GORM AutoMigrate 已建表，逻辑待接入）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint PK | 自增主键 |
| `mac_address` | varchar(17) unique | ESP32 MAC 地址 |
| `bound_user_id` | bigint | 关联用户（预留） |
| `status` | varchar(20) | `active` / `inactive` |

---

## 管道详解

### 跌倒事件管道

```
POST /api/v1/event/report
  │
  ├─ 参数校验（device_id 非空，imu_magnitude > 0）
  │
  ├─ EventPublisher.PublishFall()
  │    └─ JSON 序列化为 EventPayload
  │    └─ kafka.Writer.WriteMessages()（Key = device_id，按设备分区保序）
  │    └─ 失败 → 返回 503，Android 侧重试
  │
  └─ 返回 200 {"success": true}

后台 Consumer goroutine（与 HTTP 完全解耦）：
  kafka.Reader.ReadMessage()
    └─ 反序列化 EventPayload
    └─ GORM db.Create(&EventLog{...})
    └─ 单条失败 → 打日志，继续消费（不阻塞管道）
```

### 药品识别管道

```
POST /api/v1/recognize/medicine
  │
  ├─ 参数校验
  │
  ├─ simulateOCR(image_base64)        ← ⚠️ 当前为 stub，需替换
  │    └─ 返回 ocrText 字符串
  │
  ├─ cacheKey = "medicine:ocr:" + MD5(ocrText)
  │
  ├─ Redis GET(cacheKey)
  │    ├─ 命中 → 返回缓存 tts_text，{cached: true}
  │    └─ Redis 异常 → 打日志，继续走 LLM（降级而非失败）
  │
  ├─ simulateLLM(ocrText)             ← ⚠️ 当前为 stub，需替换
  │    └─ 返回 advice 字符串
  │
  ├─ Redis SET(cacheKey, advice, TTL=30d)（best-effort，不影响响应）
  │
  ├─ EventPublisher.PublishMedicine()（best-effort Kafka，不阻塞响应）
  │
  └─ 返回 200 {tts_text: advice, cached: false}
```

---

## 环境变量

文件位置：`backend/.env`（首次运行由 `deploy.sh` 自动生成，不提交到 git）

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `APP_PORT` | `3000` | HTTP 监听端口 |
| `DATABASE_URL` | — | **必填**，PostgreSQL DSN |
| `REDIS_ADDR` | `localhost:6379` | Redis 地址 |
| `REDIS_PASSWORD` | 空 | Redis 密码 |
| `REDIS_DB` | `0` | Redis 数据库编号 |
| `KAFKA_BROKERS` | `localhost:9092` | Broker 地址列表（逗号分隔） |
| `KAFKA_TOPIC` | `sensor_events` | 事件 Topic 名称 |

**Docker Compose 容器内地址**（`deploy.sh` 自动写入）：

```env
DATABASE_URL=postgres://visionhub:visionhub@postgres:5432/visionhub?sslmode=disable
REDIS_ADDR=redis:6379
KAFKA_BROKERS=redpanda:9092
```

---

## 本地开发

不依赖 Docker 时，可在本地直接运行 Go 服务（需自行启动 PostgreSQL / Redis / Redpanda）。

```bash
cd backend

# 安装/同步依赖
go mod tidy

# 复制并编辑环境变量
cp .env.example .env
# 修改 DATABASE_URL / REDIS_ADDR / KAFKA_BROKERS 为本地地址

# 运行
go run ./main.go

# 构建二进制
go build -o visionhub-backend ./main.go
./visionhub-backend
```

**要求**：Go 1.22+

---

## 目录结构

```
backend/
├── main.go                    入口：依赖注入、Fiber 初始化、优雅退出
├── config/
│   └── config.go              从环境变量加载配置（godotenv）
├── infra/
│   ├── postgres.go            GORM 单例（连接池 25/10，自动迁移）
│   ├── redis.go               redis.Client 单例（带重试）
│   └── kafka.go               kafka.Writer 单例 + NewKafkaReader 工厂
├── model/
│   ├── device.go              Device 表（MAC 地址 + 用户绑定）
│   └── event_log.go           EventLog 表（JSONB detail，软删除）
├── service/
│   ├── event.go               EventPublisher：PublishFall / PublishMedicine
│   └── medicine.go            MedicineService：OCR stub → Redis → LLM stub
├── handler/
│   ├── event.go               POST /api/v1/event/report
│   └── medicine.go            POST /api/v1/recognize/medicine
├── consumer/
│   └── event_consumer.go      Kafka Consumer goroutine → GORM INSERT
├── route/
│   └── route.go               路由注册（依赖注入入口）
├── auth/                      预留（JWT / API Key 鉴权）
├── middleware/                预留（限流 / 鉴权中间件）
├── observability/             预留（Prometheus metrics / tracing）
├── integration/               预留（集成测试）
├── Dockerfile                 多阶段构建（golang:1.22-alpine → alpine:3.20）
├── docker-compose.deploy.yml  本地全栈启动（postgres + redis + redpanda + backend）
├── deploy.sh                  一键部署脚本
├── go.mod
├── go.sum
└── .env.example               环境变量模板
```

---

## 待完成

| 优先级 | 功能 | 文件 | 说明 |
|--------|------|------|------|
| 高 | 接入真实 OCR API | `service/medicine.go:simulateOCR` | 替换为阿里云/腾讯云 OCR HTTP 调用 |
| 高 | 接入真实 LLM API | `service/medicine.go:simulateLLM` | 替换为 Qwen/Claude structured prompt |
| 高 | Device 注册逻辑 | `consumer/event_consumer.go:deviceIDFromString` | 替换占位 `return 0`，按 MAC 查询 Device 表 |
| 中 | TCP 鉴权 / API Key | `middleware/` | 防止未授权设备或客户端接入 |
| 中 | 限流中间件 | `middleware/` | 防止异常设备刷接口 |
| 低 | Prometheus 监控 | `observability/` | 接口延迟、Kafka lag、缓存命中率 |
| 低 | 集成测试 | `integration/` | 覆盖 event/report 和 recognize/medicine 完整链路 |
