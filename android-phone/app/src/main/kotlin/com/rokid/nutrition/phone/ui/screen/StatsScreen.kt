package com.rokid.nutrition.phone.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rokid.nutrition.phone.data.entity.MealSessionEntity
import com.rokid.nutrition.phone.repository.DailyStats
import com.rokid.nutrition.phone.ui.component.WeightGoalProgressCard
import com.rokid.nutrition.phone.ui.theme.*
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
 * 数据来源：
 * - todayCalories: 今日总热量（来自 DailyNutritionTracker 或数据库）
 * - todayProtein/todayCarbs/todayFat: 今日营养素（来自数据库）
 * - mealCount/snackCount 等: 食品种类统计（来自数据库）
 * - recognitionHistory: 仅用于显示今日识别记录列表（内存中的数据）
 */
@Composable
fun StatsScreen(
    todayCalories: Double,
    weeklyStats: List<DailyStats>,
    sessions: List<MealSessionEntity> = emptyList(),
    recognitionHistory: List<RecognitionHistory> = emptyList(),
    targetCalories: Double = 2000.0,
    // 从数据库获取的营养数据
    todayProtein: Double = 0.0,
    todayCarbs: Double = 0.0,
    todayFat: Double = 0.0,
    mealCount: Int = 0,
    snackCount: Int = 0,
    beverageCount: Int = 0,
    dessertCount: Int = 0,
    fruitCount: Int = 0,
    // 体重目标相关参数 (TODO: 用于 WeightGoalProgressCard，待实现)
    @Suppress("UNUSED_PARAMETER")
    currentWeight: Float? = null,
    @Suppress("UNUSED_PARAMETER")
    targetWeight: Float? = null,
    @Suppress("UNUSED_PARAMETER")
    startWeight: Float? = null,
    @Suppress("UNUSED_PARAMETER")
    healthGoal: String? = null,
    @Suppress("UNUSED_PARAMETER")
    onSessionClick: (MealSessionEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 构建今日统计数据
    val todayStats = remember(todayCalories, todayProtein, todayCarbs, todayFat, mealCount, snackCount, beverageCount, dessertCount, fruitCount, recognitionHistory) {
        // 过滤今日的识别记录
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val todayRecognitions = recognitionHistory.filter { it.timestamp >= today }
        
        TodayNutritionStats(
            totalCalories = todayCalories,
            totalProtein = todayProtein,
            totalCarbs = todayCarbs,
            totalFat = todayFat,
            mealCount = mealCount,
            snackCount = snackCount,
            beverageCount = beverageCount,
            dessertCount = dessertCount,
            fruitCount = fruitCount,
            targetCalories = targetCalories,
            recognitions = todayRecognitions
        )
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 今日热量环形进度 + 健康建议（合并显示）
        CalorieProgressCard(
            consumed = todayStats.totalCalories,
            target = todayStats.targetCalories,
            mealCount = todayStats.mealCount + todayStats.snackCount + todayStats.beverageCount,
            stats = todayStats
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
            fruitCount = todayStats.fruitCount,
            recognitions = todayStats.recognitions
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 本周用餐详情和趋势
        WeeklyMealTrendCard(
            weeklyStats = weeklyStats,
            sessions = sessions
        )
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
        
        // 遍历每个食物，按食物的 category 分别统计
        recognition.foodData.rawLlm.foods.forEach { food ->
            when (food.category?.lowercase() ?: "") {
                "meal" -> mealCount++
                "snack" -> snackCount++
                "beverage" -> beverageCount++
                "dessert" -> dessertCount++
                "fruit" -> fruitCount++
                else -> {
                    // 如果没有 category，根据食物名称或热量判断
                    val calories = food.ingredients.sumOf { it.weightG * 1.5 } // 粗略估算
                    if (calories > 200) mealCount++ else snackCount++
                }
            }
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
 * 苹果风格的热量环形进度卡片（含健康建议）
 */
@Composable
private fun CalorieProgressCard(
    consumed: Double,
    target: Double,
    mealCount: Int,
    stats: TodayNutritionStats? = null
) {
    val progress = if (target > 0) (consumed / target).coerceIn(0.0, 1.5) else 0.0
    val remaining = (target - consumed).coerceAtLeast(0.0)
    val isOverTarget = consumed > target
    
    // 生成健康建议
    val advices = remember(stats) { 
        stats?.let { generateHealthAdvices(it) } ?: emptyList() 
    }
    
    // 动画效果
    val animatedProgress by animateFloatAsState(
        targetValue = progress.toFloat(),
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "progress"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "今日热量摄入",
                style = MaterialTheme.typography.labelMedium,
                color = AppleGray1
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 环形进度图 - 苹果风格
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    consumed = consumed,
                    target = target,
                    animatedProgress = animatedProgress,
                    modifier = Modifier.fillMaxSize()
                )
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${consumed.toInt()}",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverTarget) AppleRed else AppleTeal
                    )
                    Text(
                        text = "/ ${target.toInt()} kcal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppleGray1
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 状态信息 - 苹果风格卡片
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = AppleGray6
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        icon = Icons.Rounded.Restaurant,
                        value = "$mealCount",
                        label = "餐次",
                        color = AppleTeal
                    )
                    StatItem(
                        icon = Icons.Rounded.LocalFireDepartment,
                        value = if (isOverTarget) "+${(consumed - target).toInt()}" else "${remaining.toInt()}",
                        label = if (isOverTarget) "超出" else "剩余",
                        color = if (isOverTarget) AppleRed else AppleBlue
                    )
                    StatItem(
                        icon = Icons.Rounded.TrendingUp,
                        value = "${(progress * 100).toInt()}%",
                        label = "完成度",
                        color = ApplePurple
                    )
                }
            }
            
            // 健康建议（醒目位置）
            if (advices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = AppleTeal.copy(alpha = 0.08f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Lightbulb,
                                contentDescription = null,
                                tint = AppleTeal,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "健康建议",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = AppleTeal
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        advices.forEach { advice ->
                            Row(
                                modifier = Modifier.padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .offset(y = 6.dp)
                                        .clip(CircleShape)
                                        .background(AppleTeal)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = advice,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppleTeal.copy(alpha = 0.9f),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AppleGray1
        )
    }
}

/**
 * 苹果风格的环形进度指示器
 */
@Composable
private fun CircularProgressIndicator(
    consumed: Double,
    target: Double,
    animatedProgress: Float = 0f,
    modifier: Modifier = Modifier
) {
    val progress = if (animatedProgress > 0) animatedProgress else 
        if (target > 0) (consumed / target).toFloat().coerceIn(0f, 1.5f) else 0f
    val isOverTarget = consumed > target
    
    val primaryColor = if (isOverTarget) AppleRed else AppleTeal
    val backgroundColor = AppleGray5
    
    Canvas(modifier = modifier.padding(16.dp)) {
        val strokeWidth = 16.dp.toPx()
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
        
        // 进度圆环 - 渐变色
        val sweepAngle = min(progress * 360f, 360f)
        drawArc(
            brush = Brush.sweepGradient(
                colors = if (isOverTarget) {
                    listOf(AppleOrange, AppleRed, ApplePink)
                } else {
                    listOf(AppleMint, AppleTeal, AppleBlue)
                }
            ),
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
 * 苹果风格的营养成分比例卡片
 */
@Composable
private fun MacroNutrientsCard(
    protein: Double,
    carbs: Double,
    fat: Double,
    totalCalories: Double
) {
    val proteinCalories = protein * 4
    val carbsCalories = carbs * 4
    val fatCalories = fat * 9
    val totalMacroCalories = proteinCalories + carbsCalories + fatCalories
    
    val proteinPercent = if (totalMacroCalories > 0) (proteinCalories / totalMacroCalories * 100) else 0.0
    val carbsPercent = if (totalMacroCalories > 0) (carbsCalories / totalMacroCalories * 100) else 0.0
    val fatPercent = if (totalMacroCalories > 0) (fatCalories / totalMacroCalories * 100) else 0.0
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "营养成分比例",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 比例条 - 苹果风格圆角
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(AppleGray5)
            ) {
                if (totalMacroCalories > 0) {
                    val proteinWeight = proteinPercent.toFloat().coerceAtLeast(1f)
                    val carbsWeight = carbsPercent.toFloat().coerceAtLeast(1f)
                    val fatWeight = fatPercent.toFloat().coerceAtLeast(1f)
                    
                    Box(
                        modifier = Modifier
                            .weight(proteinWeight)
                            .fillMaxHeight()
                            .background(ProteinCyan)
                    )
                    Box(
                        modifier = Modifier
                            .weight(carbsWeight)
                            .fillMaxHeight()
                            .background(CarbsAmber)
                    )
                    Box(
                        modifier = Modifier
                            .weight(fatWeight)
                            .fillMaxHeight()
                            .background(CalorieRed)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 详细数据 - 苹果风格
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MacroItem(
                    name = "蛋白质",
                    grams = protein,
                    percent = proteinPercent,
                    color = ProteinCyan,
                    icon = Icons.Rounded.FitnessCenter
                )
                MacroItem(
                    name = "碳水化合物",
                    grams = carbs,
                    percent = carbsPercent,
                    color = CarbsAmber,
                    icon = Icons.Rounded.Grain
                )
                MacroItem(
                    name = "脂肪",
                    grams = fat,
                    percent = fatPercent,
                    color = CalorieRed,
                    icon = Icons.Rounded.WaterDrop
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
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "${grams.toInt()}g",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = "${percent.toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = AppleGray1
        )
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = AppleGray2,
            textAlign = TextAlign.Center
        )
    }
}


/**
 * 食品种类数据
 */
data class CategoryFoodItem(
    val name: String,
    val calories: Double,
    val timestamp: Long
)

/**
 * 苹果风格的食品种类统计卡片（支持点击查看详情）
 */
@Composable
private fun FoodCategoryCard(
    mealCount: Int,
    snackCount: Int,
    beverageCount: Int,
    dessertCount: Int,
    fruitCount: Int,
    recognitions: List<RecognitionHistory> = emptyList()
) {
    val total = mealCount + snackCount + beverageCount + dessertCount + fruitCount
    
    // 展开的分类
    var expandedCategory by remember { mutableStateOf<String?>(null) }
    
    // 从识别历史中提取各分类的食物列表（统计出现次数）
    val categoryFoods = remember(recognitions) {
        // 先按分类收集食物名称
        val rawFoods = mutableMapOf<String, MutableList<String>>()
        recognitions.forEach { recognition ->
            recognition.foodData.rawLlm.foods.forEach { food ->
                val category = food.category?.lowercase() ?: "meal"
                val name = food.dishNameCn ?: food.dishName
                rawFoods.getOrPut(category) { mutableListOf() }.add(name)
            }
        }
        // 统计每种食物的出现次数
        val result = mutableMapOf<String, List<Pair<String, Int>>>()
        rawFoods.forEach { (category, names) ->
            val countMap = names.groupingBy { it }.eachCount()
            result[category] = countMap.entries
                .sortedByDescending { it.value }
                .map { it.key to it.value }
        }
        result
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(20.dp),
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
                    text = "食品种类统计",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AppleGray6
                ) {
                    Text(
                        text = "共 $total 项",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppleGray1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 提示文字
            Text(
                text = "点击分类查看详情",
                style = MaterialTheme.typography.labelSmall,
                color = AppleGray2
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (total == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(AppleGray6),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Category,
                                contentDescription = null,
                                tint = AppleGray3,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "今日暂无识别记录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppleGray1
                        )
                    }
                }
            } else {
                // 种类列表 - 可点击展开
                CategoryRowClickable(
                    icon = Icons.Rounded.Restaurant,
                    name = "正餐",
                    count = mealCount,
                    total = total,
                    color = AppleTeal,
                    isExpanded = expandedCategory == "meal",
                    foods = categoryFoods["meal"] ?: emptyList(),
                    onClick = { expandedCategory = if (expandedCategory == "meal") null else "meal" }
                )
                Spacer(modifier = Modifier.height(10.dp))
                CategoryRowClickable(
                    icon = Icons.Rounded.Cookie,
                    name = "零食",
                    count = snackCount,
                    total = total,
                    color = AppleOrange,
                    isExpanded = expandedCategory == "snack",
                    foods = categoryFoods["snack"] ?: emptyList(),
                    onClick = { expandedCategory = if (expandedCategory == "snack") null else "snack" }
                )
                Spacer(modifier = Modifier.height(10.dp))
                CategoryRowClickable(
                    icon = Icons.Rounded.LocalDrink,
                    name = "饮料",
                    count = beverageCount,
                    total = total,
                    color = AppleBlue,
                    isExpanded = expandedCategory == "beverage",
                    foods = categoryFoods["beverage"] ?: emptyList(),
                    onClick = { expandedCategory = if (expandedCategory == "beverage") null else "beverage" }
                )
                Spacer(modifier = Modifier.height(10.dp))
                CategoryRowClickable(
                    icon = Icons.Rounded.Cake,
                    name = "甜点",
                    count = dessertCount,
                    total = total,
                    color = ApplePink,
                    isExpanded = expandedCategory == "dessert",
                    foods = categoryFoods["dessert"] ?: emptyList(),
                    onClick = { expandedCategory = if (expandedCategory == "dessert") null else "dessert" }
                )
                Spacer(modifier = Modifier.height(10.dp))
                CategoryRowClickable(
                    icon = Icons.Rounded.Eco,
                    name = "水果",
                    count = fruitCount,
                    total = total,
                    color = AppleTeal.copy(green = 0.8f),
                    isExpanded = expandedCategory == "fruit",
                    foods = categoryFoods["fruit"] ?: emptyList(),
                    onClick = { expandedCategory = if (expandedCategory == "fruit") null else "fruit" }
                )
            }
        }
    }
}

@Composable
private fun CategoryRowClickable(
    icon: ImageVector,
    name: String,
    count: Int,
    total: Int,
    color: Color,
    isExpanded: Boolean,
    foods: List<Pair<String, Int>> = emptyList(),  // 食物名称 to 出现次数
    onClick: () -> Unit
) {
    val percent = if (total > 0) count.toFloat() / total else 0f
    
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable(enabled = count > 0) { onClick() }
                .background(if (isExpanded) color.copy(alpha = 0.08f) else Color.Transparent)
                .padding(vertical = 6.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标 - 苹果风格
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isExpanded) FontWeight.Medium else FontWeight.Normal,
                modifier = Modifier.width(48.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(12.dp))
            
            // 进度条 - 苹果风格
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(AppleGray5)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(percent)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.width(28.dp),
                textAlign = TextAlign.End
            )
            
            // 展开指示器
            if (count > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = AppleGray3,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        // 展开的详情 - 显示食物列表
        AnimatedVisibility(
            visible = isExpanded && count > 0,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 44.dp, top = 8.dp),
                shape = RoundedCornerShape(10.dp),
                color = AppleGray6
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (foods.isEmpty()) {
                        Text(
                            text = "该分类下共 $count 项食物",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppleGray1
                        )
                    } else {
                        foods.take(8).forEachIndexed { index, (foodName, foodCount) ->
                            if (index > 0) {
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = foodName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                // 显示出现次数
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = color.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "×$foodCount",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = color,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        if (foods.size > 8) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "还有 ${foods.size - 8} 种...",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppleGray2
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 苹果风格的健康建议卡片
 */
@Composable
private fun HealthAdviceCard(stats: TodayNutritionStats) {
    val advices = remember(stats) { generateHealthAdvices(stats) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 0.dp,
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppleTeal.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppleTeal.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lightbulb,
                        contentDescription = null,
                        tint = AppleTeal,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "健康建议",
                    style = MaterialTheme.typography.headlineSmall,
                    color = AppleTeal
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            advices.forEachIndexed { index, advice ->
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .offset(y = 6.dp)
                            .clip(CircleShape)
                            .background(AppleTeal)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = advice,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppleTeal.copy(alpha = 0.9f),
                        lineHeight = 22.sp
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
 * 本周用餐详情和趋势卡片
 */
@Composable
private fun WeeklyMealTrendCard(
    weeklyStats: List<DailyStats>,
    sessions: List<MealSessionEntity>
) {
    var useMockData by remember { mutableStateOf(false) }
    
    // 生成模拟数据
    val mockWeeklyData = remember {
        listOf(
            WeeklyMealData("周一", 1850, 3, listOf("煎蛋", "牛肉面", "清蒸鱼")),
            WeeklyMealData("周二", 2100, 4, listOf("豆浆油条", "宫保鸡丁", "水果沙拉", "红烧肉")),
            WeeklyMealData("周三", 1920, 3, listOf("三明治", "麻婆豆腐", "蔬菜汤")),
            WeeklyMealData("周四", 2050, 4, listOf("牛奶麦片", "糖醋排骨", "凉拌黄瓜", "炒饭")),
            WeeklyMealData("周五", 1780, 3, listOf("包子", "鱼香肉丝", "番茄蛋汤")),
            WeeklyMealData("周六", 2200, 5, listOf("煎饼", "火锅", "烤肉", "甜点", "奶茶")),
            WeeklyMealData("周日", 1950, 3, listOf("粥", "红烧牛肉", "炒青菜"))
        )
    }
    
    // 从真实数据生成本周数据
    val realWeeklyData = remember(weeklyStats, sessions) {
        val dayNames = listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
        val calendar = Calendar.getInstance()
        
        weeklyStats.mapIndexed { index, stats ->
            val dayOfWeek = calendar.apply { 
                timeInMillis = System.currentTimeMillis() - (6 - index) * 24 * 60 * 60 * 1000L
            }.get(Calendar.DAY_OF_WEEK) - 1
            
            // 获取当天的餐品名称
            val dayStart = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis
            val dayEnd = dayStart + 24 * 60 * 60 * 1000L
            
            val dayMeals = sessions.filter { it.startTime in dayStart until dayEnd }
                .mapNotNull { it.mealType?.let { type ->
                    when (type) {
                        "breakfast" -> "早餐"
                        "lunch" -> "午餐"
                        "dinner" -> "晚餐"
                        "snack" -> "加餐"
                        else -> null
                    }
                } }
            
            WeeklyMealData(
                dayName = dayNames[dayOfWeek],
                calories = stats.totalCalories.toInt(),
                mealCount = dayMeals.size.coerceAtLeast(if (stats.totalCalories > 0) 1 else 0),
                meals = dayMeals.ifEmpty { if (stats.totalCalories > 0) listOf("用餐记录") else emptyList() }
            )
        }
    }
    
    val displayData = if (useMockData) mockWeeklyData else realWeeklyData
    val maxCalories = displayData.maxOfOrNull { it.calories } ?: 2000
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "本周用餐趋势",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // 模拟/真实数据切换
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (useMockData) AppleOrange.copy(alpha = 0.12f) else AppleTeal.copy(alpha = 0.12f),
                    onClick = { useMockData = !useMockData }
                ) {
                    Text(
                        text = if (useMockData) "模拟" else "真实",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = if (useMockData) AppleOrange else AppleTeal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 热量趋势图
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                displayData.forEach { data ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 热量数值
                        Text(
                            text = "${data.calories}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppleGray1,
                            fontSize = 10.sp
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // 柱状图
                        val barHeight = if (maxCalories > 0) (data.calories.toFloat() / maxCalories * 80).dp else 0.dp
                        val barColor = when {
                            data.calories > 2200 -> AppleRed
                            data.calories > 1800 -> AppleTeal
                            data.calories > 0 -> AppleOrange
                            else -> AppleGray5
                        }
                        
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(barHeight.coerceAtLeast(4.dp))
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            barColor,
                                            barColor.copy(alpha = 0.6f)
                                        )
                                    )
                                )
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // 星期
                        Text(
                            text = data.dayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = AppleGray1,
                            fontSize = 11.sp
                        )
                        
                        // 餐次
                        Text(
                            text = "${data.mealCount}餐",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppleGray2,
                            fontSize = 9.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Divider(color = AppleGray5, thickness = 0.5.dp)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 本周统计摘要
            val totalCalories = displayData.sumOf { it.calories }
            val avgCalories = if (displayData.isNotEmpty()) totalCalories / displayData.size else 0
            val totalMeals = displayData.sumOf { it.mealCount }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                WeeklySummaryItem(
                    icon = Icons.Rounded.LocalFireDepartment,
                    label = "总热量",
                    value = "$totalCalories",
                    unit = "kcal",
                    color = CalorieRed
                )
                WeeklySummaryItem(
                    icon = Icons.Rounded.TrendingUp,
                    label = "日均",
                    value = "$avgCalories",
                    unit = "kcal",
                    color = AppleTeal
                )
                WeeklySummaryItem(
                    icon = Icons.Rounded.Restaurant,
                    label = "总餐次",
                    value = "$totalMeals",
                    unit = "餐",
                    color = AppleBlue
                )
            }
        }
    }
}

/**
 * 本周用餐数据
 */
private data class WeeklyMealData(
    val dayName: String,
    val calories: Int,
    val mealCount: Int,
    val meals: List<String>
)

/**
 * 周统计摘要项
 */
@Composable
private fun WeeklySummaryItem(
    icon: ImageVector,
    label: String,
    value: String,
    unit: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = AppleGray1
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AppleGray2
        )
    }
}
