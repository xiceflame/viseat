# 三端协作开发指南（眼镜 + 手机 + 后端）

> **版本**: 3.0.0 | **更新**: 2025-11-25 | **适用于**: 3 个 Coding Agent 并行协作

---

## 📋 目录

1. [产品目标](#1-产品目标)
2. [架构总览](#2-架构总览)
3. [后端 API](#3-后端-api)
4. [SDK 参考](#4-sdk-参考)
5. [通信协议](#5-通信协议)
6. [Agent 分工](#6-agent-分工)
7. [共享数据结构](#7-共享数据结构)
8. [联调检查点](#8-联调检查点)

---

## 1. 产品目标

### 1.1 核心功能：持续监测进餐过程

```
用户点击开始 → 基准拍照 → 自动监测(5分钟/次) → 用户点击结束 → 整体评价
```

### 1.2 三个关键阶段

| 阶段 | 触发 | 输出 |
|------|------|------|
| **开始进餐** | 用户点击 + 第一张照片 | 当餐热量/营养 + 1-2句建议（结合用户健康档案） |
| **持续监测** | 自动5分钟拍照 / 用户主动拍照 | 无感计算增减量，更新当餐数值 |
| **结束进餐** | 用户点击 + 最后一张照片 | 整体评价 + 营养饼图 + 今日总摄入占比 |

### 1.3 主动拍照特殊逻辑

- 重置下次自动拍照时间（当前 + 5分钟）
- 表示餐食有明确变化，评估仔细度增加

### 1.4 个性化建议场景

```
用户档案: 45岁 + 轻度脂肪肝
    ↓
开始进餐建议: "建议低油且蓬菜为主"
    ↓
结束评价: "本餐红烧肉热量较高，建议下餐减少油腓食物"
```

### 1.5 后端数据流（待验证）

```
VLM识别餐食种类 → 食品数据库查询原材料 → 估计热量运算 → LLM生成建议
```

**Agent 3 需验证此数据流的准确性**

---

## 2. 架构总览

```
┌─────────────────────────────────────────────────────────────────┐
│          Rokid AR 眼镜（瘦客户端 - CXR-S SDK）                   │
│  拍照 → 蓝牙发送图片 → 接收结果 → AR显示 + TTS播报              │
└──────────────────────────┬──────────────────────────────────────┘
                           │ 蓝牙 (Caps 协议)
┌──────────────────────────▼──────────────────────────────────────┐
│          手机端 Android（网络中枢 - CXR-M SDK）                  │
│  蓝牙服务 → 接收图片 → 调用API → 返回结果 → 本地存储            │
└──────────────────────────┬──────────────────────────────────────┘
                           │ HTTPS (Retrofit)
┌──────────────────────────▼──────────────────────────────────────┐
│          FastAPI 后端（LLM 中枢 - Qwen-VL）                      │
│  /upload → /vision/analyze → /meal/* → /chat/nutrition          │
└─────────────────────────────────────────────────────────────────┘
```

### 职责划分

| 端 | 核心职责 | 不做 |
|----|----------|------|
| **眼镜** | 拍照、AR显示、TTS播报、侧键交互、5分钟定时器 | 不联网、不计算 |
| **手机** | 蓝牙通信、调用API、用户档案、统计UI、设备管理 | 不做 AI 识别 |
| **后端** | VLM识别、营养查询+计算、LLM建议、会话管理 | 不直接与眼镜通信 |

### 手机端功能模块

| 模块 | 说明 |
|------|------|
| **蓝牙通信** | 与眼镜收发图片/结果 (CXR-M SDK) |
| **用户档案** | BMI计算、健康状况(脂肪肝等)、饮食习惯、初次注册时收集 |
| **统计页面** | 饮食爱好排行(当月)、时间规律(用餐时长/时间)、热量营养摄入(当天/当周) |
| **设备管理** | Rokid眼镜连接/配对/设置 (SDK提供) |
| **未来扩展** | 手机/手表运动数据 → 基础消耗热量 |

### SDK 使用

| 端 | SDK | Maven | 用途 |
|----|-----|-------|------|
| 眼镜 | CXR-S | `com.rokid.ai.glass:sdk-service-bridge` | 发送消息、订阅消息 |
| 手机 | CXR-M | `com.rokid.ai.cxr:cxr-api:1.0.3` | 蓝牙连接、自定义视图、自定义协议、设备管理 |

---

## 3. 后端 API 详解

### 3.1 已部署后端

- **生产地址**: `http://viseat.cn`（Nginx 反向代理到 FastAPI 8000）
- **测试**: `curl http://viseat.cn/health`

### 3.2 核心 API 端点

| 端点 | 方法 | 说明 |
|------|------|------|
| `/health` | GET | 健康检查 |
| `/api/v1/upload` | POST | 上传图片，返回 URL |
| `/api/v1/vision/analyze` | POST | **核心**: Qwen-VL 多食材拆解 + 营养计算 |
| `/api/v1/nutrition/aggregate` | POST | 营养聚合计算 |
| `/api/v1/nutrition/lookup` | GET | 查询单个食物营养 |
| `/api/v1/nutrition/barcode` | GET | 条码查询 (Open Food Facts) |
| `/api/v1/meal/start` | POST | 开始用餐会话 |
| `/api/v1/meal/update` | POST | 更新会话（当前剩余快照） |
| `/api/v1/meal/end` | POST | 结束会话，生成报告 |
| `/api/v1/meal/session/{id}` | GET | 获取会话详情 |
| `/api/v1/meal/sessions` | GET | 获取会话列表 |
| `/api/v1/chat/nutrition` | POST | AI 营养建议对话 |

### 3.3 视觉分析 API 详解

**请求**
```http
POST /api/v1/vision/analyze
Content-Type: application/json

{"image_url": "http://viseat.cn/uploads/xxx.jpg"}
```

**响应**（Qwen-VL 多食材拆解 + 后端营养计算）
```json
{
  "foods": [
    {
      "dish_name": "宫保鸡丁",
      "cooking_method": "Stir-fry",
      "ingredients": [
        {"name_en": "Chicken breast", "weight_g": 160, "confidence": 0.9},
        {"name_en": "Peanut", "weight_g": 30, "confidence": 0.85}
      ],
      "total_weight_g": 230
    }
  ],
  "total": {
    "calories": 450,
    "protein": 35,
    "carbs": 20,
    "fat": 25
  },
  "raw_llm": {
    "is_food": true,
    "suggestion": "建议搭配蓬菜"
  }
}
```

**非食物图片**: 返回 HTTP 400
```json
{"detail": "未检测到食物，请拍摄清晰的食物照片"}
```

### 3.4 营养计算逻辑

**烹饪方式系数**（仅对热量加权）
```
Raw/Salad/Steam/Boil: 1.0
Stew/Braise:          1.1
Stir-fry:             1.3
Red-braise:           1.5
Deep-fry:             2.0
```

**计算公式**
```
factor = weight_g / 100.0
calories = calories_per_100g * factor * cooking_factor
protein  = protein_per_100g  * factor
carbs    = carbs_per_100g    * factor
fat      = fat_per_100g      * factor
```

### 3.5 用餐会话流程

```
1. Start: 上传图片 → vision/analyze → meal/start → 创建会话
2. Update: 每5分钟拍照 → vision/analyze → meal/update → 动态基线计算
3. End: 结束拍照 → vision/analyze → meal/end → 生成报告
```

**动态基线策略**（后端实现）
- 每次 Update 返回“当前剩余”快照
- 后端自动识别新菜/加菜，更新 total_served
- 支持中途加菜场景

---

## 4. SDK 参考

### 4.1 CXR-M SDK 核心 API（手机端）

```kotlin
// 获取实例
val cxrApi = CxrApi.getInstance()

// 蓝牙连接
cxrApi.initBluetooth(context, device, callback)
cxrApi.connectBluetooth(context, uuid, mac, callback, license, secret)
cxrApi.deinitBluetooth()
cxrApi.isBluetoothConnected

// 自定义协议（接收眼镜消息）
cxrApi.setCustomCmdListener { cmdName, caps -> }
cxrApi.sendCustomCmd(cmdName, caps)

// 自定义视图（发送 AR 界面到眼镜）
cxrApi.setCustomViewListener(listener)
cxrApi.sendCustomViewIcons(icons)
cxrApi.openCustomView(viewJson)
cxrApi.updateCustomView(updateJson)
cxrApi.closeCustomView()

// 拍照（从手机触发眼镜拍照）
cxrApi.takeGlassPhotoGlobal(width, height, quality) { status, data -> }
```

### 4.2 CXR-S SDK 核心 API（眼镜端）

```kotlin
// 获取实例
val cxrBridge = CXRServiceBridge()

// 状态监听
cxrBridge.setStatusListener(object : StatusListener {
    fun onConnected(name: String, type: Int)
    fun onDisconnected()
    fun onARTCStatus(health: Float, reset: Boolean)
})

// 发送消息
cxrBridge.sendMessage(name, caps): Int
cxrBridge.sendMessage(name, caps, data, offset, size): Int

// 订阅消息
cxrBridge.subscribe(name, MsgCallback)
cxrBridge.subscribe(name, MsgReplyCallback)
```

### 4.3 Caps 数据结构（共用）

```kotlin
// 写入数据
val caps = Caps().apply {
    write("string")           // String
    writeInt32(123)           // Int
    write(3.14f)              // Float
    write(true)               // Boolean
    write(nestedCaps)         // 嵌套 Caps
}

// 读取数据
val str = caps.at(0).string
val num = caps.at(1).int
val flt = caps.at(2).float
```

---

## 5. 通信协议

### 5.1 消息类型定义

```kotlin
// 消息名称常量（三端共用）
object MsgName {
    const val IMAGE = "nutrition_image"       // 眼镜→手机: 图片
    const val COMMAND = "nutrition_command"   // 眼镜→手机: 指令
    const val RESULT = "nutrition_result"     // 手机→眼镜: 结果
    const val STATUS = "session_status"       // 手机→眼镜: 会话状态
}

object CommandType {
    const val START_MEAL = "start_meal"
    const val END_MEAL = "end_meal"
    const val TAKE_PHOTO = "take_photo"
}
```

### 5.2 眼镜 → 手机

**图片消息**
```kotlin
// 眼镜端发送
val caps = Caps().apply {
    write(MsgName.IMAGE)                  // [0] 类型
    write("jpeg")                          // [1] 格式
    writeInt32(imageData.size)            // [2] 大小
    write(System.currentTimeMillis())     // [3] 时间戳
}
cxrBridge.sendMessage(MsgName.IMAGE, caps, imageData, 0, imageData.size)
```

**指令消息**
```kotlin
val caps = Caps().apply {
    write(MsgName.COMMAND)
    write(CommandType.START_MEAL)
    write(System.currentTimeMillis())
}
cxrBridge.sendMessage(MsgName.COMMAND, caps)
```

### 5.3 手机 → 眼镜

**结果消息（CustomCmd）**
```kotlin
// 手机端需要从后端 VLM 响应中提取菜品名称
// 后端返回: raw_llm.foods[].dish_name
// 手机端组合: "红烧肉 · 米饭" 或 "红烧肉等3道菜"
val dishNames = response.raw_llm.foods.map { it.dish_name }
val foodName = when {
    dishNames.size == 1 -> dishNames[0]
    dishNames.size == 2 -> "${dishNames[0]} · ${dishNames[1]}"
    else -> "${dishNames[0]}等${dishNames.size}道菜"
}

val caps = Caps().apply {
    write(MsgName.RESULT)
    write(foodName)                // [1] 菜品描述（手机端组合）
    write(result.calories)         // [2] 总热量
    write(result.protein)          // [3] 蛋白质
    write(result.carbs)            // [4] 碳水
    write(result.fat)              // [5] 脂肪
    write(result.suggestion)       // [6] LLM 建议
}
cxrApi.sendCustomCmd(MsgName.RESULT, caps)
```

**AR 视图（CustomView）**
```kotlin
val viewJson = """
{
  "type": "LinearLayout",
  "props": {"id":"root","backgroundColor":"#CC000000","orientation":"vertical"},
  "children": [
    {"type":"TextView","props":{"id":"name","text":"${result.foodName}","textColor":"#FFFFFF","textSize":"24sp"}},
    {"type":"TextView","props":{"id":"cal","text":"热量: ${result.calories} kcal","textColor":"#FF6B6B"}}
  ]
}
"""
cxrApi.openCustomView(viewJson)
```

---

## 6. Agent 分工

### Agent 1：手机端（新建 `android-phone/`）

| 阶段 | 任务 | 产出文件 |
|------|------|----------|
| Day1 | 项目初始化 + Gradle 配置 | `build.gradle.kts`, `AndroidManifest.xml` |
| Day1-2 | 蓝牙服务实现 | `BluetoothManager.kt`, `GlassesService.kt` |
| Day2-3 | 网络层实现 | `NetworkManager.kt`, `ApiService.kt` |
| Day3 | Room 数据库 | `AppDatabase.kt`, `*Dao.kt`, `*Entity.kt` |
| Day3-4 | UI 界面 | `MainActivity.kt`, `*Screen.kt` |

**Gradle 依赖**
```kotlin
// CXR-M SDK
implementation("com.rokid.ai.cxr:cxr-api:1.0.3")
// Network
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
// Room
implementation("androidx.room:room-runtime:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")
// Compose
implementation(platform("androidx.compose:compose-bom:2024.02.00"))
```

---

### Agent 2：眼镜端（修改 `android/`）

| 阶段 | 任务 | 修改文件 |
|------|------|----------|
| Day1 | 启用 CXR-S SDK | `RokidManager.kt` |
| Day1-2 | 蓝牙发送功能 | `BluetoothSender.kt` (新建) |
| Day2 | 改造拍照流程 | `MainActivity.kt`, `CameraManager.kt` |
| Day2-3 | 接收结果显示 | `RokidManager.kt`, `ResultDisplayManager.kt` |
| Day3 | 移除网络代码 | 删除 `NetworkManager.kt` 中的直接调用 |

**关键修改点**
```kotlin
// MainActivity.kt - 拍照后发送
fun onPhotoTaken(bitmap: Bitmap) {
    val data = bitmap.toJpegByteArray(quality = 85)
    rokidManager.sendImageToPhone(data, "jpeg")
    showLoading("正在识别...")
}

// RokidManager.kt - 接收结果
cxrBridge.subscribe(MsgName.RESULT) { name, caps, _ ->
    val result = parseNutritionResult(caps)
    showARResult(result)
    speakResult(result)
}
```

---

### Agent 3：后端数据流验证 + 优化

| 阶段 | 任务 | 产出 |
|------|------|------|
| **Day1** | 验证数据流 | VLM识别 → 数据库查询 → 热量计算 → LLM建议 |
| Day1-2 | 准确性测试 | 多种菜品识别测试、营养计算对比 |
| Day2 | 个性化建议 API | 支持用户档案(BMI/健康状况)作为 RAG 输入 |
| Day2-3 | 结束报告优化 | 生成营养饼图数据、今日总摄入统计 |
| Day3 | 联调支持 | 调试脚本、日志分析 |

**重点验证任务**
```
1. VLM识别准确性: 输入食物图片 → 输出 dish_name + ingredients
2. 数据库查询: ingredients.name_en → 查询 foodstruct 数据库 → calories_per_100g
3. 热量计算: weight_g * calories_per_100g / 100 * cooking_factor
4. LLM建议生成: 结合用户档案 + 当餐热量 → 个性化建议
```

---

## 7. 共享数据结构

### NutritionResult（三端统一）

```kotlin
/**
 * 营养结果数据类
 * 
 * 注意：foodName 是手机端从后端 VLM 结果中提取的菜品描述，
 * 而不是原始食材列表。
 * 
 * 后端 VLM 返回: raw_llm.foods[].dish_name
 * 手机端组合后发送给眼镜:
 * - 单道菜: "红烧肉"
 * - 两道菜: "红烧肉 · 米饭"
 * - 多道菜: "红烧肉等3道菜"
 */
data class NutritionResult(
    val foodName: String,           // 菜品描述（手机端从 VLM 结果提取）
    val calories: Double,           // 总热量
    val protein: Double,            // 蛋白质
    val carbs: Double,              // 碳水化合物
    val fat: Double,                // 脂肪
    val suggestion: String,         // LLM 建议
    val timestamp: Long = System.currentTimeMillis()
)
```

### VisionAnalyzeResponse（后端返回结构）

后端 `/api/v1/vision/analyze` 返回的完整结构：

```json
{
  "raw_llm": {
    "is_food": true,
    "foods": [
      {
        "dish_name": "红烧肉",
        "cooking_method": "braise",
        "ingredients": [
          {"name_en": "pork", "weight_g": 150, "confidence": 0.9}
        ],
        "total_weight_g": 170,
        "confidence": 0.85
      },
      {
        "dish_name": "米饭",
        "cooking_method": "steam",
        "ingredients": [
          {"name_en": "rice", "weight_g": 200, "confidence": 0.95}
        ]
      }
    ]
  },
  "snapshot": {
    "foods": [...],
    "nutrition": {
      "calories": 650,
      "protein": 25,
      "carbs": 80,
      "fat": 28
    }
  }
}
```

**重要字段说明**：
- `dish_name`: 中文菜品名称（用于显示）
- `cooking_method`: **英文**烹饪方式，用于热量系数计算
  - `raw` (生食) → 1.0x
  - `steam`/`boil` (清蒸/水煮) → 1.0x
  - `braise`/`stew` (红烧/炖) → 1.2x
  - `stir-fry`/`pan-fry` (炒/煎) → 1.3x
  - `deep-fry` (油炸) → 2.0x
- `name_en`: **英文**食材名称，用于营养数据库查询（如 `pork`, `rice`, `chicken breast`）

**手机端处理流程**：
1. 从 `raw_llm.foods[].dish_name` 提取菜品名称
2. 组合成简洁描述（如 "红烧肉 · 米饭"）
3. 从 `snapshot.nutrition` 提取营养数据
4. 调用 `/api/v1/chat/nutrition` 获取 LLM 建议
5. 通过蓝牙发送给眼镜

```kotlin
data class VisionAnalyzeResponse(
    val raw_llm: RawLlm,
    val snapshot: Snapshot
)

data class FoodItem(
    val dish_name: String,
    val cooking_method: String,
    val ingredients: List<Ingredient>,
    val total_weight_g: Double,
    val confidence: Double
)

data class Ingredient(
    val name_en: String,
    val weight_g: Double,
    val confidence: Double
)

data class NutritionTotal(
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

data class RawLlm(
    val is_food: Boolean,
    val suggestion: String?
)
```

### MealSession（会话管理）

```kotlin
data class MealSession(
    val session_id: String,
    val status: String,           // "active", "ended"
    val start_time: Long,
    val end_time: Long?,
    val total_consumed_kcal: Double?,
    val consumption_ratio: Double?,
    val duration_minutes: Int?
)
```

### 数据库表结构（后端 SQLite/PostgreSQL）

```
meal_sessions:     id, user_id, status, start_time, end_time, total_consumed_kcal
meal_snapshots:    id, session_id, image_url, captured_at, raw_json, total_kcal
snapshot_foods:    id, snapshot_id, name, weight_g, calories_kcal, confidence
session_aggregates: session_id, consumed_kcal, protein_g, carbs_g, fat_g
```

---

## 8. 联调检查点

### Checkpoint 1: 蓝牙连接（Day 2）
- [ ] 手机能扫描到眼镜
- [ ] 手机能与眼镜配对连接
- [ ] 连接状态正确显示

### Checkpoint 2: 消息传输（Day 3）
- [ ] 眼镜能发送图片到手机
- [ ] 手机能接收并解析图片
- [ ] 手机能发送结果到眼镜
- [ ] 眼镜能接收并显示结果

### Checkpoint 3: 完整流程（Day 4-5）
- [ ] 拍照 → 发送 → 上传 → 识别 → 返回 → 显示 + 播报
- [ ] 开始/结束用餐指令正常
- [ ] 会话数据正确存储
- [ ] 异常处理（断连、网络失败）

---

## 📎 附录

### Maven 仓库配置

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.rokid.com/repository/maven-public/") }
    }
}
```

### 权限配置

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.BLUETOOTH"/>
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT"/>
<uses-permission android:name="android.permission.BLUETOOTH_SCAN"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.CAMERA"/>
```

### License 文件

从 Rokid 开发者平台下载 `.lc` 授权文件，放置于：
- 手机端：`android-phone/app/src/main/assets/rokid_license.lc`

---

**文档维护**: 每次架构变更后同步更新本文档
