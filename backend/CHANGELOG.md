# 后端 API 变更日志

## 2025-11-26 (v1.2.0) - 多数据源营养数据库

### 🆕 新增功能

#### 1. 多数据源营养数据库
整合了三大权威营养数据源，提供更准确、更全面的食物营养信息：

| 数据源 | 食物数量 | 特点 |
|--------|----------|------|
| 中国食物成分表（第6版） | 1838种 | 中国食材，32个营养字段 |
| USDA Foundation Foods | 500+ | 实验室精确数据 |
| FoodStruct | 3000+ | 国际通用食物 |

**融合策略**：
- 中文食物名 → 优先匹配中国食物成分表
- 英文食物名 → 优先匹配 USDA/FoodStruct
- 智能别名映射（rice → 籼米, pork → 猪肉）

#### 2. 营养数据管理模块
**新增文件**: `app/nutrition_db.py`

```python
from app.nutrition_db import get_nutrition_db, lookup_nutrition

# 获取数据库实例
db = get_nutrition_db()

# 查询食物营养
nutrition = lookup_nutrition("猪肉")
# {"calories": 331, "protein": 15.1, "carbs": 0.0, "fat": 30.1}

# 获取完整营养数据（32个字段）
full_data = db.lookup("猪肉")[0].to_full_dict()
```

#### 3. VLM 食材识别优化
优化 Qwen-VL prompt，要求返回标准化的中英文食材名称：
- 新增 `name_cn` 字段（中文食材名）
- 新增 `dish_name_cn` 字段（中文菜名）
- 避免返回笼统名称如 "meat"、"vegetable"
- 使用标准烹饪方式名称

### 📁 新增文件
- `app/nutrition_db.py` - 多数据源营养数据库管理模块
- `data/NUTRITION_DATA_SOURCES.md` - 数据来源说明文档
- `data/china-food-composition-data/` - 中国食物成分表数据
- `data/FoodData_Central_foundation.../` - USDA Foundation Foods

### ⚙️ 技术细节
- 数据模型: `NutritionData` dataclass，包含32个营养字段
- 查询策略: 优先级链式查询 + 别名映射 + 模糊匹配
- 向后兼容: 保留旧的 `NUTRITION_DB`、`NUTRITION_DB_EXT` 查询逻辑

---

## 2025-11-25 (v1.1.0) - 营养成分与个性化建议

### 🆕 新增功能

#### 1. 营养成分总量返回
**影响 API**: `POST /api/v1/meal/end`, `POST /api/v1/meal/update`

`end_meal_session` 现在返回营养成分总量：
```json
{
    "session_id": "xxx",
    "status": "ended",
    "total_consumed_kcal": 620,
    "consumption_ratio": 0.795,
    "duration_minutes": 25,
    "report": "✅ 用餐适量...",
    "total_protein": 22.0,
    "total_carbs": 68.0,
    "total_fat": 28.0,
    "nutrition_breakdown": {
        "protein_g": 22.0,
        "carbs_g": 68.0,
        "fat_g": 28.0,
        "protein_percent": 18.6,
        "carbs_percent": 57.6,
        "fat_percent": 23.7
    }
}
```

#### 2. 今日统计 API
**新增 API**: `GET /api/v1/stats/daily`

参数:
- `user_id`: 用户 ID (默认: "default_user")
- `date`: 日期 YYYY-MM-DD (默认: 今天)

返回:
```json
{
    "date": "2025-11-25",
    "total_calories": 1850,
    "total_protein": 75.5,
    "total_carbs": 220.3,
    "total_fat": 65.2,
    "target_calories": 2000,
    "calories_ratio": 92.5,
    "meals": [...],
    "nutrition_breakdown": {
        "protein_percent": 20.9,
        "carbs_percent": 61.0,
        "fat_percent": 18.1
    }
}
```

#### 3. 个性化建议 API
**增强 API**: `POST /api/v1/chat/nutrition`

新增参数:
- `user_profile`: 用户健康档案
- `meal_context`: 当前用餐上下文
- `message_type`: 消息类型 (chat/meal_start/meal_end)

请求示例:
```json
{
    "query": "分析本餐",
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
}
```

新增返回字段:
```json
{
    "answer": "您有轻度脂肪肝...",
    "suggested_actions": ["添加蔬菜", "选择低脂食物"],
    "meal_advice": "建议减少油脂摄入"
}
```

### 🔄 API 响应格式调整

#### meal/start 响应
```json
{
    "session_id": "xxx",
    "status": "active",
    "initial_kcal": 780,
    "initial_nutrition": {...},
    "auto_capture_interval": 300
}
```

#### meal/update 响应
新增扁平字段:
```json
{
    "session_id": "xxx",
    "current_remaining_kcal": 400,
    "total_served_kcal": 780,
    "consumed_kcal": 380,
    "consumption_ratio": 0.487,
    "duration_minutes": 10.5,
    "suggestion": "进食节奏良好",
    "consumed_protein": 14,
    "consumed_carbs": 42,
    "consumed_fat": 17
}
```

### 📁 新增文件

- `backend/scripts/test_api.sh` - Shell 测试脚本
- `backend/scripts/test_full_flow.py` - Python 完整流程测试
- `backend/test_data/mock_responses.json` - Mock 响应数据

### 📋 手机端需同步更新

1. **MealEndResponse 数据类**
```kotlin
data class MealEndResponse(
    val session_id: String,
    val status: String,
    val total_consumed_kcal: Double,
    val consumption_ratio: Double,
    val duration_minutes: Int,
    val report: String?,
    // 新增
    val total_protein: Double,
    val total_carbs: Double,
    val total_fat: Double,
    val nutrition_breakdown: NutritionBreakdown?
)

data class NutritionBreakdown(
    val protein_g: Double,
    val carbs_g: Double,
    val fat_g: Double,
    val protein_percent: Double,
    val carbs_percent: Double,
    val fat_percent: Double
)
```

2. **SessionStatus 蓝牙消息格式**
```
[0] String: 会话ID
[1] String: 状态 ("active" | "ended")
[2] Float:  总摄入热量 (kcal)
[3] String: 消息文本
[4] Float:  总蛋白质 (g)     // 新增
[5] Float:  总碳水化合物 (g)  // 新增
[6] Float:  总脂肪 (g)       // 新增
[7] Int:    用餐时长 (分钟)   // 新增
```

---

**后端 Agent 完成时间**: 2025-11-25 18:30
