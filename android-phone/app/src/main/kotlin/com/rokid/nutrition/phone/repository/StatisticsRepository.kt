package com.rokid.nutrition.phone.repository

import com.rokid.nutrition.phone.data.dao.MealSessionDao
import com.rokid.nutrition.phone.data.dao.MealSnapshotDao
import com.rokid.nutrition.phone.data.dao.SnapshotFoodDao
import com.rokid.nutrition.phone.data.entity.MealSessionEntity
import com.rokid.nutrition.phone.data.entity.SnapshotFoodEntity
import java.util.*

/**
 * 今日营养统计数据
 */
data class TodayNutritionData(
    val totalCalories: Double = 0.0,
    val totalProtein: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val totalFat: Double = 0.0,
    val mealCount: Int = 0,
    val snackCount: Int = 0,
    val beverageCount: Int = 0,
    val dessertCount: Int = 0,
    val fruitCount: Int = 0,
    val foods: List<SnapshotFoodEntity> = emptyList()
)

/**
 * 统计数据仓库
 */
class StatisticsRepository(
    private val sessionDao: MealSessionDao,
    private val snapshotDao: MealSnapshotDao? = null,
    private val foodDao: SnapshotFoodDao? = null
) {
    /**
     * 获取今日热量摄入
     */
    suspend fun getTodayCalories(): Double {
        val (start, end) = getTodayRange()
        return sessionDao.getDailyCalories(start, end)
    }
    
    /**
     * 获取指定日期的热量摄入
     */
    suspend fun getDayCalories(date: Date): Double {
        val (start, end) = getDayRange(date)
        return sessionDao.getDailyCalories(start, end)
    }
    
    /**
     * 获取每周统计数据
     */
    suspend fun getWeeklyStats(): List<DailyStats> {
        val calendar = Calendar.getInstance()
        val result = mutableListOf<DailyStats>()
        
        // 获取过去7天的数据
        repeat(7) { daysAgo ->
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
            
            val (start, end) = getDayRange(calendar.time)
            val calories = sessionDao.getDailyCalories(start, end)
            val sessions = sessionDao.getSessionsInRange(start, end)
            
            result.add(DailyStats(
                date = calendar.time,
                totalCalories = calories,
                mealCount = sessions.size,
                sessions = sessions
            ))
        }
        
        return result.reversed()  // 按时间正序
    }
    
    /**
     * 获取历史记录
     */
    suspend fun getHistory(limit: Int = 50, offset: Int = 0): List<MealSessionEntity> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -30)  // 过去30天
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()
        
        return sessionDao.getSessionsInRange(startTime, endTime)
    }
    
    private fun getTodayRange(): Pair<Long, Long> {
        return getDayRange(Date())
    }
    
    private fun getDayRange(date: Date): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = calendar.timeInMillis
        
        return startOfDay to endOfDay
    }
    
    /**
     * 获取今日营养数据（从数据库）
     * 
     * 包括：总热量、蛋白质、碳水、脂肪、各类食品数量
     */
    suspend fun getTodayNutritionData(): TodayNutritionData {
        if (foodDao == null || snapshotDao == null) {
            return TodayNutritionData()
        }
        
        val (start, end) = getTodayRange()
        val foods = foodDao.getFoodsInTimeRange(start, end)
        
        // 计算营养总量
        var totalCalories = 0.0
        var totalProtein = 0.0
        var totalCarbs = 0.0
        var totalFat = 0.0
        
        // 按每个食物的 category 统计
        var mealCount = 0
        var snackCount = 0
        var beverageCount = 0
        var dessertCount = 0
        var fruitCount = 0
        
        foods.forEach { food ->
            totalCalories += food.caloriesKcal
            totalProtein += food.proteinG ?: 0.0
            totalCarbs += food.carbsG ?: 0.0
            totalFat += food.fatG ?: 0.0
            
            // 按食物的 category 分类统计
            when (food.category?.lowercase() ?: "meal") {
                "meal" -> mealCount++
                "snack" -> snackCount++
                "beverage" -> beverageCount++
                "dessert" -> dessertCount++
                "fruit" -> fruitCount++
                else -> mealCount++  // 默认为正餐
            }
        }
        
        return TodayNutritionData(
            totalCalories = totalCalories,
            totalProtein = totalProtein,
            totalCarbs = totalCarbs,
            totalFat = totalFat,
            mealCount = mealCount,
            snackCount = snackCount,
            beverageCount = beverageCount,
            dessertCount = dessertCount,
            fruitCount = fruitCount,
            foods = foods
        )
    }
}

/**
 * 每日统计数据
 */
data class DailyStats(
    val date: Date,
    val totalCalories: Double,
    val mealCount: Int,
    val sessions: List<MealSessionEntity>
)
