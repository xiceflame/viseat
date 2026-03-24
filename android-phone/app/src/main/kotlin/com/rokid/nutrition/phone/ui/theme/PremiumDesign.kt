package com.rokid.nutrition.phone.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Premium Design System
 * 
 * 高端设计系统 - 参考 Apple、Dieter Rams、无印良品的设计理念
 * 追求：克制、精致、质感、呼吸感
 */

// ==================== 高级配色 - 更柔和、更有质感 ====================

object PremiumColors {
    // 主色 - 降低饱和度，更柔和
    val Sage = Color(0xFF5B8A72)              // 鼠尾草绿 - 替代刺眼的 Teal
    val SageLight = Color(0xFF7BA892)         // 浅鼠尾草
    val SageMuted = Color(0xFFB8D4C8)         // 柔和鼠尾草
    
    // 中性暖色 - 增加温度感
    val Warmth = Color(0xFFF5F1EB)            // 暖白
    val Linen = Color(0xFFFAF8F5)             // 亚麻白
    val Cream = Color(0xFFF8F6F1)             // 奶油白
    val Sand = Color(0xFFE8E2D9)              // 沙色
    
    // 文字色 - 避免纯黑，使用深灰
    val Ink = Color(0xFF1D1D1F)               // 墨色（深灰而非纯黑）
    val InkLight = Color(0xFF424245)          // 浅墨色
    val InkMuted = Color(0xFF6E6E73)          // 柔和墨色
    val InkSubtle = Color(0xFF86868B)         // 淡墨色
    
    // 强调色 - 低饱和、高级感
    val Terracotta = Color(0xFFBF8B67)        // 赤陶色
    val Dusty = Color(0xFFA89F91)             // 尘土色
    val Slate = Color(0xFF6B7280)             // 石板灰
    val Mist = Color(0xFF9CA3AF)              // 薄雾灰
    
    // 功能色 - 柔和版本
    val Success = Color(0xFF6B9B7A)           // 柔和绿
    val Warning = Color(0xFFD4A574)           // 柔和橙
    val Error = Color(0xFFB85C5C)             // 柔和红
    val Info = Color(0xFF6B8FB8)              // 柔和蓝
    
    // 深色模式 - 避免纯黑，使用深灰
    val NightSurface = Color(0xFF161618)      // 夜色表面
    val NightCard = Color(0xFF1C1C1E)         // 夜色卡片
    val NightElevated = Color(0xFF2C2C2E)     // 抬升夜色
}

// ==================== 高级渐变 - 更自然、更微妙 ====================

object PremiumGradients {
    // 自然光渐变 - 模拟自然光线
    val NaturalLight = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.8f),
            Color.White.copy(alpha = 0f)
        )
    )
    
    // 柔和高光 - 顶部微光
    val SoftHighlight = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.15f),
            Color.Transparent
        )
    )
    
    // 深度阴影 - 底部渐隐
    val DepthShadow = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            Color.Black.copy(alpha = 0.03f)
        )
    )
    
    // 页面背景 - 极其微妙的渐变
    @Composable
    fun pageBackground(): Brush {
        val isDark = isSystemInDarkTheme()
        return if (isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    PremiumColors.NightSurface,
                    Color(0xFF0D0D0E)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    PremiumColors.Linen,
                    PremiumColors.Warmth,
                    Color.White
                )
            )
        }
    }
    
    // 卡片内渐变 - 增加质感
    val CardSheen = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.08f),
            Color.Transparent,
            Color.Black.copy(alpha = 0.02f)
        )
    )
}

// ==================== 间距系统 - 更有呼吸感 ====================

object PremiumSpacing {
    val Hairline: Dp = 1.dp      // 发丝线
    val Micro: Dp = 2.dp         // 微距
    val Tiny: Dp = 4.dp          // 极小
    val Small: Dp = 8.dp         // 小
    val Base: Dp = 12.dp         // 基础
    val Medium: Dp = 16.dp       // 中
    val Large: Dp = 24.dp        // 大
    val XLarge: Dp = 32.dp       // 超大
    val XXLarge: Dp = 48.dp      // 特大
    val Section: Dp = 64.dp      // 区块
    val Page: Dp = 80.dp         // 页面
}

// ==================== 圆角系统 - 更精致 ====================

object PremiumCorners {
    val None: Dp = 0.dp
    val Subtle: Dp = 4.dp        // 微圆角
    val Small: Dp = 8.dp         // 小圆角
    val Medium: Dp = 12.dp       // 中圆角
    val Large: Dp = 16.dp        // 大圆角
    val XLarge: Dp = 20.dp       // 超大圆角
    val XXLarge: Dp = 28.dp      // 特大圆角
    val Pill: Dp = 999.dp        // 胶囊形
}

// ==================== 阴影系统 - 更自然 ====================

object PremiumElevation {
    val None: Dp = 0.dp
    val Subtle: Dp = 1.dp        // 微妙 - 几乎看不到
    val Low: Dp = 2.dp           // 低 - 轻微浮起
    val Medium: Dp = 4.dp        // 中 - 明显浮起
    val High: Dp = 8.dp          // 高 - 卡片
    val Floating: Dp = 16.dp     // 浮动 - 弹窗
    val Modal: Dp = 24.dp        // 模态 - 全屏弹窗
}

// ==================== 动画时长 - 更优雅 ====================

object PremiumDuration {
    const val Instant = 80           // 即时反馈
    const val Quick = 150            // 快速过渡
    const val Standard = 250         // 标准过渡
    const val Emphasized = 350       // 强调过渡
    const val Enter = 300            // 进入动画
    const val Exit = 200             // 退出动画
    const val Stagger = 50           // 错开延迟
}

// ==================== 透明度系统 ====================

object PremiumAlpha {
    const val Invisible = 0f
    const val Hint = 0.04f           // 提示 - 几乎看不到
    const val Subtle = 0.08f         // 微妙
    const val Light = 0.12f          // 轻微
    const val Medium = 0.24f         // 中等
    const val High = 0.48f           // 较高
    const val Prominent = 0.72f      // 突出
    const val Full = 1f              // 完全不透明
}

// ==================== 质感效果 ====================

object PremiumTexture {
    // 噪点纹理强度
    const val NoiseIntensity = 0.02f
    
    // 磨砂玻璃模糊度
    val FrostedBlur: Dp = 24.dp
    
    // 内阴影强度
    const val InnerShadowAlpha = 0.06f
}
