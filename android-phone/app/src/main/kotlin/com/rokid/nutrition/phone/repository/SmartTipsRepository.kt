package com.rokid.nutrition.phone.repository

import android.util.Log
import com.rokid.nutrition.phone.network.NetworkManager
import com.rokid.nutrition.phone.network.model.PersonalizedTip
import com.rokid.nutrition.phone.network.model.PersonalizedTipsResponse
import com.rokid.nutrition.phone.network.model.DataSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 智能健康提示数据仓库
 * 
 * 封装个性化建议 API 调用，管理提示缓存，提供 Flow 数据流
 */
class SmartTipsRepository(
    private val networkManager: NetworkManager
) {
    companion object {
        private const val TAG = "SmartTipsRepository"
        
        // 缓存有效期：30分钟
        private const val CACHE_VALIDITY_MS = 30 * 60 * 1000L
    }
    
    // 提示列表
    private val _tips = MutableStateFlow<List<PersonalizedTip>>(emptyList())
    val tips: StateFlow<List<PersonalizedTip>> = _tips.asStateFlow()
    
    // 数据摘要
    private val _dataSummary = MutableStateFlow<DataSummary?>(null)
    val dataSummary: StateFlow<DataSummary?> = _dataSummary.asStateFlow()
    
    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // 错误信息
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // 缓存时间戳
    private var lastFetchTime: Long = 0L
    
    /**
     * 获取个性化提示
     * 
     * @param userId 用户ID
     * @param forceRefresh 是否强制刷新（忽略缓存）
     * @return 提示列表
     */
    suspend fun fetchTips(userId: String, forceRefresh: Boolean = false): Result<PersonalizedTipsResponse> {
        // 检查缓存是否有效
        if (!forceRefresh && isCacheValid()) {
            Log.d(TAG, "使用缓存的提示数据")
            return Result.success(PersonalizedTipsResponse(
                tips = _tips.value,
                generatedAt = null,
                dataSummary = _dataSummary.value
            ))
        }
        
        _isLoading.value = true
        _error.value = null
        
        val result = networkManager.getPersonalizedTips(userId)
        
        result.fold(
            onSuccess = { response ->
                // 按优先级排序（优先级数字越小越重要）
                val sortedTips = response.tips.sortedBy { it.priority }
                _tips.value = sortedTips
                _dataSummary.value = response.dataSummary
                lastFetchTime = System.currentTimeMillis()
                Log.d(TAG, "获取提示成功: ${sortedTips.size} 条")
            },
            onFailure = { e ->
                _error.value = e.message ?: "获取提示失败"
                Log.e(TAG, "获取提示失败", e)
            }
        )
        
        _isLoading.value = false
        return result
    }
    
    /**
     * 刷新个性化提示
     * 
     * @param userId 用户ID
     * @param trigger 触发类型
     * @param mealSessionId 用餐会话ID
     */
    suspend fun refreshTips(
        userId: String,
        trigger: String = "manual",
        mealSessionId: String? = null
    ): Result<List<PersonalizedTip>> {
        _isLoading.value = true
        _error.value = null
        
        val result = networkManager.refreshPersonalizedTips(userId, trigger, mealSessionId)
        
        result.fold(
            onSuccess = { response ->
                // 如果返回了新的提示列表，更新缓存
                response.tips?.let { newTips ->
                    val sortedTips = newTips.sortedBy { it.priority }
                    _tips.value = sortedTips
                    lastFetchTime = System.currentTimeMillis()
                    Log.d(TAG, "刷新提示成功: ${sortedTips.size} 条")
                }
            },
            onFailure = { e ->
                _error.value = e.message ?: "刷新提示失败"
                Log.e(TAG, "刷新提示失败", e)
            }
        )
        
        _isLoading.value = false
        return result.map { it.tips ?: _tips.value }
    }
    
    /**
     * 用餐结束后刷新提示
     */
    suspend fun refreshAfterMeal(userId: String, sessionId: String) {
        refreshTips(userId, "meal_ended", sessionId)
    }
    
    /**
     * 每日刷新
     */
    suspend fun dailyRefresh(userId: String) {
        refreshTips(userId, "daily_refresh")
    }
    
    /**
     * 检查缓存是否有效
     */
    private fun isCacheValid(): Boolean {
        if (_tips.value.isEmpty()) return false
        val elapsed = System.currentTimeMillis() - lastFetchTime
        return elapsed < CACHE_VALIDITY_MS
    }
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        _tips.value = emptyList()
        _dataSummary.value = null
        lastFetchTime = 0L
        _error.value = null
    }
    
    /**
     * 清除错误
     */
    fun clearError() {
        _error.value = null
    }
}
