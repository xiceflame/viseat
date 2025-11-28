# 用户信息管理实现总结

## 实现日期
2025-11-27

## 概述
实现了完整的用户信息管理系统，包括后端 API 和前端调用。

## 后端 API 端点

| 端点 | 方法 | 功能 |
|------|------|------|
| `/api/v1/user/register` | POST | 用户注册/登录 |
| `/api/v1/user/profile?device_id=xxx` | GET | 获取用户档案 |
| `/api/v1/user/profile?user_id=xxx` | PUT | 更新用户档案 |
| `/api/v1/user/{user_id}/stats` | GET | 获取用户统计数据 |
| `/api/v1/user/{user_id}/meals` | GET | 获取用户用餐历史 |

### 数据库模型（已存在）

```python
# models.py
class User:
    id, device_id, device_type, device_model, app_version, 
    created_at, updated_at, last_active_at

class UserProfile:
    id, user_id, age, gender, height, weight, bmi,
    activity_level, health_goal, target_weight, target_calories,
    health_conditions, dietary_preferences, created_at, updated_at

class MealSession:
    id, user_id, status, start_time, end_time, ...
```

### API 详情

#### 1. 用户注册 POST /api/v1/user/register
```json
// 请求
{
  "device_id": "xxx",
  "device_type": "phone"
}

// 响应
{
  "user_id": "uuid",
  "device_id": "xxx",
  "is_new_user": true
}
```

#### 2. 获取用户档案 GET /api/v1/user/profile?device_id=xxx
```json
// 响应
{
  "user_id": "xxx",
  "profile": {
    "age": 30,
    "gender": "male",
    "height": 175.0,
    "weight": 70.0,
    "bmi": 22.9,           // 后端自动计算
    "activity_level": "moderate",
    "health_goal": "maintain",
    "target_weight": null,
    "target_calories": 2556,  // 后端自动计算
    "health_conditions": ["hypertension"],
    "dietary_preferences": ["low_salt"]
  }
}
```

#### 3. 更新用户档案 PUT /api/v1/user/profile?user_id=xxx
```json
// 请求
{
  "gender": "male",
  "age": 30,
  "height": 175,
  "weight": 70,
  "activity_level": "moderate",
  "health_goal": "maintain",
  "health_conditions": ["hypertension"],
  "dietary_preferences": ["low_salt"]
}

// 响应 - 后端自动计算 BMI 和 target_calories
{
  "user_id": "xxx",
  "message": "档案更新成功",
  "profile": {
    "bmi": 22.9,
    "target_calories": 2556,
    ...
  }
}
```

#### 4. 获取用户统计 GET /api/v1/user/{user_id}/stats?days=7
```json
// 响应
{
  "user_id": "xxx",
  "today": {
    "calories": 1500.0,
    "meal_count": 2
  },
  "daily_trend": [
    {"date": "2025-11-21", "calories": 1800.0, "meal_count": 3},
    {"date": "2025-11-22", "calories": 1600.0, "meal_count": 2},
    ...
  ],
  "total_meals": 50
}
```

#### 5. 获取用餐历史 GET /api/v1/user/{user_id}/meals?limit=20&offset=0
```json
// 响应
{
  "user_id": "xxx",
  "meals": [
    {
      "session_id": "xxx",
      "start_time": "2025-11-27T12:00:00",
      "end_time": "2025-11-27T12:30:00",
      "status": "completed",
      "duration_minutes": 30.0,
      "total_calories": 650.0,
      "foods": [
        {"name": "rice", "calories": 200.0},
        {"name": "chicken", "calories": 300.0}
      ]
    }
  ],
  "total": 50,
  "limit": 20,
  "offset": 0
}
```

## 前端实现

### 新增/修改文件

| 文件 | 修改内容 |
|------|----------|
| `ApiService.kt` | 添加 5 个新 API 接口 |
| `ApiResponses.kt` | 添加响应模型 |
| `NetworkManager.kt` | 添加 API 调用方法 |
| `UserProfileRepository.kt` | 添加后端同步功能 |

### 新增响应模型

```kotlin
// ApiResponses.kt
data class UserProfileResponse(...)
data class UserProfileData(...)
data class UserProfileUpdateRequest(...)
data class UserProfileUpdateResponse(...)
data class UserStatsResponse(...)
data class TodayStats(...)
data class DailyTrendItem(...)
data class UserMealsResponse(...)
data class MealHistoryItem(...)
data class MealFoodItem(...)
```

### NetworkManager 新增方法

```kotlin
suspend fun getUserProfile(userId: String): Result<UserProfileResponse>
suspend fun updateUserProfile(...): Result<UserProfileUpdateResponse>
suspend fun getUserStats(userId: String, days: Int): Result<UserStatsResponse>
suspend fun getUserMeals(userId: String, limit: Int, offset: Int): Result<UserMealsResponse>
```

### UserProfileRepository 新增方法

```kotlin
// 保存并同步到后端
suspend fun saveProfileAndSync(profile: UserProfile, userId: String): Result<Unit>

// 从后端同步到本地
suspend fun syncFromBackend(userId: String): Result<UserProfile?>
```

## 数据存储情况

### 用餐数据存储

✅ **后端数据库已存储：**
- `meal_sessions` 表：用餐会话记录
- `meal_snapshots` 表：用餐快照（图片、营养数据）
- `snapshot_foods` 表：每个快照的食物详情

✅ **前端本地数据库已存储：**
- `meal_sessions` 表：本地会话记录
- `meal_snapshots` 表：本地快照
- `snapshot_foods` 表：本地食物详情
- `user_profiles` 表：用户档案

### 数据同步流程

```
用户编辑档案
    ↓
ProfileScreen.onSaveProfile()
    ↓
UserProfileRepository.saveProfileAndSync()
    ↓
├── 1. 保存到本地 Room 数据库
└── 2. 调用 NetworkManager.updateUserProfile()
        ↓
    后端 PUT /api/v1/user/profile/{user_id}
        ↓
    后端保存到 PostgreSQL/SQLite
```

## 使用示例

### 1. 用户注册（App 启动时）
```kotlin
// HomeViewModel.kt
private fun checkAndRegisterUser() {
    viewModelScope.launch {
        if (userManager.needsRegistration()) {
            val deviceId = userManager.getDeviceId()
            val result = networkManager.registerUser(deviceId)
            result.fold(
                onSuccess = { response ->
                    userManager.saveRegisterResponse(response)
                },
                onFailure = { e -> Log.e(TAG, "注册失败", e) }
            )
        }
    }
}
```

### 2. 保存用户档案（ProfileScreen）
```kotlin
// 在 ProfileScreen 中
onSaveProfile = { newProfile ->
    viewModelScope.launch {
        val userId = userManager.getUserId()
        if (userId != null) {
            userProfileRepository.saveProfileAndSync(newProfile, userId)
        } else {
            userProfileRepository.saveProfile(newProfile)
        }
    }
}
```

### 3. 获取用户统计（StatsScreen）
```kotlin
viewModelScope.launch {
    val userId = userManager.getUserId() ?: return@launch
    val result = networkManager.getUserStats(userId, days = 7)
    result.fold(
        onSuccess = { stats ->
            _uiState.update { it.copy(
                todayCalories = stats.today.calories,
                dailyTrend = stats.dailyTrend
            )}
        },
        onFailure = { e -> Log.e(TAG, "获取统计失败", e) }
    )
}
```

## 编译状态
✅ 前端编译成功 (2025-11-27)
⚠️ 后端需要重启以加载新 API

## 待完成
1. ProfileScreen 集成 saveProfileAndSync
2. StatsScreen 集成后端统计 API
3. HistoryScreen 集成后端用餐历史 API
4. 添加数据同步状态指示器
