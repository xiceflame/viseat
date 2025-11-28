#!/bin/bash
# Rokid 营养助手后端 API 测试脚本
# 用法: ./test_api.sh [BASE_URL]
# 示例: ./test_api.sh http://localhost:8000
#       ./test_api.sh http://viseat.cn

set -e

BASE_URL=${1:-"http://localhost:8000"}
echo "====================================="
echo "  Rokid 营养助手 API 测试"
echo "  Base URL: $BASE_URL"
echo "====================================="

# 检查 jq 是否安装
if ! command -v jq &> /dev/null; then
    echo "警告: jq 未安装，输出将不会格式化"
    JQ="cat"
else
    JQ="jq"
fi

echo -e "\n=== 1. 健康检查 ==="
curl -s "$BASE_URL/health" | $JQ

echo -e "\n=== 2. 营养查询 (lookup) ==="
curl -s "$BASE_URL/api/v1/nutrition/lookup?name=rice" | $JQ

echo -e "\n=== 3. 营养聚合 (aggregate) ==="
curl -s -X POST "$BASE_URL/api/v1/nutrition/aggregate" \
  -H "Content-Type: application/json" \
  -d '{
    "foods": [
      {"name": "rice", "weight_g": 200},
      {"name": "chicken breast", "weight_g": 150}
    ]
  }' | $JQ

echo -e "\n=== 4. 开始用餐会话 (meal/start) ==="
START_RESULT=$(curl -s -X POST "$BASE_URL/api/v1/meal/start?user_id=test_user&meal_type=lunch" \
  -H "Content-Type: application/json" \
  -d '{
    "foods": [
      {"name": "rice", "weight_g": 200},
      {"name": "pork", "weight_g": 150}
    ],
    "nutrition": {
      "calories": 650,
      "protein": 30,
      "carbs": 80,
      "fat": 25
    }
  }')
echo "$START_RESULT" | $JQ

SESSION_ID=$(echo "$START_RESULT" | jq -r '.session_id' 2>/dev/null || echo "")
if [ -z "$SESSION_ID" ] || [ "$SESSION_ID" = "null" ]; then
    echo "错误: 无法获取 session_id"
    exit 1
fi
echo "Session ID: $SESSION_ID"

echo -e "\n=== 5. 更新用餐会话 (meal/update) ==="
curl -s -X POST "$BASE_URL/api/v1/meal/update?session_id=$SESSION_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "foods": [
      {"name": "rice", "weight_g": 100},
      {"name": "pork", "weight_g": 80}
    ],
    "nutrition": {
      "calories": 350,
      "protein": 16,
      "carbs": 40,
      "fat": 14
    }
  }' | $JQ

echo -e "\n=== 6. 获取会话详情 (meal/session) ==="
curl -s "$BASE_URL/api/v1/meal/session/$SESSION_ID" | $JQ

echo -e "\n=== 7. 结束用餐会话 (meal/end) ==="
curl -s -X POST "$BASE_URL/api/v1/meal/end?session_id=$SESSION_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "foods": [
      {"name": "rice", "weight_g": 20},
      {"name": "pork", "weight_g": 10}
    ],
    "nutrition": {
      "calories": 70,
      "protein": 3,
      "carbs": 8,
      "fat": 3
    }
  }' | $JQ

echo -e "\n=== 8. 获取会话列表 (meal/sessions) ==="
curl -s "$BASE_URL/api/v1/meal/sessions?user_id=test_user" | $JQ

echo -e "\n=== 9. 今日统计 (stats/daily) ==="
TODAY=$(date +%Y-%m-%d)
curl -s "$BASE_URL/api/v1/stats/daily?user_id=test_user&date=$TODAY" | $JQ

echo -e "\n=== 10. AI 营养对话 (chat/nutrition) ==="
curl -s -X POST "$BASE_URL/api/v1/chat/nutrition" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "今天午餐吃了红烧肉和米饭，健康吗？",
    "user_profile": {
      "age": 45,
      "bmi": 26.5,
      "health_conditions": ["轻度脂肪肝"],
      "dietary_preferences": ["低油"]
    },
    "meal_context": {
      "foods": ["红烧肉", "米饭"],
      "total_calories": 780,
      "total_fat": 35
    },
    "message_type": "meal_end"
  }' | $JQ

echo -e "\n====================================="
echo "  所有测试完成!"
echo "====================================="
