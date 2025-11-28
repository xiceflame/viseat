package com.rokid.nutrition.phone.repository

import com.rokid.nutrition.phone.network.model.VisionAnalyzeResponse

/**
 * 识别历史记录
 */
data class RecognitionHistory(
    val timestamp: Long,
    val foodData: VisionAnalyzeResponse,
    val category: String,  // meal/snack/beverage/dessert/fruit
    val totalCalories: Double,
    val foodName: String
) {
    /**
     * 获取分类显示文本
     */
    fun getCategoryText(): String {
        return when (category.lowercase()) {
            "meal" -> "正餐"
            "snack" -> "零食"
            "beverage" -> "饮料"
            "dessert" -> "甜点"
            "fruit" -> "水果"
            else -> "其他"
        }
    }
    
    /**
     * 获取分类颜色（用于 UI 显示）
     */
    fun getCategoryColor(): Long {
        return when (category.lowercase()) {
            "meal" -> 0xFF4CAF50  // 绿色
            "snack" -> 0xFFFF9800  // 橙色
            "beverage" -> 0xFF2196F3  // 蓝色
            "dessert" -> 0xFFE91E63  // 粉色
            "fruit" -> 0xFF8BC34A  // 浅绿色
            else -> 0xFF9E9E9E  // 灰色
        }
    }
}
