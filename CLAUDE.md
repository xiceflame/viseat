# CLAUDE.md

This file provides context and guidelines for Claude Code to work effectively with the RokidAI repository.

## 全局规则 (Global Rules)

- **语言**: 始终使用中文回复用户
- **Language**: Always respond to the user in Chinese

## Project Overview

**Rokid Smart Nutrition Assistant (Viseat)**
A distributed AR system for real-time food analysis and nutrition tracking.
- **Architecture**: 3-Tier (Glasses Thin Client ↔ Android Phone Hub ↔ FastAPI Backend)
- **Key Tech**: Rokid CXR SDKs (S/M), Jetpack Compose, Kotlin, Room, Retrofit.

## Repository Structure

- `android/`: **Glasses App** (CXR-S SDK). Runs on Rokid glasses. Handles Camera & UI.
- `android-phone/`: **Phone App** (CXR-M SDK). Logic hub. Handles Bluetooth, Network, DB, Heavy UI.
- `viseat-tech-visualizer/`: **Web Visualizer**. React demo for technical showcasing.

## Development Guidelines (Cost-Effective)

### 1. Navigation & Context
- **Always `cd` first**: This is a multi-root workspace.
    - Glasses: `cd android`
    - Phone: `cd android-phone`
    - Web: `cd viseat-tech-visualizer`
- **Focused Reading**: Read only relevant files. Use `grep` to find symbols before reading files.
- **Avoid Listing**: Do not `ls -R`. Use `find . -maxdepth 3 -name "*Pattern*"` to locate files.

### 2. Build & Run (Efficient)
Prefer Debug builds for development to save time.

#### Phone App (`android-phone`)
- **Build**: `./gradlew :app:assembleDebug`
- **Install**: `./gradlew :app:installDebug`
- **Test**: `./gradlew :app:testDebugUnitTest` (Unit) or `./gradlew :app:connectedDebugAndroidTest` (Instrumented)
- **Clean**: `./gradlew clean`

#### Glasses App (`android`)
- **Build**: `./gradlew :app:assembleDebug`
- **Install**: `./gradlew :app:installDebug`
- **Logcat**: `adb logcat -s "RokidNutrition" "CXRService"`

### 3. Code Style & Standards
- **Language**: Kotlin (Android), TypeScript (Web).
- **UI Framework**: Jetpack Compose (Phone), Android Views/Compose (Glasses).
- **Architecture**: MVVM + Clean Architecture principles.
    - `data/`: Entities, DAOs (Room).
    - `network/`: API models, Retrofit services.
    - `repository/`: Single source of truth, manages offline/online sync.
    - `ui/`: Compose screens, ViewModels, States.
- **Dependency Injection**: Manual DI via Singleton (e.g., `NutritionPhoneApp.instance`, `NetworkManager.getInstance()`).
- **Async**: Coroutines & Flows. Avoid RxJava unless necessary.

### 4. Key Configuration
- **Permissions**: Bluetooth (`CONNECT`, `SCAN`), Camera, Internet are critical.
- **SDK Compatibility**:
    - Phone: `minSdk 28`, `compileSdk 34`.
    - Glasses: `minSdk 28`, `compileSdk 34`.
- **Backend URL**: Defined in `Config.kt`. Ensure it points to `https://viseat.cn` or local mock.

## Core Workflows

### Bluetooth Protocol (CXR Caps)
- **Image**: `nutrition_image` (Glasses -> Phone)
- **Command**: `nutrition_command` (Glasses -> Phone)
- **Result**: `nutrition_result` (Phone -> Glasses)
- **Status**: `session_status` (Phone -> Glasses)

### Data Flow
1. **Capture**: Glasses takes photo -> BT -> Phone.
2. **Process**: Phone `NetworkManager` uploads -> Backend analysis.
3. **Store**: Phone `Room` DB stores session/snapshot.
4. **Feedback**: Phone sends analysis result -> BT -> Glasses UI.

## 后端服务器 (Backend Server)

- **后端地址**: `https://viseat.cn`
- **后端代码**: 部署在服务器上，不在本仓库中
- **技术栈**: FastAPI + Qwen-VL + PostgreSQL

## SDK 版本约束 (Critical)

SDK 版本不可混用，否则会导致通信失败：
- **眼镜端**: `com.rokid.cxr:cxr-service-bridge:1.0-20250519.061355-45`
- **手机端**: `com.rokid.cxr:client-m:1.0.1-20250812.080117-2`

## 网络配置

- `CLIENT_SECRET = "c7cba481-c9da-11f0-961e-043f72fdb9c8"`
- `NETWORK_TIMEOUT_SECONDS = 30L` (VLM 分析可能需要 60s)
- 重试策略: 3次，1s-8s 指数退避

## 完整消息协议

| 消息名 | 方向 | 用途 |
|--------|------|------|
| `nutrition_image` | 眼镜 → 手机 | 图片数据 |
| `nutrition_command` | 眼镜 → 手机 | start_meal, end_meal, take_photo |
| `nutrition_result` | 手机 → 眼镜 | 识别结果 |
| `session_status` | 手机 → 眼镜 | 会话状态 |
| `processing_phase` | 手机 → 眼镜 | 处理阶段 (1-6) |
| `meal_summary` | 手机 → 眼镜 | 用餐总结 |
| `personalized_tip` | 手机 → 眼镜 | 个性化建议 |

## 常见陷阱

1. **图片压缩**: 上传前必须压缩到 <1MB，否则会超时
2. **会话状态检查**: 调用 `updateMeal()` 或 `endMeal()` 前必须检查 `isSessionActive`
3. **眼镜测试**: 需要实体 Rokid 眼镜，无法用模拟器
4. **蓝牙权限**: Android 12+ 需要运行时申请 `BLUETOOTH_CONNECT` 和 `BLUETOOTH_SCAN`

## 功能规格文档

新功能的需求、设计、任务分解位于: `.kiro/specs/<feature-name>/`

## Demo 模式

无眼镜/后端时的测试模式: `android-phone/.../demo/`

## Token Economy Tips
- **Summarize**: When reading large files, look for interfaces and public methods first.
- **Plan**: Use `todo_list` to break down complex tasks (e.g., "Add new API endpoint").
- **Batch**: Run multiple related shell commands in sequence or parallel.
