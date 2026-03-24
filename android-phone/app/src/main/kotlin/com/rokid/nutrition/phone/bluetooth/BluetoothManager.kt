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
        private const val KEY_DEVICE_NAME = "device_name"
        private const val KEY_LAST_CONNECT_TIME = "last_connect_time"
        
        // 自动重连配置
        private const val MAX_RECONNECT_ATTEMPTS = 3
        private const val RECONNECT_DELAY_MS = 2000L
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
    private var savedDeviceName: String? = null
    
    // 是否启用自动连接
    private var autoConnectEnabled = true
    
    // 重连计数器
    private var reconnectAttempts = 0
    
    // 是否正在自动重连
    private var isAutoReconnecting = false
    
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
                    
                    // 优先连接之前保存的设备（通过设备名称匹配，因为 BLE MAC 地址会随机变化）
                    val savedDeviceName = prefs.getString(KEY_DEVICE_NAME, null)
                    if (savedDeviceName != null && deviceName == savedDeviceName && autoConnectEnabled) {
                        DebugLogger.i(TAG, "发现之前连接过的设备（名称匹配），自动重新配对: $deviceName")
                        autoConnectEnabled = false
                        initBluetooth(device)
                    }
                    // 如果是 Glasses 开头的设备且没有保存的设备，自动连接
                    else if (isGlassesDevice && autoConnectEnabled && savedDeviceName == null) {
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
            
            // 重置重连计数器
            reconnectAttempts = 0
            isAutoReconnecting = false
            
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
            
            // 保存连接时间
            prefs.edit().putLong(KEY_LAST_CONNECT_TIME, System.currentTimeMillis()).apply()
            
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
            
            // 断开后只清除 socketUuid，保留 MAC 地址用于下次自动识别设备
            // socketUuid 在重新安装 APP 后会失效，需要重新配对获取
            Log.d(TAG, "断开连接，清除 socketUuid 但保留 MAC 地址")
            socketUuid = null
            prefs.edit().remove(KEY_SOCKET_UUID).apply()
        }
        
        override fun onFailed(errorCode: ValueUtil.CxrBluetoothErrorCode?) {
            DebugLogger.e(TAG, "=== 蓝牙连接失败 ===")
            DebugLogger.e(TAG, "错误码: ${errorCode?.name}")
            DebugLogger.e(TAG, "重连尝试次数: $reconnectAttempts / $MAX_RECONNECT_ATTEMPTS")
            
            // 如果是 SOCKET_CONNECT_FAILED，说明 socketUuid 已失效，需要重新配对
            if (errorCode?.name?.contains("SOCKET") == true) {
                Log.w(TAG, "Socket 连接失败，socketUuid 可能已失效")
                
                // 清除 socketUuid 但保留 MAC 地址（用于识别设备）
                socketUuid = null
                prefs.edit().remove(KEY_SOCKET_UUID).apply()
                
                // 自动重新扫描并配对
                if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                    reconnectAttempts++
                    isAutoReconnecting = true
                    Log.d(TAG, "自动重新扫描配对 (第 $reconnectAttempts 次)")
                    _connectionState.value = ConnectionState.Connecting
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        startScan(enableAutoConnect = true)
                    }, RECONNECT_DELAY_MS)
                } else {
                    Log.e(TAG, "已达到最大重连次数，停止重连")
                    isAutoReconnecting = false
                    reconnectAttempts = 0
                    _connectionState.value = ConnectionState.Error("连接失败，请手动重新连接")
                }
            } else {
                _connectionState.value = ConnectionState.Error("连接失败: ${errorCode?.name ?: "Unknown"}")
            }
        }
    }
    
    init {
        // 从本地存储恢复连接信息
        socketUuid = prefs.getString(KEY_SOCKET_UUID, null)
        macAddress = prefs.getString(KEY_MAC_ADDRESS, null)
        savedDeviceName = prefs.getString(KEY_DEVICE_NAME, null)
        
        Log.d(TAG, "初始化 BluetoothManager")
        Log.d(TAG, "已保存的 socketUuid: ${socketUuid?.take(8)}...")
        Log.d(TAG, "已保存的 macAddress: $macAddress")
        Log.d(TAG, "已保存的 deviceName: $savedDeviceName")
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
     * 自动连接：智能判断连接策略
     * 
     * 策略：
     * 1. 如果 SDK 已连接，直接返回
     * 2. 如果有完整凭证（socketUuid + macAddress），尝试快速重连
     * 3. 如果只有 macAddress（socketUuid 失效），扫描并自动配对该设备
     * 4. 如果没有任何凭证，扫描并自动连接 Glasses 开头的设备
     */
    fun autoConnect() {
        DebugLogger.i(TAG, "=== autoConnect() 被调用 ===")
        DebugLogger.i(TAG, "当前连接状态: ${_connectionState.value}")
        DebugLogger.i(TAG, "SDK isBluetoothConnected: ${cxrApi.isBluetoothConnected}")
        DebugLogger.i(TAG, "socketUuid: ${socketUuid?.take(8)}...")
        DebugLogger.i(TAG, "macAddress: $macAddress")
        DebugLogger.i(TAG, "savedDeviceName: $savedDeviceName")
        
        // 如果 SDK 报告已连接，更新状态并返回
        if (cxrApi.isBluetoothConnected) {
            Log.d(TAG, "SDK 报告已连接，更新状态")
            _connectionState.value = ConnectionState.Connected
            setupCustomCmdListener()
            return
        }
        
        // 如果正在连接中或自动重连中，不要重复操作
        if (_connectionState.value is ConnectionState.Connecting || 
            _connectionState.value is ConnectionState.Scanning ||
            isAutoReconnecting) {
            Log.d(TAG, "正在连接/扫描/重连中，跳过")
            return
        }
        
        // 策略判断
        if (hasSavedCredentials()) {
            // 有完整凭证，尝试快速重连
            DebugLogger.i(TAG, "有完整凭证，尝试快速重连...")
            connectWithCredentials()
        } else if (macAddress != null) {
            // 只有 MAC 地址（socketUuid 失效），扫描并自动配对
            DebugLogger.i(TAG, "socketUuid 失效，扫描并重新配对 MAC: $macAddress")
            startScan(enableAutoConnect = true)
        } else {
            // 没有任何凭证，扫描并自动连接
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
        
        // 提前保存设备信息（MAC 地址和名称）
        // 即使配对失败，下次也能识别这个设备
        device.name?.let { name ->
            savedDeviceName = name
            prefs.edit().putString(KEY_DEVICE_NAME, name).apply()
        }
        macAddress = device.address
        prefs.edit().putString(KEY_MAC_ADDRESS, device.address).apply()
        
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
     * [3] Int32:  图片类型: 0=自动拍照, 1=手动拍照, 2=结束用餐拍照
     * [4] Binary: 图片二进制数据
     */
    private fun handleImageReceived(caps: Caps?) {
        caps ?: return
        
        try {
            // 索引从 0 开始，与眼镜端发送格式一致
            val format = caps.at(0)?.string ?: "jpeg"
            val dataSize = caps.at(1)?.int ?: 0
            val timestamp = caps.at(2)?.long ?: System.currentTimeMillis()
            val imageType = caps.at(3)?.int ?: ImageType.MANUAL_CAPTURE
            
            // 兼容旧版本：如果 imageType 是 0 或 1，保持原有逻辑
            val isManualCapture = imageType != ImageType.AUTO_CAPTURE
            
            // 图片二进制数据在 Caps[4]
            val binaryValue = caps.at(4)?.binary ?: run {
                Log.e(TAG, "图片二进制数据为空 (Caps[4])")
                return
            }
            val imageData = binaryValue.data.copyOfRange(binaryValue.offset, binaryValue.offset + binaryValue.length)
            
            Log.d(TAG, "收到图片: format=$format, size=${imageData.size}, imageType=$imageType (0=自动, 1=手动, 2=结束用餐)")
            
            _receivedImage.tryEmit(ImageData(
                data = imageData,
                format = format,
                timestamp = timestamp,
                isManualCapture = isManualCapture,
                imageType = imageType
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
     * [6] String: 食物类型 (meal/snack/beverage/dessert/fruit)
     * 
     * @param result 营养结果
     * @param speakResult 是否通过 TTS 播报结果（默认 true）
     */
    fun sendNutritionResult(result: NutritionResult, speakResult: Boolean = true) {
        val caps = Caps().apply {
            write(result.foodName)
            writeFloat(result.calories.toFloat())
            writeFloat(result.protein.toFloat())
            writeFloat(result.carbs.toFloat())
            writeFloat(result.fat.toFloat())
            write(result.suggestion)
            write(result.category)  // 新增：食物类型
        }
        cxrApi.sendCustomCmd(Config.MsgName.RESULT, caps)
        Log.d(TAG, "发送营养结果: ${result.foodName}, category=${result.category}")
        
        // 通过 SDK 发送 TTS 到眼镜播报
        if (speakResult) {
            val ttsText = buildTtsText(result)
            sendGlobalTts(ttsText)
        }
    }
    
    /**
     * 构建 TTS 播报文本
     */
    private fun buildTtsText(result: NutritionResult): String {
        val caloriesInt = result.calories.toInt()
        val suggestion = result.suggestion.takeIf { it.isNotBlank() }
        
        return if (suggestion != null) {
            "${result.foodName}，${caloriesInt}千卡。$suggestion"
        } else {
            "${result.foodName}，${caloriesInt}千卡"
        }
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
     * 同步个性化建议到眼镜
     * 
     * 眼镜端会在分析等待阶段显示这条建议
     * 
     * Caps 结构:
     * [0] String: 建议内容
     * [1] String: 建议类型 (nutrition/timing/habit/warning/encouragement)
     * 
     * @param tipContent 建议内容
     * @param tipCategory 建议类型
     * @return true 发送成功
     */
    fun syncPersonalizedTip(tipContent: String, tipCategory: String = "nutrition"): Boolean {
        if (!isConnected()) {
            Log.w(TAG, "眼镜未连接，跳过同步个性化建议")
            return false
        }
        
        try {
            val caps = Caps().apply {
                write(tipContent)
                write(tipCategory)
            }
            cxrApi.sendCustomCmd(Config.MsgName.PERSONALIZED_TIP, caps)
            Log.d(TAG, "同步个性化建议: [$tipCategory] $tipContent")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "同步个性化建议失败", e)
            return false
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
        NOT_FOOD(6, "未识别到餐品")
    }
    
    /**
     * 错误类型枚举 - 用于发送具体错误信息到眼镜
     */
    enum class ErrorType(val code: Int, val defaultMessage: String, val ttsMessage: String) {
        NETWORK_ERROR(101, "网络连接失败", "网络连接失败，请检查手机网络"),
        NETWORK_TIMEOUT(102, "网络连接超时", "网络连接超时，请稍后重试"),
        SERVER_BUSY(103, "服务器繁忙", "服务器正在繁忙，请稍后重试"),
        SERVER_ERROR(104, "服务器错误", "服务器错误，请稍后重试"),
        NOT_FOOD(105, "未识别到餐品", "未识别到餐品，请重新拍摄"),
        IMAGE_UPLOAD_FAILED(106, "图片上传失败", "图片上传失败，请重试"),
        AI_ANALYSIS_FAILED(107, "AI分析失败", "AI分析失败，请重试"),
        UNKNOWN_ERROR(199, "未知错误", "识别失败，请重试")
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
     * 通知眼镜上传开始（同时播报 TTS）
     */
    fun notifyUploading(): Boolean {
        sendGlobalTts("已拍摄，正在分析")
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
     * 发送具体错误信息到眼镜
     * 
     * 使用 ERROR 阶段代码，但携带具体的错误消息
     * 眼镜端会根据消息内容播报具体原因
     * 
     * @param errorType 错误类型
     * @param customMessage 自定义消息（可选，默认使用 errorType 的 defaultMessage）
     */
    fun sendError(errorType: ErrorType, customMessage: String? = null): Boolean {
        val message = customMessage ?: errorType.defaultMessage
        try {
            val caps = Caps().apply {
                write(ProcessingPhase.ERROR.code.toString())
                write(message)
            }
            cxrApi.sendCustomCmd(Config.MsgName.PROCESSING_PHASE, caps)
            Log.d(TAG, "发送错误信息: ${errorType.name} - $message")
            
            // 同时发送 TTS 到眼镜播报具体原因
            sendGlobalTts(errorType.ttsMessage)
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "发送错误信息失败", e)
            return false
        }
    }
    
    /**
     * 通知网络错误
     */
    fun notifyNetworkError(): Boolean {
        sendError(ErrorType.NETWORK_ERROR)
        return notifyNoNetwork()
    }
    
    /**
     * 通知网络超时
     */
    fun notifyNetworkTimeout(): Boolean {
        return sendError(ErrorType.NETWORK_TIMEOUT)
    }
    
    /**
     * 通知服务器繁忙
     */
    fun notifyServerBusy(): Boolean {
        return sendError(ErrorType.SERVER_BUSY)
    }
    
    /**
     * 通知服务器错误
     */
    fun notifyServerError(): Boolean {
        return sendError(ErrorType.SERVER_ERROR)
    }
    
    /**
     * 通知图片上传失败
     */
    fun notifyImageUploadFailed(): Boolean {
        sendError(ErrorType.IMAGE_UPLOAD_FAILED)
        return notifyPicUploadError()
    }
    
    /**
     * 通知 AI 分析失败（带具体原因）
     */
    fun notifyAiAnalysisFailed(reason: String? = null): Boolean {
        sendError(ErrorType.AI_ANALYSIS_FAILED, reason)
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
    
    /**
     * 获取保存的设备名称
     */
    fun getSavedDeviceName(): String? = savedDeviceName
    
    /**
     * 检查是否有保存的设备信息（用于判断是否曾经连接过）
     */
    fun hasSavedDevice(): Boolean = macAddress != null
    
    /**
     * 重置重连状态（用于手动触发连接时）
     */
    fun resetReconnectState() {
        reconnectAttempts = 0
        isAutoReconnecting = false
        autoConnectEnabled = true
    }
}
