package com.rokid.nutrition.phone.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rokid.nutrition.phone.bluetooth.BluetoothManager
import com.rokid.nutrition.phone.bluetooth.ConnectionState
import com.rokid.nutrition.phone.bluetooth.ImageData
import com.rokid.nutrition.phone.bluetooth.NutritionResult
import com.rokid.nutrition.phone.network.NetworkManager
import com.rokid.nutrition.phone.network.model.FoodType
import com.rokid.nutrition.phone.network.model.MealContext
import com.rokid.nutrition.phone.network.model.UserProfilePayload
import com.rokid.nutrition.phone.network.model.getFoodType
import com.rokid.nutrition.phone.repository.DailyNutritionTracker
import com.rokid.nutrition.phone.repository.MealSessionManager
import com.rokid.nutrition.phone.repository.MealSessionRepository
import com.rokid.nutrition.phone.repository.UserManager
import com.rokid.nutrition.phone.repository.UserProfile
import com.rokid.nutrition.phone.repository.UserProfileRepository
import com.rokid.nutrition.phone.repository.toBaselineFood
import com.rokid.nutrition.phone.util.DebugLogger
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 首页 ViewModel
 */
class HomeViewModel(
    private val bluetoothManager: BluetoothManager,
    private val networkManager: NetworkManager,
    private val sessionRepository: MealSessionRepository,
    private val userProfileRepository: UserProfileRepository,
    private val mealSessionManager: MealSessionManager,
    private val userManager: UserManager,
    private val dailyNutritionTracker: DailyNutritionTracker
) : ViewModel() {
    
    companion object {
        private const val TAG = "HomeViewModel"
    }
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private var currentSessionId: String? = null
    private var userProfile: UserProfile? = null
    private var sessionStartTime: Long? = null  // 用餐开始时间
    
    // 保存完整的后端响应数据用于详情页面
    private var latestFoodData: com.rokid.nutrition.phone.network.model.VisionAnalyzeResponse? = null
    
    // 识别历史记录（最多保存 20 条）
    private val _recognitionHistory = MutableStateFlow<List<RecognitionHistory>>(emptyList())
    val recognitionHistory: StateFlow<List<RecognitionHistory>> = _recognitionHistory.asStateFlow()
    
    init {
        observeBluetoothState()
        observeReceivedData()
        loadUserProfile()
        checkActiveSession()
        
        // 启动时自动尝试连接眼镜
        autoConnectOnStartup()
        
        // 检查并注册用户
        checkAndRegisterUser()
    }
    
    /**
     * 检查并注册用户
     */
    private fun checkAndRegisterUser() {
        viewModelScope.launch {
            if (userManager.needsRegistration()) {
                Log.d(TAG, "需要注册用户")
                val deviceId = userManager.getDeviceId()
                val result = networkManager.registerUser(deviceId)
                result.fold(
                    onSuccess = { response ->
                        userManager.saveRegisterResponse(response)
                        Log.d(TAG, "用户注册成功: userId=${response.userId}")
                    },
                    onFailure = { e ->
                        Log.e(TAG, "用户注册失败", e)
                    }
                )
            } else {
                Log.d(TAG, "用户已注册: userId=${userManager.getUserId()}")
            }
        }
    }
    
    /**
     * 获取设备ID
     */
    fun getDeviceId(): String = userManager.getDeviceId()
    
    /**
     * 获取用户ID
     */
    fun getUserId(): String? = userManager.getUserId()
    
    /**
     * 获取当前会话ID
     */
    fun getSessionId(): String? = userManager.getCurrentSessionId() ?: currentSessionId
    
    /**
     * 启动时自动连接眼镜
     */
    private fun autoConnectOnStartup() {
        viewModelScope.launch {
            // 延迟一点确保蓝牙服务已初始化
            kotlinx.coroutines.delay(500)
            Log.d(TAG, "启动时自动连接眼镜...")
            bluetoothManager.autoConnect()
        }
    }
    
    private fun observeBluetoothState() {
        viewModelScope.launch {
            bluetoothManager.connectionState.collect { state ->
                _uiState.update { it.copy(connectionState = state) }
                
                if (state is ConnectionState.Connected) {
                    loadGlassesInfo()
                }
            }
        }
    }
    
    private fun observeReceivedData() {
        // 监听图片接收
        viewModelScope.launch {
            bluetoothManager.receivedImage.collect { imageData ->
                handleImageReceived(imageData)
            }
        }
        
        // 监听指令接收
        viewModelScope.launch {
            bluetoothManager.receivedCommand.collect { command ->
                handleCommandReceived(command)
            }
        }
    }
    
    private fun loadUserProfile() {
        viewModelScope.launch {
            userProfile = userProfileRepository.getProfile()
        }
    }
    
    private fun checkActiveSession() {
        viewModelScope.launch {
            val activeSession = sessionRepository.getActiveSession()
            if (activeSession != null) {
                currentSessionId = activeSession.sessionId
                _uiState.update {
                    it.copy(
                        hasActiveSession = true,
                        sessionStatus = "用餐中",
                        totalCalories = activeSession.totalServedKcal ?: 0.0
                    )
                }
            }
        }
    }
    
    private fun loadGlassesInfo() {
        bluetoothManager.getGlassInfo { info ->
            info?.let {
                _uiState.update { state ->
                    state.copy(glassesName = it.name)
                }
            }
        }
        
        bluetoothManager.setBatteryListener { level, _ ->
            _uiState.update { it.copy(batteryLevel = level) }
        }
    }

    
    /**
     * 处理接收到的图片
     * 
     * 处理流程：
     * 1. 如果有活跃会话 → 使用 analyze_meal_update（带容错）
     * 2. 如果没有会话 → 使用 vision/analyze（开始识别）
     * 3. 只有正餐才会激活用餐监测，零食不会
     */
    private fun handleImageReceived(imageData: ImageData) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, statusMessage = "正在处理...") }
            
            // 通知眼镜开始处理
            bluetoothManager.notifyAiStart()
            bluetoothManager.notifyUploading()
            
            try {
                if (mealSessionManager.isActive) {
                    // 有活跃会话：使用容错 API
                    handleMealUpdate(imageData)
                } else {
                    // 没有会话：开始识别
                    handleNewAnalysis(imageData)
                }
            } catch (e: Exception) {
                Log.e(TAG, "处理图片异常", e)
                bluetoothManager.notifyAiError()
                _uiState.update { it.copy(isProcessing = false, statusMessage = "处理失败") }
            }
        }
    }
    
    /**
     * 处理新的分析（没有活跃会话时）
     */
    private suspend fun handleNewAnalysis(imageData: ImageData) {
        val profilePayload = userProfile?.let {
            UserProfilePayload(
                age = it.age, gender = it.gender, bmi = it.bmi,
                activityLevel = it.activityLevel, healthGoal = it.healthGoal,
                targetWeight = it.targetWeight, healthConditions = it.healthConditions,
                dietaryPreferences = it.dietaryPreferences
            )
        }
        
        // 保存上传后的图片URL
        var uploadedImageUrl: String = ""
        
        val result = networkManager.uploadAndAnalyzeWithProgress(
            imageData = imageData.data,
            userProfile = profilePayload,
            onUploadComplete = { imageUrl ->
                uploadedImageUrl = imageUrl
                Log.d(TAG, "图片上传完成，URL: $imageUrl")
                bluetoothManager.notifyAnalyzing()
                _uiState.update { it.copy(statusMessage = "识别菜品中...") }
            },
            onAnalyzeComplete = {
                bluetoothManager.notifyCalculating()
                _uiState.update { it.copy(statusMessage = "热量计算中...") }
            }
        )
        
        result.fold(
            onSuccess = { response ->
                bluetoothManager.notifyComplete()
                
                // 保存完整的后端响应数据
                latestFoodData = response
                
                // 添加到识别历史记录
                addToRecognitionHistory(response)
                
                // 构建营养结果
                val nutritionResult = buildNutritionResult(response)
                bluetoothManager.sendNutritionResult(nutritionResult)
                
                // 判断食物类型
                val foodType = response.getFoodType()
                DebugLogger.i(TAG, "食物类型: $foodType")
                
                // 只有正餐才激活用餐监测
                val shouldStartSession = foodType == FoodType.MEAL
                DebugLogger.i(TAG, "是否开始会话: $shouldStartSession, 图片URL: $uploadedImageUrl")
                
                // 生成会话ID（用于编辑功能）
                val tempSessionId = if (shouldStartSession) null else "snapshot_${System.currentTimeMillis()}"
                
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = if (shouldStartSession) "用餐监测中" else "识别完成",
                        latestResult = nutritionResult,
                        latestImageUrl = uploadedImageUrl,
                        latestSessionId = tempSessionId,
                        hasActiveSession = shouldStartSession,
                        sessionStatus = if (shouldStartSession) "用餐中" else "空闲",
                        sessionStartTime = if (shouldStartSession) System.currentTimeMillis() else null
                    )
                }
                
                // 如果是正餐，开始会话（传递图片URL）
                if (shouldStartSession) {
                    DebugLogger.i(TAG, "开始正餐会话")
                    sessionStartTime = System.currentTimeMillis()
                    startMealSessionInternal(response, uploadedImageUrl)
                } else {
                    // 即使不是正餐，也保存快照到数据库（用于历史记录显示）
                    DebugLogger.i(TAG, "保存非正餐快照")
                    saveSnapshotForNonMeal(response, uploadedImageUrl, tempSessionId!!)
                }
            },
            onFailure = { e -> handleAnalysisError(e) }
        )
    }
    
    /**
     * 处理用餐更新（有活跃会话时）
     */
    private suspend fun handleMealUpdate(imageData: ImageData) {
        val baselineFoods = mealSessionManager.getBaselineFoods()
        
        val result = networkManager.uploadAndAnalyzeMealUpdate(
            imageData = imageData.data,
            baselineFoods = baselineFoods,
            onUploadComplete = {
                bluetoothManager.notifyAnalyzing()
                _uiState.update { it.copy(statusMessage = "识别菜品中...") }
            },
            onAnalyzeComplete = {
                bluetoothManager.notifyCalculating()
                _uiState.update { it.copy(statusMessage = "热量计算中...") }
            }
        )
        
        result.fold(
            onSuccess = { response ->
                // 处理更新响应
                mealSessionManager.handleUpdateResponse(response)
                
                when (response.status) {
                    "skip" -> {
                        // 静默跳过，不更新 UI
                        Log.w(TAG, "跳过本次更新: ${response.message}")
                        _uiState.update { it.copy(isProcessing = false, statusMessage = "用餐监测中") }
                    }
                    else -> {
                        // accept 或 adjust：更新 UI
                        bluetoothManager.notifyComplete()
                        val nutritionResult = buildNutritionResultFromUpdate(response)
                        bluetoothManager.sendNutritionResult(nutritionResult)
                        
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                statusMessage = if (response.status == "adjust") "已加菜" else "已更新",
                                latestResult = nutritionResult,
                                totalCalories = mealSessionManager.getTotalConsumed()?.calories ?: 0.0
                            )
                        }
                    }
                }
            },
            onFailure = { e -> handleAnalysisError(e) }
        )
    }
    
    /**
     * 内部开始用餐会话
     */
    private suspend fun startMealSessionInternal(response: com.rokid.nutrition.phone.network.model.VisionAnalyzeResponse, imageUrl: String) {
        val baselineFoods = response.rawLlm.foods.map { it.toBaselineFood() }
        
        // 调用后端创建会话
        val mealResult = networkManager.startMeal("default_user", "lunch", response)
        mealResult.fold(
            onSuccess = { mealResponse ->
                currentSessionId = mealResponse.sessionId
                mealSessionManager.startSession(
                    sessionId = mealResponse.sessionId,
                    baselineFoods = baselineFoods,
                    baselineImageUrl = imageUrl,
                    baselineNutrition = response.snapshot.nutrition
                )
                
                // 保存快照到本地数据库（包含图片URL）
                sessionRepository.saveSnapshot(
                    sessionId = mealResponse.sessionId,
                    visionResult = response,
                    imageUrl = imageUrl
                )
                Log.d(TAG, "快照已保存: sessionId=${mealResponse.sessionId}, imageUrl=$imageUrl")
                
                // 通知眼镜会话已开始
                bluetoothManager.sendSessionStatus(
                    sessionId = mealResponse.sessionId,
                    status = "active",
                    totalConsumed = 0.0,
                    message = "用餐监测已开始"
                )
                Log.d(TAG, "用餐会话已开始: ${mealResponse.sessionId}")
            },
            onFailure = { e ->
                Log.e(TAG, "创建会话失败", e)
            }
        )
    }
    
    /**
     * 为非正餐保存快照（零食、饮料等）
     * 
     * 即使不开始用餐会话，也保存快照到数据库，以便在历史记录中显示
     * 同时创建一个已完成的会话记录，这样可以在历史列表中显示
     * 
     * @param sessionId 会话ID（由调用方提供，用于UI状态同步）
     */
    private suspend fun saveSnapshotForNonMeal(
        response: com.rokid.nutrition.phone.network.model.VisionAnalyzeResponse, 
        imageUrl: String,
        sessionId: String
    ) {
        DebugLogger.i(TAG, "saveSnapshotForNonMeal 开始, imageUrl=$imageUrl, sessionId=$sessionId")
        try {
            val tempSessionId = sessionId
            DebugLogger.i(TAG, "使用会话ID: $tempSessionId")
            
            // 获取食物分类作为 mealType（从后端返回的 category 字段）
            val category = response.rawLlm.foods.firstOrNull()?.category?.lowercase() ?: "snack"
            val mealType = when (category) {
                "beverage" -> "beverage"
                "dessert" -> "dessert"
                "fruit" -> "fruit"
                else -> "snack"
            }
            
            // 先创建一个已完成的会话记录（这样可以在历史列表中显示）
            sessionRepository.saveNonMealSession(
                sessionId = tempSessionId,
                mealType = mealType,
                totalCalories = response.snapshot.nutrition.calories
            )
            DebugLogger.i(TAG, "非正餐会话已创建: sessionId=$tempSessionId, mealType=$mealType")
            
            // 保存快照到本地数据库
            sessionRepository.saveSnapshot(
                sessionId = tempSessionId,
                visionResult = response,
                imageUrl = imageUrl
            )
            DebugLogger.i(TAG, "非正餐快照已保存: sessionId=$tempSessionId, imageUrl=$imageUrl")
        } catch (e: Exception) {
            DebugLogger.e(TAG, "保存非正餐快照失败", e)
        }
    }
    
    /**
     * 构建营养结果
     */
    private fun buildNutritionResult(response: com.rokid.nutrition.phone.network.model.VisionAnalyzeResponse): NutritionResult {
        val foodName = bluetoothManager.formatFoodName(
            response.rawLlm.foods.map {
                com.rokid.nutrition.phone.bluetooth.FoodItem(
                    dishName = it.getDisplayName(),
                    cookingMethod = it.cookingMethod,
                    weightG = it.totalWeightG,
                    confidence = it.confidence
                )
            }
        )
        // 使用后端返回的 suggestion 字段，如果没有则为空
        val suggestion = response.rawLlm.suggestion ?: ""
        return NutritionResult(
            foodName = foodName,
            calories = response.snapshot.nutrition.calories,
            protein = response.snapshot.nutrition.protein,
            carbs = response.snapshot.nutrition.carbs,
            fat = response.snapshot.nutrition.fat,
            suggestion = suggestion
        )
    }
    
    /**
     * 从更新响应构建营养结果
     */
    private fun buildNutritionResultFromUpdate(response: com.rokid.nutrition.phone.network.model.MealUpdateAnalyzeResponse): NutritionResult {
        val foodName = bluetoothManager.formatFoodName(
            response.rawLlm.foods.map {
                com.rokid.nutrition.phone.bluetooth.FoodItem(
                    dishName = it.getDisplayName(),
                    cookingMethod = it.cookingMethod,
                    weightG = it.totalWeightG,
                    confidence = it.confidence
                )
            }
        )
        return NutritionResult(
            foodName = foodName,
            calories = response.snapshot.nutrition.calories,
            protein = response.snapshot.nutrition.protein,
            carbs = response.snapshot.nutrition.carbs,
            fat = response.snapshot.nutrition.fat,
            suggestion = response.message
        )
    }
    
    /**
     * 处理分析错误
     */
    private fun handleAnalysisError(e: Throwable) {
        Log.e(TAG, "分析失败", e)
        val errorMessage = e.message ?: "未知错误"
        
        when {
            errorMessage.contains("未检测到食物", ignoreCase = true) ||
            errorMessage.contains("not food", ignoreCase = true) -> {
                bluetoothManager.notifyNotFood()
                _uiState.update { it.copy(isProcessing = false, statusMessage = "未检测到食物") }
            }
            errorMessage.contains("network", ignoreCase = true) ||
            errorMessage.contains("timeout", ignoreCase = true) -> {
                bluetoothManager.notifyNoNetwork()
                _uiState.update { it.copy(isProcessing = false, statusMessage = "网络连接失败") }
            }
            else -> {
                bluetoothManager.notifyAiError()
                _uiState.update { it.copy(isProcessing = false, statusMessage = "识别失败") }
            }
        }
    }
    
    /**
     * 处理接收到的指令
     */
    private fun handleCommandReceived(command: String) {
        viewModelScope.launch {
            when (command) {
                "start_meal" -> startMealSession()
                "end_meal" -> endMealSession()
            }
        }
    }
    
    /**
     * 开始用餐会话
     */
    private suspend fun startMealSession() {
        // 需要先有一张图片才能开始会话
        // 这里简化处理，实际应该等待第一张图片
        _uiState.update {
            it.copy(
                hasActiveSession = true,
                sessionStatus = "用餐中"
            )
        }
        
        bluetoothManager.sendSessionStatus(
            sessionId = "",
            status = "active",
            totalConsumed = 0.0,
            message = "用餐已开始"
        )
    }
    
    /**
     * 结束用餐会话（眼镜指令触发）
     */
    private suspend fun endMealSession() {
        Log.d(TAG, "收到结束用餐指令")
        
        // 检查是否有活跃会话
        if (!mealSessionManager.isActive) {
            Log.w(TAG, "没有活跃的用餐会话")
            return
        }
        
        val sessionId = currentSessionId ?: mealSessionManager.state?.sessionId
        if (sessionId == null) {
            Log.w(TAG, "会话 ID 为空")
            return
        }
        
        // 收集上下文数据
        val mealContext = collectMealContext()
        val mealContextPayload = mealContext?.let {
            com.rokid.nutrition.phone.network.model.MealContextPayload(
                sessionId = it.sessionId,
                startTime = it.startTime,
                durationMinutes = it.durationMinutes,
                recognitionCount = it.recognitionCount,
                totalConsumedSoFar = it.totalConsumedSoFar,
                eatingSpeed = it.eatingSpeed
            )
        }
        
        val dailyContext = dailyNutritionTracker.getDailyContext()
        val dailyContextPayload = com.rokid.nutrition.phone.network.model.DailyContextPayload(
            totalCaloriesToday = dailyContext.totalCaloriesToday,
            totalProteinToday = dailyContext.totalProteinToday,
            totalCarbsToday = dailyContext.totalCarbsToday,
            totalFatToday = dailyContext.totalFatToday,
            mealCountToday = dailyContext.mealCountToday,
            lastMealHoursAgo = dailyContext.lastMealHoursAgo
        )
        
        val userProfilePayload = userProfile?.let {
            UserProfilePayload(
                age = it.age, gender = it.gender, bmi = it.bmi,
                activityLevel = it.activityLevel, healthGoal = it.healthGoal,
                targetWeight = it.targetWeight, healthConditions = it.healthConditions,
                dietaryPreferences = it.dietaryPreferences
            )
        }
        
        // 获取已消耗的营养数据
        val consumed = mealSessionManager.getTotalConsumed()
        val localCalories = consumed?.calories ?: 0.0
        
        // 调用后端结束会话（传递完整上下文）
        val result = sessionRepository.endSession(
            sessionId = sessionId,
            localCalories = localCalories,
            mealContext = mealContextPayload,
            dailyContext = dailyContextPayload,
            userProfile = userProfilePayload
        )
        
        result.fold(
            onSuccess = { response ->
                Log.d(TAG, "会话结束成功: ${response.message}")
                
                // 发送用餐总结到眼镜
                response.mealSummary?.let { summary ->
                    bluetoothManager.sendMealSummary(summary)
                } ?: run {
                    bluetoothManager.sendSessionStatus(
                        sessionId = sessionId,
                        status = "ended",
                        totalConsumed = response.finalStats.totalConsumed,
                        message = response.message
                    )
                }
                
                // 更新今日摄入统计
                val mealNutrition = com.rokid.nutrition.phone.network.model.NutritionTotal(
                    calories = response.finalStats.totalConsumed,
                    protein = response.mealSummary?.totalProtein ?: 0.0,
                    carbs = response.mealSummary?.totalCarbs ?: 0.0,
                    fat = response.mealSummary?.totalFat ?: 0.0
                )
                dailyNutritionTracker.updateDailyTotals(mealNutrition)
                
                // 清除本地会话状态
                mealSessionManager.clearSession()
                currentSessionId = null
                sessionStartTime = null
                
                // 更新 UI
                _uiState.update {
                    it.copy(
                        hasActiveSession = false,
                        sessionStatus = "空闲",
                        totalCalories = response.finalStats.totalConsumed,
                        sessionStartTime = null
                    )
                }
            },
            onFailure = { e ->
                Log.e(TAG, "结束会话失败", e)
                
                // 即使后端失败，也清除本地状态
                mealSessionManager.clearSession()
                currentSessionId = null
                sessionStartTime = null
                
                // 发送本地计算的数据到眼镜
                bluetoothManager.sendSessionStatus(
                    sessionId = sessionId,
                    status = "ended",
                    totalConsumed = consumed?.calories ?: 0.0,
                    message = "用餐已结束"
                )
                
                _uiState.update {
                    it.copy(
                        hasActiveSession = false,
                        sessionStatus = "空闲",
                        sessionStartTime = null
                    )
                }
            }
        )
    }
    
    /**
     * 手动触发远程拍照
     * 
     * 同时使用两种方式触发眼镜拍照：
     * 1. SDK 的 takeGlassPhoto API（可能需要眼镜端特定支持）
     * 2. 自定义消息 sendTakePhotoCommand（眼镜端应用订阅）
     */
    fun takePhoto() {
        DebugLogger.d(TAG, "════════════════════════════════════════")
        DebugLogger.d(TAG, "=== takePhoto() 被调用 ===")
        DebugLogger.d(TAG, "连接状态: ${bluetoothManager.isConnected()}")
        
        if (!bluetoothManager.isConnected()) {
            DebugLogger.w(TAG, "眼镜未连接")
            _uiState.update { it.copy(statusMessage = "请先连接眼镜") }
            return
        }
        
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(statusMessage = "正在拍照...", isProcessing = true) }
                DebugLogger.d(TAG, "更新 UI 状态为拍照中")
                
                // 通知眼镜 AI 开始处理
                bluetoothManager.notifyAiStart()
                DebugLogger.d(TAG, "已通知眼镜 AI 开始")
                
                // 同时发送两种拍照命令，确保眼镜端能收到
                // 方式1: 发送自定义消息（眼镜端应用订阅）
                DebugLogger.d(TAG, "方式1: 发送自定义拍照命令 (take_photo)")
                bluetoothManager.sendTakePhotoCommand()
                
                // 方式2: 使用 SDK 远程拍照功能（可能需要眼镜端特定支持）
                DebugLogger.d(TAG, "方式2: 调用 SDK takeGlassPhoto")
                bluetoothManager.takeGlassPhoto(
                    width = 1920,
                    height = 1080,
                    quality = 85
                ) { imageData ->
                    DebugLogger.d(TAG, "SDK 拍照回调: imageData=${imageData?.size ?: "null"} bytes")
                    
                    if (imageData != null && imageData.isNotEmpty()) {
                        DebugLogger.d(TAG, "SDK 拍照成功，图片大小: ${imageData.size} bytes")
                        // 收到图片，处理分析
                        handleImageReceived(ImageData(
                            data = imageData,
                            format = "jpeg",
                            timestamp = System.currentTimeMillis(),
                            isManualCapture = true
                        ))
                    } else {
                        // SDK 拍照失败，等待眼镜端通过自定义消息响应
                        DebugLogger.w(TAG, "SDK 拍照失败，等待眼镜端自定义消息响应...")
                        // 不立即显示失败，因为眼镜端可能通过自定义消息响应
                    }
                }
                
                DebugLogger.d(TAG, "拍照指令已发送，等待回调")
                DebugLogger.d(TAG, "════════════════════════════════════════")
                
            } catch (e: Exception) {
                DebugLogger.e(TAG, "拍照异常", e)
                _uiState.update { it.copy(statusMessage = "拍照异常: ${e.message}", isProcessing = false) }
                bluetoothManager.notifyAiError()
            }
        }
    }
    
    /**
     * 连接眼镜 - 使用自动连接功能
     * 会自动搜索并连接以 "Glasses" 开头的设备
     */
    fun connectGlasses() {
        Log.d(TAG, "connectGlasses() 被调用")
        _uiState.update { it.copy(statusMessage = "正在连接眼镜...") }
        bluetoothManager.autoConnect()
    }
    
    /**
     * 开始扫描设备（手动模式，显示设备列表）
     */
    fun startDeviceScan() {
        Log.d(TAG, "开始扫描眼镜设备（手动模式）...")
        _uiState.update { 
            it.copy(
                showDeviceSelector = true,
                scannedDevices = emptyList(),
                statusMessage = "正在搜索眼镜..."
            )
        }
        bluetoothManager.startScan()
        observeScannedDevices()
    }
    
    /**
     * 监听扫描到的设备
     */
    private fun observeScannedDevices() {
        viewModelScope.launch {
            bluetoothManager.scannedDevices.collect { devices ->
                val scannedList = devices.map { device ->
                    ScannedDevice(
                        name = device.name ?: "Rokid Glasses",
                        address = device.address
                    )
                }
                _uiState.update { it.copy(scannedDevices = scannedList) }
                
                // 如果只扫描到一个设备，可以自动连接
                // 或者等待用户选择
                Log.d(TAG, "扫描到 ${devices.size} 个设备")
            }
        }
    }
    
    /**
     * 选择并连接设备
     */
    fun selectDevice(address: String) {
        val device = bluetoothManager.scannedDevices.value.find { it.address == address }
        if (device != null) {
            Log.d(TAG, "选择设备: ${device.name ?: device.address}")
            _uiState.update { 
                it.copy(
                    showDeviceSelector = false,
                    statusMessage = "正在连接..."
                )
            }
            bluetoothManager.initBluetooth(device)
        }
    }
    
    /**
     * 取消设备选择
     */
    fun cancelDeviceSelection() {
        bluetoothManager.stopScan()
        _uiState.update { 
            it.copy(
                showDeviceSelector = false,
                statusMessage = ""
            )
        }
    }
    
    /**
     * 断开连接
     */
    fun disconnectGlasses() {
        bluetoothManager.disconnect()
    }
    
    /**
     * 清除保存的连接信息并重新扫描
     */
    fun forgetAndRescan() {
        bluetoothManager.clearSavedCredentials()
        startDeviceScan()
    }
    
    /**
     * 获取最新的食物数据（用于详情页面）
     */
    fun getLatestFoodData(): com.rokid.nutrition.phone.network.model.VisionAnalyzeResponse? {
        return latestFoodData
    }
    
    /**
     * 添加到识别历史记录
     */
    private fun addToRecognitionHistory(response: com.rokid.nutrition.phone.network.model.VisionAnalyzeResponse) {
        val category = response.rawLlm.foods.firstOrNull()?.category ?: "unknown"
        val foodName = response.rawLlm.foods.joinToString(" · ") { it.getDisplayName() }
        
        val history = RecognitionHistory(
            timestamp = System.currentTimeMillis(),
            foodData = response,
            category = category,
            totalCalories = response.snapshot.nutrition.calories,
            foodName = foodName
        )
        
        val currentList = _recognitionHistory.value.toMutableList()
        currentList.add(0, history)  // 添加到列表开头
        
        // 最多保留 20 条记录
        if (currentList.size > 20) {
            currentList.removeAt(currentList.size - 1)
        }
        
        _recognitionHistory.value = currentList
        Log.d(TAG, "添加识别历史: $foodName, category=$category")
    }
    
    /**
     * 手动结束用餐会话
     * 
     * 完整流程：
     * 1. 收集 MealContext（用餐时长、进食速度）
     * 2. 收集 DailyContext（今日摄入）
     * 3. 获取 UserProfile
     * 4. 调用后端 API
     * 5. 发送用餐总结到眼镜
     * 6. 更新今日摄入统计
     */
    fun endMealSessionManually() {
        viewModelScope.launch {
            try {
                val sessionId = currentSessionId ?: userManager.getCurrentSessionId()
                sessionId?.let { sid ->
                    // 1. 收集 MealContext
                    val mealContext = collectMealContext()
                    val mealContextPayload = mealContext?.let {
                        com.rokid.nutrition.phone.network.model.MealContextPayload(
                            sessionId = it.sessionId,
                            startTime = it.startTime,
                            durationMinutes = it.durationMinutes,
                            recognitionCount = it.recognitionCount,
                            totalConsumedSoFar = it.totalConsumedSoFar,
                            eatingSpeed = it.eatingSpeed
                        )
                    }
                    
                    // 2. 收集 DailyContext
                    val dailyContext = dailyNutritionTracker.getDailyContext()
                    val dailyContextPayload = com.rokid.nutrition.phone.network.model.DailyContextPayload(
                        totalCaloriesToday = dailyContext.totalCaloriesToday,
                        totalProteinToday = dailyContext.totalProteinToday,
                        totalCarbsToday = dailyContext.totalCarbsToday,
                        totalFatToday = dailyContext.totalFatToday,
                        mealCountToday = dailyContext.mealCountToday,
                        lastMealHoursAgo = dailyContext.lastMealHoursAgo
                    )
                    
                    // 3. 获取 UserProfile
                    val userProfilePayload = userProfile?.let {
                        UserProfilePayload(
                            age = it.age, gender = it.gender, bmi = it.bmi,
                            activityLevel = it.activityLevel, healthGoal = it.healthGoal,
                            targetWeight = it.targetWeight, healthConditions = it.healthConditions,
                            dietaryPreferences = it.dietaryPreferences
                        )
                    }
                    
                    // 获取本地热量数据（后端失败时使用）
                    val localCalories = mealSessionManager.getTotalConsumed()?.calories ?: 0.0
                    
                    Log.d(TAG, "结束用餐: mealContext=$mealContextPayload, dailyContext=$dailyContextPayload")
                    
                    // 4. 调用后端结束会话（传递完整上下文）
                    val result = sessionRepository.endSession(
                        sessionId = sid,
                        localCalories = localCalories,
                        mealContext = mealContextPayload,
                        dailyContext = dailyContextPayload,
                        userProfile = userProfilePayload
                    )
                    
                    result.fold(
                        onSuccess = { response ->
                            Log.d(TAG, "会话结束成功: ${response.message}")
                            
                            // 5. 发送用餐总结到眼镜
                            response.mealSummary?.let { summary ->
                                val sent = bluetoothManager.sendMealSummary(summary)
                                Log.d(TAG, "用餐总结发送${if (sent) "成功" else "失败（眼镜未连接）"}")
                            } ?: run {
                                // 如果没有 mealSummary，发送会话状态
                                bluetoothManager.sendSessionStatus(
                                    sessionId = sid,
                                    status = "ended",
                                    totalConsumed = response.finalStats.totalConsumed,
                                    message = response.message
                                )
                            }
                            
                            // 显示用餐建议（如果有）
                            response.advice?.let { advice ->
                                Log.d(TAG, "用餐建议: ${advice.summary}")
                                _uiState.update { it.copy(statusMessage = advice.summary) }
                            }
                            
                            // 6. 更新今日摄入统计
                            val mealNutrition = com.rokid.nutrition.phone.network.model.NutritionTotal(
                                calories = response.finalStats.totalConsumed,
                                protein = response.mealSummary?.totalProtein ?: 0.0,
                                carbs = response.mealSummary?.totalCarbs ?: 0.0,
                                fat = response.mealSummary?.totalFat ?: 0.0
                            )
                            dailyNutritionTracker.updateDailyTotals(mealNutrition)
                            Log.d(TAG, "今日摄入已更新: +${mealNutrition.calories} kcal")
                        },
                        onFailure = { e ->
                            Log.e(TAG, "结束会话失败", e)
                            bluetoothManager.sendSessionStatus(
                                sessionId = sid,
                                status = "ended",
                                totalConsumed = mealSessionManager.getTotalConsumed()?.calories ?: 0.0,
                                message = "用餐已结束"
                            )
                        }
                    )
                    
                    // 清除本地状态
                    mealSessionManager.clearSession()
                    currentSessionId = null
                    sessionStartTime = null
                    userManager.setCurrentSessionId(null)  // 清除 UserManager 中的 session_id
                    
                    _uiState.update {
                        it.copy(
                            hasActiveSession = false,
                            sessionStatus = "空闲",
                            sessionStartTime = null
                        )
                    }
                    
                    Log.d(TAG, "手动结束用餐会话")
                }
            } catch (e: Exception) {
                Log.e(TAG, "结束用餐会话失败", e)
            }
        }
    }
    
    /**
     * 收集用餐上下文
     * 
     * @return MealContext 用餐上下文数据
     */
    fun collectMealContext(): MealContext? {
        val sessionId = currentSessionId ?: userManager.getCurrentSessionId() ?: return null
        val startTime = sessionStartTime ?: return null
        
        val durationMinutes = (System.currentTimeMillis() - startTime) / 60000.0
        val eatingSpeed = classifyEatingSpeed(durationMinutes)
        val totalConsumed = mealSessionManager.getTotalConsumed()?.calories ?: 0.0
        val recognitionCount = _recognitionHistory.value.size
        
        Log.d(TAG, "收集用餐上下文: duration=$durationMinutes min, speed=$eatingSpeed, consumed=$totalConsumed kcal")
        
        return MealContext(
            sessionId = sessionId,
            startTime = startTime,
            durationMinutes = durationMinutes,
            recognitionCount = recognitionCount,
            totalConsumedSoFar = totalConsumed,
            eatingSpeed = eatingSpeed
        )
    }
    
    /**
     * 分类进食速度
     * 
     * @param durationMinutes 用餐时长（分钟）
     * @return "fast" | "normal" | "slow"
     */
    fun classifyEatingSpeed(durationMinutes: Double): String {
        return when {
            durationMinutes < 10 -> "fast"
            durationMinutes in 15.0..30.0 -> "normal"
            durationMinutes > 45 -> "slow"
            else -> "normal"
        }
    }
    
    /**
     * 手动开始用餐会话
     * 
     * 流程：
     * 1. 检查眼镜连接状态
     * 2. 拍摄基线照片
     * 3. 上传分析照片
     * 4. 创建用餐会话
     */
    fun startMealSessionManually() {
        viewModelScope.launch {
            // 检查眼镜连接
            if (!bluetoothManager.isConnected()) {
                _uiState.update { it.copy(statusMessage = "请先连接眼镜") }
                return@launch
            }
            
            _uiState.update { 
                it.copy(isProcessing = true, statusMessage = "正在拍摄基线照片...") 
            }
            
            try {
                // 使用现有的 takeGlassPhoto 方法拍摄基线照片
                bluetoothManager.takeGlassPhoto(
                    width = 1920,
                    height = 1080,
                    quality = 85
                ) { imageData ->
                    if (imageData != null && imageData.isNotEmpty()) {
                        Log.d(TAG, "基线照片拍摄成功，大小: ${imageData.size} bytes")
                        // 处理基线照片
                        viewModelScope.launch {
                            handleBaselineImageReceived(ImageData(
                                data = imageData,
                                format = "jpeg",
                                timestamp = System.currentTimeMillis(),
                                isManualCapture = true
                            ))
                        }
                    } else {
                        Log.e(TAG, "基线照片拍摄失败")
                        _uiState.update { 
                            it.copy(isProcessing = false, statusMessage = "拍照失败，请重试") 
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "开始用餐会话失败", e)
                _uiState.update { 
                    it.copy(isProcessing = false, statusMessage = "拍照异常: ${e.message}") 
                }
            }
        }
    }
    
    /**
     * 处理基线照片（手动开始用餐时）
     * 
     * 复用 handleNewAnalysis 的大部分逻辑，但强制创建用餐会话
     */
    private suspend fun handleBaselineImageReceived(imageData: ImageData) {
        bluetoothManager.notifyAiStart()
        bluetoothManager.notifyUploading()
        
        val profilePayload = userProfile?.let {
            UserProfilePayload(
                age = it.age, gender = it.gender, bmi = it.bmi,
                activityLevel = it.activityLevel, healthGoal = it.healthGoal,
                targetWeight = it.targetWeight, healthConditions = it.healthConditions,
                dietaryPreferences = it.dietaryPreferences
            )
        }
        
        var uploadedImageUrl = ""
        
        val result = networkManager.uploadAndAnalyzeWithProgress(
            imageData = imageData.data,
            userProfile = profilePayload,
            onUploadComplete = { imageUrl ->
                uploadedImageUrl = imageUrl
                Log.d(TAG, "基线照片上传完成，URL: $imageUrl")
                bluetoothManager.notifyAnalyzing()
                _uiState.update { it.copy(statusMessage = "识别菜品中...") }
            },
            onAnalyzeComplete = {
                bluetoothManager.notifyCalculating()
                _uiState.update { it.copy(statusMessage = "创建用餐会话...") }
            }
        )
        
        result.fold(
            onSuccess = { response ->
                bluetoothManager.notifyComplete()
                
                // 保存完整的后端响应数据
                latestFoodData = response
                
                // 添加到识别历史记录
                addToRecognitionHistory(response)
                
                // 构建营养结果
                val nutritionResult = buildNutritionResult(response)
                bluetoothManager.sendNutritionResult(nutritionResult)
                
                // 强制开始会话（不管食物类型）
                sessionStartTime = System.currentTimeMillis()
                startMealSessionInternal(response, uploadedImageUrl)
                
                // 更新 UI
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = "用餐监测已开始",
                        latestResult = nutritionResult,
                        latestImageUrl = uploadedImageUrl,
                        latestSessionId = null,  // 会话ID由 startMealSessionInternal 设置
                        hasActiveSession = true,
                        sessionStatus = "用餐中",
                        sessionStartTime = sessionStartTime
                    )
                }
                
                Log.d(TAG, "手动开始用餐会话成功")
            },
            onFailure = { e ->
                handleAnalysisError(e)
                _uiState.update { it.copy(isProcessing = false) }
            }
        )
    }
    
    /**
     * 显示删除确认对话框
     */
    fun showDeleteConfirmDialog(session: com.rokid.nutrition.phone.data.entity.MealSessionEntity) {
        _uiState.update { 
            it.copy(showDeleteDialog = true, sessionToDelete = session) 
        }
    }
    
    /**
     * 隐藏删除确认对话框
     */
    fun hideDeleteConfirmDialog() {
        _uiState.update { 
            it.copy(showDeleteDialog = false, sessionToDelete = null) 
        }
    }
    
    /**
     * 删除用餐会话
     * 
     * 流程：
     * 1. 删除本地数据
     * 2. 如果是今天的记录，更新 DailyNutritionTracker
     * 3. 调用后端 API（失败时记录日志）
     */
    fun deleteMealSession(session: com.rokid.nutrition.phone.data.entity.MealSessionEntity) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "删除用餐会话: ${session.sessionId}")
                
                // 1. 删除本地数据
                val result = sessionRepository.deleteSession(session.sessionId)
                result.onFailure { e ->
                    Log.e(TAG, "删除本地数据失败", e)
                    _uiState.update { it.copy(statusMessage = "删除失败") }
                    return@launch
                }
                
                // 2. 如果是今天的记录，更新 DailyNutritionTracker
                if (isToday(session.startTime)) {
                    dailyNutritionTracker.subtractNutrition(
                        calories = session.totalConsumedKcal ?: session.totalServedKcal ?: 0.0,
                        protein = 0.0,  // 会话实体中没有详细营养数据
                        carbs = 0.0,
                        fat = 0.0,
                        mealTimestamp = session.startTime,
                        decreaseMealCount = true  // 删除整个用餐记录时减少用餐次数
                    )
                    Log.d(TAG, "已更新今日营养统计")
                }
                
                // 3. 调用后端 API（失败时记录日志，不影响本地删除）
                val apiResult = networkManager.deleteMealSession(session.sessionId)
                apiResult.onFailure { e ->
                    Log.w(TAG, "后端删除失败，本地已删除", e)
                }
                
                // 隐藏对话框并显示成功消息
                _uiState.update { 
                    it.copy(
                        showDeleteDialog = false, 
                        sessionToDelete = null,
                        statusMessage = "记录已删除"
                    ) 
                }
                
                Log.d(TAG, "用餐会话删除成功: ${session.sessionId}")
            } catch (e: Exception) {
                Log.e(TAG, "删除用餐会话失败", e)
                _uiState.update { 
                    it.copy(
                        showDeleteDialog = false, 
                        sessionToDelete = null,
                        statusMessage = "删除失败"
                    ) 
                }
            }
        }
    }
    
    /**
     * 判断时间戳是否是今天
     */
    private fun isToday(timestamp: Long): Boolean {
        val today = java.util.Calendar.getInstance()
        val target = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
        return today.get(java.util.Calendar.YEAR) == target.get(java.util.Calendar.YEAR) &&
               today.get(java.util.Calendar.DAY_OF_YEAR) == target.get(java.util.Calendar.DAY_OF_YEAR)
    }
}

/**
 * 识别历史记录
 */
data class RecognitionHistory(
    val timestamp: Long,
    val foodData: com.rokid.nutrition.phone.network.model.VisionAnalyzeResponse,
    val category: String,
    val totalCalories: Double,
    val foodName: String
) {
    /**
     * 获取分类颜色
     */
    fun getCategoryColor(): Long {
        return when (category.lowercase()) {
            "meal" -> 0xFF4CAF50  // 绿色 - 正餐
            "snack" -> 0xFFFF9800  // 橙色 - 零食
            "beverage" -> 0xFF2196F3  // 蓝色 - 饮料
            "dessert" -> 0xFFE91E63  // 粉色 - 甜点
            "fruit" -> 0xFF8BC34A  // 浅绿色 - 水果
            else -> 0xFF9E9E9E  // 灰色 - 其他
        }
    }
    
    /**
     * 获取分类文本
     */
    fun getCategoryText(): String {
        return when (category.lowercase()) {
            "meal" -> "正餐"
            "snack" -> "零食"
            "beverage" -> "饮料"
            "dessert" -> "甜点"
            "fruit" -> "水果"
            else -> "其他"
        }
    }
}

/**
 * 首页 UI 状态
 */
data class HomeUiState(
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val glassesName: String? = null,
    val batteryLevel: Int = 0,
    val hasActiveSession: Boolean = false,
    val sessionStatus: String = "空闲",
    val elapsedTime: String = "00:00",
    val totalCalories: Double = 0.0,
    val latestResult: NutritionResult? = null,
    val latestImageUrl: String? = null,  // 最新识别的图片URL
    val latestSessionId: String? = null,  // 最新识别的会话ID（用于编辑）
    val isProcessing: Boolean = false,
    val statusMessage: String = "",
    val scannedDevices: List<ScannedDevice> = emptyList(),
    val showDeviceSelector: Boolean = false,
    val sessionStartTime: Long? = null,  // 用餐开始时间
    val showDeleteDialog: Boolean = false,  // 是否显示删除确认对话框
    val sessionToDelete: com.rokid.nutrition.phone.data.entity.MealSessionEntity? = null  // 待删除的会话
)

/**
 * 扫描到的设备信息
 */
data class ScannedDevice(
    val name: String,
    val address: String,
    val rssi: Int = -70  // 信号强度，默认中等
)
