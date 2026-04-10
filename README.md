# 暖阳智视（VisionHub）

暖阳智视是一个面向可穿戴陪护场景的视觉中枢项目：由 **ESP32-S3 设备端** 采集传感器与图像数据，发送到 **Android 手机端** 完成本地跌倒检测、障碍感知与药品识别，再按需接入 **Go 后端** 做事件上报、缓存、消息队列和持久化。

当前仓库已经拆分为两个主要开发分支：

- `android`：Android 应用主线
- `backend`：Go 后端主线

## 仓库内容

本仓库当前目录主要承载 Android 端实现，同时保留后端目录与模型资源：

- `app/`：Android 应用（Compose UI、前台服务、TCP 接收、本地视觉）
- `backend/`：Go 后端（Fiber + Redis + Kafka + PostgreSQL）
- `yolo/`：模型与训练/导出相关资源
- `ARCHITECTURE.md`：整体架构设计
- `DEPLOYMENT.md`：部署说明与构建说明

## 系统总览

```text
ESP32-S3 wearable badge  --TCP:8080-->  Android phone (VisionHub)
                                            |
                                            | HTTP
                                            v
                                      Go backend (:3000)
                                            |
                              Redis / Kafka(Redpanda) / PostgreSQL
```

核心链路：

1. 设备端通过 TCP 向 Android 手机发送传感器 JSON 和 JPEG 图像帧
2. Android 前台服务接收数据并驱动跌倒检测、本地 YOLO 推理与语音/告警能力
3. 必要时 Android 或其他客户端接入后端 API 做事件上报与药品识别结果处理
4. 后端通过 Redis 做缓存、Kafka 做事件流转、PostgreSQL 做持久化

## Android 端能力

### 当前已实现

- 前台服务常驻运行
- TCP `8080` 监听设备连接
- 混合流解析：
  - 换行结尾的 JSON 传感器帧
  - 原始 JPEG 二进制图像帧
- 跌倒检测状态机
- 本地 ONNX Runtime 推理
- Compose 多页面 UI
- 避障开关、音量调节、跌倒灵敏度切换
- 拍照识别入口
- 紧急求助确认弹窗与拨号权限流程
- 语音输入入口

### 主要 Android 模块

- `MainActivity.kt`：应用入口、权限引导、主界面承载
- `VisionHubService.kt`：前台服务，负责运行期编排
- `VisionTcpServer.kt`：监听 TCP 连接
- `VisionStreamDecoder.kt`：解析传感器帧与 JPEG 帧
- `VisionDataHub.kt`：全局 `StateFlow` / `SharedFlow` 状态中枢
- `FallDetectionEngine.kt`：跌倒检测状态机
- `LocalVisionAnalyzer.kt`：本地视觉分析入口
- `YoloInferenceManager.kt`：ONNX 模型推理与后处理

## 后端能力

后端位于 `backend/`，技术栈如下：

- Go
- Fiber v2
- GORM
- Redis
- Kafka / Redpanda
- PostgreSQL

### 当前接口

- `GET /healthz`：健康检查
- `POST /api/v1/event/report`：跌倒事件上报
- `POST /api/v1/recognize/medicine`：药品识别接口

## 快速开始

## 1. 准备环境

### Android 构建环境

- JDK 17
- Android SDK（API 35 / Build Tools 35+）

先切到 JDK 17：

```bash
export JAVA_HOME="/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
```

### 后端部署环境

- Docker >= 24
- Docker Compose v2

## 2. 启动后端

复制环境变量：

```bash
cp backend/.env.example backend/.env
```

然后在仓库根目录按 `DEPLOYMENT.md` 中的示例准备 `docker-compose.yml`，启动：

```bash
docker compose up -d
```

健康检查：

```bash
curl http://localhost:3000/healthz
```

默认端口：

- `3000`：后端 HTTP API
- `5432`：PostgreSQL
- `6379`：Redis
- `9092`：Kafka / Redpanda

## 3. 构建 Android APK

```bash
./gradlew assembleDebug
```

输出：

- `app/build/outputs/apk/debug/app-debug.apk`

安装到设备：

```bash
./gradlew installDebug
```

## 4. 运行测试

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
```

单测单类运行示例：

```bash
./gradlew testDebugUnitTest --tests "com.example.myapplication.YoloInferenceManagerTest"
```

## 首次启动权限

应用首次运行时，按功能会请求以下权限：

- `POST_NOTIFICATIONS`：前台服务通知
- `CALL_PHONE`：紧急拨号
- `CAMERA`：拍照识别
- `RECORD_AUDIO`：语音输入

## TCP 接入说明

Android 应用在设备侧监听 `8080` 端口，支持在同一条 TCP 连接中混合发送：

1. 传感器帧：以换行结尾的 JSON
2. 图像帧：原始 JPEG 字节流

### 传感器帧示例

```json
{"radar_dist":80,"ax":0.1,"ay":9.8,"az":0.3,"btn_a":0,"btn_b":0}
```

### Python 发送示例

```python
import json
import socket

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.connect(("192.168.x.x", 8080))

frame = {
    "radar_dist": 80,
    "ax": 0.1,
    "ay": 9.8,
    "az": 0.3,
    "btn_a": 0,
    "btn_b": 0,
}

sock.send((json.dumps(frame) + "\n").encode())
```

## 常用命令

### Android

```bash
./gradlew assembleDebug
./gradlew installDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew connectedDebugAndroidTest
```

### Backend

```bash
docker compose up -d
docker compose down
```

## 文档索引

- [ARCHITECTURE.md](ARCHITECTURE.md) — 查看系统分层、依赖关系、数据流
- [DEPLOYMENT.md](DEPLOYMENT.md) — 查看后端部署、APK 构建、TCP 接入示例

## 当前限制

- OCR/LLM 药品理解链路仍为占位实现
- 设备页部分能力仍是预留入口
- TCP 协议当前未做鉴权与应用层确认
- 紧急联系人配置仍未开放为 App 内可编辑项

## 分支说明

- `android`：Android 应用开发分支
- `backend`：后端服务开发分支

如需查看完整架构与部署细节，请优先阅读：

- [ARCHITECTURE.md](ARCHITECTURE.md)
- [DEPLOYMENT.md](DEPLOYMENT.md)
