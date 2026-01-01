# Rokid 智能营养助手 - 项目总览

> **版本**: 3.3.0 | **状态**: 智能建议系统完成 | **更新**: 2025-11-27

## 📋 项目简介

基于 Rokid AR 眼镜的**持续进餐监测系统**，实时了解吃了哪些食品，给出个性化营养建议。

## 🎯 产品目标

### 核心流程
```
用户点击开始 → 基准拍照 → 自动监测(5分钟/次) → 用户点击结束 → 整体评价
```

### 三个关键阶段

| 阶段 | 触发 | 输出 |
|------|------|------|
| **开始进餐** | 用户点击 + 第一张照片 | 当餐热量/营养 + 1-2句建议（结合用户健康档案） |
| **持续监测** | 自动5分钟拍照 / 用户主动拍照 | 无感计算增减量，更新当餐数值 |
| **结束进餐** | 用户点击 + 最后一张照片 | 整体评价 + 营养饼图 + 今日总摄入占比 |

### 个性化建议场景
```
用户档案: 45岁 + 轻度脂肪肝
    ↓
开始建议: "建议低油且蓬菜为主"
    ↓
结束评价: "本餐红烧肉热量较高，建议下餐减少油腓食物"
```

## 🏗️ 系统架构（V3.0 - 三层架构）

```
┌─────────────────────────────────────────────────────────────────┐
│              Rokid AR 眼镜（瘦客户端 - CXR-S SDK）               │
│  • 拍照  • 5分钟定时器  • AR显示  • TTS播报  • 不联网          │
└───────────────────────────┬─────────────────────────────────────┘
                            │ 蓝牙（Caps 消息）
┌───────────────────────────▼─────────────────────────────────────┐
│              手机端 Android 应用（网络中枢 - CXR-M SDK）         │
│  • 蓝牙服务  • API调用  • 用户档案  • 统计页面  • 设备管理     │
└───────────────────────────┬─────────────────────────────────────┘
                            │ HTTPS (Retrofit)
┌───────────────────────────▼─────────────────────────────────────┐
│              FastAPI 后端服务（已部署 viseat.cn）               │
│  • VLM识别  • 营养计算  • LLM建议  • 会话管理                  │
└─────────────────────────────────────────────────────────────────┘
```

### 职责划分

| 端 | 核心职责 | 不做 |
|----|----------|------|
| **眼镜** | 拍照、AR显示、TTS播报、5分钟定时器 | 不联网、不计算 |
| **手机** | 蓝牙通信、调用API、用户档案、统计UI | 不做 AI 识别 |
| **后端** | VLM识别、营养查询+计算、LLM建议 | 不直接与眼镜通信 |

## 📁 项目结构

```
RokidAI/
├── android/                              # 眼镜端（瘦客户端）✅
│   └── app/src/main/kotlin/com/rokid/nutrition/
│       ├── MainActivity.kt               # 主界面
│       ├── CameraManager.kt              # 相机管理
│       ├── RokidManager.kt               # TTS、AR显示
│       ├── Config.kt                     # 配置
│       └── bluetooth/
│           ├── BluetoothSender.kt        # 发送图片到手机
│           └── BluetoothReceiver.kt      # 接收手机结果
│
├── android-phone/                        # 手机端（网络中枢）⏳ 待开发
│   └── app/src/main/kotlin/
│       ├── bluetooth/                    # 蓝牙服务 (CXR-M SDK)
│       ├── network/                      # API 调用 (Retrofit)
│       ├── database/                     # 本地存储 (Room)
│       └── ui/                           # 统计页面、用户档案
│
├── backend/                              # FastAPI 后端 ✅ 已部署 viseat.cn
│   ├── app/
│   │   ├── main.py                       # FastAPI 主应用
│   │   └── nutrition_db.py               # 多数据源营养数据库
│   ├── data/
│   │   ├── china_food_composition.csv    # 中国食物成分表
│   │   ├── usda_foundation_foods.csv     # USDA 基础食物数据
│   │   ├── foodstruct_nutrition.csv      # FoodStruct 数据
│   │   └── NUTRITION_DATA_SOURCES.md     # 数据来源说明
│   ├── scripts/
│   │   └── test_real_flow.py             # 真实数据流测试
│   └── CHANGELOG.md                      # 更新日志
│
├── README.md                             # 项目主文档
├── PROJECT_OVERVIEW.md                   # 项目总览（本文件）
├── ROKID_DEVELOPMENT_GUIDE.md            # 开发指南
├── THREE_AGENT_DEVELOPMENT_GUIDE.md      # 三端协作开发指南
└── AGENT_TASKS.md                        # Agent 任务清单
```

## ✅ 已完成工作

### 后端服务 ✅ 已部署 viseat.cn
- ✅ FastAPI + Qwen-VL 集成
- ✅ 用餐会话 API（start/update/end）
- ✅ 动态基线策略（支持中途加菜）
- ✅ 非食物检测（返回 400 错误）
- ✅ **多数据源营养数据库**（中国食物成分表优先、USDA、FoodStruct）
- ✅ **VLM Prompt 优化**：支持零食、饮料、包装食品识别
- ✅ **别名映射系统**：支持零食、饮料、加工食品的营养查询
- ✅ VLM→数据库→热量计算 数据流已验证
- ✅ **基线列表模式**：vision/analyze 支持 `mode=update` + `baseline_foods` 保持食材名称一致性
- ✅ **容错分析 API**：`analyze_meal_update` 自动检测拍摄问题、加菜等异常情况
- ✅ **智能建议系统**（2025-11-27）：
  - `/api/v1/meal/end` 增强：支持 `meal_context`、`daily_context`、`user_profile` 参数
  - LLM 驱动的个性化建议生成 + 规则引擎降级策略
  - 新增响应字段：`meal_summary`（眼镜显示）、`advice`（详细建议）、`next_meal_suggestion`（下一餐建议）
  - `/api/v1/vision/analyze_meal_update` 增强：新增 `eating_pace_advice`、`progress_summary`
- ✅ **Bug 修复**：移除零食热量修正逻辑，避免水果等天然食物热量被错误修正

### 眼镜端 ✅ 架构重构完成
- ✅ 瘦客户端架构（不联网）
- ✅ 蓝牙通信模块（BluetoothSender/Receiver）
- ✅ 相机拍照（CameraX）
- ✅ 5分钟自动拍照定时器
- ✅ 侧键交互（短按拍照/长按开始结束）
- ✅ TTS 语音播报框架
- ✅ AR 显示框架
- ⏳ **待完成**: 启用 CXR-S SDK 蓝牙实际连接

### 手机端 ⏳ 待开发
- ⏳ 蓝牙服务端（CXR-M SDK）
- ⏳ NetworkManager（Retrofit + OkHttp）
- ⏳ 用户档案（BMI、健康状况、饮食习惯）
- ⏳ 统计页面（饮食爱好排行、热量摄入图表）
- ⏳ 设备管理（Rokid眼镜连接/配对）

### 文档 ✅
- ✅ README.md（项目主文档）
- ✅ ROKID_DEVELOPMENT_GUIDE.md（开发指南）
- ✅ THREE_AGENT_DEVELOPMENT_GUIDE.md（三端协作）
- ✅ AGENT_TASKS.md（任务清单）

## 🚀 快速开始

### 1. 启动后端服务

```bash
# 进入后端目录
cd backend

# 启动服务
./start.sh

# 验证服务
curl http://localhost:8000/health
```




## 🛠️ 技术栈

### 后端
- **框架**: FastAPI (Python 3.13)
- **AI 模型**: Qwen-VL (qwen-vl-max via DashScope API)
- **数据库**: SQLite (MVP) / PostgreSQL (生产)
- **营养数据**: 
  - 中国食物成分表（优先，中文食物）
  - USDA Foundation Foods（英文食物）
  - FoodStruct（补充数据）
- **营养字段**: 32 个字段（能量、蛋白质、脂肪、碳水、膳食纤维、维生素、矿物质等）

### 眼镜端（瘦客户端）
- **语言**: Kotlin
- **SDK**: CXR-S SDK
- **相机**: CameraX
- **通信**: Android Bluetooth（蓝牙客户端）
- **平台**: Android 9.0+ (YodaOS-Sprite)

### 手机端（网络中枢）
- **语言**: Kotlin
- **UI 框架**: Jetpack Compose
- **网络**: Retrofit + OkHttp
- **数据库**: Room
- **通信**: CXR-M SDK（蓝牙服务端）
- **图表**: MPAndroidChart
- **图片加载**: Coil

## 📊 API 端点

### 后端服务（http://viseat.cn）

#### 健康检查
```
GET /health
```

#### 视觉分析（核心）
```
POST /api/v1/vision/analyze         # Qwen-VL 多食材拆解 + 营养计算
POST /api/v1/vision/analyze_meal_update  # 带容错的会话更新（推荐）
```

##### 1. 基础分析（Start 模式）
**请求**
```json
{"image_url": "http://viseat.cn/uploads/xxx.jpg"}
```

**响应**
```json
{
  "mode": "start",
  "raw_llm": {
    "is_food": true,
    "foods": [
      {
        "dish_name": "Braised Pork",
        "dish_name_cn": "红烧肉",
        "category": "meal",
        "cooking_method": "braise",
        "ingredients": [
          {"name_en": "pork", "name_cn": "猪肉", "weight_g": 150.0, "confidence": 0.9}
        ],
        "total_weight_g": 150.0
      }
    ]
  },
  "snapshot": {
    "foods": [{"name": "pork", "weight_g": 150.0, "cooking_method": "braise"}],
    "nutrition": {"calories": 363.0, "protein": 40.5, "carbs": 0.0, "fat": 21.0}
  }
}
```

##### 2. 基线列表模式（Update - 推荐用于会话）
**请求**
```json
{
  "image_url": "当前图片URL",
  "mode": "update",
  "baseline_foods": [
    {
      "dish_name": "Braised Pork",
      "dish_name_cn": "红烧肉",
      "ingredients": [{"name_en": "pork", "weight_g": 150}],
      "total_weight_g": 150
    }
  ]
}
```

**响应**：与 Start 相同，但 VLM 会使用与基线相同的食材名称，`weight_g` 表示当前剩余量。

##### 3. 带容错的会话更新（最佳实践）
**请求** `POST /api/v1/vision/analyze_meal_update`
```json
{
  "image_url": "当前图片URL",
  "baseline_foods": [...]
}
```

**响应**
```json
{
  "status": "accept|skip|adjust",
  "reason": "normal|no_food_detected|too_many_missing|partial_issues",
  "message": "识别正常",
  "use_last": false,
  "eating_pace_advice": "进食节奏良好，有助于消化吸收",
  "progress_summary": "已摄入 350 kcal，约占本餐 55%",
  "comparison": {
    "warnings": [{"type": "missing", "ingredient": "rice", "suggestion": "keep_last"}],
    "adjustments": [{"action": "add_new", "ingredient": "tofu", "weight": 50}]
  },
  "raw_llm": {...},
  "snapshot": {...}
}
```

**status 状态说明**：
| 状态 | 含义 | 前端处理 |
|------|------|----------|
| `accept` | 识别正常 | 使用当前结果 |
| `skip` | 拍摄问题（未检测到食物或过多食材消失） | 保持上次数据 |
| `adjust` | 检测到加菜或部分异常 | 查看 adjustments 调整基线 |

**字段说明**：
- `category`: 食物类别（`meal`/`snack`/`beverage`/`dessert`/`fruit`）
- `cooking_method`: 烹饪方式（`raw`/`steam`/`boil`/`braise`/`stir-fry`/`deep-fry`/`bake`/`grill`）
- `ingredients[].name_en`: 英文食材名（用于数据库查询）
- `ingredients[].weight_g`: 食材重量（克）

#### 营养相关
```
POST /api/v1/nutrition/calculate    # 计算食物营养总量
GET  /api/v1/nutrition/lookup       # 按名称查询（支持中英文）
GET  /api/v1/nutrition/barcode      # 按条码查询
GET  /api/v1/nutrition/stats        # 获取数据库统计信息
```

**营养查询示例**
```bash
# 查询食物营养（支持中英文）
curl "https://viseat.cn/api/v1/nutrition/lookup?name=猪肉&full=true"

# 获取数据库统计
curl "https://viseat.cn/api/v1/nutrition/stats"
```

#### 用餐会话
```
POST /api/v1/meal/start             # 开始用餐
POST /api/v1/meal/update            # 更新进度
POST /api/v1/meal/end               # 结束用餐（支持智能建议）
GET  /api/v1/meal/session/{id}      # 获取会话详情
GET  /api/v1/meal/sessions          # 获取会话列表
```

##### meal/end 增强版请求（2025-11-27）
```json
{
  "session_id": "abc123",
  "final_snapshot": null,
  "meal_context": {
    "duration_minutes": 25.5,
    "total_consumed_so_far": 650
  },
  "daily_context": {
    "total_calories_today": 1200,
    "meal_count_today": 2
  },
  "user_profile": {
    "age": 28,
    "bmi": 22.5,
    "health_goal": "maintain"
  }
}
```

##### meal/end 增强版响应
```json
{
  "session_id": "abc123",
  "status": "ended",
  "meal_summary": {
    "total_calories": 650,
    "rating": "good",
    "short_advice": "营养均衡，继续保持！"
  },
  "advice": {
    "summary": "本餐营养均衡，蛋白质摄入充足",
    "suggestions": ["用餐时长适中", "今日蛋白质摄入已达 45g"],
    "highlights": ["蛋白质充足", "用餐节奏健康"],
    "warnings": []
  },
  "next_meal_suggestion": {
    "recommended_time": "4小时后",
    "meal_type": "晚餐",
    "calorie_budget": 600,
    "focus_nutrients": ["蔬菜", "膳食纤维"],
    "avoid": ["高糖食物"]
  }
}
```

#### AI 建议
```
POST /api/v1/chat/nutrition         # LLM 营养建议对话
```

#### API 文档
```
http://viseat.cn/docs               # Swagger UI
http://viseat.cn/redoc              # ReDoc
```

## 🔧 配置说明

### 后端配置（backend/.env）
```bash
# Qwen-VL API（必需）
DASHSCOPE_API_KEY=sk-xxx
QWEN_VL_MODEL=qwen-vl-max

# 数据库
DATABASE_URL=sqlite:///./app.db

# 服务配置
HOST=0.0.0.0
PORT=8000
DEBUG=True
```

### 眼镜端配置（android/）
```kotlin
// Config.kt - 眼镜端不联网，通过蓝牙与手机通信
object Config {
    // 蓝牙服务 UUID（需与手机端一致）
    const val BLUETOOTH_SERVICE_UUID = "00009100-0000-1000-8000-00805f9b34fb"
    
    // 自动拍照间隔（5分钟）
    const val AUTO_CAPTURE_INTERVAL_MS = 5 * 60 * 1000L
}
```

### 手机端配置（android-phone/）
```kotlin
// Config.kt - 手机端负责联网
object Config {
    const val API_BASE_URL = "https://viseat.cn"
    const val BLUETOOTH_SERVICE_UUID = "00009100-0000-1000-8000-00805f9b34fb"
}
```
## 📚 文档索引

### 核心文档
- [README.md](README.md) - 项目主文档（产品目标、架构、API）
- [ROKID_DEVELOPMENT_GUIDE.md](ROKID_DEVELOPMENT_GUIDE.md) - 完整开发指南

### 开发协作
- [THREE_AGENT_DEVELOPMENT_GUIDE.md](THREE_AGENT_DEVELOPMENT_GUIDE.md) - 三端协作开发指南
- [AGENT_TASKS.md](AGENT_TASKS.md) - Agent 详细任务清单

### 子项目文档
- [android/README.md](android/README.md) - 眼镜端说明（瘦客户端）
- [backend/README.md](backend/README.md) - 后端服务说明

## 🔍 常见问题

### Q1: 如何验证环境是否正确配置？
```bash
cd android
./verify-setup.sh
```

### Q2: 如何启动后端服务？
```bash
cd backend
./start.sh
```

### Q3: 如何在 Android Studio 中打开项目？
```bash
cd android
open -a "Android Studio" .
```

### Q4: 模拟器如何访问本地后端？
使用 `10.0.2.2` 代替 `localhost`

### Q5: 真机如何访问本地后端？
使用电脑的局域网 IP，或使用 ngrok 公网访问

## 🎯 下一步行动

### 立即可做

1. **验证后端服务**
   ```bash
   curl https://viseat.cn/health
   ```

2. **构建眼镜端应用**
   ```bash
   cd android && ./gradlew assembleDebug
   ```

### 下一阶段开发（手机端）

1. **创建手机端项目**
   - 新建 `android-phone/` 目录
   - 集成 CXR-M SDK（蓝牙服务端）
   - 实现 NetworkManager（Retrofit）

2. **核心功能**
   - 蓝牙接收眼镜图片
   - 调用后端 API 获取结果
   - 蓝牙返回结果给眼镜

3. **用户功能**
   - 用户档案收集（初次注册）
   - 统计页面（饮食爱好、热量摄入）
   - 设备管理（眼镜配对）

## 📞 获取帮助

- 查看 [THREE_AGENT_DEVELOPMENT_GUIDE.md](THREE_AGENT_DEVELOPMENT_GUIDE.md) 了解开发协作
- 查看 [AGENT_TASKS.md](AGENT_TASKS.md) 了解具体任务

---

**项目状态**: ✅ 智能建议系统完成  
**最后更新**: 2025-11-27  
**版本**: 3.3.0  

| 组件 | 状态 | 说明 |
|------|------|------|
| 后端 | ✅ 已优化 | 基线模式 + 容错API + 多数据源营养库 + **智能建议系统** |
| 眼镜端 | ✅ 重构完成 | 瘦客户端架构 |
| 手机端 | ⏳ 待开发 | 下一阶段重点（参考前端适配指南） |
| 蓝牙通信 | ⏳ 待启用 | SDK 待集成 |
| 端到端联调 | ⏳ 待测试 | 三端联调 |
