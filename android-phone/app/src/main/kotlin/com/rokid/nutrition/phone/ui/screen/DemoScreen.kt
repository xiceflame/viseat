package com.rokid.nutrition.phone.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rokid.nutrition.phone.demo.*
import com.rokid.nutrition.phone.ui.theme.*

/**
 * 演示模式主界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoScreen(
    uiState: DemoUiState,
    onSelectMode: (DemoMode) -> Unit,
    onSelectFood: (String) -> Unit,
    onStartRecognition: () -> Unit,
    onStartMeal: () -> Unit,
    onTriggerCapture: () -> Unit,
    onEndMeal: () -> Unit,
    onBack: () -> Unit,
    onClearError: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        when (uiState.currentMode) {
                            DemoMode.SELECTION -> "演示模式"
                            DemoMode.SINGLE_IMAGE -> "单图识别"
                            DemoMode.MEAL_MONITOR -> "用餐监测"
                        }
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (uiState.currentMode) {
                DemoMode.SELECTION -> ModeSelectionContent(
                    dataStatus = uiState.dataStatus,
                    onSelectMode = onSelectMode
                )
                DemoMode.SINGLE_IMAGE -> SingleImageContent(
                    uiState = uiState,
                    onSelectFood = onSelectFood,
                    onStartRecognition = onStartRecognition
                )
                DemoMode.MEAL_MONITOR -> MealMonitorContent(
                    uiState = uiState,
                    onStartMeal = onStartMeal,
                    onTriggerCapture = onTriggerCapture,
                    onEndMeal = onEndMeal
                )
            }
            
            // 错误提示
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = onClearError) {
                            Text("关闭")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }
}

/**
 * 模式选择内容
 */
@Composable
private fun ModeSelectionContent(
    dataStatus: DemoDataStatus,
    onSelectMode: (DemoMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "选择演示模式",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "模拟眼镜端的食物识别功能，数据将保存到历史记录",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 单图识别卡片
        ModeCard(
            title = "单图识别",
            description = "选择预设食物图片进行识别\n支持：可乐、薯片",
            icon = Icons.Rounded.CameraAlt,
            enabled = dataStatus.singleImageModeAvailable,
            onClick = { onSelectMode(DemoMode.SINGLE_IMAGE) }
        )
        
        // 用餐监测卡片
        ModeCard(
            title = "用餐监测",
            description = "模拟完整用餐流程\n开始 → 用餐中 → 结束",
            icon = Icons.Rounded.Restaurant,
            enabled = dataStatus.mealMonitorModeAvailable,
            onClick = { onSelectMode(DemoMode.MEAL_MONITOR) }
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // 数据状态提示
        if (!dataStatus.singleImageModeAvailable || !dataStatus.mealMonitorModeAvailable) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "部分模拟数据不可用，请检查 assets/demo 目录",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

/**
 * 模式选择卡片
 */
@Composable
private fun ModeCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        if (enabled) AppleTeal else Color.Gray,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) 
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) 
                    else 
                        Color.Gray
                )
            }
            
            if (enabled) {
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * 单图识别内容
 */
@Composable
private fun SingleImageContent(
    uiState: DemoUiState,
    onSelectFood: (String) -> Unit,
    onStartRecognition: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 食物选择
        Text(
            text = "选择食物",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FoodSelectionCard(
                name = "可乐",
                type = "coke",
                selected = uiState.selectedFood == "coke",
                enabled = uiState.dataStatus.cokeAvailable,
                modifier = Modifier.weight(1f),
                onClick = { onSelectFood("coke") }
            )
            
            FoodSelectionCard(
                name = "薯片",
                type = "chips",
                selected = uiState.selectedFood == "chips",
                enabled = uiState.dataStatus.chipsAvailable,
                modifier = Modifier.weight(1f),
                onClick = { onSelectFood("chips") }
            )
        }
        
        // 图片预览
        uiState.previewImage?.let { bitmap ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "预览图片",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        
        // 识别按钮
        Button(
            onClick = onStartRecognition,
            enabled = uiState.selectedFood != null && !uiState.isProcessing,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AppleTeal)
        ) {
            if (uiState.isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(uiState.processingMessage)
            } else {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("开始识别")
            }
        }
        
        // 识别结果
        uiState.recognitionResult?.let { result ->
            RecognitionResultCard(result = result)
        }
    }
}

/**
 * 食物选择卡片
 */
@Composable
private fun FoodSelectionCard(
    name: String,
    type: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = when {
                selected -> AppleTeal.copy(alpha = 0.2f)
                enabled -> MaterialTheme.colorScheme.surface
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        border = if (selected) 
            androidx.compose.foundation.BorderStroke(2.dp, AppleTeal) 
        else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                when (type) {
                    "coke" -> Icons.Rounded.LocalDrink
                    else -> Icons.Rounded.Fastfood
                },
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (selected) AppleTeal else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

/**
 * 识别结果卡片
 */
@Composable
private fun RecognitionResultCard(
    result: com.rokid.nutrition.phone.network.model.VisionAnalyzeResponse
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "识别结果",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 食物名称
            result.rawLlm.foods.forEach { food ->
                Text(
                    text = food.getDisplayName(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 营养数据
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NutritionItem("热量", "${result.snapshot.nutrition.calories.toInt()}", "kcal")
                NutritionItem("蛋白质", "${result.snapshot.nutrition.protein.toInt()}", "g")
                NutritionItem("碳水", "${result.snapshot.nutrition.carbs.toInt()}", "g")
                NutritionItem("脂肪", "${result.snapshot.nutrition.fat.toInt()}", "g")
            }
            
            // 建议
            result.getEffectiveSuggestion().takeIf { it.isNotBlank() }?.let { suggestion ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = suggestion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * 营养项
 */
@Composable
private fun NutritionItem(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AppleTeal
        )
        Text(
            text = "$label($unit)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
    }
}

/**
 * 用餐监测内容
 */
@Composable
private fun MealMonitorContent(
    uiState: DemoUiState,
    onStartMeal: () -> Unit,
    onTriggerCapture: () -> Unit,
    onEndMeal: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 阶段指示器
        MealPhaseIndicator(currentPhase = uiState.mealPhase)
        
        // 图片预览
        uiState.previewImage?.let { bitmap ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "当前图片",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        
        // 进度信息
        uiState.mealProgress?.let { progress ->
            MealProgressCard(progress = progress)
        }
        
        // 操作按钮
        when (uiState.mealPhase) {
            MealPhase.NOT_STARTED -> {
                Button(
                    onClick = onStartMeal,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppleTeal)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("开始用餐")
                }
            }
            MealPhase.STARTING, MealPhase.ENDING -> {
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(uiState.processingMessage)
                }
            }
            MealPhase.IN_PROGRESS -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onTriggerCapture,
                        enabled = !uiState.isProcessing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("拍摄")
                    }
                    
                    Button(
                        onClick = onEndMeal,
                        enabled = !uiState.isProcessing,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AppleTeal)
                    ) {
                        Icon(Icons.Rounded.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("结束用餐")
                    }
                }
            }
            MealPhase.COMPLETED -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = AppleTeal.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = AppleTeal,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "用餐已完成，数据已保存到历史记录",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        
        // 识别结果
        uiState.recognitionResult?.let { result ->
            RecognitionResultCard(result = result)
        }
    }
}

/**
 * 用餐阶段指示器
 */
@Composable
private fun MealPhaseIndicator(currentPhase: MealPhase) {
    val phases = listOf(
        "开始" to MealPhase.STARTING,
        "用餐中" to MealPhase.IN_PROGRESS,
        "结束" to MealPhase.COMPLETED
    )
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        phases.forEachIndexed { index, (name, phase) ->
            val isActive = currentPhase.ordinal >= phase.ordinal
            val isCurrent = currentPhase == phase || 
                (currentPhase == MealPhase.STARTING && phase == MealPhase.STARTING) ||
                (currentPhase == MealPhase.ENDING && phase == MealPhase.COMPLETED)
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (isActive) AppleTeal else Color.Gray.copy(alpha = 0.3f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isActive && !isCurrent) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(
                            text = "${index + 1}",
                            color = if (isActive) Color.White else Color.Gray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive) AppleTeal else Color.Gray
                )
            }
            
            if (index < phases.size - 1) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .padding(horizontal = 8.dp)
                        .align(Alignment.CenterVertically)
                        .background(
                            if (currentPhase.ordinal > phase.ordinal) AppleTeal 
                            else Color.Gray.copy(alpha = 0.3f)
                        )
                )
            }
        }
    }
}

/**
 * 用餐进度卡片
 */
@Composable
private fun MealProgressCard(progress: MealProgress) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "用餐进度",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 进度条
            LinearProgressIndicator(
                progress = progress.consumptionRatio.toFloat().coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = AppleTeal,
                trackColor = Color.Gray.copy(alpha = 0.2f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "已消耗: ${progress.consumedCalories.toInt()} kcal",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${(progress.consumptionRatio * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppleTeal
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "基线: ${progress.baselineCalories.toInt()} kcal | 剩余: ${progress.currentCalories.toInt()} kcal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}
