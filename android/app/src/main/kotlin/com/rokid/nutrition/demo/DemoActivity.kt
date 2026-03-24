package com.rokid.nutrition.demo

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.rokid.nutrition.AppPhase
import com.rokid.nutrition.ProcessingPhase
import com.rokid.nutrition.RokidColors
import com.rokid.nutrition.RokidTheme
import com.rokid.nutrition.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 演示模式 Activity - 真实分析版
 * 
 * 支持两种模式：
 * 1. 单图识别模式：预设图片 → 真实API分析 → 显示结果
 * 2. 用餐监测模式：首图 → 分析图 → 结束图（待实现）
 * 
 * 操作方式：
 * - 点击屏幕 / 按任意键 → 进入下一步
 * - 分析过程自动进行，完成后等待点击
 * 
 * 特点：
 * - 纯黑背景 + 绿色UI
 * - 使用真实后端API分析预设图片
 * - 点击控制节奏
 */
class DemoActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "DemoActivity"
    }
    
    // UI 状态
    private val uiState = UiState()
    
    // 网络管理器
    private val networkManager = DemoNetworkManager.getInstance()
    
    // 当前步骤索引
    private var currentStepIndex = mutableStateOf(0)
    
    // 当前演示阶段
    private var currentPhase = mutableStateOf(DemoPhase.SPLASH)
    
    // 状态消息
    private var statusMessage = mutableStateOf("")
    
    // 步骤列表
    private val steps = mutableListOf<DemoStep>()
    
    // ===== 单图识别模式 =====
    // 预设图片数据
    private var cokeImageData: ByteArray? = null
    private var chipsImageData: ByteArray? = null
    
    // 预设 JSON 响应数据
    private var cokeResponseJson: String? = null
    private var chipsResponseJson: String? = null
    
    // ===== 用餐监测模式 =====
    // 用餐监测图片数据
    private var mealStartImageData: ByteArray? = null
    private var mealProgressImageData: ByteArray? = null
    private var mealEndImageData: ByteArray? = null
    
    // 用餐监测 JSON 响应数据（分析后保存）
    private var mealStartResponseJson: String? = null
    private var mealProgressResponseJson: String? = null
    private var mealEndResponseJson: String? = null
    
    // ===== 演示模式控制 =====
    // 演示模式：SINGLE_IMAGE = 单图识别, MEAL_MONITORING = 用餐监测
    private var demoMode = DemoMode.MEAL_MONITORING  // 默认用餐监测模式
    
    // 当前识别的图片索引
    // 单图模式：0=coke, 1=chips
    // 用餐监测：0=start, 1=progress, 2=end
    private var currentImageIndex = 0
    
    // 用餐监测会话 ID（从第一次分析返回）
    private var mealSessionId: String? = null
    
    // 分析任务
    private var analyzeJob: Job? = null
    
    // 自动前进任务
    private var autoAdvanceJob: Job? = null
    
    // 用餐计时器任务
    private var mealTimerJob: Job? = null
    
    // 是否正在分析中（分析中不允许点击跳过）
    private var isAnalyzing = false
    
    // 是否使用本地数据（两种模式都使用本地 JSON 数据）
    private val useLocalData: Boolean
        get() = true
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        android.util.Log.d(TAG, "DemoActivity onCreate")
        
        // 保持屏幕常亮
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // 加载预设图片
        loadPresetImages()
        
        // 初始化步骤列表
        buildStepList()
        
        // 初始化 UI 状态
        initializeUiState()
        
        setContent {
            RokidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    val currentStepData = steps.getOrNull(currentStepIndex.value)
                    DemoScreen(
                        uiState = uiState,
                        currentPhase = currentPhase.value,
                        scenario = DemoScenario.fullDemo(),
                        currentStep = currentStepIndex.value,
                        totalSteps = steps.size,
                        stepName = currentStepData?.name ?: "",
                        statusMessage = statusMessage.value,
                        isAutoAdvance = (currentStepData?.autoAdvanceMs ?: 0) > 0,
                        autoAdvanceSeconds = ((currentStepData?.autoAdvanceMs ?: 0) / 1000).toInt(),
                        onNextStep = { goToNextStep() }
                    )
                }
            }
        }
        
        // 执行第一步
        executeCurrentStep()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        analyzeJob?.cancel()
        autoAdvanceJob?.cancel()
    }
    
    /**
     * 加载预设图片和 JSON 数据
     */
    private fun loadPresetImages() {
        // ===== 单图识别模式资源 =====
        loadAssetImage("demo/coke.jpg") { cokeImageData = it }
        loadAssetImage("demo/chips.jpg") { chipsImageData = it }
        loadAssetJson("demo/coke_response.json") { cokeResponseJson = it }
        loadAssetJson("demo/chips_response.json") { chipsResponseJson = it }
        
        // ===== 用餐监测模式资源 =====
        loadAssetImage("demo/meal_start.jpg") { mealStartImageData = it }
        loadAssetImage("demo/meal_middle.jpg") { mealProgressImageData = it }
        loadAssetImage("demo/meal_end.jpg") { mealEndImageData = it }
        
        // 用餐监测的 JSON 响应（如果有预设的话）
        loadAssetJson("demo/meal_start_response.json") { mealStartResponseJson = it }
        loadAssetJson("demo/meal_middle_response.json") { mealProgressResponseJson = it }
        loadAssetJson("demo/meal_end_response.json") { mealEndResponseJson = it }
        
        android.util.Log.d(TAG, "资源加载完成 - 模式: $demoMode")
    }
    
    private fun loadAssetImage(path: String, setter: (ByteArray) -> Unit) {
        try {
            val inputStream = assets.open(path)
            val data = inputStream.readBytes()
            inputStream.close()
            setter(data)
            android.util.Log.d(TAG, "图片加载成功: $path (${data.size} bytes)")
        } catch (e: Exception) {
            android.util.Log.w(TAG, "图片加载失败: $path")
        }
    }
    
    private fun loadAssetJson(path: String, setter: (String) -> Unit) {
        try {
            val inputStream = assets.open(path)
            val json = inputStream.bufferedReader().readText()
            inputStream.close()
            setter(json)
            android.util.Log.d(TAG, "JSON加载成功: $path")
        } catch (e: Exception) {
            // JSON 文件可能不存在（用餐监测模式需要真实API分析）
        }
    }
    
    /**
     * 获取当前要识别的图片数据
     */
    private fun getCurrentImageData(): ByteArray? {
        return when (demoMode) {
            DemoMode.SINGLE_IMAGE -> when (currentImageIndex) {
                0 -> cokeImageData
                1 -> chipsImageData
                else -> null
            }
            DemoMode.MEAL_MONITORING -> when (currentImageIndex) {
                0 -> mealStartImageData
                1 -> mealProgressImageData
                2 -> mealEndImageData
                else -> null
            }
        }
    }
    
    /**
     * 获取当前图片名称
     */
    private fun getCurrentImageName(): String {
        return when (demoMode) {
            DemoMode.SINGLE_IMAGE -> when (currentImageIndex) {
                0 -> "可乐"
                1 -> "薯片"
                else -> "未知"
            }
            DemoMode.MEAL_MONITORING -> when (currentImageIndex) {
                0 -> "用餐开始"
                1 -> "用餐中"
                2 -> "用餐结束"
                else -> "未知"
            }
        }
    }
    
    /**
     * 获取当前预设的 JSON 响应
     */
    private fun getCurrentResponseJson(): String? {
        return when (demoMode) {
            DemoMode.SINGLE_IMAGE -> when (currentImageIndex) {
                0 -> cokeResponseJson
                1 -> chipsResponseJson
                else -> null
            }
            DemoMode.MEAL_MONITORING -> when (currentImageIndex) {
                0 -> mealStartResponseJson
                1 -> mealProgressResponseJson
                2 -> mealEndResponseJson
                else -> null
            }
        }
    }
    
    /**
     * 获取当前用餐监测的 mode 参数
     * 
     * 后端 API 模式：
     * - start: 开始用餐，建立基线
     * - update: 用餐中途，计算消耗
     * - end: 用餐结束，计算总消耗
     */
    private fun getCurrentMealMode(): String {
        return when (currentImageIndex) {
            0 -> "start"
            1 -> "update"  // 修正：update 而不是 progress
            2 -> "end"
            else -> "start"
        }
    }
    
    /**
     * 点击屏幕触发下一步
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            goToNextStep()
            return true
        }
        return super.onTouchEvent(event)
    }
    
    /**
     * 按任意键触发下一步
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.repeatCount == 0) {
            goToNextStep()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
    
    /**
     * 构建步骤列表 - 根据演示模式选择
     */
    private fun buildStepList() {
        steps.clear()
        
        // 公共开场步骤
        addCommonStartSteps()
        
        // 根据模式添加不同的识别步骤
        when (demoMode) {
            DemoMode.SINGLE_IMAGE -> buildSingleImageSteps()
            DemoMode.MEAL_MONITORING -> buildMealMonitoringSteps()
        }
        
        // 公共结束步骤
        addCommonEndSteps()
    }
    
    /**
     * 公共开场步骤
     */
    private fun addCommonStartSteps() {
        // 1. 开屏动画
        steps.add(DemoStep("开屏动画", DemoPhase.SPLASH) {
            uiState.appPhase.value = AppPhase.SPLASH
            statusMessage.value = "品牌展示"
        })
        
        // 2. 等待连接
        steps.add(DemoStep("等待连接", DemoPhase.CONNECTING) {
            uiState.appPhase.value = AppPhase.CONNECTING
            statusMessage.value = "模拟连接过程"
        })
        
        // 3. 首页待机
        steps.add(DemoStep("首页待机", DemoPhase.IDLE) {
            uiState.appPhase.value = AppPhase.READY
            uiState.isProcessing.value = false
            uiState.foodName.value = ""
            uiState.showMealSummary.value = false
            uiState.showFullscreenPreview.value = false
            currentImageIndex = 0
            statusMessage.value = "等待拍照"
        })
    }
    
    /**
     * 单图识别模式步骤（可乐 + 薯片）
     */
    private fun buildSingleImageSteps() {
        // ========== 第一次识别：可乐 ==========
        addRecognitionSteps("可乐", 0)
        
        // 返回待机
        steps.add(DemoStep("返回待机", DemoPhase.IDLE) {
            uiState.foodName.value = ""
            uiState.isProcessing.value = false
            uiState.showFullscreenPreview.value = false
            currentImageIndex = 1
            statusMessage.value = "准备识别薯片"
        })
        
        // ========== 第二次识别：薯片 ==========
        addRecognitionSteps("薯片", 1)
    }
    
    /**
     * 用餐监测模式步骤
     * 
     * 与真实应用逻辑一致：
     * 后端基线模式用餐监测流程：
     * 1. start: 拍照建立基线，显示初始食物和热量
     * 2. update: 后台拍照，计算已消耗热量（不显示图片预览）
     * 3. end: 拍照结束，显示总消耗热量
     */
    private fun buildMealMonitoringSteps() {
        // ========== 阶段1: 开始用餐 (start 模式) ==========
        // 拍照 → 预览 → 分析 → 显示初始食物 → 进入用餐监测
        addMealStartSteps()
        
        // 用餐监测中（等待后台拍照）
        steps.add(DemoStep("用餐监测中", DemoPhase.MONITORING, autoAdvanceMs = 5000) {
            uiState.foodName.value = ""
            uiState.isProcessing.value = false
            uiState.showFullscreenPreview.value = false
            uiState.sessionStatus.value = "用餐中"
            uiState.statusMessage.value = "用餐监测中..."
            statusMessage.value = "监测中 (5秒后后台拍照)"
        })
        
        // ========== 阶段2: 用餐中途 (update 模式) ==========
        // 后台拍照 → 分析 → 只显示已消耗热量（不显示图片预览）
        addMealUpdateSteps()
        
        // 用餐监测中（等待结束拍照）
        steps.add(DemoStep("用餐监测中", DemoPhase.MONITORING, autoAdvanceMs = 5000) {
            uiState.isProcessing.value = false
            uiState.showFullscreenPreview.value = false
            uiState.sessionStatus.value = "用餐中"
            uiState.statusMessage.value = "用餐监测中..."
            statusMessage.value = "监测中 (5秒后结束用餐)"
        })
        
        // ========== 阶段3: 用餐结束 (end 模式) ==========
        // 拍照 → 分析 → 显示总消耗 → 用餐总结
        addMealEndSteps()
        
        // ========== 用餐总结 ==========
        steps.add(DemoStep("用餐总结", DemoPhase.SUMMARY) {
            uiState.isProcessing.value = false
            uiState.foodName.value = ""
            uiState.showMealSummary.value = true
            uiState.sessionStatus.value = "空闲"
            
            // 设置用餐总结数据（使用总消耗热量）
            uiState.mealSummaryCalories.value = mealTotalConsumed
            uiState.mealSummaryMessage.value = generateMealSummary(mealTotalConsumed)
            uiState.mealDurationMinutes.value = 33  // 33分27秒
            uiState.mealTotalProtein.value = mealTotalProtein
            uiState.mealTotalCarbs.value = mealTotalCarbs
            uiState.mealTotalFat.value = mealTotalFat
            
            statusMessage.value = "总消耗: ${mealTotalConsumed}kcal"
        })
    }
    
    /**
     * 阶段1: 开始用餐 (start 模式)
     * - 拍照建立基线
     * - 显示初始食物和热量
     * - 进入用餐监测模式
     */
    private fun addMealStartSteps() {
        // 拍照取景
        steps.add(DemoStep("拍照取景[用餐开始]", DemoPhase.CAPTURING) {
            currentImageIndex = 0
            currentMealPhase = "start"
            uiState.isProcessing.value = true
            uiState.processingPhase.value = ProcessingPhase.CAPTURING
            uiState.statusMessage.value = "取景中..."
            uiState.showFullscreenPreview.value = false
            statusMessage.value = "拍摄: 用餐开始"
            
            getCurrentImageData()?.let { data ->
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                uiState.capturedThumbnail.value = bitmap
            }
        })
        
        // 已拍摄预览
        steps.add(DemoStep("预览[用餐开始]", DemoPhase.PREVIEW, autoAdvanceMs = 2000) {
            uiState.showFullscreenPreview.value = true
            uiState.isProcessing.value = false
            uiState.statusMessage.value = "已拍摄"
            statusMessage.value = "用餐开始 预览 2秒"
        })
        
        // 分析（start 模式）
        steps.add(DemoStep("分析[用餐开始]", DemoPhase.PROCESSING) {
            uiState.showFullscreenPreview.value = false
            uiState.isProcessing.value = true
            uiState.processingPhase.value = ProcessingPhase.SENDING
            uiState.statusMessage.value = "正在建立基线..."
            statusMessage.value = "分析 用餐开始 (start)..."
            startRealAnalysis()
        })
        
        // 显示初始食物结果
        steps.add(DemoStep("结果[用餐开始]", DemoPhase.RESULT, autoAdvanceMs = 5000) {
            uiState.isProcessing.value = false
            uiState.sessionStatus.value = "用餐中"
            uiState.statusMessage.value = "用餐监测已开始"
            statusMessage.value = "初始热量: ${mealBaselineCalories}kcal (5秒后继续)"
        })
    }
    
    /**
     * 阶段2: 用餐中途 (update 模式)
     * - 后台拍照（不显示图片预览）
     * - 计算已消耗热量
     * - 只显示消耗数据
     */
    private fun addMealUpdateSteps() {
        // 后台拍照（不显示预览）
        steps.add(DemoStep("后台拍照[用餐中]", DemoPhase.CAPTURING) {
            currentImageIndex = 1
            currentMealPhase = "update"
            uiState.isProcessing.value = true
            uiState.processingPhase.value = ProcessingPhase.CAPTURING
            uiState.statusMessage.value = "后台拍照中..."
            uiState.showFullscreenPreview.value = false  // 不显示预览
            statusMessage.value = "后台拍摄: 用餐中"
            
            getCurrentImageData()?.let { data ->
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                uiState.capturedThumbnail.value = bitmap
            }
        })
        
        // 分析（update 模式）
        steps.add(DemoStep("分析[用餐中]", DemoPhase.PROCESSING) {
            uiState.isProcessing.value = true
            uiState.processingPhase.value = ProcessingPhase.SENDING
            uiState.statusMessage.value = "计算消耗中..."
            statusMessage.value = "分析 用餐中 (update)..."
            startRealAnalysis()
        })
        
        // 显示已消耗热量（不显示食物详情）
        steps.add(DemoStep("结果[用餐中]", DemoPhase.MONITORING, autoAdvanceMs = 5000) {
            uiState.isProcessing.value = false
            uiState.foodName.value = ""  // 不显示食物名称
            uiState.sessionStatus.value = "用餐中"
            uiState.statusMessage.value = "已消耗 ${mealCurrentConsumed}kcal"
            uiState.mealCurrentCalories.value = mealCurrentConsumed
            statusMessage.value = "已消耗: ${mealCurrentConsumed}kcal (5秒后继续)"
            
            // 设置用餐时间为 5:00 (300秒) 并开始计时
            uiState.mealElapsedSeconds.value = 300
            startMealTimer(300)
        })
    }
    
    /**
     * 阶段3: 用餐结束 (end 模式)
     * - 拍照计算最终消耗
     * - 显示总消耗热量
     */
    private fun addMealEndSteps() {
        // 拍照
        steps.add(DemoStep("拍照[用餐结束]", DemoPhase.CAPTURING) {
            currentImageIndex = 2
            currentMealPhase = "end"
            uiState.isProcessing.value = true
            uiState.processingPhase.value = ProcessingPhase.CAPTURING
            uiState.statusMessage.value = "拍照中..."
            uiState.showFullscreenPreview.value = false
            statusMessage.value = "拍摄: 用餐结束"
            
            getCurrentImageData()?.let { data ->
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                uiState.capturedThumbnail.value = bitmap
            }
        })
        
        // 分析（end 模式）
        steps.add(DemoStep("分析[用餐结束]", DemoPhase.PROCESSING) {
            uiState.isProcessing.value = true
            uiState.processingPhase.value = ProcessingPhase.SENDING
            uiState.statusMessage.value = "计算总消耗..."
            statusMessage.value = "分析 用餐结束 (end)..."
            startRealAnalysis()
        })
        
        // 显示总消耗
        steps.add(DemoStep("结果[用餐结束]", DemoPhase.RESULT, autoAdvanceMs = 5000) {
            uiState.isProcessing.value = false
            uiState.foodName.value = "用餐完成"
            uiState.calories.value = mealTotalConsumed
            uiState.sessionStatus.value = "用餐中"
            uiState.statusMessage.value = "总消耗 ${mealTotalConsumed}kcal"
            statusMessage.value = "总消耗: ${mealTotalConsumed}kcal (5秒后显示总结)"
            
            // 停止计时，设置最终时间为 33:27 (2007秒)
            stopMealTimer()
            uiState.mealElapsedSeconds.value = 2007
        })
    }
    
    /**
     * 生成用餐总结建议
     */
    private fun generateMealSummary(totalCalories: Int): String {
        return when {
            totalCalories < 300 -> "本餐摄入较少，注意营养均衡"
            totalCalories < 600 -> "本餐摄入适中，继续保持"
            totalCalories < 900 -> "本餐摄入较多，建议适当运动"
            else -> "本餐摄入过多，建议控制饮食"
        }
    }
    
    /**
     * 启动用餐计时器
     * @param startSeconds 起始秒数
     */
    private fun startMealTimer(startSeconds: Int) {
        stopMealTimer()
        var currentSeconds = startSeconds
        mealTimerJob = lifecycleScope.launch {
            while (mealTimerJob?.isActive == true) {
                delay(1000)
                currentSeconds++
                uiState.mealElapsedSeconds.value = currentSeconds
            }
        }
        android.util.Log.d(TAG, "用餐计时器启动，起始时间: ${formatTime(startSeconds)}")
    }
    
    /**
     * 停止用餐计时器
     */
    private fun stopMealTimer() {
        mealTimerJob?.cancel()
        mealTimerJob = null
        android.util.Log.d(TAG, "用餐计时器停止")
    }
    
    /**
     * 格式化时间为 mm:ss
     */
    private fun formatTime(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return "%d:%02d".format(mins, secs)
    }
    
    // 当前用餐阶段
    private var currentMealPhase = "start"
    
    // 基线热量（start 模式返回的初始热量）
    private var mealBaselineCalories = 0
    
    // 当前已消耗热量（update 模式返回）
    private var mealCurrentConsumed = 0
    
    // 总消耗热量（end 模式返回）
    private var mealTotalConsumed = 0
    
    // 累计营养数据
    private var mealTotalProtein = 0
    private var mealTotalCarbs = 0
    private var mealTotalFat = 0
    
    /**
     * 添加单次识别的步骤（拍照 → 预览 → 分析 → 结果）
     */
    private fun addRecognitionSteps(name: String, imageIndex: Int) {
        // 拍照取景
        steps.add(DemoStep("拍照取景[$name]", DemoPhase.CAPTURING) {
            currentImageIndex = imageIndex
            uiState.isProcessing.value = true
            uiState.processingPhase.value = ProcessingPhase.CAPTURING
            uiState.statusMessage.value = "取景中..."
            uiState.showFullscreenPreview.value = false
            statusMessage.value = "拍摄: $name"
            
            // 加载图片作为缩略图
            getCurrentImageData()?.let { data ->
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                uiState.capturedThumbnail.value = bitmap
            }
        })
        
        // 已拍摄预览
        steps.add(DemoStep("预览[$name]", DemoPhase.PREVIEW, autoAdvanceMs = 3000) {
            uiState.showFullscreenPreview.value = true
            uiState.isProcessing.value = false
            uiState.statusMessage.value = "已拍摄"
            statusMessage.value = "$name 预览 3秒"
        })
        
        // 分析
        steps.add(DemoStep("分析[$name]", DemoPhase.PROCESSING) {
            uiState.showFullscreenPreview.value = false
            uiState.isProcessing.value = true
            uiState.processingPhase.value = ProcessingPhase.SENDING
            uiState.statusMessage.value = "正在上传..."
            statusMessage.value = "分析 $name..."
            
            startRealAnalysis()
        })
        
        // 显示结果
        steps.add(DemoStep("结果[$name]", DemoPhase.RESULT) {
            uiState.isProcessing.value = false
            statusMessage.value = "$name 识别完成"
        })
    }
    
    /**
     * 公共结束步骤
     */
    private fun addCommonEndSteps() {
        steps.add(DemoStep("结束", DemoPhase.END) {
            uiState.showMealSummary.value = false
            uiState.foodName.value = ""
            uiState.sessionStatus.value = "空闲"
            uiState.appPhase.value = AppPhase.READY
            statusMessage.value = "演示结束"
            
            // 保存所有分析结果到文件
            saveAnalyzeResults()
        })
    }
    
    /**
     * 启动分析 - 单图识别使用本地数据，用餐监测使用真实API
     */
    private fun startRealAnalysis() {
        val imageName = getCurrentImageName()
        
        isAnalyzing = true
        analyzeJob = lifecycleScope.launch {
            try {
                if (useLocalData) {
                    // 使用本地预设数据
                    startLocalAnalysis(imageName)
                } else {
                    // 使用真实 API（用餐监测模式）
                    startRemoteAnalysis(imageName)
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "${imageName}分析失败", e)
                uiState.statusMessage.value = "分析失败: ${e.message}"
                statusMessage.value = "${imageName}错误: ${e.message}"
                isAnalyzing = false
            }
        }
    }
    
    /**
     * 使用本地预设数据进行分析
     */
    private suspend fun startLocalAnalysis(imageName: String) {
        val responseJson = getCurrentResponseJson()
        
        if (responseJson == null) {
            throw Exception("没有预设响应数据")
        }
        
        // 模拟上传过程
        uiState.processingPhase.value = ProcessingPhase.SENDING
        uiState.statusMessage.value = "正在上传..."
        statusMessage.value = "上传${imageName}..."
        delay(800)  // 模拟上传延迟
        
        // 模拟分析过程
        uiState.processingPhase.value = ProcessingPhase.ANALYZING_FOOD
        uiState.statusMessage.value = "识别食物中..."
        statusMessage.value = "分析${imageName}..."
        delay(1200)  // 模拟分析延迟
        
        // 解析本地 JSON 数据
        val json = org.json.JSONObject(responseJson)
        
        // 用餐监测模式：根据阶段解析不同数据
        if (demoMode == DemoMode.MEAL_MONITORING) {
            parseMealMonitoringResponse(json)
        } else {
            // 单图识别模式
            val result = parseLocalResponse(json)
            uiState.foodName.value = result.foodName
            uiState.calories.value = result.calories
            uiState.protein.value = result.protein
            uiState.carbs.value = result.carbs
            uiState.fat.value = result.fat
            uiState.suggestion.value = result.suggestion
            statusMessage.value = "${imageName}: ${result.foodName}"
        }
        
        // 更新UI状态
        uiState.processingPhase.value = ProcessingPhase.CALCULATING_CALORIES
        uiState.statusMessage.value = "计算热量中..."
        delay(500)
        
        isAnalyzing = false
        android.util.Log.d(TAG, "本地数据分析完成 - 阶段: $currentMealPhase")
        
        // 自动进入结果显示步骤
        goToNextStep()
    }
    
    /**
     * 解析用餐监测模式的响应数据
     */
    private fun parseMealMonitoringResponse(json: org.json.JSONObject) {
        // 保存 session_id
        json.optString("session_id", "").takeIf { it.isNotBlank() }?.let {
            mealSessionId = it
            android.util.Log.d(TAG, "获取到 session_id: $mealSessionId")
        }
        
        when (currentMealPhase) {
            "start" -> {
                // 解析基线热量（从 snapshot.nutrition）
                val snapshot = json.optJSONObject("snapshot")
                val nutrition = snapshot?.optJSONObject("nutrition")
                mealBaselineCalories = nutrition?.optDouble("calories", 0.0)?.toInt() ?: 0
                
                // 获取食物名称
                val rawLlm = json.optJSONObject("raw_llm")
                val foods = rawLlm?.optJSONArray("foods") ?: org.json.JSONArray()
                val foodNames = mutableListOf<String>()
                for (i in 0 until foods.length()) {
                    val food = foods.getJSONObject(i)
                    val name = food.optString("dish_name_cn", "").takeIf { it.isNotBlank() }
                        ?: food.optString("dish_name", "未知食物")
                    foodNames.add(name)
                }
                val foodName = if (foodNames.isNotEmpty()) foodNames.joinToString("、") else "套餐"
                
                // 更新 UI
                uiState.foodName.value = foodName
                uiState.calories.value = mealBaselineCalories
                uiState.protein.value = nutrition?.optDouble("protein", 0.0)?.toInt() ?: 0
                uiState.carbs.value = nutrition?.optDouble("carbs", 0.0)?.toInt() ?: 0
                uiState.fat.value = nutrition?.optDouble("fat", 0.0)?.toInt() ?: 0
                uiState.suggestion.value = json.optString("suggestion", "")
                
                android.util.Log.d(TAG, "start 阶段 - 基线热量: $mealBaselineCalories kcal, 食物: $foodName")
                statusMessage.value = "初始: ${mealBaselineCalories}kcal"
            }
            "update" -> {
                // 解析已消耗热量（从 consumed.nutrition.calories）
                val consumed = json.optJSONObject("consumed")
                val consumedNutrition = consumed?.optJSONObject("nutrition")
                mealCurrentConsumed = consumedNutrition?.optDouble("calories", 0.0)?.toInt() ?: 0
                
                // 更新 UI - 只显示已消耗热量
                uiState.mealCurrentCalories.value = mealCurrentConsumed
                
                android.util.Log.d(TAG, "update 阶段 - 已消耗热量: $mealCurrentConsumed kcal")
                statusMessage.value = "已消耗: ${mealCurrentConsumed}kcal"
            }
            "end" -> {
                // 解析总消耗热量（从 consumed.nutrition）
                val consumed = json.optJSONObject("consumed")
                val consumedNutrition = consumed?.optJSONObject("nutrition")
                mealTotalConsumed = consumedNutrition?.optDouble("calories", 0.0)?.toInt() ?: 0
                mealTotalProtein = consumedNutrition?.optDouble("protein", 0.0)?.toInt() ?: 0
                mealTotalCarbs = consumedNutrition?.optDouble("carbs", 0.0)?.toInt() ?: 0
                mealTotalFat = consumedNutrition?.optDouble("fat", 0.0)?.toInt() ?: 0
                
                // 更新 UI
                uiState.mealCurrentCalories.value = mealTotalConsumed
                uiState.suggestion.value = json.optString("suggestion", "用餐完成")
                
                android.util.Log.d(TAG, "end 阶段 - 总消耗热量: $mealTotalConsumed kcal")
                statusMessage.value = "总消耗: ${mealTotalConsumed}kcal"
            }
            else -> {
                android.util.Log.w(TAG, "未知的用餐阶段: $currentMealPhase")
            }
        }
    }
    
    /**
     * 解析本地 JSON 响应
     */
    private fun parseLocalResponse(json: org.json.JSONObject): AnalyzeResult {
        val rawLlm = json.optJSONObject("raw_llm")
        val snapshot = json.optJSONObject("snapshot")
        val nutrition = snapshot?.optJSONObject("nutrition")
        val foods = rawLlm?.optJSONArray("foods") ?: org.json.JSONArray()
        
        val isFood = rawLlm?.optBoolean("is_food", false) ?: false
        
        // 获取食物名称
        val foodNames = mutableListOf<String>()
        for (i in 0 until foods.length()) {
            val food = foods.getJSONObject(i)
            val name = food.optString("dish_name_cn", "").takeIf { it.isNotBlank() }
                ?: food.optString("dish_name", "未知食物")
            foodNames.add(name)
        }
        val foodName = if (foodNames.isNotEmpty()) foodNames.joinToString("、") else "未知食物"
        
        // 获取营养数据
        val calories = nutrition?.optDouble("calories", 0.0)?.toInt() ?: 0
        val protein = nutrition?.optDouble("protein", 0.0)?.toInt() ?: 0
        val carbs = nutrition?.optDouble("carbs", 0.0)?.toInt() ?: 0
        val fat = nutrition?.optDouble("fat", 0.0)?.toInt() ?: 0
        
        val suggestion = json.optString("suggestion", "").takeIf { it.isNotBlank() }
            ?: rawLlm?.optString("suggestion", "") ?: ""
        
        return AnalyzeResult(
            isFood = isFood,
            foodName = foodName,
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat,
            suggestion = suggestion
        )
    }
    
    /**
     * 使用真实 API 进行分析（用餐监测模式）
     * 
     * 图片会压缩到 160KB 以加快上传速度
     */
    private suspend fun startRemoteAnalysis(imageName: String) {
        val imageData = getCurrentImageData()
        
        if (imageData == null) {
            throw Exception("没有预设图片: $imageName")
        }
        
        // 压缩图片到 160KB
        val compressedData = compressImage(imageData, 160 * 1024)
        android.util.Log.d(TAG, "图片压缩: ${imageData.size} -> ${compressedData.size} bytes")
        
        // 上传图片
        uiState.processingPhase.value = ProcessingPhase.SENDING
        uiState.statusMessage.value = "正在上传..."
        statusMessage.value = "上传${imageName}..."
        
        val uploadResult = networkManager.uploadImage(compressedData)
        if (uploadResult.isFailure) {
            throw uploadResult.exceptionOrNull() ?: Exception("上传失败")
        }
        val imageUrl = uploadResult.getOrThrow()
        statusMessage.value = "${imageName}上传完成"
        
        // 分析图片
        uiState.processingPhase.value = ProcessingPhase.ANALYZING_FOOD
        uiState.statusMessage.value = "识别食物中..."
        statusMessage.value = "分析${imageName}..."
        
        // 用餐监测模式需要传入 mode 参数
        val mode = if (demoMode == DemoMode.MEAL_MONITORING) getCurrentMealMode() else null
        val sessionId = if (demoMode == DemoMode.MEAL_MONITORING) mealSessionId else null
        
        val analyzeResult = networkManager.analyzeImage(imageUrl, mode, sessionId)
        if (analyzeResult.isFailure) {
            throw analyzeResult.exceptionOrNull() ?: Exception("分析失败")
        }
        val result = analyzeResult.getOrThrow()
        
        // 用餐监测模式：根据阶段解析不同数据
        if (demoMode == DemoMode.MEAL_MONITORING) {
            networkManager.lastRawResponse?.let { rawJson ->
                try {
                    val json = org.json.JSONObject(rawJson)
                    
                    when (currentMealPhase) {
                        "start" -> {
                            // 保存 session_id
                            json.optString("session_id", "").takeIf { it.isNotBlank() }?.let {
                                mealSessionId = it
                                android.util.Log.d(TAG, "获取到 session_id: $mealSessionId")
                            }
                            // 保存基线热量（从 snapshot.nutrition.calories）
                            val snapshot = json.optJSONObject("snapshot")
                            val nutrition = snapshot?.optJSONObject("nutrition")
                            mealBaselineCalories = nutrition?.optDouble("calories", 0.0)?.toInt() ?: result.calories
                            android.util.Log.d(TAG, "基线热量: $mealBaselineCalories kcal")
                        }
                        "update" -> {
                            // 解析已消耗热量（从 consumed.nutrition.calories）
                            val consumed = json.optJSONObject("consumed")
                            val consumedNutrition = consumed?.optJSONObject("nutrition")
                            mealCurrentConsumed = consumedNutrition?.optDouble("calories", 0.0)?.toInt() ?: 0
                            android.util.Log.d(TAG, "已消耗热量: $mealCurrentConsumed kcal")
                        }
                        "end" -> {
                            // 解析总消耗热量（从 consumed.nutrition.calories）
                            val consumed = json.optJSONObject("consumed")
                            val consumedNutrition = consumed?.optJSONObject("nutrition")
                            mealTotalConsumed = consumedNutrition?.optDouble("calories", 0.0)?.toInt() ?: 0
                            
                            // 解析总营养数据
                            mealTotalProtein = consumedNutrition?.optDouble("protein", 0.0)?.toInt() ?: 0
                            mealTotalCarbs = consumedNutrition?.optDouble("carbs", 0.0)?.toInt() ?: 0
                            mealTotalFat = consumedNutrition?.optDouble("fat", 0.0)?.toInt() ?: 0
                            
                            android.util.Log.d(TAG, "总消耗热量: $mealTotalConsumed kcal")
                        }
                        else -> {
                            android.util.Log.w(TAG, "未知的用餐阶段: $currentMealPhase")
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "解析用餐监测数据失败", e)
                }
            }
        }
        
        // 更新UI状态
        uiState.processingPhase.value = ProcessingPhase.CALCULATING_CALORIES
        uiState.statusMessage.value = "计算热量中..."
        delay(500)
        
        // 填充结果（用餐监测模式根据阶段显示不同内容）
        if (demoMode == DemoMode.MEAL_MONITORING) {
            when (currentMealPhase) {
                "start" -> {
                    // 显示初始食物和热量
                    uiState.foodName.value = result.foodName
                    uiState.calories.value = mealBaselineCalories
                    uiState.protein.value = result.protein
                    uiState.carbs.value = result.carbs
                    uiState.fat.value = result.fat
                    uiState.suggestion.value = result.suggestion
                }
                "update" -> {
                    // 只更新已消耗热量，不显示食物详情
                    uiState.mealCurrentCalories.value = mealCurrentConsumed
                }
                "end" -> {
                    // 显示总消耗
                    uiState.mealCurrentCalories.value = mealTotalConsumed
                }
            }
        } else {
            // 单图识别模式
            uiState.foodName.value = result.foodName
            uiState.calories.value = result.calories
            uiState.protein.value = result.protein
            uiState.carbs.value = result.carbs
            uiState.fat.value = result.fat
            uiState.suggestion.value = result.suggestion
        }
        
        statusMessage.value = "${imageName}: ${result.foodName}"
        isAnalyzing = false
        
        // 自动进入结果显示步骤
        goToNextStep()
    }
    
    /**
     * 压缩图片到指定大小
     * 
     * @param imageData 原始图片数据
     * @param maxSize 最大字节数（默认 160KB）
     */
    private fun compressImage(imageData: ByteArray, maxSize: Int = 160 * 1024): ByteArray {
        if (imageData.size <= maxSize) {
            return imageData
        }
        
        // 解码图片
        val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
            ?: return imageData
        
        var quality = 90
        var compressedData: ByteArray
        
        do {
            val outputStream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, outputStream)
            compressedData = outputStream.toByteArray()
            quality -= 10
        } while (compressedData.size > maxSize && quality > 10)
        
        return compressedData
    }
    
    private fun initializeUiState() {
        uiState.phoneConnected.value = true
        uiState.batteryLevel.value = 85
        uiState.sessionStatus.value = "空闲"
        uiState.foodName.value = ""
        uiState.calories.value = 0
        uiState.showMealSummary.value = false
        uiState.mealCurrentCalories.value = 0
        uiState.mealElapsedSeconds.value = 0
        
        // 重置用餐监测状态
        mealSessionId = null
        currentImageIndex = 0
        currentMealPhase = "start"
        
        // 重置基线模式数据
        mealBaselineCalories = 0
        mealCurrentConsumed = 0
        mealTotalConsumed = 0
        mealTotalProtein = 0
        mealTotalCarbs = 0
        mealTotalFat = 0
        
        // 清空之前的分析记录
        networkManager.clearResponses()
    }
    
    /**
     * 保存所有分析结果到文件
     * 
     * 文件保存在应用私有目录，可通过 adb pull 获取：
     * adb pull /data/data/com.rokid.nutrition/files/demo_analyze_results.json
     * 
     * 或者直接从 Logcat 复制 JSON 内容
     */
    private fun saveAnalyzeResults() {
        val responses = networkManager.allResponses
        if (responses.isEmpty()) {
            android.util.Log.w(TAG, "没有分析结果需要保存")
            return
        }
        
        try {
            // 构建 JSON 数组
            val jsonArray = org.json.JSONArray()
            responses.forEachIndexed { index, record ->
                val jsonObj = org.json.JSONObject().apply {
                    put("index", index)
                    put("timestamp", record.timestamp)
                    put("time", record.getFormattedTime())
                    put("imageUrl", record.imageUrl)
                    put("foodName", record.foodName)
                    put("calories", record.calories)
                    // 解析原始响应为 JSON 对象
                    put("rawResponse", org.json.JSONObject(record.rawResponse))
                }
                jsonArray.put(jsonObj)
            }
            
            // 格式化 JSON
            val jsonContent = jsonArray.toString(2)
            
            // 保存到应用私有目录
            val file = java.io.File(filesDir, "demo_analyze_results.json")
            file.writeText(jsonContent)
            
            android.util.Log.d(TAG, "分析结果已保存到: ${file.absolutePath}")
            android.util.Log.d(TAG, "共 ${responses.size} 条记录")
            android.util.Log.d(TAG, "获取文件: adb pull ${file.absolutePath} .")
            
            // 输出到 Logcat 方便复制
            android.util.Log.i(TAG, "")
            android.util.Log.i(TAG, "╔════════════════════════════════════════════════════════════════╗")
            android.util.Log.i(TAG, "║           分析结果 JSON - 可直接复制到手机端使用                  ║")
            android.util.Log.i(TAG, "╚════════════════════════════════════════════════════════════════╝")
            
            // 分段输出（Logcat 有长度限制）
            val lines = jsonContent.split("\n")
            lines.forEach { line ->
                android.util.Log.i(TAG, line)
            }
            
            android.util.Log.i(TAG, "════════════════════════════════════════════════════════════════")
            
            statusMessage.value = "已保存 ${responses.size} 条结果"
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "保存分析结果失败", e)
            statusMessage.value = "保存失败: ${e.message}"
        }
    }
    
    /**
     * 执行当前步骤
     */
    private fun executeCurrentStep() {
        autoAdvanceJob?.cancel()
        
        if (currentStepIndex.value < steps.size) {
            val step = steps[currentStepIndex.value]
            currentPhase.value = step.phase
            step.action()
            
            // 如果设置了自动前进时间，启动定时器
            if (step.autoAdvanceMs > 0) {
                autoAdvanceJob = lifecycleScope.launch {
                    delay(step.autoAdvanceMs)
                    goToNextStep()
                }
            }
        }
    }
    
    /**
     * 前进到下一步
     */
    private fun goToNextStep() {
        // 分析中不允许手动跳过
        if (isAnalyzing) {
            android.util.Log.d(TAG, "分析中，不允许跳过")
            return
        }
        
        if (currentStepIndex.value < steps.size - 1) {
            currentStepIndex.value++
            executeCurrentStep()
        }
    }
}

/**
 * 演示步骤
 * 
 * @param name 步骤名称
 * @param phase 演示阶段
 * @param autoAdvanceMs 自动前进时间（毫秒），0表示手动控制
 * @param action 执行的动作
 */
data class DemoStep(
    val name: String,
    val phase: DemoPhase,
    val autoAdvanceMs: Long = 0,  // 0 = 手动点击，>0 = 自动前进
    val action: () -> Unit
)

/**
 * 演示模式枚举
 */
enum class DemoMode {
    SINGLE_IMAGE,     // 单图识别模式（可乐 + 薯片，使用本地数据）
    MEAL_MONITORING   // 用餐监测模式（开始 → 用餐中 → 结束，使用真实API）
}

/**
 * 演示阶段枚举
 */
enum class DemoPhase {
    SPLASH,      // 开屏
    CONNECTING,  // 连接
    IDLE,        // 空闲
    CAPTURING,   // 拍照
    PREVIEW,     // 预览
    PROCESSING,  // 处理中
    RESULT,      // 显示结果
    MONITORING,  // 用餐监测
    SUMMARY,     // 用餐总结
    END          // 结束
}

/**
 * 演示界面 - 黑色背景 + 复用主程序UI
 * 
 * @param showStepIndicator 是否显示步骤指示器（录制时设为 false）
 */
@Composable
fun DemoScreen(
    uiState: UiState,
    currentPhase: DemoPhase,
    scenario: DemoScenario,
    currentStep: Int = 0,
    totalSteps: Int = 1,
    stepName: String = "",
    statusMessage: String = "",
    isAutoAdvance: Boolean = false,
    autoAdvanceSeconds: Int = 0,
    onNextStep: () -> Unit = {},
    showStepIndicator: Boolean = true
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onNextStep() }
    ) {
        // 判断是否显示全屏预览（Demo 专用绿色滤镜版本）
        if (uiState.showFullscreenPreview.value && uiState.capturedThumbnail.value != null) {
            // Demo 专用：绿色滤镜 + 逆时针旋转90度
            DemoFullscreenPreview(bitmap = uiState.capturedThumbnail.value!!)
        } else {
            // 其他状态复用主程序的 AppScreen
            com.rokid.nutrition.AppScreen(uiState = uiState)
        }
        
        // 步骤指示器（调试用，录制时可隐藏）
        if (showStepIndicator) {
            StepIndicator(
                currentStep = currentStep,
                totalSteps = totalSteps,
                phaseName = stepName.ifEmpty { currentPhase.name },
                statusMessage = statusMessage,
                isAutoAdvance = isAutoAdvance,
                autoAdvanceSeconds = autoAdvanceSeconds,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
            )
        }
    }
}

/**
 * Demo 专用全屏预览 - 绿色滤镜 + 逆时针旋转90度
 * 
 * 与主程序不同，Demo 模式下图片显示为纯绿色，模拟 AR 眼镜的显示效果
 */
@Composable
fun DemoFullscreenPreview(bitmap: android.graphics.Bitmap) {
    // 淡入动画
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(300),
        label = "previewAlpha"
    )
    
    // 绿色滤镜矩阵：将所有颜色转换为绿色通道
    val greenColorMatrix = ColorMatrix(floatArrayOf(
        0f, 0f, 0f, 0f, 0f,           // R = 0
        0.3f, 0.59f, 0.11f, 0f, 0f,   // G = 灰度值（亮度）
        0f, 0f, 0f, 0f, 0f,           // B = 0
        0f, 0f, 0f, 1f, 0f            // A = 原值
    ))
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        // 全屏显示拍摄的照片（逆时针旋转90度 + 绿色滤镜）
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "拍摄的照片",
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .graphicsLayer { rotationZ = -90f }  // 逆时针旋转90度
                .clip(RoundedCornerShape(16.dp))
                .border(3.dp, RokidColors.Green, RoundedCornerShape(16.dp)),
            colorFilter = ColorFilter.colorMatrix(greenColorMatrix)
        )
        
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
                    text = "[OK]",
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
    }
}

/**
 * 步骤指示器 - 显示当前步骤（录制时隐藏）
 */
@Composable
fun StepIndicator(
    currentStep: Int,
    totalSteps: Int,
    phaseName: String,
    statusMessage: String = "",
    isAutoAdvance: Boolean = false,
    autoAdvanceSeconds: Int = 0,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.End
    ) {
        // 步骤计数
        Text(
            text = "${currentStep + 1}/$totalSteps",
            color = RokidColors.Green,
            fontSize = 12.sp
        )
        // 阶段名称
        Text(
            text = phaseName,
            color = RokidColors.Green80,
            fontSize = 10.sp
        )
        // 状态消息
        if (statusMessage.isNotEmpty()) {
            Text(
                text = statusMessage,
                color = RokidColors.Green40,
                fontSize = 9.sp
            )
        }
        // 操作提示
        Text(
            text = if (isAutoAdvance) "${autoAdvanceSeconds}s 后自动" else "点击下一步 →",
            color = RokidColors.Green40,
            fontSize = 9.sp
        )
    }
}
