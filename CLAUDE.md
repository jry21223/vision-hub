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
