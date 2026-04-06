# Vision Hub

这是一个运行在 Android 设备侧的视觉中枢应用，用于通过 TCP 接收传感器数据和图像帧，在本地执行跌倒检测，并在简洁的 Compose 界面上展示服务状态、连接状态和本地视觉状态。

## 当前能力

- 以前台服务方式在后台持续运行
- 通过 TCP 接收传感器 JSON 帧和 JPEG 图像帧
- 本地跌倒检测状态机处理 IMU 数据，并可尝试发起紧急呼叫
- 本地视觉流水线消费 JPEG 字节流，并发布当前分析状态
- 首页展示连接状态、本地视觉状态和跌倒告警状态

## 技术栈

- Kotlin
- Android 单模块应用，使用 Gradle Kotlin DSL
- Jetpack Compose + Material 3
- Kotlin Coroutines + Flow

## 项目结构

- `app/src/main/java/com/example/myapplication/MainActivity.kt` — 应用入口、权限请求和当前 UI
- `app/src/main/java/com/example/myapplication/VisionHubService.kt` — 前台服务编排中心
- `app/src/main/java/com/example/myapplication/VisionTcpServer.kt` — TCP 监听服务，默认端口 `8080`
- `app/src/main/java/com/example/myapplication/VisionStreamDecoder.kt` — 解析换行 JSON 传感器帧和 JPEG 二进制帧
- `app/src/main/java/com/example/myapplication/FallDetectionEngine.kt` — 跌倒检测状态机
- `app/src/main/java/com/example/myapplication/LocalVisionAnalyzer.kt` — 当前本地图像分析器
- `app/src/main/java/com/example/myapplication/VisionDataHub.kt` — 传感器 / 图像 / 状态的内存 Flow 中枢

## 环境要求

- JDK 17（运行 Gradle / AGP 必须使用）
- Android Studio 或 Android SDK 工具链
- 可运行应用的真机或模拟器

执行 Gradle 命令前先切到 JDK 17：

```bash
export JAVA_HOME="/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
```

## 构建与测试

在项目根目录执行：

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

运行单个单元测试：

```bash
./gradlew testDebugUnitTest --tests "com.example.myapplication.LocalVisionAnalyzerTest"
```

安装到已连接设备或模拟器：

```bash
./gradlew installDebug
```

## 运行方式

### Android Studio

1. 用 Android Studio 打开本项目
2. 确认 Gradle 使用的是 JDK 17
3. 连接真机或启动模拟器
4. 运行 `app` 配置

### 启动时会发生什么

- `MainActivity` 会在需要时请求通知权限
- 应用启动 `VisionHubService`
- 服务进入前台模式并启动 TCP 服务器
- TCP 服务器开始监听 `8080` 端口

## API 文档

当前项目对外暴露的是**设备侧 TCP 接入 API**，不是 HTTP REST API。外部发送端连接 Android 设备的 `8080` 端口后，可以连续发送传感器 JSON 帧和 JPEG 图像帧给应用处理。

### API 概览

| 项目 | 值 |
| --- | --- |
| 协议 | TCP |
| 默认端口 | `8080` |
| 数据方向 | Client → Device |
| 应用响应 | 无应用层响应体 |
| 支持帧类型 | 换行分隔的 JSON 传感器帧、JPEG 二进制帧 |
| 帧顺序 | 按接收顺序处理 |
| 连接方式 | 同一条 TCP 连接中可混合发送传感器帧和图像帧 |

### TCP 端点

应用在 `app/src/main/java/com/example/myapplication/VisionTcpServer.kt` 中启动本地 TCP 服务。

- 端口：`8080`
- 主机：Android 设备当前可被发送端访问到的 IP 地址
- 传输层：纯 TCP
- 鉴权：无
- 加密：无

### 数据流格式

一条 TCP 连接中可以混合包含以下两类数据：

1. **传感器 JSON 帧**，以换行符 `\n` 分隔
2. **JPEG 图像帧**，通过 JPEG 起止标记识别：`0xFF 0xD8 ... 0xFF 0xD9`

解码逻辑位于：
`app/src/main/java/com/example/myapplication/VisionStreamDecoder.kt`

#### 传感器帧格式

每个传感器帧必须是一个完整 JSON 对象，并以换行结束。

示例：

```json
{"radar_dist":120,"imu":{"ax":0.1,"ay":0.5,"az":9.8},"btn_a":0,"btn_b":1}
```

支持字段如下：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `radar_dist` | integer | 是 | 雷达距离读数 |
| `imu.ax` | number | 是 | IMU X 轴加速度 |
| `imu.ay` | number | 是 | IMU Y 轴加速度 |
| `imu.az` | number | 是 | IMU Z 轴加速度 |
| `btn_a` | integer | 是 | 按键 A 状态 |
| `btn_b` | integer | 是 | 按键 B 状态 |

说明：

- 数值字段支持整数或小数（按当前解析逻辑）
- 当前解析器要求所有字段都存在
- 缺字段或格式不合法的 JSON 不属于当前支持范围

#### JPEG 帧格式

JPEG 图像帧必须以以下字节开头：

```text
FF D8
```

并以以下字节结束：

```text
FF D9
```

说明：

- JPEG 帧**不要求**末尾带换行
- JPEG 和 JSON 可以在同一条 TCP 连接内交错发送
- 当前本地视觉分析器只做轻量级校验/分析，还没有 OCR 或模型推理

### 混合流示例

合法的数据流顺序示例：

1. 传感器 JSON + 换行
2. 传感器 JSON + 换行
3. JPEG 二进制帧
4. 传感器 JSON + 换行

应用会严格按照到达顺序处理。

### 应用侧处理约定

收到的数据会通过 `VisionDataHub` 中的 Flow 向应用其它部分分发。

#### 连接状态

由 `VisionTcpServer` 发布到 `VisionDataHub.connectionState`。

| 状态 | 含义 |
| --- | --- |
| `STOPPED` | TCP 服务未运行 |
| `STARTING` | 服务已创建，但服务器尚未监听 |
| `LISTENING` | 正在监听客户端连接 |
| `CONNECTED` | 至少已有一个客户端连接成功 |
| `ERROR` | TCP 服务出现异常 |

#### 跌倒检测流水线

传感器数据由 `app/src/main/java/com/example/myapplication/FallDetectionEngine.kt` 处理。

当前默认阈值来自 `FallDetectionConfig.DEFAULT`：

| 配置项 | 值 |
| --- | --- |
| `freeFallThreshold` | `2.5` |
| `impactThreshold` | `18.0` |
| `impactWindowMillis` | `1000` |
| `cooldownMillis` | `10000` |

高层逻辑如下：

1. 若加速度模长低于 `freeFallThreshold`，状态进入 `DETECTING`
2. 若在 `impactWindowMillis` 内检测到强冲击，则判定跌倒成立
3. 应用尝试拨打配置好的紧急号码
4. 引擎进入 `cooldownMillis` 冷却期

应用层展示的跌倒状态如下：

| 状态 | 含义 |
| --- | --- |
| `IDLE` | 正常监测中 |
| `DETECTING` | 检测到疑似跌倒 |
| `FALL_CONFIRMED` | 已确认跌倒 |
| `EMERGENCY_CALLING` | 正在尝试紧急呼叫 |

#### 紧急呼叫行为

配置文件位于：
`app/src/main/java/com/example/myapplication/EmergencyContactConfig.kt`

| 项目 | 当前值 |
| --- | --- |
| 紧急号码 | `112` |
| 触发方式 | `Intent.ACTION_CALL` |
| 所需权限 | `CALL_PHONE` |

如果设备未授予 `CALL_PHONE` 权限，或者系统中没有可处理拨号的 Activity，则不会发起呼叫。

#### 本地视觉流水线

JPEG 图像帧会被 `app/src/main/java/com/example/myapplication/LocalVisionAnalyzer.kt` 消费，并发布到 `VisionDataHub.localVisionState`。

当前本地视觉状态包括：

| 状态 | 含义 |
| --- | --- |
| `IDLE` | 等待图像帧 |
| `PROCESSING` | 正在分析最新图像 |
| `FRAME_ANALYZED` | 图像通过当前轻量分析器 |
| `ERROR` | 图像无效或分析失败 |

当前分析器行为：

- 校验输入是否像 JPEG 帧
- 生成一条最近图像的摘要信息
- 暂不执行 OCR、目标检测或云端上传

### 发送端示例

#### 使用 `nc` 发送单条传感器帧

```bash
printf '%s\n' '{"radar_dist":120,"imu":{"ax":0.1,"ay":0.5,"az":9.8},"btn_a":0,"btn_b":1}' | nc <device-ip> 8080
```

#### 使用 `nc` 发送 JPEG 文件

```bash
cat frame.jpg | nc <device-ip> 8080
```

#### 使用 Python 发送混合帧

```python
import socket

sensor = b'{"radar_dist":120,"imu":{"ax":0.1,"ay":0.5,"az":9.8},"btn_a":0,"btn_b":1}\n'
image = open("frame.jpg", "rb").read()

with socket.create_connection(("<device-ip>", 8080)) as sock:
    sock.sendall(sensor)
    sock.sendall(image)
    sock.sendall(sensor)
```

### 运行说明与限制

- 当前服务端不会向客户端返回确认响应
- 当前协议面向受信任的本地网络 / 设备调试环境
- 目前没有重试、鉴权、校验和或统一消息封装
- 错误格式的数据可能导致当前连接的数据处理终止
- UI 只负责展示状态；真正的数据接收和处理都发生在前台服务中

## 权限说明

在 `app/src/main/AndroidManifest.xml` 中声明的权限如下：

- `INTERNET`
- `ACCESS_NETWORK_STATE`
- `WAKE_LOCK`
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_DATA_SYNC`
- `POST_NOTIFICATIONS`
- `CALL_PHONE`

## 当前限制

- 本地视觉仍是轻量级占位实现，还没有真正接入 OCR / TFLite 推理
- 紧急联系人号码目前写在代码里，还不能在 App 内动态配置
- UI 目前刻意保持为单 Activity / 单页面的简化结构
- 还没有接入云端 OCR / LLM 流程

## 备注

- 当前 SDK 配置较新：`minSdk 35`、`targetSdk 36`、`compileSdk 36`
- TCP 默认端口：`8080`
- GitHub 仓库：`jry21223/vision-hub`
