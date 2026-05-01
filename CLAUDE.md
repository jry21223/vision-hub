# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

This is a single-module Android app for a wearable safety/vision workflow, not a starter-template app anymore.

- Module layout: one Android application module, `:app`
- Build system: Gradle Kotlin DSL with a version catalog in `gradle/libs.versions.toml`
- Package root: `com.example.myapplication`
- UI stack: Jetpack Compose + Material 3
- Runtime shape: app launch starts a foreground service that hosts TCP ingestion, fall detection, and local ONNX vision inference

## Build and test commands

Use JDK 17 for all Gradle commands in this repo. Gradle/AGP task creation fails under the installed JDK 24.

```bash
export JAVA_HOME="/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
```

From the repository root:

```bash
./gradlew assembleDebug
./gradlew installDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew connectedDebugAndroidTest
```

Run one JVM test class:

```bash
./gradlew testDebugUnitTest --tests "com.example.myapplication.VisionStreamDecoderTest"
```

Run one test method:

```bash
./gradlew testDebugUnitTest --tests "com.example.myapplication.YoloInferenceManagerTest.parseDetections supports row first output with objectness"
```

## Architecture

### Runtime flow

The app is built around a long-running foreground service.

1. `MainActivity.kt` starts `VisionHubService` on launch and renders Compose UI from `VisionDataHub` state.
2. `VisionHubService.kt` owns the background runtime:
   - starts the TCP server
   - collects sensor packets for fall detection
   - collects JPEG frames for local vision
   - updates shared UI state through `VisionDataHub`
3. `VisionTcpServer.kt` listens on port `8080`, accepts client sockets, and hands each stream to `VisionStreamDecoder`.
4. `VisionStreamDecoder.kt` parses an interleaved stream of newline-delimited sensor JSON and raw JPEG frames.
5. `VisionDataHub.kt` is the in-memory event/state hub:
   - `SharedFlow` for sensor packets and image frames
   - `StateFlow` for connection state, fall alert state, and local vision state

### Fall detection path

- `FallDetectionEngine.kt` is a small state machine: `IDLE -> DETECTING -> FALL_CONFIRMED -> COOLDOWN`
- It derives fall outcomes from IMU magnitude thresholds defined in `FallDetectionConfig.kt`
- `EmergencyCallHandler.kt` attempts `Intent.ACTION_CALL` using `EmergencyContactConfig`
- UI copy for fall status lives in `MainActivity.kt` helper functions and is driven by `VisionDataHub.fallAlertState`

### Local vision path

- `LocalVisionAnalyzer.kt` is only a thin wrapper around `YoloInferenceManager`
- `YoloInferenceManager.kt` is the ONNX integration point:
  - loads `yolo11n.onnx` from app assets
  - lazily creates and reuses `OrtEnvironment` / `OrtSession`
  - decodes JPEG bytes into `Bitmap`
  - resizes to `640x640`
  - converts pixels to RGB CHW float tensor normalized by `/255f`
  - runs inference off the main thread on `Dispatchers.Default`
  - post-processes detections into `Medicine_Box` / `Text_Region` summaries
  - crops the highest-confidence text region for downstream OCR-style work
- `LocalVisionState.kt` is the UI contract for this path; the UI only renders its `summary` string

### Model expectations

- `app/build.gradle.kts` packages assets from both `src/main/assets` and `src/assets`
- `app/src/main/assets/yolo11n.onnx` — detection model (also mirrored at `app/src/assets/yolo11n.onnx`)
- Local vision logic expects model class labels to include both:
  - `Medicine_Box`
  - `Text_Region`
- If model metadata does not contain those labels, inference returns `LocalVisionStatus.ERROR` with a summary explaining the mismatch
- Before changing post-processing logic, verify the model metadata/class map still matches the business labels

### Model development assets (`yolo/`)

The `yolo/` directory at repo root holds training/evaluation assets not packaged into the APK:

- `yolo/onnx/yolo11n.onnx` — canonical source copy of the detection model
- `yolo/onnx/mobilenet_v3_feat.onnx` — MobileNetV3 feature extractor for item matching (not yet wired into the app runtime)
- `yolo/special_items.json` — feature-vector database; each entry has `item_name` and a float array `feature_vector` for nearest-neighbour matching against MobileNet embeddings

The intended pipeline (partially implemented): YOLO detects `Medicine_Box` → crop is fed to MobileNetV3 for feature extraction → cosine similarity against `special_items.json` vectors identifies the specific product.

### UI structure

- There is no navigation layer or ViewModel layer yet
- `MainActivity.kt` renders a single `VisionHubScreen` composable
- Most user-visible text is produced by small helper functions in `MainActivity.kt`
- Theme files under `ui/theme/` remain standard Compose theme wiring

## Android wiring

- `AndroidManifest.xml` declares:
  - foreground service permissions
  - notification permission
  - call permission
  - optional telephony feature
  - `VisionHubService` as a non-exported foreground service with `dataSync` type
- `NotificationHelper.kt` creates the foreground notification/channel used by the service wake-up path

## Tests

- JVM tests live in `app/src/test/java/com/example/myapplication/`
- Instrumentation / Compose UI tests live in `app/src/androidTest/java/com/example/myapplication/`
- Current meaningful coverage is centered on deterministic logic:
  - `VisionStreamDecoderTest.kt` for mixed stream parsing
  - `FallDetectionEngineTest.kt` for state-machine transitions
  - `LocalVisionAnalyzerTest.kt` (class `YoloInferenceManagerTest`) for ONNX post-processing helpers
  - `ExampleInstrumentedTest.kt` for top-level Compose status copy
- Prefer adding deterministic JVM tests for parsing/post-processing/state transitions before adding device-dependent coverage

## Build configuration notes

- Root `build.gradle.kts` is intentionally minimal and only applies plugin aliases
- Repositories and module inclusion live in `settings.gradle.kts`
- Dependency/plugin versions are centralized in `gradle/libs.versions.toml`
- `app/build.gradle.kts` contains nearly all app-specific build behavior, including Compose enablement, test deps, ONNX Runtime, and asset source sets
- The app currently targets very new SDK levels: `minSdk 35`, `targetSdk 36`, `compileSdk 36`

## Repo-specific cautions

- Heavy image/model work should stay off the main thread
- `VisionDataHub.publishImageFrame()` copies frame bytes before emission; preserve that boundary behavior when changing frame ingestion
- `YoloInferenceManager` currently centralizes ONNX session lifecycle; do not recreate sessions per frame unless there is a deliberate reason

---

## Backend API — HTTP接口文档 + 安全认证规范

Base URL: `http://47.94.146.53:3000`  
All requests that require auth carry `Authorization: Bearer <JWT>` (injected by `AuthInterceptor` in `RetrofitClient.kt`).

### 接口总览

| 接口 | 方法 | 路径 |
|------|------|------|
| 设备注册 | POST | `/device/register` |
| 设备绑定状态查询 | GET | `/device_bind` |
| 图片上传 | POST | `/upload/image` |
| 获取OCR识别文本 | GET | `/get_text` |
| 报警信息上传（排障/障碍物共用） | POST | `/alarm/post` |
| 环境录音上传 | POST | `/upload/audio` |

---

### 1. 设备注册接口

- **POST** `/device/register`
- Content-Type: `application/json`
- 说明：CSRF-JWT 安全认证，token 字段由设备端 HMAC-SHA256 签名生成

**请求体字段**

| 字段 | 类型 | 说明 |
|------|------|------|
| deviceId | String | 设备唯一标识符 |
| displayCode | String | 显示码，用于云端关联展示 |
| token | String | HMAC-SHA256 签名，每次请求重新生成 |

**安全认证算法（云端必须对等实现）**

固定密钥（SECRET_KEY）由设备端和云端共享，不随请求传输。  
签名方式：`HMAC-SHA256(SECRET_KEY, deviceId + ":" + displayCode)`

设备端示例（C++）：
```cpp
std::string sign(const std::string& deviceId, const std::string& displayCode) {
    std::string msg = deviceId + ":" + displayCode;
    // HMAC-SHA256 with SECRET_KEY
    return hmac_sha256_hex(SECRET_KEY, msg);
}
```

云端对等实现（Python）：
```python
import hmac, hashlib
def sign(device_id, display_code, secret_key):
    msg = f"{device_id}:{display_code}".encode()
    return hmac.new(secret_key.encode(), msg, hashlib.sha256).hexdigest()
```

**响应体**：成功时返回设备下发信息（注册确认）。

---

### 2. 设备绑定状态查询接口

- **GET** `/device_bind`
- 需要 Bearer token

**URL 查询参数**

| 参数 | 值 | 说明 |
|------|----|------|
| deviceId | DEVICE_ID | 当前摄像头/设备 ID |

**响应规则**：返回设备是否已与当前用户账号绑定的状态（bool）。

备注：调用前需先登录，用于查询云端关联关系。

---

### 3. 图片上传接口

- **POST** `/upload/image`
- Content-Type: `image/jpeg`
- 请求体：JPEG 图片二进制数据（raw bytes）

备注：
- 设备需要 WiFi 连接才可上传
- 上传后由后台 `ocr_task` 异步处理，结果通过接口 4 查询

---

### 4. 获取OCR识别文本接口

- **GET** `/get_text`

**响应体**

```json
{
  "text": "识别出的文字内容",
  "timestamp": "2024-01-01T00:00:00Z"
}
```

备注：仅返回最近一次图片识别的结果。

---

### 5. 报警信息上传接口（排障/障碍物共用）

- **POST** `/alarm/post`
- Content-Type: `application/json`
- 单一接口，通过 `alarmType` 区分告警类型

#### 5.1 排障报警 请求体

| 字段 | 类型 | 说明 |
|------|------|------|
| deviceId | String | 设备ID |
| alarmType | String | 报警类型（如 `"FALL"`） |
| angle | Float | Y轴角度 |
| angleX | Float | X轴角度 |
| imageUrl | String | 关联图像 URL |
| latitude | Double | 纬度 |
| longitude | Double | 经度 |

#### 5.2 障碍物报警 请求体

| 字段 | 类型 | 说明 |
|------|------|------|
| deviceId | String | 设备ID |
| alarmType | String | 报警类型（如 `"OBSTACLE"`） |
| imageUrl | String | 关联图像 URL |

---

### 6. 环境录音上传接口

- **POST** `/upload/audio`
- Content-Type: `multipart/form-data`

**请求参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| audio | File | 录音文件 |

备注：设备直连接口，文件大小有上限，具体格式需服务端支持。

---

## 硬件串口通信协议 — ESP32 ↔ K210

### ESP32 → K210（指令下发）

ESP32 向 K210 发送控制指令（具体格式待补充）。

### K210 → ESP32（传感器数据上报）

K210 将传感器采集数据通过串口上报给 ESP32，字段如下：

| 字段 | 说明 |
|------|------|
| GYRO | 陀螺仪数据 |
| ACC | 加速度计数据 |
| MAG | 磁力计数据 |
| ACCEL | 加速度 |
| BARO | 气压计数据 |
| WIFI_SSID | WiFi 名称 |
| WIFI_PASS | WiFi 密码 |

ESP32 接收后通过 WiFi 转发至后端服务器（`/alarm/post` 或 `/upload/image` 等接口）。
