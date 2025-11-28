# Rokid智能营养助手

> **版本**: 3.0.0 | **状态**: 架构重构中 | **后端**: ✅ 已部署 viseat.cn | **手机端**: ⏳ 待开发

基于Rokid AR眼镜的实时食物营养分析系统，**持续监测进餐过程，了解吃了哪些食品，实时给出个性化建议**。

---

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
用户档案: 45岁 + 轻度脂肪肝 → 开始建议: "低油蓬菜为主" → 结束评价: "本餐红烧肉热量较高"
```

**用户档案收集**：初次注册时收集（BMI、健康状况、饮食习惯），保存到数据库，之后不再提示。

---

## 项目概述

本项目采用 **"眼镜端（瘦客户端）+ 手机端（网络中枢）+ 后端 LLM 服务" 三层架构**：

| 端 | 核心职责 | SDK |
|----|----------|-----|
| **眼镜** | 拍照、AR显示、TTS播报、侧键交互、5分钟定时器 | CXR-S SDK |
| **手机** | 蓝牙通信、调用API、用户档案、统计UI、设备管理 | CXR-M SDK |
| **后端** | VLM识别、营养查询+计算、LLM建议、会话管理 | FastAPI |

### 设计理念
- **眼镜端轻量化**：专注拍照、AR 显示、语音播报，通过蓝牙与手机通信，不直接联网
- **手机端网络中枢**：负责联网、API 调用、数据存储（SQLite/Room）、会话管理、统计趋势与历史记录
- **后端 LLM 双层服务**：视觉识别与营养估算、对话/总结与个性化建议全部在 FastAPI 后端完成
- **统一 HTTP API**：手机端通过 Retrofit + OkHttp 访问 `/api/v1/upload`、`/api/v1/vision/analyze`、`/api/v1/nutrition/*` 等接口
- **低成本上线**：Mac 本地 FastAPI + ngrok 公网访问，或部署到云服务器
- **动态用餐跟踪**：按照会话聚合多次拍照，自动识别中途出现的新菜品，基于“总上菜量 - 当前剩余”动态计算实际摄入。
- **安全与鲁棒性**：内置非食物检测与滥用防护，当图片中没有食物时直接返回错误提示而不计入会话。
- **简洁交互**：“开始用餐 → 自动监测 → 结束用餐”的会话式体验

### 主要功能

**眼镜端（Rokid AR - 瘦客户端）**
- 📸 **拍照**：侧键短按拍照，图片通过蓝牙发送给手机
- ⏱️ **5分钟定时器**：自动拍照并发送给手机（无感监测）
- 👁️ **AR显示**：接收手机返回的营养信息并叠加显示
- 🔊 **TTS播报**：关键营养数据语音提示
- 🔗 **蓝牙通信**：与手机端实时双向通信

**手机端（网络中枢）**
- 📱 **蓝牙通信**：与眼镜收发图片/结果 (CXR-M SDK)
- 🌐 **服务器通信**：调用后端 API (Retrofit + OkHttp)
- 👤 **用户档案**：BMI计算、健康状况（脂肪肝等）、饮食习惯（初次注册时收集）
- 📊 **统计页面**：饮食爱好排行(当月)、时间规律(用餐时长/时间)、热量摄入(当天/当周)
- ⚙️ **设备管理**：Rokid眼镜连接/配对/设置 (SDK提供)
- 💾 **本地存储**：Room 存储用餐会话、历史记录、用户设置

**云端服务（已部署 viseat.cn）**
- 🤖 **VLM识别**：Qwen-VL 多食材拆解与重量估算
- 🧮 **营养计算**：查询食品数据库 + 烹饪方式加权
- 💡 **LLM建议**：结合用户档案生成个性化建议
- 📊 **会话管理**：开始/更新/结束用餐会话，动态基线追踪

## 系统架构（V3.0 - 眼镜+手机+后端三层架构）

### 核心理念
**"戴上眼镜，你的AI营养师"** - 眼镜专注交互体验，手机负责网络与数据

### 整体架构图

```
┌─────────────────────────────────────────────────────────────────┐
│              Rokid AR眼镜（瘦客户端 - CXR-S SDK）                │
│                                                                   │
│  • 硬件交互：拍照 / 侧键 / 语音识别                              │
│  • AR 显示：营养信息叠加                                         │
│  • TTS 播报：语音反馈                                            │
│  • 不直接联网，通过蓝牙与手机通信                                │
└───────────┬────────────────────────────────────────────────────┘
            │ 蓝牙（图片/指令/结果 双向传输）
┌───────────▼────────────────────────────────────────────────────┐
│               手机端 Android 应用（网络中枢）                    │
│                                                                   │
│  • 蓝牙服务：接收眼镜图片/指令，返回识别结果                     │
│  • 网络层：Retrofit + OkHttp 调用 FastAPI 后端                  │
│  • 本地存储：Room/SQLite 存储会话与历史                          │
│  • UI 界面：会话管理 / 历史记录 / 统计趋势 / 设置               │
└───────────┬────────────────────────────────────────────────────┘
            │ HTTPS（Retrofit + OkHttp）
┌───────────▼────────────────────────────────────────────────────┐
│         FastAPI LLM 与营养会话服务（Docker / 云端）            │
│  • FastAPI + SQLAlchemy（SQLite/PostgreSQL）                   │
│  • 大模型调用：Qwen-VL（食材拆解）+ Qwen-Text/Qwen2.5（总结与建议）
│  • 营养数据库：USDA + CFCT + foodstruct CSV + OFF 条码        │
│  • API：/api/v1/nutrition/*（聚合/查表/条码）                  │
│  • API：/api/v1/meal/*（开始/更新/结束/查询）                 │
│  • API：/api/v1/vision/analyze, /api/v1/chat/nutrition         │
└─────────────────────────────────────────────────────────────────┘
```

### 双层AI架构说明

#### 第一层：视觉识别（Qwen-VL）
专注于精确的食物识别和营养分析
- 输入：用户拍摄的食物照片
- 输出：结构化的食物数据（名称、重量、营养成分）
- 特点：快速、准确、专业

#### 第二层：对话Agent（Qwen2.5 + RAG）
智能对话和个性化建议
- 输入：用户语音/文本问题
- RAG增强：历史记录 + 营养知识库 + 用户画像
- 输出：个性化的建议和分析
- 特点：理解上下文、记忆历史、主动建议

### 完整使用流程

#### 场景1：拍照识别
1. **用户拍照**：用餐时通过侧键拍照
2. **图片上传**：HTTPS POST 上传图片文件到 `/api/v1/upload`，获取 URL
3. **第一层AI**：调用 `/api/v1/vision/analyze` (传入 URL) 识别食物种类和重量；若模型判断图片不包含食物，则返回 400 错误并提示用户重新拍摄，避免将非食物图片计入记录。
4. **营养映射**：查询营养数据库，计算营养成分
5. **结果显示**：眼镜AR显示 + TTS播报（3-6秒）
6. **数据保存**：存入本地SQLite + 同步云端

#### 场景2：对话咨询
1. **用户提问**："今天吃了多少热量？"
2. **语音识别**：ASR转文本
3. **RAG检索**：查询用户历史记录 + 营养知识库
4. **第二层AI**：Qwen2.5生成个性化回复
5. **语音播报**：TTS播报回答
6. **持续对话**：支持多轮对话

#### 场景3：趋势查看
1. **用户切换**：侧键长按切换到统计页
2. **数据加载**：从本地SQLite读取
3. **图表展示**：今日/本周/本月趋势
4. **健康评分**：营养均衡度分析
5. **主动建议**：Agent给出改善建议

## 用餐会话（开始/结束 + 自动监测）

- 开始：用户点击“开始用餐”，上传首张照片，系统创建会话并记录首帧基线快照。
- 自动监测：按设定间隔（默认5分钟）静默上传照片更新进度，计算本次摄入与进食速度，给出即时建议。
- 结束：用户点击“结束用餐”，可上传最终照片；系统基于“动态基线”计算本餐总摄入——按会话累计所有快照中首次出现的菜品作为总上菜量，以最新快照的剩余热量作为当前剩余，从而支持中途加菜/新发现食物，生成本餐报告并存档。

## 提高热量与重量准确性的策略（摘要）

- 用餐前后对比（推荐）：用餐前/后各拍一张，直接计算实际摄入，准确度高、成本低、交互简单。
- 连续监测（可选）：用餐中每1-5分钟自动采样，获得实时摄入与进食速度，准确度最高但更耗电与成本。
- 动态基线追踪（当前实现）：每次上传的是“当前剩余”的完整快照，不要求模型输出 delta，后端在会话内部自动构建“总上菜量”，避免出现负摄入并支持中途加菜。
- 多角度估算（可选）：俯视+侧视近似体积，提升重量估算，复杂度与交互成本较高。

## 技术栈（V3.0版本）

### 1. Rokid眼镜端（瘦客户端）
- **平台**: Android 10+ (YodaOS-Sprite)
- **语言**: Kotlin
- **SDK**: CXR-S SDK（眼镜端开发）
- **核心库**:
  - CameraX（拍照）
  - Android Bluetooth（蓝牙通信）
  - Gson（JSON解析）
  - Android TTS（语音播报）
  - Android Speech Recognition（语音识别）
- **功能**:
  - 拍照并通过蓝牙发送给手机
  - 接收手机返回的识别结果并 AR 显示
  - 侧键交互（短按拍照、长按切换）
  - TTS 语音播报
- **特点**: 不直接联网，通过蓝牙与手机通信，APK < 5MB

### 2. 手机端（网络中枢）
- **平台**: Android 8.0+
- **语言**: Kotlin
- **UI框架**: Jetpack Compose
- **核心库**:
  - Android Bluetooth（蓝牙服务端）
  - Retrofit + OkHttp（网络请求）
  - Room（本地数据库）
  - Gson（JSON解析）
  - MPAndroidChart（趋势图表）
  - Coil（图片加载）
- **页面功能**:
  - 主页（连接状态、当前会话）
  - 会话管理（用餐进行中/历史）
  - 统计趋势（图表展示）
  - 历史记录（时间轴）
  - 设置（后端地址、蓝牙配对）
- **职责**: 联网、API调用、数据存储、会话状态管理

### 3. 云端服务（双层AI架构）

#### 第一层：视觉识别服务
- **API框架**: FastAPI (Python 3.10+)
- **AI模型**: Qwen-VL-Max (DashScope API)
- **功能**: 食物识别、重量估算、营养计算

#### 第二层：对话Agent服务
- **AI模型**: Qwen2.5-Max (DashScope API)
- **RAG框架**: LangChain / LlamaIndex
- **向量数据库**: ChromaDB / pgvector
- **知识库**:
  - 用户历史记录
  - 营养知识库
  - 饮食指南
- **功能**: 多轮对话、个性化建议、趋势分析

#### 数据层
- **数据库**: PostgreSQL 15 (用户数据、历史记录)
- **缓存**: Redis 7 (会话管理、用户画像)
- **营养数据**: USDA FoodData Central + CFCT
- **部署**: Docker Compose / Mac本地

## MVP实现方案（V3.0 - 眼镜+手机+后端三层架构）

### 方案特点
- ✅ **快速上线**：4周内完成核心功能
- ✅ **眼镜端轻量**：专注拍照与显示，通过蓝牙与手机通信
- ✅ **手机端中枢**：负责联网、数据存储、会话管理
- ✅ **双层AI在后端**：视觉识别 + 对话/总结均由 FastAPI 中的大模型服务完成
- ✅ **云端可控**：本地 MacBook 或云服务器部署

### 开发范围说明

- 采用眼镜+手机+后端三层架构。
- **眼镜端**：拍照、AR 显示、TTS 播报、侧键交互，通过蓝牙与手机通信。
- **手机端**：联网、调用 FastAPI 后端 API、数据存储（Room）、会话管理、统计趋势、历史记录。
- **云端**：FastAPI 统一调用 Qwen-VL 进行食物识别与营养估算，调用 Qwen2.5/Qwen-Text 生成个性化建议，并通过公开 HTTP API（`/api/v1/vision/analyze`、`/api/v1/nutrition/*`、`/api/v1/meal/*`、`/api/v1/chat/nutrition`）提供服务。
- 用餐会话：开始 → 自动监测 → 结束，生成本餐报告并入库。

### 快速开始



#### 3. 数据流说明

1. **眼镜拍照**：用户按侧键拍照，眼镜通过蓝牙将图片发送给手机。
2. **手机上传**：手机通过 `POST /api/v1/upload` 上传图片到后端，获取 URL。
3. **视觉分析**：手机调用 `POST /api/v1/vision/analyze` 请求后端分析。
4. **营养计算**：FastAPI 后端调用 Qwen-VL 做多食材拆解，计算总营养。
5. **结果返回**：手机通过蓝牙将识别结果发送给眼镜，眼镜 AR 显示并 TTS 播报。
6. **会话管理**：手机通过 `/api/v1/meal/start|update|end` 管理用餐会话。

### 眼镜与手机通信

- **通信方式**：蓝牙（SDK 支持）
- **眼镜 → 手机**：图片数据、用户指令（开始/结束用餐）
- **手机 → 眼镜**：识别结果、营养信息、建议文本
- 手机作为蓝牙服务端，眼镜作为客户端连接

## 快速开始

### 前置要求

1. **硬件设备**
   - Rokid AR眼镜（一台）
   - MacBook/PC（作为云端服务器）

2. **开发者账号**
   - 阿里云DashScope账号（Qwen API）: https://dashscope.aliyun.com

3. **开发环境**
   - Python 3.10+ （云端服务）
   - Android Studio 2023.1+（可选，用于眼镜端应用开发）
   - Docker & Docker Compose（可选）

### 30分钟快速部署

#### 步骤1：云端服务部署（10分钟）

**方式A：Docker一键部署（推荐）**
```bash
# 1. 克隆项目
git clone https://github.com/yourusername/rokid-nutrition-assistant.git
cd rokid-nutrition-assistant/backend

# 2. 配置环境变量
cp .env.example .env
nano .env  # 填入Qwen API Key

# .env配置示例：
# QWEN_API_KEY=sk-xxxxx              # 必填
# DATABASE_URL=postgresql://...      # 自动配置
# API_HOST=0.0.0.0
# API_PORT=8000

# 3. 一键启动（包含PostgreSQL+Redis+API）
docker-compose up -d

# 4. 验证服务
curl http://localhost:8000/health
# 输出：{"status": "healthy"}

# 5. 查看API文档
open http://localhost:8000/docs
```

**方式B：本地Python部署**
```bash
# 1. 安装依赖
cd backend
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt

# 2. 启动服务（自动使用SQLite，无需PostgreSQL）
uvicorn main:app --host 0.0.0.0 --port 8000 --reload

# MVP阶段：可用内存数据库快速测试
```

#### 步骤2：公网访问（ngrok）（10分钟）

```bash
brew install ngrok
ngrok config add-authtoken YOUR_AUTH_TOKEN
ngrok http 8000
# 将生成的公网URL配置到眼镜端应用的API_BASE_URL
```

### 网络配置

#### 本地开发（推荐）
```
MacBook (云端): 192.168.1.100:8000
Rokid眼镜: 连接同一WiFi，配置API地址为上述IP
```

#### 公网部署（可选）
```
# 使用ngrok或frp暴露本地服务
ngrok http 8000
# 将生成的公网地址配置到眼镜端
```

## API文档（云端服务接口）

### 核心接口

#### 1. 用餐会话（开始/更新/结束）
```http
POST /api/v1/upload          # 上传图片文件，返回 {url: "..."}

POST /api/v1/meal/start      # 开始会话（首张图）
form-data: image (已弃用，建议先 Upload 后传 URL) 或 JSON


POST /api/v1/meal/update   # 自动更新（间隔上传）
query: session_id
form-data: image

POST /api/v1/meal/end      # 结束会话（可带最终图）
query: session_id
form-data: final_image (optional)

GET  /api/v1/meal/session/{id}  # 会话详情
GET  /api/v1/meal/sessions?user_id=xxx[&status=active]
```

#### 2. 视觉识别与非食物检测
```http
POST /api/v1/vision/analyze
Content-Type: application/json

Body:
{
  "image_url": "https://.../your_image.jpg",
  "question": "可选的补充说明"
}

Response (正常):
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

# 字段说明:
# - cooking_method: 英文烹饪方式 (raw/steam/boil/braise/stir-fry/deep-fry)
# - name_en: 英文食材名，用于数据库查询 (如 pork, rice, chicken breast)

# 手机端处理:
# 1. 从 raw_llm.foods[].dish_name 提取菜品名称
# 2. 组合成简洁描述（如 "红烧肉 · 米饭"）
# 3. 从 snapshot.nutrition 提取营养数据
# 4. 通过蓝牙发送给眼镜

# 非食物图片：返回 HTTP 400
# Response: { "detail": "未检测到食物，请拍摄清晰的食物照片" }
```

#### 3. 营养聚合与查表
```http
POST /api/v1/nutrition/aggregate
Content-Type: application/json

Body:
{
  "foods": [
    {"name": "rice", "weight_g": 150},
    {"name": "chicken breast", "weight_g": 100}
  ]
}

Response:
{
  "foods": [...],                      # 每道菜的营养细分
  "total": {"calories":339.0, "protein":34.9, "carbs":38.9, "fat":4.1}
}
```

### 完整API文档
- Swagger UI: http://your-server:8000/docs
- ReDoc: http://your-server:8000/redoc

## 项目结构（当前仓库）

```
RokidAI/
├── android/                       # 眼镜端 Android 应用（瘦客户端）✅
│   ├── app/src/main/kotlin/       # Kotlin 源代码
│   └── build.gradle.kts
│
├── android-phone/                 # 手机端 Android 应用（网络中枢）⏳ 待开发
│   ├── app/src/main/kotlin/
│   │   ├── bluetooth/             # 蓝牙服务 (CXR-M SDK)
│   │   ├── network/               # API 调用 (Retrofit)
│   │   ├── database/              # 本地存储 (Room)
│   │   └── ui/                    # 统计页面、用户档案
│   └── build.gradle.kts
│
├── backend/                       # FastAPI 后端服务 ✅ 已部署 viseat.cn
│   ├── app/main.py                # FastAPI 主应用
│   ├── data/                      # 营养数据库 (foodstruct)
│   ├── test_api.py
│   └── test_meal_session.py
│
├── ROKID_DEVELOPMENT_GUIDE.md     # 完整开发指南
├── THREE_AGENT_DEVELOPMENT_GUIDE.md # 三端协作开发指南
├── AGENT_TASKS.md                 # Agent 详细任务清单
└── README.md                      # 项目主文档
```



## 配置说明



### Qwen模型配置

```python
# 使用Qwen VL Plus (推荐)
model = 'qwen-vl-plus'

# 或使用Qwen VL Max (更高精度)
model = 'qwen-vl-max'

# 调用示例
response = MultiModalConversation.call(
    model=model,
    messages=[{
        "role": "user",
        "content": [
            {"image": image_base64},
            {"text": prompt}
        ]
    }]
)
```

## 性能优化

### 图像处理
- 压缩至1024x1024分辨率
- JPEG质量85%
- 预估传输大小: 200-500KB

### 响应时间
- 拍照: <100ms
- 图像上传: 1-2s (取决于网络)
- AI分析: 2-4s
- 总响应时间: 3-6s

### 缓存策略
- Redis缓存常见食物营养数据
- 本地缓存用户历史记录
- 离线模式支持基础功能

## 故障排除

### 常见问题

**1. Rokid SDK初始化失败**
```
错误: SDK初始化失败
解决: 检查AppKey、AppSecret是否正确，确保网络连接正常
```

**2. 相机权限被拒绝**
```
错误: Camera permission denied
解决: 在设置中授予应用相机权限
```

**3. API调用超时**
```
错误: Connection timeout
解决: 检查后端服务是否运行，防火墙是否开放8000端口
```

**4. Qwen API配额不足**
```
错误: API quota exceeded
解决: 检查DashScope账户余额，升级套餐
```

## 性能指标与优化

### MVP版本目标

| 指标 | 目标值 | 说明 |
|------|--------|------|
| 端到端延迟 | 3-6秒 | 拍照→显示结果 |
| 图像上传 | <2秒 | 1024x1024 JPEG 85% |
| AI推理 | 2-4秒 | Qwen VL云端调用 |
| 眼镜端APK | <10MB | 无本地模型 |
| 识别准确率 | >80% | 常见中餐菜品Top-3 |
| 营养误差 | <20% | 热量估算MAPE |
| 并发支持 | 10用户 | MVP本地部署 |

### 优化策略

#### 1. 图像传输优化
```python
# 压缩策略
- 分辨率: 1024x1024 (从4000x3000降采样)
- 格式: JPEG 质量85%
- 预估大小: 200-500KB
- 上传时间: 1-2秒 (4G网络)
```

#### 2. AI推理加速（可选）
```python
# 方案A：纯Qwen VL（简单）
- 优点：无需本地模型，快速上线
- 延迟：3-5秒
- 成本：~¥0.02/次

# 方案B：YOLOv8预处理（进阶）
- 本地YOLOv8检测→裁剪食物区域→Qwen VL识别
- 优点：减少Qwen输入尺寸，提速20%
- 延迟：2-4秒
```

#### 3. 缓存机制
```python
# Redis缓存常见食物
- 缓存命中率目标: >60%
- 缓存有效期: 30天
- 示例：米饭、鸡蛋等100+常见食物
```

## 开发路线图

### Phase 1：后端服务 ✅ 已完成
- [x] 云端 FastAPI + Qwen-VL 集成
- [x] 用餐会话 API（开始/更新/结束/查询）
- [x] 动态基线策略（支持中途加菜）
- [x] 非食物检测（返回 400 错误）
- [x] 部署到 viseat.cn 云服务器
- [ ] **待验证**：VLM识别 → 数据库查询 → 热量计算 → LLM建议 数据流准确性

### Phase 2：手机端应用 ⏳ 进行中（1周）
- [ ] 新建 `android-phone/` 项目
- [ ] 蓝牙服务端实现 (CXR-M SDK)
- [ ] NetworkManager（调用 FastAPI 后端）
- [ ] **用户档案**：BMI计算、健康状况、饮食习惯（初次注册收集）
- [ ] **统计页面**：饮食爱好排行、时间规律、热量摄入图表
- [ ] **设备管理**：Rokid眼镜连接/配对/设置

### Phase 3：眼镜端改造（3-5天）
- [ ] 移除直接联网逻辑
- [ ] 添加蓝牙客户端（与手机通信）
- [ ] **5分钟定时器**：自动拍照
- [ ] 保留：拍照、AR 显示、TTS、侧键监听

### Phase 4：端到端联调（3-5天）
- [ ] 眼镜 → 手机 → 后端 完整流程测试
- [ ] 用餐会话：开始 → 自动监测 → 结束
- [ ] **结束报告**：整体评价 + 营养饼图 + 今日总摄入占比
- [ ] 异常处理（网络断开、眼镜断开）

### Phase 5：功能完善（1周）
- [ ] **个性化建议**：结合用户档案（如脂肪肝 → 低油建议）
- [ ] 手机端：统计图表、趋势分析、历史编辑
- [ ] 眼镜端：AR 显示优化、语音识别优化

## 贡献指南

欢迎贡献代码、报告问题或提出建议！

1. Fork本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启Pull Request

## 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

## 联系方式

- 项目主页: https://github.com/yourusername/rokid-nutrition-assistant
- 问题反馈: https://github.com/yourusername/rokid-nutrition-assistant/issues
- 邮箱: your.email@example.com

## 致谢

- [Rokid](https://www.rokid.com/) - 提供AR眼镜硬件和SDK支持
- [阿里云DashScope](https://dashscope.aliyun.com/) - 提供Qwen大模型服务
- [USDA FoodData Central](https://fdc.nal.usda.gov/) - 营养数据来源

## 参考资料

- [Rokid开发者文档](https://developer.rokid.com/)
- [Qwen VL模型文档](https://help.aliyun.com/zh/dashscope/developer-reference/qwen-vl-plus)
- [Android Camera2 API](https://developer.android.com/training/camera2)
- [FastAPI文档](https://fastapi.tiangolo.com/)

---

**注意**: 本项目仅供学习和研究使用，营养数据仅供参考，不能替代专业营养师的建议。
