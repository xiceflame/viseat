# 智能健康提示系统设计文档

## Overview

智能健康提示系统是一个基于后端 AI 分析的健康建议模块。后端 AI 根据用户的真实行为数据（进食记录、体重变化、营养摄入、进食时间等）生成个性化、有针对性的健康提示。客户端负责收集数据上下文、调用 API 并展示自动轮播的提示。

### 现有实现分析

**已有的部分：**
- `ApiService.kt` 中已定义 API 接口：`getPersonalizedTips`、`refreshPersonalizedTips`
- `ApiResponses.kt` 中已定义数据模型：`PersonalizedTip`、`PersonalizedTipsResponse`
- `AIInsightsCard.kt` UI 组件已存在（但需要改造为自动轮播）

**缺失的部分：**
- `NetworkManager.kt` 中没有调用个性化建议 API 的方法
- `PersonalizedTipsRepository` 未实现
- 后端 API 可能需要增强，支持接收更丰富的用户数据上下文
- UI 需要改造为自动轮播，移除反馈按钮

### 核心设计理念

1. **后端 AI 驱动**：所有提示由后端 AI 分析用户数据后生成，客户端不做规则判断
2. **数据上下文传递**：客户端收集完整的用户数据上下文，传递给后端
3. **简洁交互**：自动轮播展示，移除不必要的交互元素

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Android Client                          │
├─────────────────────────────────────────────────────────────┤
│  UI Layer                                                    │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  SmartHealthTipsCard (自动轮播展示)                  │    │
│  └─────────────────────────────────────────────────────┘    │
│                              │                               │
│  ViewModel Layer                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  SmartHealthTipsViewModel                            │    │
│  │  - collectUserDataContext()  收集用户数据            │    │
│  │  - fetchTipsFromBackend()    调用后端 API            │    │
│  └─────────────────────────────────────────────────────┘    │
│                              │                               │
│  Network Layer                                               │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  POST /api/v1/users/{user_id}/smart-tips            │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      Backend Server                          │
├─────────────────────────────────────────────────────────────┤
│  Smart Tips AI Service (需要后端开发)                        │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  1. 接收用户数据上下文                               │    │
│  │  2. AI 分析用户饮食行为模式                          │    │
│  │  3. 生成个性化健康提示                               │    │
│  │  4. 返回提示列表（带优先级和样式）                    │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

---

## 后端 API 需求（需要后端 Agent 开发）

### API 端点

```
POST /api/v1/users/{user_id}/smart-tips
```

### 请求体 (SmartTipsRequest)

客户端会收集以下用户数据上下文，发送给后端 AI 分析：

```json
{
  "user_id": "string",
  "request_time": "2024-01-15T12:30:00Z",
  
  "meal_data": {
    "total_meal_count": 45,
    "today_meal_count": 2,
    "last_meal_time": "2024-01-15T08:30:00Z",
    "last_meal_duration_minutes": 8,
    "recent_meals": [
      {
        "date": "2024-01-15",
        "meal_count": 2,
        "total_calories": 1200,
        "meals": [
          {
            "time": "08:30",
            "calories": 450,
            "duration_minutes": 8,
            "foods": ["煎蛋", "面包", "牛奶"]
          }
        ]
      }
    ]
  },
  
  "today_nutrition": {
    "calories": 1200,
    "protein": 45.5,
    "carbs": 150.0,
    "fat": 40.0
  },
  
  "user_goals": {
    "target_calories": 2000,
    "target_protein": 80,
    "target_carbs": 250,
    "target_fat": 65,
    "health_goal": "lose_weight"
  },
  
  "weight_data": {
    "current_weight": 72.5,
    "target_weight": 68.0,
    "start_weight": 75.0,
    "weekly_change": -0.5,
    "recent_records": [
      {"date": "2024-01-15", "weight": 72.5},
      {"date": "2024-01-14", "weight": 72.8}
    ]
  },
  
  "user_profile": {
    "age": 28,
    "gender": "male",
    "height": 175,
    "activity_level": "moderate",
    "diet_type": "balanced",
    "allergens": ["nuts"],
    "health_conditions": []
  }
}
```

### 响应体 (SmartTipsResponse)

后端 AI 分析后返回的提示列表：

```json
{
  "tips": [
    {
      "id": "tip_001",
      "type": "eating_speed",
      "content": "根据最近的进食时间观察，细嚼慢咽有助于消化哦～",
      "priority": 1,
      "icon": "schedule",
      "color_theme": "blue",
      "data_reference": {
        "last_meal_duration": 8,
        "recommended_duration": 20
      }
    },
    {
      "id": "tip_002",
      "type": "high_calorie_warning",
      "content": "最近高热量食物吃的有点多，注意控制哦！",
      "priority": 2,
      "icon": "warning",
      "color_theme": "orange"
    },
    {
      "id": "tip_003",
      "type": "weight_progress",
      "content": "本周减重进展不错，继续保持！💪",
      "priority": 5,
      "icon": "trending_down",
      "color_theme": "green"
    }
  ],
  "generated_at": "2024-01-15T12:30:05Z",
  "next_refresh_hint": "meal_ended"
}
```

### 后端 AI 需要分析的场景

| 场景 | 触发条件 | 示例提示内容 |
|------|---------|-------------|
| 无数据引导 | total_meal_count = 0 | "开始记录第一顿美食吧！戴上眼镜，开启健康之旅 🍽️" |
| 进食速度过快 | last_meal_duration < 10分钟 | "根据最近的进食时间观察，细嚼慢咽有助于消化哦～" |
| 高热量警告 | 最近7天高热量餐占比 > 50% | "最近高热量食物吃的有点多，注意控制哦！" |
| 热量进度 | 今日热量 > 目标的80% | "今日热量即将达标，晚餐建议选择清淡食物" |
| 蛋白质不足 | 连续3天蛋白质 < 目标的60% | "蛋白质摄入偏低，建议多吃鸡蛋、鱼肉、豆制品" |
| 体重进展 | 减重目标且本周有下降 | "本周减重进展不错，继续保持！💪" |
| 进食间隔 | 超过6小时未进食 | "距离上次进食已超过6小时，记得按时吃饭哦" |
| 营养均衡 | 某营养素长期偏高/偏低 | AI 根据数据生成具体建议 |
| 鼓励 | 达成某个目标 | "今日营养目标已达成，太棒了！🎉" |

### 提示类型定义

| type | 说明 | icon | color_theme |
|------|------|------|-------------|
| `onboarding` | 引导提示 | restaurant | teal |
| `eating_speed` | 进食速度提醒 | schedule | blue |
| `high_calorie_warning` | 高热量警告 | warning | orange |
| `calorie_progress` | 热量进度提醒 | trending_up | purple |
| `protein_deficit` | 蛋白质不足 | fitness_center | red |
| `weight_progress` | 体重进展 | monitor_weight | green |
| `meal_interval` | 进食间隔提醒 | access_time | mint |
| `nutrition_balance` | 营养均衡建议 | balance | blue |
| `encouragement` | 鼓励 | emoji_events | green |
| `custom` | 自定义 | lightbulb | purple |

### 颜色主题定义

| color_theme | 用途 | Android 颜色 |
|-------------|------|-------------|
| `teal` | 引导、正面 | AppleTeal (#30D5C8) |
| `blue` | 信息、时间 | AppleBlue (#007AFF) |
| `orange` | 警告 | AppleOrange (#FF9500) |
| `red` | 严重警告 | AppleRed (#FF3B30) |
| `green` | 成功、鼓励 | AppleGreen (#34C759) |
| `purple` | 进度 | ApplePurple (#AF52DE) |
| `mint` | 提醒 | AppleMint (#00C7BE) |

### 刷新时机

客户端应在以下时机调用此 API：
1. 首页加载时
2. 用餐结束后（`next_refresh_hint: "meal_ended"`）
3. 记录体重后
4. 日期变更时
5. 用户手动下拉刷新时

---

## Android 客户端实现

### 1. SmartTip 数据模型

```kotlin
/**
 * 智能健康提示（来自后端）
 */
data class SmartTip(
    val id: String,
    val type: String,
    val content: String,
    val priority: Int,
    val icon: String,
    val colorTheme: String,
    val dataReference: Map<String, Any>? = null
)
```

### 2. SmartTipsRequest 请求模型

```kotlin
/**
 * 智能提示请求
 */
data class SmartTipsRequest(
    @SerializedName("user_id") val userId: String,
    @SerializedName("request_time") val requestTime: String,
    @SerializedName("meal_data") val mealData: MealDataContext,
    @SerializedName("today_nutrition") val todayNutrition: NutritionContext,
    @SerializedName("user_goals") val userGoals: GoalsContext,
    @SerializedName("weight_data") val weightData: WeightDataContext?,
    @SerializedName("user_profile") val userProfile: ProfileContext?
)
```

### 3. SmartHealthTipsCard UI 组件

```kotlin
/**
 * 智能健康提示卡片（自动轮播）
 */
@Composable
fun SmartHealthTipsCard(
    tips: List<SmartTip>,
    isLoading: Boolean = false,
    autoScrollInterval: Long = 5000L,
    modifier: Modifier = Modifier
)
```

### 4. 图标和颜色映射

```kotlin
fun getIconForType(icon: String): ImageVector = when (icon) {
    "restaurant" -> Icons.Rounded.Restaurant
    "schedule" -> Icons.Rounded.Schedule
    "warning" -> Icons.Rounded.Warning
    "trending_up" -> Icons.Rounded.TrendingUp
    "fitness_center" -> Icons.Rounded.FitnessCenter
    "monitor_weight" -> Icons.Rounded.MonitorWeight
    "access_time" -> Icons.Rounded.AccessTime
    "emoji_events" -> Icons.Rounded.EmojiEvents
    else -> Icons.Rounded.Lightbulb
}

fun getColorForTheme(theme: String): Color = when (theme) {
    "teal" -> AppleTeal
    "blue" -> AppleBlue
    "orange" -> AppleOrange
    "red" -> AppleRed
    "green" -> AppleGreen
    "purple" -> ApplePurple
    "mint" -> AppleMint
    else -> AppleGray1
}
```

---

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: 轮播循环
*For any* list of tips with size N > 1, after N carousel transitions, the current index SHALL return to 0.
**Validates: Requirements 2.4**

### Property 2: 图标映射一致性
*For any* icon string from the backend response, the getIconForType function SHALL return a valid ImageVector.
**Validates: Requirements 3.4**

### Property 3: 颜色映射一致性
*For any* color_theme string from the backend response, the getColorForTheme function SHALL return a valid Color.
**Validates: Requirements 3.4**

### Property 4: 提示优先级排序
*For any* list of tips from the backend, the UI SHALL display them in ascending order by priority (lower priority number first).
**Validates: Requirements 4.4**

---

## Error Handling

### 网络错误处理

| 场景 | 处理方式 |
|------|---------|
| 网络超时 | 显示缓存的提示，或默认提示 |
| API 错误 | 显示默认鼓励提示 |
| 空响应 | 显示"保持健康饮食习惯"默认提示 |

### 边界条件

- 提示列表为空时：显示默认的"保持健康饮食习惯"提示
- 只有一条提示时：不显示轮播指示器，不进行自动轮播
- 提示内容过长时：最多显示2行，使用省略号截断

---

## Testing Strategy

### 单元测试

1. **数据上下文收集测试**
   - 测试 MealDataContext 构建
   - 测试 NutritionContext 构建
   - 测试 WeightDataContext 构建

2. **图标/颜色映射测试**
   - 测试所有已知类型的映射
   - 测试未知类型的默认值

### 属性测试

使用 Kotest 的 Property-Based Testing 框架：

```kotlin
class SmartHealthTipsPropertyTest : FunSpec({
    test("Property 1: 轮播循环") {
        // **Feature: smart-health-tips, Property 1: 轮播循环**
        // **Validates: Requirements 2.4**
        checkAll(Arb.list(Arb.smartTip(), 2..10)) { tips ->
            var index = 0
            repeat(tips.size) {
                index = (index + 1) % tips.size
            }
            index shouldBe 0
        }
    }
})
```

### UI 测试

1. **轮播功能测试**
   - 验证自动滚动间隔
   - 验证触摸暂停/恢复
   - 验证循环轮播

2. **样式测试**
   - 验证不同类型提示的图标和颜色
   - 验证文字截断

### 测试框架

- 单元测试：JUnit 5
- 属性测试：Kotest Property Testing
- UI 测试：Compose UI Testing
