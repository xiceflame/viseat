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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
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
private const val SCREEN_TIMEOUT_MS = 30000L  // 30秒直接熄屏

/**
 * 眼镜端主界面（瘦客户端）
 * 
 * 职责：
 * - 拍照并通过蓝牙发送给手机
 * - 接收手机返回的识别结果并显示
 * - 5分钟定时器自动拍照
 * - AR显示 + TTS播报
 * - 15秒无操作自动熄屏（省电）
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
    private var recognitionTimeoutJob: Job? = null  // 识别超时计时器
    private var screenTimeoutJob: Job? = null  // 屏幕熄灭计时器
    private var lastFoodDetected = false  // 上次是否识别到餐品
    private var currentRetryCount = 0  // 当前重试次数
    private var isWaitingForResult = false  // 是否正在等待识别结果
    private var isScreenOff = false  // 屏幕是否已熄灭
     private var endMealCountdownJob: Job? = null  // 结束用餐倒计时任务
     private var isEndMealPending = false  // 是否正在等待结束用餐确认

     private var lastResultOverlayJob: Job? = null

     private var lastResultFoodName = ""
     private var lastResultCalories = 0
     private var lastResultSuggestion = ""


    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.all { it }
        Log.d(TAG, "Permissions granted: $granted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始保持屏幕常亮，之后通过计时器控制熄屏
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        requestPermissionsIfNeeded()
        initializeManagers()
        startBatteryMonitor()
        
        // 启动屏幕熄灭计时器（15秒无操作后熄屏）
        startScreenTimeout()
        
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
        recognitionTimeoutJob?.cancel()
        screenTimeoutJob?.cancel()
        pendingClickJob?.cancel()
        endMealCountdownJob?.cancel()
        lastResultOverlayJob?.cancel()
        cameraManager.release()
        rokidManager.release()
        // 不要释放蓝牙管理器！它们是 Application 级别的单例
        // bluetoothSender.release()
        // bluetoothReceiver.release()
        Log.d(TAG, "Activity onDestroy，蓝牙连接保持")
    }
    
    /**
     * 启动屏幕熄灭计时器（15秒无操作后熄屏）
     */
    private fun startScreenTimeout() {
        // 用餐监测中不熄屏
        if (isInMealSession) {
            Log.d(TAG, "用餐监测中，跳过熄屏计时")
            return
        }
        
        screenTimeoutJob?.cancel()
        screenTimeoutJob = lifecycleScope.launch {
            delay(SCREEN_TIMEOUT_MS)
            if (!isInMealSession && !uiState.isProcessing.value) {
                turnOffScreen()
            }
        }
    }
    
    /**
     * 重置屏幕熄灭计时器（有用户操作时调用）
     */
    private fun resetScreenTimeout() {
        // 如果屏幕已熄灭，先唤醒
        if (isScreenOff) {
            turnOnScreen()
        }
        // 重新开始计时
        startScreenTimeout()
    }
    
    /**
     * 熄灭屏幕
     * 
     * 30秒无操作后，移除 KEEP_SCREEN_ON 标志，让系统自动熄屏
     * 不再手动设置亮度，完全依赖系统的自动熄屏机制
     */
    private fun turnOffScreen() {
        if (isScreenOff) return
        
        Log.d(TAG, "30秒无操作，允许系统自动熄屏")
        isScreenOff = true
        uiState.isScreenOff.value = true
        
        // 移除 KEEP_SCREEN_ON 标志，允许系统自动熄屏
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // 不再手动设置亮度，让系统自行处理熄屏
    }
    
    /**
     * 唤醒屏幕
     * 
     * 不再手动设置亮度，只恢复 KEEP_SCREEN_ON 标志
     */
    private fun turnOnScreen() {
        if (!isScreenOff) return
        
        Log.d(TAG, "唤醒屏幕")
        isScreenOff = false
        uiState.isScreenOff.value = false
        
        // 保持屏幕常亮（直到下次计时器触发）
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // 尝试唤醒屏幕
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            @Suppress("DEPRECATION")
            val wakeLock = powerManager?.newWakeLock(
                android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK or android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "viseat:wakeup"
            )
            wakeLock?.acquire(1000)  // 1秒后自动释放
            wakeLock?.release()
        } catch (e: Exception) {
            Log.w(TAG, "唤醒屏幕失败", e)
        }
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
        // 任何触摸操作都重置熄屏计时器
        resetScreenTimeout()
        
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

                if (deltaX < -SWIPE_THRESHOLD && Math.abs(deltaX) > Math.abs(deltaY)) {
                    Log.d(TAG, "检测到前滑手势")
                    handleForwardSwipe()
                    return true
                }

                return false
            }
        }
        return super.onTouchEvent(event)
    }
    
    // Rokid 触控板手势通过 GenericMotionEvent 传递
    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        // 任何触控板操作都重置熄屏计时器
        resetScreenTimeout()
        Log.d(TAG, "GenericMotion: action=${event.action}, x=${event.x}, y=${event.y}")
        return super.onGenericMotionEvent(event)
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.repeatCount > 0) return true  // 忽略重复按键
        
        // 任何按键操作都重置熄屏计时器
        resetScreenTimeout()
        
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
     * 处理点击
     * 
     * 交互逻辑：
     * - 非用餐中：单击拍照识别
     * - 用餐中：
     *   - 首次点击：开始3秒倒计时，提示"3秒后自动结束用餐，取消请再次点击"
     *   - 倒计时中再次点击：取消结束用餐
     *   - 倒计时结束：自动拍照并结束用餐
     */
    private fun handleClick() {
        if (isInMealSession) {
            // 用餐中：处理结束用餐逻辑
            if (isEndMealPending) {
                // 正在倒计时中，再次点击取消结束用餐
                Log.d(TAG, "用餐中再次点击，取消结束用餐")
                cancelEndMealCountdown()
            } else {
                // 首次点击，开始3秒倒计时
                Log.d(TAG, "用餐中点击，开始3秒倒计时")
                startEndMealCountdown()
            }
        } else {
            // 非用餐中：单击拍照识别
            handleManualCapture()
        }
    }
    
    /**
     * 开始结束用餐倒计时（3秒）
     */
    private fun startEndMealCountdown() {
        isEndMealPending = true
        uiState.endMealCountdown.value = 3
        uiState.statusMessage.value = "3秒后自动结束用餐，取消请再次点击"
        rokidManager.speak("3秒后结束用餐，取消请再次点击")
        
        endMealCountdownJob?.cancel()
        endMealCountdownJob = lifecycleScope.launch {
            // 倒计时 3 -> 2 -> 1 -> 0
            for (i in 3 downTo 1) {
                uiState.endMealCountdown.value = i
                uiState.statusMessage.value = "${i}秒后自动结束用餐，取消请再次点击"
                delay(1000)
            }
            
            // 倒计时结束，执行结束用餐
            uiState.endMealCountdown.value = 0
            isEndMealPending = false
            Log.d(TAG, "倒计时结束，执行结束用餐")
            handleEndMealWithPhoto()
        }
    }
    
    /**
     * 取消结束用餐倒计时
     */
    private fun cancelEndMealCountdown() {
        endMealCountdownJob?.cancel()
        isEndMealPending = false
        uiState.endMealCountdown.value = 0
        uiState.statusMessage.value = "已取消结束用餐"
        rokidManager.speak("已取消")
        
        // 2秒后恢复正常状态提示
        lifecycleScope.launch {
            delay(2000)
            if (isInMealSession && !isEndMealPending) {
                uiState.statusMessage.value = "用餐监测中"
            }
        }
    }
    
    /**
     * 结束用餐并拍摄最后一张照片
     */
    private fun handleEndMealWithPhoto() {
        if (!isInMealSession) {
            Log.w(TAG, "当前没有进行中的用餐会话")
            return
        }
        
        lifecycleScope.launch {
             // 停止自动监测
             autoMonitorJob?.cancel()
             mealTimerJob?.cancel()
             recognitionTimeoutJob?.cancel()
             isWaitingForResult = false

            
            // 更新状态
            uiState.statusMessage.value = "正在拍摄最后一张..."
            rokidManager.speak("正在结束用餐")
            
            // 拍摄最后一张照片并发送
            try {
                val bitmap = suspendCoroutine<Bitmap?> { cont ->
                    cameraManager.takePicture { bmp, _ -> cont.resume(bmp) }
                }
                
                if (bitmap != null) {
             val outputStream = ByteArrayOutputStream()
             bitmap.compress(Bitmap.CompressFormat.JPEG, Config.IMAGE_QUALITY, outputStream)
             var imageData = outputStream.toByteArray()

             if (imageData.size > Config.IMAGE_MAX_BYTES) {
                 val fallback = Bitmap.createScaledBitmap(bitmap, Config.IMAGE_FALLBACK_WIDTH, Config.IMAGE_FALLBACK_HEIGHT, true)
                 val fallbackStream = ByteArrayOutputStream()
                 fallback.compress(Bitmap.CompressFormat.JPEG, (Config.IMAGE_QUALITY - 8).coerceAtLeast(70), fallbackStream)
                 imageData = fallbackStream.toByteArray()
                 fallback.recycle()
             }

             val sent = bluetoothSender.sendImage(imageData, "jpeg", isManualCapture = true, isEndMealCapture = true)
             if (sent) {
                 Log.d(TAG, "最后一张照片已发送（结束用餐模式）")
             } else {
                 Log.w(TAG, "最后一张照片发送失败（结束用餐模式）")
             }
                } else {
                    Log.w(TAG, "拍摄最后一张照片失败，bitmap为空")
                }
            } catch (e: Exception) {
                Log.e(TAG, "拍摄最后一张照片失败", e)
            }
            
             delay(500)
             handleEndMeal()
        }
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

     private fun handleForwardSwipe() {
         val screenState = getScreenState(uiState)
         val allowed = screenState == ScreenState.MONITORING || screenState == ScreenState.IDLE

         if (!allowed) {
             return
         }

         if (uiState.showLastResult.value) {
             uiState.showLastResult.value = false
             lastResultOverlayJob?.cancel()
             return
         }

         if (lastResultFoodName.isBlank()) {
             return
         }

         uiState.lastFoodName.value = lastResultFoodName
         uiState.lastCalories.value = lastResultCalories
         uiState.lastSuggestion.value = lastResultSuggestion
         uiState.showLastResult.value = true

         lastResultOverlayJob?.cancel()
         lastResultOverlayJob = lifecycleScope.launch {
             delay(5000)
             uiState.showLastResult.value = false
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
        
        // 延迟再次检查连接状态（等待探测完成）
        // 这样即使初始状态是未连接，探测成功后也会更新 UI
        lifecycleScope.launch {
            delay(1500)  // 等待探测完成
            val connectedAfterProbe = bluetoothSender.isConnected()
            Log.d(TAG, "延迟检查连接状态: $connectedAfterProbe")
            if (connectedAfterProbe && !uiState.phoneConnected.value) {
                uiState.phoneConnected.value = true
                uiState.appPhase.value = AppPhase.READY
                Log.d(TAG, "探测后更新连接状态为已连接")
            }
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
                     resultAutoHideJob?.cancel()
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
                         uiState.protein.value = 0
                         uiState.carbs.value = 0
                         uiState.fat.value = 0
                         uiState.suggestion.value = ""
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
        
        // 监听个性化建议（手机端同步的健康建议）
        bluetoothReceiver.setPersonalizedTipListener { content, category ->
            Log.d(TAG, "收到个性化建议: [$category] $content")
            // 更新个性化建议，下次拍照时会显示
            uiState.personalizedTip.value = content
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
     * 
     * 注意：用餐中禁止手动拍照，只允许 5 分钟自动拍照
     * 用餐中的点击操作会触发结束用餐（在 handleClick 中处理）
     */
    private fun handleManualCapture() {
        // 用餐中禁止手动拍照
        if (isInMealSession) {
            Log.d(TAG, "用餐中禁止手动拍照，请点击结束用餐")
            uiState.statusMessage.value = "用餐监测中，点击结束用餐"
            rokidManager.speak("用餐监测中，点击可结束用餐")
            return
        }
        
        lifecycleScope.launch {
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
     * 4. 如果超时或失败，使用本地已有数据结束用餐
     */
    private fun handleEndMeal() {
        if (!isInMealSession) {
            Log.w(TAG, "当前没有进行中的用餐会话")
            return
        }
        
        lifecycleScope.launch {
             // 停止自动监测和超时计时器
             autoMonitorJob?.cancel()
             mealTimerJob?.cancel()
             recognitionTimeoutJob?.cancel()
             isWaitingForResult = false

            
            // 更新状态为"正在结束"
            uiState.statusMessage.value = "正在结束用餐..."
            
            // 发送结束用餐指令到手机端
            val sent = bluetoothSender.sendEndMealCommand()
            
            if (sent) {
                Log.d(TAG, "结束用餐指令已发送，等待手机端响应...")
                // 手机端会通过 sessionStatusListener 返回结果
                // UI 更新会在 sessionStatusListener 的回调中处理
                
                // 设置超时：10秒内没有响应则使用本地数据结束
                delay(10000)
                if (isInMealSession) {
                    Log.w(TAG, "结束用餐响应超时，使用本地数据结束用餐")
                    forceEndMealWithLocalData()
                }
            } else {
                Log.e(TAG, "发送结束用餐指令失败，使用本地数据结束用餐")
                // 通信失败，使用本地已有数据结束用餐
                forceEndMealWithLocalData()
            }
        }
    }
    
    /**
     * 强制使用本地数据结束用餐
     * 
     * 当手机端通信失败或超时时调用，使用眼镜端已累计的数据显示用餐总结
     */
    private fun forceEndMealWithLocalData() {
        Log.d(TAG, "强制使用本地数据结束用餐，累计热量: ${mealTotalCalories.toInt()}kcal")
        
        // 停止所有计时器
        autoMonitorJob?.cancel()
        mealTimerJob?.cancel()
        recognitionTimeoutJob?.cancel()
        isWaitingForResult = false
        
        // 计算用餐时长
        val mealDurationMs = System.currentTimeMillis() - mealStartTime
        val mealDurationMinutes = (mealDurationMs / 60000).toInt()
        
        // 更新总结数据（使用本地累计数据）
        uiState.mealDurationMinutes.value = mealDurationMinutes
        uiState.mealSummaryCalories.value = mealTotalCalories.toInt()
        uiState.mealTotalProtein.value = mealTotalProtein.toInt()
        uiState.mealTotalCarbs.value = mealTotalCarbs.toInt()
        uiState.mealTotalFat.value = mealTotalFat.toInt()
        uiState.mealSummaryMessage.value = generateMealSummary(mealTotalCalories)
        
        // 更新状态
        isInMealSession = false
        uiState.sessionStatus.value = "空闲"
        uiState.showMealSummary.value = true
        
        // 播报总结
        val ttsText = if (mealTotalCalories > 0) {
            "本餐共摄入${mealTotalCalories.toInt()}千卡。${uiState.mealSummaryMessage.value}"
        } else {
            "用餐已结束"
        }
        rokidManager.speak(ttsText)
        
        // 重置累计数据
        val savedCalories = mealTotalCalories  // 保存用于日志
        mealTotalCalories = 0.0
        mealTotalProtein = 0.0
        mealTotalCarbs = 0.0
        mealTotalFat = 0.0
        uiState.mealCurrentCalories.value = 0
        
        // 恢复屏幕常亮
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // 10秒后自动关闭总结页面
        lifecycleScope.launch {
            delay(10000)
            uiState.showMealSummary.value = false
            uiState.foodName.value = ""
            uiState.calories.value = 0
        }
        
        Log.d(TAG, "用餐已强制结束（本地数据），总热量: ${savedCalories.toInt()}kcal，时长: ${mealDurationMinutes}分钟")
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
     * 启动识别超时计时器（20秒）
     * 
     * @param isManualCapture 是否为手动拍照
     */
    private fun startRecognitionTimeout(isManualCapture: Boolean) {
        recognitionTimeoutJob?.cancel()
        isWaitingForResult = true
        
        recognitionTimeoutJob = lifecycleScope.launch {
            delay(Config.RECOGNITION_TIMEOUT_MS)
            
            if (isWaitingForResult) {
                Log.w(TAG, "识别超时（${Config.RECOGNITION_TIMEOUT_MS / 1000}秒），isInMealSession=$isInMealSession")
                handleRecognitionTimeout(isManualCapture)
            }
        }
    }
    
    /**
     * 取消识别超时计时器
     */
    private fun cancelRecognitionTimeout() {
        recognitionTimeoutJob?.cancel()
        isWaitingForResult = false
        currentRetryCount = 0
    }
    
    /**
     * 处理识别超时
     * 
     * 逻辑：
     * - 非用餐中：返回上一级页面，提示"分析失败请重试"
     * - 用餐监测中：自动重拍并上传（最多重试3次）
     */
    private fun handleRecognitionTimeout(isManualCapture: Boolean) {
        isWaitingForResult = false
        
        // 立即停止所有动画和预览
        uiState.showFullscreenPreview.value = false
        uiState.showThumbnail.value = false
        uiState.capturedThumbnail.value = null
        
        if (isInMealSession) {
            // 用餐监测中：自动重拍
            currentRetryCount++
            
            if (currentRetryCount <= Config.MAX_RETRY_COUNT) {
                Log.d(TAG, "用餐中识别超时，自动重拍（第${currentRetryCount}次）")
                uiState.statusMessage.value = "分析超时，自动重拍..."
                rokidManager.speak("分析超时，正在重拍")
                
                // 自动重拍
                lifecycleScope.launch {
                    delay(500)  // 短暂延迟
                    captureAndSend(isManualCapture = false)
                }
            } else {
                // 超过最大重试次数，恢复监测状态
                Log.w(TAG, "用餐中识别超时，已达最大重试次数")
                currentRetryCount = 0
                uiState.processingPhase.value = ProcessingPhase.IDLE
                uiState.isProcessing.value = false
                uiState.statusMessage.value = "分析失败，继续监测"
                rokidManager.speak("分析失败，继续监测中")
            }
        } else {
            // 非用餐中：返回初始状态，提示失败
            Log.d(TAG, "非用餐中识别超时，返回初始状态")
            currentRetryCount = 0
            uiState.processingPhase.value = ProcessingPhase.IDLE
            uiState.isProcessing.value = false
            uiState.foodName.value = ""
            uiState.calories.value = 0
            uiState.statusMessage.value = "分析失败，请重试"
            rokidManager.speak("分析失败，请重试")
            
            // 3秒后恢复初始提示
            lifecycleScope.launch {
                delay(3000)
                if (!uiState.isProcessing.value && !isInMealSession) {
                    uiState.statusMessage.value = "点击拍照识别食物"
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
             if (uiState.foodName.value.isNotEmpty()) {
                 uiState.foodName.value = ""
                 uiState.calories.value = 0
                 uiState.protein.value = 0
                 uiState.carbs.value = 0
                 uiState.fat.value = 0
                 uiState.suggestion.value = ""
                 Log.d(TAG, "结果已自动隐藏")
             }
         }
     }

    /**
     * 拍照并发送到手机
     * 
     * 流程（取景框 + 即时反馈优化）：
     * 1. 显示取景框动画（1秒）- 让用户知道正在对焦/取景
     * 2. 拍照 + 播放快门音效
     * 3. 显示缩略图 + 分析动画 - 降低等待感
     * 4. 后台同时发送到手机并等待结果
     * 5. 结果返回后播报完整信息
     * 
     * @param isManualCapture true=用户主动拍照，false=自动拍照（静默模式）
     */
    private suspend fun captureAndSend(isManualCapture: Boolean) {
        if (!bluetoothSender.isConnected()) {
            if (isManualCapture) {
                uiState.statusMessage.value = "未连接手机"
                rokidManager.speak("未连接手机")
            }
            return
        }
        
        // 自动拍照时静默处理
        if (!isManualCapture) {
            Log.d(TAG, "自动拍照开始（静默模式）")
        }
        
        try {
            // ========== 阶段1：显示取景框动画（手动拍照时） ==========
            if (isManualCapture) {
                // 标记已经进行过拍照（后续不再显示完整 Logo）
                uiState.hasEverCaptured.value = true
                uiState.isFirstLaunch.value = false
                
                uiState.isProcessing.value = true
                uiState.processingPhase.value = ProcessingPhase.CAPTURING
                uiState.statusMessage.value = "取景中..."
                uiState.showFullscreenPreview.value = false
                uiState.showThumbnail.value = false
                
                // 优先使用个性化建议，否则使用随机提示
                uiState.currentTip.value = uiState.personalizedTip.value 
                    ?: HealthTips.getRandomTip()
                
                // 取景框动画显示1秒
                delay(1000)
            }
            
            // ========== 阶段2：拍照 ==========
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
            
            // ========== 阶段3：即时反馈 - 显示缩略图 ==========
            if (isManualCapture) {
                // 播放快门音效
                playShutterSound()
                
                // 生成预览图（用于缩略图显示）
                val previewMaxSize = 300
                val previewScale = minOf(
                    previewMaxSize.toFloat() / bitmap.width,
                    previewMaxSize.toFloat() / bitmap.height,
                    1f
                )
                val previewWidth = (bitmap.width * previewScale).toInt()
                val previewHeight = (bitmap.height * previewScale).toInt()
                val preview = if (previewScale < 1f) {
                    Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, true)
                } else {
                    bitmap
                }
                
                // 保存预览图并显示全屏预览 3 秒
                uiState.capturedThumbnail.value = preview
                uiState.showFullscreenPreview.value = true
                uiState.showThumbnail.value = false
                uiState.statusMessage.value = "已拍摄"
                
                // 播报"已拍摄"，然后追加播报健康小提示
                rokidManager.speak("已拍摄，正在分析")
                // 追加播报当前的健康小提示（不打断"已拍摄"）
                rokidManager.speakAppend(uiState.currentTip.value)
                
                // 全屏预览 3 秒后切换到分析动画
                delay(3000)
                uiState.showFullscreenPreview.value = false
                uiState.showThumbnail.value = true
                uiState.processingPhase.value = ProcessingPhase.ANALYZING_FOOD
                uiState.statusMessage.value = "正在分析..."
            }
            
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, Config.IMAGE_QUALITY, outputStream)
            var imageData = outputStream.toByteArray()

            if (imageData.size > Config.IMAGE_MAX_BYTES) {
                val fallback = Bitmap.createScaledBitmap(bitmap, Config.IMAGE_FALLBACK_WIDTH, Config.IMAGE_FALLBACK_HEIGHT, true)
                val fallbackStream = ByteArrayOutputStream()
                fallback.compress(Bitmap.CompressFormat.JPEG, (Config.IMAGE_QUALITY - 8).coerceAtLeast(70), fallbackStream)
                imageData = fallbackStream.toByteArray()
                fallback.recycle()
            }
            
            val sent = bluetoothSender.sendImage(imageData, "jpeg", isManualCapture)
            
            if (sent) {
                // 发送成功，启动超时计时器
                Log.d(TAG, "图片已发送（${if (isManualCapture) "手动" else "自动"}），等待识别结果...")
                startRecognitionTimeout(isManualCapture)
                
                // 更新状态提示（更友好的文案）
                if (isManualCapture) {
                    uiState.statusMessage.value = "正在分析美食..."
                }
            } else {
                if (isManualCapture) {
                    uiState.processingPhase.value = ProcessingPhase.IDLE
                    uiState.isProcessing.value = false
                    uiState.statusMessage.value = "发送失败"
                    rokidManager.speak("发送失败，请重试")
                } else {
                    Log.w(TAG, "自动拍照发送失败")
                    // 用餐中自动拍照失败，尝试重拍
                    if (isInMealSession) {
                        lifecycleScope.launch {
                            delay(2000)
                            captureAndSend(isManualCapture = false)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "拍照发送失败", e)
            if (isManualCapture) {
                uiState.processingPhase.value = ProcessingPhase.IDLE
                uiState.isProcessing.value = false
                uiState.statusMessage.value = "拍照失败"
                rokidManager.speak("拍照失败")
            } else if (isInMealSession) {
                // 用餐中自动拍照异常，尝试重拍
                lifecycleScope.launch {
                    delay(2000)
                    captureAndSend(isManualCapture = false)
                }
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
        // 收到处理阶段说明通信正常，重置超时计时器（延长等待时间）
        if (phaseCode in listOf(
                Config.ProcessingPhaseCode.UPLOADING,
                Config.ProcessingPhaseCode.ANALYZING,
                Config.ProcessingPhaseCode.CALCULATING
            )) {
            // 重新启动超时计时器（因为正在处理中）
            recognitionTimeoutJob?.cancel()
            recognitionTimeoutJob = lifecycleScope.launch {
                delay(Config.RECOGNITION_TIMEOUT_MS)
                if (isWaitingForResult) {
                    Log.w(TAG, "处理阶段后超时")
                    handleRecognitionTimeout(true)
                }
            }
        }
        
        when (phaseCode) {
            Config.ProcessingPhaseCode.UPLOADING -> {
                uiState.isProcessing.value = true
                uiState.processingPhase.value = ProcessingPhase.ANALYZING_FOOD
                uiState.statusMessage.value = "正在上传..."
            }
            Config.ProcessingPhaseCode.ANALYZING -> {
                uiState.processingPhase.value = ProcessingPhase.ANALYZING_FOOD
                uiState.statusMessage.value = "正在识别美食..."
            }
            Config.ProcessingPhaseCode.CALCULATING -> {
                uiState.processingPhase.value = ProcessingPhase.CALCULATING_CALORIES
                uiState.statusMessage.value = "计算营养成分..."
            }
            Config.ProcessingPhaseCode.COMPLETE -> {
                // 完成状态会在 handleNutritionResult 中进一步处理
                cancelRecognitionTimeout()
                uiState.statusMessage.value = phaseMessage
            }
            Config.ProcessingPhaseCode.ERROR -> {
                // 取消超时计时器
                cancelRecognitionTimeout()
                
                // 立即停止所有动画和预览
                uiState.processingPhase.value = ProcessingPhase.IDLE
                uiState.isProcessing.value = false
                uiState.showFullscreenPreview.value = false
                uiState.showThumbnail.value = false
                uiState.capturedThumbnail.value = null
                uiState.statusMessage.value = phaseMessage
                
                // 根据错误消息生成具体的 TTS 播报
                // 手机端会发送具体的错误信息，如"网络连接失败"、"服务器繁忙"等
                val ttsMessage = when {
                    phaseMessage.contains("未检测到食物") -> "未识别到餐品，请重新拍照"
                    phaseMessage.contains("未识别到餐品") -> "未识别到餐品，请重新拍照"
                    phaseMessage.contains("网络") -> phaseMessage
                    phaseMessage.contains("超时") -> phaseMessage
                    phaseMessage.contains("服务器") -> phaseMessage
                    phaseMessage.contains("上传") -> phaseMessage
                    else -> "识别失败，请重试"
                }
                
                // 用餐中错误：自动重拍
                if (isInMealSession) {
                    currentRetryCount++
                    if (currentRetryCount <= Config.MAX_RETRY_COUNT) {
                        Log.d(TAG, "用餐中识别错误，自动重拍（第${currentRetryCount}次）")
                        // 播报具体错误原因，然后提示正在重拍
                        rokidManager.speak("$ttsMessage，正在重拍")
                        lifecycleScope.launch {
                            delay(1000)
                            captureAndSend(isManualCapture = false)
                        }
                        return
                    } else {
                        currentRetryCount = 0
                        uiState.statusMessage.value = "识别失败，继续监测"
                        rokidManager.speak("$ttsMessage，继续监测中")
                    }
                } else {
                    // 非用餐中：清除结果，返回首页
                    uiState.foodName.value = ""
                    uiState.calories.value = 0
                    // 播报具体错误原因
                    rokidManager.speak(ttsMessage)
                }
                
                // 3秒后恢复初始状态
                lifecycleScope.launch {
                    delay(3000)
                    if (!uiState.isProcessing.value) {
                        uiState.statusMessage.value = if (isInMealSession) "用餐监测中" else "点击拍照识别食物"
                    }
                }
            }
            Config.ProcessingPhaseCode.NOT_FOOD -> {
                // 取消超时计时器
                cancelRecognitionTimeout()
                
                // 立即停止所有动画和预览
                uiState.processingPhase.value = ProcessingPhase.IDLE
                uiState.isProcessing.value = false
                uiState.showFullscreenPreview.value = false
                uiState.showThumbnail.value = false
                uiState.capturedThumbnail.value = null
                uiState.statusMessage.value = phaseMessage
                
                // 用餐中未检测到食物：自动重拍
                if (isInMealSession) {
                    currentRetryCount++
                    if (currentRetryCount <= Config.MAX_RETRY_COUNT) {
                        Log.d(TAG, "用餐中未检测到食物，自动重拍（第${currentRetryCount}次）")
                        uiState.statusMessage.value = "未检测到食物，自动重拍..."
                        rokidManager.speak("未检测到食物，正在重拍")
                        lifecycleScope.launch {
                            delay(1000)
                            captureAndSend(isManualCapture = false)
                        }
                        return
                    } else {
                        currentRetryCount = 0
                        uiState.statusMessage.value = "未检测到食物，继续监测"
                        rokidManager.speak("未检测到食物，继续监测中")
                    }
                } else {
                    uiState.showNotFoodWarning.value = true
                    rokidManager.speak("未检测到食物，请重新拍照")
                }
                
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
         // 取消超时计时器
         cancelRecognitionTimeout()
         
         // 隐藏缩略图（结果已返回）
         uiState.showThumbnail.value = false
         uiState.capturedThumbnail.value = null

         uiState.showLastResult.value = false
         lastResultOverlayJob?.cancel()

        
        // 停止处理动画
        uiState.processingPhase.value = ProcessingPhase.IDLE
        uiState.isProcessing.value = false
        
        // 检查是否识别到餐品
        val isFood = result.calories > 0 && result.foodName.isNotBlank() 
                     && !result.foodName.contains("未识别") 
                     && !result.foodName.contains("非餐品")
        
         if (!isFood) {
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

        val suggestionText = result.suggestion.ifBlank { uiState.personalizedTip.value ?: "" }

        uiState.foodName.value = result.foodName
        uiState.calories.value = result.calories.toInt()
        uiState.protein.value = result.protein.toInt()
        uiState.carbs.value = result.carbs.toInt()
        uiState.fat.value = result.fat.toInt()
        uiState.suggestion.value = suggestionText

        lastResultFoodName = result.foodName
        lastResultCalories = result.calories.toInt()
        lastResultSuggestion = suggestionText
        uiState.lastFoodName.value = lastResultFoodName
        uiState.lastCalories.value = lastResultCalories
        uiState.lastSuggestion.value = lastResultSuggestion


        
        // 根据 category 判断是否进入用餐监测
        val isMeal = result.category == "meal"
        Log.d(TAG, "食物类型: ${result.category}, 是否正餐: $isMeal, 当前用餐状态: $isInMealSession")
        
        if (!isInMealSession && isMeal) {
            // 首次识别到正餐，进入用餐监测模式
            Log.d(TAG, "检测到正餐，进入用餐监测模式")
            isInMealSession = true
            mealStartTime = System.currentTimeMillis()
            uiState.sessionStatus.value = "用餐中"
            uiState.mealElapsedSeconds.value = 0
            
            // 重置累计数据
            mealTotalCalories = 0.0
            mealTotalProtein = 0.0
            mealTotalCarbs = 0.0
            mealTotalFat = 0.0
            
            // 启动用餐计时器和自动监测
            startMealTimer()
            startAutoMonitorTimer()
            
            // 允许屏幕自动熄灭（省电）
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            
            uiState.statusMessage.value = "用餐监测已开始"
            // 播报时包含建议信息
            val ttsText = if (suggestionText.isNotBlank()) {
                "${result.foodName}，${result.calories.toInt()}千卡。${suggestionText}。开始用餐监测"
            } else {
                "${result.foodName}，${result.calories.toInt()}千卡，开始用餐监测"
            }
            rokidManager.speak(ttsText)
        } else if (!isInMealSession) {
            // 非正餐，只显示结果（包含建议）
            uiState.statusMessage.value = "已识别"
            val ttsText = if (suggestionText.isNotBlank()) {
                "${result.foodName}，${result.calories.toInt()}千卡。${suggestionText}"
            } else {
                "${result.foodName}，${result.calories.toInt()}千卡"
            }
            rokidManager.speak(ttsText)
        } else {
            // 用餐中，更新数据
            uiState.statusMessage.value = "已更新"
            val ttsText = if (suggestionText.isNotBlank()) {
                "${result.foodName}，${result.calories.toInt()}千卡。${suggestionText}"
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

/**
 * Rokid AR眼镜颜色规范
 * 
 * 核心规则：
 * - 只能使用 #40FF5E 绿色
 * - 通过透明度区分层次（40%、80%、100%）
 * - 禁止使用渐变和大面积高亮
 * - 禁止使用其他颜色（红、黄、蓝等）
 */
object RokidColors {
    // 背景
    val Black = Color(0xFF000000)
    
    // 主色 - Rokid官方绿 #40FF5E
    val Green = Color(0xFF40FF5E)
    
    // 透明度变体（用于区分层次）
    val Green100 = Color(0xFF40FF5E)  // 100% - 主要内容、按下状态
    val Green80 = Color(0xCC40FF5E)   // 80% - 选中状态、重要文字
    val Green40 = Color(0x6640FF5E)   // 40% - 常态、次要内容
    val Green20 = Color(0x3340FF5E)   // 20% - 提示、背景
    val Green10 = Color(0x1A40FF5E)   // 10% - 微弱背景
    
    // 兼容旧代码的别名（全部映射到绿色透明度）
    val Cyan = Green80           // 原信息色 -> 80%绿
    val White = Green100         // 原白色 -> 100%绿
    val Gray = Green40           // 原灰色 -> 40%绿
    val DarkGray = Green20       // 原深灰 -> 20%绿
    val Red = Green80            // 原错误色 -> 80%绿（用透明度表示）
    val Yellow = Green80         // 原警告色 -> 80%绿
    val Orange = Green80         // 原脂肪色 -> 80%绿
    
    // 透明度变体（兼容旧代码）
    val GreenLight = Green40     // 40% 绿色
    val WhiteMedium = Green80    // 80% 绿色
    val WhiteLight = Green40     // 40% 绿色
    
    // 深浅绿色系列（用于饼图区分 - 使用透明度而非不同色值）
    val GreenDark = Green100     // 100% - 蛋白质（最亮）
    val GreenMedium = Green80    // 80% - 碳水（中等）
    val GreenLight2 = Green40    // 40% - 脂肪（最暗）
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
 * 健康小提示列表
 */
object HealthTips {
    private val tips = listOf(
        "AI算法可能不准确，结果仅供参考",
        "细嚼慢咽有助于消化吸收",
        "每餐七分饱，健康又长寿",
        "多吃蔬菜水果，营养更均衡",
        "饭后散步有助于消化",
        "少油少盐，清淡饮食更健康",
        "规律进餐，保护肠胃",
        "多喝水，促进新陈代谢",
        "蛋白质是身体的建筑材料",
        "膳食纤维有助于肠道健康",
        "早餐要吃好，午餐要吃饱",
        "晚餐宜清淡，睡眠更安稳",
        "食物多样化，营养更全面",
        "控制糖分摄入，预防慢性病"
    )
    
    fun getRandomTip(): String = tips.random()
    
    fun getDisclaimer(): String = "AI算法可能不准确，结果仅供参考"
}

/**
 * UI 状态
 */
class UiState {
    // 应用阶段
    val appPhase = mutableStateOf(AppPhase.SPLASH)
    
    // 首次启动标记（只在首次显示完整 Logo）
    val isFirstLaunch = mutableStateOf(true)
    // 是否已经进行过拍照（用于判断是否显示简洁界面）
    val hasEverCaptured = mutableStateOf(false)
    
    // 连接状态
    val phoneConnected = mutableStateOf(false)
    val sessionStatus = mutableStateOf("空闲")           // 空闲 / 用餐中
    val statusMessage = mutableStateOf("等待手机连接")
    
    // 健康小提示（分析过程中显示）
    // 个性化建议由手机端从后端获取并同步
    val currentTip = mutableStateOf(HealthTips.getRandomTip())
    // 个性化建议（基于用户健康数据，由手机端同步）
    val personalizedTip = mutableStateOf<String?>(null)
    
    // 处理状态
    val isProcessing = mutableStateOf(false)
    val processingPhase = mutableStateOf(ProcessingPhase.IDLE)
    val showNotFoodWarning = mutableStateOf(false)
    val showCaptureFlash = mutableStateOf(false)
    
    // 拍照缩略图（即时反馈用）
    val capturedThumbnail = mutableStateOf<Bitmap?>(null)
    val showThumbnail = mutableStateOf(false)
    val showFullscreenPreview = mutableStateOf(false)  // 全屏预览（2秒）
    
    // 电量
    val batteryLevel = mutableStateOf(100)  // 电量百分比
    
    // 屏幕状态
    val isScreenOff = mutableStateOf(false)  // 屏幕是否已熄灭
    
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

     val showLastResult = mutableStateOf(false)
     val lastFoodName = mutableStateOf("")
     val lastCalories = mutableStateOf(0)
     val lastSuggestion = mutableStateOf("")

    
    // 结束用餐倒计时（0表示不在倒计时中，>0表示剩余秒数）
    val endMealCountdown = mutableStateOf(0)
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
        
        // 底部项目信息
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Spatial Joy 2025 · 清觉科技",
                color = RokidColors.Gray.copy(alpha = 0.6f),
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "AI结果仅供参考",
                color = RokidColors.Gray.copy(alpha = 0.4f),
                fontSize = 9.sp
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
        CenterContent(
            uiState = uiState,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 80.dp, bottom = 120.dp)
        )

        LastResultOverlay(
            visible = uiState.showLastResult.value,
            foodName = uiState.lastFoodName.value,
            calories = uiState.lastCalories.value,
            suggestion = uiState.lastSuggestion.value,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 88.dp, end = 24.dp)
        )

        BottomStatusBar(
            screenState = screenState,
            isInSession = isInSession,
            batteryLevel = uiState.batteryLevel.value,
            mealElapsedSeconds = uiState.mealElapsedSeconds.value,
            endMealCountdown = uiState.endMealCountdown.value,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 60.dp)
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
    endMealCountdown: Int = 0,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：用餐监测计时（仅用餐中显示）或操作提示
        if (isInSession) {
            MealTimerIndicator(
                elapsedSeconds = mealElapsedSeconds,
                endMealCountdown = endMealCountdown
            )
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
 * 
 * @param elapsedSeconds 用餐已进行的秒数
 * @param endMealCountdown 结束用餐倒计时（0表示不在倒计时中，>0表示剩余秒数）
 */
@Composable
fun MealTimerIndicator(
    elapsedSeconds: Int,
    endMealCountdown: Int = 0,
    modifier: Modifier = Modifier
) {
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val isCountingDown = endMealCountdown > 0
    
    // 倒计时时的闪烁动画
    val infiniteTransition = rememberInfiniteTransition(label = "countdown")
    val countdownAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "countdownAlpha"
    )
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier
                .border(
                    1.dp,
                    if (isCountingDown) RokidColors.Yellow else RokidColors.Gray,
                    RoundedCornerShape(2.dp)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 圆点（倒计时时变黄色并闪烁）
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .alpha(if (isCountingDown) countdownAlpha else 1f)
                    .background(
                        if (isCountingDown) RokidColors.Yellow else RokidColors.Green,
                        CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(4.dp))
            // 用餐中 + 计时
            Text(
                text = if (isCountingDown) "结束倒计时" else "用餐中",
                color = if (isCountingDown) RokidColors.Yellow else RokidColors.Green,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isCountingDown) "${endMealCountdown}s" else String.format("%02d:%02d", minutes, seconds),
                color = if (isCountingDown) RokidColors.Yellow else RokidColors.Gray,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        // 点击提示（倒计时中显示取消提示）
        Text(
            text = if (isCountingDown) "● 再次点击取消" else "● 点击结束用餐",
            color = if (isCountingDown) 
                RokidColors.Yellow.copy(alpha = if (isCountingDown) countdownAlpha else 0.7f) 
                else RokidColors.Green.copy(alpha = 0.7f),
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
    IDLE,               // 初始状态（品牌标题）
    FULLSCREEN_PREVIEW, // 全屏预览（拍照后2秒）
    PROCESSING,         // 处理中
    RESULT,             // 显示结果
    NOT_FOOD,           // 未识别到餐品
    MONITORING,         // 用餐监测中（结果隐藏后）
    MEAL_SUMMARY        // 用餐总结
}

/**
 * 计算当前屏幕状态
 */
fun getScreenState(uiState: UiState): ScreenState {
    return when {
        uiState.showMealSummary.value -> ScreenState.MEAL_SUMMARY
        uiState.showNotFoodWarning.value -> ScreenState.NOT_FOOD
        uiState.showFullscreenPreview.value -> ScreenState.FULLSCREEN_PREVIEW
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
                
                ScreenState.FULLSCREEN_PREVIEW -> {
                    // 全屏预览（拍照后2秒）
                    FullscreenPhotoPreview(
                        bitmap = uiState.capturedThumbnail.value
                    )
                }
                
                ScreenState.PROCESSING -> {
                    ProcessingAnimationWithThumbnail(
                        phase = uiState.processingPhase.value,
                        statusMessage = uiState.statusMessage.value,
                        thumbnail = uiState.capturedThumbnail.value,
                        showThumbnail = uiState.showThumbnail.value,
                        currentTip = uiState.currentTip.value
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
                    // 首次启动显示完整品牌标识，后续显示极简界面
                    if (uiState.isFirstLaunch.value) {
                        BrandTitle()
                    } else {
                        // 非首次：极简待机界面，降低打扰
                        MinimalIdleScreen()
                    }
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
 * 极简待机界面 - 非首次启动时显示
 * 
 * 设计原则：降低打扰，只显示必要信息
 * - 小型 Logo 标识（让用户知道 APP 在运行）
 * - 简洁的操作提示
 */
@Composable
fun MinimalIdleScreen() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 小型 Logo - 低调存在感
        Text(
            text = "VISEAT",
            color = RokidColors.Green.copy(alpha = 0.5f),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 简洁提示
        Text(
            text = "单击拍照",
            color = RokidColors.Gray.copy(alpha = 0.6f),
            fontSize = 14.sp
        )
    }
}

/**
 * 全屏照片预览 - 拍照后显示3秒
 * 
 * 3秒后自动消失，回到分析动画
 */
@Composable
fun FullscreenPhotoPreview(bitmap: Bitmap?) {
    // 淡入动画
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(300),
        label = "previewAlpha"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            // 全屏显示拍摄的照片（逆时针旋转90度）
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "拍摄的照片",
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .graphicsLayer { rotationZ = 0f }
                    .clip(RoundedCornerShape(16.dp))
                    .border(3.dp, RokidColors.Green, RoundedCornerShape(16.dp))
            )
        }
        
        // 顶部状态提示
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        color = RokidColors.Green.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "✓",
                    color = RokidColors.Green,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "已拍摄",
                    color = RokidColors.Green,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // 底部倒计时提示
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "即将开始分析...",
                color = RokidColors.Gray,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * 带缩略图的处理动画 - 即时反馈优化
 * 
 * 拍照成功后立即显示缩略图，让用户知道拍摄成功
 * 分析过程中显示健康小提示
 */
@Composable
fun ProcessingAnimationWithThumbnail(
    phase: ProcessingPhase,
    statusMessage: String,
    thumbnail: Bitmap?,
    showThumbnail: Boolean,
    currentTip: String = HealthTips.getRandomTip()
) {
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
            // 显示缩略图 + 加载动画
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                // 外圈旋转动画
                Canvas(
                    modifier = Modifier
                        .size(100.dp)
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
                
                // 中心内容：缩略图或图标
                if (showThumbnail && thumbnail != null) {
                    // 显示拍摄的缩略图（逆时针旋转90度）
                    Image(
                        bitmap = thumbnail.asImageBitmap(),
                        contentDescription = "拍摄的照片",
                        modifier = Modifier
                            .size(70.dp)
.graphicsLayer { rotationZ = 0f }
                            .clip(RoundedCornerShape(8.dp))
                            .border(2.dp, RokidColors.Green, RoundedCornerShape(8.dp))
                    )
                } else {
                    // 内圈脉冲 + 图标
                    Box(
                        modifier = Modifier
                            .size(60.dp)
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
                        // 阶段图标（使用绿色文字，不用彩色emoji）
                        Text(
                            text = when (phase) {
                                ProcessingPhase.SENDING -> "↑"
                                ProcessingPhase.ANALYZING_FOOD -> "◎"
                                ProcessingPhase.CALCULATING_CALORIES -> "≡"
                                ProcessingPhase.CALCULATING_NUTRITION -> "◇"
                                else -> "○"
                            },
                            fontSize = 32.sp,
                            color = RokidColors.Green,
                            fontWeight = FontWeight.Light
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
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
        
        // 健康小提示（分析过程中显示）
        if (phase != ProcessingPhase.CAPTURING && phase != ProcessingPhase.IDLE) {
            Spacer(modifier = Modifier.height(20.dp))
            HealthTipDisplay(tip = currentTip)
        }
    }
}

/**
 * 健康小提示显示组件
 * 
 * 在分析过程中显示，包含免责声明和健康建议
 */
@Composable
fun HealthTipDisplay(tip: String) {
    // 轮播动画 - 淡入淡出
    val infiniteTransition = rememberInfiniteTransition(label = "tip")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tipAlpha"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        // 小提示图标和文字
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .alpha(alpha)
                .border(
                    width = 1.dp,
                    color = RokidColors.Green.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = "•",
                fontSize = 14.sp,
                color = RokidColors.Green
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = tip,
                color = RokidColors.Green.copy(alpha = 0.8f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 科技感处理动画（保留兼容）
 */
@Composable
fun ProcessingAnimation(phase: ProcessingPhase, statusMessage: String) {
    ProcessingAnimationWithThumbnail(
        phase = phase,
        statusMessage = statusMessage,
        thumbnail = null,
        showThumbnail = false
    )
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
 * 
 * UI优化：
 * - 增大边距，确保文字完整显示
 * - 饼图使用深浅绿色区分营养比例
 * - 突出显示用餐时长
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
    
     Column(
         modifier = Modifier
             .fillMaxWidth()
             .padding(horizontal = 28.dp),
         horizontalAlignment = Alignment.CenterHorizontally
     ) {
        // 标题
        Text(
            text = "[ 用餐结束 ]",
            fontSize = 24.sp,
            color = RokidColors.Green,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 主要内容区：饼图 + 数据
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：饼状图（深浅绿色）
            NutrientPieChart(
                protein = protein,
                carbs = carbs,
                fat = fat,
                modifier = Modifier.size(96.dp)
            )
            
            // 右侧：数据信息
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 总热量 - 大字显示
                Text(
                    text = animatedCalories.toInt().toString(),
                    color = RokidColors.Green,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "千卡",
                    color = RokidColors.Green.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 用餐时长 - 突出显示
                Row(
                    modifier = Modifier
                        .background(
                            color = RokidColors.Green.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "◷",
                        fontSize = 14.sp,
                        color = RokidColors.Green
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (durationMinutes > 0) "用餐 ${durationMinutes} 分钟" else "用餐 <1 分钟",
                        color = RokidColors.Green,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, RokidColors.Gray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NutrientMiniItem("蛋白质", protein, RokidColors.GreenDark)    // 深绿
            NutrientMiniItem("碳水", carbs, RokidColors.Green)            // 主绿
            NutrientMiniItem("脂肪", fat, RokidColors.GreenLight2)        // 浅绿
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, RokidColors.Green.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = summaryMessage,
                color = RokidColors.Green,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "→ 滑动继续",
            color = RokidColors.Gray,
            fontSize = 11.sp
        )
    }
    

}

/**
 * 营养成分饼状图 - 使用深浅绿色区分
 * 
 * 颜色方案：
 * - 蛋白质：深绿色 (GreenDark)
 * - 碳水：主绿色 (Green)
 * - 脂肪：浅绿色 (GreenLight2)
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
            val strokeWidth = 14.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)
            
            // 计算角度
            val proteinAngle = (protein.toFloat() / total) * 360f * animatedProgress
            val carbsAngle = (carbs.toFloat() / total) * 360f * animatedProgress
            val fatAngle = (fat.toFloat() / total) * 360f * animatedProgress
            
            var startAngle = -90f
            
            // 蛋白质 - 深绿色
            drawArc(
                color = RokidColors.GreenDark,
                startAngle = startAngle,
                sweepAngle = proteinAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )
            startAngle += proteinAngle
            
            // 碳水 - 主绿色
            drawArc(
                color = RokidColors.Green,
                startAngle = startAngle,
                sweepAngle = carbsAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )
            startAngle += carbsAngle
            
            // 脂肪 - 浅绿色
            drawArc(
                color = RokidColors.GreenLight2,
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
                fontSize = 11.sp
            )
            Text(
                text = "占比",
                color = RokidColors.Gray,
                fontSize = 11.sp
            )
        }
    }
}

/**
 * 迷你营养项（用于总结页面）
 * 
 * 使用深浅绿色区分不同营养成分
 */
@Composable
fun NutrientMiniItem(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // 数值
        Text(
            text = "${value}g",
            color = color,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        // 标签
        Text(
            text = label,
            color = color.copy(alpha = 0.8f),
            fontSize = 12.sp
        )
        // 颜色指示条
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(3.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
    }
}

/**
 * 非餐品警告
 */
@Composable
fun LastResultOverlay(
    visible: Boolean,
    foodName: String,
    calories: Int,
    suggestion: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(150))
    ) {
        Column(
            modifier = modifier
                .widthIn(max = 220.dp)
                .border(1.dp, RokidColors.Green.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .background(RokidColors.DarkGray.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = foodName,
                color = RokidColors.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = calories.toString(),
                    color = RokidColors.Green,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "kcal",
                    color = RokidColors.Green.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
            if (suggestion.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = suggestion,
                    color = RokidColors.Green.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    maxLines = 2,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun NotFoodWarning() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "△",
            fontSize = 48.sp,
            color = RokidColors.Green80
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
    
    // 健康提示（TIPS）- 带标签的线框样式
    AnimatedVisibility(
        visible = uiState.suggestion.value.isNotEmpty(),
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(200))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // TIPS 标签 + 建议内容
            Row(
                modifier = Modifier
                    .border(1.dp, RokidColors.Green.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // TIPS 标签
                Box(
                    modifier = Modifier
                        .background(RokidColors.Green, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "TIPS",
                        color = RokidColors.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // 建议内容
                Text(
                    text = uiState.suggestion.value,
                    color = RokidColors.Green,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f, fill = false)
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
