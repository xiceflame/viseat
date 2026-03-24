package com.rokid.nutrition.phone.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rokid.nutrition.phone.ui.theme.*
import com.rokid.nutrition.phone.ui.util.*

// ==================== 健康目标提醒卡片 ====================

/**
 * 健康目标提醒卡片
 * 显示每日进度和激励信息
 */
@Composable
fun GoalReminderCard(
    healthGoal: String?,
    currentCalories: Double,
    targetCalories: Int,
    currentWeight: Float?,
    targetWeight: Float?,
    startWeight: Float?,
    modifier: Modifier = Modifier
) {
    if (healthGoal == null) return
    
    val goalType = HealthGoalType.fromKey(healthGoal)
    val calorieProgress = if (targetCalories > 0) (currentCalories / targetCalories * 100).toInt() else 0
    
    // 计算体重进度
    val weightProgress = if (currentWeight != null && targetWeight != null && startWeight != null) {
        calculateWeightGoalProgress(startWeight, currentWeight, targetWeight, healthGoal)
    } else null
    
    // 生成激励信息
    val motivationalMessage = getMotivationalMessage(goalType, calorieProgress, weightProgress)
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = goalType.color.copy(alpha = 0.1f),
                spotColor = goalType.color.copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(goalType.color.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = goalType.icon,
                        contentDescription = null,
                        tint = goalType.color,
                        modifier = Modifier.size(22.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "今日${goalType.displayName}目标",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "热量进度 $calorieProgress%",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppleGray1
                    )
                }
                
                // 进度徽章
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = goalType.color.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = goalType.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = goalType.color,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 进度条
            LinearProgressIndicator(
                progress = (calorieProgress / 100f).coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = goalType.color,
                trackColor = AppleGray5
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 激励信息
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = goalType.color.copy(alpha = 0.08f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = goalType.color,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = motivationalMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = goalType.color.copy(alpha = 0.9f)
                    )
                }
            }
            
            // 体重进度（如果有）
            if (weightProgress != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MonitorWeight,
                        contentDescription = null,
                        tint = AppleGray1,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "体重目标进度: ${weightProgress.toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppleGray1
                    )
                }
            }
        }
    }
}

/**
 * 获取激励信息
 */
private fun getMotivationalMessage(
    goalType: HealthGoalType,
    calorieProgress: Int,
    weightProgress: Float?
): String {
    return when {
        calorieProgress >= 100 -> when (goalType) {
            HealthGoalType.LOSE_WEIGHT -> "今日热量已达标，注意控制哦！"
            HealthGoalType.GAIN_MUSCLE -> "热量摄入充足，继续保持！"
            HealthGoalType.MAINTAIN -> "今日热量已达标，很棒！"
        }
        calorieProgress >= 80 -> when (goalType) {
            HealthGoalType.LOSE_WEIGHT -> "接近目标了，选择轻食更健康"
            HealthGoalType.GAIN_MUSCLE -> "还差一点，加油补充能量！"
            HealthGoalType.MAINTAIN -> "接近目标，继续保持！"
        }
        calorieProgress >= 50 -> "进度不错，继续加油！"
        else -> "新的一天，为健康目标努力！"
    }
}

// ==================== 热量警告提示 ====================

/**
 * 热量警告提示组件
 * 当摄入接近目标时显示
 */
@Composable
fun CalorieWarningTip(
    currentCalories: Double,
    targetCalories: Int,
    warningThreshold: Float = 0.8f,
    modifier: Modifier = Modifier
) {
    val shouldShow = shouldShowCalorieWarning(currentCalories, targetCalories.toDouble(), warningThreshold)
    val isExceeded = currentCalories >= targetCalories
    
    AnimatedVisibility(
        visible = shouldShow,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = if (isExceeded) AppleRed.copy(alpha = 0.1f) else AppleOrange.copy(alpha = 0.1f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isExceeded) Icons.Rounded.Warning else Icons.Rounded.Info,
                    contentDescription = null,
                    tint = if (isExceeded) AppleRed else AppleOrange,
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isExceeded) "热量已超标" else "接近热量目标",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = if (isExceeded) AppleRed else AppleOrange
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isExceeded) {
                            "已超出 ${(currentCalories - targetCalories).toInt()} kcal，建议增加运动"
                        } else {
                            "建议选择低热量食物，如沙拉、水果"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isExceeded) AppleRed.copy(alpha = 0.8f) else AppleOrange.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

// ==================== 饮食限制提示 ====================

/**
 * 饮食限制提示组件
 * 根据用户的过敏原和饮食类型显示相关提示
 */
@Composable
fun DietaryRestrictionTips(
    dietType: String?,
    allergens: List<String>,
    modifier: Modifier = Modifier
) {
    val tips = remember(dietType, allergens) {
        buildList {
            // 饮食类型提示
            when (dietType) {
                "vegetarian" -> add(DietTip(Icons.Rounded.Eco, "素食饮食", "避免肉类，多吃蔬菜和豆制品", AppleTeal))
                "vegan" -> add(DietTip(Icons.Rounded.Eco, "纯素饮食", "避免所有动物制品", AppleTeal))
                "low_carb" -> add(DietTip(Icons.Rounded.Grain, "低碳水饮食", "减少主食，增加蛋白质摄入", AppleBlue))
                "keto" -> add(DietTip(Icons.Rounded.LocalFireDepartment, "生酮饮食", "高脂肪、极低碳水，注意补充电解质", AppleOrange))
                "mediterranean" -> add(DietTip(Icons.Rounded.Restaurant, "地中海饮食", "多吃鱼类、橄榄油和全谷物", AppleMint))
            }
            
            // 过敏原提示
            allergens.forEach { allergen ->
                when (allergen) {
                    "gluten" -> add(DietTip(Icons.Rounded.Warning, "麸质过敏", "避免小麦、大麦、黑麦制品", AppleRed))
                    "dairy" -> add(DietTip(Icons.Rounded.Warning, "乳制品过敏", "避免牛奶、奶酪、酸奶", AppleRed))
                    "nuts" -> add(DietTip(Icons.Rounded.Warning, "坚果过敏", "避免花生、杏仁、核桃等", AppleRed))
                    "shellfish" -> add(DietTip(Icons.Rounded.Warning, "海鲜过敏", "避免虾、蟹、贝类", AppleRed))
                    "eggs" -> add(DietTip(Icons.Rounded.Warning, "鸡蛋过敏", "避免鸡蛋及含蛋制品", AppleRed))
                    "soy" -> add(DietTip(Icons.Rounded.Warning, "大豆过敏", "避免豆腐、豆浆、酱油", AppleRed))
                }
            }
        }
    }
    
    if (tips.isEmpty()) return
    
    Column(modifier = modifier.fillMaxWidth()) {
        tips.forEachIndexed { index, tip ->
            if (index > 0) Spacer(modifier = Modifier.height(8.dp))
            
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = tip.color.copy(alpha = 0.08f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(tip.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tip.icon,
                            contentDescription = null,
                            tint = tip.color,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tip.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = tip.color
                        )
                        Text(
                            text = tip.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = tip.color.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 饮食提示数据类
 */
private data class DietTip(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val color: Color
)

// ==================== 健康提示卡片（综合） ====================

/**
 * 健康提示卡片（综合显示所有提示）
 */
@Composable
fun HealthTipsCard(
    healthGoal: String?,
    currentCalories: Double,
    targetCalories: Int,
    currentWeight: Float?,
    targetWeight: Float?,
    startWeight: Float?,
    dietType: String?,
    allergens: List<String>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 健康目标提醒
        if (healthGoal != null) {
            GoalReminderCard(
                healthGoal = healthGoal,
                currentCalories = currentCalories,
                targetCalories = targetCalories,
                currentWeight = currentWeight,
                targetWeight = targetWeight,
                startWeight = startWeight
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        // 热量警告
        CalorieWarningTip(
            currentCalories = currentCalories,
            targetCalories = targetCalories
        )
        
        // 饮食限制提示
        if (dietType != null || allergens.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            DietaryRestrictionTips(
                dietType = dietType,
                allergens = allergens
            )
        }
    }
}
