package com.rokid.nutrition.phone.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.rokid.nutrition.phone.ui.theme.*
import java.util.Calendar

// ==================== 进度颜色状态 ====================

/**
 * 进度颜色状态枚举
 * 用于统一管理进度指示器的颜色编码
 */
enum class ProgressColorState(val color: Color) {
    ON_TRACK(AppleTeal),      // < 80% - 绿色
    APPROACHING(AppleOrange), // 80% - 100% - 橙色
    EXCEEDED(AppleRed)        // > 100% - 红色
}

/**
 * 根据当前值和目标值获取进度颜色状态
 * 
 * @param current 当前值
 * @param target 目标值
 * @return 进度颜色状态
 * 
 * 规则:
 * - current/target < 0.8 -> ON_TRACK (绿色)
 * - 0.8 <= current/target < 1.0 -> APPROACHING (橙色)
 * - current/target >= 1.0 -> EXCEEDED (红色)
 */
fun getProgressColorState(current: Double, target: Double): ProgressColorState {
    if (target <= 0) return ProgressColorState.ON_TRACK
    val percentage = current / target
    return when {
        percentage >= 1.0 -> ProgressColorState.EXCEEDED
        percentage >= 0.8 -> ProgressColorState.APPROACHING
        else -> ProgressColorState.ON_TRACK
    }
}

/**
 * 获取进度颜色（便捷方法）
 */
fun getProgressColor(current: Double, target: Double): Color {
    return getProgressColorState(current, target).color
}

// ==================== 健康目标类型 ====================

/**
 * 健康目标类型枚举
 * 定义不同健康目标的显示属性
 */
enum class HealthGoalType(
    val key: String,
    val displayName: String,
    val color: Color,
    val icon: ImageVector
) {
    LOSE_WEIGHT("lose_weight", "减重", AppleBlue, Icons.Rounded.TrendingDown),
    MAINTAIN("maintain", "维持", AppleTeal, Icons.Rounded.Balance),
    GAIN_MUSCLE("gain_muscle", "增肌", AppleOrange, Icons.Rounded.FitnessCenter);
    
    companion object {
        /**
         * 根据 key 获取健康目标类型
         */
        fun fromKey(key: String?): HealthGoalType {
            return entries.find { it.key == key } ?: MAINTAIN
        }
        
        /**
         * 获取健康目标的颜色
         */
        fun getColor(key: String?): Color {
            return fromKey(key).color
        }
        
        /**
         * 获取健康目标的显示名称
         */
        fun getDisplayName(key: String?): String {
            return fromKey(key).displayName
        }
        
        /**
         * 获取健康目标的图标
         */
        fun getIcon(key: String?): ImageVector {
            return fromKey(key).icon
        }
    }
}

// ==================== 营养素类型 ====================

/**
 * 营养素类型枚举
 * 定义不同营养素的显示属性
 */
enum class NutrientType(
    val key: String,
    val displayName: String,
    val color: Color,
    val caloriesPerGram: Int
) {
    PROTEIN("protein", "蛋白质", AppleBlue, 4),
    CARBS("carbs", "碳水化合物", AppleOrange, 4),
    FAT("fat", "脂肪", ApplePurple, 9),
    CALORIES("calories", "热量", AppleTeal, 1);
    
    companion object {
        /**
         * 根据 key 获取营养素类型
         */
        fun fromKey(key: String): NutrientType? {
            return entries.find { it.key == key }
        }
        
        /**
         * 获取营养素的颜色
         */
        fun getColor(key: String): Color {
            return fromKey(key)?.color ?: AppleGray1
        }
    }
}

// ==================== BMI 状态 ====================

/**
 * BMI 状态枚举
 */
enum class BMIStatus(
    val label: String,
    val color: Color,
    val suggestion: String,
    val minBMI: Float,
    val maxBMI: Float
) {
    UNDERWEIGHT("偏瘦", AppleBlue, "建议适当增加营养摄入", 0f, 18.5f),
    NORMAL("正常", AppleTeal, "继续保持健康饮食", 18.5f, 24.0f),
    OVERWEIGHT("偏胖", AppleOrange, "建议控制热量摄入", 24.0f, 28.0f),
    OBESE("肥胖", AppleRed, "建议咨询营养师", 28.0f, Float.MAX_VALUE);
    
    companion object {
        /**
         * 根据 BMI 值获取状态
         */
        fun fromBMI(bmi: Float): BMIStatus = when {
            bmi < 18.5f -> UNDERWEIGHT
            bmi < 24.0f -> NORMAL
            bmi < 28.0f -> OVERWEIGHT
            else -> OBESE
        }
    }
}

/**
 * 计算 BMI 值
 * 
 * @param weight 体重（kg）
 * @param height 身高（cm）
 * @return BMI 值，如果输入无效则返回 0
 */
fun calculateBMI(weight: Float, height: Float): Float {
    if (weight <= 0 || height <= 0) return 0f
    val heightInMeters = height / 100f
    return weight / (heightInMeters * heightInMeters)
}

/**
 * 获取 BMI 状态
 */
fun getBMIStatus(bmi: Float): BMIStatus = BMIStatus.fromBMI(bmi)

// ==================== 宏量营养素比例 ====================

/**
 * 宏量营养素比例数据类
 */
data class MacroRatio(
    val proteinRatio: Float,
    val carbsRatio: Float,
    val fatRatio: Float
) {
    init {
        // 确保比例总和为 1.0
        require(kotlin.math.abs(proteinRatio + carbsRatio + fatRatio - 1.0f) < 0.01f) {
            "Macro ratios must sum to 1.0"
        }
    }
    
    /**
     * 根据总热量计算各营养素的克数
     */
    fun calculateGrams(totalCalories: Int): Triple<Int, Int, Int> {
        val proteinGrams = (totalCalories * proteinRatio / 4).toInt()  // 1g蛋白质 = 4kcal
        val carbsGrams = (totalCalories * carbsRatio / 4).toInt()      // 1g碳水 = 4kcal
        val fatGrams = (totalCalories * fatRatio / 9).toInt()          // 1g脂肪 = 9kcal
        return Triple(proteinGrams, carbsGrams, fatGrams)
    }
}

/**
 * 根据饮食类型获取宏量营养素比例
 * 
 * @param dietType 饮食类型
 * @return 宏量营养素比例
 */
fun getMacroRatioByDietType(dietType: String?): MacroRatio {
    return when (dietType) {
        "keto" -> MacroRatio(0.25f, 0.05f, 0.70f)
        "low_carb" -> MacroRatio(0.30f, 0.20f, 0.50f)
        else -> MacroRatio(0.25f, 0.50f, 0.25f)  // 默认比例
    }
}

// ==================== 每日热量目标计算 ====================

/**
 * 活动系数
 */
object ActivityMultiplier {
    const val SEDENTARY = 1.2f      // 久坐
    const val LIGHT = 1.375f        // 轻度活动
    const val MODERATE = 1.55f      // 中度活动
    const val ACTIVE = 1.725f       // 高度活动
    const val VERY_ACTIVE = 1.9f    // 非常活跃
    
    fun fromKey(key: String?): Float = when (key) {
        "sedentary" -> SEDENTARY
        "light" -> LIGHT
        "moderate" -> MODERATE
        "active" -> ACTIVE
        "very_active" -> VERY_ACTIVE
        else -> LIGHT  // 默认轻度活动
    }
}

/**
 * 健康目标热量调整
 */
object GoalCalorieAdjustment {
    const val LOSE_WEIGHT = -500    // 减重：减少 500 kcal
    const val MAINTAIN = 0          // 维持：不调整
    const val GAIN_MUSCLE = 300     // 增肌：增加 300 kcal
    
    fun fromKey(key: String?): Int = when (key) {
        "lose_weight" -> LOSE_WEIGHT
        "gain_muscle" -> GAIN_MUSCLE
        else -> MAINTAIN
    }
}

/**
 * 计算基础代谢率 (BMR) - 使用 Mifflin-St Jeor 公式
 * 
 * @param weight 体重（kg）
 * @param height 身高（cm）
 * @param age 年龄
 * @param gender 性别 ("male" 或 "female")
 * @return BMR 值
 */
fun calculateBMR(weight: Float, height: Float, age: Int, gender: String?): Float {
    // Mifflin-St Jeor 公式
    val baseBMR = 10 * weight + 6.25f * height - 5 * age
    return if (gender == "male") {
        baseBMR + 5
    } else {
        baseBMR - 161
    }
}

/**
 * 计算每日热量目标
 * 
 * @param weight 体重（kg）
 * @param height 身高（cm）
 * @param age 年龄
 * @param gender 性别
 * @param activityLevel 活动水平
 * @param healthGoal 健康目标
 * @return 每日热量目标
 */
fun calculateDailyCalories(
    weight: Float,
    height: Float,
    age: Int,
    gender: String?,
    activityLevel: String?,
    healthGoal: String?
): Int {
    val bmr = calculateBMR(weight, height, age, gender)
    val activityMultiplier = ActivityMultiplier.fromKey(activityLevel)
    val goalAdjustment = GoalCalorieAdjustment.fromKey(healthGoal)
    
    val tdee = bmr * activityMultiplier
    return (tdee + goalAdjustment).toInt().coerceAtLeast(1200)  // 最低 1200 kcal
}

// ==================== 体重目标进度计算 ====================

/**
 * 计算体重目标进度百分比
 * 
 * @param startWeight 起始体重
 * @param currentWeight 当前体重
 * @param targetWeight 目标体重
 * @param healthGoal 健康目标
 * @return 进度百分比 (0-100)
 */
fun calculateWeightGoalProgress(
    startWeight: Float,
    currentWeight: Float,
    targetWeight: Float,
    healthGoal: String?
): Float {
    if (startWeight == targetWeight) return 100f
    
    return when (healthGoal) {
        "lose_weight" -> {
            // 减重: (startWeight - currentWeight) / (startWeight - targetWeight) * 100
            if (startWeight <= targetWeight) return 0f
            val progress = (startWeight - currentWeight) / (startWeight - targetWeight) * 100
            progress.coerceIn(0f, 100f)
        }
        "gain_muscle" -> {
            // 增肌: (currentWeight - startWeight) / (targetWeight - startWeight) * 100
            if (targetWeight <= startWeight) return 0f
            val progress = (currentWeight - startWeight) / (targetWeight - startWeight) * 100
            progress.coerceIn(0f, 100f)
        }
        else -> {
            // 维持: 如果当前体重接近起始体重，则进度为 100%
            val deviation = kotlin.math.abs(currentWeight - startWeight)
            val tolerance = startWeight * 0.02f  // 2% 容差
            if (deviation <= tolerance) 100f else (100f - deviation / startWeight * 100).coerceIn(0f, 100f)
        }
    }
}

// ==================== 年龄计算 ====================

/**
 * 从出生日期计算年龄
 * 
 * @param birthDateMillis 出生日期的毫秒时间戳
 * @return 年龄
 */
fun calculateAgeFromBirthDate(birthDateMillis: Long): Int {
    if (birthDateMillis <= 0) return 0
    
    val birthCalendar = Calendar.getInstance().apply {
        timeInMillis = birthDateMillis
    }
    val today = Calendar.getInstance()
    
    var age = today.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)
    
    // 如果今年的生日还没到，年龄减 1
    if (today.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) {
        age--
    }
    
    return age.coerceAtLeast(0)
}

// ==================== 热量警告阈值 ====================

/**
 * 检查是否应该显示热量警告
 * 
 * @param currentCalories 当前热量摄入
 * @param targetCalories 目标热量
 * @param warningThreshold 警告阈值（默认 0.8，即 80%）
 * @return 是否应该显示警告
 */
fun shouldShowCalorieWarning(
    currentCalories: Double,
    targetCalories: Double,
    warningThreshold: Float = 0.8f
): Boolean {
    if (targetCalories <= 0) return false
    return currentCalories >= targetCalories * warningThreshold
}
