package com.rokid.nutrition.phone.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rokid.nutrition.phone.network.model.MealAdviceResponse
import com.rokid.nutrition.phone.network.model.MealSummaryResponse
import com.rokid.nutrition.phone.network.model.NextMealSuggestion

/**
 * 用餐结束建议弹窗
 * 
 * 显示用餐总结、详细建议和下一餐建议
 */
@Composable
fun MealEndAdviceDialog(
    mealSummary: MealSummaryResponse?,
    advice: MealAdviceResponse?,
    nextMealSuggestion: NextMealSuggestion?,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // 标题
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "用餐总结",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                // 用餐总结卡片
                if (mealSummary != null) {
                    MealSummaryCard(mealSummary)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // 详细建议
                if (advice != null) {
                    AdviceCard(advice)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // 下一餐建议
                if (nextMealSuggestion != null) {
                    NextMealCard(nextMealSuggestion)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // 确认按钮
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text("知道了", fontSize = 16.sp)
                }
            }
        }
    }
}

/**
 * 用餐总结卡片
 */
@Composable
private fun MealSummaryCard(summary: MealSummaryResponse) {
    val ratingColor = when (summary.rating.lowercase()) {
        "good" -> Color(0xFF4CAF50)  // 绿色
        "fair" -> Color(0xFFFF9800)  // 橙色
        "poor" -> Color(0xFFFF5722)  // 红色
        else -> Color.Gray
    }
    val ratingText = when (summary.rating.lowercase()) {
        "good" -> "优秀"
        "fair" -> "一般"
        "poor" -> "需改进"
        else -> summary.rating
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ratingColor.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 评级
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "本餐评级",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(ratingColor)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = ratingText,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 营养数据
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NutrientColumn("热量", "${summary.totalCalories.toInt()}", "kcal", Color(0xFFFF6B6B))
                NutrientColumn("蛋白质", String.format("%.1f", summary.totalProtein), "g", Color(0xFF4ECDC4))
                NutrientColumn("碳水", String.format("%.1f", summary.totalCarbs), "g", Color(0xFFFFE66D))
                NutrientColumn("脂肪", String.format("%.1f", summary.totalFat), "g", Color(0xFF95E1D3))
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 用餐时长
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "用餐时长: ${summary.durationMinutes.toInt()} 分钟",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            
            // 简短建议
            if (summary.shortAdvice.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = summary.shortAdvice,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = ratingColor
                )
            }
        }
    }
}


/**
 * 营养素列
 */
@Composable
private fun NutrientColumn(label: String, value: String, unit: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
        Text(text = unit, fontSize = 10.sp, color = Color.Gray)
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
    }
}

/**
 * 详细建议卡片
 */
@Composable
private fun AdviceCard(advice: MealAdviceResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2196F3).copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 总结
            if (advice.summary.isNotBlank()) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF2196F3)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = advice.summary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // 亮点
            if (advice.highlights.isNotEmpty()) {
                Text(
                    text = "✨ 亮点",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
                Spacer(modifier = Modifier.height(4.dp))
                advice.highlights.forEach { highlight ->
                    Text(
                        text = "• $highlight",
                        fontSize = 14.sp,
                        color = Color(0xFF4CAF50)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // 建议
            if (advice.suggestions.isNotEmpty()) {
                Text(
                    text = "💡 建议",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3)
                )
                Spacer(modifier = Modifier.height(4.dp))
                advice.suggestions.forEach { suggestion ->
                    Text(
                        text = "• $suggestion",
                        fontSize = 14.sp,
                        color = Color(0xFF2196F3)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // 警告
            if (advice.warnings.isNotEmpty()) {
                Text(
                    text = "⚠️ 注意",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF9800)
                )
                Spacer(modifier = Modifier.height(4.dp))
                advice.warnings.forEach { warning ->
                    Text(
                        text = "• $warning",
                        fontSize = 14.sp,
                        color = Color(0xFFFF9800)
                    )
                }
            }
        }
    }
}

/**
 * 下一餐建议卡片
 */
@Composable
private fun NextMealCard(nextMeal: NextMealSuggestion) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF9C27B0).copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Restaurant,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFF9C27B0)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "下一餐建议",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9C27B0)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 推荐时间和餐次
            Row {
                Text(
                    text = "${nextMeal.mealType}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = " · ${nextMeal.recommendedTime}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            
            // 热量预算
            Text(
                text = "建议热量: ${nextMeal.calorieBudget.toInt()} kcal",
                fontSize = 14.sp,
                color = Color.Gray
            )
            
            // 重点营养素
            if (nextMeal.focusNutrients.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "重点补充: ${nextMeal.focusNutrients.joinToString("、")}",
                    fontSize = 14.sp,
                    color = Color(0xFF4CAF50)
                )
            }
            
            // 避免食物
            if (nextMeal.avoid.isNotEmpty()) {
                Text(
                    text = "建议避免: ${nextMeal.avoid.joinToString("、")}",
                    fontSize = 14.sp,
                    color = Color(0xFFFF9800)
                )
            }
        }
    }
}
