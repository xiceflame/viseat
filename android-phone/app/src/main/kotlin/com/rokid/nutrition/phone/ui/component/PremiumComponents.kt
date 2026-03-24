package com.rokid.nutrition.phone.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rokid.nutrition.phone.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ==================== 高端卡片 ====================

/**
 * 高端质感卡片
 * 
 * 特点：微妙的内阴影、自然的光泽、精致的边框
 */
@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = PremiumCorners.Large,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isDark) PremiumColors.NightCard else Color.White
    val borderColor = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f)
    
    Surface(
        modifier = modifier
            .shadow(
                elevation = PremiumElevation.Low,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .border(
                width = 0.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(cornerRadius)
            ),
        shape = RoundedCornerShape(cornerRadius),
        color = backgroundColor
    ) {
        // 顶部光泽层
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawWithContent {
                        drawContent()
                        // 顶部微光
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDark) 0.03f else 0.06f),
                                    Color.Transparent
                                ),
                                endY = size.height * 0.3f
                            )
                        )
                    }
                    .padding(PremiumSpacing.Medium),
                content = content
            )
        }
    }
}

/**
 * 凹陷质感卡片 - 用于输入区域或次要内容
 */
@Composable
fun InsetCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = PremiumCorners.Medium,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isDark) Color.Black.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.03f)
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .drawBehind {
                // 内阴影效果
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        endY = 8.dp.toPx()
                    ),
                    cornerRadius = CornerRadius(cornerRadius.toPx())
                )
            }
            .padding(PremiumSpacing.Medium),
        content = content
    )
}

// ==================== 高端进度指示器 ====================

/**
 * 精致环形进度
 * 
 * 特点：细线条、自然渐变、优雅的端点
 */
@Composable
fun RefinedCircularProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    strokeWidth: Dp = 6.dp,
    trackColor: Color = PremiumColors.Sand,
    progressColor: Color = PremiumColors.Sage
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(
            durationMillis = PremiumDuration.Emphasized,
            easing = FastOutSlowInEasing
        ),
        label = "progress"
    )
    
    Canvas(modifier = modifier.size(size)) {
        val strokeWidthPx = strokeWidth.toPx()
        val radius = (size.toPx() - strokeWidthPx) / 2
        val center = Offset(size.toPx() / 2, size.toPx() / 2)
        
        // 背景轨道
        drawCircle(
            color = trackColor,
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
        )
        
        // 进度弧
        val sweepAngle = animatedProgress * 360f
        drawArc(
            color = progressColor,
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2),
            size = Size(size.toPx() - strokeWidthPx, size.toPx() - strokeWidthPx),
            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
        )
    }
}

/**
 * 线性进度条 - 精致版
 */
@Composable
fun RefinedLinearProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 4.dp,
    trackColor: Color = PremiumColors.Sand,
    progressColor: Color = PremiumColors.Sage
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(
            durationMillis = PremiumDuration.Emphasized,
            easing = FastOutSlowInEasing
        ),
        label = "progress"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .clip(RoundedCornerShape(height / 2))
                .background(progressColor)
        )
    }
}

// ==================== 高端按钮 ====================

/**
 * 高端主按钮
 * 
 * 特点：柔和的颜色、精致的阴影、优雅的交互
 */
@Composable
fun PremiumButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: PremiumButtonStyle = PremiumButtonStyle.Primary
) {
    val isDark = isSystemInDarkTheme()
    
    val backgroundColor = when (style) {
        PremiumButtonStyle.Primary -> PremiumColors.Sage
        PremiumButtonStyle.Secondary -> if (isDark) PremiumColors.NightElevated else PremiumColors.Sand
        PremiumButtonStyle.Ghost -> Color.Transparent
    }
    
    val contentColor = when (style) {
        PremiumButtonStyle.Primary -> Color.White
        PremiumButtonStyle.Secondary -> PremiumColors.Ink
        PremiumButtonStyle.Ghost -> PremiumColors.Sage
    }
    
    val borderColor = when (style) {
        PremiumButtonStyle.Ghost -> PremiumColors.Sage.copy(alpha = 0.3f)
        else -> Color.Transparent
    }
    
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(52.dp)
            .then(
                if (style == PremiumButtonStyle.Primary) {
                    Modifier.shadow(
                        elevation = PremiumElevation.Low,
                        shape = RoundedCornerShape(PremiumCorners.Medium),
                        ambientColor = PremiumColors.Sage.copy(alpha = 0.2f)
                    )
                } else Modifier
            )
            .border(
                width = if (style == PremiumButtonStyle.Ghost) 1.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(PremiumCorners.Medium)
            ),
        shape = RoundedCornerShape(PremiumCorners.Medium),
        color = backgroundColor.copy(alpha = if (enabled) 1f else 0.5f),
        enabled = enabled
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor.copy(alpha = if (enabled) 1f else 0.5f),
                letterSpacing = 0.5.sp
            )
        }
    }
}

enum class PremiumButtonStyle {
    Primary,    // 主要操作
    Secondary,  // 次要操作
    Ghost       // 幽灵按钮
}

// ==================== 高端数值显示 ====================

/**
 * 精致数值显示
 * 
 * 特点：优雅的字体层次、微妙的颜色变化
 */
@Composable
fun PremiumStatDisplay(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = PremiumColors.Ink,
    labelColor: Color = PremiumColors.InkMuted,
    valueFontSize: TextUnit = 28.sp,
    labelFontSize: TextUnit = 12.sp
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = valueFontSize,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            letterSpacing = (-0.5).sp
        )
        Spacer(modifier = Modifier.height(PremiumSpacing.Tiny))
        Text(
            text = label,
            fontSize = labelFontSize,
            fontWeight = FontWeight.Normal,
            color = labelColor,
            letterSpacing = 0.5.sp
        )
    }
}

// ==================== 高端分割线 ====================

/**
 * 精致分割线
 */
@Composable
fun PremiumDivider(
    modifier: Modifier = Modifier,
    color: Color = PremiumColors.Sand,
    thickness: Dp = 0.5.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .background(color)
    )
}

// ==================== 高端加载动画 ====================

/**
 * 优雅的加载指示器 - 三点呼吸
 */
@Composable
fun PremiumLoader(
    modifier: Modifier = Modifier,
    color: Color = PremiumColors.Sage,
    dotSize: Dp = 6.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loader")
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(PremiumSpacing.Small)
    ) {
        repeat(3) { index ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 600,
                        delayMillis = index * 150,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )
            
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(color.copy(alpha = alpha))
            )
        }
    }
}

/**
 * 精致的旋转加载器
 */
@Composable
fun PremiumSpinner(
    modifier: Modifier = Modifier,
    color: Color = PremiumColors.Sage,
    size: Dp = 24.dp,
    strokeWidth: Dp = 2.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spinner")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Canvas(modifier = modifier.size(size)) {
        val strokeWidthPx = strokeWidth.toPx()
        val radius = (size.toPx() - strokeWidthPx) / 2
        val center = Offset(size.toPx() / 2, size.toPx() / 2)
        
        // 渐变弧线
        drawArc(
            color = color,
            startAngle = rotation,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2),
            size = Size(size.toPx() - strokeWidthPx, size.toPx() - strokeWidthPx),
            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
            alpha = 0.8f
        )
    }
}

// ==================== 高端标签 ====================

/**
 * 精致标签
 */
@Composable
fun PremiumChip(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    color: Color = PremiumColors.Sage
) {
    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (selected) {
        color.copy(alpha = 0.12f)
    } else {
        if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f)
    }
    val textColor = if (selected) color else PremiumColors.InkMuted
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(PremiumCorners.Pill),
        color = backgroundColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = PremiumSpacing.Base,
                vertical = PremiumSpacing.Small
            ),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = textColor
        )
    }
}

// ==================== 高端图标容器 ====================

/**
 * 精致图标背景
 */
@Composable
fun PremiumIconContainer(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    backgroundColor: Color = PremiumColors.Sage.copy(alpha = 0.1f),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))  // ~12dp for 44dp
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
        content = content
    )
}

// ==================== 高端空状态 ====================

/**
 * 优雅的空状态
 */
@Composable
fun PremiumEmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    action: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier.padding(PremiumSpacing.XLarge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        icon?.invoke()
        
        if (icon != null) {
            Spacer(modifier = Modifier.height(PremiumSpacing.Large))
        }
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            color = PremiumColors.Ink
        )
        
        Spacer(modifier = Modifier.height(PremiumSpacing.Small))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = PremiumColors.InkMuted,
            lineHeight = 22.sp
        )
        
        if (action != null) {
            Spacer(modifier = Modifier.height(PremiumSpacing.Large))
            action()
        }
    }
}
