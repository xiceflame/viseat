package com.rokid.nutrition.phone.util

/**
 * 食物项更新数据
 */
data class FoodItemUpdates(
    val foodName: String? = null,  // 食品名称（修改后需要重新查询营养信息）
    val weightG: Double? = null,
    val caloriesKcal: Double? = null,
    val proteinG: Double? = null,
    val carbsG: Double? = null,
    val fatG: Double? = null,
    val recalculateFromWeight: Boolean = false
)

/**
 * 验证结果
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: Map<String, String> = emptyMap()
) {
    companion object {
        fun success() = ValidationResult(true)
        fun failure(errors: Map<String, String>) = ValidationResult(false, errors)
    }
}

/**
 * 食物项验证器
 * 
 * 验证食物数据更新是否有效
 */
object FoodItemValidator {
    
    /**
     * 验证食物项更新
     * 
     * @param updates 要验证的更新数据
     * @return 验证结果，包含是否有效和错误信息
     */
    fun validate(updates: FoodItemUpdates): ValidationResult {
        val errors = mutableMapOf<String, String>()
        
        // 验证重量
        updates.weightG?.let { weight ->
            if (weight < 0) {
                errors["weightG"] = "重量不能为负数"
            }
        }
        
        // 验证热量
        updates.caloriesKcal?.let { calories ->
            if (calories < 0) {
                errors["caloriesKcal"] = "热量不能为负数"
            }
        }
        
        // 验证蛋白质
        updates.proteinG?.let { protein ->
            if (protein < 0) {
                errors["proteinG"] = "蛋白质不能为负数"
            }
        }
        
        // 验证碳水化合物
        updates.carbsG?.let { carbs ->
            if (carbs < 0) {
                errors["carbsG"] = "碳水化合物不能为负数"
            }
        }
        
        // 验证脂肪
        updates.fatG?.let { fat ->
            if (fat < 0) {
                errors["fatG"] = "脂肪不能为负数"
            }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.success()
        } else {
            ValidationResult.failure(errors)
        }
    }
    
    /**
     * 验证单个数值是否非负
     */
    fun isNonNegative(value: Double?): Boolean {
        return value == null || value >= 0
    }
}
