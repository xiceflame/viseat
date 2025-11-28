package com.rokid.nutrition.phone.network.model

import com.google.gson.annotations.SerializedName

// ==================== 上下文数据模型 ====================

/**
 * 用餐上下文
 * 包含用餐时长、识别次数、累计热量等会话级数据
 */
data class MealContext(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("start_time") val startTime: Long,
    @SerializedName("duration_minutes") val durationMinutes: Double,
    @SerializedName("recognition_count") val recognitionCount: Int,
    @SerializedName("total_consumed_so_far") val totalConsumedSoFar: Double,
    @SerializedName("eating_speed") val eatingSpeed: String  // "fast" | "normal" | "slow"
)

/**
 * 今日上下文
 * 包含今日总摄入、用餐次数、上次用餐时间等
 */
data class DailyContext(
    @SerializedName("total_calories_today") val totalCaloriesToday: Double,
    @SerializedName("total_protein_today") val totalProteinToday: Double,
    @SerializedName("total_carbs_today") val totalCarbsToday: Double,
    @SerializedName("total_fat_today") val totalFatToday: Double,
    @SerializedName("meal_count_today") val mealCountToday: Int,
    @SerializedName("last_meal_hours_ago") val lastMealHoursAgo: Double
)

/**
 * 用餐上下文请求载荷（发送给后端）
 */
data class MealContextPayload(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("start_time") val startTime: Long,
    @SerializedName("duration_minutes") val durationMinutes: Double,
    @SerializedName("recognition_count") val recognitionCount: Int,
    @SerializedName("total_consumed_so_far") val totalConsumedSoFar: Double,
    @SerializedName("eating_speed") val eatingSpeed: String? = null
)

/**
 * 今日上下文请求载荷（发送给后端）
 */
data class DailyContextPayload(
    @SerializedName("total_calories_today") val totalCaloriesToday: Double = 0.0,
    @SerializedName("total_protein_today") val totalProteinToday: Double = 0.0,
    @SerializedName("total_carbs_today") val totalCarbsToday: Double = 0.0,
    @SerializedName("total_fat_today") val totalFatToday: Double = 0.0,
    @SerializedName("meal_count_today") val mealCountToday: Int = 0,
    @SerializedName("last_meal_hours_ago") val lastMealHoursAgo: Double = 0.0
)


// ==================== 用餐结束响应模型 ====================

/**
 * 用餐总结（眼镜显示）
 * 用餐结束时在眼镜上显示的简洁营养总结
 */
data class MealSummaryResponse(
    @SerializedName("total_calories") val totalCalories: Double,
    @SerializedName("total_protein") val totalProtein: Double,
    @SerializedName("total_carbs") val totalCarbs: Double,
    @SerializedName("total_fat") val totalFat: Double,
    @SerializedName("duration_minutes") val durationMinutes: Double,
    val rating: String,  // "good" | "fair" | "poor"
    @SerializedName("short_advice") val shortAdvice: String
)

/**
 * 详细建议（手机显示）
 */
data class MealAdviceResponse(
    val summary: String,
    val suggestions: List<String>,
    val highlights: List<String>,
    val warnings: List<String>
)

/**
 * 下一餐建议
 */
data class NextMealSuggestion(
    @SerializedName("recommended_time") val recommendedTime: String,
    @SerializedName("meal_type") val mealType: String,
    @SerializedName("calorie_budget") val calorieBudget: Double,
    @SerializedName("focus_nutrients") val focusNutrients: List<String>,
    val avoid: List<String>
)

// ==================== 用餐结束请求模型 ====================

/**
 * 用餐结束请求
 */
data class MealEndRequest(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("final_snapshot") val finalSnapshot: SnapshotPayload? = null,
    @SerializedName("meal_context") val mealContext: MealContextPayload? = null,
    @SerializedName("daily_context") val dailyContext: DailyContextPayload? = null,
    @SerializedName("user_profile") val userProfile: UserProfilePayload? = null
)

// ==================== 扩展函数 ====================

/**
 * MealContext 转换为 MealContextPayload
 */
fun MealContext.toPayload(): MealContextPayload {
    return MealContextPayload(
        sessionId = sessionId,
        startTime = startTime,
        durationMinutes = durationMinutes,
        recognitionCount = recognitionCount,
        totalConsumedSoFar = totalConsumedSoFar,
        eatingSpeed = eatingSpeed
    )
}

/**
 * DailyContext 转换为 DailyContextPayload
 */
fun DailyContext.toPayload(): DailyContextPayload {
    return DailyContextPayload(
        totalCaloriesToday = totalCaloriesToday,
        totalProteinToday = totalProteinToday,
        totalCarbsToday = totalCarbsToday,
        totalFatToday = totalFatToday,
        mealCountToday = mealCountToday,
        lastMealHoursAgo = lastMealHoursAgo
    )
}
