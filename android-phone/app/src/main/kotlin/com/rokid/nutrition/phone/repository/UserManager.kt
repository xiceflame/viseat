package com.rokid.nutrition.phone.repository

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.util.Log
import com.rokid.nutrition.phone.Config
import com.rokid.nutrition.phone.network.NetworkManager
import com.rokid.nutrition.phone.network.model.UserRegisterResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * 用户管理器
 * 
 * 负责管理用户身份信息：
 * - device_id: 设备唯一标识
 * - user_id: 用户ID（后端返回）
 * - token: 认证令牌
 * - session_id: 当前用餐会话ID
 */
class UserManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "UserManager"
        private const val PREFS_NAME = "user_prefs"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_CURRENT_SESSION_ID = "current_session_id"
        private const val KEY_IS_REGISTERED = "is_registered"
        
        @Volatile
        private var instance: UserManager? = null
        
        fun getInstance(context: Context): UserManager {
            return instance ?: synchronized(this) {
                instance ?: UserManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // 状态流
    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId.asStateFlow()
    
    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()
    
    private val _isRegistered = MutableStateFlow(false)
    val isRegistered: StateFlow<Boolean> = _isRegistered.asStateFlow()
    
    init {
        // 从本地存储恢复状态
        _userId.value = prefs.getString(KEY_USER_ID, null)
        _currentSessionId.value = prefs.getString(KEY_CURRENT_SESSION_ID, null)
        _isRegistered.value = prefs.getBoolean(KEY_IS_REGISTERED, false)
        
        Log.d(TAG, "初始化 UserManager: userId=${_userId.value}, sessionId=${_currentSessionId.value}, registered=${_isRegistered.value}")
    }
    
    /**
     * 获取设备ID
     * 
     * 优先使用 Android ID，如果不可用则生成 UUID
     */
    fun getDeviceId(): String {
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)
        if (deviceId == null) {
            // 尝试获取 Android ID
            deviceId = try {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            } catch (e: Exception) {
                null
            }
            
            // 如果 Android ID 不可用，生成 UUID
            if (deviceId.isNullOrBlank()) {
                deviceId = "phone_${UUID.randomUUID()}"
            }
            
            // 保存到本地
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
            Log.d(TAG, "生成新的 deviceId: $deviceId")
        }
        return deviceId
    }
    
    /**
     * 获取用户ID
     */
    fun getUserId(): String? = _userId.value
    
    /**
     * 获取认证令牌
     */
    fun getAuthToken(): String? = prefs.getString(KEY_AUTH_TOKEN, null)
    
    /**
     * 获取当前会话ID
     */
    fun getCurrentSessionId(): String? = _currentSessionId.value
    
    /**
     * 设置用户ID
     */
    fun setUserId(userId: String) {
        _userId.value = userId
        prefs.edit().putString(KEY_USER_ID, userId).apply()
        Log.d(TAG, "设置 userId: $userId")
    }
    
    /**
     * 设置认证令牌
     */
    fun setAuthToken(token: String) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
        Log.d(TAG, "设置 authToken")
    }
    
    /**
     * 设置当前会话ID
     */
    fun setCurrentSessionId(sessionId: String?) {
        _currentSessionId.value = sessionId
        if (sessionId != null) {
            prefs.edit().putString(KEY_CURRENT_SESSION_ID, sessionId).apply()
        } else {
            prefs.edit().remove(KEY_CURRENT_SESSION_ID).apply()
        }
        Log.d(TAG, "设置 sessionId: $sessionId")
    }
    
    /**
     * 标记用户已注册
     */
    fun setRegistered(registered: Boolean) {
        _isRegistered.value = registered
        prefs.edit().putBoolean(KEY_IS_REGISTERED, registered).apply()
        Log.d(TAG, "设置 registered: $registered")
    }
    
    /**
     * 保存注册响应
     */
    fun saveRegisterResponse(response: UserRegisterResponse) {
        setUserId(response.userId)
        response.token?.let { setAuthToken(it) }
        setRegistered(true)
        Log.d(TAG, "保存注册响应: userId=${response.userId}, isNewUser=${response.isNewUser}")
    }
    
    /**
     * 清除所有用户数据
     */
    fun clearAll() {
        prefs.edit().clear().apply()
        _userId.value = null
        _currentSessionId.value = null
        _isRegistered.value = false
        Log.d(TAG, "清除所有用户数据")
    }
    
    /**
     * 检查是否需要注册
     */
    fun needsRegistration(): Boolean {
        return !_isRegistered.value || _userId.value == null
    }
    
    /**
     * 获取请求头
     * 
     * 返回需要添加到 API 请求的头信息
     */
    fun getRequestHeaders(): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        
        headers["X-Device-ID"] = getDeviceId()
        headers["X-App-Version"] = Config.APP_VERSION
        
        getUserId()?.let { headers["X-User-ID"] = it }
        getAuthToken()?.let { headers["Authorization"] = "Bearer $it" }
        getCurrentSessionId()?.let { headers["X-Session-ID"] = it }
        
        return headers
    }
}
