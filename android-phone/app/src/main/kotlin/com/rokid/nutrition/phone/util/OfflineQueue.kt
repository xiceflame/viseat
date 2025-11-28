package com.rokid.nutrition.phone.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 离线操作队列
 * 
 * 在网络不可用时缓存操作，网络恢复后按 FIFO 顺序重放
 */
class OfflineQueue(context: Context) {
    
    companion object {
        private const val TAG = "OfflineQueue"
    }
    
    private val queue = ConcurrentLinkedQueue<QueuedOperation>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
    
    private val connectivityManager = 
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    init {
        registerNetworkCallback()
        checkInitialNetworkState()
    }
    
    private fun registerNetworkCallback() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "网络已连接")
                _isOnline.value = true
                replayQueue()
            }
            
            override fun onLost(network: Network) {
                Log.d(TAG, "网络已断开")
                _isOnline.value = false
            }
        })
    }
    
    private fun checkInitialNetworkState() {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        _isOnline.value = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
    
    /**
     * 添加操作到队列
     */
    fun enqueue(operation: QueuedOperation) {
        if (_isOnline.value) {
            // 在线时直接执行
            scope.launch {
                try {
                    operation.execute()
                } catch (e: Exception) {
                    Log.e(TAG, "操作执行失败，加入队列", e)
                    queue.offer(operation)
                }
            }
        } else {
            // 离线时加入队列
            Log.d(TAG, "离线模式，操作已加入队列: ${operation.id}")
            queue.offer(operation)
        }
    }
    
    /**
     * 重放队列中的操作
     */
    private fun replayQueue() {
        scope.launch {
            Log.d(TAG, "开始重放队列，共 ${queue.size} 个操作")
            
            while (queue.isNotEmpty() && _isOnline.value) {
                val operation = queue.poll() ?: break
                
                try {
                    Log.d(TAG, "执行操作: ${operation.id}")
                    operation.execute()
                } catch (e: Exception) {
                    Log.e(TAG, "操作执行失败: ${operation.id}", e)
                    // 失败的操作重新加入队列末尾
                    if (operation.retryCount < 3) {
                        operation.retryCount++
                        queue.offer(operation)
                    }
                }
                
                // 操作间隔
                delay(500)
            }
        }
    }
    
    /**
     * 获取队列大小
     */
    fun queueSize(): Int = queue.size
    
    /**
     * 清空队列
     */
    fun clear() {
        queue.clear()
    }
    
    fun release() {
        scope.cancel()
    }
}

/**
 * 队列中的操作
 */
data class QueuedOperation(
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    var retryCount: Int = 0,
    val execute: suspend () -> Unit
)
