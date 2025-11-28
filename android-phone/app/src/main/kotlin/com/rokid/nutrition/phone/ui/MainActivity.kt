package com.rokid.nutrition.phone.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
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
import kotlinx.coroutines.launch

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {
    
    private var bluetoothManager: BluetoothManager? = null
    private var homeViewModel: HomeViewModel? = null
    private var statsViewModel: StatsViewModel? = null
    private var foodDetailViewModel: FoodDetailViewModel? = null
    
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
        
        // 获取今日用餐次数
        val dailyNutritionTracker = remember { DailyNutritionTracker(this@MainActivity) }
        
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
        
        // 收集识别历史记录
        LaunchedEffect(viewModelReady) {
            if (viewModelReady && homeViewModel != null) {
                homeViewModel?.recognitionHistory?.collectLatest { history ->
                    recognitionHistory = history
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
        
        // 收集会话列表
        val database = NutritionPhoneApp.instance.database
        LaunchedEffect(Unit) {
            database.mealSessionDao().getRecentSessions(20).collectLatest { list ->
                sessions = list
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
        
        // 构建带照片的会话列表
        LaunchedEffect(sessions) {
            Log.d(TAG, "Building sessionsWithPhotos, sessions count: ${sessions.size}")
            sessionsWithPhotos = sessions.map { session ->
                // 尝试获取会话的照片
                val snapshot = database.mealSnapshotDao().getLatestSnapshot(session.sessionId)
                val photoUri = snapshot?.localImagePath ?: snapshot?.imageUrl
                Log.d(TAG, "Session ${session.sessionId}: snapshot=${snapshot != null}, photoUri=$photoUri")
                MealSessionWithPhoto(session = session, photoUri = photoUri)
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
                    // 清除本地数据
                    dailyNutritionTracker.clearAll()
                    database.clearAllTables()
                    Log.d(TAG, "本地数据已清除")
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
        val statsRepo = StatisticsRepository(database.mealSessionDao())
        val mealSessionManager = MealSessionManager(this)
        val dailyNutritionTracker = com.rokid.nutrition.phone.repository.DailyNutritionTracker(this)
        
        homeViewModel = HomeViewModel(btManager, networkManager, sessionRepo, userProfileRepo, mealSessionManager, userManager, dailyNutritionTracker)
        statsViewModel = StatsViewModel(statsRepo)
        
        // 初始化 FoodDetailViewModel
        val mealEditRepo = MealEditRepository(
            database.mealSnapshotDao(),
            database.snapshotFoodDao(),
            database.syncQueueDao()
        )
        val photoStorageRepo = PhotoStorageRepository(this, database.mealSnapshotDao())
        val syncManager = SyncManager(
            context = this,
            syncQueueDao = database.syncQueueDao(),
            snapshotDao = database.mealSnapshotDao(),
            foodDao = database.snapshotFoodDao(),
            apiService = networkManager.apiService
        )
        foodDetailViewModel = FoodDetailViewModel(mealEditRepo, photoStorageRepo, syncManager, dailyNutritionTracker)
        
        // 监听食物数据变化事件，刷新统计数据
        lifecycleScope.launch {
            foodDetailViewModel?.dataChangeEvent?.collect { event ->
                Log.d(TAG, "收到食物数据变化事件: $event")
                // 刷新统计数据
                statsViewModel?.refreshData()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
    }
}
