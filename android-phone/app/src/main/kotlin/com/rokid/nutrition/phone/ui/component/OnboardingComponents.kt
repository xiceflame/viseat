@file:OptIn(ExperimentalMaterial3Api::class)

package com.rokid.nutrition.phone.ui.component

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rokid.nutrition.phone.domain.BMIStatus
import com.rokid.nutrition.phone.domain.NutritionCalculator
import com.rokid.nutrition.phone.ui.theme.*
import kotlin.math.abs

/**
 * 进度指示器
 */
@Composable
fun OnboardingProgressIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(totalSteps) { index ->
            val isCompleted = index < currentStep
            val isCurrent = index == currentStep
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        when {
                            isCompleted -> AppleTeal
                            isCurrent -> AppleTeal.copy(alpha = 0.5f)
                            else -> AppleGray5
                        }
                    )
            )
        }
    }
}

/**
 * 步骤标题
 */
@Composable
fun StepHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 产品能力展示
 */
@Composable
fun CapabilityHighlight(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = AppleTeal.copy(alpha = 0.1f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppleTeal,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = AppleTeal
            )
        }
    }
}

/**
 * 导航按钮
 */
@Composable
fun NavigationButtons(
    onBack: (() -> Unit)? = null,
    onNext: () -> Unit,
    nextEnabled: Boolean = true,
    nextText: String = "下一步",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (onBack != null) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("上一步")
            }
        }
        
        Button(
            onClick = onNext,
            enabled = nextEnabled,
            modifier = Modifier.weight(if (onBack != null) 1f else 1f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppleTeal)
        ) {
            Text(nextText)
        }
    }
}

/**
 * 目标选择卡片
 */
@Composable
fun GoalCard(
    icon: ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        if (isSelected) AppleTeal.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        label = "bgColor"
    )
    val contentColor by animateColorAsState(
        if (isSelected) AppleTeal else MaterialTheme.colorScheme.onSurface,
        label = "contentColor"
    )
    
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = if (isSelected) AppleTeal else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) AppleTeal else AppleGray5),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else AppleGray1,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = AppleTeal,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// 性别特定颜色
private val MaleColor = Color(0xFF4A90D9)      // 清爷蓝
private val MaleColorLight = Color(0xFFE8F2FC) // 浅蓝背景
private val FemaleColor = Color(0xFFE88B9C)    // 温柔粉
private val FemaleColorLight = Color(0xFFFCEEF0) // 浅粉背景

/**
 * 性别选择卡片 - 带性别特定颜色
 */
@Composable
fun GenderCard(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isMale: Boolean = true  // 新增参数区分性别
) {
    // 根据性别选择颜色
    val genderColor = if (isMale) MaleColor else FemaleColor
    val genderColorLight = if (isMale) MaleColorLight else FemaleColorLight
    
    val backgroundColor by animateColorAsState(
        if (isSelected) genderColorLight else Color(0xFFF8F8F8),
        label = "bgColor",
        animationSpec = tween(durationMillis = 200)
    )
    val contentColor by animateColorAsState(
        if (isSelected) genderColor else AppleGray1,
        label = "contentColor",
        animationSpec = tween(durationMillis = 200)
    )
    val borderColor by animateColorAsState(
        if (isSelected) genderColor else Color(0xFFE5E5E5),
        label = "borderColor",
        animationSpec = tween(durationMillis = 200)
    )
    
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        onClick = onClick,
        modifier = modifier
            .shadow(
                elevation = if (isSelected) 6.dp else 2.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = if (isSelected) genderColor.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.05f),
                spotColor = if (isSelected) genderColor.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 24.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 性别图标容器 - 带性别特定颜色
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        color = if (isSelected) genderColor else Color(0xFFEEEEEE)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else AppleGray2,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

/**
 * BMI 指示器
 */
@Composable
fun BMIIndicator(
    bmi: Float,
    modifier: Modifier = Modifier
) {
    val status = NutritionCalculator.getBMIStatus(bmi)
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = status.color.copy(alpha = 0.1f),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "BMI",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = String.format("%.1f", bmi),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = status.color
                )
                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = status.color
                ) {
                    Text(
                        text = status.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // BMI 范围条
            BMIRangeBar(currentBMI = bmi)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = status.suggestion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * BMI 范围条
 */
@Composable
private fun BMIRangeBar(
    currentBMI: Float,
    modifier: Modifier = Modifier
) {
    val minBMI = 15f
    val maxBMI = 35f
    val normalizedPosition = ((currentBMI - minBMI) / (maxBMI - minBMI)).coerceIn(0f, 1f)
    
    Box(modifier = modifier.fillMaxWidth()) {
        // 背景条
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        ) {
            // 偏瘦区域 (< 18.5)
            Box(
                modifier = Modifier
                    .weight((18.5f - minBMI) / (maxBMI - minBMI))
                    .fillMaxHeight()
                    .background(BMIStatus.UNDERWEIGHT.color)
            )
            // 正常区域 (18.5 - 24)
            Box(
                modifier = Modifier
                    .weight((24f - 18.5f) / (maxBMI - minBMI))
                    .fillMaxHeight()
                    .background(BMIStatus.NORMAL.color)
            )
            // 偏胖区域 (24 - 28)
            Box(
                modifier = Modifier
                    .weight((28f - 24f) / (maxBMI - minBMI))
                    .fillMaxHeight()
                    .background(BMIStatus.OVERWEIGHT.color)
            )
            // 肥胖区域 (> 28)
            Box(
                modifier = Modifier
                    .weight((maxBMI - 28f) / (maxBMI - minBMI))
                    .fillMaxHeight()
                    .background(BMIStatus.OBESE.color)
            )
        }
        
        // 当前位置指示器
        Box(
            modifier = Modifier
                .fillMaxWidth(normalizedPosition)
                .align(Alignment.CenterStart)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 8.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, NutritionCalculator.getBMIStatus(currentBMI).color, CircleShape)
            )
        }
    }
}

/**
 * 活动量选择卡片
 */
@Composable
fun ActivityLevelCard(
    icon: ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        if (isSelected) AppleTeal.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        label = "bgColor"
    )
    val contentColor by animateColorAsState(
        if (isSelected) AppleTeal else MaterialTheme.colorScheme.onSurface,
        label = "contentColor"
    )
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = if (isSelected) AppleTeal else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) AppleTeal else AppleGray5),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else AppleGray1,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = AppleTeal,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * 饮食偏好选择卡片（更精致的 FilterChip 替代品）
 */
@Composable
fun PreferenceChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        if (isSelected) AppleTeal.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        label = "bgColor"
    )
    val contentColor by animateColorAsState(
        if (isSelected) AppleTeal else AppleGray2,
        label = "contentColor"
    )
    val borderColor by animateColorAsState(
        if (isSelected) AppleTeal else Color.Transparent,
        label = "borderColor"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        onClick = onClick,
        modifier = modifier
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

/**
 * 激励文案
 */
@Composable
fun MotivationalMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = AppleMint.copy(alpha = 0.2f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Lightbulb,
                contentDescription = null,
                tint = AppleTeal,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * 安全警告
 */
@Composable
fun SafetyWarning(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = AppleOrange.copy(alpha = 0.1f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Rounded.Warning,
                contentDescription = null,
                tint = AppleOrange,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = AppleOrange
            )
        }
    }
}

/**
 * 宏量营养素显示项
 */
@Composable
fun MacroItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
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
 * 每日热量目标卡片
 */
@Composable
fun DailyCaloriesCard(
    dailyCalories: Int,
    bmr: Int,
    activityCalories: Int,
    goalAdjustment: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = AppleTeal.copy(alpha = 0.1f),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "建议每日摄入",
                style = MaterialTheme.typography.labelLarge,
                color = AppleGray1
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = dailyCalories.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppleTeal
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "kcal",
                    style = MaterialTheme.typography.titleMedium,
                    color = AppleGray2,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Divider(color = AppleGray5, thickness = 1.dp)
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                CalorieDetailItem("基础代谢", bmr, AppleGray1)
                CalorieDetailItem("日常活动", activityCalories, AppleTeal, prefix = "+")
                if (goalAdjustment != 0) {
                    CalorieDetailItem(
                        if (goalAdjustment < 0) "目标缺口" else "目标盈余",
                        abs(goalAdjustment),
                        if (goalAdjustment < 0) AppleOrange else AppleTeal,
                        prefix = if (goalAdjustment < 0) "-" else "+"
                    )
                }
            }
        }
    }
}

@Composable
private fun CalorieDetailItem(
    label: String,
    value: Int,
    color: Color,
    prefix: String = ""
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$prefix$value",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AppleGray2
        )
    }
}
