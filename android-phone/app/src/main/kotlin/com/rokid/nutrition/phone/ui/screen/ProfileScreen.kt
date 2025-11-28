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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    onClearData: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    
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
        
        // 3. 健康档案
        HealthProfileCard(
            healthConditions = profile?.healthConditions ?: emptyList(),
            dietaryPreferences = profile?.dietaryPreferences ?: emptyList(),
            onEditClick = { showEditDialog = true }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 4. 设置区域
        SettingsSection(
            onDebugLogClick = onOpenDebugLog,
            onClearDataClick = { showClearDataDialog = true }
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


// ==================== 用户信息头部卡片 ====================

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
            .clickable { onEditClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(20.dp)
        ) {
            if (profile != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 头像
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (profile.gender == "female") Icons.Default.Face else Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        // 昵称
                        Text(
                            text = profile.nickname ?: "用户",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = Color.White
                        )
                        
                        // 基本信息
                        Text(
                            text = "$genderText · ${profile.age}岁 · ${profile.height.toInt()}cm · ${profile.weight.toInt()}kg",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // BMI 指示器
                        if (profile.bmi > 0) {
                            val bmiStatus = getBMIStatus(profile.bmi)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "BMI ${String.format("%.1f", profile.bmi)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = bmiStatus.color
                                ) {
                                    Text(
                                        text = bmiStatus.label,
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    // 健康目标徽章
                    Column(horizontalAlignment = Alignment.End) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = goalText,
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                        
                        // 目标体重差值
                        if (profile.targetWeight != null && profile.healthGoal == "lose_weight") {
                            val diff = profile.weight - profile.targetWeight!!
                            if (diff > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "还需减 ${diff.toInt()}kg",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "编辑",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                // 空状态
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "点击设置个人信息",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "完善资料获取个性化建议",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}


// ==================== 今日营养进度卡片 ====================

@Composable
private fun NutritionProgressCard(
    currentCalories: Double,
    targetCalories: Int,
    mealCount: Int,
    onClick: () -> Unit
) {
    val progress = if (targetCalories > 0) (currentCalories / targetCalories).coerceIn(0.0, 1.5) else 0.0
    val progressColor = getProgressColor(currentCalories, targetCalories)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 圆形进度条
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(100.dp)) {
                    val strokeWidth = 10.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val center = Offset(size.width / 2, size.height / 2)
                    
                    // 背景圆环
                    drawCircle(
                        color = Color.Gray.copy(alpha = 0.2f),
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
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = progressColor
                    )
                    Text(
                        text = "/ $targetCalories",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(20.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "今日热量",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = when {
                        progress >= 1.0 -> "已超出目标 ${((progress - 1) * 100).toInt()}%"
                        progress >= 0.8 -> "接近目标，注意控制"
                        else -> "继续保持"
                    },
                    fontSize = 14.sp,
                    color = progressColor
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 用餐次数
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "今日用餐 $mealCount 次",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}

// ==================== 健康档案卡片 ====================

@Composable
private fun HealthProfileCard(
    healthConditions: List<String>,
    dietaryPreferences: List<String>,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditClick() }
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


// ==================== 设置区域 ====================

@Composable
private fun SettingsSection(
    onDebugLogClick: () -> Unit,
    onClearDataClick: () -> Unit
) {
    var showAbout by remember { mutableStateOf(false) }
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            // 调试日志
            SettingsItem(
                icon = Icons.Default.BugReport,
                title = "调试日志",
                onClick = onDebugLogClick
            )
            
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            
            // 关于应用
            Column {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "关于应用",
                    onClick = { showAbout = !showAbout },
                    showChevron = !showAbout
                )
                
                AnimatedVisibility(
                    visible = showAbout,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 56.dp, end = 16.dp, bottom = 16.dp)
                    ) {
                        Text(
                            text = "VISEAT 食智",
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "版本 1.0.0",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "智能眼镜营养识别助手，帮助您更好地管理饮食健康。",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
            
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            
            // 清除数据
            SettingsItem(
                icon = Icons.Default.Delete,
                title = "清除本地数据",
                onClick = onClearDataClick,
                tint = Color(0xFFFF5722)
            )
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.primary,
    showChevron: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        if (showChevron) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}

// ==================== 预定义选项 ====================

val healthConditionOptions = listOf(
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

@Composable
private fun EditProfileDialog(
    profile: UserProfile?,
    onSave: (UserProfile) -> Unit,
    onDismiss: () -> Unit
) {
    var nickname by remember { mutableStateOf(profile?.nickname ?: "") }
    var age by remember { mutableStateOf(profile?.age?.toString() ?: "") }
    var gender by remember { mutableStateOf(profile?.gender ?: "male") }
    var height by remember { mutableStateOf(profile?.height?.toString() ?: "") }
    var weight by remember { mutableStateOf(profile?.weight?.toString() ?: "") }
    var activityLevel by remember { mutableStateOf(profile?.activityLevel ?: "moderate") }
    var healthGoal by remember { mutableStateOf(profile?.healthGoal ?: "maintain") }
    var targetWeight by remember { mutableStateOf(profile?.targetWeight?.toString() ?: "") }
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
                
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("年龄") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
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
                            val newProfile = UserProfile(
                                nickname = nickname.ifBlank { null },
                                age = age.toIntOrNull() ?: 25,
                                gender = gender,
                                height = height.toFloatOrNull() ?: 170f,
                                weight = weight.toFloatOrNull() ?: 65f,
                                bmi = 0f,
                                activityLevel = activityLevel,
                                healthGoal = healthGoal,
                                targetWeight = if (healthGoal != "maintain") targetWeight.toFloatOrNull() else null,
                                healthConditions = selectedConditions.toList(),
                                dietaryPreferences = selectedPreferences.toList()
                            )
                            onSave(newProfile)
                        },
                        enabled = age.isNotEmpty() && height.isNotEmpty() && weight.isNotEmpty()
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}
