# 演示模式资源文件

## 目录结构

```
demo/
├── README.md                    # 本文件
├── coke.jpg                     # 可乐图片
├── coke_response.json           # 可乐分析结果（真实API数据）
├── chips.jpg                    # 薯片图片
├── chips_response.json          # 薯片分析结果（真实API数据）
├── meal_start.jpg               # 用餐开始图片（待添加）
├── meal_start_response.json     # 用餐开始分析结果（待添加）
├── meal_progress.jpg            # 用餐中图片（待添加）
├── meal_progress_response.json  # 用餐中分析结果（待添加）
├── meal_end.jpg                 # 用餐结束图片（待添加）
└── meal_end_response.json       # 用餐结束分析结果（待添加）
```

## 演示模式

### 单图识别模式（使用本地数据）

**不需要网络**，直接使用预设的 JSON 响应数据：
- `coke.jpg` + `coke_response.json` - 可乐（111kcal）
- `chips.jpg` + `chips_response.json` - 薯片（536kcal）

数据来源：2025-12-01 18:40 真实 API 分析结果

### 用餐监测模式（使用真实API）

**需要网络**，图片会压缩到 160KB 后上传分析：
- `meal_start.jpg` - 用餐开始（完整餐食）
- `meal_progress.jpg` - 用餐中（部分食用）
- `meal_end.jpg` - 用餐结束（剩余食物）

## JSON 响应格式

```json
{
  "raw_llm": {
    "is_food": true,
    "suggestion": "饮食建议...",
    "foods": [
      {
        "dish_name": "可口可乐",
        "dish_name_cn": "可口可乐",
        "category": "beverage",
        "cooking_method": "raw",
        "ingredients": [...],
        "total_weight_g": 300,
        "confidence": 0.95
      }
    ]
  },
  "mode": "start",
  "suggestion": "饮食建议...",
  "snapshot": {
    "foods": [...],
    "nutrition": {
      "calories": 111,
      "protein": 0.2,
      "carbs": 28.8,
      "fat": 0.1
    }
  }
}
```

## 图片要求

- 格式：JPEG
- 建议分辨率：1280x960 或更高
- 用餐监测模式会自动压缩到 160KB
