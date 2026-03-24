package com.rokid.nutrition.phone.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ==================== Apple-Style Color Palette ====================

// Primary Colors - 清新的蓝绿色系
val AppleTeal = Color(0xFF34C759)           // 苹果绿
val AppleBlue = Color(0xFF007AFF)           // 苹果蓝
val AppleMint = Color(0xFF00C7BE)           // 薄荷绿

// Accent Colors
val AppleOrange = Color(0xFFFF9500)         // 活力橙
val ApplePink = Color(0xFFFF2D55)           // 玫红
val ApplePurple = Color(0xFFAF52DE)         // 紫色
val AppleRed = Color(0xFFFF3B30)            // 警告红
val AppleGreen = Color(0xFF34C759)          // 苹果绿（与 AppleTeal 相同）

// Neutral Colors - 苹果风格的灰度
val AppleGray1 = Color(0xFF8E8E93)          // 次要文字
val AppleGray2 = Color(0xFFAEAEB2)          // 占位符
val AppleGray3 = Color(0xFFC7C7CC)          // 分割线
val AppleGray4 = Color(0xFFD1D1D6)          // 边框
val AppleGray5 = Color(0xFFE5E5EA)          // 背景
val AppleGray6 = Color(0xFFF2F2F7)          // 浅背景

// Semantic Colors
val CalorieRed = Color(0xFFFF6B6B)          // 热量红
val ProteinCyan = Color(0xFF5AC8FA)         // 蛋白质青
val CarbsAmber = Color(0xFFFFCC00)          // 碳水金
val FatGreen = Color(0xFF34C759)            // 脂肪绿

// ==================== 渐变色系统 (Gradient System) ====================

// 主色渐变 - 用于主要按钮和强调元素
val PrimaryGradient = Brush.linearGradient(
    colors = listOf(AppleTeal, AppleMint)
)

val PrimaryGradientVertical = Brush.verticalGradient(
    colors = listOf(AppleTeal, AppleMint)
)

// 蓝色渐变 - 用于信息卡片
val BlueGradient = Brush.linearGradient(
    colors = listOf(AppleBlue, Color(0xFF5AC8FA))
)

// 紫色渐变 - 用于 AI 功能
val PurpleGradient = Brush.linearGradient(
    colors = listOf(ApplePurple, AppleBlue)
)

// 温暖渐变 - 用于警告和热量
val WarmGradient = Brush.linearGradient(
    colors = listOf(AppleOrange, ApplePink)
)

// 背景渐变 - 用于页面背景
val BackgroundGradientLight = Brush.verticalGradient(
    colors = listOf(
        AppleGray6,
        Color.White.copy(alpha = 0.95f),
        Color.White
    )
)

val BackgroundGradientDark = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF1C1C1E),
        Color(0xFF000000)
    )
)

// 毛玻璃背景 - 用于导航栏和弹窗
val GlassBackgroundLight = Color.White.copy(alpha = 0.72f)
val GlassBackgroundDark = Color(0xFF1C1C1E).copy(alpha = 0.72f)

// 发光边框色 - 用于深色模式卡片
val GlowBorderLight = AppleTeal.copy(alpha = 0.3f)
val GlowBorderDark = Color(0xFF30D158).copy(alpha = 0.4f)

// ==================== 阴影系统 (Shadow System) ====================

object AppElevation {
    val None: Dp = 0.dp
    val ExtraSmall: Dp = 1.dp   // 微小阴影 - 分割线、边框
    val Small: Dp = 2.dp        // 小阴影 - 次要卡片
    val Medium: Dp = 4.dp       // 中等阴影 - 普通卡片
    val Large: Dp = 8.dp        // 大阴影 - 重要卡片
    val ExtraLarge: Dp = 16.dp  // 超大阴影 - 弹窗、浮动元素
}

// ==================== 动画时长系统 (Animation Duration) ====================

object AppAnimation {
    const val Instant = 100        // 即时 - 微交互
    const val Fast = 200           // 快速 - 按钮点击
    const val Normal = 300         // 正常 - 页面切换
    const val Slow = 500           // 慢速 - 强调动画
    const val VerySlow = 800       // 非常慢 - 庆祝动画
    const val CountUp = 1500       // 计数动画
    const val Shimmer = 1200       // 骨架屏波纹
    const val AutoScroll = 5000    // 自动轮播
}

// ==================== 间距系统 (Spacing System - 8pt Grid) ====================

object AppSpacing {
    val XXSmall: Dp = 2.dp     // 超小 - 图标内部
    val XSmall: Dp = 4.dp      // 微小 - 紧凑元素
    val Small: Dp = 8.dp       // 小 - 同组元素间
    val Medium: Dp = 12.dp     // 中 - 相关元素间
    val Normal: Dp = 16.dp     // 正常 - 卡片内边距
    val Large: Dp = 24.dp      // 大 - 区块分隔
    val XLarge: Dp = 32.dp     // 超大 - 页面边距
    val XXLarge: Dp = 48.dp    // 特大 - 重要区块分隔
}

// ==================== 圆角系统 (Corner Radius System) ====================

object AppCorners {
    val None: Dp = 0.dp
    val XSmall: Dp = 4.dp      // 微小 - 标签、徽章
    val Small: Dp = 8.dp       // 小 - 按钮、输入框
    val Medium: Dp = 12.dp     // 中 - 小卡片
    val Large: Dp = 16.dp      // 大 - 普通卡片
    val XLarge: Dp = 20.dp     // 超大 - 重要卡片
    val XXLarge: Dp = 24.dp    // 特大 - 弹窗
    val Full: Dp = 999.dp      // 全圆 - 胶囊按钮
}

// ==================== Light Theme ====================

private val LightColorScheme = lightColorScheme(
    primary = AppleTeal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F9EC),
    onPrimaryContainer = Color(0xFF1B5E20),
    secondary = AppleBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3F2FD),
    onSecondaryContainer = Color(0xFF0D47A1),
    tertiary = AppleMint,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0F7FA),
    onTertiaryContainer = Color(0xFF006064),
    background = AppleGray6,
    onBackground = Color(0xFF1C1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = AppleGray6,
    onSurfaceVariant = AppleGray1,
    outline = AppleGray4,
    outlineVariant = AppleGray5,
    error = AppleRed,
    onError = Color.White
)

// ==================== Dark Theme ====================

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF30D158),             // 暗色模式绿
    onPrimary = Color(0xFF003314),
    primaryContainer = Color(0xFF1A3D1F),
    onPrimaryContainer = Color(0xFFB8F5C5),
    secondary = Color(0xFF0A84FF),           // 暗色模式蓝
    onSecondary = Color(0xFF001D3D),
    secondaryContainer = Color(0xFF0D3B66),
    onSecondaryContainer = Color(0xFFB3D4FF),
    tertiary = Color(0xFF64D2FF),            // 暗色模式青
    onTertiary = Color(0xFF003544),
    tertiaryContainer = Color(0xFF004D61),
    onTertiaryContainer = Color(0xFFB8EAFF),
    background = Color(0xFF000000),
    onBackground = Color(0xFFE5E5E5),
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFE5E5E5),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFF8E8E93),
    outline = Color(0xFF48484A),
    outlineVariant = Color(0xFF3A3A3C),
    error = Color(0xFFFF453A),
    onError = Color.White
)

// ==================== Typography ====================

private val AppleTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 41.sp,
        letterSpacing = 0.25.sp
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.41).sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.24).sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.41).sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.24).sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.08).sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.41).sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.24).sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.08).sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.08).sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.06.sp
    )
)

// ==================== Shapes ====================

private val AppleShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

// ==================== Theme ====================

@Composable
fun NutritionPhoneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 使用透明状态栏，让内容延伸到状态栏下方
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppleTypography,
        shapes = AppleShapes,
        content = content
    )
}
