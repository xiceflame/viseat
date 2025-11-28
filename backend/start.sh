#!/bin/bash

# 激活虚拟环境
source venv/bin/activate

# 启动FastAPI服务
echo "🚀 启动Rokid营养助手后端服务..."
echo "📍 服务地址: http://localhost:8000"
echo "📖 API文档: http://localhost:8000/docs"
echo ""

uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
