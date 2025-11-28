# Rokid营养助手 - 后端服务

## ✅ 环境已搭建完成

### 当前状态
- ✅ Python 3.13.7
- ✅ 虚拟环境已创建
- ✅ 所有依赖已安装
- ✅ 营养数据库 CSV 已准备（backend/data）
- ✅ FastAPI营养服务已启动

### 服务地址
- **本地API**: http://localhost:8000
- **健康检查**: http://localhost:8000/health
- **API文档**: http://localhost:8000/docs
- **交互式文档**: http://localhost:8000/redoc

---

## 🚀 快速启动

### 方式1: 使用启动脚本（推荐）
```bash
cd /Users/linjunjie/CascadeProjects/RokidAI/backend
./start.sh
```

### 方式2: 手动启动
```bash
cd /Users/linjunjie/CascadeProjects/RokidAI/backend
source venv/bin/activate
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

---

## 🧪 测试API

### 1. 健康检查
```bash
curl http://localhost:8000/health
```

预期响应：
```json
{
  "status": "healthy",
  "timestamp": "2025-11-09T17:06:35.592278",
  "nutrition_db_loaded": true
}
```

### 2. 测试营养聚合接口
```bash
curl -X POST http://localhost:8000/api/v1/nutrition/aggregate \
  -H "Content-Type: application/json" \
  -d '{
    "foods": [
      {"name": "white rice", "weight_g": 150},
      {"name": "chicken breast", "weight_g": 100}
    ]
  }'
```

### 3. 测试用餐会话（JSON快照）
```bash
curl -X POST "http://localhost:8000/api/v1/meal/start?user_id=test_user&meal_type=lunch" \
  -H "Content-Type: application/json" \
  -d '{
    "foods": [
      {"name": "white rice", "weight_g": 150},
      {"name": "chicken breast", "weight_g": 100}
    ]
  }'
```

### 4. 使用Python测试脚本
```bash
source venv/bin/activate
python test_api.py
```

---

## 📡 内网穿透（让眼镜访问本地服务）

### 安装ngrok
```bash
brew install ngrok
```

### 注册并配置
1. 访问 https://dashboard.ngrok.com/signup
2. 注册账号并获取authtoken
3. 配置token：
```bash
ngrok config add-authtoken YOUR_AUTH_TOKEN
```

### 启动隧道
```bash
ngrok http 8000
```

你会得到一个公网URL，例如：
```
Forwarding  https://xxxx-xx-xx-xx-xx.ngrok-free.app -> http://localhost:8000
```

**将这个URL配置到眼镜端应用中！**

---

## 📁 项目结构

```
backend/
├── app/
│   ├── __init__.py
│   └── main.py          # FastAPI主应用（营养与会话服务）
├── data/                # 数据文件（营养数据库CSV等）
├── venv/                # Python虚拟环境
├── .env                 # 环境变量（数据库、端口等）
├── requirements.txt     # Python依赖
├── start.sh            # 启动脚本
├── test_api.py         # 测试脚本
└── README.md           # 本文件
```

---

## 🔧 配置说明

### .env 文件
```bash
# 数据库配置（默认使用SQLite）
DATABASE_URL=sqlite:///./app.db

# Open Food Facts 配置（可选）
OPEN_FOOD_FACTS_BASE_URL=https://world.openfoodfacts.org

# 服务配置
HOST=0.0.0.0
PORT=8000
DEBUG=True

# CORS配置
ALLOWED_ORIGINS=*
```

**⚠️ 注意**: 不要将.env文件提交到Git仓库！

---

## 📊 API端点说明

### GET /
根路径，返回服务信息

### GET /health
健康检查，返回服务状态和营养数据库加载状态

### POST /api/v1/nutrition/aggregate
根据食物列表聚合营养成分（总热量、蛋白质、碳水、脂肪）

### GET /api/v1/nutrition/lookup?name=...
按名称查询每100g营养信息

### GET /api/v1/nutrition/barcode?barcode=...
按条码从 Open Food Facts 查询包装食品营养

### POST /api/v1/meal/start
开始用餐会话，Body 为 JSON 快照（foods + 可选 nutrition）

### POST /api/v1/meal/update
更新用餐会话，Body 为 JSON 快照

### POST /api/v1/meal/end
结束用餐会话，可附带最终快照

### GET /api/v1/meal/session/{id}
获取单个用餐会话详情

### GET /api/v1/meal/sessions?user_id=xxx[&status=active]
获取用户会话列表

---

## 🔍 常见问题

### Q1: 服务启动失败
```bash
# 检查端口是否被占用
lsof -i :8000

# 如果被占用，杀死进程
kill -9 <PID>
```

### Q2: Qwen API调用失败
检查.env文件中的API密钥是否正确：
```bash
cat .env | grep DASHSCOPE_API_KEY
```

### Q3: 图片上传失败
确保图片格式为JPEG/PNG，大小<10MB

### Q4: ngrok连接不稳定
免费版ngrok会话有时间限制，需要定期重启。考虑升级到付费版或使用其他内网穿透工具（frp、Cloudflare Tunnel）

---

## 📈 性能优化

### 当前配置（MVP）
- Qwen API调用：3-5秒/张
- 图片压缩：1024x1024, 85%质量
- 并发处理：默认（单进程）

### 后续优化方向
1. **使用4090本地部署Qwen-VL-Chat**
   - 推理速度：5-8秒/张
   - 完全离线，无API费用
   - 需要16GB显存

2. **添加Redis缓存**
   - 缓存常见食物识别结果
   - 减少API调用次数

3. **使用Gunicorn多进程**
   - 提高并发处理能力
   - 适合生产环境

---

## 🚀 下一步

1. ✅ 云端服务已搭建完成
2. ⏭️ 配置ngrok内网穿透
3. ⏭️ 开发眼镜端Android应用
4. ⏭️ 开发手机端Android应用
5. ⏭️ 端到端测试

---

## 📞 技术支持

如有问题，请查看：
- FastAPI文档: https://fastapi.tiangolo.com/
- Qwen API文档: https://help.aliyun.com/zh/dashscope/
- ngrok文档: https://ngrok.com/docs

---

**更新时间**: 2025-11-17
**版本**: 1.0.0
