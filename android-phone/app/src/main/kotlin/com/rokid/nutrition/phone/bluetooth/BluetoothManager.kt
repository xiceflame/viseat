package com.rokid.nutrition.phone.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager as AndroidBluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.core.content.ContextCompat
import com.rokid.cxr.Caps
import com.rokid.cxr.client.extend.CxrApi
import com.rokid.cxr.client.extend.callbacks.BluetoothStatusCallback
import com.rokid.cxr.client.extend.callbacks.GlassInfoResultCallback
import com.rokid.cxr.client.extend.callbacks.PhotoResultCallback
import com.rokid.cxr.client.extend.listeners.BatteryLevelUpdateListener
import com.rokid.cxr.client.extend.listeners.BrightnessUpdateListener
import com.rokid.cxr.client.extend.listeners.CustomCmdListener
import com.rokid.cxr.client.extend.listeners.VolumeUpdateListener
import com.rokid.cxr.client.utils.ValueUtil
import com.rokid.nutrition.phone.Config
import com.rokid.nutrition.phone.util.DebugLogger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * 蓝牙管理器 - 使用 Rokid CXR-M SDK
 * 
 * 连接流程（根据厂家文档）：
 * 1. BLE 扫描获取眼镜设备（使用 Rokid 固定 UUID: 00009100-0000-1000-8000-00805f9b34fb）
 * 2. 调用 initBluetooth() 发起配对，获取 socketUuid + macAddress
 * 3. 调用 connectBluetooth() 建立通信连接
 * 4. 连接成功后可收发自定义消息
 */
class BluetoothManager(private val context: Context) {
    
    companion object {
        private const val TAG = "BluetoothManager"
        
        // Rokid 眼镜 BLE 扫描 UUID（厂家指定）
        private val ROKID_SERVICE_UUID = UUID.fromString("00009100-0000-1000-8000-00805f9b34fb")
        
        // SharedPreferences keys
        private const val PREFS_NAME = "rokid_bluetooth"
        private const val KEY_SOCKET_UUID = "socket_uuid"
        private const val KEY_MAC_ADDRESS = "mac_address"
    }
    
    private val cxrApi = CxrApi.getInstance()
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as AndroidBluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // 连接状态
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    // 扫描到的设备
    private val _scannedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val scannedDevices: StateFlow<List<BluetoothDevice>> = _scannedDevices.asStateFlow()
    
    // 接收到的图片数据
    private val _receivedImage = MutableSharedFlow<ImageData>(extraBufferCapacity = 1)
    val receivedImage: SharedFlow<ImageData> = _receivedImage.asSharedFlow()
    
    // 接收到的指令
    private val _receivedCommand = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val receivedCommand: SharedFlow<String> = _receivedCommand.asSharedFlow()
    
    // 保存的连接信息
    private var socketUuid: String? = null
    private var macAddress: String? = null
    
    // 是否启用自动连接
    private var autoConnectEnabled = true
    
    // BLE 扫描回调
    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val deviceName = device.name ?: ""
            
            // 检查是否是 Glasses 开头的设备（目标设备）
            val isGlassesDevice = deviceName.startsWith("Glasses", ignoreCase = true)
            
            // 也检查其他 Rokid 相关设备
            val isRokidDevice = deviceName.contains("Rokid", ignoreCase = true) ||
                               deviceName.contains("Air", ignoreCase = true) ||
                               deviceName.contains("Max", ignoreCase = true)
            
            // 也检查 Service UUID
            val hasRokidUuid = result.scanRecord?.serviceUuids?.any { 
                it.uuid == ROKID_SERVICE_UUID 
            } == true
            
            if (isGlassesDevice || isRokidDevice || hasRokidUuid) {
                val currentList = _scannedDevices.value.toMutableList()
                if (!currentList.any { it.address == device.address }) {
                    currentList.add(device)
                    _scannedDevices.value = currentList
                    DebugLogger.i(TAG, "发现眼镜设备: $deviceName - ${device.address}")
                    
                    // 如果是 Glasses 开头的设备，自动连接
                    if (isGlassesDevice && autoConnectEnabled) {
                        DebugLogger.i(TAG, "自动连接 Glasses 设备: $deviceName")
                        autoConnectEnabled = false  // 防止重复连接
                        initBluetooth(device)
                    }
                }
            }
        }
        
        override fun onScanFailed(errorCode: Int) {
            val errorMsg = when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> "扫描已在进行中"
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "应用注册失败"
                SCAN_FAILED_FEATURE_UNSUPPORTED -> "不支持 BLE 扫描"
                SCAN_FAILED_INTERNAL_ERROR -> "内部错误"
                else -> "未知错误 ($errorCode)"
            }
            DebugLogger.e(TAG, "扫描失败: $errorMsg")
            _connectionState.value = ConnectionState.Error("扫描失败: $errorMsg")
        }
    }
    
    // 蓝牙状态回调
    private val bluetoothCallback = object : BluetoothStatusCallback {
        override fun onConnectionInfo(
            socketUuid: String?,
            macAddress: String?,
            rokidAccount: String?,
            glassesType: Int
        ) {
            Log.d(TAG, "=== 收到连接信息 ===")
            Log.d(TAG, "socketUuid=$socketUuid")
            Log.d(TAG, "macAddress=$macAddress")
            Log.d(TAG, "rokidAccount=$rokidAccount")
            Log.d(TAG, "glassesType=$glassesType")
            
            // 保存连接信息用于后续重连
            socketUuid?.let { 
                this@BluetoothManager.socketUuid = it
                prefs.edit().putString(KEY_SOCKET_UUID, it).apply()
                Log.d(TAG, "已保存 socketUuid")
            }
            macAddress?.let { 
                this@BluetoothManager.macAddress = it
                prefs.edit().putString(KEY_MAC_ADDRESS, it).apply()
                Log.d(TAG, "已保存 macAddress")
            }
            
            // 配对成功后，使用 connectBluetooth 建立通信连接
            Log.d(TAG, "准备调用 connectWithCredentials()...")
            connectWithCredentials()
        }
        
        override fun onConnected() {
            DebugLogger.i(TAG, "=== 蓝牙已连接回调 ===")
            DebugLogger.i(TAG, "isBluetoothConnected: ${cxrApi.isBluetoothConnected}")
            _connectionState.value = ConnectionState.Connected
            DebugLogger.i(TAG, "连接状态已更新为 Connected")
            setupCustomCmdListener()
            
            // 发送连接确认消息到眼镜端
            sendConnectionConfirm()
        }
        
        override fun onDisconnected() {
            DebugLogger.w(TAG, "=== 蓝牙已断开回调 ===")
            _connectionState.value = ConnectionState.Disconnected
            
            // 断开后清除凭证，因为 socketUuid 可能已失效
            // 下次连接时会自动重新扫描配对
            Log.d(TAG, "断开连接，清除凭证以便下次重新配对")
            clearSavedCredentials()
        }
        
        override fun onFailed(errorCode: ValueUtil.CxrBluetoothErrorCode?) {
            DebugLogger.e(TAG, "=== 蓝牙连接失败 ===")
            DebugLogger.e(TAG, "错误码: ${errorCode?.name}")
            
            // 如果是 SOCKET_CONNECT_FAILED，清除保存的凭证并自动重新扫描配对
            if (errorCode?.name?.contains("SOCKET") == true) {
                Log.w(TAG, "Socket 连接失败，清除保存的凭证并自动重新扫描")
                clearSavedCredentials()
                // 自动重新扫描并配对，而不是显示错误
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    startScan(enableAutoConnect = true)
                }, 500)
            } else {
                _connectionState.value = ConnectionState.Error("连接失败: ${errorCode?.name ?: "Unknown"}")
            }
        }
    }
    
    init {
        // 从本地存储恢复连接信息
        socketUuid = prefs.getString(KEY_SOCKET_UUID, null)
        macAddress = prefs.getString(KEY_MAC_ADDRESS, null)
    }
    
    /**
     * 检查蓝牙权限
     */
    fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * 检查蓝牙是否启用
     */
    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true
    
    /**
     * 开始扫描眼镜设备
     * 会自动连接以 "Glasses" 开头的设备
     * 
     * @param enableAutoConnect 是否启用自动连接（默认 true）
     */
    @SuppressLint("MissingPermission")
    fun startScan(enableAutoConnect: Boolean = true) {
        DebugLogger.i(TAG, "startScan() 被调用, autoConnect=$enableAutoConnect")
        
        if (!hasBluetoothPermissions()) {
            DebugLogger.e(TAG, "缺少蓝牙权限")
            _connectionState.value = ConnectionState.Error("缺少蓝牙权限")
            return
        }
        
        if (!isBluetoothEnabled()) {
            DebugLogger.e(TAG, "蓝牙未启用")
            _connectionState.value = ConnectionState.Error("蓝牙未启用")
            return
        }
        
        autoConnectEnabled = enableAutoConnect
        _scannedDevices.value = emptyList()
        _connectionState.value = ConnectionState.Scanning
        
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            DebugLogger.e(TAG, "无法获取 BLE Scanner")
            _connectionState.value = ConnectionState.Error("无法获取 BLE Scanner")
            return
        }
        
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        
        try {
            scanner.startScan(null, settings, scanCallback)
            DebugLogger.i(TAG, "开始扫描眼镜设备（自动连接 Glasses 开头的设备）...")
        } catch (e: Exception) {
            DebugLogger.e(TAG, "启动扫描失败: ${e.message}")
            _connectionState.value = ConnectionState.Error("启动扫描失败: ${e.message}")
        }
    }
    
    /**
     * 自动连接：先尝试重连已保存的设备，否则扫描并自动连接
     */
    fun autoConnect() {
        DebugLogger.i(TAG, "=== autoConnect() 被调用 ===")
        DebugLogger.i(TAG, "当前连接状态: ${_connectionState.value}")
        DebugLogger.i(TAG, "SDK isBluetoothConnected: ${cxrApi.isBluetoothConnected}")
        DebugLogger.i(TAG, "hasSavedCredentials: ${hasSavedCredentials()}")
        DebugLogger.i(TAG, "socketUuid: $socketUuid")
        DebugLogger.i(TAG, "macAddress: $macAddress")
        
        // 如果 SDK 报告已连接，更新状态并返回
        if (cxrApi.isBluetoothConnected) {
            Log.d(TAG, "SDK 报告已连接，更新状态")
            _connectionState.value = ConnectionState.Connected
            setupCustomCmdListener()
            return
        }
        
        // 如果正在连接中，不要重复操作
        if (_connectionState.value is ConnectionState.Connecting || 
            _connectionState.value is ConnectionState.Scanning) {
            Log.d(TAG, "正在连接/扫描中，跳过")
            return
        }
        
        // 先尝试使用保存的凭证重连
        if (hasSavedCredentials()) {
            DebugLogger.i(TAG, "尝试使用保存的凭证重连...")
            connectWithCredentials()
        } else {
            // 没有保存的凭证，开始扫描并自动连接
            DebugLogger.i(TAG, "没有保存的凭证，开始扫描...")
            startScan(enableAutoConnect = true)
        }
    }
    
    /**
     * 停止扫描
     */
    @SuppressLint("MissingPermission")
    fun stopScan() {
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        if (_connectionState.value == ConnectionState.Scanning) {
            _connectionState.value = ConnectionState.Disconnected
        }
        Log.d(TAG, "停止扫描")
    }


    /**
     * 初始化蓝牙连接（首次配对）
     * 
     * 注意：调用此方法时，不可注册自动重连广播，
     * 否则系统可能不会弹出配对弹窗
     */
    @SuppressLint("MissingPermission")
    fun initBluetooth(device: BluetoothDevice) {
        stopScan()
        _connectionState.value = ConnectionState.Connecting
        Log.d(TAG, "=== 初始化蓝牙连接 ===")
        Log.d(TAG, "设备名称: ${device.name}")
        Log.d(TAG, "设备地址: ${device.address}")
        Log.d(TAG, "设备类型: ${device.type}")
        Log.d(TAG, "设备绑定状态: ${device.bondState}")
        
        cxrApi.initBluetooth(context, device, bluetoothCallback)
        Log.d(TAG, "initBluetooth() 已调用，等待回调...")
    }
    
    /**
     * 使用保存的凭证连接（已配对设备重连）
     */
    private fun connectWithCredentials() {
        val uuid = socketUuid
        val mac = macAddress
        
        if (uuid == null || mac == null) {
            Log.e(TAG, "缺少连接凭证: uuid=$uuid, mac=$mac")
            _connectionState.value = ConnectionState.Error("缺少连接凭证")
            return
        }
        
        Log.d(TAG, "=== 使用凭证连接 ===")
        Log.d(TAG, "uuid=$uuid")
        Log.d(TAG, "mac=$mac")
        _connectionState.value = ConnectionState.Connecting
        cxrApi.connectBluetooth(context, uuid, mac, bluetoothCallback)
        Log.d(TAG, "connectBluetooth() 已调用")
    }
    
    /**
     * 尝试自动重连（使用保存的凭证）
     */
    fun tryReconnect(): Boolean {
        if (socketUuid != null && macAddress != null) {
            _connectionState.value = ConnectionState.Connecting
            connectWithCredentials()
            return true
        }
        return false
    }
    
    /**
     * 设置自定义协议监听器
     */
    private fun setupCustomCmdListener() {
        cxrApi.setCustomCmdListener(object : CustomCmdListener {
            override fun onCustomCmd(cmdName: String?, caps: Caps?) {
                Log.d(TAG, "收到自定义消息: $cmdName")
                when (cmdName) {
                    Config.MsgName.IMAGE -> handleImageReceived(caps)
                    Config.MsgName.COMMAND -> handleCommandReceived(caps)
                }
            }
        })
    }
    
    /**
     * 发送连接确认消息到眼镜端
     * 
     * 使用多种方式通知眼镜端连接成功：
     * 1. sendGlobalToast - 直接在眼镜上显示 Toast
     * 2. sendCustomCmd - 发送自定义消息（眼镜端 VISEAT 应用订阅）
     * 
     * 增加重试机制，确保眼镜端能收到连接确认
     */
    private fun sendConnectionConfirm() {
        val deviceName = android.os.Build.MODEL ?: "Android Phone"
        
        // 首先关闭之前可能残留的 AR 视图
        try {
            cxrApi.closeCustomView()
            Log.d(TAG, "已关闭之前的 AR 视图")
        } catch (e: Exception) {
            Log.w(TAG, "关闭 AR 视图失败: ${e.message}")
        }
        
        // 发送连接确认消息（带重试）
        sendConnectionConfirmWithRetry(deviceName, 0)
    }
    
    /**
     * 带重试的连接确认消息发送
     */
    private fun sendConnectionConfirmWithRetry(deviceName: String, attempt: Int) {
        val maxAttempts = 5
        val retryDelayMs = 1000L
        
        try {
            val caps = Caps().apply {
                write(deviceName)  // [0] 设备名称
            }
            cxrApi.sendCustomCmd("connection_confirm", caps)
            Log.d(TAG, "已发送连接确认消息 (第 ${attempt + 1} 次): $deviceName")
            
            // 延迟后再发送一次，确保眼镜端能收到
            if (attempt < maxAttempts - 1) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (isConnected()) {
                        sendConnectionConfirmWithRetry(deviceName, attempt + 1)
                    }
                }, retryDelayMs)
            }
        } catch (e: Exception) {
            Log.e(TAG, "发送连接确认消息失败 (第 ${attempt + 1} 次)", e)
            
            // 重试
            if (attempt < maxAttempts - 1) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (isConnected()) {
                        sendConnectionConfirmWithRetry(deviceName, attempt + 1)
                    }
                }, retryDelayMs)
            }
        }
    }
    
    /**
     * 处理接收到的图片数据
     * 
     * 眼镜端发送格式 (BluetoothSender.sendImage):
     * Caps 结构:
     * [0] String: 图片格式 = "jpeg"
     * [1] Int32:  数据大小 (bytes)
     * [2] Int64:  时间戳 (毫秒)
     * [3] Int32:  是否主动拍照 (1=是, 0=否)
     * [4] Binary: 图片二进制数据
     */
    private fun handleImageReceived(caps: Caps?) {
        caps ?: return
        
        try {
            // 索引从 0 开始，与眼镜端发送格式一致
            val format = caps.at(0)?.string ?: "jpeg"
            val dataSize = caps.at(1)?.int ?: 0
            val timestamp = caps.at(2)?.long ?: System.currentTimeMillis()
            val isManualCapture = (caps.at(3)?.int ?: 0) != 0
            
            // 图片二进制数据在 Caps[4]
            val binaryValue = caps.at(4)?.binary ?: run {
                Log.e(TAG, "图片二进制数据为空 (Caps[4])")
                return
            }
            val imageData = binaryValue.data.copyOfRange(binaryValue.offset, binaryValue.offset + binaryValue.length)
            
            Log.d(TAG, "收到图片: format=$format, size=${imageData.size}, manual=$isManualCapture")
            
            _receivedImage.tryEmit(ImageData(
                data = imageData,
                format = format,
                timestamp = timestamp,
                isManualCapture = isManualCapture
            ))
        } catch (e: Exception) {
            Log.e(TAG, "解析图片数据失败", e)
        }
    }
    
    /**
     * 处理接收到的指令
     * 
     * 眼镜端发送格式 (BluetoothSender.sendCommand):
     * Caps 结构:
     * [0] String: 指令类型 = "start_meal" | "end_meal"
     * [1] Int64:  时间戳 (毫秒)
     */
    private fun handleCommandReceived(caps: Caps?) {
        caps ?: return
        
        try {
            // 索引从 0 开始，与眼镜端发送格式一致
            val commandType = caps.at(0)?.string ?: return
            Log.d(TAG, "收到指令: $commandType")
            _receivedCommand.tryEmit(commandType)
        } catch (e: Exception) {
            Log.e(TAG, "解析指令失败", e)
        }
    }
    
    /**
     * 发送营养结果到眼镜
     * 
     * Caps 结构:
     * [0] String: 菜品描述
     * [1] Float:  总热量 (kcal)
     * [2] Float:  蛋白质 (g)
     * [3] Float:  碳水化合物 (g)
     * [4] Float:  脂肪 (g)
     * [5] String: LLM 建议文本
     */
    fun sendNutritionResult(result: NutritionResult) {
        val caps = Caps().apply {
            write(result.foodName)
            writeFloat(result.calories.toFloat())
            writeFloat(result.protein.toFloat())
            writeFloat(result.carbs.toFloat())
            writeFloat(result.fat.toFloat())
            write(result.suggestion)
        }
        cxrApi.sendCustomCmd(Config.MsgName.RESULT, caps)
        Log.d(TAG, "发送营养结果: ${result.foodName}")
    }
    
    /**
     * 发送会话状态到眼镜
     * 
     * Caps 结构:
     * [0] String: 会话ID
     * [1] String: 状态 ("active" | "ended")
     * [2] Float:  总摄入热量 (kcal)
     * [3] String: 消息文本
     */
    fun sendSessionStatus(sessionId: String, status: String, totalConsumed: Double, message: String) {
        Log.d(TAG, "准备发送会话状态: status=$status, sessionId=$sessionId, calories=$totalConsumed")
        
        val caps = Caps().apply {
            write(sessionId)
            write(status)
            writeFloat(totalConsumed.toFloat())
            write(message)
        }
        
        try {
            cxrApi.sendCustomCmd(Config.MsgName.SESSION_STATUS, caps)
            Log.d(TAG, "会话状态已发送: $status, ${totalConsumed}kcal")
        } catch (e: Exception) {
            Log.e(TAG, "发送会话状态失败", e)
        }
    }
    
    /**
     * 发送用餐总结到眼镜
     * 
     * Caps 结构:
     * [0] Float: 总热量 (kcal)
     * [1] Float: 蛋白质 (g)
     * [2] Float: 碳水化合物 (g)
     * [3] Float: 脂肪 (g)
     * [4] Float: 用餐时长 (分钟)
     * [5] String: 评级 ("good" | "fair" | "poor")
     * [6] String: 简短建议 (≤20字符)
     * 
     * @param summary 用餐总结数据
     * @return true 发送成功，false 发送失败（眼镜未连接或发送异常）
     */
    fun sendMealSummary(summary: com.rokid.nutrition.phone.network.model.MealSummaryResponse): Boolean {
        if (!isConnected()) {
            Log.w(TAG, "眼镜未连接，跳过发送用餐总结")
            return false
        }
        
        try {
            val caps = Caps().apply {
                writeFloat(summary.totalCalories.toFloat())
                writeFloat(summary.totalProtein.toFloat())
                writeFloat(summary.totalCarbs.toFloat())
                writeFloat(summary.totalFat.toFloat())
                writeFloat(summary.durationMinutes.toFloat())
                write(summary.rating)
                write(summary.shortAdvice.take(20))  // 限制 20 字符
            }
            cxrApi.sendCustomCmd(Config.MsgName.MEAL_SUMMARY, caps)
            Log.d(TAG, "发送用餐总结: ${summary.totalCalories} kcal, ${summary.durationMinutes} min, rating=${summary.rating}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "发送用餐总结失败", e)
            return false
        }
    }
    
    /**
     * 从 VLM 响应中提取菜品描述
     */
    fun formatFoodName(foods: List<FoodItem>): String {
        return when {
            foods.isEmpty() -> "未识别到食物"
            foods.size == 1 -> foods[0].dishName
            foods.size == 2 -> "${foods[0].dishName} · ${foods[1].dishName}"
            else -> "${foods[0].dishName}等${foods.size}道菜"
        }
    }
    
    /**
     * 断开连接
     */
    fun disconnect() {
        cxrApi.deinitBluetooth()
        _connectionState.value = ConnectionState.Disconnected
        Log.d(TAG, "蓝牙已断开")
    }
    
    /**
     * 检查连接状态
     */
    fun isConnected(): Boolean {
        return cxrApi.isBluetoothConnected || _connectionState.value is ConnectionState.Connected
    }
    
    /**
     * 获取眼镜设备信息
     */
    fun getGlassInfo(callback: (GlassInfo?) -> Unit) {
        if (isConnected()) {
            cxrApi.getGlassInfo(object : GlassInfoResultCallback {
                override fun onGlassInfoResult(status: ValueUtil.CxrStatus?, glassInfo: com.rokid.cxr.client.extend.infos.GlassInfo?) {
                    if (status == ValueUtil.CxrStatus.RESPONSE_SUCCEED && glassInfo != null) {
                        callback(GlassInfo(
                            name = glassInfo.deviceName ?: "Rokid Glasses",
                            firmwareVersion = glassInfo.systemVersion ?: "Unknown",
                            serialNumber = glassInfo.deviceId ?: "",
                            batteryLevel = glassInfo.batteryLevel,
                            isCharging = glassInfo.isCharging
                        ))
                    } else {
                        callback(null)
                    }
                }
            })
        } else {
            callback(null)
        }
    }
    
    /**
     * 设置电量监听
     */
    fun setBatteryListener(listener: (Int, Boolean) -> Unit) {
        cxrApi.setBatteryLevelUpdateListener(object : BatteryLevelUpdateListener {
            override fun onBatteryLevelUpdated(level: Int, isCharging: Boolean) {
                listener(level, isCharging)
            }
        })
    }
    
    /**
     * 清除保存的连接信息
     */
    fun clearSavedCredentials() {
        socketUuid = null
        macAddress = null
        prefs.edit().clear().apply()
        Log.d(TAG, "已清除保存的连接信息")
    }
    
    /**
     * 打开自定义视图（AR 显示）
     */
    fun openARView(result: NutritionResult) {
        val viewJson = buildNutritionViewJson(result)
        cxrApi.openCustomView(viewJson)
        Log.d(TAG, "打开 AR 视图")
    }
    
    /**
     * 更新自定义视图
     */
    fun updateARView(result: NutritionResult) {
        val updateJson = buildNutritionUpdateJson(result)
        cxrApi.updateCustomView(updateJson)
    }
    
    /**
     * 关闭自定义视图
     */
    fun closeARView() {
        cxrApi.closeCustomView()
        Log.d(TAG, "关闭 AR 视图")
    }
    
    private fun buildNutritionViewJson(result: NutritionResult): String {
        return """
        {
            "type": "LinearLayout",
            "props": {
                "id": "root",
                "layout_width": "match_parent",
                "layout_height": "wrap_content",
                "backgroundColor": "#CC000000",
                "orientation": "vertical",
                "paddingStart": "16dp",
                "paddingEnd": "16dp",
                "paddingTop": "8dp",
                "paddingBottom": "8dp"
            },
            "children": [
                {
                    "type": "TextView",
                    "props": {
                        "id": "foodName",
                        "text": "${result.foodName}",
                        "textColor": "#FFFFFF",
                        "textSize": "20sp",
                        "textStyle": "bold"
                    }
                },
                {
                    "type": "TextView",
                    "props": {
                        "id": "calories",
                        "text": "热量: ${result.calories.toInt()} kcal",
                        "textColor": "#FF6B6B",
                        "textSize": "16sp"
                    }
                },
                {
                    "type": "TextView",
                    "props": {
                        "id": "macros",
                        "text": "蛋白质: ${result.protein.toInt()}g | 碳水: ${result.carbs.toInt()}g | 脂肪: ${result.fat.toInt()}g",
                        "textColor": "#AAAAAA",
                        "textSize": "14sp"
                    }
                },
                {
                    "type": "TextView",
                    "props": {
                        "id": "suggestion",
                        "text": "${result.suggestion}",
                        "textColor": "#4CAF50",
                        "textSize": "14sp",
                        "marginTop": "8dp"
                    }
                }
            ]
        }
        """.trimIndent()
    }
    
    private fun buildNutritionUpdateJson(result: NutritionResult): String {
        return """
        {
            "updateList": [
                {"id": "foodName", "props": {"text": "${result.foodName}"}},
                {"id": "calories", "props": {"text": "热量: ${result.calories.toInt()} kcal"}},
                {"id": "macros", "props": {"text": "蛋白质: ${result.protein.toInt()}g | 碳水: ${result.carbs.toInt()}g | 脂肪: ${result.fat.toInt()}g"}},
                {"id": "suggestion", "props": {"text": "${result.suggestion}"}}
            ]
        }
        """.trimIndent()
    }
    
    // ==================== SDK 高级功能 ====================
    
    /**
     * 远程触发眼镜拍照
     * 
     * @param width 图片宽度
     * @param height 图片高度
     * @param quality 图片质量 (1-100)
     * @param callback 拍照结果回调
     */
    fun takeGlassPhoto(
        width: Int = 1920,
        height: Int = 1080,
        quality: Int = 85,
        callback: (ByteArray?) -> Unit
    ) {
        DebugLogger.d(TAG, "════════════════════════════════════════")
        DebugLogger.d(TAG, "=== takeGlassPhoto() 被调用 ===")
        DebugLogger.d(TAG, "分辨率: ${width}x${height}, 质量: $quality")
        DebugLogger.d(TAG, "isConnected(): ${isConnected()}")
        DebugLogger.d(TAG, "SDK isBluetoothConnected: ${cxrApi.isBluetoothConnected}")
        
        if (!isConnected()) {
            DebugLogger.w(TAG, "未连接，无法拍照")
            callback(null)
            return
        }
        
        try {
            DebugLogger.d(TAG, "调用 cxrApi.takeGlassPhoto()...")
            cxrApi.takeGlassPhoto(width, height, quality, object : PhotoResultCallback {
                override fun onPhotoResult(status: ValueUtil.CxrStatus?, imageData: ByteArray?) {
                    DebugLogger.d(TAG, "=== SDK 拍照回调 ===")
                    DebugLogger.d(TAG, "状态: ${status?.name}")
                    DebugLogger.d(TAG, "图片大小: ${imageData?.size ?: 0} bytes")
                    
                    if (status == ValueUtil.CxrStatus.RESPONSE_SUCCEED && imageData != null) {
                        DebugLogger.d(TAG, "SDK 拍照成功，图片大小: ${imageData.size} bytes")
                        callback(imageData)
                    } else {
                        DebugLogger.e(TAG, "SDK 拍照失败: ${status?.name}")
                        callback(null)
                    }
                }
            })
            DebugLogger.d(TAG, "SDK takeGlassPhoto 指令已发送")
            DebugLogger.d(TAG, "════════════════════════════════════════")
        } catch (e: Exception) {
            DebugLogger.e(TAG, "发送拍照指令异常", e)
            callback(null)
        }
    }
    
    /**
     * 发送拍照命令到眼镜（备用方法）
     * 通过自定义消息通知眼镜拍照
     */
    fun sendTakePhotoCommand() {
        DebugLogger.d(TAG, "=== sendTakePhotoCommand() 被调用 ===")
        DebugLogger.d(TAG, "isConnected: ${isConnected()}")
        DebugLogger.d(TAG, "SDK isBluetoothConnected: ${cxrApi.isBluetoothConnected}")
        
        if (!isConnected()) {
            DebugLogger.w(TAG, "未连接，无法发送拍照命令")
            return
        }
        
        try {
            DebugLogger.d(TAG, "════════════════════════════════════════")
            DebugLogger.d(TAG, "准备发送拍照命令到眼镜...")
            val caps = Caps().apply {
                write("take_photo")
                write(System.currentTimeMillis().toString())
            }
            DebugLogger.d(TAG, "Caps 内容: take_photo, timestamp")
            val status = cxrApi.sendCustomCmd("take_photo", caps)
            DebugLogger.d(TAG, "拍照命令发送结果: ${status?.name}")
            DebugLogger.d(TAG, "status 对象: $status")
            DebugLogger.d(TAG, "════════════════════════════════════════")
        } catch (e: Exception) {
            DebugLogger.e(TAG, "发送拍照命令失败", e)
        }
    }
    
    /**
     * 设置眼镜亮度
     * 
     * @param brightness 亮度值 (0-100)
     */
    fun setGlassBrightness(brightness: Int): Boolean {
        val status = cxrApi.setGlassBrightness(brightness)
        Log.d(TAG, "设置眼镜亮度: $brightness, 结果: ${status?.name}")
        return status == ValueUtil.CxrStatus.RESPONSE_SUCCEED
    }
    
    /**
     * 设置眼镜音量
     * 
     * @param volume 音量值 (0-100)
     */
    fun setGlassVolume(volume: Int): Boolean {
        val status = cxrApi.setGlassVolume(volume)
        Log.d(TAG, "设置眼镜音量: $volume, 结果: ${status?.name}")
        return status == ValueUtil.CxrStatus.RESPONSE_SUCCEED
    }
    
    /**
     * 设置亮度变化监听
     */
    fun setBrightnessListener(listener: (Int) -> Unit) {
        cxrApi.setBrightnessUpdateListener(object : BrightnessUpdateListener {
            override fun onBrightnessUpdated(brightness: Int) {
                listener(brightness)
            }
        })
    }
    
    /**
     * 设置音量变化监听
     */
    fun setVolumeListener(listener: (Int) -> Unit) {
        cxrApi.setVolumeUpdateListener(object : VolumeUpdateListener {
            override fun onVolumeUpdated(volume: Int) {
                listener(volume)
            }
        })
    }
    
    /**
     * 同步眼镜时间
     */
    fun syncGlassTime(): Boolean {
        val status = cxrApi.setGlassTime()
        Log.d(TAG, "同步眼镜时间, 结果: ${status?.name}")
        return status == ValueUtil.CxrStatus.RESPONSE_SUCCEED
    }
    
    /**
     * 发送 TTS 语音内容到眼镜播报
     * 
     * @param text 要播报的文本
     */
    fun sendTtsContent(text: String): Boolean {
        val status = cxrApi.sendTtsContent(text)
        Log.d(TAG, "发送 TTS: $text, 结果: ${status?.name}")
        return status == ValueUtil.CxrStatus.RESPONSE_SUCCEED
    }
    
    /**
     * 发送全局 Toast 消息到眼镜
     * 
     * @param duration 显示时长 (毫秒)
     * @param content 消息内容
     * @param playSound 是否播放提示音
     */
    fun sendGlobalToast(duration: Int, content: String, playSound: Boolean = false): Boolean {
        val status = cxrApi.sendGlobalToastContent(duration, content, playSound)
        Log.d(TAG, "发送 Toast: $content, 结果: ${status?.name}")
        return status == ValueUtil.CxrStatus.RESPONSE_SUCCEED
    }
    
    /**
     * 发送全局 TTS 内容
     */
    fun sendGlobalTts(text: String): Boolean {
        val status = cxrApi.sendGlobalTtsContent(text)
        Log.d(TAG, "发送全局 TTS: $text, 结果: ${status?.name}")
        return status == ValueUtil.CxrStatus.RESPONSE_SUCCEED
    }
    
    /**
     * 通知眼镜无网络
     */
    fun notifyNoNetwork(): Boolean {
        val status = cxrApi.notifyNoNetwork()
        Log.d(TAG, "通知无网络, 结果: ${status?.name}")
        return status == ValueUtil.CxrStatus.RESPONSE_SUCCEED
    }
    
    /**
     * 通知眼镜图片上传错误
     */
    fun notifyPicUploadError(): Boolean {
        val status = cxrApi.notifyPicUploadError()
        Log.d(TAG, "通知图片上传错误, 结果: ${status?.name}")
        return status == ValueUtil.CxrStatus.RESPONSE_SUCCEED
    }
    
    /**
     * 通知眼镜 AI 处理错误
     */
    fun notifyAiError(): Boolean {
        val status = cxrApi.notifyAiError()
        Log.d(TAG, "通知 AI 错误, 结果: ${status?.name}")
        return status == ValueUtil.CxrStatus.RESPONSE_SUCCEED
    }
    
    /**
     * 通知眼镜 AI 开始处理
     */
    fun notifyAiStart(): Boolean {
        val status = cxrApi.notifyAiStart()
        Log.d(TAG, "通知 AI 开始, 结果: ${status?.name}")
        return status == ValueUtil.CxrStatus.RESPONSE_SUCCEED
    }
    
    // ==================== 处理阶段通知 ====================
    
    /**
     * 处理阶段枚举
     */
    enum class ProcessingPhase(val code: Int, val message: String) {
        UPLOADING(1, "上传中..."),
        ANALYZING(2, "识别菜品中..."),
        CALCULATING(3, "热量计算中..."),
        COMPLETE(4, "识别完成"),
        ERROR(5, "识别失败"),
        NOT_FOOD(6, "未检测到食物")
    }
    
    /**
     * 发送处理阶段状态到眼镜
     * 
     * 眼镜端可以根据这个状态更新 UI 动画
     * 
     * Caps 结构:
     * [0] String: 阶段代码（字符串形式）
     * [1] String: 阶段消息
     */
    fun sendProcessingPhase(phase: ProcessingPhase): Boolean {
        try {
            val caps = Caps().apply {
                write(phase.code.toString())  // 转为字符串
                write(phase.message)
            }
            cxrApi.sendCustomCmd(Config.MsgName.PROCESSING_PHASE, caps)
            Log.d(TAG, "发送处理阶段: ${phase.name} - ${phase.message}")
            
            // 不再发送 Toast，眼镜端有自己的 Compose UI 显示状态
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "发送处理阶段失败", e)
            return false
        }
    }
    
    /**
     * 通知眼镜上传开始
     */
    fun notifyUploading(): Boolean {
        return sendProcessingPhase(ProcessingPhase.UPLOADING)
    }
    
    /**
     * 通知眼镜正在分析
     */
    fun notifyAnalyzing(): Boolean {
        return sendProcessingPhase(ProcessingPhase.ANALYZING)
    }
    
    /**
     * 通知眼镜正在计算热量
     */
    fun notifyCalculating(): Boolean {
        return sendProcessingPhase(ProcessingPhase.CALCULATING)
    }
    
    /**
     * 通知眼镜处理完成
     */
    fun notifyComplete(): Boolean {
        return sendProcessingPhase(ProcessingPhase.COMPLETE)
    }
    
    /**
     * 通知眼镜未检测到食物
     */
    fun notifyNotFood(): Boolean {
        sendProcessingPhase(ProcessingPhase.NOT_FOOD)
        // 同时调用 SDK 的 AI 错误通知
        return notifyAiError()
    }
    
    /**
     * 设置眼镜屏幕关闭超时
     * 
     * @param timeout 超时时间 (毫秒)
     */
    fun setScreenOffTimeout(timeout: Long): Boolean {
        val status = cxrApi.setScreenOffTimeout(timeout)
        Log.d(TAG, "设置屏幕关闭超时: ${timeout}ms, 结果: ${status?.name}")
        return status == ValueUtil.CxrStatus.RESPONSE_SUCCEED
    }
    
    /**
     * 设置眼镜自动关机超时
     * 
     * @param timeout 超时时间 (分钟)
     */
    fun setPowerOffTimeout(timeout: Int): Boolean {
        val status = cxrApi.setPowerOffTimeout(timeout)
        Log.d(TAG, "设置自动关机超时: ${timeout}分钟, 结果: ${status?.name}")
        return status == ValueUtil.CxrStatus.RESPONSE_SUCCEED
    }
    
    /**
     * 通知眼镜重启
     */
    fun notifyGlassReboot(): Boolean {
        val status = cxrApi.notifyGlassReboot()
        Log.d(TAG, "通知眼镜重启, 结果: ${status?.name}")
        return status == ValueUtil.CxrStatus.RESPONSE_SUCCEED
    }
    
    /**
     * 通知眼镜关机
     */
    fun notifyGlassShutdown(): Boolean {
        val status = cxrApi.notifyGlassShutdown()
        Log.d(TAG, "通知眼镜关机, 结果: ${status?.name}")
        return status == ValueUtil.CxrStatus.RESPONSE_SUCCEED
    }
    
    /**
     * 检查是否有保存的连接凭证
     */
    fun hasSavedCredentials(): Boolean {
        return socketUuid != null && macAddress != null
    }
    
    /**
     * 获取保存的设备 MAC 地址
     */
    fun getSavedMacAddress(): String? = macAddress
}
