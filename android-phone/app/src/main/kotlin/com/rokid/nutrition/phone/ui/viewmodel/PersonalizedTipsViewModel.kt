package com.rokid.nutrition.phone.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rokid.nutrition.phone.network.model.PersonalizedTip
import com.rokid.nutrition.phone.repository.PersonalizedTipsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 个性化建议 UI 状态
 */
data class PersonalizedTipsUiState(
    val tips: List<PersonalizedTip> = emptyList(),
    val currentTip: PersonalizedTip? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastRefreshTime: Long = 0L
)

/**
 * 个性化建议 ViewModel
 * 
 * 管理 AI 健康洞察的 UI 状态和用户交互
 */
class PersonalizedTipsViewModel(
    private val repository: PersonalizedTipsRepository,
    private val getUserId: () -> String?
) : ViewModel() {
    
    companion object {
        private const val TAG = "PersonalizedTipsVM"
    }
    
    private val _uiState = MutableStateFlow(PersonalizedTipsUiState())
    val uiState: StateFlow<PersonalizedTipsUiState> = _uiState.asStateFlow()
    
    init {
        // 监听 repository 状态变化
        viewModelScope.launch {
            repository.tips.collect { tips ->
                _uiState.value = _uiState.value.copy(tips = tips)
            }
        }
        
        viewModelScope.launch {
            repository.currentTip.collect { tip ->
                _uiState.value = _uiState.value.copy(currentTip = tip)
            }
        }
        
        viewModelScope.launch {
            repository.isLoading.collect { loading ->
                _uiState.value = _uiState.value.copy(isLoading = loading)
            }
        }
    }
    
    /**
     * 加载个性化建议
     */
    fun loadTips(forceRefresh: Boolean = false) {
        val userId = getUserId()
        if (userId == null) {
            Log.w(TAG, "用户ID为空，无法加载建议")
            _uiState.value = _uiState.value.copy(error = "请先登录")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(error = null)
            
            val result = repository.fetchTips(userId, forceRefresh)
            
            result.onSuccess { response ->
                _uiState.value = _uiState.value.copy(
                    tips = response.tips,
                    lastRefreshTime = System.currentTimeMillis(),
                    error = null
                )
                Log.d(TAG, "加载建议成功: ${response.tips.size} 条")
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "加载失败"
                )
                Log.e(TAG, "加载建议失败", e)
            }
        }
    }
    
    /**
     * 刷新建议
     */
    fun refreshTips() {
        loadTips(forceRefresh = true)
    }
    
    /**
     * 用餐结束后刷新建议
     */
    fun refreshAfterMeal(sessionId: String) {
        val userId = getUserId() ?: return
        
        viewModelScope.launch {
            repository.refreshAfterMeal(userId, sessionId)
        }
    }
    
    /**
     * 每日刷新
     */
    fun dailyRefresh() {
        val userId = getUserId() ?: return
        
        viewModelScope.launch {
            repository.dailyRefresh(userId)
        }
    }
    
    /**
     * 提交反馈
     */
    fun submitFeedback(tip: PersonalizedTip, isHelpful: Boolean) {
        val userId = getUserId() ?: return
        
        viewModelScope.launch {
            val result = repository.submitFeedback(userId, tip.id, isHelpful)
            result.onSuccess {
                Log.d(TAG, "反馈已提交: ${tip.id}, isHelpful=$isHelpful")
            }.onFailure { e ->
                Log.e(TAG, "提交反馈失败", e)
            }
        }
    }
    
    /**
     * 忽略建议
     */
    fun ignoreTip(tip: PersonalizedTip) {
        val userId = getUserId() ?: return
        
        viewModelScope.launch {
            repository.ignoreTip(userId, tip.id)
        }
    }
    
    /**
     * 切换到下一条建议
     */
    fun nextTip() {
        repository.nextTip()
    }
    
    /**
     * 获取当前建议内容（用于眼镜端同步）
     */
    fun getCurrentTipContent(): String? {
        return repository.getCurrentTipContent()
    }
    
    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
