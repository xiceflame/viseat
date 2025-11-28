# Rokid 智能营养助手 - 项目总览

> **版本**: 3.0.0 | **状态**: 架构重构中 | **更新**: 2025-11-25

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
│   ├── app/main.py                       # FastAPI 主应用
│   ├── data/                             # 营养数据库 (foodstruct)
│   ├── test_api.py
│   └── test_meal_session.py
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
- ✅ 营养数据库（foodstruct, USDA, Open Food Facts）
- ⏳ **待验证**: VLM→数据库→热量计算→LLM建议 数据流准确性

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

### 2. 配置内网穿透（可选）

```bash
# 安装 ngrok
brew install ngrok

# 启动隧道
ngrok http 8000

# 复制生成的公网 URL
```

### 3. 开发 Android 应用

#### 方式 A: 使用 Android Studio（推荐）

```bash
cd android
./quick-start.sh
# 选择选项 1: 使用 Android Studio 打开项目
```

#### 方式 B: 命令行开发

```bash
cd android

# 验证环境
./verify-setup.sh

# 构建项目
./gradlew build

# 创建模拟器
./quick-start.sh
# 选择选项 3
```

## 📝 开发路线图

### Phase 1: 后端服务 ✅ 已完成
- [x] FastAPI + Qwen-VL 集成
- [x] 用餐会话 API（start/update/end）
- [x] 动态基线策略
- [x] 部署到 viseat.cn
- [ ] **待验证**: VLM→数据库→热量计算→LLM建议 数据流准确性

### Phase 2: 眼镜端重构 ✅ 已完成
- [x] 瘦客户端架构（移除网络调用）
- [x] 蓝牙通信模块（BluetoothSender/Receiver）
- [x] 5分钟自动拍照定时器
- [x] 侧键交互逻辑
- [ ] **待完成**: 启用 CXR-S SDK 蓝牙实际连接

### Phase 3: 手机端应用 ⏳ 进行中（1周）
- [ ] 新建 `android-phone/` 项目
- [ ] 蓝牙服务端（CXR-M SDK）
- [ ] NetworkManager（Retrofit 调用后端）
- [ ] **用户档案**：BMI、健康状况、饮食习惯（初次注册收集）
- [ ] **统计页面**：饮食爱好排行、时间规律、热量摄入图表
- [ ] **设备管理**：Rokid眼镜连接/配对/设置

### Phase 4: 端到端联调（3-5天）
- [ ] 眼镜 → 手机 → 后端 完整流程
- [ ] 三阶段测试：开始 → 自动监测 → 结束
- [ ] **结束报告**：整体评价 + 营养饼图 + 今日总摄入占比
- [ ] 异常处理（网络断开、眼镜断开）

### Phase 5: 功能完善（1周）
- [ ] **个性化建议**：结合用户档案（如脂肪肝→低油建议）
- [ ] 手机端：趋势分析、历史编辑
- [ ] 眼镜端：AR显示优化

## 🛠️ 技术栈

### 后端
- **框架**: FastAPI (Python 3.13)
- **AI 模型**: Qwen-VL (DashScope API)
- **数据库**: SQLite (MVP) / PostgreSQL (生产)
- **营养数据**: USDA、CFCT、foodstruct、Open Food Facts

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
```

**请求**
```json
{"image_url": "http://viseat.cn/uploads/xxx.jpg"}
```

**响应**（VLM 返回多菜品结构）
```json
{
  "raw_llm": {
    "is_food": true,
    "foods": [
      {"dish_name": "红烧肉", "cooking_method": "braise", "ingredients": [{"name_en": "pork", "weight_g": 150}]},
      {"dish_name": "米饭", "cooking_method": "steam", "ingredients": [{"name_en": "rice", "weight_g": 200}]}
    ]
  },
  "snapshot": {
    "foods": [...],
    "nutrition": {"calories": 650, "protein": 25, "carbs": 80, "fat": 28}
  }
}
```

**字段说明**：
- `cooking_method`: 英文烹饪方式（`raw`/`steam`/`boil`/`braise`/`stir-fry`/`deep-fry`）
- `name_en`: 英文食材名（用于数据库查询，如 `pork`, `rice`, `chicken breast`）

#### 营养相关
```
POST /api/v1/nutrition/aggregate    # 营养聚合
GET  /api/v1/nutrition/lookup       # 按名称查询
GET  /api/v1/nutrition/barcode      # 按条码查询
```

#### 用餐会话
```
POST /api/v1/meal/start             # 开始用餐
POST /api/v1/meal/update            # 更新进度
POST /api/v1/meal/end               # 结束用餐
GET  /api/v1/meal/session/{id}      # 获取会话详情
GET  /api/v1/meal/sessions          # 获取会话列表
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
# 数据库
DATABASE_URL=sqlite:///./app.db

# Open Food Facts
OPEN_FOOD_FACTS_BASE_URL=https://world.openfoodfacts.org

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

## 🧪 测试

### 后端测试
```bash
cd backend
source venv/bin/activate

# 测试 API
python test_api.py

# 测试用餐会话
python test_meal_session.py

# 测试图片识别
python test_with_real_image.py
```

### Android 测试
```bash
cd android

# 单元测试
./gradlew test

# UI 测试
./gradlew connectedAndroidTest
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

**项目状态**: 🟡 架构重构中  
**最后更新**: 2025-11-25  
**版本**: 3.0.0  

| 组件 | 状态 | 说明 |
|------|------|------|
| 后端 | ✅ 已部署 | viseat.cn |
| 眼镜端 | ✅ 重构完成 | 瘦客户端架构 |
| 手机端 | ⏳ 待开发 | 下一阶段重点 |
| 蓝牙通信 | ⏳ 待启用 | SDK 待集成 |
| 端到端联调 | ⏳ 待测试 | 三端联调 |
