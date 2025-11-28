package com.rokid.nutrition.phone.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rokid.nutrition.phone.util.DebugLogger

/**
 * 调试日志显示界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugLogScreen(
    onBack: () -> Unit
) {
    val logs by DebugLogger.logs.collectAsState()
    val listState = rememberLazyListState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("调试日志") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { DebugLogger.clearUiLogs() }) {
                        Icon(Icons.Default.Delete, contentDescription = "清除")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 日志文件路径
            DebugLogger.getLogFilePath()?.let { path ->
                Text(
                    text = "日志文件: $path",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            
            Divider()
            
            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无日志", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(logs) { log ->
                        LogItem(log)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogItem(log: DebugLogger.LogEntry) {
    val backgroundColor = when (log.level) {
        "E" -> Color(0x20FF0000)  // 红色半透明
        "W" -> Color(0x20FFA500)  // 橙色半透明
        "NET" -> Color(0x200000FF)  // 蓝色半透明
        else -> Color.Transparent
    }
    
    val textColor = when (log.level) {
        "E" -> Color(0xFFCC0000)
        "W" -> Color(0xFFCC7700)
        "NET" -> Color(0xFF0066CC)
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "[${log.level}] ${log.tag}",
                fontSize = 10.sp,
                color = textColor,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = log.timestamp.takeLast(12),  // 只显示时间部分
                fontSize = 10.sp,
                color = Color.Gray,
                fontFamily = FontFamily.Monospace
            )
        }
        Text(
            text = log.message,
            fontSize = 12.sp,
            color = textColor,
            fontFamily = FontFamily.Monospace,
            lineHeight = 14.sp
        )
        Divider(
            modifier = Modifier.padding(top = 4.dp),
            color = Color.LightGray.copy(alpha = 0.3f)
        )
    }
}
