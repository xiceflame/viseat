package com.rokid.nutrition.phone.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.gson.Gson
import com.rokid.nutrition.phone.data.dao.MealSnapshotDao
import com.rokid.nutrition.phone.data.dao.SnapshotFoodDao
import com.rokid.nutrition.phone.data.dao.SyncQueueDao
import com.rokid.nutrition.phone.data.entity.SyncQueueEntity
import com.rokid.nutrition.phone.network.ApiService
import com.rokid.nutrition.phone.network.model.UpdateFoodRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/**
 * 同步状态
 */
enum class SyncStatus {
    Synced,     // 已同步
    Pending,    // 待同步
    Syncing,    // 同步中
    Failed      // 同步失败
}

/**
 * 同步管理器
 * 
 * 管理数据同步逻辑，包括离线队列和重试机制
 */
class SyncManager(
    private val context: Context,
    private val syncQueueDao: SyncQueueDao,
    private val snapshotDao: MealSnapshotDao,
    private val foodDao: SnapshotFoodDao,
    private val apiService: ApiService
) {
    companion object {
        private const val TAG = "SyncManager"
        private const val MAX_RETRY_COUNT = 3
        private val RETRY_DELAYS = listOf(1000L, 5000L, 15000L) // 指数退避
    }
    
    private val gson = Gson()
    private val _syncStatus = MutableStateFlow(SyncStatus.Synced)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus
    
    /**
     * 添加同步操作到队列
     */
    suspend fun enqueueSyncOperation(operation: SyncQueueEntity) {
        syncQueueDao.insert(operation)
        _syncStatus.value = SyncStatus.Pending
        Log.d(TAG, "已添加同步操作: ${operation.operationType} - ${operation.targetId}")
    }
    
    /**
     * 处理所有待同步操作
     * 
     * @return 成功同步的操作数量
     */
    suspend fun processPendingOperations(): Int {
        if (!isNetworkAvailable()) {
            Log.d(TAG, "网络不可用，跳过同步")
            return 0
        }
        
        _syncStatus.value = SyncStatus.Syncing
        var successCount = 0
        
        try {
            val pendingOperations = syncQueueDao.getPendingOperations()
            Log.d(TAG, "待同步操作数量: ${pendingOperations.size}")
            
            for (operation in pendingOperations) {
                val success = processOperation(operation)
                if (success) {
                    successCount++
                    syncQueueDao.delete(operation.id)
                }
            }
            
            // 更新状态
            val remainingCount = syncQueueDao.getPendingCount()
            _syncStatus.value = if (remainingCount > 0) SyncStatus.Pending else SyncStatus.Synced
            
        } catch (e: Exception) {
            Log.e(TAG, "同步过程出错", e)
            _syncStatus.value = SyncStatus.Failed
        }
        
        return successCount
    }
    
    /**
     * 处理单个同步操作
     */
    private suspend fun processOperation(operation: SyncQueueEntity): Boolean {
        return try {
            when (operation.operationType) {
                "update_food" -> processFoodUpdate(operation)
                else -> {
                    Log.w(TAG, "未知操作类型: ${operation.operationType}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理操作失败: ${operation.id}", e)
            handleOperationFailure(operation, e.message ?: "Unknown error")
            false
        }
    }
    
    /**
     * 处理食物更新同步
     */
    private suspend fun processFoodUpdate(operation: SyncQueueEntity): Boolean {
        val payload = gson.fromJson(operation.payload, Map::class.java)
        
        val request = UpdateFoodRequest(
            foodId = payload["food_id"] as String,
            weightG = (payload["weight_g"] as? Number)?.toDouble(),
            caloriesKcal = (payload["calories_kcal"] as? Number)?.toDouble(),
            proteinG = (payload["protein_g"] as? Number)?.toDouble(),
            carbsG = (payload["carbs_g"] as? Number)?.toDouble(),
            fatG = (payload["fat_g"] as? Number)?.toDouble(),
            editedAt = (payload["edited_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
        
        // 调用 API
        val response = apiService.updateFood(request)
        
        if (response.success) {
            // 更新本地快照的同步时间
            updateLocalSyncStatus(operation.targetId)
            Log.d(TAG, "食物更新同步成功: ${operation.targetId}")
            return true
        } else {
            Log.w(TAG, "食物更新同步失败: ${response.message}")
            return false
        }
    }
    
    /**
     * 更新本地同步状态
     */
    private suspend fun updateLocalSyncStatus(foodId: String) {
        val food = foodDao.getFoodById(foodId) ?: return
        val snapshots = snapshotDao.getSnapshotsForSession(food.snapshotId)
        val snapshot = snapshots.firstOrNull { it.id == food.snapshotId } ?: return
        
        val updatedSnapshot = snapshot.copy(
            lastSyncedAt = System.currentTimeMillis()
        )
        snapshotDao.saveSnapshot(updatedSnapshot)
    }
    
    /**
     * 处理操作失败
     */
    private suspend fun handleOperationFailure(operation: SyncQueueEntity, error: String) {
        if (operation.retryCount >= MAX_RETRY_COUNT) {
            syncQueueDao.markFailed(operation.id, error)
            Log.w(TAG, "操作已达到最大重试次数: ${operation.id}")
        } else {
            syncQueueDao.markFailed(operation.id, error)
            // 重置状态为 pending 以便下次重试
            syncQueueDao.updateStatus(operation.id, "pending")
        }
    }
    
    /**
     * 观察同步状态
     */
    fun observeSyncStatus(): Flow<SyncStatus> = syncStatus
    
    /**
     * 观察待同步数量
     */
    fun observePendingCount(): Flow<Int> = syncQueueDao.getPendingCountFlow()
    
    /**
     * 检查网络是否可用
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    /**
     * 带重试的同步操作
     */
    suspend fun syncWithRetry(operation: SyncQueueEntity): Boolean {
        var currentRetry = 0
        
        while (currentRetry < MAX_RETRY_COUNT) {
            if (processOperation(operation)) {
                return true
            }
            
            val delayMs = RETRY_DELAYS.getOrElse(currentRetry) { RETRY_DELAYS.last() }
            Log.d(TAG, "同步失败，${delayMs}ms 后重试 (${currentRetry + 1}/$MAX_RETRY_COUNT)")
            delay(delayMs)
            currentRetry++
        }
        
        return false
    }
}

/**
 * 冲突解决器
 * 
 * 基于时间戳解决本地和云端数据冲突
 */
object ConflictResolver {
    
    /**
     * 解决数据冲突
     * 
     * @param localEditedAt 本地编辑时间
     * @param remoteEditedAt 云端编辑时间
     * @return true 表示使用本地数据，false 表示使用云端数据
     */
    fun shouldUseLocal(localEditedAt: Long?, remoteEditedAt: Long?): Boolean {
        // 如果本地没有编辑时间，使用云端
        if (localEditedAt == null) return false
        // 如果云端没有编辑时间，使用本地
        if (remoteEditedAt == null) return true
        // 使用更新的版本
        return localEditedAt >= remoteEditedAt
    }
}
