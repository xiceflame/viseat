#!/bin/bash
# 实时查看 Rokid 后端日志

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
LOG_DIR="$PROJECT_ROOT/logs"
LOG_FILE="$LOG_DIR/backend.log"
TAIL_LINES=${TAIL_LINES:-200}

if [ ! -f "$LOG_FILE" ]; then
  echo "❌ 未找到日志文件: $LOG_FILE"
  echo "➡️  请先运行 start.sh 以启动服务并生成日志。"
  exit 1
fi

echo "📄 正在 tail 日志: $LOG_FILE"
echo "📌 提示: 通过设置 TAIL_LINES=100 tail_logs.sh 可调整初始行数"
echo

tail -n "$TAIL_LINES" -F "$LOG_FILE"
