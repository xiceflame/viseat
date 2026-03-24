package com.rokid.nutrition.phone.demo

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rokid.nutrition.phone.network.model.VisionAnalyzeResponse
import com.rokid.nutrition.phone.repository.MealSessionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 演示模式 ViewModel
 * 
 * 管理演示模式的UI状态和业务逻辑
 */
class DemoViewModel(
    private val demoDataRepository: DemoDataRepository,
    private val mealSessionRepository: MealSessionRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DemoUiState())
    val uiState: StateFlow<DemoUiState> = _uiState.asStateFlow()
    
    // 用餐监测会话ID
    private var currentSessionId: String? = null
    private var mealStartTime: Long = 0L
    private var baselineCalories: Double = 0.0
    
    init {
        checkDataAvailability()
    }
    
    /**
     * 检查数据可用性
     */
    private fun checkDataAvailability() {
        val status = demoDataRepository.isDemoDataAvailable()
        _uiState.update { it.copy(dataStatus = status) }
    }
    
    /**
     * 选择演示模式
     */
    fun selectMode(mode: DemoMode) {
        _uiState.update { 
            it.copy(
                currentMode = mode,
                selectedFood = null,
                previewImage = null,
                recognitionResult = null,
                mealPhase = MealPhase.NOT_STARTED,
                mealProgress = null,
                error = null
            )
        }
    }
    
    /**
     * 返回模式选择
     */
    fun backToSelection() {
        _uiState.update { 
            it.copy(
                currentMode = DemoMode.SELECTION,
                selectedFood = null,
                previewImage = null,
                recognitionResult = null,
                isProcessing = false,
                error = null
            )
        }
    }
    
    // ==================== 单图识别模式 ====================
    
    /**
     * 选择食物
     */
    fun selectFood(foodType: String) {
        val imageName = demoDataRepository.getSingleImageFileName(foodType)
        val bitmap = demoDataRepository.loadDemoImage(imageName)
        
        _uiState.update { 
            it.copy(
                selectedFood = foodType,
                previewImage = bitmap,
                recognitionResult = null,
                error = if (bitmap == null) "无法加载图片: $imageName" else null
            )
        }
    }
    
    /**
     * 开始单图识别
     */
    fun startSingleImageRecognition() {
        val foodType = _uiState.value.selectedFood ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, processingMessage = "正在分析...") }
            
            // 模拟延迟
            delay(1500)
            
            val result = demoDataRepository.loadSingleImageData(foodType)
            
            if (result != null) {
                _uiState.update { 
                    it.copy(
                        isProcessing = false,
                        recognitionResult = result,
                        processingMessage = ""
                    )
                }
                
                // 保存到数据库
                saveNonMealResult(result, foodType)
            } else {
                _uiState.update { 
                    it.copy(
                        isProcessing = false,
                        error = "加载模拟数据失败",
                        processingMessage = ""
                    )
                }
            }
        }
    }
    
    /**
     * 保存非正餐识别结果
     */
    private suspend fun saveNonMealResult(result: VisionAnalyzeResponse, foodType: String) {
        try {
            val sessionId = "demo_${foodType}_${System.currentTimeMillis()}"
            val mealType = when (foodType) {
                "coke" -> "beverage"
                "chips" -> "snack"
                else -> "snack"
            }
            
            mealSessionRepository.saveNonMealSession(
                sessionId = sessionId,
                mealType = mealType,
                totalCalories = result.snapshot.nutrition.calories
            )
            
            // 复制demo图片到本地存储
            val imageName = demoDataRepository.getSingleImageFileName(foodType)
            val localImagePath = demoDataRepository.copyDemoImageToLocalStorage(imageName)
            
            // 保存快照（包含图片路径）
            mealSessionRepository.saveSnapshot(
                sessionId = sessionId, 
                visionResult = result,
                localImagePath = localImagePath
            )
            
            _uiState.update { it.copy(latestSessionId = sessionId) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "保存失败: ${e.message}") }
        }
    }
    
    // ==================== 用餐监测模式 ====================
    
    /**
     * 开始用餐监测
     */
    fun startMealMonitoring() {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    mealPhase = MealPhase.STARTING,
                    isProcessing = true,
                    processingMessage = "正在识别餐食..."
                )
            }
            
            // 加载开始图片
            val imageName = demoDataRepository.getMealPhaseImageFileName("start")
            val bitmap = demoDataRepository.loadDemoImage(imageName)
            _uiState.update { it.copy(previewImage = bitmap) }
            
            // 模拟延迟
            delay(2000)
            
            val result = demoDataRepository.loadMealPhaseData("start")
            
            if (result != null) {
                currentSessionId = "demo_meal_${System.currentTimeMillis()}"
                mealStartTime = System.currentTimeMillis()
                baselineCalories = result.snapshot.nutrition.calories
                
                val progress = MealProgress(
                    baselineCalories = baselineCalories,
                    currentCalories = baselineCalories,
                    consumedCalories = 0.0,
                    consumptionRatio = 0.0,
                    startTime = mealStartTime,
                    snapshots = listOf(
                        SnapshotInfo("start", result.snapshot.nutrition.calories, System.currentTimeMillis())
                    )
                )
                
                _uiState.update { 
                    it.copy(
                        mealPhase = MealPhase.IN_PROGRESS,
                        isProcessing = false,
                        recognitionResult = result,
                        mealProgress = progress,
                        processingMessage = ""
                    )
                }
            } else {
                _uiState.update { 
                    it.copy(
                        mealPhase = MealPhase.NOT_STARTED,
                        isProcessing = false,
                        error = "加载用餐开始数据失败",
                        processingMessage = ""
                    )
                }
            }
        }
    }
    
    /**
     * 触发中间拍摄
     */
    fun triggerMiddleCapture() {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isProcessing = true,
                    processingMessage = "正在识别剩余食物..."
                )
            }
            
            // 加载中间图片
            val imageName = demoDataRepository.getMealPhaseImageFileName("middle")
            val bitmap = demoDataRepository.loadDemoImage(imageName)
            _uiState.update { it.copy(previewImage = bitmap) }
            
            // 模拟延迟
            delay(1500)
            
            val result = demoDataRepository.loadMealPhaseData("middle")
            
            if (result != null) {
                val currentCalories = result.snapshot.nutrition.calories
                val consumed = baselineCalories - currentCalories
                val ratio = if (baselineCalories > 0) consumed / baselineCalories else 0.0
                
                val currentProgress = _uiState.value.mealProgress
                val newSnapshots = (currentProgress?.snapshots ?: emptyList()) + 
                    SnapshotInfo("middle", currentCalories, System.currentTimeMillis())
                
                val progress = MealProgress(
                    baselineCalories = baselineCalories,
                    currentCalories = currentCalories,
                    consumedCalories = consumed,
                    consumptionRatio = ratio,
                    startTime = mealStartTime,
                    snapshots = newSnapshots
                )
                
                _uiState.update { 
                    it.copy(
                        isProcessing = false,
                        recognitionResult = result,
                        mealProgress = progress,
                        processingMessage = ""
                    )
                }
            } else {
                _uiState.update { 
                    it.copy(
                        isProcessing = false,
                        error = "加载用餐中数据失败",
                        processingMessage = ""
                    )
                }
            }
        }
    }
    
    /**
     * 结束用餐监测
     */
    fun endMealMonitoring() {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    mealPhase = MealPhase.ENDING,
                    isProcessing = true,
                    processingMessage = "正在生成用餐报告..."
                )
            }
            
            // 加载结束图片
            val imageName = demoDataRepository.getMealPhaseImageFileName("end")
            val bitmap = demoDataRepository.loadDemoImage(imageName)
            _uiState.update { it.copy(previewImage = bitmap) }
            
            // 模拟延迟
            delay(2000)
            
            val result = demoDataRepository.loadMealPhaseData("end")
            
            if (result != null) {
                val currentCalories = result.snapshot.nutrition.calories
                val consumed = baselineCalories - currentCalories
                val ratio = if (baselineCalories > 0) consumed / baselineCalories else 0.0
                val durationMinutes = (System.currentTimeMillis() - mealStartTime) / 60000.0
                
                val currentProgress = _uiState.value.mealProgress
                val newSnapshots = (currentProgress?.snapshots ?: emptyList()) + 
                    SnapshotInfo("end", currentCalories, System.currentTimeMillis())
                
                val progress = MealProgress(
                    baselineCalories = baselineCalories,
                    currentCalories = currentCalories,
                    consumedCalories = consumed,
                    consumptionRatio = ratio,
                    startTime = mealStartTime,
                    snapshots = newSnapshots,
                    durationMinutes = durationMinutes
                )
                
                _uiState.update { 
                    it.copy(
                        mealPhase = MealPhase.COMPLETED,
                        isProcessing = false,
                        recognitionResult = result,
                        mealProgress = progress,
                        processingMessage = ""
                    )
                }
                
                // 保存完整会话到数据库
                saveMealSession(progress)
            } else {
                _uiState.update { 
                    it.copy(
                        mealPhase = MealPhase.IN_PROGRESS,
                        isProcessing = false,
                        error = "加载用餐结束数据失败",
                        processingMessage = ""
                    )
                }
            }
        }
    }
    
    /**
     * 保存用餐会话
     */
    private suspend fun saveMealSession(progress: MealProgress) {
        // 简化实现：保存为已完成的会话
        currentSessionId?.let { sessionId ->
            try {
                mealSessionRepository.saveNonMealSession(
                    sessionId = sessionId,
                    mealType = "meal",
                    totalCalories = progress.consumedCalories
                )
                
                // 复制用餐开始的图片到本地存储（作为这次用餐的代表图片）
                val imageName = demoDataRepository.getMealPhaseImageFileName("start")
                val localImagePath = demoDataRepository.copyDemoImageToLocalStorage(imageName)
                
                // 加载用餐开始的数据作为快照
                val startResult = demoDataRepository.loadMealPhaseData("start")
                startResult?.let { result ->
                    mealSessionRepository.saveSnapshot(
                        sessionId = sessionId,
                        visionResult = result,
                        localImagePath = localImagePath
                    )
                }
                
                _uiState.update { it.copy(latestSessionId = sessionId) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "保存会话失败: ${e.message}") }
            }
        }
    }
    
    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    override fun onCleared() {
        super.onCleared()
        demoDataRepository.clearCache()
    }
}

// ==================== UI State ====================

/**
 * 演示模式UI状态
 */
data class DemoUiState(
    val currentMode: DemoMode = DemoMode.SELECTION,
    val selectedFood: String? = null,
    val previewImage: Bitmap? = null,
    val isProcessing: Boolean = false,
    val processingMessage: String = "",
    val recognitionResult: VisionAnalyzeResponse? = null,
    val mealPhase: MealPhase = MealPhase.NOT_STARTED,
    val mealProgress: MealProgress? = null,
    val error: String? = null,
    val dataStatus: DemoDataStatus = DemoDataStatus(
        cokeAvailable = false,
        chipsAvailable = false,
        mealStartAvailable = false,
        mealMiddleAvailable = false,
        mealEndAvailable = false
    ),
    val latestSessionId: String? = null
)

/**
 * 演示模式
 */
enum class DemoMode {
    SELECTION,      // 模式选择
    SINGLE_IMAGE,   // 单图识别
    MEAL_MONITOR    // 用餐监测
}

/**
 * 用餐阶段
 */
enum class MealPhase {
    NOT_STARTED,    // 未开始
    STARTING,       // 开始用餐中
    IN_PROGRESS,    // 用餐中
    ENDING,         // 结束用餐中
    COMPLETED       // 已完成
}

/**
 * 用餐进度
 */
data class MealProgress(
    val baselineCalories: Double,
    val currentCalories: Double,
    val consumedCalories: Double,
    val consumptionRatio: Double,
    val startTime: Long,
    val snapshots: List<SnapshotInfo>,
    val durationMinutes: Double = 0.0
)

/**
 * 快照信息
 */
data class SnapshotInfo(
    val phase: String,
    val calories: Double,
    val timestamp: Long
)
