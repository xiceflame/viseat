package com.rokid.nutrition.phone.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rokid.nutrition.phone.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ==================== 骨架屏 Shimmer 效果 ====================

/**
 * 骨架屏加载效果
 * 
 * @param modifier Modifier
 * @param shape 形状
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(AppCorners.Medium)
) {
    val shimmerColors = listOf(
        AppleGray5.copy(alpha = 0.9f),
        AppleGray4.copy(alpha = 0.4f),
        AppleGray5.copy(alpha = 0.9f)
    )
    
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = AppAnimation.Shimmer,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim)
    )
    
    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
    )
}

/**
 * 骨架屏卡片
 */
@Composable
fun ShimmerCard(
    modifier: Modifier = Modifier,
    height: Dp = 120.dp
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppCorners.Large))
            .background(MaterialTheme.colorScheme.surface)
            .padding(AppSpacing.Normal)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShimmerBox(
                modifier = Modifier.size(48.dp),
                shape = CircleShape
            )
            Spacer(modifier = Modifier.width(AppSpacing.Medium))
            Column {
                ShimmerBox(
                    modifier = Modifier
                        .width(120.dp)
                        .height(16.dp)
                )
                Spacer(modifier = Modifier.height(AppSpacing.Small))
                ShimmerBox(
                    modifier = Modifier
                        .width(80.dp)
                        .height(12.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(AppSpacing.Normal))
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(height - 80.dp)
        )
    }
}

// ==================== 数值计数动画 ====================

/**
 * 带计数动画的数值显示
 * 
 * @param targetValue 目标数值
 * @param suffix 后缀（如 "kcal"）
 * @param duration 动画时长
 * @param style 文字样式
 */
@Composable
fun AnimatedCounter(
    targetValue: Int,
    modifier: Modifier = Modifier,
    suffix: String = "",
    prefix: String = "",
    duration: Int = AppAnimation.CountUp,
    fontSize: TextUnit = 32.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    var currentValue by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(targetValue) {
        if (targetValue == 0) {
            currentValue = 0
            return@LaunchedEffect
        }
        
        val startTime = System.currentTimeMillis()
        val startValue = currentValue
        val diff = targetValue - startValue
        
        while (currentValue != targetValue) {
            val elapsed = System.currentTimeMillis() - startTime
            val progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
            val easedProgress = FastOutSlowInEasing.transform(progress)
            currentValue = (startValue + diff * easedProgress).toInt()
            
            if (progress >= 1f) {
                currentValue = targetValue
                break
            }
            delay(16) // ~60fps
        }
    }
    
    Text(
        text = "$prefix$currentValue$suffix",
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color,
        modifier = modifier
    )
}

/**
 * 带计数动画的浮点数显示
 */
@Composable
fun AnimatedFloatCounter(
    targetValue: Float,
    modifier: Modifier = Modifier,
    decimalPlaces: Int = 1,
    suffix: String = "",
    prefix: String = "",
    duration: Int = AppAnimation.CountUp,
    fontSize: TextUnit = 32.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    var currentValue by remember { mutableFloatStateOf(0f) }
    
    LaunchedEffect(targetValue) {
        if (targetValue == 0f) {
            currentValue = 0f
            return@LaunchedEffect
        }
        
        val startTime = System.currentTimeMillis()
        val startValue = currentValue
        val diff = targetValue - startValue
        
        while (currentValue != targetValue) {
            val elapsed = System.currentTimeMillis() - startTime
            val progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
            val easedProgress = FastOutSlowInEasing.transform(progress)
            currentValue = startValue + diff * easedProgress
            
            if (progress >= 1f) {
                currentValue = targetValue
                break
            }
            delay(16)
        }
    }
    
    Text(
        text = "$prefix${String.format("%.${decimalPlaces}f", currentValue)}$suffix",
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color,
        modifier = modifier
    )
}

// ==================== 环形进度条 ====================

/**
 * 渐变环形进度条
 * 
 * @param progress 进度 (0f - 1f)
 * @param strokeWidth 线宽
 * @param gradientColors 渐变色
 */
@Composable
fun GradientCircularProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 12.dp,
    gradientColors: List<Color> = listOf(AppleTeal, AppleMint),
    trackColor: Color = AppleGray5,
    size: Dp = 120.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(
            durationMillis = AppAnimation.Slow,
            easing = FastOutSlowInEasing
        ),
        label = "circularProgress"
    )
    
    val sweepAngle = animatedProgress * 360f
    
    Canvas(
        modifier = modifier.size(size)
    ) {
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
        
        // 渐变进度
        val gradientBrush = Brush.sweepGradient(
            colors = gradientColors + gradientColors.first(),
            center = center
        )
        
        rotate(-90f, center) {
            drawArc(
                brush = gradientBrush,
                startAngle = 0f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2),
                size = Size(size.toPx() - strokeWidthPx, size.toPx() - strokeWidthPx),
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * 带中心内容的环形进度条
 */
@Composable
fun CircularProgressWithContent(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 10.dp,
    gradientColors: List<Color> = listOf(AppleTeal, AppleMint),
    size: Dp = 100.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        GradientCircularProgress(
            progress = progress,
            strokeWidth = strokeWidth,
            gradientColors = gradientColors,
            size = size
        )
        content()
    }
}

// ==================== 弧形进度条（半圆） ====================

/**
 * 渐变弧形进度条
 */
@Composable
fun GradientArcProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 16.dp,
    gradientColors: List<Color> = listOf(AppleTeal, AppleMint, AppleBlue),
    trackColor: Color = AppleGray5,
    size: Dp = 200.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(
            durationMillis = AppAnimation.Slow,
            easing = FastOutSlowInEasing
        ),
        label = "arcProgress"
    )
    
    val sweepAngle = animatedProgress * 180f
    
    val strokeWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { strokeWidth.toPx() }
    val sizePx = with(androidx.compose.ui.platform.LocalDensity.current) { size.toPx() }
    
    Canvas(
        modifier = modifier.size(size, size / 2 + strokeWidth)
    ) {
        val arcSize = Size(sizePx - strokeWidthPx, sizePx - strokeWidthPx)
        val topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2)
        
        // 背景轨道
        drawArc(
            color = trackColor,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
        )
        
        // 渐变进度
        val gradientBrush = Brush.horizontalGradient(
            colors = gradientColors
        )
        
        drawArc(
            brush = gradientBrush,
            startAngle = 180f,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
        )
    }
}

// ==================== 粒子庆祝动画 ====================

data class Particle(
    val id: Int,
    var x: Float,
    var y: Float,
    val velocityX: Float,
    val velocityY: Float,
    val color: Color,
    val size: Float,
    var alpha: Float = 1f,
    val rotation: Float = Random.nextFloat() * 360f,
    val rotationSpeed: Float = Random.nextFloat() * 10f - 5f
)

/**
 * 庆祝粒子效果
 */
@Composable
fun CelebrationParticles(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    particleCount: Int = 50,
    colors: List<Color> = listOf(
        AppleTeal, AppleBlue, ApplePurple, 
        AppleOrange, ApplePink, CarbsAmber
    ),
    duration: Int = 3000
) {
    var particles by remember { mutableStateOf<List<Particle>>(emptyList()) }
    var isAnimating by remember { mutableStateOf(false) }
    
    LaunchedEffect(isActive) {
        if (isActive && !isAnimating) {
            isAnimating = true
            // 生成粒子
            particles = List(particleCount) { id ->
                Particle(
                    id = id,
                    x = 0.5f, // 从中心开始
                    y = 0.5f,
                    velocityX = (Random.nextFloat() - 0.5f) * 0.02f,
                    velocityY = -Random.nextFloat() * 0.015f - 0.005f,
                    color = colors.random(),
                    size = Random.nextFloat() * 8f + 4f
                )
            }
            
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < duration) {
                particles = particles.map { p ->
                    p.copy(
                        x = p.x + p.velocityX,
                        y = p.y + p.velocityY + 0.001f, // 重力
                        alpha = 1f - ((System.currentTimeMillis() - startTime).toFloat() / duration)
                    )
                }
                delay(16)
            }
            particles = emptyList()
            isAnimating = false
        }
    }
    
    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { particle ->
            drawCircle(
                color = particle.color.copy(alpha = particle.alpha.coerceIn(0f, 1f)),
                radius = particle.size,
                center = Offset(
                    particle.x * size.width,
                    particle.y * size.height
                )
            )
        }
    }
}

// ==================== 呼吸光晕效果 ====================

/**
 * 呼吸光晕效果（用于头像等）
 */
@Composable
fun BreathingGlow(
    modifier: Modifier = Modifier,
    color: Color = AppleTeal,
    size: Dp = 100.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowRadius by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowRadius"
    )
    
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    
    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                val glowSizePx = this.size.minDimension * glowRadius
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = glowAlpha),
                            color.copy(alpha = 0f)
                        ),
                        radius = glowSizePx
                    ),
                    radius = glowSizePx
                )
            }
    )
}

// ==================== 脉冲动画 ====================

/**
 * 脉冲圆点动画
 */
@Composable
fun PulsingDot(
    modifier: Modifier = Modifier,
    color: Color = AppleTeal,
    size: Dp = 12.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    
    Box(
        modifier = modifier
            .size(size * scale)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}

// ==================== 波纹加载动画 ====================

/**
 * 波纹扩散加载动画
 */
@Composable
fun RippleLoader(
    modifier: Modifier = Modifier,
    color: Color = AppleTeal,
    size: Dp = 60.dp,
    rippleCount: Int = 3
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ripple")
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        repeat(rippleCount) { index ->
            val delay = index * 400
            val scale by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 1200,
                        delayMillis = delay,
                        easing = LinearOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rippleScale$index"
            )
            
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 1200,
                        delayMillis = delay,
                        easing = LinearOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rippleAlpha$index"
            )
            
            Box(
                modifier = Modifier
                    .size(size * scale)
                    .clip(CircleShape)
                    .background(color.copy(alpha = alpha))
            )
        }
    }
}

// ==================== 渐变按钮 ====================

/**
 * 渐变背景修饰符
 */
fun Modifier.gradientBackground(
    brush: Brush,
    shape: Shape = RoundedCornerShape(AppCorners.Large)
): Modifier = this
    .clip(shape)
    .background(brush)
