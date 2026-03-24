package com.rokid.nutrition.phone.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rokid.nutrition.phone.ui.theme.*

/**
 * 调试模式区域组件
 * 
 * 可折叠的卡片，包含调试日志和演示模式两个选项
 */
@Composable
fun DebugModeSection(
    onDebugLogClick: () -> Unit,
    onDemoModeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            // 标题栏 - 可点击展开/折叠
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 图标
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .shadow(2.dp, RoundedCornerShape(10.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.DeveloperMode,
                        contentDescription = null,
                        tint = AppleOrange,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // 标题
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "调试模式",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "开发者工具和测试功能",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppleGray1
                    )
                }
                
                // 展开/折叠指示器
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (isExpanded) "折叠" else "展开",
                    tint = AppleGray2
                )
            }
            
            // 展开内容
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    Divider(color = AppleGray5, thickness = 0.5.dp)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 调试日志选项
                    DebugOptionItem(
                        icon = Icons.Rounded.BugReport,
                        title = "调试日志",
                        description = "查看应用运行日志",
                        iconTint = AppleBlue,
                        onClick = onDebugLogClick
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 演示模式选项
                    DebugOptionItem(
                        icon = Icons.Rounded.Science,
                        title = "演示模式",
                        description = "模拟眼镜端食物识别",
                        iconTint = AppleTeal,
                        onClick = onDemoModeClick
                    )
                }
            }
        }
    }
}

/**
 * 调试选项项
 */
@Composable
private fun DebugOptionItem(
    icon: ImageVector,
    title: String,
    description: String,
    iconTint: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppleGray1
                )
            }
            
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = AppleGray3,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
