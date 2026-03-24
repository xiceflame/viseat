package com.rokid.nutrition.phone.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.rokid.nutrition.phone.R
import coil.request.ImageRequest
import com.rokid.nutrition.phone.bluetooth.ConnectionState
import com.rokid.nutrition.phone.bluetooth.GlassInfo
import com.rokid.nutrition.phone.data.entity.MealSessionEntity
import com.rokid.nutrition.phone.ui.component.DeleteMealConfirmDialog
import com.rokid.nutrition.phone.ui.component.MealEndAdviceDialog
import com.rokid.nutrition.phone.ui.component.SmartHealthTipsCard
import com.rokid.nutrition.phone.ui.theme.*
import com.rokid.nutrition.phone.ui.viewmodel.HomeUiState
import com.rokid.nutrition.phone.ui.viewmodel.RecognitionHistory
import com.rokid.nutrition.phone.ui.viewmodel.ScannedDevice
import java.text.SimpleDateFormat
import java.util.*

// 从 HistoryScreen 导入 MealSessionWithPhoto
// 注意：MealSessionWithPhoto 定义在 HistoryScreen.kt 中

/**
 * 首页 - 眼镜连接与最近用餐记录
 */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    glassInfo: GlassInfo?,
    recentSessions: List<MealSessionEntity>,
    recentSessionsWithPhotos: List<MealSessionWithPhoto> = emptyList(),
    recognitionHistory: List<RecognitionHistory>,
    // 智能健康提示相关参数（从 uiState 获取）
    personalizedTips: List<com.rokid.nutrition.phone.network.model.PersonalizedTip> = uiState.smartTips,
    isLoadingTips: Boolean = uiState.isLoadingTips,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onTakePhotoClick: () -> Unit,
    onDeviceSelected: (String) -> Unit,
    onCancelDeviceSelection: () -> Unit,
    onSessionClick: (MealSessionEntity) -> Unit,
    onSessionLongPress: (MealSessionEntity) -> Unit = {},
    onLatestResultClick: () -> Unit,
    onEndMealSession: () -> Unit,
    onStartMealSession: () -> Unit,
    onDeleteConfirm: () -> Unit = {},
    onDeleteDismiss: () -> Unit = {},
    onHistoryClick: (RecognitionHistory) -> Unit = {},
    onMealEndDialogDismiss: () -> Unit = {},  // 关闭用餐结束建议弹窗
    onRefreshTips: () -> Unit = {},  // 刷新智能健康提示
    onForgetAndRescan: () -> Unit = {},  // 忘记设备并重新扫描
    modifier: Modifier = Modifier
) {
    val isConnected = uiState.connectionState is ConnectionState.Connected
    val isDark = isSystemInDarkTheme()
    
    // 高端背景 - 更柔和的渐变
    val backgroundGradient = PremiumGradients.pageBackground()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(brush = backgroundGradient)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // 眼镜状态卡片
        GlassesStatusCard(
            connectionState = uiState.connectionState,
            glassInfo = glassInfo,
            glassesName = uiState.glassesName,
            onForgetAndRescan = onForgetAndRescan,
            batteryLevel = uiState.batteryLevel,
            onConnectClick = onConnectClick,
            onDisconnectClick = onDisconnectClick
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 状态消息
        if (uiState.statusMessage.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    text = uiState.statusMessage,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // 用餐状态卡片（移到最新识别之前，替代快捷操作）
        MealStatusCard(
            hasActiveSession = uiState.hasActiveSession,
            sessionStatus = uiState.sessionStatus,
            sessionStartTime = uiState.sessionStartTime,
            totalCalories = uiState.totalCalories,
            isConnected = isConnected,
            isProcessing = uiState.isProcessing,
            onEndMealSession = onEndMealSession,
            onStartMealSession = onStartMealSession
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        // 智能健康提示卡片（自动轮播）
        SmartHealthTipsCard(
            tips = personalizedTips,
            isLoading = isLoadingTips,
            onRefresh = onRefreshTips
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        // 最新识别结果（带缩略图）
        if (uiState.latestResult != null) {
            LatestResultCardWithPhoto(
                foodName = uiState.latestResult.foodName,
                calories = uiState.latestResult.calories,
                protein = uiState.latestResult.protein,
                carbs = uiState.latestResult.carbs,
                fat = uiState.latestResult.fat,
                suggestion = uiState.latestResult.suggestion,
                imageUrl = uiState.latestImageUrl,
                onClick = onLatestResultClick
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // 最近用餐记录（带照片缩略图）
        RecentMealsCardWithPhotos(
            sessionsWithPhotos = recentSessionsWithPhotos,
            onSessionClick = onSessionClick,
            onSessionLongPress = onSessionLongPress
        )
    }
    
    // 设备选择对话框
    if (uiState.showDeviceSelector) {
        DeviceSelectorDialog(
            devices = uiState.scannedDevices,
            isScanning = uiState.connectionState is ConnectionState.Scanning,
            onDeviceSelected = onDeviceSelected,
            onDismiss = onCancelDeviceSelection
        )
    }
    
    // 删除确认对话框
    if (uiState.showDeleteDialog && uiState.sessionToDelete != null) {
        DeleteMealConfirmDialog(
            session = uiState.sessionToDelete,
            onConfirm = onDeleteConfirm,
            onDismiss = onDeleteDismiss
        )
    }
    
    // 用餐结束建议弹窗
    if (uiState.showMealEndDialog) {
        MealEndAdviceDialog(
            mealSummary = uiState.mealSummary,
            advice = uiState.mealAdvice,
            nextMealSuggestion = uiState.nextMealSuggestion,
            onDismiss = onMealEndDialogDismiss
        )
    }
}


/**
 * Rokid 风格的眼镜连接区域 - 模糊渐变背景 + 眼镜图片
 */
@Composable
private fun GlassesStatusCard(
    connectionState: ConnectionState,
    glassInfo: GlassInfo?,
    glassesName: String?,
    batteryLevel: Int,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onForgetAndRescan: (() -> Unit)? = null
) {
    val isConnected = connectionState is ConnectionState.Connected
    val isConnecting = connectionState is ConnectionState.Connecting || 
                       connectionState is ConnectionState.Scanning
    
    // 连接状态指示灯动画
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (isConnected) 1f else 0.3f,
        animationSpec = tween(500),
        label = "indicatorAlpha"
    )
    
    // 呼吸灯效果（连接中时）
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingAlpha"
    )
    
    var showHelpDialog by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        // 顶部提示栏 - 如何连接眼镜
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showHelpDialog = true },
            color = AppleBlue.copy(alpha = 0.1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Info,
                    contentDescription = null,
                    tint = AppleBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isConnected) "已连接" else "未连接",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isConnected) AppleTeal else AppleOrange
                )
                Text(
                    text = "，如何连接眼镜",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = AppleGray2,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 眼镜展示区域 - 模糊渐变背景
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isConnected) {
                            listOf(
                                AppleTeal.copy(alpha = 0.15f),
                                AppleMint.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        } else {
                            listOf(
                                AppleGray5,
                                AppleGray6.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        }
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 眼镜图标 - 大尺寸（使用真实眼镜图片）
                Box(contentAlignment = Alignment.Center) {
                    // Rokid 眼镜图片
                    Image(
                        painter = painterResource(id = R.drawable.ic_glasses_rokid),
                        contentDescription = "Rokid 眼镜",
                        modifier = Modifier
                            .size(160.dp)
                            .graphicsLayer {
                                alpha = if (isConnected) 1f else 0.6f
                            },
                        contentScale = ContentScale.Fit
                    )
                    
                    // 连接状态指示灯 - 右上角
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-10).dp, y = 25.dp)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                color = when {
                                    isConnected -> AppleTeal.copy(alpha = indicatorAlpha)
                                    isConnecting -> AppleOrange.copy(alpha = breathingAlpha)
                                    else -> AppleGray3.copy(alpha = 0.5f)
                                }
                            )
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 设备名称
                Text(
                    text = when {
                        isConnected -> glassesName ?: glassInfo?.name ?: "Glasses"
                        isConnecting -> "搜索中..."
                        else -> "未连接"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // 电池状态（已连接时显示）
                if (isConnected) {
                    Spacer(modifier = Modifier.height(8.dp))
                    // 优先使用实时更新的 batteryLevel，其次使用 glassInfo 中的值
                    val displayBattery = when {
                        batteryLevel > 0 -> batteryLevel
                        glassInfo != null && glassInfo.batteryLevel > 0 -> glassInfo.batteryLevel
                        else -> 0  // 无数据时显示0
                    }
                    val isCharging = glassInfo?.isCharging == true
                    val batteryColor = when {
                        isCharging -> AppleTeal
                        displayBattery > 50 -> AppleTeal
                        displayBattery > 20 -> AppleOrange
                        displayBattery > 0 -> AppleRed
                        else -> AppleGray2  // 无数据时灰色
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryFull,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = batteryColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                isCharging && displayBattery > 0 -> "$displayBattery% 充电中"
                                isCharging -> "充电中"
                                displayBattery > 0 -> "$displayBattery%"
                                else -> "获取中..."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppleGray1
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 连接/断开按钮
        if (isConnected) {
            OutlinedButton(
                onClick = onDisconnectClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AppleGray1
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.linearGradient(listOf(AppleGray4, AppleGray4))
                )
            ) {
                Text(
                    "断开连接",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        } else {
            Button(
                onClick = onConnectClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isConnecting,
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppleTeal,
                    disabledContainerColor = AppleTeal.copy(alpha = 0.5f)
                )
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "搜索中...",
                        style = MaterialTheme.typography.titleMedium
                    )
                } else {
                    Text(
                        "去连接",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
        
        // 错误提示
        if (connectionState is ConnectionState.Error) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = connectionState.message,
                style = MaterialTheme.typography.bodySmall,
                color = AppleRed,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
    
    // 帮助对话框
    if (showHelpDialog) {
        ConnectionHelpDialog(
            onDismiss = { showHelpDialog = false },
            onForgetAndRescan = onForgetAndRescan
        )
    }
}

/**
 * 连接帮助对话框
 */
@Composable
private fun ConnectionHelpDialog(
    onDismiss: () -> Unit,
    onForgetAndRescan: (() -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "如何连接眼镜",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "连接失败，可能有以下情况",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppleGray1
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 步骤 1
                HelpStep(
                    number = "1",
                    title = "可能关机了",
                    description = "戴上眼镜拍照，有拍照音效则表示没有关机",
                    hint = "如果关机，请长按功能键3秒开机"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 步骤 2
                HelpStep(
                    number = "2",
                    title = "可能没电了",
                    description = "请充电后再尝试连接"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 步骤 3
                HelpStep(
                    number = "3",
                    title = "若始终无法连接",
                    description = "长按功能键12秒强制重启"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 步骤 4 - 忘记设备重新配对
                HelpStep(
                    number = "4",
                    title = "忘记设备重新配对",
                    description = "如果之前连接过但现在无法连接，可以尝试忘记设备后重新配对"
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 忘记设备按钮（如果提供了回调）
                if (onForgetAndRescan != null) {
                    OutlinedButton(
                        onClick = {
                            onForgetAndRescan()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AppleOrange
                        )
                    ) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("忘记设备并重新扫描", style = MaterialTheme.typography.titleMedium)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                // 关闭按钮
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppleTeal)
                ) {
                    Text("我知道了", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun HelpStep(
    number: String,
    title: String,
    description: String,
    hint: String? = null
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // 序号
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(AppleBlue),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = AppleGray1
            )
            if (hint != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppleBlue
                )
            }
        }
    }
}

@Composable
private fun QuickActionsCard(onTakePhotoClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "快捷操作",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onTakePhotoClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("拍照识别")
            }
        }
    }
}


@Composable
private fun LatestResultCard(
    foodName: String,
    calories: Double,
    protein: Double,
    carbs: Double,
    fat: Double,
    suggestion: String,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "最新识别", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = "刚刚", fontSize = 12.sp, color = Color.Gray)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(text = foodName, fontWeight = FontWeight.Medium, fontSize = 20.sp)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NutrientItem("热量", "${calories.toInt()}", "kcal", Color(0xFFFF6B6B))
                NutrientItem("蛋白质", String.format("%.1f", protein), "g", Color(0xFF4ECDC4))
                NutrientItem("碳水", String.format("%.1f", carbs), "g", Color(0xFFFFAB40))  // 深橙黄色，更容易看清
                NutrientItem("脂肪", String.format("%.1f", fat), "g", Color(0xFF95E1D3))
            }
            
            if (suggestion.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = suggestion, fontSize = 14.sp, color = Color(0xFF4CAF50))
                    }
                }
            }
            
            // 点击查看详情提示
            if (onClick != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "点击查看详情",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * 苹果风格的最新识别结果卡片
 */
@Composable
private fun LatestResultCardWithPhoto(
    foodName: String,
    calories: Double,
    protein: Double,
    carbs: Double,
    fat: Double,
    suggestion: String,
    imageUrl: String?,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            )
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AppleTeal)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "最新识别", 
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AppleGray6
                ) {
                    Text(
                        text = "刚刚",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppleGray1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 照片和食物信息并排显示
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // 缩略图 - 苹果风格圆角
                if (imageUrl != null) {
                    Card(
                        modifier = Modifier
                            .size(88.dp),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "食物照片",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }
                
                // 食物名称和热量
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = foodName, 
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${calories.toInt()}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = CalorieRed
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "kcal",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CalorieRed.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 营养素 - 苹果风格卡片
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AppleGray6
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    NutrientItem("蛋白质", String.format("%.1f", protein), "g", ProteinCyan)
                    NutrientItem("碳水", String.format("%.1f", carbs), "g", CarbsAmber)
                    NutrientItem("脂肪", String.format("%.1f", fat), "g", FatGreen)
                }
            }
            
            // 建议
            if (suggestion.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AppleTeal.copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Lightbulb,
                            contentDescription = null,
                            tint = AppleTeal,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = suggestion, 
                            style = MaterialTheme.typography.bodySmall,
                            color = AppleTeal
                        )
                    }
                }
            }
            
            // 点击编辑提示
            if (onClick != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "点击编辑",
                        style = MaterialTheme.typography.labelMedium,
                        color = AppleBlue
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = AppleBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * 苹果风格的营养素项
 */
@Composable
private fun NutrientItem(label: String, value: String, unit: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        Text(
            text = value, 
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = unit, 
            style = MaterialTheme.typography.labelSmall,
            color = AppleGray2
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label, 
            style = MaterialTheme.typography.labelMedium,
            color = AppleGray1
        )
    }
}

/**
 * 苹果风格的用餐状态卡片
 */
@Composable
private fun MealStatusCard(
    hasActiveSession: Boolean,
    sessionStatus: String,
    sessionStartTime: Long?,
    totalCalories: Double,
    isConnected: Boolean,
    isProcessing: Boolean,
    onEndMealSession: () -> Unit,
    onStartMealSession: () -> Unit
) {
    // 动画效果
    val cardColor by animateColorAsState(
        targetValue = if (hasActiveSession) 
            AppleTeal.copy(alpha = 0.06f) 
        else 
            MaterialTheme.colorScheme.surface,
        animationSpec = tween(300),
        label = "mealCardColor"
    )
    
    val statusColor = if (hasActiveSession) AppleTeal else AppleGray2
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 状态图标 - 苹果风格
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (hasActiveSession) 
                                AppleTeal.copy(alpha = 0.15f)
                            else 
                                AppleGray5
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (hasActiveSession) Icons.Rounded.Restaurant else Icons.Rounded.RestaurantMenu,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(14.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "用餐状态",
                        style = MaterialTheme.typography.labelMedium,
                        color = AppleGray1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = sessionStatus,
                        style = MaterialTheme.typography.headlineMedium,
                        color = statusColor
                    )
                    
                    // 显示开始时间和持续时间（实时更新）
                    if (hasActiveSession && sessionStartTime != null) {
                        var elapsedSeconds by remember { mutableStateOf(0L) }
                        
                        LaunchedEffect(sessionStartTime) {
                            while (true) {
                                elapsedSeconds = (System.currentTimeMillis() - sessionStartTime) / 1000
                                kotlinx.coroutines.delay(1000)
                            }
                        }
                        
                        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
                        val startTimeText = formatter.format(Date(sessionStartTime))
                        val minutes = elapsedSeconds / 60
                        val seconds = elapsedSeconds % 60
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = AppleGray2
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$startTimeText 开始 · ${minutes}分${seconds}秒",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppleGray1
                            )
                        }
                    }
                    
                    // 显示已摄入热量
                    if (hasActiveSession && totalCalories > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.LocalFireDepartment,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = CalorieRed
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "已摄入 ${totalCalories.toInt()} kcal",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = CalorieRed
                            )
                        }
                    }
                }
                
                // 操作按钮
                if (hasActiveSession) {
                    Button(
                        onClick = onEndMealSession,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ApplePink
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "结束", 
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

/**
 * 最近识别记录卡片
 */
@Composable
private fun RecentRecognitionsCard(
    recognitionHistory: List<RecognitionHistory>,
    onHistoryClick: (RecognitionHistory) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "最近识别",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (recognitionHistory.isNotEmpty()) {
                    Text(
                        text = "共 ${recognitionHistory.size} 条",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (recognitionHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "暂无识别记录",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "拍照识别食物开始记录",
                            color = Color.Gray.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                recognitionHistory.take(10).forEach { history ->
                    RecognitionHistoryItem(
                        history = history,
                        onClick = { onHistoryClick(history) }
                    )
                    if (history != recognitionHistory.take(10).last()) {
                        Divider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = Color.Gray.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 单条识别记录项
 */
@Composable
private fun RecognitionHistoryItem(
    history: RecognitionHistory,
    onClick: () -> Unit = {}
) {
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
    val categoryColor = Color(history.getCategoryColor())
    
    // 根据分类选择图标
    val categoryIcon = when (history.category.lowercase()) {
        "meal" -> Icons.Default.Restaurant
        "snack" -> Icons.Default.Cookie
        "beverage" -> Icons.Default.LocalDrink
        "dessert" -> Icons.Default.Cake
        "fruit" -> Icons.Default.Eco
        else -> Icons.Default.Fastfood
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 分类图标
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(categoryColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = categoryIcon,
                contentDescription = null,
                tint = categoryColor,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 食物信息
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 分类标签
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(categoryColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = history.getCategoryText(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = categoryColor
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = dateFormat.format(Date(history.timestamp)),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = history.foodName,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // 热量
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${history.totalCalories.toInt()}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFFFF6B6B)
            )
            Text(
                text = "kcal",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}


/**
 * 最近用餐记录卡片
 */
@Composable
private fun RecentMealsCard(
    sessions: List<MealSessionEntity>,
    onSessionClick: (MealSessionEntity) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "最近用餐",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (sessions.isNotEmpty()) {
                    Text(
                        text = "共 ${sessions.size} 条",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (sessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.RestaurantMenu,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "暂无用餐记录",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "连接眼镜开始记录",
                            color = Color.Gray.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                sessions.take(5).forEach { session ->
                    MealSessionItem(
                        session = session,
                        onClick = { onSessionClick(session) }
                    )
                    if (session != sessions.take(5).last()) {
                        Divider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = Color.Gray.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 单条用餐记录项
 */
@Composable
private fun MealSessionItem(
    session: MealSessionEntity,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
    val mealTypeText = when (session.mealType) {
        "breakfast" -> "早餐"
        "lunch" -> "午餐"
        "dinner" -> "晚餐"
        "snack" -> "加餐"
        else -> "用餐"
    }
    val mealTypeIcon = when (session.mealType) {
        "breakfast" -> Icons.Default.WbSunny
        "lunch" -> Icons.Default.LightMode
        "dinner" -> Icons.Default.NightsStay
        "snack" -> Icons.Default.Cookie
        else -> Icons.Default.Restaurant
    }
    val mealTypeColor = when (session.mealType) {
        "breakfast" -> Color(0xFFFFB74D)
        "lunch" -> Color(0xFF4FC3F7)
        "dinner" -> Color(0xFF7986CB)
        "snack" -> Color(0xFFAED581)
        else -> Color(0xFF90A4AE)
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 餐类图标
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(mealTypeColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = mealTypeIcon,
                contentDescription = null,
                tint = mealTypeColor,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 餐类和时间
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mealTypeText,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            )
            Text(
                text = dateFormat.format(Date(session.startTime)),
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
        
        // 热量
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${(session.totalServedKcal ?: 0.0).toInt()}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFFFF6B6B)
            )
            Text(
                text = "kcal",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.Gray.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * 设备选择对话框
 */
@Composable
private fun DeviceSelectorDialog(
    devices: List<ScannedDevice>,
    isScanning: Boolean,
    onDeviceSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "选择设备",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (devices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (isScanning) {
                                CircularProgressIndicator(modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "正在搜索设备...",
                                    color = Color.Gray
                                )
                            } else {
                                Icon(
                                    Icons.Default.BluetoothSearching,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = Color.Gray.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "未发现设备",
                                    color = Color.Gray
                                )
                                Text(
                                    text = "请确保眼镜已开机并靠近手机",
                                    fontSize = 12.sp,
                                    color = Color.Gray.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                } else {
                    devices.forEach { device ->
                        DeviceItem(
                            device = device,
                            onClick = { onDeviceSelected(device.address) }
                        )
                        if (device != devices.last()) {
                            Divider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = Color.Gray.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("取消")
                }
            }
        }
    }
}

/**
 * 设备列表项
 */
@Composable
private fun DeviceItem(
    device: ScannedDevice,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AppleTeal.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_glasses),
                contentDescription = null,
                tint = AppleTeal,
                modifier = Modifier.size(26.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = device.address,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
        
        // 信号强度指示
        val signalStrength = when {
            device.rssi > -50 -> 3
            device.rssi > -70 -> 2
            else -> 1
        }
        Row {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height((8 + index * 4).dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (index < signalStrength) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                Color.Gray.copy(alpha = 0.3f)
                        )
                )
                if (index < 2) Spacer(modifier = Modifier.width(2.dp))
            }
        }
    }
}


/**
 * 苹果风格的最近用餐记录卡片
 */
@Composable
private fun RecentMealsCardWithPhotos(
    sessionsWithPhotos: List<MealSessionWithPhoto>,
    onSessionClick: (MealSessionEntity) -> Unit,
    onSessionLongPress: (MealSessionEntity) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "最近用餐",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (sessionsWithPhotos.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AppleGray6
                    ) {
                        Text(
                            text = "${sessionsWithPhotos.size} 条记录",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppleGray1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (sessionsWithPhotos.isEmpty()) {
                // 空状态 - 苹果风格
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(AppleGray6),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.RestaurantMenu,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = AppleGray3
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "暂无用餐记录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppleGray1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "连接眼镜开始记录",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppleGray2
                        )
                    }
                }
            } else {
                sessionsWithPhotos.take(5).forEachIndexed { index, sessionWithPhoto ->
                    MealSessionItemWithPhoto(
                        sessionWithPhoto = sessionWithPhoto,
                        onClick = { onSessionClick(sessionWithPhoto.session) },
                        onLongPress = { onSessionLongPress(sessionWithPhoto.session) }
                    )
                    if (index < sessionsWithPhotos.take(5).size - 1) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Divider(
                            modifier = Modifier.padding(start = 72.dp),
                            color = AppleGray5,
                            thickness = 0.5.dp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

/**
 * 获取食物分类颜色
 */
private fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "meal" -> AppleTeal        // 正餐 - 绿色
        "snack" -> AppleOrange     // 零食 - 橙色
        "beverage" -> AppleBlue    // 饮料 - 蓝色
        "dessert" -> ApplePink     // 甜点 - 粉色
        "fruit" -> Color(0xFF8BC34A)  // 水果 - 浅绿色
        else -> AppleGray1         // 其他 - 灰色
    }
}

/**
 * 苹果风格的用餐记录项
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MealSessionItemWithPhoto(
    sessionWithPhoto: MealSessionWithPhoto,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {}
) {
    val session = sessionWithPhoto.session
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
    val mealTypeText = when (session.mealType) {
        "breakfast" -> "早餐"
        "lunch" -> "午餐"
        "dinner" -> "晚餐"
        "snack" -> "加餐"
        else -> "用餐"
    }
    val mealTypeColor = when (session.mealType) {
        "breakfast" -> AppleOrange
        "lunch" -> AppleBlue
        "dinner" -> ApplePurple
        "snack" -> AppleTeal
        else -> AppleGray1
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 照片缩略图 - 苹果风格圆角
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AppleGray6),
            contentAlignment = Alignment.Center
        ) {
            if (sessionWithPhoto.photoUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(sessionWithPhoto.photoUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "食物照片",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Restaurant,
                    contentDescription = null,
                    tint = AppleGray3,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(14.dp))
        
        // 用餐信息
        Column(modifier = Modifier.weight(1f)) {
            // 第一行：餐类标签 + 食物标签
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 餐类标签
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = mealTypeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = mealTypeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = mealTypeColor,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                
                // 食物标签（最多显示3个）
                val foodsToShow = sessionWithPhoto.foods.take(3)
                if (foodsToShow.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    foodsToShow.forEach { food ->
                        val tagColor = getCategoryColor(food.category)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = tagColor.copy(alpha = 0.12f),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = food.name.take(4),  // 最多4个字
                                style = MaterialTheme.typography.labelSmall,
                                color = tagColor,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    // 如果还有更多
                    if (sessionWithPhoto.foods.size > 3) {
                        Text(
                            text = "+${sessionWithPhoto.foods.size - 3}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppleGray2,
                            fontSize = 10.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 第二行：日期时间 + 时长
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = dateFormat.format(Date(session.startTime)),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppleGray1
                )
                if (session.durationMinutes != null && session.durationMinutes > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppleGray2
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${session.durationMinutes.toInt()}分钟",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppleGray2
                    )
                }
            }
        }
        
        // 热量 - 苹果风格
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${session.totalConsumedKcal?.toInt() ?: 0}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = CalorieRed
            )
            Text(
                text = "kcal",
                style = MaterialTheme.typography.labelSmall,
                color = AppleGray2
            )
        }
        
        Spacer(modifier = Modifier.width(4.dp))
        
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = AppleGray3,
            modifier = Modifier.size(20.dp)
        )
    }
}


