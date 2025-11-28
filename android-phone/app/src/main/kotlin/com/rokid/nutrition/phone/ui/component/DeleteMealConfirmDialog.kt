package com.rokid.nutrition.phone.ui.component

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.rokid.nutrition.phone.data.entity.MealSessionEntity
import java.text.SimpleDateFormat
import java.util.*

/**
 * 删除用餐记录确认对话框
 */
@Composable
fun DeleteMealConfirmDialog(
    session: MealSessionEntity,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    val timeStr = dateFormat.format(Date(session.startTime))
    val calories = (session.totalConsumedKcal ?: session.totalServedKcal ?: 0.0).toInt()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除用餐记录") },
        text = { 
            Text("确定要删除这条用餐记录吗？\n\n时间: $timeStr\n热量: $calories kcal\n\n此操作无法撤销。") 
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("删除", color = Color(0xFFE53935))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
