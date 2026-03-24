package com.rokid.nutrition.phone.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rokid.nutrition.phone.repository.DailyNutritionTracker
import com.rokid.nutrition.phone.repository.DailyStats
import com.rokid.nutrition.phone.repository.StatisticsRepository
import com.rokid.nutrition.phone.repository.TodayNutritionData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 统计页面 ViewModel
 */
class StatsViewModel(
    private val statisticsRepository: StatisticsRepository,
    private val dailyNutritionTracker: DailyNutritionTracker? = null
) : ViewModel() {
    
    companion object {
        private const val TAG = "StatsViewModel"
    }
    
    private val _todayCalories = MutableStateFlow(0.0)
    val todayCalories: StateFlow<Double> = _todayCalories.asStateFlow()
    
    private val _weeklyStats = MutableStateFlow<List<DailyStats>>(emptyList())
    val weeklyStats: StateFlow<List<DailyStats>> = _weeklyStats.asStateFlow()
    
    // 今日营养数据（从数据库获取）
    private val _todayNutritionData = MutableStateFlow(TodayNutritionData())
    val todayNutritionData: StateFlow<TodayNutritionData> = _todayNutritionData.asStateFlow()
    
    init {
        loadStats()
    }
    
    fun loadStats() {
        viewModelScope.launch {
            // 优先使用 DailyNutritionTracker 的数据（实时更新）
            val calories = dailyNutritionTracker?.getTodayCalories() 
                ?: statisticsRepository.getTodayCalories()
            _todayCalories.value = calories
            _weeklyStats.value = statisticsRepository.getWeeklyStats()
            
            // 加载今日营养数据（从数据库）
            val nutritionData = statisticsRepository.getTodayNutritionData()
            _todayNutritionData.value = nutritionData
            
            Log.d(TAG, "加载统计数据: todayCalories=$calories, protein=${nutritionData.totalProtein}, carbs=${nutritionData.totalCarbs}, fat=${nutritionData.totalFat}")
        }
    }
    
    /**
     * 刷新数据（食物数据变化后调用）
     */
    fun refreshData() {
        Log.d(TAG, "刷新统计数据")
        loadStats()
    }
}
