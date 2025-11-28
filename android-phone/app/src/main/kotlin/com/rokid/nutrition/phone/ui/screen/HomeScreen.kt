package com.rokid.nutrition.phone.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rokid.nutrition.phone.bluetooth.ConnectionState
import com.rokid.nutrition.phone.bluetooth.GlassInfo
import com.rokid.nutrition.phone.data.entity.MealSessionEntity
import com.rokid.nutrition.phone.ui.component.DeleteMealConfirmDialog
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
    modifier: Modifier = Modifier
) {
    val isConnected = uiState.connectionState is ConnectionState.Connected
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 眼镜状态卡片
        GlassesStatusCard(
            connectionState = uiState.connectionState,
            glassInfo = glassInfo,
            glassesName = uiState.glassesName,
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
}


@Composable
private fun GlassesStatusCard(
    connectionState: ConnectionState,
    glassInfo: GlassInfo?,
    glassesName: String?,
    batteryLevel: Int,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit
) {
    val isConnected = connectionState is ConnectionState.Connected
    val isConnecting = connectionState is ConnectionState.Connecting || 
                       connectionState is ConnectionState.Scanning
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            if (isConnected) MaterialTheme.colorScheme.primary
                            else Color.Gray.copy(alpha = 0.3f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            isConnected -> glassesName ?: glassInfo?.name ?: "Rokid 眼镜"
                            isConnecting -> "正在连接..."
                            connectionState is ConnectionState.Error -> "连接失败"
                            else -> "未连接眼镜"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    
                    if (isConnected) {
                        val displayBattery = when {
                            glassInfo != null && glassInfo.batteryLevel > 0 -> glassInfo.batteryLevel
                            batteryLevel > 0 -> batteryLevel
                            else -> 0
                        }
                        val isCharging = glassInfo?.isCharging == true
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = when {
                                    isCharging -> Color(0xFF4CAF50)
                                    displayBattery > 50 -> Color(0xFF4CAF50)
                                    displayBattery > 20 -> Color(0xFFFFC107)
                                    else -> Color(0xFFFF5722)
                                }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isCharging && displayBattery == 0) "充电中" else "$displayBattery%${if (isCharging) " 充电中" else ""}",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    
                    if (connectionState is ConnectionState.Error) {
                        Text(
                            text = connectionState.message,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (isConnected) {
                OutlinedButton(
                    onClick = onDisconnectClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.BluetoothDisabled, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("断开连接")
                }
            } else {
                Button(
                    onClick = onConnectClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isConnecting
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("连接中...")
                    } else {
                        Icon(Icons.Default.Bluetooth, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("连接眼镜")
                    }
                }
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
                NutrientItem("碳水", String.format("%.1f", carbs), "g", Color(0xFFFFE66D))
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
 * 带照片缩略图的最新识别结果卡片
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
            
            // 照片和食物信息并排显示
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // 缩略图（逆时针旋转90度）
                if (imageUrl != null) {
                    Card(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "食物照片",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    rotationZ = -90f  // 逆时针旋转90度
                                }
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
                
                // 食物名称和热量
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = foodName, 
                        fontWeight = FontWeight.Medium, 
                        fontSize = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${calories.toInt()} kcal",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6B6B)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 营养素
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NutrientItem("蛋白质", String.format("%.1f", protein), "g", Color(0xFF4ECDC4))
                NutrientItem("碳水", String.format("%.1f", carbs), "g", Color(0xFFFFE66D))
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
            
            // 点击查看详情/编辑提示
            if (onClick != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "点击编辑",
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

@Composable
private fun NutrientItem(label: String, value: String, unit: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
        Text(text = unit, fontSize = 10.sp, color = Color.Gray)
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
    }
}

/**
 * 用餐状态卡片 - 显示用餐时间和控制按钮
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (hasActiveSession) 
                Color(0xFF4CAF50).copy(alpha = 0.1f) 
            else 
                Color.Gray.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 状态图标
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (hasActiveSession) Color(0xFF4CAF50).copy(alpha = 0.2f)
                            else Color.Gray.copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (hasActiveSession) Icons.Default.Restaurant else Icons.Default.RestaurantMenu,
                        contentDescription = null,
                        tint = if (hasActiveSession) Color(0xFF4CAF50) else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "用餐状态",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = sessionStatus,
                        color = if (hasActiveSession) Color(0xFF4CAF50) else Color.Gray,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // 显示开始时间和持续时间
                    if (hasActiveSession && sessionStartTime != null) {
                        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        val startTimeText = formatter.format(Date(sessionStartTime))
                        val durationMinutes = (System.currentTimeMillis() - sessionStartTime) / 1000 / 60
                        Text(
                            text = "开始: $startTimeText (${durationMinutes}分钟)",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    
                    // 显示已摄入热量
                    if (hasActiveSession && totalCalories > 0) {
                        Text(
                            text = "已摄入: ${totalCalories.toInt()} kcal",
                            fontSize = 12.sp,
                            color = Color(0xFFFF6B6B),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // 操作按钮 - 只在有活跃会话时显示结束用餐按钮
                if (hasActiveSession) {
                    Button(
                        onClick = onEndMealSession,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF5722)
                        ),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("结束用餐", fontSize = 13.sp)
                    }
                }
                // 已删除"开始用餐"按钮，用餐会话由眼镜端自动触发
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
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Visibility,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
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
 * 带照片缩略图的最近用餐记录卡片
 */
@Composable
private fun RecentMealsCardWithPhotos(
    sessionsWithPhotos: List<MealSessionWithPhoto>,
    onSessionClick: (MealSessionEntity) -> Unit,
    onSessionLongPress: (MealSessionEntity) -> Unit = {}
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
                if (sessionsWithPhotos.isNotEmpty()) {
                    Text(
                        text = "共 ${sessionsWithPhotos.size} 条",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (sessionsWithPhotos.isEmpty()) {
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
                sessionsWithPhotos.take(5).forEach { sessionWithPhoto ->
                    MealSessionItemWithPhoto(
                        sessionWithPhoto = sessionWithPhoto,
                        onClick = { onSessionClick(sessionWithPhoto.session) },
                        onLongPress = { onSessionLongPress(sessionWithPhoto.session) }
                    )
                    if (sessionWithPhoto != sessionsWithPhotos.take(5).last()) {
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
 * 带照片的单条用餐记录项
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
        "breakfast" -> Color(0xFFFFB74D)
        "lunch" -> Color(0xFF4FC3F7)
        "dinner" -> Color(0xFF7986CB)
        "snack" -> Color(0xFFAED581)
        else -> Color(0xFF90A4AE)
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 照片缩略图
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
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
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationZ = -90f  // 逆时针旋转90度
                        }
                )
            } else {
                // 占位图标
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 用餐信息
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 餐类标签
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(mealTypeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = mealTypeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = mealTypeColor
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = dateFormat.format(Date(session.startTime)),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 时长
            if (session.durationMinutes != null) {
                Text(
                    text = "${session.durationMinutes.toInt()} 分钟",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }
        
        // 热量
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${session.totalConsumedKcal?.toInt() ?: 0}",
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
