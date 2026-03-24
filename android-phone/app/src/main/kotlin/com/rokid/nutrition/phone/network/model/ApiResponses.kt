package com.rokid.nutrition.phone.network.model

import com.google.gson.annotations.SerializedName

// ==================== 响应模型 ====================

/**
 * 图片上传响应
 */
data class UploadResponse(
    val filename: String,
    val url: String,
    val message: String
)

/**
 * 用户注册响应
 */
data class UserRegisterResponse(
    @SerializedName("user_id") val userId: String,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("is_new_user") val isNewUser: Boolean,
    val token: String? = null
)

/**
 * 用户注册请求
 */
data class UserRegisterRequest(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("device_type") val deviceType: String = "phone",
    @SerializedName("device_model") val deviceModel: String? = null,
    @SerializedName("app_version") val appVersion: String? = null
)

/**
 * 用户档案响应
 */
data class UserProfileResponse(
    @SerializedName("user_id") val userId: String,
    val profile: UserProfileData?,
    val message: String? = null
)

/**
 * 用户档案数据
 */
data class UserProfileData(
    val age: Int?,
    val gender: String?,
    val height: Float?,
    val weight: Float?,
    val bmi: Float?,
    @SerializedName("activity_level") val activityLevel: String?,
    @SerializedName("health_goal") val healthGoal: String?,
    @SerializedName("target_weight") val targetWeight: Float?,
    @SerializedName("target_calories") val targetCalories: Float?,
    @SerializedName("health_conditions") val healthConditions: List<String>?,
    @SerializedName("dietary_preferences") val dietaryPreferences: List<String>?,
    @SerializedName("updated_at") val updatedAt: String?,
    // 新增字段 - 增强版个人信息
    @SerializedName("birth_date") val birthDate: String? = null,  // ISO 8601 格式: "1990-01-15"
    @SerializedName("target_date") val targetDate: String? = null,  // 目标达成日期
    @SerializedName("diet_type") val dietType: String? = null,  // 饮食类型: balanced/low_carb/high_protein/vegetarian/vegan/keto
    val allergens: List<String>? = null,  // 过敏原列表
    @SerializedName("target_protein") val targetProtein: Float? = null,  // 蛋白质目标 (g)
    @SerializedName("target_carbs") val targetCarbs: Float? = null,  // 碳水目标 (g)
    @SerializedName("target_fat") val targetFat: Float? = null  // 脂肪目标 (g)
)

/**
 * 用户档案更新请求
 * 
 * 后端会自动计算 BMI 和 target_calories
 * 
 * 字段映射：
 * - height -> height_cm (身高)
 * - weight -> weight_kg (体重)
 * - birthDate -> birth_year (出生年份，从 birthDate 提取)
 * - allergens -> allergies (过敏原)
 */
data class UserProfileUpdateRequest(
    val age: Int? = null,
    val gender: String? = null,
    @SerializedName("height_cm") val height: Float? = null,  // 身高 (cm)
    @SerializedName("weight_kg") val weight: Float? = null,  // 体重 (kg)
    @SerializedName("activity_level") val activityLevel: String? = null,
    @SerializedName("health_goal") val healthGoal: String? = null,
    @SerializedName("target_weight") val targetWeight: Float? = null,
    @SerializedName("health_conditions") val healthConditions: List<String>? = null,
    @SerializedName("dietary_preferences") val dietaryPreferences: List<String>? = null,
    // 新增字段 - 增强版个人信息
    @SerializedName("birth_year") val birthYear: Int? = null,  // 出生年份
    @SerializedName("target_date") val targetDate: String? = null,  // 目标达成日期
    @SerializedName("diet_type") val dietType: String? = null,  // 饮食类型
    val allergies: List<String>? = null,  // 过敏原列表
    @SerializedName("target_protein") val targetProtein: Float? = null,  // 蛋白质目标 (g)
    @SerializedName("target_carbs") val targetCarbs: Float? = null,  // 碳水目标 (g)
    @SerializedName("target_fat") val targetFat: Float? = null  // 脂肪目标 (g)
)

/**
 * 用户档案更新响应
 */
data class UserProfileUpdateResponse(
    @SerializedName("user_id") val userId: String,
    val message: String,
    val profile: UserProfileData
)

/**
 * 用户统计响应
 */
data class UserStatsResponse(
    @SerializedName("user_id") val userId: String,
    val today: TodayStats,
    @SerializedName("daily_trend") val dailyTrend: List<DailyTrendItem>,
    @SerializedName("total_meals") val totalMeals: Int
)

data class TodayStats(
    val calories: Double,
    @SerializedName("meal_count") val mealCount: Int
)

data class DailyTrendItem(
    val date: String,
    val calories: Double,
    @SerializedName("meal_count") val mealCount: Int
)

/**
 * 用户用餐历史响应
 */
data class UserMealsResponse(
    @SerializedName("user_id") val userId: String,
    val meals: List<MealHistoryItem>,
    val total: Int,
    val limit: Int,
    val offset: Int
)

data class MealHistoryItem(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("start_time") val startTime: String?,
    @SerializedName("end_time") val endTime: String?,
    val status: String,
    @SerializedName("duration_minutes") val durationMinutes: Double,
    @SerializedName("total_calories") val totalCalories: Double,
    val foods: List<MealFoodItem>
)

data class MealFoodItem(
    val name: String,
    val calories: Double
)

/**
 * 开始用餐会话请求
 */
data class MealStartRequest(
    @SerializedName("user_id") val userId: String,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("meal_type") val mealType: String = "meal",
    @SerializedName("auto_capture_interval") val autoCaptureInterval: Int = 300
)

/**
 * 视觉分析响应
 * 
 * 后端返回的 suggestion 字段在两个位置：
 * 1. 顶层 suggestion（推荐使用）
 * 2. raw_llm.suggestion（LLM 原始输出）
 */
data class VisionAnalyzeResponse(
    @SerializedName("snapshot_id") val snapshotId: String? = null,
    @SerializedName("session_id") val sessionId: String? = null,
    @SerializedName("raw_llm") val rawLlm: RawLlm,
    val snapshot: SnapshotData,
    @SerializedName("suggestion") val topLevelSuggestion: String? = null  // 顶层建议（后端提取）
) {
    /** 获取建议，优先使用顶层，其次使用 raw_llm 中的 */
    fun getEffectiveSuggestion(): String = topLevelSuggestion?.takeIf { it.isNotBlank() } 
        ?: rawLlm.suggestion?.takeIf { it.isNotBlank() } 
        ?: ""
}

data class RawLlm(
    @SerializedName("is_food") val isFood: Boolean,
    val foods: List<FoodItemResponse>,
    val suggestion: String? = null  // LLM 餐饮建议
)

data class FoodItemResponse(
    @SerializedName("dish_name") val dishName: String,
    @SerializedName("dish_name_cn") val dishNameCn: String?,  // 中文菜名
    @SerializedName("cooking_method") val cookingMethod: String,
    val ingredients: List<Ingredient>,
    @SerializedName("total_weight_g") val totalWeightG: Double,
    val confidence: Double,
    val category: String? = null  // 后端返回的分类: meal/snack/beverage/dessert/fruit
) {
    /** 优先返回中文名，如果没有则返回英文名 */
    fun getDisplayName(): String = dishNameCn?.takeIf { it.isNotBlank() } ?: dishName
}

data class Ingredient(
    @SerializedName("name_en") val nameEn: String,
    @SerializedName("weight_g") val weightG: Double,
    val confidence: Double
)

data class SnapshotData(
    val foods: List<FoodInput>,
    val nutrition: NutritionTotal,
    @SerializedName("image_url") val imageUrl: String? = null  // 后端返回的图片URL
)

/**
 * 开始用餐会话响应
 */
data class MealStartResponse(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("initial_nutrition") val initialNutrition: NutritionTotal,
    @SerializedName("auto_capture_interval") val autoCaptureInterval: Int,
    val status: String
)


/**
 * 更新用餐会话响应
 */
data class MealUpdateResponse(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("current_status") val currentStatus: CurrentStatus,
    @SerializedName("since_last_update") val sinceLastUpdate: SinceLastUpdate,
    val advice: String,
    @SerializedName("snapshots_count") val snapshotsCount: Int
)

data class CurrentStatus(
    @SerializedName("remaining_calories") val remainingCalories: Double,
    @SerializedName("consumed_total") val consumedTotal: Double,
    @SerializedName("consumption_ratio") val consumptionRatio: Double,
    @SerializedName("duration_minutes") val durationMinutes: Double
)

data class SinceLastUpdate(
    @SerializedName("net_calories_change") val netCaloriesChange: Double,
    @SerializedName("time_elapsed_minutes") val timeElapsedMinutes: Double
)

/**
 * 结束用餐会话响应
 */
data class MealEndResponse(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("final_stats") val finalStats: FinalStats,
    val report: String? = null,
    val message: String,
    val status: String? = null,
    @SerializedName("duration_minutes") val durationMinutes: Double? = null,
    @SerializedName("meal_summary") val mealSummary: MealSummaryResponse? = null,
    val advice: MealAdviceResponse? = null,
    @SerializedName("next_meal_suggestion") val nextMealSuggestion: NextMealSuggestion? = null
)

data class FinalStats(
    @SerializedName("total_served") val totalServed: Double,
    @SerializedName("total_consumed") val totalConsumed: Double,
    @SerializedName("consumption_ratio") val consumptionRatio: Double,
    @SerializedName("duration_minutes") val durationMinutes: Double,
    @SerializedName("average_eating_speed") val averageEatingSpeed: Double,
    @SerializedName("waste_ratio") val wasteRatio: Double,
    @SerializedName("snapshots_count") val snapshotsCount: Int
)

/**
 * 会话详情响应
 */
data class MealSessionDetailResponse(
    val session: SessionInfo,
    val progress: ProgressInfo
)

data class SessionInfo(
    val id: String,
    @SerializedName("user_id") val userId: String,
    val status: String,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String?,
    @SerializedName("auto_capture_interval") val autoCaptureInterval: Int
)

data class ProgressInfo(
    @SerializedName("total_served_calories") val totalServedCalories: Double,
    @SerializedName("current_calories") val currentCalories: Double,
    @SerializedName("total_consumed") val totalConsumed: Double,
    @SerializedName("consumption_ratio") val consumptionRatio: Double,
    @SerializedName("duration_minutes") val durationMinutes: Double
)

/**
 * 会话列表响应
 */
data class MealSessionsListResponse(
    val sessions: List<SessionSummary>,
    val total: Int
)

data class SessionSummary(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String?,
    val status: String,
    @SerializedName("initial_calories") val initialCalories: Double,
    @SerializedName("current_calories") val currentCalories: Double,
    @SerializedName("snapshots_count") val snapshotsCount: Int
)

/**
 * 营养对话响应
 */
data class ChatNutritionResponse(
    val answer: String,
    @SerializedName("suggested_actions") val suggestedActions: List<String>
)


// ==================== 用餐更新响应（带容错） ====================

/**
 * 用餐更新分析响应
 * 来自 /api/v1/vision/analyze_meal_update
 */
data class MealUpdateAnalyzeResponse(
    val status: String,  // "accept" | "skip" | "adjust"
    val message: String,
    @SerializedName("raw_llm") val rawLlm: RawLlm,
    val snapshot: SnapshotData,
    val comparison: ComparisonData?
)

/**
 * 比较数据（加菜时返回）
 */
data class ComparisonData(
    @SerializedName("baseline_total_weight") val baselineTotalWeight: Double,
    @SerializedName("current_total_weight") val currentTotalWeight: Double,
    @SerializedName("weight_change_ratio") val weightChangeRatio: Double,
    val adjustments: List<Adjustment>?
)

/**
 * 调整项（加菜详情）
 */
data class Adjustment(
    val action: String,  // "add_new" | "increase" | "decrease"
    val ingredient: String,
    val weight: Float,
    val reason: String?
)

// ==================== 食物类型判断 ====================

/**
 * 食物类型
 */
enum class FoodType {
    MEAL,       // 正餐（会激活用餐监测）
    SNACK,      // 零食（不激活用餐监测）
    PACKAGED,   // 包装食品（不激活用餐监测）
    UNKNOWN     // 未知
}

/**
 * 删除用餐会话响应
 */
data class DeleteMealResponse(
    val success: Boolean,
    val message: String
)

// ==================== 个性化建议 API ====================

/**
 * 个性化建议响应
 * GET /api/v1/users/{user_id}/personalized-tips
 */
data class PersonalizedTipsResponse(
    val tips: List<PersonalizedTip>,
    @SerializedName("generated_at") val generatedAt: String?,
    @SerializedName("data_summary") val dataSummary: DataSummary?
)

/**
 * 个性化建议项
 */
data class PersonalizedTip(
    val id: String,
    val content: String,
    val category: String,  // nutrition/timing/habit/warning/encouragement
    val priority: Int,     // 1-10，1最高
    @SerializedName("valid_until") val validUntil: String?
)

/**
 * 数据摘要
 */
data class DataSummary(
    @SerializedName("avg_daily_calories") val avgDailyCalories: Double?,
    @SerializedName("protein_ratio") val proteinRatio: Double?,
    @SerializedName("carbs_ratio") val carbsRatio: Double?,
    @SerializedName("fat_ratio") val fatRatio: Double?,
    @SerializedName("meal_regularity_score") val mealRegularityScore: Double?
)

/**
 * 刷新个性化建议请求
 * POST /api/v1/users/{user_id}/personalized-tips/refresh
 */
data class RefreshTipsRequest(
    val trigger: String,  // meal_ended/daily_refresh/manual
    @SerializedName("meal_session_id") val mealSessionId: String? = null
)

/**
 * 刷新个性化建议响应
 */
data class RefreshTipsResponse(
    val status: String,  // queued/completed
    @SerializedName("estimated_time") val estimatedTime: Int?,
    val tips: List<PersonalizedTip>?  // 如果 status=completed，直接返回新建议
)

/**
 * 建议反馈请求
 * PUT /api/v1/users/{user_id}/personalized-tips/{tip_id}/feedback
 */
data class TipFeedbackRequest(
    @SerializedName("is_helpful") val isHelpful: Boolean
)

/**
 * 建议反馈响应
 */
data class TipFeedbackResponse(
    val success: Boolean,
    val message: String
)

/**
 * 判断食物类型的扩展函数
 * 
 * 直接使用后端返回的 category 字段判断：
 * - meal → 正餐（激活用餐监测）
 * - snack/beverage/dessert/fruit → 非正餐（不激活用餐监测）
 * 
 * 判断逻辑：只要有任何一个 meal 类型的食物，就激活用餐监测
 * 例如：米饭(meal) + 可乐(beverage) → 激活用餐监测（以正餐为主）
 */
fun VisionAnalyzeResponse.getFoodType(): FoodType {
    val TAG = "FoodTypeDetector"
    
    if (!rawLlm.isFood || rawLlm.foods.isEmpty()) {
        android.util.Log.d(TAG, "非食物或食物列表为空")
        return FoodType.UNKNOWN
    }
    
    // 统计各类别数量
    var mealCount = 0
    var snackCount = 0
    var otherCount = 0
    
    for (food in rawLlm.foods) {
        val category = food.category?.lowercase() ?: ""
        android.util.Log.d(TAG, "食物: ${food.getDisplayName()}, category=$category")
        
        when (category) {
            "meal" -> mealCount++
            "snack" -> snackCount++
            "beverage", "dessert", "fruit" -> otherCount++
            else -> {
                // 如果后端没返回 category，根据热量简单判断
                android.util.Log.w(TAG, "未知分类，使用热量判断")
            }
        }
    }
    
    android.util.Log.d(TAG, "分类统计: meal=$mealCount, snack=$snackCount, other=$otherCount")
    
    // 判断逻辑：只要有任何一个 meal 类型的食物，就激活用餐监测（以正餐为主）
    val result = when {
        // 有任何正餐，就激活用餐监测（正餐优先）
        mealCount > 0 -> FoodType.MEAL
        // 没有正餐，但有零食/饮料/甜点/水果
        snackCount > 0 || otherCount > 0 -> FoodType.SNACK
        // 后端没返回 category，根据热量判断（阈值 400kcal）
        snapshot.nutrition.calories > 400 -> FoodType.MEAL
        else -> FoodType.SNACK
    }
    
    android.util.Log.d(TAG, "最终判断: $result (meal优先策略)")
    return result
}



// ==================== 体重追踪 API ====================

/**
 * 体重记录请求
 * POST /api/v1/users/{user_id}/weight
 */
data class WeightEntryRequest(
    val weight: Float,  // 体重 (kg)
    @SerializedName("recorded_at") val recordedAt: String? = null,  // ISO 8601 格式，默认当前时间
    val note: String? = null  // 可选备注
)

/**
 * 体重记录响应
 */
data class WeightEntryResponse(
    val id: String,
    val weight: Float,
    @SerializedName("recorded_at") val recordedAt: String,
    val note: String?,
    val message: String
)

/**
 * 体重历史响应
 * GET /api/v1/users/{user_id}/weight/history
 */
data class WeightHistoryResponse(
    val entries: List<WeightEntryData>,
    val summary: WeightSummary?
)

/**
 * 体重记录数据
 */
data class WeightEntryData(
    val id: String,
    val weight: Float,
    @SerializedName("recorded_at") val recordedAt: String,
    val note: String?
)

/**
 * 体重统计摘要
 */
data class WeightSummary(
    @SerializedName("start_weight") val startWeight: Float?,  // 起始体重
    @SerializedName("current_weight") val currentWeight: Float?,  // 当前体重
    @SerializedName("target_weight") val targetWeight: Float?,  // 目标体重
    @SerializedName("total_change") val totalChange: Float?,  // 总变化
    @SerializedName("weekly_avg_change") val weeklyAvgChange: Float?,  // 每周平均变化
    @SerializedName("progress_percent") val progressPercent: Float?  // 进度百分比
)

/**
 * 删除体重记录响应
 */
data class DeleteWeightResponse(
    val success: Boolean,
    val message: String
)
