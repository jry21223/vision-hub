# Vision Hub

Android wearable-side vision hub app for receiving sensor and image data over TCP, detecting falls locally, and showing current service/vision status in a simple Compose UI.

## Current capabilities

- Foreground service keeps the pipeline running in the background
- TCP server listens for incoming sensor JSON frames and JPEG image frames
- Fall detection state machine processes IMU data and can trigger an emergency call
- Local vision pipeline consumes JPEG bytes and publishes a simple analysis status
- Home screen shows connection state, local vision state, and fall alert state

## Tech stack

- Kotlin
- Android app module with Gradle Kotlin DSL
- Jetpack Compose + Material 3
- Kotlin coroutines and Flow

## Project structure

- `app/src/main/java/com/example/myapplication/MainActivity.kt` — app entry, permission requests, and current UI
- `app/src/main/java/com/example/myapplication/VisionHubService.kt` — foreground service orchestration
- `app/src/main/java/com/example/myapplication/VisionTcpServer.kt` — TCP listener on port `8080`
- `app/src/main/java/com/example/myapplication/VisionStreamDecoder.kt` — decodes newline JSON sensor packets and JPEG frames
- `app/src/main/java/com/example/myapplication/FallDetectionEngine.kt` — fall detection state machine
- `app/src/main/java/com/example/myapplication/LocalVisionAnalyzer.kt` — current local image-frame analyzer
- `app/src/main/java/com/example/myapplication/VisionDataHub.kt` — in-memory Flow hub for sensor/image/state updates

## Requirements

- JDK 17 for Gradle/AGP tasks
- Android Studio or Android SDK tools
- Device or emulator for running the app

Set JDK 17 before running Gradle commands:

```bash
export JAVA_HOME="/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
```

## Build and test

From the repository root:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

Run a single unit test:

```bash
./gradlew testDebugUnitTest --tests "com.example.myapplication.LocalVisionAnalyzerTest"
```

Install to a connected device/emulator:

```bash
./gradlew installDebug
```

## Run the app

### Android Studio

1. Open this project in Android Studio
2. Make sure Gradle uses JDK 17
3. Connect a device or start an emulator
4. Run the `app` configuration

### What happens on startup

- `MainActivity` requests notification and phone-call permissions when needed
- The app starts `VisionHubService`
- The service enters foreground mode and starts the TCP server
- The TCP server listens on port `8080`

## API documentation

This project currently exposes a **device-side TCP ingest API** rather than an HTTP REST API. External senders connect to the Android device on port `8080` and stream sensor JSON frames and JPEG image frames into the app.

### API summary

| Item | Value |
| --- | --- |
| Protocol | TCP |
| Default port | `8080` |
| Direction | Client → device only |
| App response | No application-layer response body |
| Supported frame types | Newline-delimited JSON sensor frames, JPEG binary frames |
| Frame ordering | Preserved in receive order |
| Connection handling | Single TCP stream can mix sensor frames and image frames |

### TCP endpoint

The app starts a local TCP server in `app/src/main/java/com/example/myapplication/VisionTcpServer.kt`.

- Port: `8080`
- Host: the Android device IP address reachable from your sender
- Transport: plain TCP
- Authentication: none
- Encryption: none

### Stream format

A single TCP connection may contain a mix of the following payloads:

1. **Sensor JSON frames** separated by newline (`\n`)
2. **JPEG image frames** detected by JPEG SOI/EOI markers (`0xFF 0xD8 ... 0xFF 0xD9`)

The decoder is implemented in `app/src/main/java/com/example/myapplication/VisionStreamDecoder.kt`.

#### Sensor frame format

Each sensor frame must be one complete JSON object followed by a newline.

Example:

```json
{"radar_dist":120,"imu":{"ax":0.1,"ay":0.5,"az":9.8},"btn_a":0,"btn_b":1}
```

Supported fields:

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `radar_dist` | integer | yes | Radar distance reading |
| `imu.ax` | number | yes | IMU acceleration X |
| `imu.ay` | number | yes | IMU acceleration Y |
| `imu.az` | number | yes | IMU acceleration Z |
| `btn_a` | integer | yes | Button A state |
| `btn_b` | integer | yes | Button B state |

Notes:

- Numbers may be integer or decimal where applicable.
- The current parser expects all keys to be present.
- Malformed or missing-key JSON is not part of the supported contract.

#### JPEG frame format

A JPEG frame is any binary payload beginning with:

```text
FF D8
```

and ending with:

```text
FF D9
```

Notes:

- JPEG frames do **not** need a trailing newline.
- JPEG and JSON frames may be interleaved on the same TCP stream.
- The current local analyzer performs lightweight validation/analysis only; it does not yet do OCR or model inference.

### Mixed-stream example

Valid stream order example:

1. sensor JSON + newline
2. sensor JSON + newline
3. JPEG binary bytes
4. sensor JSON + newline

The app processes frames in the order they arrive.

### App-side processing contract

Incoming data updates the app through `VisionDataHub` flows.

#### Connection state

Published by `VisionTcpServer` through `VisionDataHub.connectionState`.

| State | Meaning |
| --- | --- |
| `STOPPED` | TCP server is not running |
| `STARTING` | Service created, server not yet listening |
| `LISTENING` | Server is listening for clients |
| `CONNECTED` | At least one client is connected |
| `ERROR` | Server failed unexpectedly |

#### Fall-detection pipeline

Sensor packets are processed by `app/src/main/java/com/example/myapplication/FallDetectionEngine.kt`.

Current default thresholds from `FallDetectionConfig.DEFAULT`:

| Setting | Value |
| --- | --- |
| `freeFallThreshold` | `2.5` |
| `impactThreshold` | `18.0` |
| `impactWindowMillis` | `1000` |
| `cooldownMillis` | `10000` |

High-level behavior:

1. If acceleration magnitude drops below `freeFallThreshold`, state becomes `DETECTING`
2. If a strong impact arrives within `impactWindowMillis`, the fall is confirmed
3. The app attempts to call the configured emergency number
4. The engine enters cooldown for `cooldownMillis`

Published UI-facing fall states:

| State | Meaning |
| --- | --- |
| `IDLE` | Normal monitoring |
| `DETECTING` | Possible fall detected |
| `FALL_CONFIRMED` | Fall confirmed |
| `EMERGENCY_CALLING` | Emergency call attempt in progress |

#### Emergency-call behavior

Configured in `app/src/main/java/com/example/myapplication/EmergencyContactConfig.kt`.

| Item | Current value |
| --- | --- |
| Emergency number | `112` |
| Trigger action | `Intent.ACTION_CALL` |
| Required permission | `CALL_PHONE` |

If `CALL_PHONE` is not granted or no call activity is available, the app does not place the call.

#### Local-vision pipeline

JPEG frames are consumed by `app/src/main/java/com/example/myapplication/LocalVisionAnalyzer.kt` and published through `VisionDataHub.localVisionState`.

Current local-vision states:

| State | Meaning |
| --- | --- |
| `IDLE` | Waiting for image frames |
| `PROCESSING` | Latest frame is being analyzed |
| `FRAME_ANALYZED` | Frame passed the current lightweight analyzer |
| `ERROR` | Invalid frame or analysis failure |

Current analyzer behavior:

- validates that the payload looks like a JPEG frame
- returns a summary string for the latest frame
- does not yet perform OCR, object detection, or cloud upload

### Sender examples

#### Send one sensor frame with `nc`

```bash
printf '%s\n' '{"radar_dist":120,"imu":{"ax":0.1,"ay":0.5,"az":9.8},"btn_a":0,"btn_b":1}' | nc <device-ip> 8080
```

#### Send a JPEG file with `nc`

```bash
cat frame.jpg | nc <device-ip> 8080
```

#### Send mixed frames with Python

```python
import socket

sensor = b'{"radar_dist":120,"imu":{"ax":0.1,"ay":0.5,"az":9.8},"btn_a":0,"btn_b":1}\n'
image = open("frame.jpg", "rb").read()

with socket.create_connection(("<device-ip>", 8080)) as sock:
    sock.sendall(sensor)
    sock.sendall(image)
    sock.sendall(sensor)
```

### Operational notes

- The server does not currently send acknowledgements back to the client.
- The protocol is intended for trusted local-network/device testing in its current form.
- There is no retry, auth, checksum, or message envelope yet.
- A malformed payload may terminate processing for that connection.
- The app UI is only a status surface; data ingestion and analysis happen in the foreground service.

## Permissions used

Declared in `app/src/main/AndroidManifest.xml`:

- `INTERNET`
- `ACCESS_NETWORK_STATE`
- `WAKE_LOCK`
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_DATA_SYNC`
- `POST_NOTIFICATIONS`
- `CALL_PHONE`

## Current limitations

- Local vision is still a lightweight placeholder analysis, not full OCR/TFLite inference
- Emergency number is code-configured, not user-configurable in-app
- UI is intentionally minimal and stays in a single activity/screen
- No cloud OCR / LLM integration yet

## Notes

- The app currently uses very new SDK targets (`minSdk 35`, `targetSdk 36`, `compileSdk 36`)
- TCP default port is `8080`
- GitHub repository: `jry21223/vision-hub`
