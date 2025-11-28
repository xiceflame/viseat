package com.rokid.nutrition.phone.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.rokid.nutrition.phone.network.model.DailyContext
import com.rokid.nutrition.phone.network.model.NutritionTotal
import java.text.SimpleDateFormat
import java.util.*

/**
 * 今日营养追踪器
 * 
 * 负责追踪今日营养摄入，使用 SharedPreferences 存储
 * 支持日期切换自动重置
 */
class DailyNutritionTracker(context: Context) {
    
    companion object {
        private const val TAG = "DailyNutritionTracker"
        private const val PREFS_NAME = "daily_nutrition"
        
        // SharedPreferences keys
        private const val KEY_DAILY_CALORIES = "daily_calories"
        private const val KEY_DAILY_PROTEIN = "daily_protein"
        private const val KEY_DAILY_CARBS = "daily_carbs"
        private const val KEY_DAILY_FAT = "daily_fat"
        private const val KEY_MEAL_COUNT = "meal_count"
        private const val KEY_LAST_MEAL_TIME = "last_meal_time"
        private const val KEY_LAST_UPDATE_DATE = "last_update_date"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    /**
     * 更新今日营养总计
     * 
     * @param mealNutrition 本餐营养数据
     */
    fun updateDailyTotals(mealNutrition: NutritionTotal) {
        // 检查是否是新的一天
        if (isNewDay()) {
            Log.d(TAG, "检测到新的一天，重置今日统计")
            resetDailyTotals()
        }
        
        // 累加今日摄入
        val currentCalories = prefs.getFloat(KEY_DAILY_CALORIES, 0f)
        val currentProtein = prefs.getFloat(KEY_DAILY_PROTEIN, 0f)
        val currentCarbs = prefs.getFloat(KEY_DAILY_CARBS, 0f)
        val currentFat = prefs.getFloat(KEY_DAILY_FAT, 0f)
        val currentMealCount = prefs.getInt(KEY_MEAL_COUNT, 0)
        
        prefs.edit()
            .putFloat(KEY_DAILY_CALORIES, currentCalories + mealNutrition.calories.toFloat())
            .putFloat(KEY_DAILY_PROTEIN, currentProtein + mealNutrition.protein.toFloat())
            .putFloat(KEY_DAILY_CARBS, currentCarbs + mealNutrition.carbs.toFloat())
            .putFloat(KEY_DAILY_FAT, currentFat + mealNutrition.fat.toFloat())
            .putInt(KEY_MEAL_COUNT, currentMealCount + 1)
            .putLong(KEY_LAST_MEAL_TIME, System.currentTimeMillis())
            .putString(KEY_LAST_UPDATE_DATE, getCurrentDateString())
            .apply()
        
        Log.d(TAG, "更新今日统计: +${mealNutrition.calories} kcal, 总计: ${currentCalories + mealNutrition.calories.toFloat()} kcal")
    }

    
    /**
     * 获取今日上下文
     * 
     * @return DailyContext 今日营养摄入数据
     */
    fun getDailyContext(): DailyContext {
        // 检查是否是新的一天
        if (isNewDay()) {
            Log.d(TAG, "检测到新的一天，重置今日统计")
            resetDailyTotals()
        }
        
        val lastMealTime = prefs.getLong(KEY_LAST_MEAL_TIME, 0)
        val hoursAgo = if (lastMealTime > 0) {
            (System.currentTimeMillis() - lastMealTime) / 3600000.0
        } else {
            0.0
        }
        
        return DailyContext(
            totalCaloriesToday = prefs.getFloat(KEY_DAILY_CALORIES, 0f).toDouble(),
            totalProteinToday = prefs.getFloat(KEY_DAILY_PROTEIN, 0f).toDouble(),
            totalCarbsToday = prefs.getFloat(KEY_DAILY_CARBS, 0f).toDouble(),
            totalFatToday = prefs.getFloat(KEY_DAILY_FAT, 0f).toDouble(),
            mealCountToday = prefs.getInt(KEY_MEAL_COUNT, 0),
            lastMealHoursAgo = hoursAgo
        )
    }
    
    /**
     * 检查是否是新的一天
     */
    private fun isNewDay(): Boolean {
        val lastDate = prefs.getString(KEY_LAST_UPDATE_DATE, "")
        val currentDate = getCurrentDateString()
        return lastDate != currentDate
    }
    
    /**
     * 重置今日统计
     */
    private fun resetDailyTotals() {
        prefs.edit()
            .putFloat(KEY_DAILY_CALORIES, 0f)
            .putFloat(KEY_DAILY_PROTEIN, 0f)
            .putFloat(KEY_DAILY_CARBS, 0f)
            .putFloat(KEY_DAILY_FAT, 0f)
            .putInt(KEY_MEAL_COUNT, 0)
            .putString(KEY_LAST_UPDATE_DATE, getCurrentDateString())
            .apply()
        
        Log.d(TAG, "今日统计已重置")
    }
    
    /**
     * 获取当前日期字符串
     */
    private fun getCurrentDateString(): String {
        return dateFormat.format(Date())
    }
    
    /**
     * 获取今日总热量
     */
    fun getTodayCalories(): Double {
        if (isNewDay()) {
            resetDailyTotals()
        }
        return prefs.getFloat(KEY_DAILY_CALORIES, 0f).toDouble()
    }
    
    /**
     * 获取今日用餐次数
     */
    fun getTodayMealCount(): Int {
        if (isNewDay()) {
            resetDailyTotals()
        }
        return prefs.getInt(KEY_MEAL_COUNT, 0)
    }
    
    /**
     * 清除所有数据（用于测试或重置）
     */
    fun clearAll() {
        prefs.edit().clear().apply()
        Log.d(TAG, "所有数据已清除")
    }
    
    /**
     * 减去营养数据（删除用餐记录时使用）
     * 
     * @param calories 热量
     * @param protein 蛋白质
     * @param carbs 碳水化合物
     * @param fat 脂肪
     * @param mealTimestamp 被删除用餐的时间戳（用于判断是否是今天的记录）
     * @param decreaseMealCount 是否减少用餐次数（删除整个用餐记录时为 true，删除单个食物时为 false）
     */
    fun subtractNutrition(
        calories: Double, 
        protein: Double, 
        carbs: Double, 
        fat: Double,
        mealTimestamp: Long = System.currentTimeMillis(),
        decreaseMealCount: Boolean = false
    ) {
        // 检查是否是新的一天
        if (isNewDay()) {
            resetDailyTotals()
        }
        
        // 检查被删除的用餐是否是今天的记录
        val mealDate = dateFormat.format(Date(mealTimestamp))
        val todayDate = getCurrentDateString()
        
        if (mealDate != todayDate) {
            Log.d(TAG, "删除的是历史记录 ($mealDate)，不影响今日统计 ($todayDate)")
            return  // 不是今天的记录，不需要减
        }
        
        val currentCalories = prefs.getFloat(KEY_DAILY_CALORIES, 0f)
        val currentProtein = prefs.getFloat(KEY_DAILY_PROTEIN, 0f)
        val currentCarbs = prefs.getFloat(KEY_DAILY_CARBS, 0f)
        val currentFat = prefs.getFloat(KEY_DAILY_FAT, 0f)
        val currentMealCount = prefs.getInt(KEY_MEAL_COUNT, 0)
        
        val newCalories = maxOf(0f, currentCalories - calories.toFloat())
        val newProtein = maxOf(0f, currentProtein - protein.toFloat())
        val newCarbs = maxOf(0f, currentCarbs - carbs.toFloat())
        val newFat = maxOf(0f, currentFat - fat.toFloat())
        val newMealCount = if (decreaseMealCount) maxOf(0, currentMealCount - 1) else currentMealCount
        
        prefs.edit()
            .putFloat(KEY_DAILY_CALORIES, newCalories)
            .putFloat(KEY_DAILY_PROTEIN, newProtein)
            .putFloat(KEY_DAILY_CARBS, newCarbs)
            .putFloat(KEY_DAILY_FAT, newFat)
            .putInt(KEY_MEAL_COUNT, newMealCount)
            .apply()
        
        Log.d(TAG, "减去营养: -$calories kcal, 剩余: $newCalories kcal, 减少用餐次数: $decreaseMealCount")
    }
}
