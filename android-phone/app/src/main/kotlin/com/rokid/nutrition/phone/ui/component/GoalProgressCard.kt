package com.rokid.nutrition.phone.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rokid.nutrition.phone.ui.theme.*
import com.rokid.nutrition.phone.ui.util.*
import java.util.Calendar
import kotlin.math.abs

// ==================== 目标进度卡片 ====================

/**
 * 健康目标进度卡片
 * 显示当前体重、目标体重、进度百分比和预计完成日期
 */
@Composable
fun GoalProgressCard(
    currentWeight: Float,
    targetWeight: Float,
    startWeight: Float,
    healthGoal: String?,
    weeklyWeightChange: Float = 0f,  // 每周体重变化（用于预测）
    onEditClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val goalType = HealthGoalType.fromKey(healthGoal)
    val progress = calculateWeightGoalProgress(startWeight, currentWeight, targetWeight, healthGoal)
    
    // 计算剩余体重
    val remainingWeight = abs(targetWeight - currentWeight)
    
    // 预计完成日期
    val estimatedDays = if (weeklyWeightChange != 0f && remainingWeight > 0) {
        (remainingWeight / abs(weeklyWeightChange) * 7).toInt()
    } else null
    
    val estimatedDate = estimatedDays?.let {
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, it)
        }
    }
    
    // 动画进度
    val animatedProgress by animateFloatAsState(
        targetValue = progress / 100f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "progress"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = goalType.color.copy(alpha = 0.1f),
                spotColor = goalType.color.copy(alpha = 0.2f)
            )
            .clickable { onEditClick() },
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(goalType.color.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = goalType.icon,
                            contentDescription = null,
                            tint = goalType.color,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${goalType.displayName}目标",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = AppleGray3,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 进度环和数据
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 进度环
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
                            color = AppleGray5,
                            radius = radius,
                            center = center,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        
                        // 进度圆环
                        val sweepAngle = animatedProgress * 360f
                        drawArc(
                            color = goalType.color,
                            startAngle = -90f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    
                    // 中心进度文字
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${progress.toInt()}%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = goalType.color
                        )
                        Text(
                            text = "完成",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppleGray1
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(20.dp))
                
                // 体重数据
                Column(modifier = Modifier.weight(1f)) {
                    // 当前体重
                    WeightDataRow(
                        label = "当前",
                        value = "${currentWeight.toInt()}",
                        unit = "kg",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 目标体重
                    WeightDataRow(
                        label = "目标",
                        value = "${targetWeight.toInt()}",
                        unit = "kg",
                        color = goalType.color
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 剩余
                    WeightDataRow(
                        label = "剩余",
                        value = String.format("%.1f", remainingWeight),
                        unit = "kg",
                        color = AppleGray1
                    )
                }
            }
            
            // 预计完成日期
            if (estimatedDate != null && remainingWeight > 0.5f) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = goalType.color.copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = goalType.color,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "预计 ${estimatedDate.get(Calendar.MONTH) + 1}月${estimatedDate.get(Calendar.DAY_OF_MONTH)}日 达成目标",
                            style = MaterialTheme.typography.bodySmall,
                            color = goalType.color.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "约${estimatedDays}天",
                            style = MaterialTheme.typography.labelSmall,
                            color = goalType.color.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            // 已完成提示
            if (progress >= 100f) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AppleTeal.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = AppleTeal,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "恭喜！你已达成${goalType.displayName}目标！",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = AppleTeal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeightDataRow(
    label: String,
    value: String,
    unit: String,
    color: Color
) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = AppleGray1,
            modifier = Modifier.width(40.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = unit,
            style = MaterialTheme.typography.bodySmall,
            color = AppleGray1
        )
    }
}

// ==================== StatsScreen 体重目标进度卡片 ====================

/**
 * 体重目标进度卡片（用于 StatsScreen）
 * 显示体重变化趋势和预计目标完成
 */
@Composable
fun WeightGoalProgressCard(
    currentWeight: Float,
    targetWeight: Float,
    startWeight: Float,
    healthGoal: String?,
    weightHistory: List<Pair<Long, Float>> = emptyList(),  // 时间戳和体重
    modifier: Modifier = Modifier
) {
    if (targetWeight <= 0) return
    
    val goalType = HealthGoalType.fromKey(healthGoal)
    val progress = calculateWeightGoalProgress(startWeight, currentWeight, targetWeight, healthGoal)
    val weightChange = currentWeight - startWeight
    val isPositiveChange = when (healthGoal) {
        "lose_weight" -> weightChange < 0
        "gain_muscle" -> weightChange > 0
        else -> abs(weightChange) < startWeight * 0.02f
    }
    
    Card(
        modifier = modifier
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
            // 标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${goalType.displayName}进度",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isPositiveChange) AppleTeal.copy(alpha = 0.1f) else AppleOrange.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (weightChange < 0) Icons.Rounded.TrendingDown else Icons.Rounded.TrendingUp,
                            contentDescription = null,
                            tint = if (isPositiveChange) AppleTeal else AppleOrange,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${if (weightChange >= 0) "+" else ""}${String.format("%.1f", weightChange)}kg",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isPositiveChange) AppleTeal else AppleOrange
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 进度条
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${startWeight.toInt()}kg",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppleGray1
                    )
                    Text(
                        text = "${targetWeight.toInt()}kg",
                        style = MaterialTheme.typography.labelSmall,
                        color = goalType.color
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(AppleGray5)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = (progress / 100f).coerceIn(0f, 1f))
                            .background(goalType.color)
                    )
                    
                    // 当前位置标记
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .offset(x = ((progress / 100f).coerceIn(0f, 1f) * 300).dp - 6.dp)
                            .width(12.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = "当前 ${currentWeight}kg",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 统计数据
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(
                    label = "完成度",
                    value = "${progress.toInt()}%",
                    color = goalType.color
                )
                StatColumn(
                    label = "已变化",
                    value = "${String.format("%.1f", abs(weightChange))}kg",
                    color = if (isPositiveChange) AppleTeal else AppleOrange
                )
                StatColumn(
                    label = "剩余",
                    value = "${String.format("%.1f", abs(targetWeight - currentWeight))}kg",
                    color = AppleGray1
                )
            }
        }
    }
}

@Composable
private fun StatColumn(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
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
