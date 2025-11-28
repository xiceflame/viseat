package com.rokid.nutrition.phone.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rokid.nutrition.phone.bluetooth.GlassInfo
import com.rokid.nutrition.phone.data.entity.MealSessionEntity
import com.rokid.nutrition.phone.repository.DailyStats
import com.rokid.nutrition.phone.repository.UserProfile
import com.rokid.nutrition.phone.ui.screen.*
import com.rokid.nutrition.phone.ui.viewmodel.FoodDetailUiState
import com.rokid.nutrition.phone.ui.viewmodel.HomeUiState
import com.rokid.nutrition.phone.ui.viewmodel.RecognitionHistory
import com.rokid.nutrition.phone.util.FoodItemUpdates

/**
 * 导航路由 - 简化为 3 个 Tab
 */
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "眼镜", Icons.Default.Visibility)
    object Stats : Screen("stats", "统计", Icons.Default.BarChart)
    object Profile : Screen("profile", "我的", Icons.Default.Person)
    object DebugLog : Screen("debug_log", "日志", Icons.Default.BugReport)  // 调试日志
    object FoodDetail : Screen("food_detail", "食物详情", Icons.Default.Restaurant)  // 食物详情
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
    onTriggerSync: () -> Unit = {}
) {
    val navController = rememberNavController()
    
    // 保存选中的历史记录用于详情页
    var selectedHistory by remember { mutableStateOf<RecognitionHistory?>(null) }
    // 保存选中的会话用于增强详情页
    var selectedSessionId by remember { mutableStateOf<String?>(null) }
    // 是否使用增强版详情页（有数据库数据时使用）
    var useEnhancedDetail by remember { mutableStateOf(false) }
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    uiState = homeUiState,
                    glassInfo = glassInfo,
                    recentSessions = sessions.take(5),  // 最近5条记录
                    recentSessionsWithPhotos = sessionsWithPhotos.take(5),
                    recognitionHistory = recognitionHistory,
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
                    }
                )
            }
            
            composable(Screen.Stats.route) {
                StatsScreen(
                    todayCalories = todayCalories,
                    weeklyStats = weeklyStats,
                    sessions = sessions,
                    recognitionHistory = recognitionHistory,
                    targetCalories = 2000.0,  // TODO: 从用户档案获取
                    onSessionClick = { session ->
                        // 导航到增强版详情页
                        selectedSessionId = session.sessionId
                        useEnhancedDetail = true
                        navController.navigate(Screen.FoodDetail.route)
                    }
                )
            }
            
            composable(Screen.Profile.route) {
                ProfileScreen(
                    profile = userProfile,
                    todayCalories = todayCalories,
                    mealCount = todayMealCount,
                    onSaveProfile = onSaveProfile,
                    onNavigateToStats = { navController.navigate(Screen.Stats.route) },
                    onOpenDebugLog = { navController.navigate(Screen.DebugLog.route) },
                    onClearData = onClearData
                )
            }
            
            composable(Screen.DebugLog.route) {
                DebugLogScreen(
                    onBack = { navController.popBackStack() }
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
                        onTriggerSync = onTriggerSync
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
