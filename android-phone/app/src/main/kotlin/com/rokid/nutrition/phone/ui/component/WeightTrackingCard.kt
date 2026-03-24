@file:OptIn(ExperimentalMaterial3Api::class)

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
import com.rokid.nutrition.phone.data.entity.WeightEntryEntity
import com.rokid.nutrition.phone.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 体重追踪卡片
 * 
 * 显示当前体重、目标体重和进度
 */
@Composable
fun WeightTrackingCard(
    currentWeight: Float?,
    targetWeight: Float?,
    startWeight: Float?,
    latestEntry: WeightEntryEntity?,
    onAddWeight: () -> Unit,
    onViewHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 计算进度
    val progress = if (currentWeight != null && targetWeight != null && startWeight != null) {
        val totalChange = startWeight - targetWeight
        val currentChange = startWeight - currentWeight
        if (totalChange != 0f) (currentChange / totalChange).coerceIn(0f, 1f) else 0f
    } else 0f
    
    val progressColor = when {
        progress >= 1f -> AppleTeal
        progress >= 0.7f -> AppleBlue
        progress >= 0.3f -> AppleOrange
        else -> AppleGray2
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            )
            .clickable { onViewHistory() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 标题栏
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
                            .background(AppleBlue.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.MonitorWeight,
                            contentDescription = null,
                            tint = AppleBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "体重追踪",
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (currentWeight != null) {
                // 有体重数据时显示
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 进度环
                    if (targetWeight != null && startWeight != null) {
                        WeightProgressRing(
                            progress = progress,
                            color = progressColor,
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        // 当前体重
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = String.format("%.1f", currentWeight),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "kg",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppleGray1,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        
                        // 目标体重
                        if (targetWeight != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "目标 ${String.format("%.1f", targetWeight)} kg",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppleGray1
                            )
                            
                            // 差值
                            val diff = currentWeight - targetWeight
                            if (diff > 0) {
                                Text(
                                    text = "还需减 ${String.format("%.1f", diff)} kg",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppleOrange
                                )
                            } else if (diff < 0) {
                                Text(
                                    text = "已超目标 ${String.format("%.1f", -diff)} kg",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppleTeal
                                )
                            } else {
                                Text(
                                    text = "🎉 已达成目标！",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppleTeal
                                )
                            }
                        }
                        
                        // 最后记录时间
                        latestEntry?.let { entry ->
                            Spacer(modifier = Modifier.height(8.dp))
                            val dateText = formatRecordDate(entry.recordedAt)
                            Text(
                                text = "上次记录: $dateText",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppleGray2
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 添加记录按钮
                Button(
                    onClick = onAddWeight,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppleBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("记录今日体重")
                }
            } else {
                // 无体重数据时显示空状态
                EmptyWeightState(onAddWeight = onAddWeight)
            }
        }
    }
}

/**
 * 体重进度环
 */
@Composable
private fun WeightProgressRing(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
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
            val sweepAngle = progress * 360f
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        
        // 中心百分比
        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * 空状态
 */
@Composable
private fun EmptyWeightState(
    onAddWeight: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.MonitorWeight,
            contentDescription = null,
            tint = AppleGray3,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "开始记录体重",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "追踪体重变化，达成健康目标",
            style = MaterialTheme.typography.bodySmall,
            color = AppleGray2
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onAddWeight,
            colors = ButtonDefaults.buttonColors(containerColor = AppleBlue),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("记录体重")
        }
    }
}

/**
 * 格式化记录日期
 */
private fun formatRecordDate(timestamp: Long): String {
    val date = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    val today = LocalDate.now()
    
    return when {
        date == today -> "今天"
        date == today.minusDays(1) -> "昨天"
        date.year == today.year -> date.format(DateTimeFormatter.ofPattern("MM月dd日"))
        else -> date.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))
    }
}
