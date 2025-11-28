package com.rokid.nutrition.bluetooth

import android.util.Log
import com.rokid.nutrition.Config

/**
 * CXR-S SDK 通信管理器（眼镜端）
 * 
 * 使用 CXR-S SDK 与手机端通信：
 * - 监听连接状态
 * - 发送图片和指令到手机
 * - 订阅手机返回的结果
 * 
 * ⚠️ 真机测试说明：
 * 在 Rokid 设备上编译时，需要：
 * 1. 取消注释下方的 SDK imports
 * 2. 取消注释 cxrBridge 实例化
 * 3. 将 USE_REAL_SDK 设为 true
 * 
 * 参考: https://www.cnblogs.com/xiongbatianduxing/p/19143814
 */

// ========== 真机模式已启用 ==========
import com.rokid.cxr.CXRServiceBridge
import com.rokid.cxr.Caps
// ========================================

class BluetoothSender {
    
    companion object {
        private const val TAG = "CXRManager"
        
        /**
         * ⚠️ 真机测试开关
         * 
         * 开发环境（Mac/PC）：false - 使用模拟模式
         * Rokid 设备：true - 使用真实 SDK（需先取消上方 import 注释）
         */
        private const val USE_REAL_SDK = true
        
        // 设备类型常量
        const val DEVICE_TYPE_ANDROID = 1
        const val DEVICE_TYPE_IPHONE = 2
    }
    
    // ========== 真机模式已启用 ==========
    private val cxrBridge = CXRServiceBridge()
    // ========================================
    
    private var isConnected = false
    private var connectedDeviceName: String? = null
    private var connectedDeviceType: Int = 0
    
    // 回调监听器
    private var connectionListener: ((Boolean, String?) -> Unit)? = null
    private var artcHealthListener: ((Float) -> Unit)? = null
    
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var initRetryCount = 0
    private val MAX_INIT_RETRY = 3
    private val INIT_RETRY_DELAY_MS = 2000L
    
    // 持续探测相关
    private var periodicProbeRunnable: Runnable? = null
    private var probeAttempts = 0
    private val MAX_PROBE_ATTEMPTS = 12  // 最多探测 12 次（60 秒）
    private val PROBE_INTERVAL_MS = 5000L  // 每 5 秒探测一次
    
    /**
     * 初始化 CXR-S SDK
     * 
     * 注意：不要调用 disconnectCXRDevice()，否则会断开手机和眼镜的蓝牙连接！
     */
    fun initialize() {
        if (USE_REAL_SDK) {
            // ========== 真机模式已启用 ==========
            Log.d(TAG, "════════════════════════════════════════")
            Log.d(TAG, "CXR-S SDK 初始化开始（真实模式）")
            Log.d(TAG, "════════════════════════════════════════")
            
            // 重置内部状态（不断开蓝牙连接）
            isConnected = false
            connectedDeviceName = null
            connectedDeviceType = 0
            initRetryCount = 0
            
            // 直接设置监听器（不需要延迟）
            setupStatusListener()
            setupMessageSubscriptions()
            
            // 延迟检测连接状态（给 CXR Service 启动时间）
            // 增加更多探测尝试，确保能检测到连接
            handler.postDelayed({ probeConnectionStatus() }, 1000)
            handler.postDelayed({ probeConnectionStatus() }, 2000)
            handler.postDelayed({ probeConnectionStatus() }, 3000)
            handler.postDelayed({ probeConnectionStatus() }, 5000)
            handler.postDelayed({ probeConnectionStatus() }, 8000)
            handler.postDelayed({ probeConnectionStatus() }, 10000)
            
            // 持续探测（每 5 秒一次，最多 60 秒）
            startPeriodicProbe()
            
            Log.d(TAG, "CXR-S SDK 初始化完成，等待手机连接...")
            // ========================================
        } else {
            // 模拟模式 - 自动连接以便测试 UI
            Log.d(TAG, "CXR-S SDK 初始化（模拟模式）")
            simulateConnection()
        }
    }
    
    /**
     * 设置连接状态监听器
     */
    private fun setupStatusListener() {
        Log.d(TAG, "[INIT] 设置 StatusListener...")
        try {
            cxrBridge.setStatusListener(object : CXRServiceBridge.StatusListener {
                override fun onConnected(name: String, type: Int) {
                    Log.d(TAG, "════════════════════════════════════════")
                    Log.d(TAG, "★★★ StatusListener.onConnected ★★★")
                    Log.d(TAG, "设备名称: $name")
                    Log.d(TAG, "设备类型: ${getDeviceTypeName(type)}")
                    Log.d(TAG, "════════════════════════════════════════")
                    updateConnectionState(true, name, type)
                }
                
                override fun onDisconnected() {
                    Log.d(TAG, "════════════════════════════════════════")
                    Log.d(TAG, "★★★ StatusListener.onDisconnected ★★★")
                    Log.d(TAG, "════════════════════════════════════════")
                    updateConnectionState(false, null, 0)
                }
                
                override fun onARTCStatus(health: Float, reset: Boolean) {
                    Log.d(TAG, "[ARTC] health=$health, reset=$reset")
                    artcHealthListener?.invoke(health)
                    
                    // ARTC 有状态说明已连接
                    if (health > 0 && !isConnected) {
                        Log.d(TAG, "[ARTC] 检测到 ARTC 活动，推断已连接")
                        updateConnectionState(true, "Phone (via ARTC)", DEVICE_TYPE_ANDROID)
                    }
                }
            })
            Log.d(TAG, "[INIT] StatusListener 设置成功")
        } catch (e: Exception) {
            Log.e(TAG, "[INIT] StatusListener 设置失败", e)
        }
    }
    
    /**
     * 设置消息订阅
     */
    private fun setupMessageSubscriptions() {
        Log.d(TAG, "[INIT] 订阅消息...")
        
        // 订阅连接确认消息（手机端连接后会发送）
        try {
            val result = cxrBridge.subscribe("connection_confirm", object : CXRServiceBridge.MsgCallback {
                override fun onReceive(name: String, args: Caps, data: ByteArray?) {
                    Log.d(TAG, "════════════════════════════════════════")
                    Log.d(TAG, "★★★ 收到 connection_confirm 消息 ★★★")
                    try {
                        val deviceName = args.at(0).getString()
                        Log.d(TAG, "设备名称: $deviceName")
                        updateConnectionState(true, deviceName, DEVICE_TYPE_ANDROID)
                    } catch (e: Exception) {
                        Log.e(TAG, "解析连接确认失败", e)
                        // 即使解析失败，收到消息本身就说明已连接
                        updateConnectionState(true, "Phone", DEVICE_TYPE_ANDROID)
                    }
                    Log.d(TAG, "════════════════════════════════════════")
                }
            })
            Log.d(TAG, "[INIT] 订阅 connection_confirm 结果: $result")
        } catch (e: Exception) {
            Log.e(TAG, "[INIT] 订阅 connection_confirm 失败", e)
        }
        
        // 订阅任意消息作为连接探测
        try {
            val result = cxrBridge.subscribe("ping", object : CXRServiceBridge.MsgCallback {
                override fun onReceive(name: String, args: Caps, data: ByteArray?) {
                    Log.d(TAG, "[PING] 收到 ping 消息")
                    if (!isConnected) {
                        updateConnectionState(true, "Phone (via ping)", DEVICE_TYPE_ANDROID)
                    }
                }
            })
            Log.d(TAG, "[INIT] 订阅 ping 结果: $result")
        } catch (e: Exception) {
            Log.e(TAG, "[INIT] 订阅 ping 失败", e)
        }
    }
    
    /**
     * 启动持续探测
     * 每 5 秒探测一次，最多 60 秒
     */
    private fun startPeriodicProbe() {
        stopPeriodicProbe()  // 先停止之前的探测
        probeAttempts = 0
        
        periodicProbeRunnable = object : Runnable {
            override fun run() {
                if (isConnected) {
                    Log.d(TAG, "[PERIODIC] 已连接，停止持续探测")
                    return
                }
                
                probeAttempts++
                if (probeAttempts > MAX_PROBE_ATTEMPTS) {
                    Log.w(TAG, "[PERIODIC] 达到最大探测次数 ($MAX_PROBE_ATTEMPTS)，停止探测")
                    return
                }
                
                Log.d(TAG, "[PERIODIC] 第 $probeAttempts 次探测...")
                probeConnectionStatus()
                
                // 继续下一次探测
                if (!isConnected) {
                    handler.postDelayed(this, PROBE_INTERVAL_MS)
                }
            }
        }
        
        handler.postDelayed(periodicProbeRunnable!!, PROBE_INTERVAL_MS)
        Log.d(TAG, "[PERIODIC] 启动持续探测，间隔 ${PROBE_INTERVAL_MS}ms")
    }
    
    /**
     * 停止持续探测
     */
    private fun stopPeriodicProbe() {
        periodicProbeRunnable?.let {
            handler.removeCallbacks(it)
            periodicProbeRunnable = null
        }
    }
    
    /**
     * 探测连接状态
     * 尝试发送一个空消息，如果成功说明已连接
     */
    private fun probeConnectionStatus() {
        if (isConnected) {
            Log.d(TAG, "[PROBE] 已连接，跳过探测")
            stopPeriodicProbe()
            return
        }
        
        Log.d(TAG, "[PROBE] 尝试探测连接状态...")
        try {
            // 尝试发送一个探测消息
            val caps = Caps().apply {
                write("probe")
                writeInt64(System.currentTimeMillis())
            }
            val result = cxrBridge.sendMessage("viseat_probe", caps)
            Log.d(TAG, "[PROBE] sendMessage 结果: $result")
            
            // result == 0 表示发送成功，说明已连接
            if (result == 0) {
                Log.d(TAG, "[PROBE] ★★★ 探测成功，已连接 ★★★")
                updateConnectionState(true, "Phone (via probe)", DEVICE_TYPE_ANDROID)
                stopPeriodicProbe()
            } else {
                Log.d(TAG, "[PROBE] 探测失败，未连接 (error=$result)")
                // 不要调用 startBTPairing()，它会干扰现有连接
            }
        } catch (e: Exception) {
            Log.e(TAG, "[PROBE] 探测异常", e)
        }
    }
    
    /**
     * 更新连接状态（统一入口）
     */
    private fun updateConnectionState(connected: Boolean, deviceName: String?, deviceType: Int) {
        val wasConnected = isConnected
        isConnected = connected
        connectedDeviceName = deviceName
        connectedDeviceType = deviceType
        
        if (connected != wasConnected) {
            Log.d(TAG, "[STATE] 连接状态变更: $wasConnected -> $connected")
            handler.post {
                connectionListener?.invoke(connected, deviceName)
            }
        }
    }
    
    /**
     * 模拟连接（仅用于开发测试）
     */
    private fun simulateConnection() {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            isConnected = true
            connectedDeviceName = "模拟手机"
            connectionListener?.invoke(true, connectedDeviceName)
            Log.d(TAG, "模拟连接成功")
        }, 2000)
    }
    
    /**
     * 设置连接状态监听器
     */
    fun setConnectionListener(listener: (Boolean, String?) -> Unit) {
        connectionListener = listener
    }
    
    /**
     * 设置 ARTC 健康度监听器
     */
    fun setArtcHealthListener(listener: (Float) -> Unit) {
        artcHealthListener = listener
    }
    
    /**
     * 检查是否已连接到手机
     */
    fun isConnected(): Boolean = isConnected
    
    /**
     * 获取已连接设备名称
     */
    fun getConnectedDeviceName(): String? = connectedDeviceName
    
    /**
     * 发送图片到手机
     * 
     * 错误码说明：
     * 0 = 成功
     * -1 = EINVAL (参数错误)
     * -2 = EDUP (重复)
     * -3 = EFAULT (flora 调用失败，通常是手机端未就绪)
     * -4 = EBUSY (忙)
     */
    fun sendImage(
        imageData: ByteArray,
        format: String = "jpeg",
        isManualCapture: Boolean = false
    ): Boolean {
        Log.d(TAG, "════════════════════════════════════════")
        Log.d(TAG, "[SEND] 准备发送图片...")
        Log.d(TAG, "[SEND] isConnected=$isConnected")
        Log.d(TAG, "[SEND] 图片大小: ${imageData.size} bytes")
        Log.d(TAG, "[SEND] 消息名称: ${Config.MsgName.IMAGE}")
        
        if (!isConnected) {
            Log.w(TAG, "[SEND] ✗ 未连接到手机，无法发送图片")
            return false
        }
        
        return if (USE_REAL_SDK) {
            // ========== 真机模式已启用 ==========
            try {
                // 将所有数据（包括图片）写入 Caps
                // 因为 CXR-M SDK 的 CustomCmdListener 只接收 caps，不接收二进制数据参数
                val caps = Caps().apply {
                    write(format)                              // [0] 图片格式
                    writeInt32(imageData.size)                 // [1] 数据大小
                    writeInt64(System.currentTimeMillis())     // [2] 时间戳
                    writeInt32(if (isManualCapture) 1 else 0)  // [3] 是否主动拍照
                    write(imageData)                           // [4] 图片二进制数据
                }
                
                Log.d(TAG, "[SEND] 调用 sendMessage (数据在 Caps 中)...")
                // 不使用额外的二进制参数，因为手机端 CustomCmdListener 不接收它
                val result = cxrBridge.sendMessage(Config.MsgName.IMAGE, caps)
                
                when (result) {
                    0 -> Log.d(TAG, "[SEND] ✓ 发送成功")
                    -1 -> Log.e(TAG, "[SEND] ✗ 发送失败: EINVAL (参数错误)")
                    -2 -> Log.e(TAG, "[SEND] ✗ 发送失败: EDUP (重复)")
                    -3 -> Log.e(TAG, "[SEND] ✗ 发送失败: EFAULT (手机端未就绪，请确保手机App在前台运行)")
                    -4 -> Log.e(TAG, "[SEND] ✗ 发送失败: EBUSY (忙)")
                    else -> Log.e(TAG, "[SEND] ✗ 发送失败: 未知错误 ($result)")
                }
                Log.d(TAG, "════════════════════════════════════════")
                
                result == 0
            } catch (e: Exception) {
                Log.e(TAG, "[SEND] ✗ 发送异常", e)
                Log.d(TAG, "════════════════════════════════════════")
                false
            }
            // ========================================
        } else {
            // 模拟发送
            Log.d(TAG, "[SEND] 发送图片（模拟）: ${imageData.size} bytes, 主动: $isManualCapture")
            Log.d(TAG, "════════════════════════════════════════")
            true
        }
    }
    
    /**
     * 发送开始用餐指令
     */
    fun sendStartMealCommand(): Boolean {
        return sendCommand(Config.CommandType.START_MEAL)
    }
    
    /**
     * 发送结束用餐指令
     */
    fun sendEndMealCommand(): Boolean {
        return sendCommand(Config.CommandType.END_MEAL)
    }
    
    /**
     * 发送指令到手机
     */
    private fun sendCommand(commandType: String): Boolean {
        if (!isConnected) {
            Log.w(TAG, "未连接到手机，无法发送指令: $commandType")
            return false
        }
        
        return if (USE_REAL_SDK) {
            // ========== 真机模式已启用 ==========
            try {
                val caps = Caps().apply {
                    write(commandType)                         // [0] 指令类型
                    writeInt64(System.currentTimeMillis())     // [1] 时间戳
                }
                val result = cxrBridge.sendMessage(Config.MsgName.COMMAND, caps)
                Log.d(TAG, "发送指令: $commandType, result=$result")
                result == 0
            } catch (e: Exception) {
                Log.e(TAG, "发送指令失败", e)
                false
            }
            // ========================================
        } else {
            Log.d(TAG, "发送指令（模拟）: $commandType")
            true
        }
    }
    
    /**
     * 获取设备类型名称
     */
    private fun getDeviceTypeName(type: Int): String {
        return when (type) {
            DEVICE_TYPE_ANDROID -> "Android"
            DEVICE_TYPE_IPHONE -> "iPhone"
            else -> "Unknown($type)"
        }
    }
    
    /**
     * 释放资源
     * 
     * 注意：不要调用 disconnectCXRDevice()，否则会断开手机和眼镜的蓝牙连接！
     * App 重启时应该复用现有的蓝牙连接。
     */
    fun release() {
        Log.d(TAG, "[RELEASE] 开始释放 CXR Manager...")
        
        // 停止持续探测
        stopPeriodicProbe()
        
        // 移除所有待执行的回调
        handler.removeCallbacksAndMessages(null)
        
        // 不要断开蓝牙连接！只清理内部状态
        connectionListener = null
        artcHealthListener = null
        isConnected = false
        connectedDeviceName = null
        connectedDeviceType = 0
        
        Log.d(TAG, "[RELEASE] CXR Manager 已释放（蓝牙连接保持）")
    }
}
