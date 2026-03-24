package com.rokid.nutrition.phone.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rokid.nutrition.phone.network.model.VisionAnalyzeResponse
import com.rokid.nutrition.phone.sync.SyncStatus
import com.rokid.nutrition.phone.ui.component.EditFoodDialog
import com.rokid.nutrition.phone.ui.component.PhotoViewerDialog
import com.rokid.nutrition.phone.ui.viewmodel.EditableFoodItem
import com.rokid.nutrition.phone.ui.viewmodel.FoodDetailUiState
import com.rokid.nutrition.phone.util.FoodItemUpdates

/**
 * 食物详情页面
 * 
 * 显示后端识别的完整食材信息和营养数据
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodDetailScreen(
    foodData: VisionAnalyzeResponse?,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (foodData == null) {
        // 数据为空时显示错误
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("食物详情") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无数据", color = Color.Gray)
            }
        }
        return
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("食物详情") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 总营养概览卡片
            NutritionOverviewCard(foodData)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 菜品列表
            foodData.rawLlm.foods.forEachIndexed { index, food ->
                DishCard(food, index + 1)
                if (index < foodData.rawLlm.foods.size - 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

/**
 * 增强版食物详情页面
 * 
 * 支持照片展示、数据编辑和同步状态显示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedFoodDetailScreen(
    uiState: FoodDetailUiState,
    onBackClick: () -> Unit,
    onFoodClick: (String) -> Unit,
    onSaveEdit: (FoodItemUpdates) -> Unit,
    onCancelEdit: () -> Unit,
    onDeleteFood: () -> Unit,
    onDownloadPhoto: () -> Unit,
    onTriggerSync: () -> Unit,
    onAddFood: (String, Double) -> Unit = { _, _ -> },  // 食物名称, 重量(克)
    onClearSaveSuccess: () -> Unit = {},
    onClearDownloadSuccess: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showPhotoViewer by remember { mutableStateOf(false) }
    var showAddFoodDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 显示成功/错误消息 - 显示后立即清除状态,避免重复弹出
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar("保存成功")
            onClearSaveSuccess()
        }
    }
    
    LaunchedEffect(uiState.downloadSuccess) {
        if (uiState.downloadSuccess) {
            snackbarHostState.showSnackbar("已保存到相册")
            onClearDownloadSuccess()
        }
    }
    
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("食物详情") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 同步状态指示器
                    SyncStatusIndicator(
                        syncStatus = uiState.syncStatus,
                        pendingCount = uiState.pendingSyncCount,
                        onSyncClick = onTriggerSync
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (uiState.isLoading && uiState.snapshot == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.snapshot == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无数据", color = Color.Gray)
            }
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // 照片展示
                uiState.photoUri?.let { photoUri ->
                    MealPhotoCard(
                        photoUri = photoUri,
                        onClick = { showPhotoViewer = true }
                    )
                }
                
                Column(modifier = Modifier.padding(16.dp)) {
                    // 总营养概览
                    EnhancedNutritionOverviewCard(uiState.foods)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 可编辑的食物列表
                    Text(
                        text = "食物列表",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "点击食物可编辑数据",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    uiState.foods.forEachIndexed { index, food ->
                        EditableFoodCard(
                            food = food,
                            index = index + 1,
                            onClick = { onFoodClick(food.id) }
                        )
                        if (index < uiState.foods.size - 1) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 新增食物按钮
                    OutlinedButton(
                        onClick = { showAddFoodDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("新增食物")
                    }
                }
            }
        }
    }
    
    // 照片查看器对话框
    if (showPhotoViewer) {
        PhotoViewerDialog(
            imageUri = uiState.photoUri,
            onDismiss = { showPhotoViewer = false },
            onDownload = onDownloadPhoto,
            isDownloading = uiState.isLoading
        )
    }
    
    // 编辑对话框
    if (uiState.isEditing && uiState.editingFood != null) {
        EditFoodDialog(
            food = uiState.editingFood,
            onSave = onSaveEdit,
            onCancel = onCancelEdit,
            onDelete = onDeleteFood,
            isLoading = uiState.isLoading,
            validationErrors = uiState.validationErrors
        )
    }
    
    // 新增食物对话框
    if (showAddFoodDialog) {
        AddFoodDialog(
            onAdd = { name, weight ->
                onAddFood(name, weight)
                showAddFoodDialog = false
            },
            onDismiss = { showAddFoodDialog = false },
            isLoading = uiState.isLoading
        )
    }
}

/**
 * 照片卡片
 */
@Composable
private fun MealPhotoCard(
    photoUri: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(0.dp)
    ) {
        Box {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photoUri)
                    .crossfade(true)
                    .build(),
                contentDescription = "食物照片",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // 点击提示
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "点击查看大图",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/**
 * 同步状态指示器
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SyncStatusIndicator(
    syncStatus: SyncStatus,
    pendingCount: Int,
    onSyncClick: () -> Unit
) {
    when (syncStatus) {
        SyncStatus.Synced -> {
            Icon(
                imageVector = Icons.Default.CloudDone,
                contentDescription = "已同步",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.padding(end = 16.dp)
            )
        }
        SyncStatus.Pending -> {
            IconButton(onClick = onSyncClick) {
                BadgedBox(
                    badge = {
                        if (pendingCount > 0) {
                            Badge { Text("$pendingCount") }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "待同步",
                        tint = Color(0xFFFF9800)
                    )
                }
            }
        }
        SyncStatus.Syncing -> {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 16.dp),
                strokeWidth = 2.dp
            )
        }
        SyncStatus.Failed -> {
            IconButton(onClick = onSyncClick) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "同步失败",
                    tint = Color(0xFFF44336)
                )
            }
        }
    }
}

/**
 * 增强版营养概览卡片
 */
@Composable
private fun EnhancedNutritionOverviewCard(foods: List<EditableFoodItem>) {
    val totalCalories = foods.sumOf { it.caloriesKcal }
    val totalProtein = foods.sumOf { it.proteinG }
    val totalCarbs = foods.sumOf { it.carbsG }
    val totalFat = foods.sumOf { it.fatG }
    val hasEdited = foods.any { it.isEdited }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "总营养成分",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                if (hasEdited) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "已编辑",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 热量大卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFF6B6B).copy(alpha = 0.15f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "${totalCalories.toInt()}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 48.sp,
                        color = Color(0xFFFF6B6B)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "kcal",
                        fontSize = 20.sp,
                        color = Color(0xFFFF6B6B).copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 三大营养素
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroNutrientItem(
                    icon = Icons.Default.FitnessCenter,
                    label = "蛋白质",
                    value = String.format("%.1f", totalProtein),
                    unit = "g",
                    color = Color(0xFF4ECDC4)
                )
                MacroNutrientItem(
                    icon = Icons.Default.Grain,
                    label = "碳水",
                    value = String.format("%.1f", totalCarbs),
                    unit = "g",
                    color = Color(0xFFFFE66D)
                )
                MacroNutrientItem(
                    icon = Icons.Default.WaterDrop,
                    label = "脂肪",
                    value = String.format("%.1f", totalFat),
                    unit = "g",
                    color = Color(0xFF95E1D3)
                )
            }
        }
    }
}

/**
 * 可编辑的食物卡片
 */
@Composable
private fun EditableFoodCard(
    food: EditableFoodItem,
    index: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$index",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = food.chineseName ?: food.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        if (food.isEdited) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "已编辑",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                    Text(
                        text = "${food.weightG.toInt()}g · ${food.caloriesKcal.toInt()} kcal",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "编辑",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 营养素简要
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NutrientChip("蛋白质", "${food.proteinG.toInt()}g")
                NutrientChip("碳水", "${food.carbsG.toInt()}g")
                NutrientChip("脂肪", "${food.fatG.toInt()}g")
            }
        }
    }
}

@Composable
private fun NutrientChip(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * 总营养概览卡片
 */
@Composable
private fun NutritionOverviewCard(foodData: VisionAnalyzeResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "总营养成分",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 热量大卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFF6B6B).copy(alpha = 0.15f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "${foodData.snapshot.nutrition.calories.toInt()}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 48.sp,
                        color = Color(0xFFFF6B6B)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "kcal",
                        fontSize = 20.sp,
                        color = Color(0xFFFF6B6B).copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 三大营养素
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroNutrientItem(
                    icon = Icons.Default.FitnessCenter,
                    label = "蛋白质",
                    value = String.format("%.1f", foodData.snapshot.nutrition.protein),
                    unit = "g",
                    color = Color(0xFF4ECDC4)
                )
                MacroNutrientItem(
                    icon = Icons.Default.Grain,
                    label = "碳水",
                    value = String.format("%.1f", foodData.snapshot.nutrition.carbs),
                    unit = "g",
                    color = Color(0xFFFFE66D)
                )
                MacroNutrientItem(
                    icon = Icons.Default.WaterDrop,
                    label = "脂肪",
                    value = String.format("%.1f", foodData.snapshot.nutrition.fat),
                    unit = "g",
                    color = Color(0xFF95E1D3)
                )
            }
        }
    }
}

/**
 * 宏量营养素项
 */
@Composable
private fun MacroNutrientItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    unit: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(90.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = color
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = unit,
                fontSize = 12.sp,
                color = color.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color.Gray
        )
    }
}

/**
 * 单个菜品卡片
 */
@Composable
private fun DishCard(
    food: com.rokid.nutrition.phone.network.model.FoodItemResponse,
    index: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 菜品标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$index",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = food.dishNameCn ?: food.dishName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    if (food.dishNameCn != null && food.dishName != food.dishNameCn) {
                        Text(
                            text = food.dishName,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
                
                // 分类标签
                food.category?.let { category ->
                    val (categoryText, categoryColor) = when (category.lowercase()) {
                        "meal" -> "正餐" to Color(0xFF4CAF50)
                        "snack" -> "零食" to Color(0xFFFF9800)
                        "beverage" -> "饮料" to Color(0xFF2196F3)
                        "dessert" -> "甜点" to Color(0xFFE91E63)
                        "fruit" -> "水果" to Color(0xFF8BC34A)
                        else -> category to Color.Gray
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = categoryColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = categoryText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = categoryColor
                        )
                    }
                }
            }
            
            // 烹饪方式
            if (food.cookingMethod.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFFF9800)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "烹饪方式: ${food.cookingMethod}",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }
            
            // 总重量
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Scale,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFF9C27B0)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "总重量: ${food.totalWeightG.toInt()}g",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFF4CAF50)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "置信度: ${(food.confidence * 100).toInt()}%",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
            
            // 食材列表
            if (food.ingredients.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color.Gray.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "食材成分",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                food.ingredients.forEach { ingredient ->
                    IngredientItem(ingredient)
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

/**
 * 食材项
 */
@Composable
private fun IngredientItem(ingredient: com.rokid.nutrition.phone.network.model.Ingredient) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Gray.copy(alpha = 0.05f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = ingredient.nameEn,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${ingredient.weightG.toInt()}g",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "${(ingredient.confidence * 100).toInt()}%",
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

/**
 * 新增食物对话框
 */
@Composable
private fun AddFoodDialog(
    onAdd: (name: String, weight: Double) -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean = false
) {
    var foodName by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("100") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var weightError by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        icon = {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text("新增食物")
        },
        text = {
            Column {
                Text(
                    text = "输入食物名称和重量，系统将自动从云端推算营养数据",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 食物名称输入
                OutlinedTextField(
                    value = foodName,
                    onValueChange = { 
                        foodName = it
                        nameError = null
                    },
                    label = { Text("食物名称") },
                    placeholder = { Text("如：米饭、苹果、牛肉") },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 重量输入
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { 
                        weightText = it.filter { c -> c.isDigit() || c == '.' }
                        weightError = null
                    },
                    label = { Text("重量 (克)") },
                    placeholder = { Text("100") },
                    singleLine = true,
                    isError = weightError != null,
                    supportingText = weightError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    trailingIcon = { Text("g", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("正在查询营养数据...", fontSize = 14.sp, color = Color.Gray)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // 验证输入
                    var hasError = false
                    if (foodName.isBlank()) {
                        nameError = "请输入食物名称"
                        hasError = true
                    }
                    val weight = weightText.toDoubleOrNull()
                    if (weight == null || weight <= 0) {
                        weightError = "请输入有效重量"
                        hasError = true
                    }
                    if (!hasError && weight != null) {
                        onAdd(foodName.trim(), weight)
                    }
                },
                enabled = !isLoading
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("取消")
            }
        }
    )
}
