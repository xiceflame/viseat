package com.rokid.nutrition.phone.network.model

import com.google.gson.annotations.SerializedName

// ==================== 请求模型 ====================

/**
 * 视觉分析请求（开始进餐）
 * 注意：字段名必须与后端 API 完全匹配
 */
data class VisionAnalyzeRequest(
    @field:SerializedName("image_url") 
    val imageUrl: String,
    val mode: String = "start"  // "start" 表示开始进餐
)

/**
 * 用餐更新请求（带容错）
 * 用于 /api/v1/vision/analyze_meal_update
 */
data class MealUpdateAnalyzeRequest(
    @SerializedName("image_url") val imageUrl: String,
    @SerializedName("baseline_foods") val baselineFoods: List<BaselineFood>
)

/**
 * 基线食物（与后端 BaselineFood 对应）
 */
data class BaselineFood(
    @SerializedName("dish_name") val dishName: String,
    @SerializedName("dish_name_cn") val dishNameCn: String,
    val ingredients: List<BaselineIngredient>,
    @SerializedName("total_weight_g") val totalWeightG: Float = 0f
)

/**
 * 基线食材
 */
data class BaselineIngredient(
    @SerializedName("name_en") val nameEn: String,
    @SerializedName("weight_g") val weightG: Float,
    val confidence: Float = 0.9f
)

/**
 * 用户档案（请求用）
 */
data class UserProfilePayload(
    val age: Int? = null,
    val gender: String? = null,  // male/female
    val bmi: Float? = null,
    @SerializedName("activity_level") val activityLevel: String? = null,  // sedentary/light/moderate/active/very_active
    @SerializedName("health_goal") val healthGoal: String? = null,  // lose_weight/gain_muscle/maintain
    @SerializedName("target_weight") val targetWeight: Float? = null,
    @SerializedName("health_conditions") val healthConditions: List<String>? = null,
    @SerializedName("dietary_preferences") val dietaryPreferences: List<String>? = null
)

/**
 * 快照数据（请求用）
 */
data class SnapshotPayload(
    @SerializedName("image_url") val imageUrl: String,
    val foods: List<FoodInput>,
    val nutrition: NutritionTotal
)

/**
 * 食物输入
 */
data class FoodInput(
    val name: String,
    @SerializedName("weight_g") val weightG: Double,
    @SerializedName("cooking_method") val cookingMethod: String? = null
)

/**
 * 营养总计
 */
data class NutritionTotal(
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

/**
 * 营养对话请求
 */
data class ChatNutritionRequest(
    val question: String,
    @SerializedName("session_id") val sessionId: String? = null,
    @SerializedName("user_profile") val userProfile: UserProfilePayload? = null
)

/**
 * 更新食物数据请求
 */
data class UpdateFoodRequest(
    @SerializedName("food_id") val foodId: String,
    @SerializedName("weight_g") val weightG: Double?,
    @SerializedName("calories_kcal") val caloriesKcal: Double?,
    @SerializedName("protein_g") val proteinG: Double?,
    @SerializedName("carbs_g") val carbsG: Double?,
    @SerializedName("fat_g") val fatG: Double?,
    @SerializedName("edited_at") val editedAt: Long
)

/**
 * 更新食物数据响应
 */
data class UpdateFoodResponse(
    val success: Boolean,
    val message: String,
    @SerializedName("updated_at") val updatedAt: Long? = null
)
