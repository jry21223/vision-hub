# 部署说明

## 一、后端部署

### 1.1 前置条件

- Docker >= 24.0 + Docker Compose v2
- 可用端口：3000（API）、5432（Postgres）、6379（Redis）、9092（Kafka）

### 1.2 环境变量

复制并填写：

```bash
cp backend/.env.example backend/.env
```

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `APP_PORT` | `3000` | HTTP 监听端口 |
| `DATABASE_URL` | *必填* | `postgres://user:pass@host:5432/db?sslmode=disable` |
| `REDIS_ADDR` | `localhost:6379` | Redis 地址 |
| `REDIS_PASSWORD` | 空 | Redis 密码 |
| `REDIS_DB` | `0` | Redis 数据库编号 |
| `KAFKA_BROKERS` | `localhost:9092` | Broker 列表（逗号分隔） |
| `KAFKA_TOPIC` | `sensor_events` | 事件 Topic |

### 1.3 Docker Compose 一键启动

后端目录已提供一键部署脚本与 compose 文件：

- `backend/deploy.sh`
- `backend/docker-compose.deploy.yml`
- `backend/Dockerfile`

首次启动：

```bash
bash backend/deploy.sh
```

脚本会自动：

1. 检查 Docker / Docker Compose 是否可用
2. 若 `backend/.env` 不存在，则从 `backend/.env.example` 复制
3. 自动把容器内地址改写为：
   - `DATABASE_URL=postgres://visionhub:visionhub@postgres:5432/visionhub?sslmode=disable`
   - `REDIS_ADDR=redis:6379`
   - `KAFKA_BROKERS=redpanda:9092`
4. 执行 `docker compose up -d --build`
5. 轮询 `http://localhost:3000/healthz` 直到服务可用

如需手动执行，对应命令为：

```bash
docker compose -f backend/docker-compose.deploy.yml --env-file backend/.env up -d --build
```

健康检查：
```bash
curl http://localhost:3000/healthz
# {"status":"ok"}
```

接口冒烟测试：
```bash
# 跌倒事件上报
curl -s -X POST http://localhost:3000/api/v1/event/report \
  -H "Content-Type: application/json" \
  -d '{"device_id":"badge-001","imu_magnitude":15.3,"latitude":31.23,"longitude":121.47}'
# {"message":"fall event queued","success":true}

# 药品识别（OCR/LLM 当前为 stub）
curl -s -X POST http://localhost:3000/api/v1/recognize/medicine \
  -H "Content-Type: application/json" \
  -d '{"device_id":"badge-001","image_base64":"<base64>"}'
# {"success":true,"data":{"tts_text":"...","cached":false}}
```

查看持久化结果（PostgreSQL）：
```bash
docker exec visionhub-postgres psql -U visionhub -d visionhub \
  -c "SELECT id, device_id, event_type, detail, created_at FROM event_logs ORDER BY created_at DESC LIMIT 10;"
```

停止服务：

```bash
docker compose -f backend/docker-compose.deploy.yml --env-file backend/.env down
```

### 1.4 Compose 内容说明

`backend/docker-compose.deploy.yml` 会启动以下服务：

- `postgres`：PostgreSQL 16
- `redis`：Redis 7
- `redpanda`：Kafka 兼容消息队列
- `backend`：Go Fiber 服务

### 1.5 后端 Dockerfile

在 `backend/` 目录创建 `Dockerfile`：

```dockerfile
FROM golang:1.22-alpine AS builder
WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN go build -o visionhub-backend ./main.go

FROM alpine:latest
RUN apk --no-cache add ca-certificates tzdata
ENV TZ=Asia/Shanghai
WORKDIR /app
COPY --from=builder /app/visionhub-backend .
EXPOSE 3000
CMD ["./visionhub-backend"]
```

---

## 二、Android APK 构建

### 2.1 前置条件

- JDK 17（必须，AGP 不支持 JDK 24）
- Android SDK（API 35 / Build Tools 35+）
- `ANDROID_HOME` 已配置

### 2.2 设置 JDK 17

```bash
export JAVA_HOME="/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
```

### 2.3 构建命令

```bash
# Debug APK（用于开发调试）
./gradlew assembleDebug
# 输出：app/build/outputs/apk/debug/app-debug.apk

# 直接安装到已连接设备
./gradlew installDebug

# 单元测试
./gradlew testDebugUnitTest

# Lint 检查
./gradlew lintDebug

# 指定测试类
./gradlew testDebugUnitTest \
  --tests "com.example.myapplication.YoloInferenceManagerTest"
```

### 2.4 Release APK

在 `app/build.gradle.kts` 中配置签名后：

```bash
./gradlew assembleRelease
# 输出：app/build/outputs/apk/release/app-release.apk
```

---

## 三、首次启动权限说明

启动后 Android 系统会自动弹出权限申请对话框：

| 权限 | 用途 | 触发时机 |
|------|------|----------|
| `POST_NOTIFICATIONS` | 前台服务通知 | 应用启动（Android 13+） |
| `CALL_PHONE` | 跌倒时紧急呼叫 | 点击"紧急求助"按钮 |
| `CAMERA` | 拍照识别药品 | 点击"点击拍照识别"按钮 |
| `RECORD_AUDIO` | 语音输入查询 | 点击历史页面麦克风按钮 |

---

## 四、TCP 客户端接入

Android 应用在端口 **8080** 监听，格式见 [ARCHITECTURE.md](ARCHITECTURE.md)。

测试示例（Python）：

```python
import socket, json, time

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.connect(("192.168.x.x", 8080))  # Android 设备 IP

# 发送传感器帧
frame = {"radar_dist": 80, "ax": 0.1, "ay": 9.8, "az": 0.3,
         "btn_a": 0, "btn_b": 0}
sock.send((json.dumps(frame) + "\n").encode())
```
