#!/bin/bash

# 激活虚拟环境
source venv/bin/activate

LOG_DIR="logs"
LOG_FILE="$LOG_DIR/backend.log"
mkdir -p "$LOG_DIR"

# 启动FastAPI服务
echo "🚀 启动Rokid营养助手后端服务..."
echo "📍 服务地址: http://localhost:8000"
echo "📖 API文档: http://localhost:8000/docs"
echo "🗂️ 日志文件: $LOG_FILE"
echo ""

# 将 uvicorn 输出写入日志并同时打印到控制台
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload 2>&1 | tee -a "$LOG_FILE"
