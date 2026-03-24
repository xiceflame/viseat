package com.rokid.nutrition.phone.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rokid.nutrition.phone.data.dao.WeightEntryDao
import com.rokid.nutrition.phone.data.entity.WeightEntryEntity
import com.rokid.nutrition.phone.repository.UserProfile
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 体重追踪 UI 状态
 */
data class WeightTrackingUiState(
    val entries: List<WeightEntryEntity> = emptyList(),
    val latestEntry: WeightEntryEntity? = null,
    val currentWeight: Float? = null,
    val targetWeight: Float? = null,
    val startWeight: Float? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * 体重追踪 ViewModel
 */
class WeightTrackingViewModel(
    private val weightEntryDao: WeightEntryDao,
    private val getUserProfile: suspend () -> UserProfile?
) : ViewModel() {
    
    companion object {
        private const val TAG = "WeightTrackingVM"
    }
    
    private val _uiState = MutableStateFlow(WeightTrackingUiState())
    val uiState: StateFlow<WeightTrackingUiState> = _uiState.asStateFlow()
    
    init {
        loadWeightData()
    }
    
    /**
     * 加载体重数据
     */
    fun loadWeightData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // 获取用户档案
                val profile = getUserProfile()
                val targetWeight = profile?.targetWeight
                val startWeight = profile?.weight
                
                // 收集体重记录
                weightEntryDao.getAllEntries().collect { entries ->
                    val latestEntry = entries.firstOrNull()
                    val currentWeight = latestEntry?.weight ?: profile?.weight
                    
                    _uiState.update {
                        it.copy(
                            entries = entries,
                            latestEntry = latestEntry,
                            currentWeight = currentWeight,
                            targetWeight = targetWeight,
                            startWeight = startWeight,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载体重数据失败", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "加载失败: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * 添加体重记录
     */
    fun addWeightEntry(weight: Float, note: String? = null) {
        viewModelScope.launch {
            try {
                val entry = WeightEntryEntity(
                    id = UUID.randomUUID().toString(),
                    weight = weight,
                    note = note,
                    recordedAt = System.currentTimeMillis(),
                    createdAt = System.currentTimeMillis()
                )
                
                weightEntryDao.insert(entry)
                Log.d(TAG, "体重记录已添加: $weight kg")
                
            } catch (e: Exception) {
                Log.e(TAG, "添加体重记录失败", e)
                _uiState.update {
                    it.copy(error = "添加失败: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 更新体重记录
     */
    fun updateWeightEntry(id: String, weight: Float, note: String?) {
        viewModelScope.launch {
            try {
                val existingEntry = _uiState.value.entries.find { it.id == id }
                if (existingEntry != null) {
                    val updatedEntry = existingEntry.copy(
                        weight = weight,
                        note = note
                    )
                    weightEntryDao.insert(updatedEntry)
                    Log.d(TAG, "体重记录已更新: $weight kg")
                }
            } catch (e: Exception) {
                Log.e(TAG, "更新体重记录失败", e)
                _uiState.update {
                    it.copy(error = "更新失败: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 删除体重记录
     */
    fun deleteWeightEntry(id: String) {
        viewModelScope.launch {
            try {
                weightEntryDao.delete(id)
                Log.d(TAG, "体重记录已删除: $id")
            } catch (e: Exception) {
                Log.e(TAG, "删除体重记录失败", e)
                _uiState.update {
                    it.copy(error = "删除失败: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
