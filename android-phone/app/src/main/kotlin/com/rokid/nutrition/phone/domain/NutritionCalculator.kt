package com.rokid.nutrition.phone.domain

import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * 营养计算器
 * 
 * 提供 BMI、BMR、TDEE、宏量营养素等计算功能
 */
object NutritionCalculator {
    
    /**
     * 计算 BMI
     * BMI = 体重(kg) / 身高(m)²
     */
    fun calculateBMI(heightCm: Float, weightKg: Float): Float {
        require(heightCm > 0) { "Height must be positive" }
        require(weightKg > 0) { "Weight must be positive" }
        val heightM = heightCm / 100f
        return weightKg / (heightM * heightM)
    }
    
    /**
     * 获取 BMI 状态
     */
    fun getBMIStatus(bmi: Float): BMIStatus = when {
        bmi < 18.5f -> BMIStatus.UNDERWEIGHT
        bmi < 24f -> BMIStatus.NORMAL
        bmi < 28f -> BMIStatus.OVERWEIGHT
        else -> BMIStatus.OBESE
    }
    
    /**
     * 计算基础代谢率 (BMR) - Mifflin-St Jeor 公式
     * 
     * 男性: BMR = 10 × 体重(kg) + 6.25 × 身高(cm) - 5 × 年龄 + 5
     * 女性: BMR = 10 × 体重(kg) + 6.25 × 身高(cm) - 5 × 年龄 - 161
     */
    fun calculateBMR(
        gender: Gender,
        weightKg: Float,
        heightCm: Float,
        age: Int
    ): Int {
        val bmr = if (gender == Gender.MALE) {
            10 * weightKg + 6.25 * heightCm - 5 * age + 5
        } else {
            10 * weightKg + 6.25 * heightCm - 5 * age - 161
        }
        return bmr.toInt()
    }
    
    /**
     * 计算每日总能量消耗 (TDEE)
     * TDEE = BMR × 活动系数
     */
    fun calculateTDEE(bmr: Int, activityLevel: ActivityLevel): Int {
        return (bmr * activityLevel.factor).toInt()
    }
    
    /**
     * 计算每日热量目标
     * 
     * @return Pair<热量目标, 调整值> (调整值: 正数为盈余，负数为缺口)
     */
    fun calculateDailyCalories(
        tdee: Int,
        healthGoal: HealthGoal
    ): Pair<Int, Int> {
        return when (healthGoal) {
            HealthGoal.LOSE_WEIGHT -> {
                val deficit = 400  // 400 kcal 缺口
                Pair(tdee - deficit, -deficit)
            }
            HealthGoal.GAIN_MUSCLE -> {
                val surplus = 250  // 250 kcal 盈余
                Pair(tdee + surplus, surplus)
            }
            HealthGoal.MAINTAIN -> Pair(tdee, 0)
        }
    }
    
    /**
     * 计算宏量营养素目标
     * 
     * @return Triple<蛋白质(g), 碳水(g), 脂肪(g)>
     */
    fun calculateMacros(
        dailyCalories: Int,
        dietType: DietType
    ): Triple<Int, Int, Int> {
        val proteinCalories = dailyCalories * dietType.proteinRatio
        val carbsCalories = dailyCalories * dietType.carbsRatio
        val fatCalories = dailyCalories * dietType.fatRatio
        
        // 蛋白质和碳水: 4 kcal/g, 脂肪: 9 kcal/g
        val proteinGrams = (proteinCalories / 4).toInt()
        val carbsGrams = (carbsCalories / 4).toInt()
        val fatGrams = (fatCalories / 9).toInt()
        
        return Triple(proteinGrams, carbsGrams, fatGrams)
    }
    
    /**
     * 计算完整的营养目标
     */
    fun calculateNutritionGoals(
        gender: Gender,
        weightKg: Float,
        heightCm: Float,
        age: Int,
        activityLevel: ActivityLevel,
        healthGoal: HealthGoal,
        dietType: DietType
    ): NutritionGoals {
        val bmr = calculateBMR(gender, weightKg, heightCm, age)
        val tdee = calculateTDEE(bmr, activityLevel)
        val (dailyCalories, adjustment) = calculateDailyCalories(tdee, healthGoal)
        val (protein, carbs, fat) = calculateMacros(dailyCalories, dietType)
        
        return NutritionGoals(
            dailyCalories = dailyCalories,
            bmr = bmr,
            activityCalories = tdee - bmr,
            goalAdjustment = adjustment,
            proteinGrams = protein,
            carbsGrams = carbs,
            fatGrams = fat,
            proteinRatio = dietType.proteinRatio,
            carbsRatio = dietType.carbsRatio,
            fatRatio = dietType.fatRatio
        )
    }
    
    /**
     * 计算每周体重变化率
     */
    fun calculateWeeklyRate(
        currentWeight: Float,
        targetWeight: Float,
        targetDate: LocalDate
    ): Float {
        val weightDiff = abs(currentWeight - targetWeight)
        val daysUntilTarget = ChronoUnit.DAYS.between(LocalDate.now(), targetDate)
        val weeksUntilTarget = daysUntilTarget / 7f
        return if (weeksUntilTarget > 0) weightDiff / weeksUntilTarget else 0f
    }
    
    /**
     * 检查每周变化率是否安全
     * 
     * 减重: 每周最多 1kg
     * 增肌: 每周最多 0.5kg
     */
    fun isWeeklyRateSafe(weeklyRate: Float, healthGoal: HealthGoal): Boolean {
        return when (healthGoal) {
            HealthGoal.LOSE_WEIGHT -> weeklyRate <= 1.0f
            HealthGoal.GAIN_MUSCLE -> weeklyRate <= 0.5f
            HealthGoal.MAINTAIN -> true
        }
    }
    
    /**
     * 计算年龄
     */
    fun calculateAge(birthDate: LocalDate): Int {
        return Period.between(birthDate, LocalDate.now()).years
    }
    
    /**
     * 从字符串解析日期
     */
    fun parseDate(dateString: String): LocalDate? {
        return try {
            LocalDate.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 验证宏量比例总和
     * 
     * @return true 如果总和在 1.0 ± 1% 范围内
     */
    fun validateMacroRatios(protein: Float, carbs: Float, fat: Float): Boolean {
        val total = protein + carbs + fat
        return abs(total - 1.0f) < 0.01f
    }
    
    /**
     * 计算目标达成预计日期
     */
    fun estimateGoalDate(
        currentWeight: Float,
        targetWeight: Float,
        weeklyRate: Float = 0.5f  // 默认每周 0.5kg
    ): LocalDate {
        val weightDiff = abs(currentWeight - targetWeight)
        val weeksNeeded = (weightDiff / weeklyRate).toLong()
        return LocalDate.now().plusWeeks(weeksNeeded)
    }
}
