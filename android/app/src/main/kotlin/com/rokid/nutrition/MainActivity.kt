package com.rokid.nutrition

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.rokid.nutrition.bluetooth.BluetoothReceiver
import com.rokid.nutrition.bluetooth.BluetoothSender
import com.rokid.nutrition.bluetooth.NutritionResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val TAG = "MainActivity"

/**
 * 眼镜端主界面（瘦客户端）
 * 
 * 职责：
 * - 拍照并通过蓝牙发送给手机
 * - 接收手机返回的识别结果并显示
 * - 5分钟定时器自动拍照
 * - AR显示 + TTS播报
 */
class MainActivity : ComponentActivity() {

    private lateinit var rokidManager: RokidManager
    private lateinit var cameraManager: CameraManager
    private lateinit var bluetoothSender: BluetoothSender
    private lateinit var bluetoothReceiver: BluetoothReceiver

    private val uiState = UiState()
    private var isInMealSession = false
    private var autoMonitorJob: Job? = null
    private var mealTimerJob: Job? = null  // 用餐计时器
    private var resultAutoHideJob: Job? = null  // 结果自动隐藏
    private var lastFoodDetected = false  // 上次是否识别到餐品

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.all { it }
        Log.d(TAG, "Permissions granted: $granted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 永不熄屏
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        requestPermissionsIfNeeded()
        initializeManagers()
        startBatteryMonitor()
        
        setContent {
            RokidTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = RokidColors.Black) {
                    AppScreen(uiState = uiState)
                }
            }
        }
        
        // 开屏动画 3 秒后检查连接状态
        lifecycleScope.launch {
            delay(3000)
            // 如果已连接，直接进入 READY；否则进入 CONNECTING
            if (uiState.phoneConnected.value) {
                uiState.appPhase.value = AppPhase.READY
            } else {
                uiState.appPhase.value = AppPhase.CONNECTING
            }
        }
    }
    
    /**
     * 启动电量监控
     */
    private fun startBatteryMonitor() {
        lifecycleScope.launch {
            while (isActive) {
                updateBatteryLevel()
                delay(60000) // 每分钟更新一次
            }
        }
    }
    
    private fun updateBatteryLevel() {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
        val level = batteryManager?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
        uiState.batteryLevel.value = level
    }

    override fun onDestroy() {
        super.onDestroy()
        autoMonitorJob?.cancel()
        mealTimerJob?.cancel()
        resultAutoHideJob?.cancel()
        pendingClickJob?.cancel()
        cameraManager.release()
        rokidManager.release()
        // 不要释放蓝牙管理器！它们是 Application 级别的单例
        // bluetoothSender.release()
        // bluetoothReceiver.release()
        Log.d(TAG, "Activity onDestroy，蓝牙连接保持")
    }

    /**
     * 按键交互设计
     * 
     * 核心逻辑：
     * - 单击（触控板/音量+）：拍照识别
     * - 识别到餐食后：自动开始用餐监测
     * - 右滑：结束用餐（用餐中时）
     * - 音量-：重复上次结果
     */
    
    private var lastClickTime = 0L
    private var clickCount = 0
    private val DOUBLE_CLICK_INTERVAL = 400L  // 双击间隔
    private var pendingClickJob: Job? = null
    
    // 滑动手势检测
    private var touchStartX = 0f
    private var touchStartY = 0f
    private val SWIPE_THRESHOLD = 100f  // 滑动阈值
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.x
                touchStartY = event.y
                Log.d(TAG, "Touch DOWN: x=$touchStartX, y=$touchStartY")
                return true
            }
            MotionEvent.ACTION_UP -> {
                val deltaX = event.x - touchStartX
                val deltaY = event.y - touchStartY
                Log.d(TAG, "Touch UP: deltaX=$deltaX, deltaY=$deltaY, isInMealSession=$isInMealSession")
                
                // 检测右滑（水平滑动距离大于阈值，且大于垂直滑动）
                if (deltaX > SWIPE_THRESHOLD && Math.abs(deltaX) > Math.abs(deltaY)) {
                    Log.d(TAG, "检测到右滑手势")
                    if (isInMealSession) {
                        Log.d(TAG, "执行结束用餐")
                        handleEndMeal()
                        return true
                    } else {
                        Log.d(TAG, "不在用餐中，忽略右滑")
                    }
                }
                return false
            }
        }
        return super.onTouchEvent(event)
    }
    
    // Rokid 触控板手势通过 GenericMotionEvent 传递
    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        Log.d(TAG, "GenericMotion: action=${event.action}, x=${event.x}, y=${event.y}")
        return super.onGenericMotionEvent(event)
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.repeatCount > 0) return true  // 忽略重复按键
        
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_CAMERA,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_DPAD_CENTER -> {
                handleClick()
                true
            }
            
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                repeatLastResult()
                true
            }
            
            // 右滑手势（Rokid 触控板映射为 DPAD_RIGHT）
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (isInMealSession) {
                    handleEndMeal()
                }
                true
            }
            
            else -> super.onKeyDown(keyCode, event)
        }
    }
    
    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        // 不在 onKeyUp 处理，避免重复触发
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_CAMERA,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_DPAD_RIGHT -> true
            else -> super.onKeyUp(keyCode, event)
        }
    }
    
    /**
     * 处理点击（单击拍照）
     */
    private fun handleClick() {
        // 单击：拍照识别
        handleManualCapture()
    }
    
    /**
     * 重复播报上次结果
     */
    private fun repeatLastResult() {
        val foodName = uiState.foodName.value
        val calories = uiState.calories.value
        if (foodName.isNotBlank()) {
            rokidManager.speak("$foodName，${calories}千卡")
        } else {
            rokidManager.speak("暂无识别结果")
        }
    }
    
    /**
     * 取消当前操作
     */
    private fun cancelCurrentOperation() {
        if (uiState.isProcessing.value) {
            uiState.isProcessing.value = false
            uiState.statusMessage.value = "已取消"
            rokidManager.speak("已取消")
        }
    }

    private fun requestPermissionsIfNeeded() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_CONNECT
        )
        val needRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needRequest.isNotEmpty()) {
            permissionLauncher.launch(needRequest.toTypedArray())
        }
    }

    private fun initializeManagers() {
        // 初始化各管理器
        rokidManager = RokidManager.getInstance(this).apply { initialize() }
        cameraManager = CameraManager(this).apply {
            initialize(
                lifecycleOwner = this@MainActivity,
                onSuccess = { Log.d(TAG, "相机初始化成功") },
                onError = { e -> Log.e(TAG, "相机初始化失败", e) }
            )
        }
        
        // 使用 Application 级别的单例（确保 Activity 重启后连接状态保持）
        bluetoothSender = ViseatApplication.bluetoothSender
        bluetoothReceiver = ViseatApplication.bluetoothReceiver
        
        // 更新 UI 状态（Activity 重启时同步连接状态）
        val isConnectedNow = bluetoothSender.isConnected()
        val deviceName = bluetoothSender.getConnectedDeviceName()
        Log.d(TAG, "使用 Application 级别蓝牙管理器，当前连接状态: $isConnectedNow, 设备: $deviceName")
        
        uiState.phoneConnected.value = isConnectedNow
        if (isConnectedNow) {
            uiState.appPhase.value = AppPhase.READY
        }
        
        // 监听蓝牙连接状态
        bluetoothSender.setConnectionListener { connected, deviceName ->
            uiState.phoneConnected.value = connected
            if (connected) {
                uiState.appPhase.value = AppPhase.READY
                rokidManager.speak("已连接到手机")
            } else {
                uiState.appPhase.value = AppPhase.CONNECTING
                rokidManager.speak("手机已断开")
            }
        }
        
        // 监听 ARTC 连接质量
        bluetoothSender.setArtcHealthListener { health ->
            if (health < 0.3f) {
                uiState.statusMessage.value = "连接质量差"
            }
        }
        
        // 监听营养结果
        bluetoothReceiver.setResultListener { result ->
            handleNutritionResult(result)
        }
        
        // 监听会话状态
        bluetoothReceiver.setSessionStatusListener { status ->
            Log.d(TAG, "收到会话状态: ${status.status}, 消耗: ${status.totalConsumed}kcal")
            
            when (status.status) {
                "active" -> {
                    isInMealSession = true
                    uiState.sessionStatus.value = "用餐中"
                    mealStartTime = System.currentTimeMillis()
                    uiState.mealElapsedSeconds.value = 0
                    
                    // 重置已摄入热量（但保留首次识别的数据）
                    // mealTotalCalories 已在首次识别时累加，不重置
                    uiState.mealCurrentCalories.value = mealTotalCalories.toInt()
                    
                    // 启动用餐计时器
                    startMealTimer()
                    // 启动自动监测（5分钟后自动拍照）
                    startAutoMonitorTimer()
                    
                    // 允许屏幕自动熄灭（省电）
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    
                    Log.d(TAG, "用餐会话已开始（手机端确认），当前热量: ${mealTotalCalories.toInt()}kcal，允许屏幕熄灭")
                }
                "ended" -> {
                    isInMealSession = false
                    autoMonitorJob?.cancel()
                    mealTimerJob?.cancel()
                    uiState.sessionStatus.value = "空闲"
                    
                    // 计算用餐时长
                    val mealDurationMs = System.currentTimeMillis() - mealStartTime
                    val mealDurationMinutes = (mealDurationMs / 60000).toInt()
                    
                    // 更新总结数据
                    uiState.mealDurationMinutes.value = mealDurationMinutes
                    uiState.mealSummaryCalories.value = status.totalConsumed.toInt()
                    uiState.mealTotalProtein.value = mealTotalProtein.toInt()
                    uiState.mealTotalCarbs.value = mealTotalCarbs.toInt()
                    uiState.mealTotalFat.value = mealTotalFat.toInt()
                    uiState.mealSummaryMessage.value = generateMealSummary(status.totalConsumed)
                    
                    // 显示用餐总结
                    uiState.showMealSummary.value = true
                    
                    // 播报总结
                    val ttsText = "本餐共摄入${status.totalConsumed.toInt()}千卡。${uiState.mealSummaryMessage.value}"
                    rokidManager.speak(ttsText)
                    
                    // 重置累计数据
                    mealTotalCalories = 0.0
                    mealTotalProtein = 0.0
                    mealTotalCarbs = 0.0
                    mealTotalFat = 0.0
                    uiState.mealCurrentCalories.value = 0
                    
                    // 恢复屏幕常亮
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    
                    // 10 秒后自动关闭总结页面
                    lifecycleScope.launch {
                        delay(10000)
                        uiState.showMealSummary.value = false
                        uiState.foodName.value = ""
                        uiState.calories.value = 0
                    }
                    
                    Log.d(TAG, "用餐会话已结束，恢复屏幕常亮")
                }
            }
        }
        
        // 监听处理阶段状态（手机端发送的实时状态）
        bluetoothReceiver.setProcessingPhaseListener { phaseCode, phaseMessage ->
            Log.d(TAG, "收到处理阶段: code=$phaseCode, message=$phaseMessage")
            handleProcessingPhase(phaseCode, phaseMessage)
        }
        
        // 监听远程拍照命令（手机端触发眼镜拍照）
        bluetoothReceiver.setTakePhotoListener {
            Log.d(TAG, "收到远程拍照命令，执行拍照")
            handleRemoteCapture()
        }
        
        Log.d(TAG, "所有管理器初始化完成")
    }
    
    /**
     * 处理远程拍照命令（手机端触发）
     */
    private fun handleRemoteCapture() {
        lifecycleScope.launch {
            Log.d(TAG, "执行远程拍照...")
            captureAndSend(isManualCapture = true)
        }
    }

    /**
     * 处理用户主动拍照
     * - 重置自动拍照计时器
     * - 标记为主动拍照（评估更仔细）
     */
    private fun handleManualCapture() {
        lifecycleScope.launch {
            // 重置自动拍照计时器
            if (isInMealSession) {
                restartAutoMonitorTimer()
            }
            
            captureAndSend(isManualCapture = true)
        }
    }

    // 用餐期间累计数据
    private var mealTotalCalories = 0.0
    private var mealTotalProtein = 0.0
    private var mealTotalCarbs = 0.0
    private var mealTotalFat = 0.0
    private var mealStartTime = 0L  // 用餐开始时间
    
    /**
     * 结束用餐
     * 
     * 流程：
     * 1. 发送结束指令到手机端
     * 2. 等待手机端返回会话状态（通过 sessionStatusListener）
     * 3. 手机端返回后会触发 UI 更新
     */
    private fun handleEndMeal() {
        if (!isInMealSession) {
            Log.w(TAG, "当前没有进行中的用餐会话")
            return
        }
        
        lifecycleScope.launch {
            // 停止自动监测
            autoMonitorJob?.cancel()
            
            // 更新状态为"正在结束"
            uiState.statusMessage.value = "正在结束用餐..."
            
            // 发送结束用餐指令到手机端
            val sent = bluetoothSender.sendEndMealCommand()
            
            if (sent) {
                Log.d(TAG, "结束用餐指令已发送，等待手机端响应...")
                // 手机端会通过 sessionStatusListener 返回结果
                // UI 更新会在 sessionStatusListener 的回调中处理
                
                // 设置超时：10秒内没有响应则返回监测页面
                delay(10000)
                if (isInMealSession) {
                    Log.w(TAG, "结束用餐响应超时，返回监测页面")
                    uiState.statusMessage.value = "结束超时，继续监测"
                    rokidManager.speak("结束用餐超时，继续监测中")
                }
            } else {
                Log.e(TAG, "发送结束用餐指令失败")
                uiState.statusMessage.value = "结束失败，继续监测"
                rokidManager.speak("结束用餐失败，继续监测中")
                // 不显示总结，保持在监测状态
            }
        }
    }
    
    /**
     * 显示本地用餐总结（当手机端通信失败时使用）
     */
    private fun showLocalMealSummary() {
        lifecycleScope.launch {
            val mealDurationMs = System.currentTimeMillis() - mealStartTime
            val mealDurationMinutes = (mealDurationMs / 60000).toInt()
            
            uiState.mealDurationMinutes.value = mealDurationMinutes
            uiState.mealSummaryCalories.value = mealTotalCalories.toInt()
            uiState.mealTotalProtein.value = mealTotalProtein.toInt()
            uiState.mealTotalCarbs.value = mealTotalCarbs.toInt()
            uiState.mealTotalFat.value = mealTotalFat.toInt()
            
            val summaryText = "本餐共摄入${mealTotalCalories.toInt()}千卡，用时${mealDurationMinutes}分钟"
            uiState.mealSummaryMessage.value = summaryText
            
            isInMealSession = false
            uiState.sessionStatus.value = "空闲"
            uiState.showMealSummary.value = true
            
            rokidManager.speak(summaryText)
            
            delay(10000)
            uiState.showMealSummary.value = false
            uiState.foodName.value = ""
            uiState.calories.value = 0
            
            Log.d(TAG, "用餐已结束（本地）")
        }
    }

    /**
     * 启动5分钟自动拍照定时器
     */
    private fun startAutoMonitorTimer() {
        autoMonitorJob?.cancel()
        autoMonitorJob = lifecycleScope.launch {
            while (isActive && isInMealSession) {
                delay(Config.AUTO_CAPTURE_INTERVAL_MS)
                if (isInMealSession) {
                    Log.d(TAG, "自动拍照触发")
                    captureAndSend(isManualCapture = false)
                }
            }
        }
    }

    /**
     * 重置自动拍照计时器（用户主动拍照后重置）
     */
    private fun restartAutoMonitorTimer() {
        startAutoMonitorTimer()
    }
    
    /**
     * 启动用餐计时器（每秒更新）
     */
    private fun startMealTimer() {
        mealTimerJob?.cancel()
        uiState.mealElapsedSeconds.value = 0
        mealTimerJob = lifecycleScope.launch {
            while (isActive && isInMealSession) {
                delay(1000)
                uiState.mealElapsedSeconds.value++
            }
        }
    }
    
    /**
     * 启动结果自动隐藏计时器（15秒后隐藏）
     */
    private fun startResultAutoHideTimer() {
        resultAutoHideJob?.cancel()
        resultAutoHideJob = lifecycleScope.launch {
            delay(15000)
            // 15秒后隐藏结果（无论是否在用餐中）
            if (uiState.foodName.value.isNotEmpty()) {
                uiState.foodName.value = ""
                uiState.calories.value = 0
                uiState.suggestion.value = ""
                Log.d(TAG, "结果已自动隐藏")
            }
        }
    }

    /**
     * 拍照并发送到手机
     * 
     * 流程：
     * 1. 显示取景动画（手动拍照时）
     * 2. 拍照
     * 3. 发送到手机
     * 4. 等待手机端发送的真实处理阶段状态
     * 
     * @param isManualCapture true=用户主动拍照，false=自动拍照（静默模式）
     */
    private suspend fun captureAndSend(isManualCapture: Boolean) {
        if (!bluetoothSender.isConnected()) {
            if (isManualCapture) {
                uiState.statusMessage.value = "未连接手机"
            }
            return
        }
        
        // 自动拍照时静默处理，不显示处理动画
        if (isManualCapture) {
            // 阶段1: 取景中
            uiState.isProcessing.value = true
            uiState.processingPhase.value = ProcessingPhase.CAPTURING
            uiState.statusMessage.value = "取景中..."
            
            // 短暂显示取景框动画
            delay(200)
        } else {
            Log.d(TAG, "自动拍照开始（静默模式）")
        }
        
        try {
            // 拍照
            val bitmap = suspendCoroutine<Bitmap?> { cont ->
                cameraManager.takePicture { bmp, _ -> cont.resume(bmp) }
            }
            
            if (bitmap == null) {
                if (isManualCapture) {
                    uiState.processingPhase.value = ProcessingPhase.IDLE
                    uiState.isProcessing.value = false
                    uiState.statusMessage.value = "拍照失败"
                    rokidManager.speak("拍照失败")
                } else {
                    Log.w(TAG, "自动拍照失败")
                }
                return
            }
            
            // 拍照成功 - 手动拍照时播放快门音效
            if (isManualCapture) {
                playShutterSound()
                
                // 阶段2: 发送中
                uiState.processingPhase.value = ProcessingPhase.SENDING
                uiState.statusMessage.value = "发送中..."
            }
            
            // 压缩为 JPEG
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, Config.IMAGE_QUALITY, outputStream)
            val imageData = outputStream.toByteArray()
            
            // 通过蓝牙发送到手机
            val sent = bluetoothSender.sendImage(imageData, "jpeg", isManualCapture)
            
            if (sent) {
                // 发送成功，等待手机端发送的真实处理阶段状态
                Log.d(TAG, "图片已发送（${if (isManualCapture) "手动" else "自动"}），等待手机端处理...")
            } else {
                if (isManualCapture) {
                    uiState.processingPhase.value = ProcessingPhase.IDLE
                    uiState.isProcessing.value = false
                    uiState.statusMessage.value = "发送失败"
                    rokidManager.speak("发送失败")
                } else {
                    Log.w(TAG, "自动拍照发送失败")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "拍照发送失败", e)
            if (isManualCapture) {
                uiState.processingPhase.value = ProcessingPhase.IDLE
                uiState.isProcessing.value = false
                uiState.statusMessage.value = "拍照失败"
            }
        }
    }
    
    /**
     * 播放快门音效
     */
    private fun playShutterSound() {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            // 使用系统相机快门音效
            audioManager.playSoundEffect(android.media.AudioManager.FX_KEY_CLICK)
            // 或者使用 MediaActionSound 播放标准快门声
            val shutterSound = android.media.MediaActionSound()
            shutterSound.play(android.media.MediaActionSound.SHUTTER_CLICK)
        } catch (e: Exception) {
            Log.w(TAG, "播放快门音效失败", e)
        }
    }

    /**
     * 处理从手机接收到的处理阶段状态
     * 
     * 阶段代码:
     * 1 = 上传中
     * 2 = 识别菜品中
     * 3 = 热量计算中
     * 4 = 完成
     * 5 = 错误
     * 6 = 未检测到食物
     */
    private fun handleProcessingPhase(phaseCode: Int, phaseMessage: String) {
        when (phaseCode) {
            Config.ProcessingPhaseCode.UPLOADING -> {
                uiState.isProcessing.value = true
                uiState.processingPhase.value = ProcessingPhase.ANALYZING_FOOD
                uiState.statusMessage.value = phaseMessage
            }
            Config.ProcessingPhaseCode.ANALYZING -> {
                uiState.processingPhase.value = ProcessingPhase.ANALYZING_FOOD
                uiState.statusMessage.value = phaseMessage
            }
            Config.ProcessingPhaseCode.CALCULATING -> {
                uiState.processingPhase.value = ProcessingPhase.CALCULATING_CALORIES
                uiState.statusMessage.value = phaseMessage
            }
            Config.ProcessingPhaseCode.COMPLETE -> {
                // 完成状态会在 handleNutritionResult 中进一步处理
                uiState.statusMessage.value = phaseMessage
            }
            Config.ProcessingPhaseCode.ERROR -> {
                uiState.processingPhase.value = ProcessingPhase.IDLE
                uiState.isProcessing.value = false
                uiState.statusMessage.value = phaseMessage
                
                // 清除结果，返回首页
                uiState.foodName.value = ""
                uiState.calories.value = 0
                
                // 播报错误
                rokidManager.speak("识别失败，请重试")
                
                // 3秒后恢复初始状态
                lifecycleScope.launch {
                    delay(3000)
                    if (!uiState.isProcessing.value) {
                        uiState.statusMessage.value = if (isInMealSession) "用餐监测中" else "点击拍照识别食物"
                    }
                }
            }
            Config.ProcessingPhaseCode.NOT_FOOD -> {
                uiState.processingPhase.value = ProcessingPhase.IDLE
                uiState.isProcessing.value = false
                uiState.statusMessage.value = phaseMessage
                uiState.showNotFoodWarning.value = true
                rokidManager.speak("未检测到食物，请重新拍照")
                
                // 3秒后隐藏警告并恢复状态
                lifecycleScope.launch {
                    delay(3000)
                    uiState.showNotFoodWarning.value = false
                    if (!uiState.isProcessing.value) {
                        uiState.statusMessage.value = if (isInMealSession) "用餐监测中" else "点击拍照识别食物"
                    }
                }
            }
        }
    }

    /**
     * 处理从手机接收到的营养结果
     */
    private fun handleNutritionResult(result: NutritionResult) {
        // 停止处理动画
        uiState.processingPhase.value = ProcessingPhase.IDLE
        uiState.isProcessing.value = false
        
        // 检查是否识别到餐品
        val isFood = result.calories > 0 && result.foodName.isNotBlank() 
                     && !result.foodName.contains("未识别") 
                     && !result.foodName.contains("非餐品")
        
        if (!isFood) {
            // 未识别到餐品
            lastFoodDetected = false
            uiState.foodName.value = ""
            uiState.calories.value = 0
            uiState.suggestion.value = ""
            uiState.statusMessage.value = "未识别到餐品"
            uiState.showNotFoodWarning.value = true
            rokidManager.speak("未识别到餐品，请重新拍照")
            
            // 3秒后隐藏警告
            lifecycleScope.launch {
                delay(3000)
                uiState.showNotFoodWarning.value = false
            }
            return
        }
        
        // 识别到餐品
        lastFoodDetected = true
        uiState.showNotFoodWarning.value = false
        
        // 更新 UI 状态
        uiState.foodName.value = result.foodName
        uiState.calories.value = result.calories.toInt()
        uiState.protein.value = result.protein.toInt()
        uiState.carbs.value = result.carbs.toInt()
        uiState.fat.value = result.fat.toInt()
        uiState.suggestion.value = result.suggestion
        
        // 不再主动发送开始用餐指令
        // 手机端会根据食物类型（meal/snack/beverage等）决定是否开始用餐监测
        // 如果是正餐，手机端会通过 sessionStatusListener 发送 "active" 状态
        
        if (!isInMealSession) {
            uiState.statusMessage.value = "已识别"
            
            // TTS 播报
            val ttsText = "${result.foodName}，${result.calories.toInt()}千卡"
            rokidManager.speak(ttsText)
        } else {
            uiState.statusMessage.value = "已更新"
            
            // TTS 播报（简洁版）
            val ttsText = if (result.suggestion.isNotBlank()) {
                "${result.foodName}，${result.calories.toInt()}千卡。${result.suggestion}"
            } else {
                "${result.foodName}，${result.calories.toInt()}千卡"
            }
            rokidManager.speak(ttsText)
        }
        
        // 累加用餐数据
        mealTotalCalories += result.calories
        mealTotalProtein += result.protein
        mealTotalCarbs += result.carbs
        mealTotalFat += result.fat
        
        // 同步更新 UI 状态（用于监测页面显示）
        uiState.mealCurrentCalories.value = mealTotalCalories.toInt()
        
        // 15秒后自动隐藏结果（无论是否在用餐中）
        startResultAutoHideTimer()
    }
    
    /**
     * 开始用餐会话（内部调用，识别到餐食后自动触发）
     */
    private fun startMealSession() {
        if (!bluetoothSender.isConnected()) {
            Log.w(TAG, "未连接手机，无法开始用餐会话")
            return
        }
        
        // 发送开始用餐指令给手机
        bluetoothSender.sendStartMealCommand()
        
        // 记录用餐开始时间
        mealStartTime = System.currentTimeMillis()
        
        // 重置累计数据（但保留当前识别的数据）
        mealTotalCalories = 0.0
        mealTotalProtein = 0.0
        mealTotalCarbs = 0.0
        mealTotalFat = 0.0
        
        // 启动自动监测
        isInMealSession = true
        uiState.sessionStatus.value = "用餐中"
        startAutoMonitorTimer()
        
        Log.d(TAG, "用餐会话已自动开始")
    }
    
    /**
     * 生成用餐总结建议
     */
    private fun generateMealSummary(totalCalories: Double): String {
        return when {
            totalCalories < 400 -> "本餐摄入较少，注意营养均衡"
            totalCalories < 600 -> "本餐摄入适中，继续保持"
            totalCalories < 800 -> "本餐摄入正常，注意运动消耗"
            totalCalories < 1000 -> "本餐摄入较多，建议餐后散步"
            else -> "本餐摄入过多，建议增加运动量"
        }
    }
}

// ---------- UI 层 ----------
// Rokid YodaOS-Sprite 设计规范
// 显示区域: 480x640 可视安全, 480x400 建议显示
// 字体规范: 一级32sp, 二级24sp, 三级20sp, 四级18sp, 五级16sp
// 圆角: 12dp, 描边: 1.5dp

object RokidColors {
    val Black = Color(0xFF000000)
    val Green = Color(0xFF00FF66)      // 主色 - 官方绿
    val Cyan = Color(0xFF00CCFF)       // 信息色 - 蛋白质
    val White = Color(0xFFFFFFFF)
    val Gray = Color(0xFF666666)       // 辅助文字
    val DarkGray = Color(0xFF333333)
    val Red = Color(0xFFFF4444)        // 错误色
    val Yellow = Color(0xFFFFCC00)     // 警告色 - 碳水
    val Orange = Color(0xFFFF8800)     // 脂肪色
    
    // 透明度变体
    val GreenLight = Color(0x4D00FF66) // 30% 绿色
    val WhiteMedium = Color(0xCCFFFFFF) // 80% 白色
    val WhiteLight = Color(0x80FFFFFF)  // 50% 白色
}

/**
 * 处理阶段枚举
 */
enum class ProcessingPhase {
    IDLE,                    // 空闲
    CAPTURING,               // 取景/拍照中
    SENDING,                 // 发送中（图片已拍摄，正在发送到手机）
    ANALYZING_FOOD,          // 识别菜品中（手机端上传/分析）
    CALCULATING_CALORIES,    // 热量计算中
    CALCULATING_NUTRITION    // 营养分析中
}

/**
 * 应用阶段
 */
enum class AppPhase {
    SPLASH,      // 开屏动画 (3秒)
    CONNECTING,  // 连接手机中
    READY        // 已连接，正常使用
}

/**
 * UI 状态
 */
class UiState {
    // 应用阶段
    val appPhase = mutableStateOf(AppPhase.SPLASH)
    
    // 连接状态
    val phoneConnected = mutableStateOf(false)
    val sessionStatus = mutableStateOf("空闲")           // 空闲 / 用餐中
    val statusMessage = mutableStateOf("等待手机连接")
    
    // 处理状态
    val isProcessing = mutableStateOf(false)
    val processingPhase = mutableStateOf(ProcessingPhase.IDLE)
    val showNotFoodWarning = mutableStateOf(false)
    val showCaptureFlash = mutableStateOf(false)
    
    // 电量
    val batteryLevel = mutableStateOf(100)  // 电量百分比
    
    // 用餐监测计时（秒）
    val mealElapsedSeconds = mutableStateOf(0)
    
    // 用餐期间已摄入热量（实时更新）
    val mealCurrentCalories = mutableStateOf(0)
    
    // 结果自动隐藏计时
    val resultAutoHideJob = mutableStateOf<Job?>(null)
    
    // 主显示内容
    val foodName = mutableStateOf("")
    val calories = mutableStateOf(0)
    val suggestion = mutableStateOf("")
    
    // 营养详情
    val protein = mutableStateOf(0)
    val carbs = mutableStateOf(0)
    val fat = mutableStateOf(0)
    
    // 用餐总结（用餐结束时显示，10秒后自动退出）
    val showMealSummary = mutableStateOf(false)
    val mealSummaryCalories = mutableStateOf(0)
    val mealSummaryMessage = mutableStateOf("")
    val mealDurationMinutes = mutableStateOf(0)
    val mealTotalProtein = mutableStateOf(0)
    val mealTotalCarbs = mutableStateOf(0)
    val mealTotalFat = mutableStateOf(0)
}

@Composable
fun RokidTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}

/**
 * 应用主入口 - 根据 AppPhase 显示不同页面
 */
@Composable
fun AppScreen(uiState: UiState) {
    Crossfade(
        targetState = uiState.appPhase.value,
        label = "app_phase"
    ) { phase ->
        when (phase) {
            AppPhase.SPLASH -> SplashScreen()
            AppPhase.CONNECTING -> ConnectingScreen(uiState)
            AppPhase.READY -> MainScreen(uiState)
        }
    }
}

/**
 * 开屏动画 - 品牌展示 3 秒
 */
@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RokidColors.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Logo 呼吸动画
            val infiniteTransition = rememberInfiniteTransition(label = "splash")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )
            
            Text(
                text = "VISEAT",
                color = RokidColors.Green.copy(alpha = alpha),
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "食 智",
                color = RokidColors.WhiteMedium.copy(alpha = alpha),
                fontSize = 24.sp,
                letterSpacing = 12.sp
            )
        }
    }
}

/**
 * 连接页面 - 等待手机连接
 */
@Composable
fun ConnectingScreen(uiState: UiState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RokidColors.Black)
            .padding(top = 80.dp, bottom = 80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 连接动画
            val infiniteTransition = rememberInfiniteTransition(label = "connecting")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing)
                ),
                label = "rotation"
            )
            
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .graphicsLayer { rotationZ = rotation }
                    .border(3.dp, RokidColors.Green, CircleShape)
                    .border(3.dp, Color.Transparent.copy(alpha = 0.3f), CircleShape)
            ) {
                // 旋转的弧线
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = RokidColors.Green,
                        startAngle = 0f,
                        sweepAngle = 90f,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "等待手机连接",
                color = RokidColors.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "请打开 VISEAT 手机端应用",
                color = RokidColors.Gray,
                fontSize = 16.sp
            )
        }
        
        // 电量显示（右下角）
        BatteryIndicator(
            level = uiState.batteryLevel.value,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 8.dp)
        )
    }
}

/**
 * 电量指示器
 */
@Composable
fun BatteryIndicator(level: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 电池图标
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(12.dp)
                .border(1.dp, RokidColors.Gray, RoundedCornerShape(2.dp))
                .padding(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(level / 100f)
                    .background(
                        when {
                            level <= 20 -> RokidColors.Red
                            level <= 50 -> RokidColors.Yellow
                            else -> RokidColors.Green
                        },
                        RoundedCornerShape(1.dp)
                    )
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$level%",
            color = RokidColors.Gray,
            fontSize = 12.sp
        )
    }
}

/**
 * 主界面 - 遵循 Rokid YodaOS-Sprite 设计规范
 * 
 * 布局规范:
 * ┌─────────────────────────────────────┐
 * │         ↑ 80dp 安全区域 ↑           │  <- 顶部避开区域
 * │  [连接状态]              [会话状态]  │  <- 状态栏 (16sp)
 * │─────────────────────────────────────│
 * │                                     │
 * │           红烧肉 · 米饭             │  <- 食物名称 (24sp)
 * │              650                    │  <- 热量数字 (56sp)
 * │              千卡                   │  <- 单位 (18sp)
 * │  ┌─────────────────────────────┐   │
 * │  │ 蛋白质25g  碳水80g  脂肪28g │   │  <- 营养卡片 (20sp)
 * │  └─────────────────────────────┘   │
 * │       ┌───────────────────┐        │
 * │       │ "建议搭配蔬菜"    │        │  <- 建议卡片 (16sp)
 * │       └───────────────────┘        │
 * │─────────────────────────────────────│
 * │     ┌─────────────────────┐        │  <- 操作提示 (16sp)
 * │     │ 长按开始用餐        │        │
 * │     └─────────────────────┘        │
 * │         ↓ 80dp 安全区域 ↓           │  <- 底部避开区域
 * └─────────────────────────────────────┘
 */
@Composable
fun MainScreen(uiState: UiState) {
    val screenState = getScreenState(uiState)
    val isInSession = uiState.sessionStatus.value == "用餐中"
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RokidColors.Black)
    ) {
        // 中央主内容区 - 在安全区域内
        CenterContent(
            uiState = uiState,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 80.dp, bottom = 120.dp)  // 给底部状态栏留更多空间
        )
        
        // 底部状态栏 - 固定在底部安全区域边缘
        BottomStatusBar(
            screenState = screenState,
            isInSession = isInSession,
            batteryLevel = uiState.batteryLevel.value,
            mealElapsedSeconds = uiState.mealElapsedSeconds.value,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 60.dp)  // 在安全区域边缘
        )
    }
}

/**
 * 底部状态栏 - 整合用餐监测、提示和电量
 */
@Composable
fun BottomStatusBar(
    screenState: ScreenState,
    isInSession: Boolean,
    batteryLevel: Int,
    mealElapsedSeconds: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：用餐监测计时（仅用餐中显示）或操作提示
        if (isInSession) {
            MealTimerIndicator(elapsedSeconds = mealElapsedSeconds)
        } else {
            // 空闲时显示操作提示
            val hintText = when (screenState) {
                ScreenState.IDLE -> "单击拍照"
                ScreenState.RESULT -> "单击继续"
                else -> ""
            }
            if (hintText.isNotEmpty()) {
                Text(
                    text = hintText,
                    color = RokidColors.Gray,
                    fontSize = 12.sp
                )
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }
        }
        
        // 右侧：电量显示
        BatteryIndicator(level = batteryLevel)
    }
}

/**
 * 用餐计时指示器 - 与电量指示器统一风格，包含滑动提示
 */
@Composable
fun MealTimerIndicator(elapsedSeconds: Int, modifier: Modifier = Modifier) {
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier
                .border(1.dp, RokidColors.Gray, RoundedCornerShape(2.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 绿色圆点
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(RokidColors.Green, CircleShape)
            )
            Spacer(modifier = Modifier.width(4.dp))
            // 用餐中 + 计时
            Text(
                text = "用餐中",
                color = RokidColors.Green,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = String.format("%02d:%02d", minutes, seconds),
                color = RokidColors.Gray,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        // 滑动提示
        Text(
            text = "→ 滑动结束用餐",
            color = RokidColors.Gray.copy(alpha = 0.6f),
            fontSize = 10.sp
        )
    }
}

/**
 * 智能底部提示 - 根据状态显示不同内容（保留兼容）
 */
@Composable
fun SmartBottomHint(screenState: ScreenState, isInSession: Boolean) {
    val hintText = when {
        screenState == ScreenState.IDLE -> "单击拍照识别"
        isInSession -> "双击结束用餐"
        else -> "单击继续识别"
    }
    
    Box(
        modifier = Modifier
            .border(1.dp, RokidColors.Gray.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            text = hintText,
            color = RokidColors.WhiteLight,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

/**
 * 顶部状态栏 - 简洁的状态指示
 * 规范: 使用五级字号 16sp，位于安全区域内
 */
@Composable
fun TopStatusBar(
    isConnected: Boolean,
    sessionStatus: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),  // 贴边设计
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 连接状态
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        if (isConnected) RokidColors.Green else RokidColors.Red,
                        CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isConnected) "已连接" else "未连接",
                color = if (isConnected) RokidColors.Green else RokidColors.Gray,
                fontSize = 16.sp,  // 五级字号
                fontWeight = FontWeight.Normal
            )
        }
        
        // 会话状态标签
        Box(
            modifier = Modifier
                .background(
                    color = when (sessionStatus) {
                        "用餐中" -> RokidColors.Yellow.copy(alpha = 0.2f)
                        else -> Color.Transparent
                    },
                    shape = RoundedCornerShape(12.dp)  // 规范圆角
                )
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = sessionStatus,
                color = when (sessionStatus) {
                    "用餐中" -> RokidColors.Yellow
                    else -> RokidColors.Gray
                },
                fontSize = 16.sp,  // 五级字号
                fontWeight = if (sessionStatus == "用餐中") FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

/**
 * UI 状态枚举
 */
enum class ScreenState {
    IDLE,           // 初始状态（品牌标题）
    PROCESSING,     // 处理中
    RESULT,         // 显示结果
    NOT_FOOD,       // 未识别到餐品
    MONITORING,     // 用餐监测中（结果隐藏后）
    MEAL_SUMMARY    // 用餐总结
}

/**
 * 计算当前屏幕状态
 */
fun getScreenState(uiState: UiState): ScreenState {
    return when {
        uiState.showMealSummary.value -> ScreenState.MEAL_SUMMARY
        uiState.showNotFoodWarning.value -> ScreenState.NOT_FOOD
        uiState.isProcessing.value -> ScreenState.PROCESSING
        uiState.foodName.value.isNotEmpty() -> ScreenState.RESULT
        // 用餐中且结果已隐藏，显示监测状态
        uiState.sessionStatus.value == "用餐中" -> ScreenState.MONITORING
        else -> ScreenState.IDLE
    }
}

/**
 * 中央主内容区 - 使用 Crossfade 确保视图正确切换
 */
@Composable
fun CenterContent(uiState: UiState, modifier: Modifier = Modifier) {
    val screenState = getScreenState(uiState)
    
    Crossfade(
        targetState = screenState,
        modifier = modifier.padding(horizontal = 24.dp),
        label = "screen_transition"
    ) { state ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (state) {
                ScreenState.MEAL_SUMMARY -> {
                    MealSummaryDisplay(
                        totalCalories = uiState.mealSummaryCalories.value,
                        protein = uiState.mealTotalProtein.value,
                        carbs = uiState.mealTotalCarbs.value,
                        fat = uiState.mealTotalFat.value,
                        durationMinutes = uiState.mealDurationMinutes.value,
                        summaryMessage = uiState.mealSummaryMessage.value,
                        onDismiss = { uiState.showMealSummary.value = false }
                    )
                }
                
                ScreenState.NOT_FOOD -> {
                    NotFoodWarning()
                }
                
                ScreenState.PROCESSING -> {
                    ProcessingAnimation(
                        phase = uiState.processingPhase.value,
                        statusMessage = uiState.statusMessage.value
                    )
                }
                
                ScreenState.RESULT -> {
                    ResultDisplay(uiState = uiState)
                }
                
                ScreenState.MONITORING -> {
                    // 用餐监测中状态
                    MonitoringDisplay(
                        elapsedSeconds = uiState.mealElapsedSeconds.value,
                        totalCalories = uiState.mealCurrentCalories.value
                    )
                }
                
                ScreenState.IDLE -> {
                    // 首页仅显示品牌标识，极简设计
                    BrandTitle()
                }
            }
        }
    }
}

/**
 * 用餐监测中显示
 */
@Composable
fun MonitoringDisplay(elapsedSeconds: Int, totalCalories: Int = 0) {
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    
    // 脉冲动画
    val infiniteTransition = rememberInfiniteTransition(label = "monitoring")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 已摄入热量 - 大字显示
        Text(
            text = "${totalCalories}",
            color = RokidColors.Green,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "kcal 已摄入",
            color = RokidColors.Green.copy(alpha = 0.8f),
            fontSize = 16.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 监测图标 - 脉冲动画（缩小）
        Box(
            modifier = Modifier
                .size(50.dp)
                .alpha(pulse)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            RokidColors.Green.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(RokidColors.Green.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(RokidColors.Green, CircleShape)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 状态文字
        Text(
            text = "持续监测热量中",
            color = RokidColors.Gray,
            fontSize = 16.sp
        )
        
        // 计时显示
        Text(
            text = String.format("%02d:%02d", minutes, seconds),
            color = RokidColors.Gray.copy(alpha = 0.7f),
            fontSize = 14.sp
        )
    }
}

/**
 * VISEAT 品牌标题 + 今日信息 + 暖心建议
 * 规范: 一级字号 32sp 用于品牌，但 Logo 可适当放大
 */
@Composable
fun BrandTitle() {
    // 根据时间生成暖心建议
    val greeting = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..9 -> "早安，美好的一天从健康早餐开始"
            in 10..11 -> "上午好，记得补充水分"
            in 12..13 -> "午餐时间，均衡饮食更健康"
            in 14..17 -> "下午好，来点健康小食吧"
            in 18..20 -> "晚餐时间，清淡饮食助睡眠"
            in 21..23 -> "夜宵要适量，健康最重要"
            else -> "深夜了，注意休息"
        }
    }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Logo 文字 - 使用一级字号的 1.5 倍（品牌特殊处理）
        Text(
            text = "VISEAT",
            color = RokidColors.Green,
            fontSize = 40.sp,  // 稍微缩小，为建议留空间
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        // 中文名 - 使用二级字号
        Text(
            text = "食 智",
            color = RokidColors.WhiteMedium,
            fontSize = 20.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 12.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 暖心建议 - 线框样式
        Box(
            modifier = Modifier
                .border(1.dp, RokidColors.Green.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = greeting,
                color = RokidColors.Green.copy(alpha = 0.8f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 科技感处理动画
 */
@Composable
fun ProcessingAnimation(phase: ProcessingPhase, statusMessage: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "processing")
    
    // 旋转动画
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    // 脉冲动画
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 取景框动画
        if (phase == ProcessingPhase.CAPTURING) {
            ViewfinderAnimation()
        } else {
            // 科技感圆环动画
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                // 外圈旋转
                Canvas(
                    modifier = Modifier
                        .size(80.dp)
                        .graphicsLayer { rotationZ = rotation }
                ) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color.Transparent,
                                RokidColors.Cyan,
                                RokidColors.Green
                            )
                        ),
                        startAngle = 0f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                
                // 内圈脉冲
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .alpha(pulse)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    RokidColors.Green.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                
                // 阶段图标
                Text(
                    text = when (phase) {
                        ProcessingPhase.SENDING -> "📤"
                        ProcessingPhase.ANALYZING_FOOD -> "🍽️"
                        ProcessingPhase.CALCULATING_CALORIES -> "🔥"
                        ProcessingPhase.CALCULATING_NUTRITION -> "🧪"
                        else -> "⚙️"
                    },
                    fontSize = 28.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // 状态文字
        Text(
            text = statusMessage,
            color = RokidColors.Cyan,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
        )
        
        // 进度指示器
        Spacer(modifier = Modifier.height(12.dp))
        ProcessingProgressBar(phase = phase)
    }
}

/**
 * 取景框动画
 */
@Composable
fun ViewfinderAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "viewfinder")
    
    // 边框闪烁
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border"
    )
    
    // 扫描线
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanLine"
    )
    
    Box(
        modifier = Modifier
            .size(120.dp)
            .border(
                width = 3.dp,
                color = RokidColors.Green.copy(alpha = borderAlpha),
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // 四角标记
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cornerLength = 20.dp.toPx()
            val strokeWidth = 4.dp.toPx()
            val color = RokidColors.Green
            
            // 左上角
            drawLine(color, Offset(0f, cornerLength), Offset(0f, 0f), strokeWidth)
            drawLine(color, Offset(0f, 0f), Offset(cornerLength, 0f), strokeWidth)
            
            // 右上角
            drawLine(color, Offset(size.width - cornerLength, 0f), Offset(size.width, 0f), strokeWidth)
            drawLine(color, Offset(size.width, 0f), Offset(size.width, cornerLength), strokeWidth)
            
            // 左下角
            drawLine(color, Offset(0f, size.height - cornerLength), Offset(0f, size.height), strokeWidth)
            drawLine(color, Offset(0f, size.height), Offset(cornerLength, size.height), strokeWidth)
            
            // 右下角
            drawLine(color, Offset(size.width - cornerLength, size.height), Offset(size.width, size.height), strokeWidth)
            drawLine(color, Offset(size.width, size.height - cornerLength), Offset(size.width, size.height), strokeWidth)
            
            // 扫描线
            val lineY = size.height * scanLineY
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        RokidColors.Green,
                        Color.Transparent
                    )
                ),
                start = Offset(10f, lineY),
                end = Offset(size.width - 10f, lineY),
                strokeWidth = 2.dp.toPx()
            )
        }
        
        // 中心十字
        Text(
            text = "+",
            color = RokidColors.Green.copy(alpha = 0.7f),
            fontSize = 32.sp,
            fontWeight = FontWeight.Light
        )
    }
}

/**
 * 处理进度条
 */
@Composable
fun ProcessingProgressBar(phase: ProcessingPhase) {
    val progress = when (phase) {
        ProcessingPhase.IDLE -> 0f
        ProcessingPhase.CAPTURING -> 0.1f
        ProcessingPhase.SENDING -> 0.25f
        ProcessingPhase.ANALYZING_FOOD -> 0.5f
        ProcessingPhase.CALCULATING_CALORIES -> 0.75f
        ProcessingPhase.CALCULATING_NUTRITION -> 0.9f
    }
    
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300),
        label = "progress"
    )
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(4) { index ->
            val segmentProgress = (animatedProgress - index * 0.25f).coerceIn(0f, 0.25f) / 0.25f
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(
                        color = if (segmentProgress > 0) {
                            RokidColors.Cyan.copy(alpha = 0.3f + segmentProgress * 0.7f)
                        } else {
                            RokidColors.DarkGray
                        },
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

/**
 * 用餐总结显示 - 带饼状图和用餐时长
 */
@Composable
fun MealSummaryDisplay(
    totalCalories: Int,
    protein: Int,
    carbs: Int,
    fat: Int,
    durationMinutes: Int,
    summaryMessage: String,
    onDismiss: () -> Unit
) {
    val animatedCalories by animateFloatAsState(
        targetValue = totalCalories.toFloat(),
        animationSpec = tween(durationMillis = 800),
        label = "summaryCalories"
    )
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：饼状图
        NutrientPieChart(
            protein = protein,
            carbs = carbs,
            fat = fat,
            modifier = Modifier.size(140.dp)
        )
        
        // 右侧：数据信息
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 标题
            Text(
                text = "🍽️ 用餐结束",
                fontSize = 24.sp,
                color = RokidColors.Green,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 用餐时长
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "⏱️", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (durationMinutes > 0) "${durationMinutes}分钟" else "<1分钟",
                    color = RokidColors.Gray,
                    fontSize = 16.sp
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 总热量
            Text(
                text = animatedCalories.toInt().toString(),
                color = RokidColors.Green,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "千卡",
                color = RokidColors.Green.copy(alpha = 0.7f),
                fontSize = 16.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 营养成分数值
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NutrientMiniItem("蛋白", protein, RokidColors.Cyan)
                NutrientMiniItem("碳水", carbs, RokidColors.Yellow)
                NutrientMiniItem("脂肪", fat, RokidColors.Orange)
            }
        }
    }
    
    Spacer(modifier = Modifier.height(12.dp))
    
    // 建议 - 线框样式
    Box(
        modifier = Modifier
            .border(1.dp, RokidColors.Green.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = summaryMessage,
            color = RokidColors.Green,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    
    // 提示
    Text(
        text = "短按任意键继续",
        color = RokidColors.Gray,
        fontSize = 12.sp
    )
    
    // 8秒后自动关闭
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(8000)
        onDismiss()
    }
}

/**
 * 营养成分饼状图
 */
@Composable
fun NutrientPieChart(
    protein: Int,
    carbs: Int,
    fat: Int,
    modifier: Modifier = Modifier
) {
    val total = (protein + carbs + fat).coerceAtLeast(1)
    
    // 动画进度
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1000),
        label = "pieProgress"
    )
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 20.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)
            
            // 计算角度
            val proteinAngle = (protein.toFloat() / total) * 360f * animatedProgress
            val carbsAngle = (carbs.toFloat() / total) * 360f * animatedProgress
            val fatAngle = (fat.toFloat() / total) * 360f * animatedProgress
            
            var startAngle = -90f
            
            // 蛋白质 - 青色
            drawArc(
                color = RokidColors.Cyan,
                startAngle = startAngle,
                sweepAngle = proteinAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )
            startAngle += proteinAngle
            
            // 碳水 - 黄色
            drawArc(
                color = RokidColors.Yellow,
                startAngle = startAngle,
                sweepAngle = carbsAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )
            startAngle += carbsAngle
            
            // 脂肪 - 橙色
            drawArc(
                color = RokidColors.Orange,
                startAngle = startAngle,
                sweepAngle = fatAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )
        }
        
        // 中心文字
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "营养",
                color = RokidColors.Gray,
                fontSize = 12.sp
            )
            Text(
                text = "占比",
                color = RokidColors.Gray,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * 迷你营养项（用于总结页面）
 */
@Composable
fun NutrientMiniItem(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "${value}g",
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = label,
            color = color.copy(alpha = 0.7f),
            fontSize = 10.sp
        )
    }
}

/**
 * 非餐品警告
 */
@Composable
fun NotFoodWarning() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "⚠️",
            fontSize = 48.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "未识别到餐品",
            color = RokidColors.Orange,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "请重新拍照",
            color = RokidColors.Gray,
            fontSize = 18.sp
        )
    }
}

/**
 * 识别结果显示
 * 规范: 食物名称用二级字号24sp, 热量数字用特殊大号, 营养用四级字号18sp
 */
@Composable
fun ResultDisplay(uiState: UiState) {
    val targetCalories = uiState.calories.value.coerceAtLeast(0)
    val animatedCalories by animateFloatAsState(
        targetValue = targetCalories.toFloat(),
        animationSpec = tween(durationMillis = 600),
        label = "calories"
    )

    // 食物名称 - 二级字号 24sp
    Text(
        text = uiState.foodName.value,
        color = RokidColors.White,
        fontSize = 24.sp,  // 二级字号
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center
    )
    
    Spacer(modifier = Modifier.height(12.dp))
    
    // 热量数字 - 核心数据，使用大号字体
    Text(
        text = animatedCalories.toInt().toString(),
        color = RokidColors.Green,
        fontSize = 56.sp,  // 热量数字特殊处理，略大于一级
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "千卡",
        color = RokidColors.Green.copy(alpha = 0.7f),
        fontSize = 18.sp  // 四级字号
    )
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // 营养详情 - 线框样式
    Row(
        modifier = Modifier
            .border(1.dp, RokidColors.Gray.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        NutrientItem("蛋白质", uiState.protein.value, RokidColors.Cyan)
        NutrientItem("碳水", uiState.carbs.value, RokidColors.Yellow)
        NutrientItem("脂肪", uiState.fat.value, RokidColors.Orange)
    }
    
    // 建议（淡入淡出）- 线框样式
    AnimatedVisibility(
        visible = uiState.suggestion.value.isNotEmpty(),
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(200))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .border(1.dp, RokidColors.Green.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "\"${uiState.suggestion.value}\"",
                    color = RokidColors.Green,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * 营养项显示
 * 规范: 数值用四级字号18sp, 标签用五级字号16sp
 */
@Composable
fun NutrientItem(label: String, value: Int, color: Color = RokidColors.White) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "${value}g",
            color = color,
            fontSize = 20.sp,  // 三级字号
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = color.copy(alpha = 0.7f),
            fontSize = 14.sp  // 提示字号
        )
    }
}

/**
 * 底部操作提示
 * 规范: 使用五级字号16sp，避开底部80px不佳区域
 */
@Composable
fun BottomHint(
    statusMessage: String,
    isInSession: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 操作提示 - 使用卡片样式增强可读性
        Box(
            modifier = Modifier
                .background(
                    color = RokidColors.DarkGray.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = if (isInSession) {
                    "用餐监测中 · 长按结束"
                } else {
                    "单击拍照识别"
                },
                color = if (isInSession) RokidColors.Green else RokidColors.WhiteLight,
                fontSize = 16.sp,  // 五级字号
                fontWeight = FontWeight.Normal
            )
        }
    }
}

/**
 * 版本标识
 * 规范: 使用提示字号14sp，低调显示
 */
@Composable
fun VersionBadge(modifier: Modifier = Modifier) {
    Text(
        text = "VISEAT v1.0",
        color = RokidColors.Gray.copy(alpha = 0.5f),
        fontSize = 14.sp,  // 提示字号
        fontWeight = FontWeight.Normal,
        modifier = modifier
    )
}
