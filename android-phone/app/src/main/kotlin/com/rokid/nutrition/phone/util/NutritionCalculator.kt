package com.rokid.nutrition.phone.util

/**
 * 营养值数据类
 */
data class NutritionValues(
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

/**
 * 营养计算器
 * 
 * 用于按比例重新计算营养值
 */
object NutritionCalculator {
    
    /**
     * 按重量比例重新计算营养值
     * 
     * @param original 原始营养值
     * @param originalWeight 原始重量 (g)
     * @param newWeight 新重量 (g)
     * @return 按比例计算后的营养值
     */
    fun recalculateProportionally(
        original: NutritionValues,
        originalWeight: Double,
        newWeight: Double
    ): NutritionValues {
        // 处理边界情况
        if (originalWeight <= 0.0) {
            return original.copy()
        }
        
        if (newWeight <= 0.0) {
            return NutritionValues(0.0, 0.0, 0.0, 0.0)
        }
        
        val ratio = newWeight / originalWeight
        
        return NutritionValues(
            calories = (original.calories * ratio).coerceAtLeast(0.0),
            protein = (original.protein * ratio).coerceAtLeast(0.0),
            carbs = (original.carbs * ratio).coerceAtLeast(0.0),
            fat = (original.fat * ratio).coerceAtLeast(0.0)
        )
    }
    
    /**
     * 计算单个营养值的比例变化
     */
    fun recalculateSingleValue(
        originalValue: Double,
        originalWeight: Double,
        newWeight: Double
    ): Double {
        if (originalWeight <= 0.0) return originalValue
        if (newWeight <= 0.0) return 0.0
        
        val ratio = newWeight / originalWeight
        return (originalValue * ratio).coerceAtLeast(0.0)
    }
}
