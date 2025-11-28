package com.rokid.nutrition.phone.repository

import com.rokid.nutrition.phone.data.dao.MealSessionDao
import com.rokid.nutrition.phone.data.entity.MealSessionEntity
import java.util.*

/**
 * 统计数据仓库
 */
class StatisticsRepository(
    private val sessionDao: MealSessionDao
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
