package com.rokid.nutrition.phone.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.rokid.nutrition.phone.network.ApiService
import com.rokid.nutrition.phone.network.model.PersonalizedTip
import com.rokid.nutrition.phone.network.model.PersonalizedTipsResponse
import com.rokid.nutrition.phone.network.model.RefreshTipsRequest
import com.rokid.nutrition.phone.network.model.TipFeedbackRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 个性化建议管理器
 * 
 * 功能：
 * 1. 从后端获取个性化建议
 * 2. 本地缓存建议（离线可用）
 * 3. 用餐结束后刷新建议
 * 4. 同步建议到眼镜端
 */
class PersonalizedTipsRepository(
    private val context: Context,
    private val apiService: ApiService
) {
    companion object {
        private const val TAG = "PersonalizedTips"
        private const val PREFS_NAME = "personalized_tips"
        private const val KEY_TIPS_JSON = "tips_json"
        private const val KEY_LAST_FETCH_TIME = "last_fetch_time"
        private const val KEY_CURRENT_TIP_INDEX = "current_tip_index"
        
        // 建议有效期：24小时
        private const val TIPS_VALIDITY_MS = 24 * 60 * 60 * 1000L
    }
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    private val gson = Gson()
    
    // 当前建议列表
    private val _tips = MutableStateFlow<List<PersonalizedTip>>(emptyList())
    val tips: StateFlow<List<PersonalizedTip>> = _tips.asStateFlow()
    
    // 当前显示的建议（用于眼镜端同步）
    private val _currentTip = MutableStateFlow<PersonalizedTip?>(null)
    val currentTip: StateFlow<PersonalizedTip?> = _currentTip.asStateFlow()
    
    // 是否正在加载
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        // 启动时加载缓存的建议
        loadCachedTips()
    }
    
    /**
     * 获取当前建议文本（用于眼镜端同步）
     */
    fun getCurrentTipContent(): String? {
        return _currentTip.value?.content
    }
    
    /**
     * 获取优先级最高的建议
     */
    fun getTopPriorityTip(): PersonalizedTip? {
        return _tips.value.minByOrNull { it.priority }
    }
    
    /**
     * 切换到下一条建议
     */
    fun nextTip() {
        val tipsList = _tips.value
        if (tipsList.isEmpty()) return
        
        val currentIndex = prefs.getInt(KEY_CURRENT_TIP_INDEX, 0)
        val nextIndex = (currentIndex + 1) % tipsList.size
        
        prefs.edit().putInt(KEY_CURRENT_TIP_INDEX, nextIndex).apply()
        _currentTip.value = tipsList[nextIndex]
        
        Log.d(TAG, "切换到建议 #$nextIndex: ${_currentTip.value?.content}")
    }
    
    /**
     * 从后端获取个性化建议
     * 
     * @param userId 用户ID
     * @param forceRefresh 是否强制刷新（忽略缓存有效期）
     */
    suspend fun fetchTips(userId: String, forceRefresh: Boolean = false): Result<PersonalizedTipsResponse> {
        // 检查缓存是否有效
        if (!forceRefresh && isCacheValid()) {
            Log.d(TAG, "使用缓存的建议")
            return Result.success(PersonalizedTipsResponse(
                tips = _tips.value,
                generatedAt = null,
                dataSummary = null
            ))
        }
        
        _isLoading.value = true
        
        return try {
            Log.d(TAG, "从后端获取个性化建议: userId=$userId")
            val response = apiService.getPersonalizedTips(userId)
            
            // 更新状态
            _tips.value = response.tips
            if (response.tips.isNotEmpty()) {
                _currentTip.value = response.tips.minByOrNull { it.priority }
            }
            
            // 缓存到本地
            saveTipsToCache(response.tips)
            
            Log.d(TAG, "获取到 ${response.tips.size} 条建议")
            response.tips.forEach { tip ->
                Log.d(TAG, "  [${tip.category}] ${tip.content}")
            }
            
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "获取个性化建议失败", e)
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * 用餐结束后刷新建议
     * 
     * @param userId 用户ID
     * @param sessionId 用餐会话ID
     */
    suspend fun refreshAfterMeal(userId: String, sessionId: String): Result<List<PersonalizedTip>> {
        return try {
            Log.d(TAG, "用餐结束，刷新建议: sessionId=$sessionId")
            
            val request = RefreshTipsRequest(
                trigger = "meal_ended",
                mealSessionId = sessionId
            )
            
            val response = apiService.refreshPersonalizedTips(userId, request)
            
            if (response.status == "completed" && response.tips != null) {
                // 直接返回了新建议
                _tips.value = response.tips
                if (response.tips.isNotEmpty()) {
                    _currentTip.value = response.tips.minByOrNull { it.priority }
                }
                saveTipsToCache(response.tips)
                
                Log.d(TAG, "建议已刷新: ${response.tips.size} 条")
                Result.success(response.tips)
            } else {
                // 建议正在生成中，稍后重新获取
                Log.d(TAG, "建议正在生成中，预计 ${response.estimatedTime} 秒")
                Result.success(_tips.value)
            }
        } catch (e: Exception) {
            Log.e(TAG, "刷新建议失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 每日刷新建议
     */
    suspend fun dailyRefresh(userId: String): Result<List<PersonalizedTip>> {
        return try {
            Log.d(TAG, "每日刷新建议")
            
            val request = RefreshTipsRequest(trigger = "daily_refresh")
            val response = apiService.refreshPersonalizedTips(userId, request)
            
            if (response.tips != null) {
                _tips.value = response.tips
                if (response.tips.isNotEmpty()) {
                    _currentTip.value = response.tips.minByOrNull { it.priority }
                }
                saveTipsToCache(response.tips)
            }
            
            Result.success(_tips.value)
        } catch (e: Exception) {
            Log.e(TAG, "每日刷新失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 提交建议反馈
     */
    suspend fun submitFeedback(userId: String, tipId: String, isHelpful: Boolean): Result<Boolean> {
        return try {
            val request = TipFeedbackRequest(isHelpful = isHelpful)
            val response = apiService.submitTipFeedback(userId, tipId, request)
            
            Log.d(TAG, "反馈已提交: tipId=$tipId, isHelpful=$isHelpful")
            Result.success(response.success)
        } catch (e: Exception) {
            Log.e(TAG, "提交反馈失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 忽略某条建议
     */
    suspend fun ignoreTip(userId: String, tipId: String): Result<Boolean> {
        return try {
            val response = apiService.ignoreTip(userId, tipId)
            
            // 从本地列表中移除
            _tips.value = _tips.value.filter { it.id != tipId }
            
            // 如果当前显示的是被忽略的建议，切换到下一条
            if (_currentTip.value?.id == tipId) {
                nextTip()
            }
            
            Log.d(TAG, "建议已忽略: tipId=$tipId")
            Result.success(response.success)
        } catch (e: Exception) {
            Log.e(TAG, "忽略建议失败", e)
            Result.failure(e)
        }
    }
    
    // ==================== 缓存管理 ====================
    
    private fun loadCachedTips() {
        try {
            val json = prefs.getString(KEY_TIPS_JSON, null) ?: return
            val tips = gson.fromJson(json, Array<PersonalizedTip>::class.java).toList()
            
            _tips.value = tips
            if (tips.isNotEmpty()) {
                val index = prefs.getInt(KEY_CURRENT_TIP_INDEX, 0).coerceIn(0, tips.size - 1)
                _currentTip.value = tips[index]
            }
            
            Log.d(TAG, "从缓存加载 ${tips.size} 条建议")
        } catch (e: Exception) {
            Log.e(TAG, "加载缓存失败", e)
        }
    }
    
    private fun saveTipsToCache(tips: List<PersonalizedTip>) {
        try {
            val json = gson.toJson(tips)
            prefs.edit()
                .putString(KEY_TIPS_JSON, json)
                .putLong(KEY_LAST_FETCH_TIME, System.currentTimeMillis())
                .putInt(KEY_CURRENT_TIP_INDEX, 0)
                .apply()
            
            Log.d(TAG, "建议已缓存")
        } catch (e: Exception) {
            Log.e(TAG, "缓存建议失败", e)
        }
    }
    
    private fun isCacheValid(): Boolean {
        val lastFetchTime = prefs.getLong(KEY_LAST_FETCH_TIME, 0)
        val isValid = System.currentTimeMillis() - lastFetchTime < TIPS_VALIDITY_MS
        Log.d(TAG, "缓存有效性: $isValid (上次获取: ${System.currentTimeMillis() - lastFetchTime}ms ago)")
        return isValid && _tips.value.isNotEmpty()
    }
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        prefs.edit().clear().apply()
        _tips.value = emptyList()
        _currentTip.value = null
        Log.d(TAG, "缓存已清除")
    }
}
