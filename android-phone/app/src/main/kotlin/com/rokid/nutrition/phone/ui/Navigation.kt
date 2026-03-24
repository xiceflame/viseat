package com.rokid.nutrition.phone.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rokid.nutrition.phone.bluetooth.GlassInfo
import com.rokid.nutrition.phone.data.entity.MealSessionEntity
import com.rokid.nutrition.phone.repository.DailyStats
import com.rokid.nutrition.phone.repository.TodayNutritionData
import com.rokid.nutrition.phone.repository.UserProfile
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.rokid.nutrition.phone.NutritionPhoneApp
import com.rokid.nutrition.phone.repository.UserManager
import com.rokid.nutrition.phone.repository.UserProfileRepository
import com.rokid.nutrition.phone.ui.screen.*
import com.rokid.nutrition.phone.ui.theme.*
import com.rokid.nutrition.phone.network.model.PersonalizedTip
import com.rokid.nutrition.phone.ui.viewmodel.FoodDetailUiState
import com.rokid.nutrition.phone.ui.viewmodel.HomeUiState
import com.rokid.nutrition.phone.ui.viewmodel.OnboardingViewModel
import com.rokid.nutrition.phone.ui.viewmodel.RecognitionHistory
import com.rokid.nutrition.phone.util.FoodItemUpdates
import com.rokid.nutrition.phone.data.entity.WeightEntryEntity

/**
 * 导航路由 - 苹果风格简洁设计
 */
sealed class Screen(
    val route: String, 
    val title: String, 
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon
) {
    object Onboarding : Screen(
        route = "onboarding",
        title = "引导",
        icon = Icons.Rounded.Start
    )
    object Home : Screen(
        route = "home", 
        title = "眼镜", 
        icon = Icons.Rounded.Visibility,
        selectedIcon = Icons.Rounded.Visibility
    )
    object Stats : Screen(
        route = "stats", 
        title = "统计", 
        icon = Icons.Rounded.BarChart,
        selectedIcon = Icons.Rounded.BarChart
    )
    object Profile : Screen(
        route = "profile", 
        title = "我的", 
        icon = Icons.Rounded.Person,
        selectedIcon = Icons.Rounded.Person
    )
    object DebugLog : Screen(
        route = "debug_log", 
        title = "日志", 
        icon = Icons.Rounded.BugReport
    )
    object FoodDetail : Screen(
        route = "food_detail", 
        title = "食物详情", 
        icon = Icons.Rounded.Restaurant
    )
    object WeightHistory : Screen(
        route = "weight_history",
        title = "体重记录",
        icon = Icons.Rounded.MonitorWeight
    )
    object Loading : Screen(
        route = "loading",
        title = "加载中",
        icon = Icons.Rounded.Refresh
    )
    object Demo : Screen(
        route = "demo",
        title = "演示模式",
        icon = Icons.Rounded.Science
    )
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Stats,
    Screen.Profile
)

/**
 * 主导航组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation(
    homeUiState: HomeUiState,
    todayCalories: Double,
    todayMealCount: Int = 0,
    weeklyStats: List<DailyStats>,
    sessions: List<MealSessionEntity>,
    sessionsWithPhotos: List<MealSessionWithPhoto> = emptyList(),
    recognitionHistory: List<RecognitionHistory>,
    userProfile: UserProfile?,
    glassInfo: GlassInfo?,
    foodDetailUiState: FoodDetailUiState = FoodDetailUiState(),
    // 今日营养数据（从数据库获取）
    todayNutritionData: TodayNutritionData = TodayNutritionData(),
    // Onboarding 状态
    isOnboardingCompleted: Boolean? = null,
    onOnboardingComplete: () -> Unit = {},
    // 智能健康提示刷新回调
    onRefreshTips: () -> Unit = {},
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onTakePhotoClick: () -> Unit,
    onSaveProfile: (UserProfile) -> Unit,
    onSessionClick: (MealSessionEntity) -> Unit,
    onDeviceSelected: (String) -> Unit = {},
    onCancelDeviceSelection: () -> Unit = {},
    onSetBrightness: (Int) -> Unit = {},
    onSetVolume: (Int) -> Unit = {},
    onSyncTime: () -> Unit = {},
    onReboot: () -> Unit = {},
    onShutdown: () -> Unit = {},
    onEndMealSession: () -> Unit = {},
    onStartMealSession: () -> Unit = {},
    onClearData: () -> Unit = {},
    // 删除用餐记录相关回调
    onSessionLongPress: (MealSessionEntity) -> Unit = {},
    onDeleteConfirm: () -> Unit = {},
    onDeleteDismiss: () -> Unit = {},
    getLatestFoodData: () -> com.rokid.nutrition.phone.network.model.VisionAnalyzeResponse? = { null },
    // 食物详情编辑相关回调
    onLoadFoodDetail: (String) -> Unit = {},
    onFoodClick: (String) -> Unit = {},
    onSaveEdit: (FoodItemUpdates) -> Unit = {},
    onCancelEdit: () -> Unit = {},
    onDeleteFood: () -> Unit = {},
    onDownloadPhoto: () -> Unit = {},
    onTriggerSync: () -> Unit = {},
    onAddFood: (String, Double) -> Unit = { _, _ -> },
    onClearSaveSuccess: () -> Unit = {},
    onClearDownloadSuccess: () -> Unit = {},
    // 用餐结束建议弹窗回调
    onMealEndDialogDismiss: () -> Unit = {},
    // 忘记设备并重新扫描
    onForgetAndRescan: () -> Unit = {},
    // 体重追踪相关
    weightEntries: List<WeightEntryEntity> = emptyList(),
    latestWeightEntry: WeightEntryEntity? = null,
    onAddWeight: (Float, String?) -> Unit = { _, _ -> },
    onUpdateWeight: (String, Float, String?) -> Unit = { _, _, _ -> },
    onDeleteWeight: (String) -> Unit = {}
) {
    val navController = rememberNavController()
    
    // 保存选中的历史记录用于详情页
    var selectedHistory by remember { mutableStateOf<RecognitionHistory?>(null) }
    // 保存选中的会话用于增强详情页
    var selectedSessionId by remember { mutableStateOf<String?>(null) }
    // 是否使用增强版详情页（有数据库数据时使用）
    var useEnhancedDetail by remember { mutableStateOf(false) }
    
    // 状态确定前，显示全屏加载界面，避免 NavHost 状态切换问题
    if (isOnboardingCompleted == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = AppleTeal)
        }
        return
    }

    // 根据 onboarding 状态决定起始路由
    val startDestination = if (isOnboardingCompleted) Screen.Home.route else Screen.Onboarding.route
    
    // 监听状态变化，只处理从 false 到 true 的自动转换（如后台同步发现已完成）
    LaunchedEffect(isOnboardingCompleted) {
        if (isOnboardingCompleted == true) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentRoute == Screen.Onboarding.route || currentRoute == Screen.Loading.route) {
                navController.navigate(Screen.Home.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }
    
    Scaffold(
        bottomBar = {
            // 苹果风格的底部导航栏
            val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
            // 只在主功能页面显示底部导航
            if (currentRoute in listOf(Screen.Home.route, Screen.Stats.route, Screen.Profile.route)) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 0.dp
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        
                        NavigationBarItem(
                            icon = { 
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.icon,
                                    contentDescription = screen.title,
                                    modifier = Modifier.size(24.dp)
                                ) 
                            },
                            label = { 
                                Text(
                                    text = screen.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                ) 
                            },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AppleTeal,
                                selectedTextColor = AppleTeal,
                                unselectedIconColor = AppleGray2,
                                unselectedTextColor = AppleGray2,
                                indicatorColor = Color.Transparent  // 移除绿色色块背景
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Loading 页
            composable(Screen.Loading.route) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppleTeal)
                }
            }

            // Onboarding 引导页
            composable(Screen.Onboarding.route) {
                val context = LocalContext.current
                val database = NutritionPhoneApp.instance.database
                val userProfileRepo = remember { UserProfileRepository(database.userProfileDao()) }
                val userManager = remember { UserManager.getInstance(context) }
                val onboardingViewModel: OnboardingViewModel = viewModel {
                    OnboardingViewModel(userProfileRepo, userManager)
                }
                val onboardingState by onboardingViewModel.state.collectAsState()
                
                OnboardingScreen(
                    state = onboardingState,
                    onStartOnboarding = { onboardingViewModel.startOnboarding() },
                    onNextStep = { onboardingViewModel.nextStep() },
                    onPreviousStep = { onboardingViewModel.previousStep() },
                    onSetNickname = { onboardingViewModel.setNickname(it) },
                    onSetHealthGoal = { onboardingViewModel.setHealthGoal(it) },
                    onSetGender = { onboardingViewModel.setGender(it) },
                    onSetBirthDate = { y, m, d -> onboardingViewModel.setBirthDate(y, m, d) },
                    onSetHeight = { onboardingViewModel.setHeight(it) },
                    onSetWeight = { onboardingViewModel.setWeight(it) },
                    onSetTargetWeight = { onboardingViewModel.setTargetWeight(it) },
                    onSetTargetDate = { onboardingViewModel.setTargetDate(it) },
                    onSetActivityLevel = { onboardingViewModel.setActivityLevel(it) },
                    onSetDietType = { onboardingViewModel.setDietType(it) },
                    onToggleAllergen = { onboardingViewModel.toggleAllergen(it) },
                    onClearAllergens = { onboardingViewModel.clearAllergens() },
                    onComplete = {
                        onboardingViewModel.completeOnboarding {
                            // 引导完成，导航到主页
                            onOnboardingComplete()
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        }
                    },
                    onSkip = {
                        // 跳过引导，保存默认档案并进入主页
                        onboardingViewModel.skipOnboarding {
                            onOnboardingComplete()
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        }
                    }
                )
            }
            
            composable(Screen.Home.route) {
                // 使用与 StatsScreen 相同的热量计算逻辑
                val effectiveCalories = if (todayNutritionData.totalCalories > 0) {
                    todayNutritionData.totalCalories
                } else {
                    todayCalories
                }
                
                HomeScreen(
                    uiState = homeUiState,
                    glassInfo = glassInfo,
                    recentSessions = sessions.take(5),  // 最近5条记录
                    recentSessionsWithPhotos = sessionsWithPhotos.take(5),
                    recognitionHistory = recognitionHistory,
                    // 智能健康提示（从 uiState 获取，这里可以覆盖）
                    personalizedTips = homeUiState.smartTips,
                    isLoadingTips = homeUiState.isLoadingTips,
                    onConnectClick = onConnectClick,
                    onDisconnectClick = onDisconnectClick,
                    onTakePhotoClick = onTakePhotoClick,
                    onDeviceSelected = onDeviceSelected,
                    onCancelDeviceSelection = onCancelDeviceSelection,
                    onSessionClick = { session ->
                        // 导航到增强版详情页
                        selectedSessionId = session.sessionId
                        useEnhancedDetail = true
                        navController.navigate(Screen.FoodDetail.route)
                    },
                    onSessionLongPress = onSessionLongPress,
                    onLatestResultClick = {
                        // 如果有 latestSessionId，使用增强版详情页（支持编辑）
                        if (homeUiState.latestSessionId != null) {
                            selectedSessionId = homeUiState.latestSessionId
                            useEnhancedDetail = true
                        } else {
                            // 否则使用基础版详情页（仅显示）
                            selectedHistory = null
                            useEnhancedDetail = false
                        }
                        navController.navigate(Screen.FoodDetail.route)
                    },
                    onEndMealSession = onEndMealSession,
                    onStartMealSession = onStartMealSession,
                    onDeleteConfirm = onDeleteConfirm,
                    onDeleteDismiss = onDeleteDismiss,
                    onHistoryClick = { history ->
                        selectedHistory = history
                        useEnhancedDetail = false
                        navController.navigate(Screen.FoodDetail.route)
                    },
                    onMealEndDialogDismiss = onMealEndDialogDismiss,
                    onRefreshTips = onRefreshTips,
                    onForgetAndRescan = onForgetAndRescan
                )
            }
            
            composable(Screen.Stats.route) {
                // 使用数据库中的营养数据，如果没有则使用 todayCalories
                val effectiveCalories = if (todayNutritionData.totalCalories > 0) {
                    todayNutritionData.totalCalories
                } else {
                    todayCalories
                }
                
                StatsScreen(
                    todayCalories = effectiveCalories,
                    weeklyStats = weeklyStats,
                    sessions = sessions,
                    recognitionHistory = recognitionHistory,
                    targetCalories = userProfile?.calculateDailyCalories()?.toDouble() ?: 2000.0,
                    // 从数据库获取的营养数据
                    todayProtein = todayNutritionData.totalProtein,
                    todayCarbs = todayNutritionData.totalCarbs,
                    todayFat = todayNutritionData.totalFat,
                    mealCount = todayNutritionData.mealCount,
                    snackCount = todayNutritionData.snackCount,
                    beverageCount = todayNutritionData.beverageCount,
                    dessertCount = todayNutritionData.dessertCount,
                    fruitCount = todayNutritionData.fruitCount,
                    onSessionClick = { session ->
                        // 导航到增强版详情页
                        selectedSessionId = session.sessionId
                        useEnhancedDetail = true
                        navController.navigate(Screen.FoodDetail.route)
                    }
                )
            }
            
            composable(Screen.Profile.route) {
                // 使用与 StatsScreen 相同的热量计算逻辑
                val effectiveCalories = if (todayNutritionData.totalCalories > 0) {
                    todayNutritionData.totalCalories
                } else {
                    todayCalories
                }
                
                ProfileScreen(
                    profile = userProfile,
                    todayCalories = effectiveCalories,
                    mealCount = todayMealCount,
                    onSaveProfile = onSaveProfile,
                    onNavigateToStats = { navController.navigate(Screen.Stats.route) },
                    onOpenDebugLog = { navController.navigate(Screen.DebugLog.route) },
                    onNavigateToDemoMode = { navController.navigate(Screen.Demo.route) },
                    onClearData = onClearData,
                    // 体重追踪
                    currentWeight = latestWeightEntry?.weight,
                    latestWeightEntry = latestWeightEntry,
                    onAddWeight = onAddWeight,
                    onViewWeightHistory = { navController.navigate(Screen.WeightHistory.route) }
                )
            }
            
            composable(Screen.DebugLog.route) {
                DebugLogScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            
            // 体重历史页面
            composable(Screen.WeightHistory.route) {
                WeightHistoryScreen(
                    entries = weightEntries,
                    targetWeight = userProfile?.targetWeight,
                    startWeight = userProfile?.weight,
                    onAddWeight = onAddWeight,
                    onUpdateWeight = onUpdateWeight,
                    onDeleteWeight = onDeleteWeight,
                    onBack = { navController.popBackStack() }
                )
            }
            
            // 演示模式页面
            composable(Screen.Demo.route) {
                val database = NutritionPhoneApp.instance.database
                val demoDataRepository = remember { 
                    com.rokid.nutrition.phone.demo.DemoDataRepository(NutritionPhoneApp.instance) 
                }
                val mealSessionRepository = remember {
                    com.rokid.nutrition.phone.repository.MealSessionRepository(
                        database.mealSessionDao(),
                        database.mealSnapshotDao(),
                        database.snapshotFoodDao(),
                        com.rokid.nutrition.phone.network.NetworkManager.getInstance()
                    )
                }
                val demoViewModel: com.rokid.nutrition.phone.demo.DemoViewModel = viewModel {
                    com.rokid.nutrition.phone.demo.DemoViewModel(demoDataRepository, mealSessionRepository)
                }
                val demoUiState by demoViewModel.uiState.collectAsState()
                
                DemoScreen(
                    uiState = demoUiState,
                    onSelectMode = { mode -> 
                        demoViewModel.selectMode(mode)
                    },
                    onSelectFood = { foodType -> 
                        demoViewModel.selectFood(foodType)
                    },
                    onStartRecognition = { 
                        demoViewModel.startSingleImageRecognition()
                    },
                    onStartMeal = { 
                        demoViewModel.startMealMonitoring()
                    },
                    onTriggerCapture = { 
                        demoViewModel.triggerMiddleCapture()
                    },
                    onEndMeal = { 
                        demoViewModel.endMealMonitoring()
                    },
                    onBack = {
                        if (demoUiState.currentMode == com.rokid.nutrition.phone.demo.DemoMode.SELECTION) {
                            navController.popBackStack()
                        } else {
                            demoViewModel.backToSelection()
                        }
                    },
                    onClearError = { 
                        demoViewModel.clearError()
                    }
                )
            }
            
            composable(Screen.FoodDetail.route) {
                if (useEnhancedDetail && selectedSessionId != null) {
                    // 使用增强版详情页（支持照片和编辑）
                    LaunchedEffect(selectedSessionId) {
                        selectedSessionId?.let { onLoadFoodDetail(it) }
                    }
                    
                    EnhancedFoodDetailScreen(
                        uiState = foodDetailUiState,
                        onBackClick = { 
                            useEnhancedDetail = false
                            selectedSessionId = null
                            navController.popBackStack() 
                        },
                        onFoodClick = onFoodClick,
                        onSaveEdit = onSaveEdit,
                        onCancelEdit = onCancelEdit,
                        onDeleteFood = onDeleteFood,
                        onDownloadPhoto = onDownloadPhoto,
                        onTriggerSync = onTriggerSync,
                        onAddFood = onAddFood,
                        onClearSaveSuccess = onClearSaveSuccess,
                        onClearDownloadSuccess = onClearDownloadSuccess
                    )
                } else {
                    // 使用基础版详情页（仅显示）
                    val foodData = selectedHistory?.foodData ?: getLatestFoodData()
                    FoodDetailScreen(
                        foodData = foodData,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
