package com.rokid.nutrition.phone.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rokid.nutrition.phone.network.model.PersonalizedTip
import com.rokid.nutrition.phone.ui.theme.*

/**
 * AI 健康洞察卡片
 * 
 * 显示个性化健康建议，支持：
 * - 多条建议轮播
 * - 按类别显示不同图标和颜色
 * - 刷新建议
 * - 反馈功能
 */
@Composable
fun AIInsightsCard(
    tips: List<PersonalizedTip>,
    isLoading: Boolean = false,
    onRefresh: () -> Unit = {},
    onTipClick: (PersonalizedTip) -> Unit = {},
    onFeedback: (PersonalizedTip, Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var currentIndex by remember { mutableStateOf(0) }
    val currentTip = tips.getOrNull(currentIndex)
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(20.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // AI 图标 - 渐变背景
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(ApplePurple, AppleBlue)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Psychology,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "AI 健康洞察",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (tips.isNotEmpty()) {
                            Text(
                                text = "${currentIndex + 1}/${tips.size} 条建议",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppleGray2
                            )
                        }
                    }
                }
                
                // 刷新按钮
                IconButton(
                    onClick = onRefresh,
                    enabled = !isLoading,
                    modifier = Modifier.size(36.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = ApplePurple
                        )
                    } else {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = "刷新建议",
                            tint = AppleGray2,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 建议内容
            if (tips.isEmpty() && !isLoading) {
                EmptyTipsContent()
            } else if (currentTip != null) {
                TipContent(
                    tip = currentTip,
                    onClick = { onTipClick(currentTip) },
                    onFeedback = { isHelpful -> onFeedback(currentTip, isHelpful) }
                )
                
                // 导航点（多条建议时显示）
                if (tips.size > 1) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TipNavigationDots(
                        total = tips.size,
                        current = currentIndex,
                        onPrevious = { 
                            currentIndex = (currentIndex - 1 + tips.size) % tips.size 
                        },
                        onNext = { 
                            currentIndex = (currentIndex + 1) % tips.size 
                        }
                    )
                }
            }
        }
    }
}

/**
 * 单条建议内容
 */
@Composable
private fun TipContent(
    tip: PersonalizedTip,
    onClick: () -> Unit,
    onFeedback: (Boolean) -> Unit
) {
    val (icon, color, categoryName) = getTipStyle(tip.category)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        // 类别标签
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = categoryName,
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
            
            // 优先级指示（高优先级显示）
            if (tip.priority <= 2) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = AppleOrange.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "重要",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppleOrange,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // 建议内容
        Text(
            text = tip.content,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 反馈按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "这条建议有帮助吗？",
                style = MaterialTheme.typography.labelSmall,
                color = AppleGray2
            )
            Spacer(modifier = Modifier.width(8.dp))
            
            // 有帮助
            IconButton(
                onClick = { onFeedback(true) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Rounded.ThumbUp,
                    contentDescription = "有帮助",
                    tint = AppleGray3,
                    modifier = Modifier.size(16.dp)
                )
            }
            
            // 没帮助
            IconButton(
                onClick = { onFeedback(false) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Rounded.ThumbDown,
                    contentDescription = "没帮助",
                    tint = AppleGray3,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * 空状态内容
 */
@Composable
private fun EmptyTipsContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.Lightbulb,
            contentDescription = null,
            tint = AppleGray3,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "暂无个性化建议",
            style = MaterialTheme.typography.bodyMedium,
            color = AppleGray1
        )
        Text(
            text = "记录更多用餐数据后，AI 将为您生成专属建议",
            style = MaterialTheme.typography.bodySmall,
            color = AppleGray2
        )
    }
}

/**
 * 建议导航点
 */
@Composable
private fun TipNavigationDots(
    total: Int,
    current: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 上一条
        IconButton(
            onClick = onPrevious,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Rounded.ChevronLeft,
                contentDescription = "上一条",
                tint = AppleGray2,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // 导航点
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(if (index == current) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == current) ApplePurple else AppleGray4
                    )
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // 下一条
        IconButton(
            onClick = onNext,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = "下一条",
                tint = AppleGray2,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 获取建议类别的样式
 */
private fun getTipStyle(category: String): Triple<ImageVector, Color, String> {
    return when (category.lowercase()) {
        "nutrition" -> Triple(Icons.Rounded.Restaurant, AppleTeal, "营养建议")
        "timing" -> Triple(Icons.Rounded.Schedule, AppleBlue, "用餐时间")
        "habit" -> Triple(Icons.Rounded.FitnessCenter, ApplePurple, "饮食习惯")
        "warning" -> Triple(Icons.Rounded.Warning, AppleOrange, "健康提醒")
        "encouragement" -> Triple(Icons.Rounded.EmojiEvents, AppleGreen, "鼓励")
        else -> Triple(Icons.Rounded.Lightbulb, AppleGray1, "建议")
    }
}
