# 开发指引（眼镜 + 手机 + 后端三层架构）

> **版本**: 3.0.0 | **更新**: 2025-11-25

---

## 零、产品目标与核心功能

### 0.1 产品愿景

**“戴上眼镜，你的AI营养师”** - 持续监测进餐过程，了解吃了哪些食品，实时给出个性化建议。

### 0.2 核心流程

```
用户点击开始 → 基准拍照 → 自动监测(5分钟/次) → 用户点击结束 → 整体评价
```

### 0.3 三个关键阶段

| 阶段 | 触发 | 输出 |
|------|------|------|
| **开始进餐** | 用户点击 + 第一张照片 | 当餐热量/营养 + 1-2句建议（结合用户健康档案） |
| **持续监测** | 自动5分钟拍照 / 用户主动拍照 | 无感计算增减量，更新当餐数值 |
| **结束进餐** | 用户点击 + 最后一张照片 | 整体评价 + 营养饼图 + 今日总摄入占比 |

### 0.4 主动拍照特殊逻辑

- **重置计时器**：主动拍照后，下次自动拍照时间 = 当前时刻 + 5分钟
- **仔细评估**：用户主动拍照表示餐食有明确变化，评估应更仔细

### 0.5 个性化建议（基于用户档案 RAG）

```
用户档案: 45岁 + 轻度脂肪肝
    ↓
开始进餐建议: "建议低油且蓬菜为主"
    ↓
结束评价: "本餐红烧肉热量较高，建议下餐减少油腓食物"
```

**用户档案收集**：初次注册时收集，保存到数据库，之后每次登录不再提示输入。

### 0.6 后端数据流（待验证）

```
VLM识别餐食种类 → 食品数据库查询原材料 → 估计热量运算 → LLM生成建议
```

---

## 一、总体架构（V3.0 - 三层架构）

- **Rokid AR 眼镜（瘦客户端 + CXR-S SDK）**：负责拍照、AR 显示、TTS 播报、侧键交互，**5分钟定时器自动拍照**，通过蓝牙与手机通信，**不直接联网**。
- **手机端 Android 应用（网络中枢）**：负责联网、调用 FastAPI 后端 API、数据存储（Room）、会话管理、统计趋势、历史记录，通过蓝牙与眼镜通信。
- **FastAPI 后端（LLM 中枢 + 营养与会话服务）**：
  - 统一调用 Qwen-VL 完成多食材拆解与重量估算。
  - 基于 `foodstruct` 等营养数据库进行精确营养计算与烹饪方式加权（详见第 4 章）。
  - 可选调用 Qwen-Text/Qwen2.5 生成个性化总结与建议（/api/v1/chat/nutrition）。
  - 提供对外 HTTP API：
    - `/api/v1/nutrition/*`：营养聚合与查表接口。
    - `/api/v1/meal/*`：用餐会话开始/更新/结束/查询。
    - `/api/v1/vision/analyze`、`/api/v1/chat/nutrition` 等高级接口，供眼镜端 Android 应用直接调用。

### 1.1 组件职责划分

| 端 | 核心职责 | 不做 |
|----|----------|------|
| **眼镜** | 拍照、AR显示、TTS播报、侧键交互、5分钟定时器 | 不联网、不计算 |
| **手机** | 蓝牙通信、调用API、用户档案、统计UI、设备管理 | 不做 AI 识别 |
| **后端** | VLM识别、营养查询+计算、LLM建议、会话管理 | 不直接与眼镜通信 |

#### 眼镜端（瘦客户端 + CXR-S SDK）
- **输入**：拍照、侧键、语音指令
- **输出**：AR 叠加营养信息、TTS 播报
- **职责**：
  - 硬件交互（相机、侧键、TTS、AR 显示）
  - **5分钟定时器**：自动拍照并发送给手机
  - 通过蓝牙与手机通信，**不直接联网**

#### 手机端（网络中枢 + CXR-M SDK）
- **输入**：来自眼镜的图片、用户指令
- **输出**：识别结果、营养信息、建议文本
- **职责**：
  - **蓝牙通信**：接收眼镜图片/指令，返回结果
  - **网络层**：Retrofit/OkHttp 调用 FastAPI 后端
  - **用户档案**：BMI计算、健康状况（脂肪肝等）、饮食习惯（初次注册时收集）
  - **统计页面**：饮食爱好排行(当月)、时间规律(用餐时长/时间)、热量摄入(当天/当周)
  - **设备管理**：Rokid眼镜连接/配对/设置 (SDK提供)
  - **本地存储**：Room 存储用餐会话、历史记录、用户设置

- **本地/云端 LLM 与营养会话服务（FastAPI + DB，Docker 部署）**
  - 模型调用与视觉拆解：
    - 通过内部集成的 Qwen-VL 完成多食材拆解与重量估算（参见第 4 章 JSON Schema 与 Prompt）。
    - 已提供 `/api/v1/vision/analyze` 接口给眼镜端应用调用，返回可直接用于 `/api/v1/meal/*` 的 `snapshot` 结构。
    - 已提供 `/api/v1/chat/nutrition` 接口，基于会话/营养上下文调用 Qwen 文本模型生成建议文案。
  - 营养查询与聚合：
    - `/api/v1/nutrition/aggregate`：根据 `foods[{name, weight_g, ...}]` 计算总热量与宏营养，并结合烹饪系数调整热量估计。
    - `/api/v1/nutrition/lookup`：按名称返回每 100g 营养信息。
    - `/api/v1/nutrition/barcode`：按条码从 Open Food Facts 查询包装食品营养。
  - 用餐会话管理与持久化：
    - `/api/v1/meal/start|update|end`：接受结构化 JSON 快照，支持实时会话计算与云端备份。
    - `/api/v1/upload`：提供图片上传服务，支持眼镜端上传图片文件并获取可访问 URL。
    - **注意**：用户的完整历史记录主要存储在眼镜端本地（SQLite/Room），云端会话接口主要用于实时计算反馈与可选的云端备份。
  - 环境与部署现状：
    - 已提供本地 venv 与 Docker 镜像，两者共享同一套 `backend/app` 代码与 `requirements.txt` 依赖，保证开发与上线环境一致可迁移。
  - 职责：**统一负责 Qwen 模型调用、营养计算逻辑与数据持久化，是整个系统的 LLM 与营养中枢。**

### 1.2 （历史）Coze HTTP 插件配置（当前版本不再使用）

> 本节保留早期基于 Coze 的 HTTP 工具配置，仅供参考。当前版本中，眼镜端应用通过 Retrofit/OkHttp 直接调用 FastAPI 后端，不再依赖 Coze。

假设本地服务通过 Docker 运行在 `http://your-host:8000`，可在 Coze 中配置如下 HTTP 工具（历史方案示例）：

- **nutrition_aggregate**
  - 方法：`POST`
  - URL：`http://your-host:8000/api/v1/nutrition/aggregate`
  - 请求体（JSON）：
    ```json
    {
      "foods": [
        {"name": "white rice", "weight_g": 150},
        {"name": "chicken breast", "weight_g": 100}
      ],
      "nutrition": null
    }
    ```
  - 用途：图像识别后，把识别到的食物列表交给本地服务计算精确营养总和与每道菜的营养细分。

- **nutrition_lookup**
  - 方法：`GET`
  - URL：`http://your-host:8000/api/v1/nutrition/lookup`
  - Query：`name=white rice`
  - 用途：对话中用户问“100g 米饭多少热量？”时查表用。

- **nutrition_barcode**
  - 方法：`GET`
  - URL：`http://your-host:8000/api/v1/nutrition/barcode`
  - Query：`barcode=6901234567890`
  - 用途：用户扫描包装食品条码时，获取权威的每 100g 营养数据。

- **meal_start**
  - 方法：`POST`
  - URL：`http://your-host:8000/api/v1/meal/start`
  - Query：`user_id=xxx&meal_type=lunch&auto_interval=300`
  - 请求体（JSON，SnapshotPayload）：
    ```json
    {
      "foods": [
        {"name": "white rice", "weight_g": 150},
        {"name": "chicken breast", "weight_g": 100}
      ],
      "nutrition": {
        "calories": 430.0,
        "protein": 40.0,
        "carbs": 50.0,
        "fat": 8.0
      }
    }
    ```
  - 用途：Coze 已通过 `nutrition_aggregate` 计算好本餐基线营养后，调用此工具开始一段用餐会话，返回 `session_id` 供后续更新/结束使用。

- **meal_update**
  - 方法：`POST`
  - URL：`http://your-host:8000/api/v1/meal/update`
  - Query：`session_id=<from meal_start>`
  - 请求体：同 `SnapshotPayload`，表示当前时刻盘中剩余食物估计。
  - 用途：Coze 定时（例如每 5 分钟）对最新图片做识别后，更新本次快照，由本地服务计算“本次新增摄入 + 总摄入 + 进食速度与建议”。

- **meal_end**
  - 方法：`POST`
  - URL：`http://your-host:8000/api/v1/meal/end`
  - Query：`session_id=<from meal_start>`
  - 请求体：可选的 `SnapshotPayload`，表示用餐结束时的最终盘面；若缺省则使用最新快照。
  - 用途：结算本餐统计（总摄入、浪费比例、平均进食速度等）并返回结构化统计和简要报告文本。

- **meal_session_get / meal_sessions_list（可选）**
  - `GET /api/v1/meal/session/{session_id}`：获取某一餐完整记录，用于“帮我回顾这顿饭”。
  - `GET /api/v1/meal/sessions?user_id=xxx[&status=completed]`：获取历史会话列表，用于 Coze 做周/月度趋势分析与 RAG 检索。

> 在 Coze 智能体的系统提示中，应明确：**只有在需要精确营养数值或读写用餐记录时才调用 HTTP 工具，其余逻辑由模型与 RAG 自行完成。**

### 1.3 典型数据流示意（历史 Coze 方案，可选阅读）

> 下述数据流对应早期“Rokid → Coze → 后端”的方案，当前架构已由眼镜端应用直接调用 FastAPI 后端。可以作为对比参考，但实现时以“眼镜端 Android 应用 ↔ FastAPI 后端”的数据流为准。

- **场景 A：开始一顿饭**
  1. 用户在 Rokid 上点击“开始用餐”并拍摄首张图片。
  2. 图片发送到 Coze → 模型识别出 `foods` 列表（菜名 + 估算重量）。
  3. Coze 调用 `nutrition_aggregate` 获取精确营养总和。
  4. Coze 调用 `meal_start` 把首帧快照和营养数据写入本地服务，获得 `session_id`。
  5. Coze 用自然语言向用户解释本餐营养情况，并在内存中保存 `session_id` 以便后续更新。

- **场景 B：用餐过程中的自动监测**
  1. 到达设定间隔（如 5 分钟），Rokid 再次拍照并将图片发给 Coze。
  2. Coze 识别当前剩余食物 → 生成新的 `foods` 列表 → 调用 `meal_update`。
  3. 本地服务返回本次新增摄入、总摄入、用餐时长与建议。
  4. Coze 将建议用更加自然的语言+TTS 反馈给用户。

- **场景 C：结束用餐并生成报告**
  1. 用户点击“结束用餐”，Rokid 触发最后一次拍照（可选）。
  2. Coze 识别最终盘面，生成终局快照，并调用 `meal_end`。
  3. 本地服务计算最终统计（总摄入、浪费比例、平均速度等），并生成一句或数句总结报告。
  4. Coze 在此基础上结合用户历史记录做进一步解释和建议（如“这一周总体热量偏高，建议晚餐适当减少主食”）。

---

## 二、Rokid设备架构理解

### 1.1 核心概念
Rokid Glasses是一个**基于AOSP的完全独立Android系统**，这是理解Rokid开发的关键：

```
┌─────────────────────────────────────┐
│   Rokid Glasses (YodaOS-Sprite)    │
│   ├── Android 9.0+ (AOSP)          │
│   ├── 独立运行应用                  │
│   ├── 自带WiFi/蓝牙                │
│   └── 12MP摄像头 + AR显示          │
└─────────────────────────────────────┘
```

**重要理解**：
- ❌ 错误认知：眼镜可以直接联网访问云端 API
- ✅ 正确认知：眼镜不支持直接联网，需要通过手机作为网络中枢

### 1.2 通信架构

```
眼镜 ──蓝牙──→ 手机 ──HTTPS──→ 云端（FastAPI）
```

### 1.3 通信方式详解

**眼镜 ↔ 手机（蓝牙）**
- 通信协议：蓝牙 SPP/BLE（SDK 支持）
- 眼镜 → 手机：图片数据、用户指令（开始/结束用餐）
- 手机 → 眼镜：识别结果、营养信息、建议文本

**手机 ↔ 后端（HTTPS）**
- 通信协议：HTTPS (Retrofit + OkHttp)
- 手机 → 后端：图片上传、视觉分析请求、会话操作
- 后端 → 手机：识别结果、营养数据、会话状态

## 二、Rokid开发SDK体系

### 2.1 SDK分类

| SDK名称 | 用途 | 平台 | 说明 |
|---------|------|------|------|
| **CXR-S SDK** | 眼镜端开发 | Android | 应用直接运行在眼镜上 ✅ **眼镜端使用** |
| **CXR-M SDK** | 手机控制眼镜 | Android | 手机与眼镜蓝牙通信 ✅ **手机端使用** |
| **UXR SDK** | Unity XR开发 | Unity | 用于3D/AR内容开发 |
| **Rokid Client SDK** | TTS/ASR/NLP | 跨平台 | 语音AI能力 |

**本项目方案**：
- 眼镜端：CXR-S SDK（硬件交互、蓝牙客户端）
- 手机端：CXR-M SDK（蓝牙服务端）+ Retrofit（网络请求）

### 2.2 CXR-S vs CXR-M 对比

| 特性 | CXR-S SDK | CXR-M SDK |
|------|-----------|-----------|
| 运行位置 | 眼镜端 Android 系统 | 手机端 |
| 应用类型 | 眼镜独立应用 | 手机端应用 |
| 网络连接 | 不直接联网 | 手机 WiFi/4G |
| 蓝牙角色 | 客户端 | 服务端 |
| 交互方式 | 侧键 + 语音 | 手机触屏 |
| 本项目选择 | ✅ 眼镜端使用 | ✅ 手机端使用 |

### 2.3 CXR-S SDK核心能力

#### Maven依赖配置
```gradle
// settings.gradle.kts
maven { 
    url = uri("https://maven.rokid.com/repository/maven-public/") 
}

// build.gradle.kts
dependencies {
    // CXR-S SDK（眼镜端开发）
    // 注：具体版本号需要联系 Rokid 官方获取
    // implementation("com.rokid.cxr:client-s:latest_version")
    
    // 标准 Android 依赖
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.google.code.gson:gson:2.10.1")
}
```

#### API功能矩阵（CXR-S SDK）

| 功能分类 | API方法 | 参数 | 返回值 | 说明 |
|---------|---------|------|--------|------|
| **侧键事件** | `setSideKeyListener(listener)` | 回调函数 | - | 监听侧键事件 |
| | `handleKeyEvent(keyCode, event)` | 按键码, 事件 | `Boolean` | 处理按键事件 |
| **AR显示** | `showARContent(content)` | AR内容 | - | 显示AR叠加层 |
| | `hideARContent()` | - | - | 隐藏AR内容 |
| **TTS语音** | `speak(text, callback)` | 文本, 回调 | - | 语音播报 |
| | `stopSpeaking()` | - | - | 停止播报 |
| **相机** | 使用标准 CameraX API | - | - | 眼镜内置相机 |
| **传感器** | 使用标准 Android Sensor API | - | - | 陀螺仪、加速度计等 |
| **设备信息** | `isRunningOnGlasses()` | - | `Boolean` | 检测是否在眼镜上 |
| | `getDeviceInfo()` | - | `DeviceInfo` | 获取设备信息 |

#### 关键数据结构（CXR-S SDK）

```kotlin
// 1. 侧键事件枚举
enum class KeyEvent {
    SHORT_PRESS,  // 短按
    LONG_PRESS,   // 长按
    DOUBLE_PRESS  // 双击
}

// 2. AR 内容数据类
data class ARContent(
    val title: String,
    val subtitle: String? = null,
    val items: List<String> = emptyList(),
    val position: ARPosition = ARPosition.CENTER
)

// 3. AR 显示位置
enum class ARPosition {
    TOP,
    CENTER,
    BOTTOM,
    LEFT,
    RIGHT
}

// 4. 设备信息
data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val sdkVersion: Int
)
    fun onConnected()
    fun onDisconnected()
    fun onFailed(errorCode: CxrWifiErrorCode?)
}

// 4. 未同步文件数量回调
interface UnsyncNumResultCallback {
    fun onUnsyncNumResult(
        status: CxrStatus?,
        audioNum: Int,
        pictureNum: Int,
        videoNum: Int
    )
}
```

## 三、推荐架构方案

### 3.1 方案对比

#### 方案A：眼镜端主导（推荐）
```
用户拍照
    ↓
眼镜端：拍照 → 压缩 → 直接HTTPS上传云端
    ↓
云端：AI分析 → 返回结果
    ↓
眼镜端：AR显示 + TTS播报（3-6秒完成）
```

**优点**：
- ✅ 响应快（3-6秒）
- ✅ 眼镜独立工作，无需手机在旁
- ✅ 用户体验流畅

**缺点**：
- ❌ 眼镜需要联网（WiFi）
- ❌ 眼镜端需要实现网络上传逻辑

<!-- 方案B已移除：统一采用眼镜端一体化方案 -->

### 3.2 本项目选择：方案A

**理由**：
1. **即时性要求**：营养分析需要即时反馈（3-6秒）
2. **使用场景**：通常在家/餐厅等有WiFi环境
3. **用户体验**：拍照后立即看到结果
4. **手机定位**：专注于数据管理和校验，而非实时处理

## 四、模型选型与调用规范（多食材拆解 + 烹饪加权版）

为精准计算混合菜肴（如“宫保鸡丁”、“扬州炒饭”）以及各类非传统中/西餐成品的热量与营养，同时保持较低延迟，本项目采用 **“多食材拆解 + 后端统一营养计算 + 烹饪方式加权”** 策略。

### 4.1 核心策略

1. **VL 多模态模型（视觉拆解）**
   - 负责：
     - 识别菜品名称 `dish_name`（可为中文或描述性文字）。
     - 将整道菜拆解为若干 **主要食材**，输出 `ingredients` 数组。
     - 估算每种食材的 `weight_g`（克）。
     - 判断整体烹饪方式 `cooking_method`（用于后端加权）。
   - 不负责：
     - 不输出热量（kcal）或营养数值，由后端统一计算。

2. **FastAPI 后端（查库 + 计算）**
   - 负责：
     - 使用 `name_en` 在 `foodstruct_nutritional_facts 2.csv` 等食材数据库中查找 **每100g 的营养成分**。
     - 对每种食材按重量折算营养值，并在菜品维度做汇总。
     - 根据 `cooking_method` 选择合适的烹饪加权系数，调整最终热量估计。
   - 输出：
     - 逐食材营养信息 + 整道菜的总营养（kcal / protein / carbs / fat 等）。

3. **眼镜端 Android 应用与后端 API 调用**
   - 眼镜端应用根据业务场景调用后端接口：
     - `POST /api/v1/upload`：上传图片文件（Multipart/form-data），获取图片 URL。
     - `POST /api/v1/vision/analyze`：上传图片 URL，获取多食材拆解与营养快照。
     - `POST /api/v1/nutrition/aggregate`：在需要时对结构化 `foods` 列表做营养聚合。
     - `POST /api/v1/meal/start|update|end`：管理用餐会话与快照持久化。
     - `POST /api/v1/chat/nutrition`：基于营养上下文获取 Qwen 文本建议与总结。
   - 应用负责维护本地历史记录（Room），并将云端返回的结构化结果转化为 AR 展示与语音反馈。

### 4.2 模型输出 JSON Schema

#### 4.2.1 Start / End（单图：完整菜品分析）

```json
{
  "is_food": true,
  "foods": [
    {
      "dish_name": "string",              // 菜名或描述，如 "宫保鸡丁"、"Caesar salad"
      "cooking_method": "string",         // 如: "Steam", "Boil", "Stew", "Stir-fry", "Red-braise", "Deep-fry", "Raw", "Baked", "Grilled"
      "ingredients": [
        {
          "name_en": "string",            // 通用英文食材名，如 "Chicken breast", "Peanut", "Rice", "Tomato"
          "weight_g": 0.0,                 // 该食材的估算重量（克）
          "confidence": 0.0                // 0~1，该食材识别+重量估算的综合置信度
        }
      ],
      "total_weight_g": 0.0,               // 整道菜的总重量（可与 ingredients 求和校验）
      "confidence": 0.0                    // 对整道菜拆解结果的总体置信度
    }
  ]
}
```

说明：
- 当图片中不包含任何可食用食物（例如风景、人像、文档等）时：`is_food` 必须为 `false`，同时 `foods` 设为 **空数组 `[]`**，用于前端/后端统一做“非食物图片”拦截与提示。
- 在 `is_food == true` 时，`ingredients` 必须至少包含 1–5 个主要食材。
- 对于任意菜品（中餐/西餐/融合菜/自创菜），只要能拆解为通用食材名，后端即可计算营养。

#### 4.2.2 Update（双图：本轮新增摄入的增量）

> 说明：当前后端 MVP 实现中，自动监测阶段主要采用 **“单图 + 动态基线”** 的方式（每次只识别当前餐盘剩余量，由后端在数据库层统一推导摄入与加菜），本节“双图 delta” Schema 为后续进阶方案，暂未在现有 FastAPI 代码中启用。

```json
{
  "delta": [
    {
      "dish_name": "string",
      "cooking_method": "string",
      "ingredients": [
        {
          "name_en": "string",
          "delta_consumed_g": 0.0,         // 本轮“新增吃掉”的重量（克）
          "confidence": 0.0
        }
      ]
    }
  ]
}
```

说明：
- 仅输出“增量”，由后端以同样方式计算本轮新增摄入的热量和营养。

### 4.3 后端营养计算逻辑（基于 foodstruct 数据库）

假设在 `backend/data/foodstruct_nutritional_facts 2.csv` 中存储了每种食材 per 100g 的营养数据（kcal / protein / carbs / fat 等）。

#### 4.3.1 单个食材的营养计算

对某个 ingredient：
- 输入：`name_en`, `weight_g`。
- 查表：得到 `calories_per_100g`, `protein_per_100g`, `carbs_per_100g`, `fat_per_100g`。
- 计算：

```text
factor = weight_g / 100.0
calories_i = calories_per_100g * factor
protein_i  = protein_per_100g  * factor
carbs_i    = carbs_per_100g    * factor
fat_i      = fat_per_100g      * factor
```

#### 4.3.2 整道菜的基础营养

对某道菜的 `ingredients` 全部求和：

```text
dish_total_calories = Σ calories_i
dish_total_protein  = Σ protein_i
dish_total_carbs    = Σ carbs_i
dish_total_fat      = Σ fat_i
```

此时得到的是“按食材数据库直接加总”的结果，适用于：沙拉、生食、清蒸、水煮等低油做法。

#### 4.3.3 按烹饪方式加权（中餐/重油菜）

对于明显含较多不可见油脂/酱料的做法，可以在菜品层面增加一个烹饪系数：

- 推荐系数示例：
  - `Raw/Salad/Steam/Boil`：**1.0**
  - `Stew/Braise`：**1.1**
  - `Stir-fry`：**1.3**
  - `Red-braise`：**1.5**
  - `Deep-fry`：**2.0**

- 计算：

```text
cooking_factor      = lookup(cooking_method)
final_dish_calories = dish_total_calories * cooking_factor
```

> 注：当前实现中，**仅对总热量（kcal）应用烹饪系数，不对蛋白质 / 碳水 / 脂肪做放大**。不同烹饪方式更可能导致部分营养素流失而不是增加，如需精细建模可额外引入“营养流失系数”，本版本暂不处理，仅作为热量估算的加权修正。

#### 4.3.4 Update 增量计算

对 `delta` 中每个 ingredient，复用上述公式，将 `weight_g` 换为 `delta_consumed_g` 即可：

```text
delta_factor      = delta_consumed_g / 100.0
delta_calories_i  = calories_per_100g * delta_factor
本轮新增热量 = Σ delta_calories_i * cooking_factor
```

### 4.4 提示词模板示例（多食材拆解）

#### Start（单图，多食材拆解）

```text
角色：你是中餐与西餐的营养分析专家。
任务：
1. 识别图中每一道菜（盘/碗）的大致名称 dish_name（可以是描述性文字）。
2. 将每一道菜拆解为 1-5 种主要可见食材（ingredients），给出每种食材的英文名称 name_en 和估算重量 weight_g（克）。
3. 判断每一道菜的整体烹饪方式 cooking_method。

约束：
- 严格只输出 JSON，不要任何解释或多余文本。
- 所有重量使用克（g）单位的数字。
- name_en 必须是通用的英文食材名，例如：Chicken breast, Pork, Beef, Egg, Rice, Noodle, Tofu, Potato, Tomato, Lettuce, Cheese, Peanut, Oil 等。
- cooking_method 必须从以下集合中选择一个最接近的：
  ["Raw", "Salad", "Steam", "Boil", "Stew", "Stir-fry", "Red-braise", "Deep-fry", "Baked", "Grilled"]。
- 如果图像中完全没有可食用食物（如风景、人像、纯文档等），请返回 `is_food: false` 且 `foods: []`，不要强行输出虚假的菜品。
- 不要输出任何热量或营养成分数值，这些由后续系统计算。

输出示例（结构示意）：
{
  "is_food": true,
  "foods": [
    {
      "dish_name": "宫保鸡丁",
      "cooking_method": "Stir-fry",
      "ingredients": [
        {"name_en": "Chicken breast", "weight_g": 160, "confidence": 0.9},
        {"name_en": "Peanut",         "weight_g": 30,  "confidence": 0.85},
        {"name_en": "Cucumber",       "weight_g": 40,  "confidence": 0.8}
      ],
      "total_weight_g": 230,
      "confidence": 0.9
    }
  ]
}
```

#### Update（双图，增量拆解）

```text
角色：你是食品消耗监测员。
任务：对比“参考图像（用餐前）”与“最新图像（当前）”，只输出本轮新增被吃掉的食物增量。

约束：
- 严格只输出 JSON。
- 对于每一道菜，只输出有明显减少的食材及其 delta_consumed_g（克）。
- 仍然使用通用英文食材名 name_en 和 cooking_method，方便后端计算热量。

输出示例（结构示意）：
{
  "delta": [
    {
      "dish_name": "宫保鸡丁",
      "cooking_method": "Stir-fry",
      "ingredients": [
        {"name_en": "Chicken breast", "delta_consumed_g": 60, "confidence": 0.9},
        {"name_en": "Peanut",         "delta_consumed_g": 10, "confidence": 0.8}
      ]
    }
  ]
}
```
### OpenAI兼容API调用示例（Python）
```python
from openai import OpenAI
client = OpenAI(api_key=os.getenv("DASHSCOPE_API_KEY"), base_url="https://dashscope-intl.aliyuncs.com/compatible-mode/v1")

# Update示例（qwen3-vl-plus，双图）
resp = client.chat.completions.create(
    model="qwen3-vl-plus",
    response_format={"type": "json_object"},
    temperature=0.2, top_p=0.1, seed=123,
    messages=[{
        "role": "user",
        "content": [
            {"type": "image_url", "image_url": {"url": latest_url}},
            {"type": "image_url", "image_url": {"url": reference_url}},
            {"type": "text", "text": "仅输出JSON，返回本次delta..."}
        ]
    }]
)
print(resp.choices[0].message.content)
```

## 五、用餐会话设计（V2整合）

- 核心里念：用户开始/结束控制 + 系统按间隔自动监测（默认5分钟）。
- 交互流程：开始 → 自动监测（静默上传、即时建议）→ 结束（前后对比结算）。
- 会话更新策略（当前后端实现：单图 + 动态基线）：
  - Start：通过 `/api/v1/upload` 上传首张图片，调用 `/api/v1/vision/analyze` 做单图多食材拆解，得到“当前完整餐盘”的快照，并写入 `MealSnapshot` / `SnapshotFood`，作为会话的首帧基线。
  - Update：每隔约 5 分钟静默拍照并再次调用 `/api/v1/vision/analyze`，始终返回“当前剩余”的完整快照；后端遍历该会话全部快照，按食物名称归一化统计**首次出现时的热量**作为 `total_served_kcal`，以最新快照的 `total_kcal` 作为 `current_remaining_kcal`，从而自动识别中途出现的新菜品/加菜并更新总上菜量（动态基线）。
  - End：结束时可再拍一张最终照片并调用同一接口写入会话；后端基于上述动态基线统计计算最终 `total_consumed_kcal`、`consumption_ratio`、`duration_minutes` 等指标并生成用餐报告。

> 说明：早期设计中曾考虑由模型直接输出 delta（双图对比），当前 FastAPI 实现采用“单图 + 后端动态基线”方案，更易落地且能良好支持加菜/新发现食物场景。

## 六、后端API（用餐会话 + 单图分析）

```http
POST /api/v1/upload                 # form-data: file; Returns {url: "..."}

POST /api/v1/meal/start             # query: user_id, meal_type, auto_interval; body: SnapshotPayload(JSON)
POST /api/v1/meal/update            # query: session_id; body: SnapshotPayload(JSON，表示“当前剩余”快照)
POST /api/v1/meal/end               # query: session_id; body: SnapshotPayload(JSON，可选最终快照)
GET  /api/v1/meal/session/{id}
GET  /api/v1/meal/sessions?user_id=xxx[&status=active]

POST /api/v1/vision/analyze         # 单张食物图像分析 + 非食物检测（无食物时返回 400）
```

返回体（建议）：遵循上文 JSON Schema，所有数值单位统一，附带 `confidence`。

## 七、数据模型与数据库（要点）

- meal_sessions：id, user_id, status(active/ended), start_time, end_time,
  initial_snapshot_id, final_snapshot_id, total_consumed_kcal, total_duration_sec, avg_speed_kcal_per_min
- meal_snapshots：id, session_id, image_url, captured_at, model(qwen3-vl-plus|flash), raw_json(jsonb), total_kcal, note
- snapshot_foods：id, snapshot_id, name, chinese_name, weight_g, calories_kcal, confidence, bbox(jsonb?)
- session_aggregates：session_id, consumed_kcal, consumed_g, protein_g, carbs_g, fat_g, last_updated_at
- deltas：id, session_id, snapshot_id, name, delta_consumed_g, delta_consumed_kcal, confidence

## 八、眼镜端实现要点（多页面UI + AR）

- 页面结构：拍照识别页、对话页、统计趋势页、历史记录页（侧键长按循环）
- AR显示样式：半透明卡片显示Top-3与汇总；短TTS播报
- 交互：侧键短按拍照、长按切页；语音指令快捷入口
- 本地存储：Room 持久化会话与快照，断网可缓存重传
- 本项目实现方案：在眼镜端直接运行 Android App，使用 CXR-S SDK 提供的系统与 UI 能力构建上述多页面界面，并在应用内部通过 Retrofit/OkHttp 以 HTTPS 直接调用 FastAPI 后端（含 Qwen 接口与营养/会话服务），不再依赖 Coze。

### 2.4 统一 API 接入层（Android Retrofit Service）

为了保持前后端接口一致性，眼镜端 Android 应用通过统一的 Retrofit Service 访问 FastAPI 后端：

> 说明：本节中的 `ApiClient` 与 `RokidNutritionApi` 代码片段用于展示 Retrofit 的推荐组织方式，**当前仓库实际代码已经使用 `Config.API_BASE_URL` + `NetworkManager` 完成了等价封装**。调整后端路径或域名时，优先以项目中的 Kotlin 源码（`Config.kt`, `NetworkManager.kt`, `Models.kt`, `MainActivity.kt`）为准。

```kotlin
object ApiClient {

    // 开发环境：本地 + ngrok 公网 URL
    private const val BASE_URL = "https://your-ngrok-url.ngrok-free.dev/"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .build()
    }

    val api: RokidNutritionApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RokidNutritionApi::class.java)
    }
}
```

```kotlin
interface RokidNutritionApi {

    @GET("/health")
    suspend fun health(): HealthResponse

    @Multipart
    @POST("/api/v1/upload")
    suspend fun uploadImage(@Part file: MultipartBody.Part): UploadResponse

    /**
     * 视觉分析 - Qwen-VL 多食材拆解
     * 
     * 返回结构:
     * {
     *   "raw_llm": {
     *     "is_food": true,
     *     "foods": [
     *       {"dish_name": "红烧肉", "cooking_method": "braise", "ingredients": [{"name_en": "pork", "weight_g": 150}]},
     *       {"dish_name": "米饭", "cooking_method": "steam", "ingredients": [{"name_en": "rice", "weight_g": 200}]}
     *     ]
     *   },
     *   "snapshot": {
     *     "foods": [...],
     *     "nutrition": {"calories": 650, "protein": 25, "carbs": 80, "fat": 28}
     *   }
     * }
     * 
     * 字段说明:
     * - cooking_method: 英文烹饪方式 (raw/steam/boil/braise/stir-fry/deep-fry)
     * - name_en: 英文食材名，用于数据库查询 (如 pork, rice, chicken breast)
     * 
     * 手机端需要:
     * 1. 从 raw_llm.foods[].dish_name 提取菜品名称
     * 2. 组合成简洁描述（如 "红烧肉 · 米饭"）
     * 3. 从 snapshot.nutrition 提取营养数据
     * 4. 通过蓝牙发送给眼镜
     */
    @POST("/api/v1/vision/analyze")
    suspend fun analyzeVision(@Body body: VisionAnalyzeRequest): VisionAnalyzeResponse

    @POST("/api/v1/nutrition/aggregate")
    suspend fun aggregateNutrition(@Body body: SnapshotPayload): NutritionAggregateResponse

    @POST("/api/v1/meal/start")
    suspend fun startMealSession(
        @Query("user_id") userId: String = "default_user",
        @Query("meal_type") mealType: String = "lunch",
        @Query("auto_interval") autoInterval: Int = 300,
        @Body snapshot: SnapshotPayload
    ): MealStartResponse

    @POST("/api/v1/meal/update")
    suspend fun updateMealSession(
        @Query("session_id") sessionId: String,
        @Body snapshot: SnapshotPayload
    ): MealUpdateResponse

    @POST("/api/v1/meal/end")
    suspend fun endMealSession(
        @Query("session_id") sessionId: String,
        @Body finalSnapshot: SnapshotPayload?
    ): MealEndResponse

    @POST("/api/v1/chat/nutrition")
    suspend fun chatNutrition(@Body body: ChatNutritionRequest): ChatNutritionResponse
}
```

> 说明：
> - 所有 Android 端网络访问都通过 `RokidNutritionApi` 统一封装，避免直接在页面中拼接 URL。
> - 后端如需调整路径或参数，只需保持与上表一致即可，无须修改 CXR-S 相关代码。

## 九、开发进度与路线图（建议）

- Phase 1：云端FastAPI + Qwen3-VL集成（已完成/进行中）
- Phase 2：眼镜端多页面UI（拍照/会话/统计/历史）与AR显示
- Phase 3：对话Agent（qwen3-plus + RAG）与个性化建议
- Phase 4：端到端联调、性能与提示词优化

## 十、最佳实践建议

### 5.1 开发流程
```
1. 云端服务开发（优先）
   ├── FastAPI搭建
   ├── Qwen VL集成
   ├── 营养数据库准备
   └── API测试

2. 眼镜端应用开发
   ├── 拍照功能
   ├── 网络上传
   ├── AR显示
   └── TTS播报
4. 联调测试
   ├── 眼镜→云端→眼镜
   └── 端到端测试
```

### 5.2 常见问题

#### Q1: 眼镜端如何联网？
A: Rokid Glasses支持WiFi连接，在设置中配置WiFi即可。也可以通过手机热点共享网络。

#### Q4: 眼镜端应用如何调试？
A: 通过ADB连接眼镜，使用`adb logcat`查看日志，或使用Android Studio的无线调试功能。

#### Q5: 如何获取Rokid开发者权限？
A: 访问 https://account.rokid.com 注册账号，在开发者中心申请AppKey、AppSecret和AccessKey。

### 5.3 性能优化

#### 眼镜端
- 图片压缩：1024x1024, JPEG 85%
- 网络超时：设置15秒超时
- 电量优化：避免频繁唤醒，使用低功耗模式

 

#### 云端
- 图片预处理：异步处理，避免阻塞
- 结果缓存：Redis缓存常见食物
- 并发控制：限制同时处理的请求数

## 十一、云端架构（MVP与可演进）

- 核心组件
  - API层（FastAPI）：认证/校验、路由、CORS、统一错误码与日志
  - 视觉适配器（Qwen3-VL）：通过 OpenAI 兼容接口调用 `qwen3-vl-plus`，启用 JSON 输出，禁用 thinking
  - 会话管理（内存/DB）：维护 `MealSession`、`MealSnapshot`、`Delta` 聚合与建议生成
  - 存储层：本地 uploads（MVP）→ S3/OSS 可切换
  - 缓存层（可选）：Redis 缓存常见识别与会话热数据
  - 观测性：结构化日志（INFO/ERROR）、`/health` 健康检查

- 关键数据流
  - Start：标准化图片→视觉适配器（plus）→生成首帧“当前餐盘”快照→创建会话→返回基线与建议
  - Update：标准化图片→视觉适配器（plus，后续可灰度 flash）→生成当前餐盘“剩余量”快照→服务端基于该会话全部快照计算动态基线（识别新菜/加菜、更新 total_served）→更新会话进度与建议
  - End：可选最终图→视觉适配器（plus）→写入最终快照→基于动态基线结算→产出最终报告与汇总

- 配置（环境变量建议）
  - `DASHSCOPE_API_KEY`：Qwen密钥
  - `QWEN_BASE_URL=https://dashscope-intl.aliyuncs.com/compatible-mode/v1`
  - `MODEL_VL=qwen3-vl-plus`（统一视觉模型）
  - `ENABLE_THINKING=false`（JSON模式需禁用）
  - `STORAGE_BACKEND=local|s3`（可演进）
  - `DATABASE_URL=sqlite:///./app.db`（MVP）或 `postgresql://...`（可演进）

- 错误与限流
  - 参数校验错误 422、第三方调用失败 502、内部错误 500
  - DashScope 限流与超时重试策略；必要时增加速率限制

## 十二、本地开发步骤（从零到可联调）

1. 环境与依赖
   - Python 3.10+
   - 创建并激活虚拟环境
   - 安装依赖：`pip install -r backend/requirements.txt`
   - 追加安装（启用OpenAI兼容接口）：`pip install openai>=1.52.0`

2. 配置环境变量（backend/.env）
```
DASHSCOPE_API_KEY=你的密钥
QWEN_BASE_URL=https://dashscope-intl.aliyuncs.com/compatible-mode/v1
MODEL_VL=qwen3-vl-plus
ENABLE_THINKING=false
HOST=0.0.0.0
PORT=8000
DEBUG=True
```

3. 启动服务（本地调试）
```
cd backend
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

4. 本地测试
```bash
# 健康检查
curl http://localhost:8000/health

# 运行自动化测试
cd backend
pytest
```

5. 生产环境部署与访问（当前竞赛环境）

- 后端 FastAPI 已部署在云服务器：`http://viseat.cn`（端口 80，经 Nginx 反向代理到 FastAPI `8000`）。
- Android 端生产构建中，`Config.API_BASE_URL` 应配置为：
  
  ```kotlin
  const val API_BASE_URL = "http://viseat.cn"
  ```

- `/api/v1/upload` 返回的相对路径（例如 `/uploads/xxx.jpg`）在 Android 端会被 `NetworkManager` 自动拼接为 `http://viseat.cn/uploads/xxx.jpg`，供 Qwen-VL 访问。
- 可以通过浏览器或 curl 验证：
  
  ```bash
  curl http://viseat.cn/health
  ```

6. 内网穿透（让眼镜或真机访问本地 FastAPI）

```bash
ngrok http 8000
```

> 注意：Qwen-VL 运行在云端，**无法直接访问 `http://localhost:8000` 或 `10.0.2.2`**。传给 `/api/v1/vision/analyze` 的 `image_url` 必须是 Qwen 能访问到的公网 HTTPS 地址，例如：
>
> ```text
> https://your-subdomain.ngrok-free.dev/uploads/xxxx.jpg
> ```
>
> 因此，本地联调时推荐流程为：
> 1. 先通过 `POST /api/v1/upload` 上传图片，得到形如 `/uploads/xxx.jpg` 的相对路径；
> 2. 在 Android 端或 curl 中，将该相对路径拼接为完整公网 URL（以当前 ngrok 域名为前缀）；
> 3. 将拼接后的 URL 作为 `image_url` 传给 `/api/v1/vision/analyze`，由后端转发给 Qwen-VL 进行多模态识别。

7. 下一步代码对齐（建议）
- 将 `app.main.vision_analyze` 全面使用 OpenAI 兼容调用，统一使用 `QWEN_VL_MODEL` 与 `response_format={"type":"json_object"}`（当前 MVP 已采用兼容模式，并对 JSON 解析做了健壮处理）。
- 解析逻辑保留健壮性（string/list/dict）
- 预留 `USE_FLASH_FOR_UPDATE=false` 开关（后续灰度）

## 十三、数据集与数据库选择（优先级与落地）

- **最需要的数据库（数值映射，MVP→生产）**
  - USDA FoodData Central（开放）
    - 用途：每100g 宏营养（kcal/protein/carbs/fat）的权威基础。开放、可下载、字段一致性好。
    - 落地：导出/整理为 CSV 放置于 `backend/data/nutrition_usda.csv`。
  - 中国食物成分表 CFCT（授权/半开放）
    - 用途：本地化准确度高（中式食材/菜肴）。生产优先，注意版权与授权合规。
    - 落地：授权后导出为 CSV 放置于 `backend/data/nutrition_cn.csv`。
  - Open Food Facts（开放）
    - 用途：包装食品条码/OCR 直读营养，精确可靠，作为旁路优先信源。
    - 落地：云端优先走 OFF API 或离线快照；当检测到条码时优先使用。

- **识别/训练数据集（用于“是什么/在哪/由何组成”，提升识别与估重，非线上数据库）**
  - 分类/检测（识别“是什么”）：
    - ChineseFoodNet（中餐重点，208类）
    - Vireo-Food172（172类，含食材多标签）
    - UECFOOD-256 / UECFOOD-100（多盘菜 + 边界框）
    - Food-101（基线）/ ISIA Food-500（大规模、含中式）/ FoodX-251（长尾）
  - 分割/多实例（“是哪一份”）：
    - FoodSeg103（像素级分割，适合食材级）
    - UEC-FOODPIX / FOODPIX-Complete（多菜同图像素级标注）
  - 菜谱/食材对齐（“由何组成”）：
    - Recipe1M / Recipe1M+（图片↔菜谱/食材 对齐）
    - Vireo-Food172（再次强调：自带食材标签）
  - 营养/热量真值（“数值锚定/校准”）：
    - Nutrition5k（提供卡路里/蛋白/脂肪/碳水标签，用于回归或校准）

- **组合使用策略（Rokid场景）**
  - 识别：Food-101/ISIA 预训练 → 在 ChineseFoodNet/Vireo-Food172 微调，覆盖中餐域与长尾。
  - 定位/分割：FoodSeg103 + UEC-FOODPIX 训练像素/实例分割，为体积/重量估计做准备。
  - 配方/食材：Recipe1M(+) + Vireo-Food172 做“图片→食材/菜谱检索”，反推出油糖盐等成分先验。
  - 营养映射：识别出的菜/食材按 USDA/CFCT 每100g 映射；结合分割+参考物/多视帧估重，线性加总宏营养与热量。
  - 包装食品优先：检测到条码时，优先走 Open Food Facts 直读营养。
  - 校准：使用 Nutrition5k 对“图像估营养”回归/误差进行辅助校准，最终仍以 USDA/CFCT 锚定。

- **实现与文档对齐（当前代码已支持）**
  - 外部营养库 CSV 自动加载：`backend/data/{nutrition_cn.csv|nutrition_usda.csv|nutrition_db.csv}`
  - CSV 列规范（每100g）：
    - `name, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g`
  - 解析优先级：CFCT（授权）→ USDA（开放）→ 内置简表；条码场景优先 Open Food Facts。
  - 同义词词表：如 `rice→白米饭`, `chicken breast→鸡胸肉`；支持扩展（可选 `synonyms.csv`）。

- **落地优先级总结**
  - 中餐识别：首选 ChineseFoodNet + Vireo-Food172 进行微调；UECFOOD-256 用于多盘菜检测。
  - 数值映射：生产优先 CFCT；MVP 以 USDA 为基座（开放合规），并保留 OFF 通道覆盖包装食品。

## 十四、参考资料

### 官方文档
- Rokid Mobile SDK: https://rokid.github.io/mobile-sdk-android-docs/
- Rokid开发者中心: https://developer.rokid.com/
- Rokid开发者论坛: https://forum.rokid.com/

### 开源项目
- RokidMobileSDKAndroidDemo: https://github.com/Rokid/RokidMobileSDKAndroidDemo
- UXR SDK文档: https://github.com/RokidGlass/UXR-docs

### 技术博客
- Rokid Glasses移动端控制应用开发: https://segmentfault.com/a/1190000047319027
- Unity3D Rokid AR开发: https://blog.csdn.net/qq_42437783/article/details/140296367

## 十五、Android 应用与后端开发指南（无 Coze 版）

> 目标：指导开发者如何在 **Rokid 眼镜端运行 Android 应用（基于 CXR-S SDK）**，并与 **FastAPI 后端（LLM + 营养与会话服务）** 直接协同工作，形成完整端到端方案。

### 15.1 仓库与模块划分（建议）

推荐采用如下目录思路（实际以当前仓库为准）：

- `backend/`
  - FastAPI 营养与会话服务
  - SQLAlchemy ORM 模型、DB 配置
  - Dockerfile、测试脚本（`test_api.py`, `test_meal_session.py`）
- `android/`（建议新建）
  - `app/`：眼镜端 Android 应用（Kotlin）
  - `libs/`：放置 CXR-S/CXR-M SDK AAR（如官方采用本地依赖）
- `docs/`
  - `ROKID_DEVELOPMENT_GUIDE.md`：当前开发总指引
  - 其他竞赛/产品文档

**职责边界回顾**：

- 眼镜端 Android 应用：负责 UI、拍照、语音交互，使用 CXR-S SDK 访问硬件，并通过 Retrofit/OkHttp 直接调用 FastAPI 后端 HTTP API（包括 `/api/v1/vision/analyze`、`/api/v1/nutrition/*`、`/api/v1/meal/*`、`/api/v1/chat/nutrition` 等）。
- FastAPI 后端：统一承担 LLM 与营养中枢角色，负责 Qwen-VL/Qwen-Text 调用、营养计算与用餐会话持久化存储。

### 15.2 眼镜端 Android 应用架构（基于 CXR-S SDK）

#### 15.2.1 应用职责

- **页面/场景**（对应文档第八章“眼镜端实现要点”）：
  - 拍照识别页：取景、拍照，将当前餐盘图像发送给后端 `/api/v1/vision/analyze`，并展示返回的营养信息。
  - 对话页：与后端对话接口（如 `/api/v1/chat/nutrition`）交互，提问营养和历史建议。
  - 统计趋势页：展示后端返回的周/月度总结（折线/卡片），数据来自 `/api/v1/meal/sessions` 等接口。
  - 历史记录页：展示最近几次用餐会话（通过 `/api/v1/meal/sessions` 获取）。
- **交互入口**：
  - 侧键短按拍照、长按切页（结合 CXR-S 的按键监听能力）。
  - 语音按钮或常驻麦克风图标触发语音识别，将文本问题发送给后端对话接口。
- **网络职责**：
  - 负责将图片/文本请求通过 Retrofit/OkHttp 发送到 FastAPI 后端 HTTP API。
  - 解析后端返回的结构化 JSON（营养、会话与建议），并进行展示与本地缓存。

#### 15.2.2 分层架构建议

推荐使用 Kotlin + Jetpack Compose / Fragment + ViewModel 的 MVVM 结构：

- **ui 层**（`feature/*`）：
  - `feature/camera`: 拍照页面，展示取景预览，触发“开始/更新/结束”指令，并调用对应 UseCase。
  - `feature/chat`: 对话页面，展示 `/api/v1/chat/nutrition` 返回的建议与总结。
  - `feature/history`: 历史/统计页面，展示后端返回的历史数据和图表。
- **domain 层**（可选）：
  - 用例（UseCase）：`StartMealUseCase`, `UpdateMealUseCase`, `EndMealUseCase`, `ChatNutritionUseCase` 等，封装对 FastAPI 后端 HTTP API 的调用流程。
- **data 层**：
  - `BackendRepository`: 负责封装对 FastAPI 后端 HTTP API 的访问（基于 `RokidNutritionApi`）。
  - `LocalStorageRepository`: 使用 Room/Preferences 持久化轻量数据（如最近会话 ID、本地缓存设置）。

> 说明：本项目方案中，所有网络访问都直接指向 FastAPI 后端，Android 端不再依赖 Coze，只需要理解后端返回的 JSON 结构并负责展示与本地缓存。

#### 15.2.3 与 CXR-S/CXR-M SDK 的集成要点

- **依赖集成**：
  - 在 `settings.gradle` 中配置 Rokid 的 Maven 仓库或本地 AAR。
  - 在 `app/build.gradle` 中添加 CXR-S/CXR-M SDK 依赖（具体坐标以官方文档为准）。
- **相机与按键**：
  - 使用 SDK 提供的拍照/录制接口获取图片文件或 ByteArray。
  - 监听设备侧键事件，触发页面切换或拍照上传。
- **网络上传**：
  - 拍照后，将图片压缩为合适分辨率（如 1024x1024, JPEG 85%），通过 OkHttp/Retrofit 以 multipart 或 base64 的形式发送给 FastAPI 后端（例如 `/api/v1/vision/analyze`）。
  - 控制超时（例如 15 秒）和失败重试策略，避免用户等待过久。

### 15.3 （历史）与 Coze 智能体的交互设计（当前版本不再使用）

> 本节仅保留早期基于 Coze 的交互设计，用于对比参考。当前实现中，所有视觉识别、营养计算与对话/建议均由 FastAPI 后端直接提供，Android 应用通过统一 HTTP API 访问，不再接入 Coze。

#### 15.3.1 典型交互流（Android 视角，历史方案）

- **开始用餐（Start）**：
  1. 用户在“拍照识别页”短按侧键 → 拍照。
  2. 客户端构造请求：图片 + 提示文本（如“分析当前餐盘并开始一顿新的用餐会话”），发送给 Coze。
  3. Coze 内部：
     - 使用多模态模型识别食物和重量 → 构造 `foods` 列表。
     - 调用 `nutrition_aggregate` 插件计算精确营养。
     - 调用 `meal_start` 插件写入 FastAPI，获得 `session_id`。
  4. Coze 返回：
     - 对用户的自然语言说明（本餐热量和建议）。
     - 结构化字段（如 `session_id`, `initial_nutrition`），用于后续请求。
  5. Android 将 `session_id` 和摘要展示在 UI 上，并缓存到本地。

- **用餐过程监测（Update）**：
  - Android 每到自动间隔或用户手动触发时，再拍一张图，发送给 Coze，并在提示中携带当前 `session_id`。
  - Coze 据此调用 `meal_update` 插件，返回本次新增摄入与建议；Android 只负责展示。

- **结束用餐（End）**：
  - 用户按“结束”按钮或语音指令，Android 再拍一张或直接请求结束。
  - Coze 调用 `meal_end`，返回最终统计与总结文本。
  - Android 展示最终报告，并可在“历史页”中按会话 ID 查看详情。

#### 15.3.2 Coze 响应解析与本地模型（历史方案）

在 Android 侧建议定义对应的数据类（示意）：

- `MealSessionSummary`：包含 `sessionId`, `totalCalories`, `durationMinutes`, `adviceText` 等。
- `NutritionDetail`：包含每道菜的热量与宏营养明细（由 Coze 工具返回的聚合结果）。

注意：

- 具体字段命名以 Coze 智能体的输出约定为准，建议在 Coze 的系统提示中**固定 JSON Schema**，避免字段漂移。
- Android 只依赖 Coze 给出的最终结构，不直接耦合 FastAPI 的内部表结构。

### 15.4 FastAPI 营养服务的开发与部署流程

本地营养服务的核心代码位于 `backend/app/main.py`、`backend/app/models.py` 和 `backend/app/db.py`。

#### 15.4.1 本地开发步骤（简版）

1. **环境准备**：
   - Python 3.10+，创建虚拟环境。
   - 安装依赖：`pip install -r backend/requirements.txt`。
2. **配置环境变量**：
   - `DATABASE_URL`：开发阶段可设为 `sqlite:///./app.db`；生产建议使用 PostgreSQL。
   - `OPEN_FOOD_FACTS_BASE_URL`：可保持默认或按区域访问镜像。
3. **启动服务**：
   - `cd backend`
   - `uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload`
4. **验证 API**：
   - `GET /health`：检查 `nutrition_db_loaded`。
   - `POST /api/v1/nutrition/aggregate`：使用 `backend/test_api.py` 中示例进行验证。
   - 用 `backend/test_meal_session.py` 跑通 `meal/start|update|end` 全流程。

#### 15.4.2 Docker 部署要点

- Dockerfile 已在 `backend/Dockerfile` 提供：
  - 基于 `python:3.11-slim`。
  - 安装 `requirements.txt`，拷贝 `app/` 与 `data/`。
  - 默认监听 `0.0.0.0:8000`。
- 部署步骤（示意）：
  - `cd backend`
  - `docker build -t rokid-nutrition-backend .`
  - `docker run -p 8000:8000 --env-file .env rokid-nutrition-backend`
- 将容器暴露到公网/内网后，将其地址配置到 Android 应用的 `API_BASE_URL` / Retrofit `BASE_URL` 中。

### 15.5 协同开发流程（Android + 后端）

建议的整体开发顺序：

1. **后端优先（FastAPI）**
   - 确认营养 CSV、Open Food Facts 接入等逻辑正确。
   - 跑通 `nutrition/aggregate|lookup|barcode`、`meal/*` 与 `vision/analyze`、`chat/nutrition` 等 API。
2. **眼镜端 Android 应用开发**
   - 基于 CXR-S SDK 实现拍照、侧键、TTS、AR 显示等交互。
   - 使用 Retrofit/OkHttp 封装对 FastAPI 后端的统一 API 访问（参考第 2.4 节），在本地/测试环境使用 ngrok 暴露后端供真机访问。
3. **端到端联调与优化**
   - 端到端链路：Rokid 眼镜 → Android 应用 → FastAPI 后端 → Android 应用。
   - 关注延迟（目标 3~6 秒）、失败重试、弱网处理与用户提示。

> 提示：为方便竞赛演示，可以预置一组“离线演示数据”（例如若后端不可达时，从本地 JSON 返回模拟结果），避免现场网络异常导致 Demo 中断。

### 15.6 当前 MVP 端到端行为（眼镜端独立版本）

本仓库当前代码（`MainActivity.kt`, `CameraManager.kt`, `NetworkManager.kt`, `RokidManager.kt`）已经实现了一套 **只依赖眼镜端 + FastAPI 后端的 MVP 流程**，不接入手机端 CXR-M：

- **拍照与图片上传**
  - 使用 CameraX 获取取景画面与单帧图片；
  - 将图片压缩为 **JPEG 1024×1024，质量 85%**，保存到眼镜本地缓存目录；
  - 通过 `NetworkManager.uploadImage` 调用 `POST /api/v1/upload`，获得形如 `/uploads/xxx.jpg` 的相对路径；
  - `NetworkManager.analyzeVision` 会自动将该相对路径与 `Config.API_BASE_URL` 拼接为完整 URL，并作为 `image_url` 传给后端 `/api/v1/vision/analyze`，确保在使用 ngrok 等公网代理时 Qwen-VL 能访问到图片。

- **用餐会话控制（Start / Update / End）**
  - **开始用餐（Start）**：
    - 触发方式：
      - 侧键 **短按**（通过 `RokidManager.handleKeyEvent` 中的 `KEYCODE_CAMERA` 映射）；
      - UI 底部操作面板中“长按 · 开始/结束”这一行的点击；
      - 语音指令：“开始用餐”或“开始吃饭”（通过 Android `SpeechRecognizer` 识别后，调用 `handleStartMeal()`）。
    - 行为：
      - TTS 提示“开始用餐，请拍摄餐盘”；
      - 拍照 → 上传 → 调用 `/api/v1/vision/analyze` 获取 `snapshot`（foods + nutrition）；
      - 调用 `POST /api/v1/meal/start` 创建会话，保存 `session_id`，并启动 **5 分钟自动监测循环**（`AUTO_MONITOR_INTERVAL_SECONDS`）。

- **自动监测（Update）**：
  - 每到间隔，静默拍照并再次走“上传 → `/vision/analyze`”流程；
  - 调用 `POST /api/v1/meal/update` 写入新快照；
  - 后端基于所有快照自动计算：总上菜量、当前剩余、累计摄入与建议（见第 5 章“单图 + 动态基线”设计），前端实时更新 UI，并在需要时通过 TTS 播报建议。

- **结束用餐（End）**：
  - 触发方式：
    - 当存在活跃会话时，侧键 **短按**（与开始逻辑复用同一键位，当 `currentSessionId != null` 时调用 `handleEndMeal()`）；
    - UI 底部“长按 · 开始/结束”行的点击；
    - 语音指令：“结束用餐”或“结束吃饭”。
  - 行为：
    - 取消自动监测任务；
    - 再拍一张当前餐盘图片（可视作“结束快照”）并走分析流程；
    - 调用 `POST /api/v1/meal/end` 结算本餐，前端显示“用餐结束，共摄入 X kcal”，并通过 TTS 播报。

- **单次分析（Single Shot）**
  - 触发方式：侧键 **长按**（`KEYCODE_VOLUME_UP` 长按，在 `RokidManager.handleKeyEvent` 中映射为 `KeyEvent.LONG_PRESS`），或 UI 中“短按 · 拍照识别”行的点击；
  - 行为：只拍一次照并调用 `/api/v1/vision/analyze`，**不进入用餐会话**，适合快速查看当前餐盘大致热量。

- **语音识别与营养对话**
  - 触发方式：UI 底部“点按 · 语音问答”行的点击；
  - 行为：
    - 打开一个对话样式的浮层，使用 Android `SpeechRecognizer` 进行中文语音识别；
    - 若识别到的文本包含“开始用餐/开始吃饭/结束用餐/结束吃饭”，则优先视为 **控制指令**，直接调用上述 Start/End 流程；
    - 否则，将识别结果作为自然语言问题发送到后端 `POST /api/v1/chat/nutrition`，后端基于当前营养上下文生成回答，前端在对话框中展示文字，并通过 TTS 播报。

### 15.7 本地端到端测试流程（curl + ngrok）

在 Android 联调之前，推荐先用 curl 在本机验证“上传图片 → 视觉识别 → 营养计算”链路是否正常：

1. **启动后端并确认健康状态**
   - `cd backend && ./start.sh` 或 `uvicorn app.main:app --reload`；
   - `curl http://localhost:8000/health` 确认返回 200 且 `nutrition_db_loaded` 为 true/false（只要非 5xx 即可）。

2. **上传测试图片**
   - `curl -X POST http://localhost:8000/api/v1/upload -F "file=@pics/your_food.jpg"`；
   - 记下响应中的 `url` 字段，例如 `/uploads/abc.jpg`。

3. **通过 ngrok 暴露服务并构造公网图片 URL**
   - 运行 `ngrok http 8000`，记录面板中的 HTTPS 地址，例如 `https://xxx.ngrok-free.dev`；
   - 拼接得到完整图片 URL：`https://xxx.ngrok-free.dev/uploads/abc.jpg`，在浏览器中确认可直接访问。

4. **调用视觉识别接口**
   - `curl -X POST http://localhost:8000/api/v1/vision/analyze -H "Content-Type: application/json" -d '{"image_url":"https://xxx.ngrok-free.dev/uploads/abc.jpg","question":"请帮我估算这顿饭的营养"}'`；
   - 若配置正确，应返回 `raw_llm`（Qwen-VL 的 JSON 输出）和 `snapshot`（foods + nutrition）结构；
   - 若看到 `"detail":"调用 Qwen-VL 失败"`，请检查：
     - `.env` 中 `DASHSCOPE_API_KEY` 是否可用；
     - 图片是否为标准 JPEG/PNG（可用 `file` 命令或浏览器确认）；
     - `image_url` 是否为 Qwen 可访问的公网 HTTPS 地址（而不是 localhost/内网 IP）。

---

**更新日期**: 2025-11-17
**作者**: Cascade AI Assistant
