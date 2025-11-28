package com.rokid.nutrition.phone.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rokid.nutrition.phone.data.entity.MealSessionEntity
import com.rokid.nutrition.phone.repository.DailyStats
import com.rokid.nutrition.phone.ui.viewmodel.RecognitionHistory
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

/**
 * 今日营养统计数据
 */
data class TodayNutritionStats(
    val totalCalories: Double = 0.0,
    val totalProtein: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val totalFat: Double = 0.0,
    val mealCount: Int = 0,
    val snackCount: Int = 0,
    val beverageCount: Int = 0,
    val dessertCount: Int = 0,
    val fruitCount: Int = 0,
    val targetCalories: Double = 2000.0,  // 默认目标热量
    val recognitions: List<RecognitionHistory> = emptyList()
)

/**
 * 统计页面 - 高级版
 * 
 * 注意：todayCalories 参数现在作为主要数据源，与"我的"页面保持一致
 * recognitionHistory 仅用于显示今日识别记录列表
 */
@Composable
fun StatsScreen(
    todayCalories: Double,
    weeklyStats: List<DailyStats>,
    sessions: List<MealSessionEntity> = emptyList(),
    recognitionHistory: List<RecognitionHistory> = emptyList(),
    targetCalories: Double = 2000.0,
    onSessionClick: (MealSessionEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 计算今日统计数据 - 使用 todayCalories 作为主要热量数据源
    // recognitionHistory 仅用于计算营养素比例和食品种类
    val todayStats = remember(recognitionHistory, todayCalories) {
        calculateTodayStatsWithCalories(recognitionHistory, targetCalories, todayCalories)
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 今日热量环形进度
        CalorieProgressCard(
            consumed = todayStats.totalCalories,
            target = todayStats.targetCalories,
            mealCount = todayStats.mealCount + todayStats.snackCount + todayStats.beverageCount
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 营养成分比例
        MacroNutrientsCard(
            protein = todayStats.totalProtein,
            carbs = todayStats.totalCarbs,
            fat = todayStats.totalFat,
            totalCalories = todayStats.totalCalories
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 食品种类统计
        FoodCategoryCard(
            mealCount = todayStats.mealCount,
            snackCount = todayStats.snackCount,
            beverageCount = todayStats.beverageCount,
            dessertCount = todayStats.dessertCount,
            fruitCount = todayStats.fruitCount
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 健康建议
        HealthAdviceCard(stats = todayStats)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 今日识别记录
        TodayRecognitionsCard(recognitions = todayStats.recognitions)
    }
}

/**
 * 计算今日统计数据（使用传入的热量值作为主数据源）
 * 
 * @param recognitionHistory 识别历史（用于计算营养素比例和食品种类）
 * @param targetCalories 目标热量
 * @param actualTodayCalories 实际今日热量（来自数据库/DailyNutritionTracker）
 */
private fun calculateTodayStatsWithCalories(
    recognitionHistory: List<RecognitionHistory>,
    targetCalories: Double,
    actualTodayCalories: Double
): TodayNutritionStats {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    
    val todayRecognitions = recognitionHistory.filter { it.timestamp >= today }
    
    // 从识别历史计算营养素比例（用于显示比例条）
    var historyProtein = 0.0
    var historyCarbs = 0.0
    var historyFat = 0.0
    var historyCalories = 0.0
    var mealCount = 0
    var snackCount = 0
    var beverageCount = 0
    var dessertCount = 0
    var fruitCount = 0
    
    todayRecognitions.forEach { recognition ->
        val nutrition = recognition.foodData.snapshot.nutrition
        historyCalories += nutrition.calories
        historyProtein += nutrition.protein
        historyCarbs += nutrition.carbs
        historyFat += nutrition.fat
        
        when (recognition.category.lowercase()) {
            "meal" -> mealCount++
            "snack" -> snackCount++
            "beverage" -> beverageCount++
            "dessert" -> dessertCount++
            "fruit" -> fruitCount++
        }
    }
    
    // 如果有识别历史，按比例调整营养素（基于实际热量）
    val scaleFactor = if (historyCalories > 0 && actualTodayCalories > 0) {
        actualTodayCalories / historyCalories
    } else {
        1.0
    }
    
    return TodayNutritionStats(
        totalCalories = actualTodayCalories,  // 使用实际热量值
        totalProtein = historyProtein * scaleFactor,
        totalCarbs = historyCarbs * scaleFactor,
        totalFat = historyFat * scaleFactor,
        mealCount = mealCount,
        snackCount = snackCount,
        beverageCount = beverageCount,
        dessertCount = dessertCount,
        fruitCount = fruitCount,
        targetCalories = targetCalories,
        recognitions = todayRecognitions
    )
}

/**
 * 计算今日统计数据（旧版本，保留兼容性）
 */
private fun calculateTodayStats(
    recognitionHistory: List<RecognitionHistory>,
    targetCalories: Double
): TodayNutritionStats {
    return calculateTodayStatsWithCalories(recognitionHistory, targetCalories, 0.0)
}

/**
 * 热量环形进度卡片
 */
@Composable
private fun CalorieProgressCard(
    consumed: Double,
    target: Double,
    mealCount: Int
) {
    val progress = if (target > 0) (consumed / target).coerceIn(0.0, 1.5) else 0.0
    val remaining = (target - consumed).coerceAtLeast(0.0)
    val isOverTarget = consumed > target
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "今日热量摄入",
                fontSize = 14.sp,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 环形进度图
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    consumed = consumed,
                    target = target,
                    modifier = Modifier.fillMaxSize()
                )
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${consumed.toInt()}",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverTarget) Color(0xFFFF5722) else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "/ ${target.toInt()} kcal",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 状态信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Default.Restaurant,
                    value = "$mealCount",
                    label = "餐次",
                    color = Color(0xFF4CAF50)
                )
                StatItem(
                    icon = Icons.Default.LocalFireDepartment,
                    value = if (isOverTarget) "+${(consumed - target).toInt()}" else "${remaining.toInt()}",
                    label = if (isOverTarget) "超出" else "剩余",
                    color = if (isOverTarget) Color(0xFFFF5722) else Color(0xFF2196F3)
                )
                StatItem(
                    icon = Icons.Default.TrendingUp,
                    value = "${(progress * 100).toInt()}%",
                    label = "完成度",
                    color = Color(0xFF9C27B0)
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

/**
 * 自定义环形进度指示器
 */
@Composable
private fun CircularProgressIndicator(
    consumed: Double,
    target: Double,
    modifier: Modifier = Modifier
) {
    val progress = if (target > 0) (consumed / target).toFloat().coerceIn(0f, 1.5f) else 0f
    val isOverTarget = consumed > target
    
    val primaryColor = if (isOverTarget) Color(0xFFFF5722) else Color(0xFF4CAF50)
    val backgroundColor = Color.Gray.copy(alpha = 0.2f)
    
    Canvas(modifier = modifier.padding(16.dp)) {
        val strokeWidth = 20.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        val center = Offset(size.width / 2, size.height / 2)
        
        // 背景圆环
        drawArc(
            color = backgroundColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        
        // 进度圆环
        val sweepAngle = min(progress * 360f, 360f)
        drawArc(
            color = primaryColor,
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

/**
 * 营养成分比例卡片
 */
@Composable
private fun MacroNutrientsCard(
    protein: Double,
    carbs: Double,
    fat: Double,
    totalCalories: Double
) {
    // 计算各营养素提供的热量
    val proteinCalories = protein * 4  // 1g 蛋白质 = 4 kcal
    val carbsCalories = carbs * 4      // 1g 碳水 = 4 kcal
    val fatCalories = fat * 9          // 1g 脂肪 = 9 kcal
    val totalMacroCalories = proteinCalories + carbsCalories + fatCalories
    
    val proteinPercent = if (totalMacroCalories > 0) (proteinCalories / totalMacroCalories * 100) else 0.0
    val carbsPercent = if (totalMacroCalories > 0) (carbsCalories / totalMacroCalories * 100) else 0.0
    val fatPercent = if (totalMacroCalories > 0) (fatCalories / totalMacroCalories * 100) else 0.0
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "营养成分比例",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 比例条
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
            ) {
                if (totalMacroCalories > 0) {
                    Box(
                        modifier = Modifier
                            .weight(proteinPercent.toFloat().coerceAtLeast(0.1f))
                            .fillMaxHeight()
                            .background(Color(0xFF4ECDC4))
                    )
                    Box(
                        modifier = Modifier
                            .weight(carbsPercent.toFloat().coerceAtLeast(0.1f))
                            .fillMaxHeight()
                            .background(Color(0xFFFFE66D))
                    )
                    Box(
                        modifier = Modifier
                            .weight(fatPercent.toFloat().coerceAtLeast(0.1f))
                            .fillMaxHeight()
                            .background(Color(0xFFFF6B6B))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .background(Color.Gray.copy(alpha = 0.3f))
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 详细数据
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MacroItem(
                    name = "蛋白质",
                    grams = protein,
                    percent = proteinPercent,
                    color = Color(0xFF4ECDC4),
                    icon = Icons.Default.FitnessCenter
                )
                MacroItem(
                    name = "碳水化合物",
                    grams = carbs,
                    percent = carbsPercent,
                    color = Color(0xFFFFE66D),
                    icon = Icons.Default.Grain
                )
                MacroItem(
                    name = "脂肪",
                    grams = fat,
                    percent = fatPercent,
                    color = Color(0xFFFF6B6B),
                    icon = Icons.Default.WaterDrop
                )
            }
        }
    }
}

@Composable
private fun MacroItem(
    name: String,
    grams: Double,
    percent: Double,
    color: Color,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
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
        Text(
            text = "${grams.toInt()}g",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = "${percent.toInt()}%",
            fontSize = 12.sp,
            color = Color.Gray
        )
        Text(
            text = name,
            fontSize = 11.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}


/**
 * 食品种类统计卡片
 */
@Composable
private fun FoodCategoryCard(
    mealCount: Int,
    snackCount: Int,
    beverageCount: Int,
    dessertCount: Int,
    fruitCount: Int
) {
    val total = mealCount + snackCount + beverageCount + dessertCount + fruitCount
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    text = "食品种类统计",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "共 $total 次识别",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (total == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "今日暂无识别记录",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                // 种类列表
                CategoryRow(
                    icon = Icons.Default.Restaurant,
                    name = "正餐",
                    count = mealCount,
                    total = total,
                    color = Color(0xFF4CAF50)
                )
                Spacer(modifier = Modifier.height(12.dp))
                CategoryRow(
                    icon = Icons.Default.Cookie,
                    name = "零食",
                    count = snackCount,
                    total = total,
                    color = Color(0xFFFF9800)
                )
                Spacer(modifier = Modifier.height(12.dp))
                CategoryRow(
                    icon = Icons.Default.LocalDrink,
                    name = "饮料",
                    count = beverageCount,
                    total = total,
                    color = Color(0xFF2196F3)
                )
                Spacer(modifier = Modifier.height(12.dp))
                CategoryRow(
                    icon = Icons.Default.Cake,
                    name = "甜点",
                    count = dessertCount,
                    total = total,
                    color = Color(0xFFE91E63)
                )
                Spacer(modifier = Modifier.height(12.dp))
                CategoryRow(
                    icon = Icons.Default.Eco,
                    name = "水果",
                    count = fruitCount,
                    total = total,
                    color = Color(0xFF8BC34A)
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(
    icon: ImageVector,
    name: String,
    count: Int,
    total: Int,
    color: Color
) {
    val percent = if (total > 0) count.toFloat() / total else 0f
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = name,
            fontSize = 14.sp,
            modifier = Modifier.width(48.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        
        // 进度条
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Gray.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percent)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$count",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.width(24.dp),
            textAlign = TextAlign.End
        )
    }
}

/**
 * 健康建议卡片
 */
@Composable
private fun HealthAdviceCard(stats: TodayNutritionStats) {
    val advices = remember(stats) { generateHealthAdvices(stats) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "健康建议",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            advices.forEach { advice ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "•",
                        fontSize = 14.sp,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = advice,
                        fontSize = 14.sp,
                        color = Color(0xFF2E7D32),
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

/**
 * 生成健康建议
 */
private fun generateHealthAdvices(stats: TodayNutritionStats): List<String> {
    val advices = mutableListOf<String>()
    
    // 热量建议
    val caloriePercent = stats.totalCalories / stats.targetCalories
    when {
        stats.totalCalories == 0.0 -> {
            advices.add("今天还没有记录饮食，记得按时吃饭哦！")
        }
        caloriePercent < 0.5 -> {
            advices.add("今日热量摄入偏低，注意补充能量。")
        }
        caloriePercent > 1.2 -> {
            advices.add("今日热量已超标，建议适当运动消耗。")
        }
        caloriePercent in 0.8..1.1 -> {
            advices.add("今日热量摄入适中，继续保持！")
        }
    }
    
    // 营养素建议
    val totalMacroCalories = stats.totalProtein * 4 + stats.totalCarbs * 4 + stats.totalFat * 9
    if (totalMacroCalories > 0) {
        val proteinPercent = stats.totalProtein * 4 / totalMacroCalories
        val fatPercent = stats.totalFat * 9 / totalMacroCalories
        
        if (proteinPercent < 0.15) {
            advices.add("蛋白质摄入偏低，建议增加鸡蛋、鱼肉等优质蛋白。")
        }
        if (fatPercent > 0.35) {
            advices.add("脂肪摄入偏高，建议减少油炸食品。")
        }
    }
    
    // 食品种类建议
    if (stats.snackCount > stats.mealCount && stats.mealCount > 0) {
        advices.add("零食摄入较多，建议以正餐为主。")
    }
    if (stats.fruitCount == 0 && stats.mealCount > 0) {
        advices.add("今天还没吃水果，建议补充维生素。")
    }
    
    if (advices.isEmpty()) {
        advices.add("饮食均衡，继续保持健康的生活方式！")
    }
    
    return advices.take(3)  // 最多显示3条建议
}

/**
 * 今日识别记录卡片
 */
@Composable
private fun TodayRecognitionsCard(recognitions: List<RecognitionHistory>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "今日识别记录",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (recognitions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "今日暂无识别记录",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                recognitions.forEachIndexed { index, recognition ->
                    RecognitionItem(recognition = recognition)
                    if (index < recognitions.size - 1) {
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

@Composable
private fun RecognitionItem(recognition: RecognitionHistory) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val categoryColor = Color(recognition.getCategoryColor())
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 时间
        Text(
            text = timeFormat.format(Date(recognition.timestamp)),
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.width(48.dp)
        )
        
        // 分类标签
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(categoryColor.copy(alpha = 0.15f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = recognition.getCategoryText(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = categoryColor
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // 食物名称
        Text(
            text = recognition.foodName,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        
        // 热量
        Text(
            text = "${recognition.totalCalories.toInt()} kcal",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF6B6B)
        )
    }
}
