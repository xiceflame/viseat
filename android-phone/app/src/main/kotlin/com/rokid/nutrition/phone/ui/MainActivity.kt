package com.rokid.nutrition.phone.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.rokid.nutrition.phone.Config
import com.rokid.nutrition.phone.NutritionPhoneApp
import com.rokid.nutrition.phone.bluetooth.BluetoothManager
import com.rokid.nutrition.phone.bluetooth.GlassInfo
import com.rokid.nutrition.phone.bluetooth.GlassesConnectionService
import com.rokid.nutrition.phone.data.entity.MealSessionEntity
import com.rokid.nutrition.phone.network.NetworkManager
import com.rokid.nutrition.phone.repository.*
import com.rokid.nutrition.phone.ui.theme.NutritionPhoneTheme
import com.rokid.nutrition.phone.ui.screen.MealSessionWithPhoto
import com.rokid.nutrition.phone.ui.screen.FoodItemWithCategory
import com.rokid.nutrition.phone.ui.viewmodel.FoodDetailUiState
import com.rokid.nutrition.phone.ui.viewmodel.FoodDetailViewModel
import com.rokid.nutrition.phone.ui.viewmodel.HomeUiState
import com.rokid.nutrition.phone.ui.viewmodel.HomeViewModel
import com.rokid.nutrition.phone.ui.viewmodel.StatsViewModel
import com.rokid.nutrition.phone.repository.MealEditRepository
import com.rokid.nutrition.phone.repository.PhotoStorageRepository
import com.rokid.nutrition.phone.sync.SyncManager
import com.rokid.nutrition.phone.util.DebugLogger
import com.rokid.nutrition.phone.util.FoodItemUpdates
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private const val TAG = "MainActivity"
private const val SCREEN_TIMEOUT_MS = 15000L  // 15秒自动熄屏

class MainActivity : ComponentActivity() {
    
    private var bluetoothManager: BluetoothManager? = null
    private var homeViewModel: HomeViewModel? = null
    private var statsViewModel: StatsViewModel? = null
    private var foodDetailViewModel: FoodDetailViewModel? = null
    
    // 个性化建议 Repository（用于用餐结束后刷新建议）
    private var personalizedTipsRepo: PersonalizedTipsRepository? = null
    
    // 自动熄屏相关
    private val screenTimeoutHandler = Handler(Looper.getMainLooper())
    private val screenTimeoutRunnable = Runnable {
        // 15秒无操作后降低屏幕亮度（模拟熄屏效果）
        Log.d(TAG, "15秒无操作，降低屏幕亮度")
        setScreenBrightness(0.01f)  // 设置为最低亮度
    }
    private var isScreenDimmed = false
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as GlassesConnectionService.LocalBinder
            bluetoothManager = binder.getBluetoothManager()
            initViewModels()
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            bluetoothManager = null
        }
    }
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        Log.d(TAG, "权限请求结果: $allGranted")
        if (allGranted) {
            startBluetoothService()
        }
    }

    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化调试日志
        DebugLogger.init(this)
        DebugLogger.i(TAG, "应用启动")
        
        requestPermissionsIfNeeded()
        
        setContent {
            NutritionPhoneTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent()
                }
            }
        }
    }
    
    @Composable
    private fun MainContent() {
        val dailyNutritionTracker = remember { DailyNutritionTracker(this@MainActivity) }
        val database = remember { NutritionPhoneApp.instance.database }
        val networkManager = remember { NetworkManager.getInstance() }
        val userManager = remember { com.rokid.nutrition.phone.repository.UserManager.getInstance(this@MainActivity) }

        // 使用 mutableStateOf 来触发重组
        var viewModelReady by remember { mutableStateOf(false) }
        var homeUiState by remember { mutableStateOf(HomeUiState()) }
        var todayCalories by remember { mutableStateOf(0.0) }
        var todayMealCount by remember { mutableStateOf(0) }
        var weeklyStats by remember { mutableStateOf<List<DailyStats>>(emptyList()) }
        var sessions by remember { mutableStateOf<List<MealSessionEntity>>(emptyList()) }
        var userProfile by remember { mutableStateOf<UserProfile?>(null) }
        var glassInfo by remember { mutableStateOf<GlassInfo?>(null) }
        var recognitionHistory by remember { mutableStateOf<List<com.rokid.nutrition.phone.ui.viewmodel.RecognitionHistory>>(emptyList()) }
        var foodDetailUiState by remember { mutableStateOf(FoodDetailUiState()) }
        var sessionsWithPhotos by remember { mutableStateOf<List<MealSessionWithPhoto>>(emptyList()) }
        // 今日营养数据（从数据库获取）
        var todayNutritionData by remember { mutableStateOf(com.rokid.nutrition.phone.repository.TodayNutritionData()) }
        
        // Onboarding 状态 - 初始状态根据本地同步获取，避免启动转圈
        var isOnboardingCompleted by remember { 
            mutableStateOf<Boolean?>(
                if (userManager.hasCompletedOnboardingLocally()) true else null
            ) 
        }
        
        // 智能健康提示状态（现在由 HomeViewModel 管理，通过 homeUiState 获取）
        
        // 体重追踪状态
        var weightEntries by remember { mutableStateOf<List<com.rokid.nutrition.phone.data.entity.WeightEntryEntity>>(emptyList()) }
        var latestWeightEntry by remember { mutableStateOf<com.rokid.nutrition.phone.data.entity.WeightEntryEntity?>(null) }
        
        // 使用 MainActivity 的 personalizedTipsRepo（如果已初始化）
        // 如果还没初始化（ViewModel 还没准备好），创建一个临时的
        val tipsRepo = personalizedTipsRepo ?: remember { 
            PersonalizedTipsRepository(this@MainActivity, networkManager.apiService) 
        }
        
        // 检查 Onboarding 状态
        LaunchedEffect(Unit) {
            val repo = UserProfileRepository(database.userProfileDao(), networkManager)
            
            // 首先检查用户是否在本设备上完成过引导
            val completedLocally = userManager.hasCompletedOnboardingLocally()
            Log.d(TAG, "检查本地引导完成状态: completedLocally=$completedLocally")
            
            if (completedLocally) {
                // 用户已在本设备完成过引导，直接进入主页
                isOnboardingCompleted = true
                Log.d(TAG, "用户已在本设备完成引导，直接进入主页")
            } else {
                // 用户未在本设备完成过引导，先设置状态为 false 让 UI 跳转到引导页
                isOnboardingCompleted = false
                Log.d(TAG, "用户未在本设备完成引导，跳转到引导流程")
                
                // 在后台尝试从后端恢复档案数据（不阻塞 UI）
                val hasLocalProfile = repo.hasProfile()
                if (!hasLocalProfile) {
                    launch {
                        val deviceId = userManager.getDeviceId()
                        Log.d(TAG, "后台尝试从后端恢复档案用于预填充: deviceId=$deviceId")
                        try {
                            repo.syncFromBackend(deviceId)
                        } catch (e: Exception) {
                            Log.e(TAG, "后台恢复档案异常", e)
                        }
                    }
                }
            }
        }
        
        // 智能健康提示现在由 HomeViewModel 管理，不需要在这里单独收集
        
        // 收集体重记录
        LaunchedEffect(Unit) {
            database.weightEntryDao().getAllEntries().collectLatest { entries ->
                weightEntries = entries
                latestWeightEntry = entries.firstOrNull()
                Log.d(TAG, "体重记录更新: ${entries.size} 条")
            }
        }
        
        // 检查 ViewModel 是否准备好
        LaunchedEffect(Unit) {
            while (homeViewModel == null) {
                kotlinx.coroutines.delay(100)
            }
            viewModelReady = true
            Log.d(TAG, "ViewModel 已准备好")
        }
        
        // 收集 HomeViewModel 状态
        LaunchedEffect(viewModelReady) {
            if (viewModelReady && homeViewModel != null) {
                Log.d(TAG, "开始收集 HomeViewModel 状态")
                homeViewModel?.uiState?.collectLatest { state ->
                    Log.d(TAG, "收到 UI 状态更新: connectionState=${state.connectionState}")
                    homeUiState = state
                    
                    // 当连接成功时，获取眼镜信息
                    if (state.connectionState is com.rokid.nutrition.phone.bluetooth.ConnectionState.Connected) {
                        bluetoothManager?.getGlassInfo { info ->
                            Log.d(TAG, "获取到眼镜信息: $info")
                            glassInfo = info
                        }
                    }
                }
            }
        }
        
        // 收集识别历史记录，并在更新时刷新统计
        LaunchedEffect(viewModelReady) {
            if (viewModelReady && homeViewModel != null) {
                homeViewModel?.recognitionHistory?.collectLatest { history ->
                    val previousCount = recognitionHistory.size
                    recognitionHistory = history
                    // 如果有新的识别记录，刷新统计数据
                    if (history.size > previousCount && previousCount > 0) {
                        Log.d(TAG, "识别历史更新，刷新统计数据: ${previousCount} -> ${history.size}")
                        statsViewModel?.refreshData()
                    }
                }
            }
        }
        
        // 收集 StatsViewModel 状态
        LaunchedEffect(viewModelReady) {
            if (viewModelReady && statsViewModel != null) {
                statsViewModel?.todayCalories?.collectLatest { calories ->
                    todayCalories = calories
                    // 同时更新用餐次数
                    todayMealCount = dailyNutritionTracker.getTodayMealCount()
                }
            }
        }
        
        LaunchedEffect(viewModelReady) {
            if (viewModelReady && statsViewModel != null) {
                statsViewModel?.weeklyStats?.collectLatest { stats ->
                    weeklyStats = stats
                }
            }
        }
        
        // 收集今日营养数据（从数据库）
        LaunchedEffect(viewModelReady) {
            if (viewModelReady && statsViewModel != null) {
                statsViewModel?.todayNutritionData?.collectLatest { data ->
                    todayNutritionData = data
                    Log.d(TAG, "今日营养数据: calories=${data.totalCalories}, protein=${data.totalProtein}, carbs=${data.totalCarbs}, fat=${data.totalFat}")
                }
            }
        }
        
        // 收集会话列表，当有新会话时刷新统计
        LaunchedEffect(Unit) {
            database.mealSessionDao().getRecentSessions(20).collectLatest { list ->
                val previousCount = sessions.size
                sessions = list
                // 如果有新会话，刷新统计数据
                if (list.size > previousCount || (list.isNotEmpty() && previousCount == 0)) {
                    Log.d(TAG, "会话列表更新，刷新统计数据: ${previousCount} -> ${list.size}")
                    statsViewModel?.refreshData()
                }
            }
        }
        
        // 收集用户档案
        LaunchedEffect(Unit) {
            val repo = UserProfileRepository(database.userProfileDao())
            repo.getProfileFlow().collectLatest { profile ->
                userProfile = profile
            }
        }
        
        // 收集 FoodDetailViewModel 状态
        LaunchedEffect(viewModelReady) {
            if (viewModelReady && foodDetailViewModel != null) {
                foodDetailViewModel?.uiState?.collectLatest { state ->
                    foodDetailUiState = state
                }
            }
        }
        
        // 构建带照片和食物列表的会话列表
        LaunchedEffect(sessions) {
            Log.d(TAG, "Building sessionsWithPhotos, sessions count: ${sessions.size}")
            sessionsWithPhotos = sessions.map { session ->
                // 尝试获取会话的照片和食物列表
                val snapshot = database.mealSnapshotDao().getLatestSnapshot(session.sessionId)
                val photoUri = snapshot?.localImagePath ?: snapshot?.imageUrl
                
                // 获取食物列表（带分类）
                val foods = if (snapshot != null) {
                    database.snapshotFoodDao().getFoodsForSnapshot(snapshot.id).map { food ->
                        FoodItemWithCategory(
                            name = food.chineseName ?: food.name,
                            category = food.category ?: "meal",  // 默认为正餐
                            calories = food.caloriesKcal
                        )
                    }
                } else emptyList()
                
                Log.d(TAG, "Session ${session.sessionId}: snapshot=${snapshot != null}, photoUri=$photoUri, foods=${foods.size}")
                MealSessionWithPhoto(session = session, photoUri = photoUri, foods = foods)
            }
            Log.d(TAG, "sessionsWithPhotos built: ${sessionsWithPhotos.size}")
        }
        
        MainNavigation(
            homeUiState = homeUiState,
            todayCalories = todayCalories,
            todayMealCount = todayMealCount,
            weeklyStats = weeklyStats,
            sessions = sessions,
            sessionsWithPhotos = sessionsWithPhotos,
            recognitionHistory = recognitionHistory,
            userProfile = userProfile,
            glassInfo = glassInfo,
            foodDetailUiState = foodDetailUiState,
            todayNutritionData = todayNutritionData,
            isOnboardingCompleted = isOnboardingCompleted,
            onOnboardingComplete = {
                // 刷新 onboarding 状态
                lifecycleScope.launch {
                    val repo = UserProfileRepository(database.userProfileDao())
                    isOnboardingCompleted = repo.isOnboardingCompleted()
                    Log.d(TAG, "Onboarding 完成，刷新状态: $isOnboardingCompleted")
                }
            },
            // 智能健康提示刷新（使用 HomeViewModel 中的方法）
            onRefreshTips = {
                homeViewModel?.refreshSmartTips()
            },
            onConnectClick = { 
                Log.d(TAG, "onConnectClick - homeViewModel: $homeViewModel")
                if (homeViewModel != null) {
                    homeViewModel?.connectGlasses()
                } else {
                    Log.e(TAG, "homeViewModel 未初始化，尝试直接使用 bluetoothManager")
                    bluetoothManager?.startScan()
                }
            },
            onDisconnectClick = { bluetoothManager?.disconnect() },
            onTakePhotoClick = { 
                com.rokid.nutrition.phone.util.DebugLogger.d(TAG, "════════════════════════════════════════")
                com.rokid.nutrition.phone.util.DebugLogger.d(TAG, "=== onTakePhotoClick 被调用 ===")
                com.rokid.nutrition.phone.util.DebugLogger.d(TAG, "homeViewModel: $homeViewModel")
                com.rokid.nutrition.phone.util.DebugLogger.d(TAG, "bluetoothManager: $bluetoothManager")
                
                if (homeViewModel != null) {
                    com.rokid.nutrition.phone.util.DebugLogger.d(TAG, "调用 homeViewModel.takePhoto()")
                    homeViewModel?.takePhoto()
                } else {
                    com.rokid.nutrition.phone.util.DebugLogger.e(TAG, "homeViewModel 为 null，使用备用方法")
                    // 直接调用 BluetoothManager 的方法作为备用
                    com.rokid.nutrition.phone.util.DebugLogger.d(TAG, "调用 sendTakePhotoCommand()...")
                    bluetoothManager?.sendTakePhotoCommand()
                    com.rokid.nutrition.phone.util.DebugLogger.d(TAG, "调用 takeGlassPhoto()...")
                    bluetoothManager?.takeGlassPhoto(1920, 1080, 85) { imageData ->
                        com.rokid.nutrition.phone.util.DebugLogger.d(TAG, "直接拍照回调: ${imageData?.size ?: 0} bytes")
                    }
                }
                com.rokid.nutrition.phone.util.DebugLogger.d(TAG, "════════════════════════════════════════")
            },
            onSaveProfile = { profile ->
                lifecycleScope.launch {
                    val repo = UserProfileRepository(database.userProfileDao())
                    repo.saveProfile(profile)
                }
            },
            onSessionClick = { /* TODO: 显示会话详情 */ },
            onDeviceSelected = { address -> 
                Log.d(TAG, "onDeviceSelected: $address")
                homeViewModel?.selectDevice(address) 
            },
            onCancelDeviceSelection = { homeViewModel?.cancelDeviceSelection() },
            // 眼镜控制回调 - 使用 SDK 方法
            onSetBrightness = { brightness -> 
                bluetoothManager?.setGlassBrightness(brightness)
            },
            onSetVolume = { volume -> 
                bluetoothManager?.setGlassVolume(volume)
            },
            onSyncTime = { 
                bluetoothManager?.syncGlassTime()
            },
            onReboot = { 
                bluetoothManager?.notifyGlassReboot()
            },
            onShutdown = { 
                bluetoothManager?.notifyGlassShutdown()
            },
            onEndMealSession = {
                homeViewModel?.endMealSessionManually()
            },
            onStartMealSession = {
                homeViewModel?.startMealSessionManually()
            },
            onClearData = {
                lifecycleScope.launch {
                    try {
                        // 清除 SharedPreferences 数据
                        dailyNutritionTracker.clearAll()
                        Log.d(TAG, "SharedPreferences 数据已清除")
                        
                        // 清除数据库数据（需要在 IO 线程）
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            database.clearAllTables()
                        }
                        Log.d(TAG, "数据库数据已清除")
                        
                        // 清除本地引导完成标志，这样用户会重新进入引导页面
                        userManager.clearOnboardingCompletedLocally()
                        Log.d(TAG, "引导完成标志已清除")
                        
                        // 重置 onboarding 状态，触发重新进入引导页面
                        isOnboardingCompleted = false
                        Log.d(TAG, "本地数据已全部清除，将重新进入引导页面")
                    } catch (e: Exception) {
                        Log.e(TAG, "清除本地数据失败", e)
                    }
                }
            },
            // 删除用餐记录相关回调
            onSessionLongPress = { session ->
                homeViewModel?.showDeleteConfirmDialog(session)
            },
            onDeleteConfirm = {
                homeUiState.sessionToDelete?.let { session ->
                    homeViewModel?.deleteMealSession(session)
                }
            },
            onDeleteDismiss = {
                homeViewModel?.hideDeleteConfirmDialog()
            },
            getLatestFoodData = {
                homeViewModel?.getLatestFoodData()
            },
            // 食物详情编辑相关回调
            onLoadFoodDetail = { sessionId ->
                foodDetailViewModel?.loadMealSnapshot(sessionId)
            },
            onFoodClick = { foodId ->
                foodDetailViewModel?.startEditing(foodId)
            },
            onSaveEdit = { updates ->
                foodDetailViewModel?.updateEditingFood(updates)
                foodDetailViewModel?.saveChanges()
            },
            onCancelEdit = {
                foodDetailViewModel?.cancelEdit()
            },
            onDeleteFood = {
                foodDetailUiState.editingFoodId?.let { foodId ->
                    foodDetailViewModel?.deleteFood(foodId)
                }
            },
            onDownloadPhoto = {
                foodDetailViewModel?.downloadPhoto()
            },
            onTriggerSync = {
                foodDetailViewModel?.triggerSync()
            },
            onAddFood = { name, weight ->
                foodDetailViewModel?.addFood(name, weight)
            },
            onClearSaveSuccess = {
                foodDetailViewModel?.clearSaveSuccess()
            },
            onClearDownloadSuccess = {
                foodDetailViewModel?.clearDownloadSuccess()
            },
            // 用餐结束建议弹窗回调
            onMealEndDialogDismiss = {
                homeViewModel?.dismissMealEndDialog()
            },
            // 忘记设备并重新扫描
            onForgetAndRescan = {
                Log.d(TAG, "忘记设备并重新扫描")
                homeViewModel?.forgetAndRescan()
            },
            // 体重追踪
            weightEntries = weightEntries,
            latestWeightEntry = latestWeightEntry,
            onAddWeight = { weight, note ->
                lifecycleScope.launch {
                    val entry = com.rokid.nutrition.phone.data.entity.WeightEntryEntity(
                        id = java.util.UUID.randomUUID().toString(),
                        weight = weight,
                        note = note,
                        recordedAt = System.currentTimeMillis(),
                        createdAt = System.currentTimeMillis()
                    )
                    database.weightEntryDao().insert(entry)
                    Log.d(TAG, "体重记录已添加: $weight kg")
                }
            },
            onUpdateWeight = { id, weight, note ->
                lifecycleScope.launch {
                    val existingEntry = weightEntries.find { it.id == id }
                    if (existingEntry != null) {
                        val updatedEntry = existingEntry.copy(weight = weight, note = note)
                        database.weightEntryDao().insert(updatedEntry)
                        Log.d(TAG, "体重记录已更新: $weight kg")
                    }
                }
            },
            onDeleteWeight = { id ->
                lifecycleScope.launch {
                    database.weightEntryDao().delete(id)
                    Log.d(TAG, "体重记录已删除: $id")
                }
            }
        )
    }
    
    private fun requestPermissionsIfNeeded() {
        val neededPermissions = Config.BLUETOOTH_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (neededPermissions.isNotEmpty()) {
            permissionLauncher.launch(neededPermissions.toTypedArray())
        } else {
            startBluetoothService()
        }
    }
    
    private fun startBluetoothService() {
        val intent = Intent(this, GlassesConnectionService::class.java)
        startForegroundService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    
    private fun initViewModels() {
        val btManager = bluetoothManager ?: return
        val database = NutritionPhoneApp.instance.database
        val networkManager = NetworkManager.getInstance()
        val userManager = com.rokid.nutrition.phone.repository.UserManager.getInstance(this)
        
        val sessionRepo = MealSessionRepository(
            database.mealSessionDao(),
            database.mealSnapshotDao(),
            database.snapshotFoodDao(),
            networkManager
        )
        val userProfileRepo = UserProfileRepository(database.userProfileDao())
        val statsRepo = StatisticsRepository(
            database.mealSessionDao(),
            database.mealSnapshotDao(),
            database.snapshotFoodDao()
        )
        val mealSessionManager = MealSessionManager(this)
        val dailyNutritionTracker = com.rokid.nutrition.phone.repository.DailyNutritionTracker(this)
        
        // 初始化个性化建议 Repository
        personalizedTipsRepo = PersonalizedTipsRepository(this, networkManager.apiService)
        
        // 用餐结束后刷新个性化建议的回调
        val onMealEnded: (String) -> Unit = { sessionId ->
            lifecycleScope.launch {
                val userId = userManager.getUserId()
                if (userId != null) {
                    Log.d(TAG, "用餐结束，刷新个性化建议: sessionId=$sessionId")
                    val result = personalizedTipsRepo?.refreshAfterMeal(userId, sessionId)
                    val tips = result?.getOrNull()
                    val topTip = tips?.minByOrNull { it.priority } ?: personalizedTipsRepo?.getTopPriorityTip()
                    if (topTip != null) {
                        bluetoothManager?.syncPersonalizedTip(topTip.content, topTip.category)
                    } else {
                        Log.d(TAG, "无可同步的个性化建议")
                    }
                }
            }
        }
        
        homeViewModel = HomeViewModel(btManager, networkManager, sessionRepo, userProfileRepo, mealSessionManager, userManager, dailyNutritionTracker, onMealEnded)
        statsViewModel = StatsViewModel(statsRepo, dailyNutritionTracker)
        
        // 初始化 FoodDetailViewModel
        val mealEditRepo = MealEditRepository(
            database.mealSnapshotDao(),
            database.snapshotFoodDao(),
            database.syncQueueDao(),
            database.mealSessionDao()  // 传入 sessionDao 以便更新会话热量
        )
        val photoStorageRepo = PhotoStorageRepository(this, database.mealSnapshotDao())
        val syncManager = SyncManager(
            context = this,
            syncQueueDao = database.syncQueueDao(),
            snapshotDao = database.mealSnapshotDao(),
            foodDao = database.snapshotFoodDao(),
            apiService = networkManager.apiService
        )
        foodDetailViewModel = FoodDetailViewModel(mealEditRepo, photoStorageRepo, syncManager, dailyNutritionTracker, networkManager)
        
        // 监听食物数据变化事件，刷新统计数据和首页最新识别结果
        lifecycleScope.launch {
            foodDetailViewModel?.dataChangeEvent?.collect { event ->
                Log.d(TAG, "收到食物数据变化事件: $event")
                // 刷新统计数据
                statsViewModel?.refreshData()
                
                // 更新首页最新识别结果
                when (event) {
                    is com.rokid.nutrition.phone.ui.viewmodel.FoodDataChangeEvent.FoodUpdated -> {
                        homeViewModel?.updateLatestResultNutrition(
                            caloriesDiff = event.newCalories - event.oldCalories,
                            proteinDiff = event.newProtein - event.oldProtein,
                            carbsDiff = event.newCarbs - event.oldCarbs,
                            fatDiff = event.newFat - event.oldFat
                        )
                    }
                    is com.rokid.nutrition.phone.ui.viewmodel.FoodDataChangeEvent.FoodDeleted -> {
                        homeViewModel?.subtractFromLatestResult(
                            calories = event.calories,
                            protein = event.protein,
                            carbs = event.carbs,
                            fat = event.fat
                        )
                    }
                    is com.rokid.nutrition.phone.ui.viewmodel.FoodDataChangeEvent.FoodAdded -> {
                        homeViewModel?.updateLatestResultNutrition(
                            caloriesDiff = event.calories,
                            proteinDiff = event.protein,
                            carbsDiff = event.carbs,
                            fatDiff = event.fat
                        )
                    }
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        screenTimeoutHandler.removeCallbacks(screenTimeoutRunnable)
        unbindService(serviceConnection)
    }
    
    override fun onResume() {
        super.onResume()
        // 恢复屏幕亮度并重置计时器
        resetScreenTimeout()
    }
    
    override fun onPause() {
        super.onPause()
        // 暂停时取消计时器
        screenTimeoutHandler.removeCallbacks(screenTimeoutRunnable)
    }
    
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        // 任何触摸事件都重置熄屏计时器
        resetScreenTimeout()
        return super.dispatchTouchEvent(ev)
    }
    
    /**
     * 重置熄屏计时器
     */
    private fun resetScreenTimeout() {
        // 如果屏幕已变暗，先恢复亮度
        if (isScreenDimmed) {
            setScreenBrightness(-1f)  // 恢复系统默认亮度
            isScreenDimmed = false
        }
        
        // 重置计时器
        screenTimeoutHandler.removeCallbacks(screenTimeoutRunnable)
        screenTimeoutHandler.postDelayed(screenTimeoutRunnable, SCREEN_TIMEOUT_MS)
    }
    
    /**
     * 设置屏幕亮度
     * @param brightness 亮度值 (0.0-1.0)，-1 表示使用系统默认
     */
    private fun setScreenBrightness(brightness: Float) {
        val layoutParams = window.attributes
        layoutParams.screenBrightness = brightness
        window.attributes = layoutParams
        isScreenDimmed = brightness > 0 && brightness < 0.1f
        Log.d(TAG, "屏幕亮度设置为: $brightness, isScreenDimmed=$isScreenDimmed")
    }
}
