package com.rokid.nutrition.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Rokid YodaOS-Sprite 设计系统
 * 
 * 基于官方设计规范:
 * - 显示区域: 480x640 可视安全, 480x400 建议显示
 * - 字体: HarmonyOS Sans SC
 * - 主色: 绿色 #40FF5E（只能用这一种颜色，通过透明度区分）
 * - 圆角: 12px
 * - 描边: ≥1.5px
 */
object RokidDesign {
    
    // ==================== 颜色系统 ====================
    /**
     * Rokid AR眼镜颜色规范
     * 
     * 核心规则：
     * - 只能使用 #40FF5E 绿色
     * - 通过透明度区分层次（40%、80%、100%）
     * - 禁止使用渐变和大面积高亮
     * - 禁止使用其他颜色
     */
    object Colors {
        // 主色 - Rokid官方绿 #40FF5E
        val Primary = Color(0xFF40FF5E)         // 100% 主绿色
        val PrimaryDark = Color(0xCC40FF5E)     // 80% 绿色（选中状态）
        val PrimaryLight = Color(0x6640FF5E)    // 40% 绿色（常态）
        
        // 背景色
        val Background = Color(0xFF000000)      // 纯黑背景
        val Surface = Color(0x1A40FF5E)         // 10% 绿色表面
        val SurfaceVariant = Color(0x3340FF5E)  // 20% 绿色卡片背景
        
        // 文字色（全部使用绿色透明度）
        val OnBackground = Color(0xFF40FF5E)        // 主文字 100%
        val OnBackgroundMedium = Color(0xCC40FF5E)  // 次要文字 80%
        val OnBackgroundLight = Color(0x6640FF5E)   // 辅助文字 40%
        val OnBackgroundHint = Color(0x3340FF5E)    // 提示文字 20%
        
        // 语义色（全部使用绿色透明度区分）
        val Success = Color(0xFF40FF5E)         // 成功/完成 - 100%
        val Warning = Color(0xCC40FF5E)         // 警告/进行中 - 80%
        val Error = Color(0x6640FF5E)           // 错误/断开 - 40%（不用红色）
        val Info = Color(0xCC40FF5E)            // 信息/处理中 - 80%
        
        // 营养色（使用透明度区分，不使用彩色）
        val Protein = Color(0xFF40FF5E)         // 蛋白质 - 100%
        val Carbs = Color(0xCC40FF5E)           // 碳水 - 80%
        val Fat = Color(0x6640FF5E)             // 脂肪 - 40%
        
        // 透明度变体
        fun primary(alpha: Float) = Primary.copy(alpha = alpha)
        fun onBackground(alpha: Float) = OnBackground.copy(alpha = alpha)
    }
    
    // ==================== 字体系统 ====================
    // 基于规范: 
    // 一级 32sp/40sp, 二级 24sp/32sp, 三级 20sp/26sp, 四级 18sp/24sp, 五级 16sp/22sp
    object Typography {
        // 一级标题 - 品牌/主数据
        val H1 = TextStyle(
            fontSize = 32.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Medium,
            color = Colors.OnBackground
        )
        
        // 一级标题加粗 - 热量数字
        val H1Bold = TextStyle(
            fontSize = 32.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Bold,
            color = Colors.Primary
        )
        
        // 超大数字 - 仅用于热量显示（特殊情况）
        val CalorieDisplay = TextStyle(
            fontSize = 48.sp,  // 比规范略大，但在安全范围
            lineHeight = 56.sp,
            fontWeight = FontWeight.Bold,
            color = Colors.Primary
        )
        
        // 二级标题 - 食物名称
        val H2 = TextStyle(
            fontSize = 24.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Medium,
            color = Colors.OnBackground
        )
        
        // 三级标题 - 状态信息
        val H3 = TextStyle(
            fontSize = 20.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.Normal,
            color = Colors.OnBackgroundMedium
        )
        
        // 四级 - 营养数值
        val Body1 = TextStyle(
            fontSize = 18.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Medium,
            color = Colors.OnBackground
        )
        
        // 五级 - 辅助文字/标签
        val Body2 = TextStyle(
            fontSize = 16.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Normal,
            color = Colors.OnBackgroundLight
        )
        
        // 提示文字
        val Caption = TextStyle(
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Normal,
            color = Colors.OnBackgroundHint
        )
    }
    
    // ==================== 尺寸系统 ====================
    object Dimensions {
        // 圆角
        val CornerRadius = 12.dp
        val CornerRadiusSmall = 8.dp
        val CornerRadiusLarge = 16.dp
        
        // 描边
        val BorderWidth = 1.5.dp
        val BorderWidthThick = 2.dp
        
        // 间距
        val SpacingXs = 4.dp
        val SpacingSm = 8.dp
        val SpacingMd = 12.dp
        val SpacingLg = 16.dp
        val SpacingXl = 24.dp
        val SpacingXxl = 32.dp
        
        // 安全区域（避开显示效果不佳区域）
        val SafeAreaTop = 80.dp      // 顶部安全区（160px 区域的一半作为缓冲）
        val SafeAreaBottom = 80.dp   // 底部安全区
        val SafeAreaHorizontal = 0.dp // 水平贴边
        
        // 内容区域
        val ContentPaddingHorizontal = 24.dp
        val ContentPaddingVertical = 16.dp
        
        // 图标尺寸
        val IconSizeLarge = 48.dp    // 灵活图标
        val IconSizeMedium = 28.dp   // 交互图标
        val IconSizeSmall = 16.dp    // 辅助图标
        
        // 状态指示器
        val StatusDotSize = 8.dp
        val ProgressBarHeight = 4.dp
    }
    
    // ==================== 形状系统 ====================
    object Shapes {
        val Small = RoundedCornerShape(Dimensions.CornerRadiusSmall)
        val Medium = RoundedCornerShape(Dimensions.CornerRadius)
        val Large = RoundedCornerShape(Dimensions.CornerRadiusLarge)
    }
}

// ==================== 可复用组件 ====================

/**
 * 状态指示点 - 连接状态等
 */
@Composable
fun StatusDot(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = RokidDesign.Colors.Success,
    inactiveColor: Color = RokidDesign.Colors.Error
) {
    Box(
        modifier = modifier
            .size(RokidDesign.Dimensions.StatusDotSize)
            .background(
                color = if (isActive) activeColor else inactiveColor,
                shape = RokidDesign.Shapes.Medium
            )
    )
}

/**
 * 信息卡片 - 用于显示结果、建议等
 */
@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = RokidDesign.Colors.SurfaceVariant,
    borderColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .then(
                if (borderColor != null) {
                    Modifier.border(
                        width = RokidDesign.Dimensions.BorderWidth,
                        color = borderColor,
                        shape = RokidDesign.Shapes.Medium
                    )
                } else Modifier
            )
            .background(
                color = backgroundColor,
                shape = RokidDesign.Shapes.Medium
            )
            .padding(RokidDesign.Dimensions.SpacingLg),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

/**
 * 营养数值显示项
 */
@Composable
fun NutrientDisplay(
    label: String,
    value: Int,
    unit: String = "g",
    color: Color = RokidDesign.Colors.OnBackground,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$value$unit",
            style = RokidDesign.Typography.Body1.copy(color = color)
        )
        Spacer(modifier = Modifier.height(RokidDesign.Dimensions.SpacingXs))
        Text(
            text = label,
            style = RokidDesign.Typography.Caption.copy(color = color.copy(alpha = 0.7f))
        )
    }
}

/**
 * 热量显示组件 - 核心数据展示
 */
@Composable
fun CalorieDisplay(
    calories: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = calories.toString(),
            style = RokidDesign.Typography.CalorieDisplay
        )
        Spacer(modifier = Modifier.height(RokidDesign.Dimensions.SpacingXs))
        Text(
            text = "千卡",
            style = RokidDesign.Typography.Body2.copy(
                color = RokidDesign.Colors.Primary.copy(alpha = 0.7f)
            )
        )
    }
}

/**
 * 建议文本框
 */
@Composable
fun SuggestionBox(
    text: String,
    modifier: Modifier = Modifier
) {
    if (text.isNotBlank()) {
        Box(
            modifier = modifier
                .background(
                    color = RokidDesign.Colors.Primary.copy(alpha = 0.15f),
                    shape = RokidDesign.Shapes.Medium
                )
                .padding(
                    horizontal = RokidDesign.Dimensions.SpacingLg,
                    vertical = RokidDesign.Dimensions.SpacingMd
                )
        ) {
            Text(
                text = "\"$text\"",
                style = RokidDesign.Typography.Body2.copy(
                    color = RokidDesign.Colors.Primary,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

/**
 * 状态标签
 */
@Composable
fun StatusLabel(
    text: String,
    isActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isActive) {
        RokidDesign.Colors.Warning.copy(alpha = 0.2f)
    } else {
        Color.Transparent
    }
    val textColor = if (isActive) {
        RokidDesign.Colors.Warning
    } else {
        RokidDesign.Colors.OnBackgroundLight
    }
    
    Text(
        text = text,
        style = RokidDesign.Typography.Caption.copy(
            color = textColor,
            fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
        ),
        modifier = modifier
            .background(backgroundColor, RokidDesign.Shapes.Small)
            .padding(horizontal = RokidDesign.Dimensions.SpacingSm, vertical = RokidDesign.Dimensions.SpacingXs)
    )
}

/**
 * 安全区域容器 - 确保内容在可视范围内
 */
@Composable
fun SafeAreaContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(
                top = RokidDesign.Dimensions.SafeAreaTop,
                bottom = RokidDesign.Dimensions.SafeAreaBottom,
                start = RokidDesign.Dimensions.SafeAreaHorizontal,
                end = RokidDesign.Dimensions.SafeAreaHorizontal
            ),
        content = content
    )
}
