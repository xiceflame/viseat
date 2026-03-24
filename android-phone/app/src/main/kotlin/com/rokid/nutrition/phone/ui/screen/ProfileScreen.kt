@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.rokid.nutrition.phone.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rokid.nutrition.phone.repository.UserProfile
import com.rokid.nutrition.phone.ui.component.BreathingGlow
import com.rokid.nutrition.phone.ui.component.AnimatedCounter
import com.rokid.nutrition.phone.ui.theme.*

// ==================== BMI 状态枚举 ====================

enum class BMIStatus(
    val label: String,
    val color: Color,
    val suggestion: String
) {
    UNDERWEIGHT("偏瘦", Color(0xFF2196F3), "建议适当增加营养摄入"),
    NORMAL("正常", Color(0xFF4CAF50), "继续保持健康饮食"),
    OVERWEIGHT("偏胖", Color(0xFFFFC107), "建议控制热量摄入"),
    OBESE("肥胖", Color(0xFFFF5722), "建议咨询营养师")
}

fun getBMIStatus(bmi: Float): BMIStatus = when {
    bmi < 18.5f -> BMIStatus.UNDERWEIGHT
    bmi < 24.0f -> BMIStatus.NORMAL
    bmi < 28.0f -> BMIStatus.OVERWEIGHT
    else -> BMIStatus.OBESE
}

// ==================== 进度颜色函数 ====================

fun getProgressColor(current: Double, target: Int): Color {
    if (target <= 0) return Color(0xFF4CAF50)
    val percentage = current / target
    return when {
        percentage >= 1.0 -> Color(0xFFF44336)  // 红色：超标
        percentage >= 0.8 -> Color(0xFFFF9800)  // 橙色：接近目标
        else -> Color(0xFF4CAF50)               // 绿色：正常
    }
}

// ==================== 主页面 ====================

/**
 * 我的页面 - 重新设计版本
 */
@Composable
fun ProfileScreen(
    profile: UserProfile?,
    todayCalories: Double = 0.0,
    mealCount: Int = 0,
    onSaveProfile: (UserProfile) -> Unit,
    onNavigateToStats: () -> Unit = {},
    onOpenDebugLog: () -> Unit = {},
    onNavigateToDemoMode: () -> Unit = {},
    onClearData: () -> Unit = {},
    // 体重追踪相关
    currentWeight: Float? = null,
    latestWeightEntry: com.rokid.nutrition.phone.data.entity.WeightEntryEntity? = null,
    onAddWeight: (Float, String?) -> Unit = { _, _ -> },
    onViewWeightHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showAddWeightDialog by remember { mutableStateOf(false) }
    
    val targetCalories = profile?.calculateDailyCalories() ?: 2000
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 1. 用户信息头部
        UserHeaderCard(
            profile = profile,
            onEditClick = { showEditDialog = true }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 2. 今日营养进度
        NutritionProgressCard(
            currentCalories = todayCalories,
            targetCalories = targetCalories,
            mealCount = mealCount,
            onClick = onNavigateToStats
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 2.5 营养目标卡片
        if (profile != null) {
            NutritionGoalsCard(
                profile = profile,
                onClick = onNavigateToStats
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // 2.6 体重追踪卡片
        com.rokid.nutrition.phone.ui.component.WeightTrackingCard(
            currentWeight = currentWeight ?: profile?.weight,
            targetWeight = profile?.targetWeight,
            startWeight = profile?.weight,
            latestEntry = latestWeightEntry,
            onAddWeight = { showAddWeightDialog = true },
            onViewHistory = onViewWeightHistory
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 3. 健康档案
        HealthProfileCard(
            healthConditions = profile?.healthConditions ?: emptyList(),
            dietaryPreferences = profile?.dietaryPreferences ?: emptyList(),
            dietType = profile?.dietType,
            allergens = profile?.allergens ?: emptyList(),
            onEditClick = { showEditDialog = true }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 4. 设置区域
        SettingsSection(
            onClearDataClick = { showClearDataDialog = true }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 5. 调试模式区域
        com.rokid.nutrition.phone.ui.component.DebugModeSection(
            onDebugLogClick = onOpenDebugLog,
            onDemoModeClick = onNavigateToDemoMode
        )
    }
    
    // 添加体重对话框
    if (showAddWeightDialog) {
        com.rokid.nutrition.phone.ui.component.AddWeightDialog(
            currentWeight = currentWeight ?: profile?.weight,
            onDismiss = { showAddWeightDialog = false },
            onConfirm = { weight, note ->
                onAddWeight(weight, note)
                showAddWeightDialog = false
            }
        )
    }
    
    // 编辑对话框
    if (showEditDialog) {
        EditProfileDialog(
            profile = profile,
            onSave = { newProfile ->
                onSaveProfile(newProfile)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false }
        )
    }
    
    // 清除数据确认对话框
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF5722)) },
            title = { Text("确认清除数据？") },
            text = { Text("此操作将清除所有本地数据，包括用户档案和用餐记录。此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearData()
                        showClearDataDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5722))
                ) {
                    Text("确认清除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}


// ==================== 苹果风格用户信息头部卡片 ====================

@Composable
private fun UserHeaderCard(
    profile: UserProfile?,
    onEditClick: () -> Unit
) {
    val genderText = when (profile?.gender) {
        "male" -> "男"
        "female" -> "女"
        else -> ""
    }
    
    val goalText = when (profile?.healthGoal) {
        "lose_weight" -> "减重"
        "gain_muscle" -> "增肌"
        else -> "维持"
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = AppleTeal.copy(alpha = 0.1f),
                spotColor = AppleTeal.copy(alpha = 0.2f)
            )
            .clickable { onEditClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            AppleTeal,
                            AppleMint
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(24.dp)
        ) {
            if (profile != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 头像 - 带呼吸光晕效果
                    Box(
                        modifier = Modifier.size(76.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // 呼吸光晕背景
                        BreathingGlow(
                            color = Color.White,
                            size = 76.dp,
                            modifier = Modifier.matchParentSize()
                        )
                        // 头像圆形
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (profile.gender == "female") Icons.Rounded.Face else Icons.Rounded.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(18.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        // 昵称
                        Text(
                            text = profile.nickname ?: "用户",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // 基本信息
                        Text(
                            text = "$genderText · ${profile.age}岁 · ${profile.height.toInt()}cm · ${profile.weight.toInt()}kg",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // BMI 指示器 - 苹果风格
                        if (profile.bmi > 0) {
                            val bmiStatus = getBMIStatus(profile.bmi)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "BMI ${String.format("%.1f", profile.bmi)}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.White.copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = bmiStatus.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    // 健康目标徽章 - 苹果风格
                    Column(horizontalAlignment = Alignment.End) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = goalText,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                        
                        // 目标体重差值
                        profile.targetWeight?.let { targetWeight ->
                            if (profile.healthGoal == "lose_weight") {
                                val diff = profile.weight - targetWeight
                                if (diff > 0) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "还需减 ${diff.toInt()}kg",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.75f)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Icon(
                            Icons.Rounded.Edit,
                            contentDescription = "编辑",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            } else {
                // 空状态 - 苹果风格
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.PersonAdd,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(18.dp))
                    Column {
                        Text(
                            text = "点击设置个人信息",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "完善资料获取个性化建议",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}


// ==================== 苹果风格今日营养进度卡片 ====================

@Composable
private fun NutritionProgressCard(
    currentCalories: Double,
    targetCalories: Int,
    mealCount: Int,
    onClick: () -> Unit
) {
    val progress = if (targetCalories > 0) (currentCalories / targetCalories).coerceIn(0.0, 1.5) else 0.0
    val progressColor = if (progress >= 1.0) AppleRed else if (progress >= 0.8) AppleOrange else AppleTeal
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 圆形进度条 - 苹果风格
            Box(
                modifier = Modifier.size(90.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(90.dp)) {
                    val strokeWidth = 8.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val center = Offset(size.width / 2, size.height / 2)
                    
                    // 背景圆环
                    drawCircle(
                        color = AppleGray5,
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    
                    // 进度圆环
                    val sweepAngle = (progress * 360).toFloat().coerceAtMost(360f)
                    drawArc(
                        color = progressColor,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                
                // 中心文字
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = currentCalories.toInt().toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = progressColor
                    )
                    Text(
                        text = "/ $targetCalories",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppleGray1
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(20.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "今日热量",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = when {
                        progress >= 1.0 -> "已超出目标 ${((progress - 1) * 100).toInt()}%"
                        progress >= 0.8 -> "接近目标，注意控制"
                        else -> "继续保持"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = progressColor
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 用餐次数
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Restaurant,
                        contentDescription = null,
                        tint = AppleTeal,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "今日用餐 $mealCount 次",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppleGray1
                    )
                }
            }
            
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = AppleGray3,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ==================== 营养目标卡片 ====================

@Composable
private fun NutritionGoalsCard(
    profile: UserProfile,
    onClick: () -> Unit
) {
    val dailyCalories = profile.calculateDailyCalories()
    
    // 根据饮食类型计算宏量营养素比例
    val (proteinRatio, carbsRatio, fatRatio) = when (profile.dietType) {
        "keto" -> Triple(0.25f, 0.05f, 0.70f)
        "low_carb" -> Triple(0.30f, 0.20f, 0.50f)
        else -> Triple(0.25f, 0.50f, 0.25f)  // 默认比例
    }
    
    val proteinGrams = (dailyCalories * proteinRatio / 4).toInt()  // 1g蛋白质 = 4kcal
    val carbsGrams = (dailyCalories * carbsRatio / 4).toInt()      // 1g碳水 = 4kcal
    val fatGrams = (dailyCalories * fatRatio / 9).toInt()          // 1g脂肪 = 9kcal
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "每日营养目标",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = AppleGray3,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 热量目标
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppleTeal.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.LocalFireDepartment,
                        contentDescription = null,
                        tint = AppleTeal,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "热量",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppleGray1
                    )
                    Text(
                        text = "$dailyCalories kcal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 宏量营养素
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 蛋白质
                MacroItem(
                    label = "蛋白质",
                    value = "${proteinGrams}g",
                    color = AppleBlue,
                    modifier = Modifier.weight(1f)
                )
                
                // 碳水
                MacroItem(
                    label = "碳水",
                    value = "${carbsGrams}g",
                    color = AppleOrange,
                    modifier = Modifier.weight(1f)
                )
                
                // 脂肪
                MacroItem(
                    label = "脂肪",
                    value = "${fatGrams}g",
                    color = ApplePurple,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MacroItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AppleGray1
        )
    }
}

// ==================== 健康档案卡片 ====================

@Composable
private fun HealthProfileCard(
    healthConditions: List<String>,
    dietaryPreferences: List<String>,
    dietType: String? = null,
    allergens: List<String> = emptyList(),
    onEditClick: () -> Unit
) {
    // 饮食类型显示名称映射
    val dietTypeDisplayName = when (dietType) {
        "omnivore" -> "无限制"
        "vegetarian" -> "素食"
        "vegan" -> "纯素"
        "low_carb" -> "低碳水"
        "keto" -> "生酮"
        "mediterranean" -> "地中海饮食"
        else -> null
    }
    
    // 过敏原显示名称映射
    val allergenDisplayNames = allergens.mapNotNull { allergen ->
        when (allergen) {
            "gluten" -> "麸质"
            "dairy" -> "乳制品"
            "nuts" -> "坚果"
            "shellfish" -> "海鲜"
            "eggs" -> "鸡蛋"
            "soy" -> "大豆"
            else -> allergen
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.12f)
            )
            .clickable { onEditClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "健康档案",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "编辑",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 饮食类型
            if (dietTypeDisplayName != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.SetMeal,
                        contentDescription = null,
                        tint = AppleTeal,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "饮食类型",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                AssistChip(
                    onClick = { },
                    label = { Text(dietTypeDisplayName, fontSize = 12.sp) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = AppleTeal.copy(alpha = 0.1f)
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // 过敏原
            if (allergenDisplayNames.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = AppleOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "食物过敏",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allergenDisplayNames.forEach { allergen ->
                        AssistChip(
                            onClick = { },
                            label = { Text(allergen, fontSize = 12.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = AppleOrange.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // 健康状况
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.MedicalServices,
                    contentDescription = null,
                    tint = Color(0xFFE91E63),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "健康状况",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (healthConditions.isEmpty()) {
                Text(
                    text = "暂无",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    healthConditions.forEach { condition ->
                        AssistChip(
                            onClick = { },
                            label = { Text(condition, fontSize = 12.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFFE91E63).copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 饮食偏好
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "饮食偏好",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (dietaryPreferences.isEmpty()) {
                Text(
                    text = "暂无",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dietaryPreferences.forEach { pref ->
                        AssistChip(
                            onClick = { },
                            label = { Text(pref, fontSize = 12.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        }
    }
}


// ==================== 苹果风格设置区域 ====================

@Composable
private fun SettingsSection(
    onClearDataClick: () -> Unit
) {
    var showAbout by remember { mutableStateOf(false) }
    
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
        Column {
            // 关于应用
            Column {
                SettingsItem(
                    icon = Icons.Rounded.Info,
                    title = "关于应用",
                    onClick = { showAbout = !showAbout },
                    tint = ApplePurple,
                    showChevron = !showAbout
                )
                
                AnimatedVisibility(
                    visible = showAbout,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 56.dp, end = 16.dp, bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = AppleGray6
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "VISEAT 食智",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "版本 1.0.0",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppleGray1
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "智能眼镜营养识别助手，帮助您更好地管理饮食健康。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppleGray1,
                                lineHeight = 22.sp
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = AppleGray5, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // 项目信息
                            Text(
                                text = "本项目基于 Rokid Glasses 开发",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppleGray1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Spatial Joy 2025 全球 AR/AI 开发大赛作品",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppleTeal,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "清觉科技团队",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = AppleGray5, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // 免责声明
                            Row(
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = AppleOrange,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AI 可能会产生错误，识别结果仅供参考，不构成医疗或营养建议。",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppleGray2,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
            
            Divider(
                modifier = Modifier.padding(start = 56.dp),
                color = AppleGray5,
                thickness = 0.5.dp
            )
            
            // 清除数据
            SettingsItem(
                icon = Icons.Rounded.Delete,
                title = "清除本地数据",
                onClick = onClearDataClick,
                tint = AppleRed
            )
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    tint: Color = AppleBlue,
    showChevron: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface
        )
        if (showChevron) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = AppleGray3,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ==================== 预定义选项 ====================

val healthConditionOptions = listOf(
    "健康" to "未见异常",
    "糖尿病" to "需要控制糖分摄入",
    "高血压" to "需要低盐饮食",
    "高血脂" to "需要低脂饮食",
    "脂肪肝" to "需要控制脂肪摄入",
    "痛风" to "需要低嘌呤饮食",
    "胃病" to "需要清淡饮食",
    "肾病" to "需要控制蛋白质摄入",
    "心脏病" to "需要低盐低脂饮食"
)

val dietaryPreferenceOptions = listOf(
    "低油" to "减少油脂摄入",
    "低盐" to "减少钠摄入",
    "低糖" to "减少糖分摄入",
    "高蛋白" to "增加蛋白质摄入",
    "素食" to "不吃肉类",
    "无麸质" to "避免含麸质食物",
    "无乳糖" to "避免乳制品",
    "海鲜过敏" to "避免海鲜",
    "坚果过敏" to "避免坚果"
)

val genderOptions = listOf("male" to "男", "female" to "女")

val activityLevelOptions = listOf(
    "sedentary" to "久坐（几乎不运动）",
    "light" to "轻度活动（每周1-3次）",
    "moderate" to "中度活动（每周3-5次）",
    "active" to "高度活动（每周6-7次）",
    "very_active" to "极高活动（每天高强度）"
)

val healthGoalOptions = listOf(
    "lose_weight" to "减重",
    "maintain" to "维持体重",
    "gain_muscle" to "增肌"
)


// ==================== 编辑档案弹窗 ====================

// 饮食类型选项
val dietTypeOptions = listOf(
    "omnivore" to "无限制",
    "vegetarian" to "素食",
    "vegan" to "纯素",
    "low_carb" to "低碳水",
    "keto" to "生酮",
    "mediterranean" to "地中海饮食"
)

// 过敏原选项
val allergenOptions = listOf(
    "gluten" to "麸质",
    "dairy" to "乳制品",
    "nuts" to "坚果",
    "shellfish" to "海鲜",
    "eggs" to "鸡蛋",
    "soy" to "大豆"
)

@Composable
private fun EditProfileDialog(
    profile: UserProfile?,
    onSave: (UserProfile) -> Unit,
    onDismiss: () -> Unit
) {
    var nickname by remember { mutableStateOf(profile?.nickname ?: "") }
    var age by remember { mutableStateOf(profile?.age?.toString() ?: "") }
    var birthYear by remember { mutableStateOf(profile?.birthDate?.take(4) ?: "") }
    var gender by remember { mutableStateOf(profile?.gender ?: "male") }
    var height by remember { mutableStateOf(profile?.height?.toString() ?: "") }
    var weight by remember { mutableStateOf(profile?.weight?.toString() ?: "") }
    var activityLevel by remember { mutableStateOf(profile?.activityLevel ?: "moderate") }
    var healthGoal by remember { mutableStateOf(profile?.healthGoal ?: "maintain") }
    var targetWeight by remember { mutableStateOf(profile?.targetWeight?.toString() ?: "") }
    var dietType by remember { mutableStateOf(profile?.dietType ?: "omnivore") }
    var selectedAllergens by remember { mutableStateOf(profile?.allergens?.toSet() ?: emptySet()) }
    var selectedConditions by remember { mutableStateOf(profile?.healthConditions?.toSet() ?: emptySet()) }
    var selectedPreferences by remember { mutableStateOf(profile?.dietaryPreferences?.toSet() ?: emptySet()) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "编辑个人资料",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 昵称
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("昵称（可选）") },
                    placeholder = { Text("给自己起个名字") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 基本信息
                Text(
                    text = "基本信息",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // 性别选择
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    genderOptions.forEach { (value, label) ->
                        FilterChip(
                            onClick = { gender = value },
                            label = { Text(label) },
                            selected = gender == value,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = birthYear,
                        onValueChange = { 
                            birthYear = it
                            // 自动计算年龄
                            it.toIntOrNull()?.let { year ->
                                if (year in 1920..2020) {
                                    age = (java.time.LocalDate.now().year - year).toString()
                                }
                            }
                        },
                        label = { Text("出生年份") },
                        placeholder = { Text("例如: 1990") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = age,
                        onValueChange = { age = it },
                        label = { Text("年龄") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = false  // 自动计算，不可编辑
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = height,
                        onValueChange = { height = it },
                        label = { Text("身高(cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("体重(kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 活动量
                Text(
                    text = "日常活动量",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    activityLevelOptions.forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { activityLevel = value }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = activityLevel == value,
                                onClick = { activityLevel = value }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, fontSize = 14.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 健康目标
                Text(
                    text = "健康目标",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    healthGoalOptions.forEach { (value, label) ->
                        FilterChip(
                            onClick = { healthGoal = value },
                            label = { Text(label, fontSize = 12.sp) },
                            selected = healthGoal == value,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // 目标体重
                if (healthGoal != "maintain") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = targetWeight,
                        onValueChange = { targetWeight = it },
                        label = { Text("目标体重(kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 饮食类型
                Text(
                    text = "饮食类型",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dietTypeOptions.forEach { (value, label) ->
                        FilterChip(
                            onClick = { dietType = value },
                            label = { Text(label, fontSize = 12.sp) },
                            selected = dietType == value
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 过敏原
                Text(
                    text = "食物过敏（可多选）",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        onClick = { selectedAllergens = emptySet() },
                        label = { Text("无过敏", fontSize = 12.sp) },
                        selected = selectedAllergens.isEmpty()
                    )
                    allergenOptions.forEach { (value, label) ->
                        val isSelected = value in selectedAllergens
                        FilterChip(
                            onClick = {
                                selectedAllergens = if (isSelected) {
                                    selectedAllergens - value
                                } else {
                                    selectedAllergens + value
                                }
                            },
                            label = { Text(label, fontSize = 12.sp) },
                            selected = isSelected
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 健康状况
                Text(
                    text = "健康状况（可多选）",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    healthConditionOptions.forEach { (name, _) ->
                        val isSelected = name in selectedConditions
                        FilterChip(
                            onClick = {
                                selectedConditions = if (isSelected) {
                                    selectedConditions - name
                                } else {
                                    selectedConditions + name
                                }
                            },
                            label = { Text(name, fontSize = 12.sp) },
                            selected = isSelected
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 饮食偏好
                Text(
                    text = "饮食偏好（可多选）",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dietaryPreferenceOptions.forEach { (name, _) ->
                        val isSelected = name in selectedPreferences
                        FilterChip(
                            onClick = {
                                selectedPreferences = if (isSelected) {
                                    selectedPreferences - name
                                } else {
                                    selectedPreferences + name
                                }
                            },
                            label = { Text(name, fontSize = 12.sp) },
                            selected = isSelected
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val birthDate = birthYear.toIntOrNull()?.let { year ->
                                "$year-01-01"
                            }
                            val newProfile = UserProfile(
                                nickname = nickname.ifBlank { null },
                                age = age.toIntOrNull() ?: 25,
                                gender = gender,
                                birthDate = birthDate,
                                height = height.toFloatOrNull() ?: 170f,
                                weight = weight.toFloatOrNull() ?: 65f,
                                bmi = 0f,
                                activityLevel = activityLevel,
                                healthGoal = healthGoal,
                                targetWeight = if (healthGoal != "maintain") targetWeight.toFloatOrNull() else null,
                                dietType = dietType,
                                allergens = selectedAllergens.toList(),
                                healthConditions = selectedConditions.toList(),
                                dietaryPreferences = selectedPreferences.toList(),
                                isOnboardingCompleted = profile?.isOnboardingCompleted ?: false
                            )
                            onSave(newProfile)
                        },
                        enabled = height.isNotEmpty() && weight.isNotEmpty()
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}


// ==================== 营养目标设置对话框 ====================

@Composable
fun NutritionGoalsSettingsDialog(
    profile: UserProfile,
    onSave: (Int, Float, Float, Float) -> Unit,  // calories, proteinRatio, carbsRatio, fatRatio
    onDismiss: () -> Unit
) {
    val recommendedCalories = profile.calculateDailyCalories()
    
    // 根据饮食类型获取推荐比例
    val (recProtein, recCarbs, recFat) = when (profile.dietType) {
        "keto" -> Triple(0.25f, 0.05f, 0.70f)
        "low_carb" -> Triple(0.30f, 0.20f, 0.50f)
        else -> Triple(0.25f, 0.50f, 0.25f)
    }
    
    var caloriesText by remember { mutableStateOf(recommendedCalories.toString()) }
    var proteinRatio by remember { mutableStateOf(recProtein) }
    var carbsRatio by remember { mutableStateOf(recCarbs) }
    var fatRatio by remember { mutableStateOf(recFat) }
    
    val totalRatio = proteinRatio + carbsRatio + fatRatio
    val isValidRatio = kotlin.math.abs(totalRatio - 1.0f) < 0.01f
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "营养目标设置",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 热量目标
                Text(
                    text = "每日热量目标",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = caloriesText,
                    onValueChange = { caloriesText = it },
                    label = { Text("热量 (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 宏量营养素比例
                Text(
                    text = "宏量营养素比例",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // 蛋白质
                Text(text = "蛋白质: ${(proteinRatio * 100).toInt()}%", fontSize = 14.sp)
                Slider(
                    value = proteinRatio,
                    onValueChange = { proteinRatio = it },
                    valueRange = 0.1f..0.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = AppleBlue,
                        activeTrackColor = AppleBlue
                    )
                )
                
                // 碳水
                Text(text = "碳水化合物: ${(carbsRatio * 100).toInt()}%", fontSize = 14.sp)
                Slider(
                    value = carbsRatio,
                    onValueChange = { carbsRatio = it },
                    valueRange = 0.05f..0.65f,
                    colors = SliderDefaults.colors(
                        thumbColor = AppleOrange,
                        activeTrackColor = AppleOrange
                    )
                )
                
                // 脂肪
                Text(text = "脂肪: ${(fatRatio * 100).toInt()}%", fontSize = 14.sp)
                Slider(
                    value = fatRatio,
                    onValueChange = { fatRatio = it },
                    valueRange = 0.1f..0.75f,
                    colors = SliderDefaults.colors(
                        thumbColor = ApplePurple,
                        activeTrackColor = ApplePurple
                    )
                )
                
                // 比例总和提示
                val totalPercent = (totalRatio * 100).toInt()
                Text(
                    text = "总计: $totalPercent%",
                    fontSize = 14.sp,
                    color = if (isValidRatio) AppleTeal else AppleRed,
                    fontWeight = FontWeight.Medium
                )
                
                if (!isValidRatio) {
                    Text(
                        text = "比例总和必须等于 100%",
                        fontSize = 12.sp,
                        color = AppleRed
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 恢复推荐值按钮
                TextButton(
                    onClick = {
                        caloriesText = recommendedCalories.toString()
                        proteinRatio = recProtein
                        carbsRatio = recCarbs
                        fatRatio = recFat
                    }
                ) {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("恢复推荐值")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val calories = caloriesText.toIntOrNull() ?: recommendedCalories
                            onSave(calories, proteinRatio, carbsRatio, fatRatio)
                        },
                        enabled = caloriesText.isNotEmpty() && isValidRatio
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}
