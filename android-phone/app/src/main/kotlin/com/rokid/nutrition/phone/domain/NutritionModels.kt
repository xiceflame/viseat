package com.rokid.nutrition.phone.domain

import androidx.compose.ui.graphics.Color

/**
 * 健康目标
 */
enum class HealthGoal(val value: String, val displayName: String) {
    LOSE_WEIGHT("lose_weight", "减重瘦身"),
    MAINTAIN("maintain", "维持健康"),
    GAIN_MUSCLE("gain_muscle", "增肌塑形");
    
    companion object {
        fun fromValue(value: String): HealthGoal = entries.find { it.value == value } ?: MAINTAIN
    }
}

/**
 * 性别
 */
enum class Gender(val value: String, val displayName: String) {
    MALE("male", "男"),
    FEMALE("female", "女");
    
    companion object {
        fun fromValue(value: String): Gender = entries.find { it.value == value } ?: MALE
    }
}

/**
 * 活动量等级
 */
enum class ActivityLevel(
    val value: String,
    val displayName: String,
    val description: String,
    val factor: Float
) {
    SEDENTARY("sedentary", "久坐办公", "几乎不运动，长时间坐着工作", 1.2f),
    LIGHT("light", "轻度活动", "每周运动 1-3 次，或日常步行较多", 1.375f),
    MODERATE("moderate", "中度活动", "每周运动 3-5 次，每次 30 分钟以上", 1.55f),
    ACTIVE("active", "高度活动", "每周运动 6-7 次，或从事体力劳动", 1.725f),
    VERY_ACTIVE("very_active", "专业运动", "每天高强度训练，或专业运动员", 1.9f);
    
    companion object {
        fun fromValue(value: String): ActivityLevel = entries.find { it.value == value } ?: MODERATE
    }
}

/**
 * 饮食类型
 */
enum class DietType(
    val value: String,
    val displayName: String,
    val proteinRatio: Float,
    val carbsRatio: Float,
    val fatRatio: Float
) {
    OMNIVORE("omnivore", "无限制", 0.25f, 0.50f, 0.25f),
    VEGETARIAN("vegetarian", "素食", 0.20f, 0.55f, 0.25f),
    VEGAN("vegan", "纯素", 0.18f, 0.57f, 0.25f),
    LOW_CARB("low_carb", "低碳水", 0.30f, 0.30f, 0.40f),
    KETO("keto", "生酮", 0.25f, 0.05f, 0.70f),
    MEDITERRANEAN("mediterranean", "地中海饮食", 0.20f, 0.45f, 0.35f);
    
    companion object {
        fun fromValue(value: String): DietType = entries.find { it.value == value } ?: OMNIVORE
    }
}

/**
 * 过敏原
 */
enum class Allergen(val value: String, val displayName: String) {
    GLUTEN("gluten", "麸质"),
    DAIRY("dairy", "乳制品"),
    NUTS("nuts", "坚果"),
    SHELLFISH("shellfish", "海鲜"),
    EGGS("eggs", "鸡蛋"),
    SOY("soy", "大豆");
    
    companion object {
        fun fromValue(value: String): Allergen? = entries.find { it.value == value }
    }
}

/**
 * BMI 状态
 */
enum class BMIStatus(
    val label: String,
    val color: Color,
    val suggestion: String,
    val minBMI: Float,
    val maxBMI: Float
) {
    UNDERWEIGHT("偏瘦", Color(0xFF2196F3), "建议适当增加营养摄入", 0f, 18.5f),
    NORMAL("正常", Color(0xFF4CAF50), "继续保持健康饮食", 18.5f, 24f),
    OVERWEIGHT("偏胖", Color(0xFFFFC107), "建议控制热量摄入", 24f, 28f),
    OBESE("肥胖", Color(0xFFFF5722), "建议咨询营养师", 28f, Float.MAX_VALUE)
}

/**
 * 营养目标
 */
data class NutritionGoals(
    val dailyCalories: Int,
    val bmr: Int,
    val activityCalories: Int,
    val goalAdjustment: Int,  // 正数为盈余，负数为缺口
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
    val proteinRatio: Float,
    val carbsRatio: Float,
    val fatRatio: Float,
    val isCustom: Boolean = false
)
