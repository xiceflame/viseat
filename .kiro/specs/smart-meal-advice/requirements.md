# Requirements Document

## Introduction

本功能旨在完善各个接口的建议生成能力，确保每个接口都能基于完整的上下文信息生成个性化建议。前端负责收集和传递必要的数据，后端各接口根据各自的场景生成针对性的建议。

## Glossary

- **MealSession**: 用餐会话，从开始用餐到结束用餐的完整过程
- **MealContext**: 用餐上下文，包含用餐时长、识别次数、累计热量等会话级数据
- **DailyContext**: 今日上下文，包含今日总摄入、用餐次数、上次用餐时间等
- **UserProfile**: 用户档案，包含年龄、性别、BMI、健康目标等个人信息
- **GlassesSummary**: 眼镜端总结，用餐结束时在眼镜上显示的简洁营养总结

## Requirements

### Requirement 1: 单次识别建议 (`/api/v1/vision/analyze`)

**User Story:** As a user, I want to receive immediate nutritional advice when I take a photo of my food, so that I can make informed eating decisions.

#### 前端需要提供的数据

| 字段 | 类型 | 说明 |
|------|------|------|
| image_url | string | 图片 URL |
| user_profile | object | 用户档案（可选） |
| daily_context | object | 今日上下文（可选） |
| is_meal_active | boolean | 是否在用餐会话中 |

#### 后端需要返回的建议

| 字段 | 类型 | 说明 |
|------|------|------|
| suggestion | string | 针对本次识别的即时建议（显示在眼镜上） |
| health_tips | list | 健康提示列表（显示在手机上） |

#### Acceptance Criteria

1. WHEN analyzing a food image THEN the system SHALL return a `suggestion` field with personalized advice
2. WHEN user_profile is provided THEN the system SHALL consider health goals in the suggestion
3. WHEN daily_context shows high calorie intake THEN the system SHALL warn about calorie excess
4. WHEN the food is high in sodium/sugar THEN the system SHALL include a health warning
5. WHEN the suggestion is generated THEN it SHALL be under 30 Chinese characters for glasses display

### Requirement 2: 用餐更新建议 (`/api/v1/vision/analyze_meal_update`)

**User Story:** As a user during a meal, I want to receive real-time feedback on my eating progress, so that I can adjust my eating pace.

#### 前端需要提供的数据

| 字段 | 类型 | 说明 |
|------|------|------|
| image_url | string | 图片 URL |
| baseline_foods | list | 基线食物列表 |
| meal_context | object | 用餐上下文 |

**meal_context 结构:**
```json
{
  "session_id": "string",
  "start_time": 1700000000000,
  "duration_minutes": 15,
  "recognition_count": 3,
  "total_consumed_so_far": 450.0
}
```

#### 后端需要返回的建议

| 字段 | 类型 | 说明 |
|------|------|------|
| message | string | 状态消息 |
| eating_pace_advice | string | 进食速度建议 |
| progress_summary | string | 进度总结 |

#### Acceptance Criteria

1. WHEN meal duration exceeds 30 minutes THEN the system SHALL suggest wrapping up
2. WHEN eating speed is too fast (>50 kcal/min) THEN the system SHALL suggest slowing down
3. WHEN new food is detected (adjust status) THEN the system SHALL acknowledge the addition
4. WHEN consumption ratio exceeds 80% THEN the system SHALL suggest stopping soon
5. WHEN the update is skipped THEN the system SHALL return a brief status message

### Requirement 3: 用餐结束建议 (`/api/v1/meal/end`)

**User Story:** As a user finishing a meal, I want to see a comprehensive summary and personalized advice, so that I can plan my next meal wisely.

#### 前端需要提供的数据

| 字段 | 类型 | 说明 |
|------|------|------|
| session_id | string | 会话 ID |
| meal_context | object | 用餐上下文 |
| daily_context | object | 今日上下文 |
| user_profile | object | 用户档案 |

**meal_context 结构:**
```json
{
  "duration_minutes": 25,
  "recognition_count": 5,
  "eating_speed": "normal"
}
```

**daily_context 结构:**
```json
{
  "total_calories_today": 1200,
  "total_protein_today": 45,
  "total_carbs_today": 150,
  "total_fat_today": 40,
  "meal_count_today": 2,
  "last_meal_hours_ago": 4
}
```

**user_profile 结构:**
```json
{
  "age": 28,
  "gender": "male",
  "bmi": 22.5,
  "activity_level": "moderate",
  "health_goal": "maintain",
  "target_daily_calories": 2000,
  "health_conditions": [],
  "dietary_preferences": []
}
```

#### 后端需要返回的建议

| 字段 | 类型 | 说明 |
|------|------|------|
| final_stats | object | 最终统计数据 |
| meal_summary | object | 本餐总结（用于眼镜显示） |
| advice | object | 详细建议（用于手机显示） |
| next_meal_suggestion | object | 下一餐建议 |

**meal_summary 结构（眼镜显示）:**
```json
{
  "total_calories": 650,
  "total_protein": 35,
  "total_carbs": 80,
  "total_fat": 20,
  "duration_minutes": 25,
  "rating": "good",
  "short_advice": "营养均衡，继续保持！"
}
```

**advice 结构（手机显示）:**
```json
{
  "summary": "本餐营养均衡，蛋白质摄入充足",
  "suggestions": [
    "用餐时长适中，有助于消化",
    "今日蛋白质摄入已达标",
    "建议下一餐增加蔬菜摄入"
  ],
  "highlights": ["蛋白质充足", "用餐节奏健康"],
  "warnings": []
}
```

**next_meal_suggestion 结构:**
```json
{
  "recommended_time": "4小时后",
  "meal_type": "晚餐",
  "calorie_budget": 600,
  "focus_nutrients": ["蔬菜", "膳食纤维"],
  "avoid": ["高糖食物"]
}
```

#### Acceptance Criteria

1. WHEN a meal session ends THEN the system SHALL return a `meal_summary` for glasses display
2. WHEN a meal session ends THEN the system SHALL return detailed `advice` for phone display
3. WHEN user_profile is provided THEN the system SHALL personalize advice based on health goals
4. WHEN daily_context shows calorie excess THEN the system SHALL suggest lighter next meal
5. WHEN meal duration is too short (<10 min) THEN the system SHALL warn about fast eating
6. WHEN meal duration is healthy (15-30 min) THEN the system SHALL praise the eating pace
7. WHEN generating advice THEN the system SHALL use Qwen LLM for natural language generation
8. IF LLM call fails THEN the system SHALL fall back to rule-based advice

### Requirement 4: 眼镜端用餐总结显示

**User Story:** As a user wearing glasses, I want to see a brief meal summary when I finish eating, so that I don't need to check my phone.

#### 前端需要实现的功能

1. 用餐结束时调用 `sendMealSummary()` 方法
2. 在眼镜上显示营养数据和简短建议
3. 显示时长 8-10 秒后自动消失

#### Acceptance Criteria

1. WHEN a meal session ends THEN the phone app SHALL send meal summary to glasses via Bluetooth
2. WHEN sending meal summary THEN the system SHALL include calories, protein, carbs, fat, and duration
3. WHEN sending meal summary THEN the system SHALL include 1 short advice (under 20 characters)
4. WHEN the glasses receive summary THEN they SHALL display it for 8-10 seconds
5. IF glasses are disconnected THEN the system SHALL skip glasses summary gracefully

### Requirement 5: 用餐时长追踪

**User Story:** As a user, I want the system to accurately track my meal duration, so that I can understand my eating habits.

#### 前端需要实现的功能

1. 用餐开始时记录 `sessionStartTime`
2. 用餐结束时计算 `durationMinutes`
3. 将时长数据包含在结束请求中

#### Acceptance Criteria

1. WHEN a meal session starts THEN the system SHALL record the start timestamp in milliseconds
2. WHEN a meal session ends THEN the system SHALL calculate duration as `(endTime - startTime) / 60000`
3. WHEN sending meal end request THEN the system SHALL include `duration_minutes` in meal_context
4. WHEN duration is less than 10 minutes THEN the system SHALL classify as "fast"
5. WHEN duration is 15-30 minutes THEN the system SHALL classify as "normal"
6. WHEN duration exceeds 45 minutes THEN the system SHALL classify as "slow"

### Requirement 6: 今日摄入统计

**User Story:** As a user, I want the system to track my daily nutrition intake, so that advice can be based on my overall daily consumption.

#### 前端需要实现的功能

1. 本地存储今日各餐的营养数据
2. 计算今日累计摄入
3. 在结束用餐时将今日数据发送给后端

#### Acceptance Criteria

1. WHEN a meal ends THEN the system SHALL update local daily nutrition totals
2. WHEN calculating daily totals THEN the system SHALL sum calories, protein, carbs, and fat from all meals today
3. WHEN sending meal end request THEN the system SHALL include daily_context with today's totals
4. WHEN a new day starts THEN the system SHALL reset daily totals to zero
5. WHEN daily calories exceed target THEN the system SHALL flag for advice generation

