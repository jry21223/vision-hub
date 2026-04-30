# 暖阳智视 VisionHub

> 面向老年人与视障群体的可穿戴 AI 视觉中枢——ESP32 胸牌采集、Android 手机端侧推理、Go 后端事件持久化三层协作。

---

## 目录

- [产品定位](#产品定位)
- [整体架构](#整体架构)
- [各层详解](#各层详解)
  - [ESP32 设备端](#1-esp32-设备端)
  - [Android 手机端](#2-android-手机端)
  - [Go 后端](#3-go-后端)
  - [AI 模型管线](#4-ai-模型管线)
- [数据流](#数据流)
- [分支与目录结构](#分支与目录结构)
- [快速开始](#快速开始)
- [当前状态](#当前状态)

---

> **最新版本 v1.2.0** — [查看 Release 页面](../../releases/latest) 直接下载 APK

---

## 产品定位

暖阳智视是一套**轻量可穿戴陪护系统**，目标用户是独居老人和视障人士。核心理念是：

- **本地优先**：跌倒检测和药品目标定位全在手机端侧完成，无网络也能响应
- **语音为主**：所有关键结果通过 TTS 朗读，不要求用户盯屏幕
- **被动感知**：用户无需主动操作，胸牌持续上报传感器数据，手机后台处理
- **云端协同**：告警历史、设备绑定、老人信息均同步至云端，监护人随时查阅
- **实时告警**：跌倒/避障/药品事件通过 FCM 推送到监护人手机，弹窗+铃声双重提醒

```
场景举例：
 老人正在厨房，突然跌倒
   → ESP32 胸牌 IMU 检测到异常加速度
   → Android 跌倒检测状态机确认
   → 自动拨打紧急联系人电话
   → 后端记录跌倒事件（时间 / 位置 / IMU 数值）
   → FCM 推送通知到监护人手机（弹窗 + 铃声）
```

```
场景举例：
 老人拿着一盒药，不认识药名
   → 手机摄像头拍照
   → YOLO 定位药盒区域和文字区域
   → OCR 读取文字（后续接入云端 API）
   → LLM 生成用药建议
   → TTS 朗读给用户听
```

```
场景举例：
 监护人注册账号后绑定老人的 ESP32 胸牌
   → 搜索在线设备列表
   → 一键绑定设备
   → 填写老人信息（姓名、年龄、病史）
   → 实时查看设备 GPS 位置和历史告警记录
```

---

## 整体架构

```
┌─────────────────────┐         ┌────────────────────────────────────────┐
│   ESP32-S3 胸牌      │  TCP    │            Android 手机端               │
│                     │ :8080   │                                        │
│  IMU (ax/ay/az)     │ ──────▶ │  VisionHubService (前台服务)            │
│  雷达测距            │         │  ├─ VisionTcpServer  ← TCP 接收        │
│  摄像头 JPEG         │         │  │    └─ VisionStreamDecoder            │
│  按钮 A/B            │ ◀────── │  ├─ FallDetectionEngine  状态机        │
│  蜂鸣器/闪光灯       │ 命令下发 │  ├─ YoloInferenceManager  ONNX 推理   │
└─────────────────────┘         │  └─ EmergencyCallHandler  紧急拨号     │
                                 │                                        │
                                 │  VisionDataHub (状态中枢)               │
                                 │  Jetpack Compose UI (6 个页面)         │
                                 └──────────────┬─────────────────────────┘
                                                │  HTTP REST + FCM
                                                ▼
                                 ┌──────────────────────────────┐
                                 │        Go 后端  :3000         │
                                 │  Fiber v2  REST API           │
                                 │  ├─ POST /api/v1/auth/*       │
                                 │  ├─ GET/POST /api/v1/alerts   │
                                 │  ├─ GET/POST /api/v1/devices  │
                                 │  ├─ GET/POST elderly-profile  │
                                 │  └─ POST /event/report        │
                                 └──────┬───────────────┬────────┘
                                        │               │
                                 ┌──────▼──────┐  ┌─────▼──────────┐
                                 │    Redis    │  │  Redpanda       │
                                 │   (缓存)    │  │  (Kafka 兼容)   │
                                 └─────────────┘  └─────┬──────────┘
                                                        │ Consumer
                                                 ┌──────▼──────────┐
                                                 │   PostgreSQL    │
                                                 │  event_logs 表  │
                                                 └─────────────────┘
```

---

## 各层详解

### 1. ESP32 设备端

胸牌硬件，持续采集传感器数据并通过 TCP 推送到手机。

| 传感器 | 用途 |
|--------|------|
| IMU (ax/ay/az) | 跌倒检测 |
| 雷达测距 | 障碍物距离（未来避障语音播报） |
| 摄像头 | 拍摄 JPEG 图像帧用于药品识别 |
| 按钮 A/B | 用户主动触发操作（预留） |

**TCP 发送格式**（同一连接中混合两种帧）：

```
传感器 JSON（以 \n 结尾）：
{"radar_dist":80,"ax":0.12,"ay":9.81,"az":0.05,"btn_a":0,"btn_b":0}\n

图像帧（原始 JPEG 字节，SOI = 0xFF 0xD8，EOI = 0xFF 0xD9）：
[FF D8 FF ... FF D9]
```

**Android → ESP32 命令下发**（`VisionTcpServer` 广播，以 `\n` 结尾）：

| 命令 | 触发 | 设备行为 |
|------|------|--------|
| `BUZZER:ON\n` | 设备页蜂鸣器按钮 | 蜂鸣器鸣响 |
| `FLASHLIGHT:TOGGLE\n` | 设备页灯光按钮 | 闪光灯切换 |

---

### 2. Android 手机端

**包结构**：`app/src/main/java/com/example/myapplication/`

#### 服务层（`VisionHubService`）

前台 Service，持有 `WakeLock`，统一编排所有后台逻辑：

```
VisionHubService
  ├─ VisionTcpServer          TCP :8080，接受 ESP32 连接
  │    └─ VisionStreamDecoder 逐字节解析混合流，识别 JSON / JPEG 帧
  ├─ FallDetectionEngine      基于 IMU 幅值的四状态跌倒状态机
  │    └─ FallDetectionConfig 运行时热重载（低/中/高灵敏度）
  ├─ EmergencyCallHandler     跌倒确认后触发 ACTION_CALL
  └─ LocalVisionAnalyzer
       └─ YoloInferenceManager ONNX Runtime 本地推理
```

#### 跌倒检测状态机

```
IDLE ──(幅值 < freeFallThreshold)──▶ DETECTING
  ▲                                      │
  │                             超时(impactWindowMs)
  │                                      │
  └──────────────────────────────── 回 IDLE
                                         │
                           幅值 ≥ impactThreshold
                                         │
                                         ▼
                               FALL_CONFIRMED ──▶ 触发紧急拨号
                                         │
                               进入 COOLDOWN (10s)
                                         │
                                    ▶ 回 IDLE
```

灵敏度预设（可在 App 内切换）：

| 档位 | freeFallThreshold | impactThreshold | 窗口 |
|------|-------------------|-----------------|------|
| 低（较难触发） | 3.0 m/s² | 22.0 m/s² | 800ms |
| 中（默认）     | 2.5 m/s² | 18.0 m/s² | 1000ms |
| 高（较易触发） | 2.0 m/s² | 14.0 m/s² | 1200ms |

#### 状态中枢（`VisionDataHub`）

全局单例 object，所有状态以 Flow 形式暴露，UI 层直接 `collectAsStateWithLifecycle`：

| Flow | 类型 | 说明 |
|------|------|------|
| `connectionState` | `StateFlow` | STOPPED / STARTING / LISTENING / CONNECTED / ERROR |
| `fallAlertState` | `StateFlow` | IDLE / DETECTING / FALL_CONFIRMED / EMERGENCY_CALLING |
| `localVisionState` | `StateFlow` | IDLE / PROCESSING / FRAME_ANALYZED / ERROR |
| `obstacleEnabled` | `StateFlow` | 避障开关（控制是否触发 YOLO 推理） |
| `fallConfig` | `StateFlow` | 跌倒检测参数，Service 热重载 |
| `emergencyContact` | `StateFlow` | 紧急联系人号码，持久化至 SharedPreferences |
| `isLoggedIn` | `StateFlow` | 登录状态，控制 Auth Gate |
| `elderlyProfile` | `StateFlow` | 老人信息，登录后从云端加载 |
| `cloudAlerts` | `StateFlow` | 云端告警列表（AlertRecord） |
| `boundDevice` | `StateFlow` | 当前绑定的在线设备 |
| `deviceLatitude` | `StateFlow` | 设备 GPS 纬度（来自 SensorPacket） |
| `deviceLongitude` | `StateFlow` | 设备 GPS 经度（来自 SensorPacket） |
| `sensorPackets` | `SharedFlow` | IMU / 雷达传感器数据 |
| `imageFrames` | `SharedFlow` | JPEG 帧字节（发布前已 `copyOf` 防并发修改） |
| `deviceCommands` | `SharedFlow` | 下发给 ESP32 的命令字符串 |

#### Auth 层

App 首次启动显示登录页，未登录状态下不渲染主界面：

```
LoginScreen  ──注册──▶  RegisterScreen
     │
     └─忘记密码──▶  ForgotPasswordScreen
     │
     └─登录成功──▶  VisionHubScreen（主界面）
                        └─退出登录──▶  LoginScreen
```

JWT 存储于 `visionhub_prefs`（SharedPreferences），`AuthTokenHolder` 持有运行时 token 供 OkHttp 拦截器自动注入。

#### UI 层（Jetpack Compose）

9 个页面，无 ViewModel，直接读取 VisionDataHub：

| 页面 | 入口 | 主要功能 |
|------|------|--------|
| `LoginScreen` | 冷启动（未登录） | 手机号 + 密码登录，跳转注册/找回密码 |
| `RegisterScreen` | 登录页链接 | 姓名、手机号、密码注册 |
| `ForgotPasswordScreen` | 登录页链接 | 手机号 + 验证码重置密码 |
| `HomeScreen` | 底栏「首页」 | 实时避障 / 药品识别快捷入口，连接状态 |
| `ObstacleScreen` | 底栏「避障」 | 避障开关、音量调节、跌倒灵敏度设置 |
| `RecognitionScreen` | 底栏「识别」 | 拍照触发 YOLO 推理，展示识别结果，TTS 朗读 |
| `DeviceScreen` | 底栏「设备」 | ESP32 绑定/解绑、GPS 位置、蜂鸣器/灯光控制 |
| `ProfileScreen` | 底栏「我的」 | 用户信息、紧急联系人、老人信息管理入口、退出登录 |
| `HistoryScreen` | 「我的」浮层 | 云端 + 本地告警历史，语音搜索过滤 |
| `ElderlyProfileScreen` | 「我的」→「老人信息」 | 老人姓名/年龄/性别/病史，同步云端 |

持久化（SharedPreferences，同一文件 `visionhub_prefs`）：

| 类 | 存储内容 |
|----|--------|
| `AuthPreference` | JWT token、FCM token |
| `ElderlyPreference` | 老人信息本地缓存 |
| `VolumePreference` | TTS 音量 |
| `SensitivityPreference` | 跌倒检测灵敏度 |
| `ContactPreference` | 紧急联系人电话 |

---

### 3. Go 后端

**目录**：`backend/`，详细文档见 [backend/README.md](backend/README.md)。

```
POST /api/v1/event/report        跌倒事件上报 → Kafka → PostgreSQL
POST /api/v1/recognize/medicine  药品识别 → Redis 缓存 → OCR stub → LLM stub
GET  /healthz                    健康检查
```

技术栈：Go 1.22 · Fiber v2 · GORM · PostgreSQL 16 · Redis 7 · Redpanda（Kafka 兼容）

---

### 4. AI 模型管线

#### 本地推理（已实现）

```
JPEG 帧
  │
  ▼  YoloInferenceManager（ONNX Runtime）
  │  1. BitmapFactory 解码 JPEG
  │  2. 缩放到 640×640
  │  3. RGB → CHW Float32 Tensor（÷255 归一化）
  │  4. OrtSession.run()
  │  5. 后处理：置信度过滤（≥0.35）、类别映射
  │
  ├─ Medicine_Box 检测 → 药盒位置
  └─ Text_Region 检测 → 裁出最高置信度文字区域 → LocalVisionState.summary
```

模型文件：`app/src/main/assets/yolo11n.onnx`（同时镜像在 `yolo/onnx/yolo11n.onnx`）

#### 云端管线（OCR + LLM，待接入）

```
Text_Region 裁图（JPEG bytes）
  │
  ▼  POST /api/v1/recognize/medicine
  │
  ├─ simulateOCR()   ← 当前为 stub，需替换为阿里云/腾讯云 OCR API
  │    └─ 返回文字字符串
  │
  ├─ Redis GET（MD5 缓存键）
  │    └─ 命中 → 直接返回，节省 LLM 调用
  │
  └─ simulateLLM()   ← 当前为 stub，需替换为 Qwen/Claude/GPT-4o
       └─ 返回用药建议 tts_text → Android TTS 朗读
```

#### MobileNetV3 特征匹配（规划中）

`yolo/onnx/mobilenet_v3_feat.onnx` + `yolo/special_items.json` 是预留的商品识别管线：
- YOLO 检出 Medicine_Box 区域后裁图
- MobileNetV3 提取 embedding
- 与 `special_items.json` 中的商品特征向量做余弦相似度匹配
- 识别具体商品（如「相印餐巾纸正面」）

目前**尚未接入**应用运行时，仅留有模型文件和特征数据库。

---

## 数据流

### 跌倒事件完整链路

```
ESP32 IMU 采样（~50Hz）
  └─▶ TCP JSON 帧 → VisionStreamDecoder → SensorPacket
        └─▶ VisionDataHub.sensorPackets（SharedFlow）
              └─▶ VisionHubService.observeSensorPackets()
                    └─▶ FallDetectionEngine.process()
                          ├─ IDLE / DETECTING → 更新 FallAlertState UI
                          └─ FALL_CONFIRMED
                                ├─▶ EmergencyCallHandler.triggerEmergencyCall()
                                │    └─ 拨打 VisionDataHub.emergencyContact 号码
                                └─▶ [待接入] POST /api/v1/event/report
                                              └─▶ Kafka → Consumer → PostgreSQL
```

### 药品识别完整链路

```
用户点击「拍照识别」
  └─▶ ActivityResultContracts.TakePicture()
        └─▶ FileProvider URI → Dispatchers.IO 读取 JPEG bytes
              └─▶ VisionDataHub.publishImageFrame()
                    └─▶ VisionHubService.observeImageFrames()
                          └─▶ [gated: obstacleEnabled]
                                └─▶ YoloInferenceManager.analyze()
                                      ├─ ONNX 推理 → Medicine_Box / Text_Region
                                      └─▶ LocalVisionState（summary 字符串）
                                            ├─▶ RecognitionScreen 展示
                                            ├─▶ TTS 朗读
                                            └─▶ [待接入] POST /recognize/medicine
                                                          └─▶ OCR → LLM → tts_text
```

---

## 分支与目录结构

```
main              基线分支
android           Android 应用开发分支（当前）
backend           Go 后端开发分支

仓库目录：
├── .github/workflows/
│   └── release.yml            Push tag → 自动构建并发布 APK Release
├── app/                       Android 应用源码
│   └── src/main/java/com/example/myapplication/
│       ├── MainActivity.kt        Activity 入口 + Auth Gate + 全局 UI 编排
│       ├── VisionHubService.kt    前台服务
│       ├── VisionHubFirebaseMessagingService.kt  FCM 推送处理
│       ├── VisionDataHub.kt       全局 Flow 状态中枢
│       ├── VisionTcpServer.kt     TCP 服务器
│       ├── VisionStreamDecoder.kt 混合流解析器
│       ├── FallDetectionEngine.kt 跌倒检测状态机
│       ├── ElderlyProfile.kt      老人信息数据类
│       ├── YoloInferenceManager.kt ONNX 推理
│       ├── EmergencyCallHandler.kt 紧急拨号
│       ├── api/
│       │   ├── AuthApi.kt         登录 / 注册 / 找回密码 / FCM token 上传
│       │   ├── ElderlyApi.kt      老人信息 CRUD
│       │   ├── DeviceApi.kt       在线设备列表 / 绑定 / 解绑
│       │   ├── AlertApi.kt        云端告警历史分页查询
│       │   └── RetrofitClient.kt  OkHttp + JWT 拦截器 + API 实例
│       ├── auth/
│       │   └── AuthTokenHolder.kt 运行时 JWT 持有（OkHttp 拦截器读取）
│       ├── navigation/            页面导航枚举
│       ├── ui/
│       │   ├── screens/           9 个页面 Composable（含 Auth 三件套）
│       │   ├── components/        可复用 UI 组件（DeviceBindingCard 等）
│       │   ├── AppColors.kt
│       │   └── AppModels.kt
│       └── util/
│           ├── AuthPreference.kt  JWT / FCM token 持久化
│           ├── ElderlyPreference.kt 老人信息本地缓存
│           └── ...                其他 SharedPreferences 工具类
├── backend/                   Go 后端（见 backend/README.md）
├── yolo/
│   ├── onnx/yolo11n.onnx          检测模型（源文件）
│   ├── onnx/mobilenet_v3_feat.onnx 特征提取模型（规划中）
│   └── special_items.json         商品特征向量库
├── ARCHITECTURE.md            系统架构详细文档
├── DEPLOYMENT.md              构建与部署指南
└── README.md                  本文件
```

---

## 快速开始

### 后端（Docker 一键启动）

```bash
# 需要 Docker Desktop 运行中
bash backend/deploy.sh
```

健康检查：`curl http://localhost:3000/healthz` → `{"status":"ok"}`

详见 [backend/README.md](backend/README.md)。

### Android APK 构建

```bash
export JAVA_HOME="/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

./gradlew assembleDebug          # 构建 debug APK
./gradlew installDebug           # 安装到已连接设备
./gradlew testDebugUnitTest      # 运行单元测试
```

首次运行时 App 会请求权限：

| 权限 | 用途 |
|------|------|
| `POST_NOTIFICATIONS` | 前台服务通知 + FCM 告警推送 |
| `CALL_PHONE` | 跌倒后紧急拨号 |
| `CAMERA` | 拍照触发药品识别 |
| `RECORD_AUDIO` | 语音搜索历史记录 |

### 直接下载 APK

前往 [Releases 页面](../../releases/latest) 下载最新版 APK（由 GitHub Actions 自动构建）。

---

## 当前状态

### 已实现

**设备侧**
- ESP32 TCP 接入：混合流解析（JSON 传感器帧 + JPEG 图像帧）
- TCP 命令下发：蜂鸣器 (`BUZZER:ON`) / 闪光灯 (`FLASHLIGHT:TOGGLE`)
- SensorPacket 支持可选 GPS 经纬度字段

**安全与健康监护**
- 跌倒检测状态机（4 状态，3 档灵敏度，运行时热切换）
- 自动紧急拨号（联系人号码 App 内可编辑 + 持久化 + 云端同步）
- 本地 YOLO 药品目标检测（`Medicine_Box` / `Text_Region`）

**用户与账号**
- 用户注册 / 登录 / 找回密码（JWT 鉴权，SharedPreferences 持久化）
- OkHttp `AuthInterceptor` 自动注入 Bearer token

**监护人功能**
- 老人信息管理（姓名、年龄、性别、病史，云端同步）
- 在线设备搜索 + 一键绑定 / 解绑
- 云端告警历史（摔倒 / 避障 / 药品，分页拉取）
- GPS 位置实时展示（`GuardianLocationCard`）

**推送**
- FCM 推送服务（`VisionHubFirebaseMessagingService`）
- 高重要性告警通知渠道（弹窗 + 振动 + 铃声）

**工程**
- Compose 全功能 UI（9 页面 + TTS 朗读 + 语音搜索）
- Go 后端：事件上报 / 药品识别接口 + Kafka 异步持久化 + Redis 缓存
- GitHub Actions 自动构建并发布 APK Release

### 待接入

| 功能 | 说明 |
|------|------|
| FCM 完整激活 | 将 `google-services.json` 放入 `app/`，移除 manifest 中的 `auto_init_enabled=false` |
| 云端 OCR | 替换 `simulateOCR()` stub，接入阿里云/腾讯云 OCR |
| 云端 LLM | 替换 `simulateLLM()` stub，接入 Qwen/Claude/GPT-4o |
| ESP32 命令解析 | 固件侧实现 `BUZZER:ON` / `FLASHLIGHT:TOGGLE` 响应 |
| MobileNetV3 商品匹配 | 接入 `mobilenet_v3_feat.onnx` + `special_items.json` 管线 |
| TCP 鉴权 | 应用层握手，防止未授权设备接入 |
