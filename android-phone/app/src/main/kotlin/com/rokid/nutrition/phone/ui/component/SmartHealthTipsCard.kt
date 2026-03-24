package com.rokid.nutrition.phone.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rokid.nutrition.phone.network.model.PersonalizedTip
import com.rokid.nutrition.phone.ui.theme.*
import kotlinx.coroutines.delay

/**
 * 智能健康提示卡片
 * 
 * 特性：
 * - 自动轮播（每5秒切换）
 * - 触摸暂停，离开3秒后恢复
 * - 底部圆点指示器
 * - 无反馈按钮，简洁界面
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SmartHealthTipsCard(
    tips: List<PersonalizedTip>,
    isLoading: Boolean = false,
    autoScrollInterval: Long = 5000L,
    pauseAfterTouchMs: Long = 3000L,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 如果没有提示，显示默认提示
    val displayTips = if (tips.isEmpty()) {
        listOf(defaultTip)
    } else {
        tips
    }
    
    val pagerState = rememberPagerState(pageCount = { displayTips.size })
    var isPaused by remember { mutableStateOf(false) }
    
    // 自动轮播逻辑
    LaunchedEffect(pagerState, isPaused, displayTips.size) {
        if (!isPaused && displayTips.size > 1) {
            while (true) {
                delay(autoScrollInterval)
                if (!isPaused) {
                    val nextPage = (pagerState.currentPage + 1) % displayTips.size
                    pagerState.animateScrollToPage(nextPage)
                }
            }
        }
    }
    
    val isDark = isSystemInDarkTheme()
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) AppElevation.Small else AppElevation.Medium,
                shape = RoundedCornerShape(AppCorners.Large),
                ambientColor = if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.05f),
                spotColor = if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.1f)
            )
            .then(
                if (isDark) {
                    Modifier.border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                GlowBorderDark.copy(alpha = 0.5f),
                                ApplePurple.copy(alpha = 0.3f)
                            )
                        ),
                        shape = RoundedCornerShape(AppCorners.Large)
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(AppCorners.Large),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) 
                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f) 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 标题栏 - 简洁高端风格
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 简洁图标 - 不用渐变
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PremiumColors.Sage.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Lightbulb,
                            contentDescription = null,
                            tint = PremiumColors.Sage,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "健康建议",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PremiumColors.Ink,
                        letterSpacing = 0.sp
                    )
                }
                
                // 刷新按钮 - 更精致
                IconButton(
                    onClick = onRefresh,
                    enabled = !isLoading,
                    modifier = Modifier.size(36.dp)
                ) {
                    if (isLoading) {
                        PremiumSpinner(
                            modifier = Modifier.size(18.dp),
                            color = PremiumColors.Sage,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = "刷新建议",
                            tint = PremiumColors.InkSubtle,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 提示内容轮播
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPaused = true
                                tryAwaitRelease()
                                // 手指离开后延迟恢复
                                delay(pauseAfterTouchMs)
                                isPaused = false
                            }
                        )
                    }
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    TipContentSimple(tip = displayTips[page])
                }
            }
            
            // 底部圆点指示器（多条提示时显示）
            if (displayTips.size > 1) {
                Spacer(modifier = Modifier.height(16.dp))
                PageIndicatorDots(
                    total = displayTips.size,
                    current = pagerState.currentPage,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

/**
 * 简化的提示内容（无反馈按钮）
 */
@Composable
private fun TipContentSimple(tip: PersonalizedTip) {
    val (icon, color, categoryName) = getTipStyleByCategory(tip.category)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.08f))
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
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 页面指示器圆点
 */
@Composable
private fun PageIndicatorDots(
    total: Int,
    current: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            val isSelected = index == current
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(if (isSelected) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) ApplePurple else AppleGray4
                    )
            )
        }
    }
}

/**
 * 根据类别获取提示样式
 */
private fun getTipStyleByCategory(category: String): Triple<ImageVector, Color, String> {
    return when (category.lowercase()) {
        "nutrition" -> Triple(Icons.Rounded.Restaurant, AppleTeal, "营养建议")
        "timing" -> Triple(Icons.Rounded.Schedule, AppleBlue, "用餐时间")
        "habit" -> Triple(Icons.Rounded.FitnessCenter, ApplePurple, "饮食习惯")
        "warning" -> Triple(Icons.Rounded.Warning, AppleOrange, "健康提醒")
        "encouragement" -> Triple(Icons.Rounded.EmojiEvents, AppleGreen, "鼓励")
        else -> Triple(Icons.Rounded.Lightbulb, AppleGray1, "建议")
    }
}

/**
 * 默认提示（无数据时显示）
 */
private val defaultTip = PersonalizedTip(
    id = "default",
    content = "保持健康饮食习惯，记录每一餐，让 AI 为您提供个性化建议 🍽️",
    category = "encouragement",
    priority = 10,
    validUntil = null
)
