# Design Document: Profile Page Redesign

## Overview

重新设计 VISEAT 食智 App 的"我的页面"，打造一个简洁、美观、信息层级清晰的个人中心。页面采用卡片式布局，从上到下依次展示：用户信息头部、今日营养进度、健康档案、设置列表。

### 设计目标

1. **视觉吸引** - 使用渐变色和圆形进度条增强视觉效果
2. **信息聚焦** - 突出显示最重要的信息（BMI、今日热量）
3. **操作简化** - 减少层级，常用功能一键可达
4. **一致性** - 与 App 整体设计语言保持一致

## Architecture

```
ProfileScreen (Composable)
├── UserHeaderCard          // 用户信息头部卡片
│   ├── Avatar              // 性别头像
│   ├── BasicInfo           // 基本信息（性别·年龄·身高·体重）
│   ├── BMIIndicator        // BMI 指示器（带颜色）
│   └── HealthGoalBadge     // 健康目标徽章
├── NutritionProgressCard   // 今日营养进度卡片
│   ├── CircularProgress    // 圆形进度条
│   ├── CalorieText         // 热量文本
│   └── MealCountBadge      // 用餐次数徽章
├── HealthProfileCard       // 健康档案卡片
│   ├── HealthConditions    // 健康状况 Chips
│   └── DietaryPreferences  // 饮食偏好 Chips
├── SettingsSection         // 设置区域
│   ├── DebugLogItem        // 调试日志
│   ├── AboutItem           // 关于应用（可展开）
│   └── ClearDataItem       // 清除数据
└── EditProfileDialog       // 编辑档案弹窗（复用现有）
```

## Components and Interfaces

### 1. ProfileScreen

主页面组件，负责整体布局和状态管理。

```kotlin
@Composable
fun ProfileScreen(
    profile: UserProfile?,
    dailyNutrition: DailyNutritionState,
    onSaveProfile: (UserProfile) -> Unit,
    onNavigateToStats: () -> Unit,
    onOpenDebugLog: () -> Unit,
    onClearData: () -> Unit,
    modifier: Modifier = Modifier
)
```

### 2. UserHeaderCard

用户信息头部卡片，使用渐变背景。

```kotlin
@Composable
fun UserHeaderCard(
    profile: UserProfile?,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

**视觉设计：**
- 背景：主题色渐变（primaryContainer → primary）
- 头像：72dp 圆形，根据性别显示不同图标
- 昵称：显示用户设置的昵称，未设置时显示"用户"
- BMI：大字号显示数值，下方显示状态文字和颜色条
- 健康目标：右上角徽章显示

### 3. NutritionProgressCard

今日营养进度卡片，核心视觉元素。

```kotlin
@Composable
fun NutritionProgressCard(
    currentCalories: Double,
    targetCalories: Int,
    mealCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

**视觉设计：**
- 圆形进度条：120dp 直径，12dp 线宽
- 进度颜色：
  - 0-80%: 绿色 (#4CAF50)
  - 80-100%: 橙色 (#FF9800)
  - >100%: 红色 (#F44336)
- 中心显示：当前热量 / 目标热量
- 右侧显示：今日用餐 X 次

### 4. HealthProfileCard

健康档案卡片，展示健康状况和饮食偏好。

```kotlin
@Composable
fun HealthProfileCard(
    healthConditions: List<String>,
    dietaryPreferences: List<String>,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

**视觉设计：**
- 两个分区：健康状况（粉色系）、饮食偏好（绿色系）
- 使用 FlowRow 展示 Chips
- 空状态显示"暂无"灰色文字

### 5. SettingsSection

设置列表区域。

```kotlin
@Composable
fun SettingsSection(
    onDebugLogClick: () -> Unit,
    onClearDataClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

**设置项：**
| 图标 | 标题 | 操作 |
|------|------|------|
| BugReport | 调试日志 | 导航到日志页 |
| Info | 关于应用 | 展开显示版本信息 |
| Delete | 清除本地数据 | 显示确认弹窗 |

## Data Models

### 复用现有数据模型

本设计复用以下现有数据模型和接口：

1. **UserProfile** (UserProfileRepository.kt) - 用户档案，需扩展添加 nickname 字段
2. **DailyNutritionTracker** - 今日营养追踪器，提供 getDailyContext()、getTodayCalories()、getTodayMealCount()
3. **DailyContext** (ContextModels.kt) - 今日上下文数据

### UserProfile 扩展

在现有 UserProfile 数据类中添加 nickname 字段：

```kotlin
data class UserProfile(
    val nickname: String? = null,  // 新增：用户昵称
    val age: Int,
    val gender: String = "male",
    val height: Float,
    val weight: Float,
    val bmi: Float,
    val activityLevel: String = "moderate",
    val healthGoal: String = "maintain",
    val targetWeight: Float? = null,
    val healthConditions: List<String>,
    val dietaryPreferences: List<String>
) {
    // 复用现有的 calculateDailyCalories() 方法
}
```

### BMI 状态映射

```kotlin
enum class BMIStatus(
    val label: String,
    val color: Color,
    val suggestion: String
) {
    UNDERWEIGHT("偏瘦", Color(0xFF2196F3), "建议适当增加营养摄入"),
    NORMAL("正常", Color(0xFF4CAF50), "继续保持健康饮食"),
    OVERWEIGHT("偏胖", Color(0xFFFFC107), "建议控制热量摄入"),
    OBESE("肥胖", Color(0xFFFF5722), "建议咨询营养师")
}

fun getBMIStatus(bmi: Float): BMIStatus = when {
    bmi < 18.5 -> BMIStatus.UNDERWEIGHT
    bmi < 24.0 -> BMIStatus.NORMAL
    bmi < 28.0 -> BMIStatus.OVERWEIGHT
    else -> BMIStatus.OBESE
}
```

### 进度颜色逻辑

```kotlin
fun getProgressColor(current: Double, target: Int): Color {
    val percentage = current / target
    return when {
        percentage >= 1.0 -> Color(0xFFF44336)  // 红色：超标
        percentage >= 0.8 -> Color(0xFFFF9800)  // 橙色：接近目标
        else -> Color(0xFF4CAF50)               // 绿色：正常
    }
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: BMI Status Color Mapping

*For any* BMI value, the status color returned by `getBMIStatus()` SHALL match the defined thresholds: blue for BMI < 18.5, green for 18.5 ≤ BMI < 24, yellow for 24 ≤ BMI < 28, red for BMI ≥ 28.

**Validates: Requirements 1.3**

### Property 2: Progress Color Threshold

*For any* calorie values (current, target) where target > 0, the progress color SHALL be green when current/target < 0.8, orange when 0.8 ≤ current/target < 1.0, and red when current/target ≥ 1.0.

**Validates: Requirements 2.3, 2.4**

### Property 3: Profile Data Display Completeness

*For any* non-null UserProfile, the header card display string SHALL contain the gender text, age, height, and weight values from the profile.

**Validates: Requirements 1.2**

### Property 4: Nutrition Progress Calculation

*For any* calorie values (current, target) where target > 0, the progress percentage SHALL equal current/target clamped to [0, 1] for display purposes.

**Validates: Requirements 2.1, 2.2**

### Property 5: Health Chips Rendering

*For any* list of health conditions or dietary preferences, the number of rendered chips SHALL equal the length of the input list, and each chip label SHALL match the corresponding list item.

**Validates: Requirements 3.1, 3.2**

## Error Handling

### 空状态处理

| 场景 | 处理方式 |
|------|----------|
| profile == null | 显示"点击设置个人信息"提示 |
| healthConditions.isEmpty() | 显示"暂无"灰色文字 |
| dietaryPreferences.isEmpty() | 显示"暂无"灰色文字 |
| targetCalories == 0 | 使用默认值 2000 |

### 数据清除确认

清除本地数据前必须显示确认弹窗：
- 标题："确认清除数据？"
- 内容："此操作将清除所有本地数据，包括用户档案和用餐记录。此操作不可撤销。"
- 按钮："取消" / "确认清除"

## Testing Strategy

### 单元测试

1. **BMI 状态计算测试**
   - 测试边界值：18.4, 18.5, 23.9, 24.0, 27.9, 28.0
   - 验证返回正确的状态枚举

2. **进度颜色计算测试**
   - 测试边界值：79%, 80%, 99%, 100%, 101%
   - 验证返回正确的颜色

3. **每日热量目标计算测试**
   - 测试不同性别、年龄、活动量组合
   - 验证计算结果在合理范围内

### 属性测试

使用 Kotest 的 Property-Based Testing：

1. **BMI 状态映射属性测试**
   - 生成随机 BMI 值 (10.0 - 50.0)
   - 验证状态颜色符合阈值规则

2. **进度颜色阈值属性测试**
   - 生成随机 (current, target) 组合
   - 验证颜色符合百分比阈值

3. **档案数据显示完整性属性测试**
   - 生成随机 UserProfile
   - 验证显示字符串包含所有必要字段

### UI 测试

1. **空状态测试**
   - profile = null 时显示设置提示
   - 空列表时显示"暂无"

2. **交互测试**
   - 点击头部卡片打开编辑弹窗
   - 点击营养进度卡片导航到统计页
   - 点击清除数据显示确认弹窗

