package com.rokid.nutrition.phone.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rokid.nutrition.phone.repository.DailyStats
import com.rokid.nutrition.phone.repository.StatisticsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 统计页面 ViewModel
 */
class StatsViewModel(
    private val statisticsRepository: StatisticsRepository
) : ViewModel() {
    
    private val _todayCalories = MutableStateFlow(0.0)
    val todayCalories: StateFlow<Double> = _todayCalories.asStateFlow()
    
    private val _weeklyStats = MutableStateFlow<List<DailyStats>>(emptyList())
    val weeklyStats: StateFlow<List<DailyStats>> = _weeklyStats.asStateFlow()
    
    init {
        loadStats()
    }
    
    fun loadStats() {
        viewModelScope.launch {
            _todayCalories.value = statisticsRepository.getTodayCalories()
            _weeklyStats.value = statisticsRepository.getWeeklyStats()
        }
    }
    
    /**
     * 刷新数据（食物数据变化后调用）
     */
    fun refreshData() {
        loadStats()
    }
}
