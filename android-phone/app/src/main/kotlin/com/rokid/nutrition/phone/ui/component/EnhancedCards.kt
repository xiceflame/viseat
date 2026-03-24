package com.rokid.nutrition.phone.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rokid.nutrition.phone.ui.theme.*

// ==================== 增强型卡片组件 ====================

/**
 * 玻璃态卡片 (Glassmorphism)
 * 
 * 现代毛玻璃效果卡片，适用于重要内容展示
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = AppCorners.Large,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isDark) GlassBackgroundDark else GlassBackgroundLight
    val borderColor = if (isDark) GlowBorderDark else AppleGray4.copy(alpha = 0.3f)
    
    Surface(
        modifier = modifier
            .shadow(
                elevation = AppElevation.Medium,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.12f)
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(cornerRadius)
            ),
        shape = RoundedCornerShape(cornerRadius),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.Normal),
            content = content
        )
    }
}

/**
 * 渐变边框卡片
 * 
 * 带渐变色边框的高级卡片样式
 */
@Composable
fun GradientBorderCard(
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = listOf(AppleTeal, AppleMint, AppleBlue),
    cornerRadius: Dp = AppCorners.Large,
    borderWidth: Dp = 2.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.linearGradient(colors = gradientColors)
            )
            .padding(borderWidth)
    ) {
        Surface(
            shape = RoundedCornerShape(cornerRadius - borderWidth),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(AppSpacing.Normal),
                content = content
            )
        }
    }
}

/**
 * 浮动卡片
 * 
 * 带有高级阴影效果的浮动卡片
 */
@Composable
fun FloatingCard(
    modifier: Modifier = Modifier,
    elevation: Dp = AppElevation.Large,
    cornerRadius: Dp = AppCorners.Large,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = Color.Black.copy(alpha = 0.1f),
                spotColor = Color.Black.copy(alpha = 0.15f)
            ),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.Normal),
            content = content
        )
    }
}

// ==================== 增强型营养数据卡片 ====================

/**
 * 营养素环形进度卡片
 */
@Composable
fun NutrientRingCard(
    label: String,
    value: Float,
    maxValue: Float,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp
) {
    val progress = if (maxValue > 0) (value / maxValue).coerceIn(0f, 1f) else 0f
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressWithContent(
            progress = progress,
            size = size,
            strokeWidth = 8.dp,
            gradientColors = listOf(color, color.copy(alpha = 0.6f))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedCounter(
                    targetValue = value.toInt(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppleGray1
                )
            }
        }
        
        Spacer(modifier = Modifier.height(AppSpacing.Small))
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 增强型热量卡片
 * 
 * 带渐变弧形进度条的热量展示卡片
 */
@Composable
fun EnhancedCaloriesCard(
    currentCalories: Int,
    targetCalories: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (targetCalories > 0) {
        (currentCalories.toFloat() / targetCalories).coerceIn(0f, 1f)
    } else 0f
    
    val progressColor = when {
        progress < 0.7f -> AppleTeal
        progress < 0.9f -> AppleOrange
        else -> AppleRed
    }
    
    FloatingCard(modifier = modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "今日热量摄入",
                style = MaterialTheme.typography.titleMedium,
                color = AppleGray1
            )
            
            Spacer(modifier = Modifier.height(AppSpacing.Normal))
            
            Box(contentAlignment = Alignment.Center) {
                GradientArcProgress(
                    progress = progress,
                    strokeWidth = 20.dp,
                    gradientColors = listOf(progressColor, progressColor.copy(alpha = 0.5f)),
                    size = 180.dp
                )
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(y = 20.dp)
                ) {
                    AnimatedCounter(
                        targetValue = currentCalories,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = progressColor
                    )
                    Text(
                        text = "/ $targetCalories kcal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppleGray1
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(AppSpacing.Medium))
            
            // 状态提示
            val statusText = when {
                progress < 0.5f -> "继续加油，保持健康饮食"
                progress < 0.8f -> "进度良好，注意均衡营养"
                progress < 1f -> "即将达标，注意控制"
                else -> "已达到目标，避免超量"
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = when {
                        progress < 0.8f -> Icons.Rounded.CheckCircle
                        progress < 1f -> Icons.Rounded.Info
                        else -> Icons.Rounded.Warning
                    },
                    contentDescription = null,
                    tint = progressColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(AppSpacing.Small))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppleGray1
                )
            }
        }
    }
}

/**
 * 宏量营养素卡片组
 */
@Composable
fun MacroNutrientsCard(
    protein: Float,
    carbs: Float,
    fat: Float,
    proteinTarget: Float = 60f,
    carbsTarget: Float = 250f,
    fatTarget: Float = 65f,
    modifier: Modifier = Modifier
) {
    FloatingCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppSpacing.Small),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NutrientRingCard(
                label = "蛋白质",
                value = protein,
                maxValue = proteinTarget,
                unit = "g",
                color = ProteinCyan
            )
            
            NutrientRingCard(
                label = "碳水",
                value = carbs,
                maxValue = carbsTarget,
                unit = "g",
                color = CarbsAmber
            )
            
            NutrientRingCard(
                label = "脂肪",
                value = fat,
                maxValue = fatTarget,
                unit = "g",
                color = FatGreen
            )
        }
    }
}

// ==================== 统计卡片 ====================

/**
 * 统计数据项
 */
@Composable
fun StatItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color = AppleTeal,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(AppCorners.Medium))
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(AppSpacing.Small))
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AppleGray1
        )
    }
}

/**
 * 带动画入场的统计卡片
 */
@Composable
fun AnimatedStatsCard(
    stats: List<Triple<ImageVector, String, String>>,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    FloatingCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            stats.forEachIndexed { index, (icon, label, value) ->
                val animatedAlpha by animateFloatAsState(
                    targetValue = if (isVisible) 1f else 0f,
                    animationSpec = tween(
                        durationMillis = AppAnimation.Normal,
                        delayMillis = index * 100,
                        easing = FastOutSlowInEasing
                    ),
                    label = "statAlpha$index"
                )
                
                val animatedOffset by animateFloatAsState(
                    targetValue = if (isVisible) 0f else 20f,
                    animationSpec = tween(
                        durationMillis = AppAnimation.Normal,
                        delayMillis = index * 100,
                        easing = FastOutSlowInEasing
                    ),
                    label = "statOffset$index"
                )
                
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = animatedAlpha
                            translationY = animatedOffset
                        }
                ) {
                    StatItem(
                        icon = icon,
                        label = label,
                        value = value,
                        color = listOf(AppleTeal, AppleBlue, AppleOrange)[index % 3]
                    )
                }
            }
        }
    }
}

// ==================== 功能入口卡片 ====================

/**
 * 功能入口卡片
 */
@Composable
fun FeatureCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    gradientColors: List<Color> = listOf(AppleTeal, AppleMint),
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .shadow(
                elevation = AppElevation.Medium,
                shape = RoundedCornerShape(AppCorners.Large),
                ambientColor = gradientColors.first().copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(AppCorners.Large),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.Normal),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(AppCorners.Medium))
                    .background(
                        brush = Brush.linearGradient(colors = gradientColors)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(AppSpacing.Normal))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppleGray1
                )
            }
            
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = AppleGray2,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ==================== 渐变按钮 ====================

/**
 * 渐变主按钮
 */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = listOf(AppleTeal, AppleMint),
    enabled: Boolean = true
) {
    val alpha = if (enabled) 1f else 0.5f
    
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .graphicsLayer { this.alpha = alpha },
        shape = RoundedCornerShape(AppCorners.Large),
        color = Color.Transparent,
        enabled = enabled
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(colors = gradientColors)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

/**
 * 带图标的渐变按钮
 */
@Composable
fun GradientButtonWithIcon(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = listOf(AppleTeal, AppleMint),
    enabled: Boolean = true
) {
    val alpha = if (enabled) 1f else 0.5f
    
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .graphicsLayer { this.alpha = alpha },
        shape = RoundedCornerShape(AppCorners.Large),
        color = Color.Transparent,
        enabled = enabled
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(colors = gradientColors)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(AppSpacing.Small))
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}
