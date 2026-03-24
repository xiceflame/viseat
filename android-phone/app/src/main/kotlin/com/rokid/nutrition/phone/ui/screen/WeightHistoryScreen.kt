@file:OptIn(ExperimentalMaterial3Api::class)

package com.rokid.nutrition.phone.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rokid.nutrition.phone.data.entity.WeightEntryEntity
import com.rokid.nutrition.phone.ui.component.AddWeightDialog
import com.rokid.nutrition.phone.ui.component.EditWeightDialog
import com.rokid.nutrition.phone.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * 体重历史页面
 */
@Composable
fun WeightHistoryScreen(
    entries: List<WeightEntryEntity>,
    targetWeight: Float?,
    startWeight: Float?,
    onAddWeight: (Float, String?) -> Unit,
    onUpdateWeight: (String, Float, String?) -> Unit,
    onDeleteWeight: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<WeightEntryEntity?>(null) }
    
    val currentWeight = entries.firstOrNull()?.weight
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("体重记录") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = "添加记录")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = AppleBlue
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "添加记录", tint = Color.White)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 统计卡片
            item {
                WeightStatsCard(
                    entries = entries,
                    targetWeight = targetWeight,
                    startWeight = startWeight
                )
            }
            
            // 趋势图
            if (entries.size >= 2) {
                item {
                    WeightTrendChart(
                        entries = entries.take(30).reversed(),
                        targetWeight = targetWeight
                    )
                }
            }
            
            // 记录列表标题
            item {
                Text(
                    text = "历史记录",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            // 记录列表
            if (entries.isEmpty()) {
                item {
                    EmptyHistoryState(onAddWeight = { showAddDialog = true })
                }
            } else {
                items(entries) { entry ->
                    WeightRecordItem(
                        entry = entry,
                        previousEntry = entries.getOrNull(entries.indexOf(entry) + 1),
                        onClick = { editingEntry = entry }
                    )
                }
            }
        }
    }
    
    // 添加对话框
    if (showAddDialog) {
        AddWeightDialog(
            currentWeight = currentWeight,
            onDismiss = { showAddDialog = false },
            onConfirm = { weight, note ->
                onAddWeight(weight, note)
                showAddDialog = false
            }
        )
    }
    
    // 编辑对话框
    editingEntry?.let { entry ->
        EditWeightDialog(
            weight = entry.weight,
            note = entry.note,
            onDismiss = { editingEntry = null },
            onConfirm = { weight, note ->
                onUpdateWeight(entry.id, weight, note)
                editingEntry = null
            },
            onDelete = {
                onDeleteWeight(entry.id)
                editingEntry = null
            }
        )
    }
}

/**
 * 体重统计卡片
 */
@Composable
private fun WeightStatsCard(
    entries: List<WeightEntryEntity>,
    targetWeight: Float?,
    startWeight: Float?
) {
    val currentWeight = entries.firstOrNull()?.weight
    val weekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
    val weekEntries = entries.filter { it.recordedAt >= weekAgo }
    val weekChange = if (weekEntries.size >= 2) {
        weekEntries.first().weight - weekEntries.last().weight
    } else null
    
    val totalChange = if (currentWeight != null && startWeight != null) {
        currentWeight - startWeight
    } else null
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "体重统计",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 当前体重
                StatItem(
                    label = "当前",
                    value = currentWeight?.let { "${String.format("%.1f", it)} kg" } ?: "--",
                    color = AppleBlue
                )
                
                // 本周变化
                StatItem(
                    label = "本周",
                    value = weekChange?.let { 
                        "${if (it >= 0) "+" else ""}${String.format("%.1f", it)} kg"
                    } ?: "--",
                    color = weekChange?.let { if (it < 0) AppleTeal else AppleOrange } ?: AppleGray2
                )
                
                // 总变化
                StatItem(
                    label = "总计",
                    value = totalChange?.let {
                        "${if (it >= 0) "+" else ""}${String.format("%.1f", it)} kg"
                    } ?: "--",
                    color = totalChange?.let { if (it < 0) AppleTeal else AppleOrange } ?: AppleGray2
                )
                
                // 记录次数
                StatItem(
                    label = "记录",
                    value = "${entries.size} 次",
                    color = ApplePurple
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AppleGray1
        )
    }
}

/**
 * 体重趋势图
 */
@Composable
private fun WeightTrendChart(
    entries: List<WeightEntryEntity>,
    targetWeight: Float?
) {
    if (entries.isEmpty()) return
    
    val minWeight = (entries.minOfOrNull { it.weight } ?: 0f) - 2f
    val maxWeight = (entries.maxOfOrNull { it.weight } ?: 100f) + 2f
    val weightRange = maxWeight - minWeight
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "体重趋势",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                val width = size.width
                val height = size.height
                val padding = 40f
                val chartWidth = width - padding * 2
                val chartHeight = height - padding
                
                // 绘制目标线
                targetWeight?.let { target ->
                    val targetY = chartHeight - ((target - minWeight) / weightRange * chartHeight) + padding / 2
                    drawLine(
                        color = AppleTeal.copy(alpha = 0.5f),
                        start = Offset(padding, targetY),
                        end = Offset(width - padding, targetY),
                        strokeWidth = 2f,
                        cap = StrokeCap.Round
                    )
                }
                
                // 绘制趋势线
                if (entries.size >= 2) {
                    val path = Path()
                    entries.forEachIndexed { index, entry ->
                        val x = padding + (index.toFloat() / (entries.size - 1)) * chartWidth
                        val y = chartHeight - ((entry.weight - minWeight) / weightRange * chartHeight) + padding / 2
                        
                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    
                    drawPath(
                        path = path,
                        color = AppleBlue,
                        style = Stroke(width = 3f, cap = StrokeCap.Round)
                    )
                }
                
                // 绘制数据点
                entries.forEachIndexed { index, entry ->
                    val x = padding + (index.toFloat() / (entries.size - 1).coerceAtLeast(1)) * chartWidth
                    val y = chartHeight - ((entry.weight - minWeight) / weightRange * chartHeight) + padding / 2
                    
                    drawCircle(
                        color = AppleBlue,
                        radius = 6f,
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3f,
                        center = Offset(x, y)
                    )
                }
            }
            
            // 图例
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(AppleBlue)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "实际体重",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppleGray1
                )
                
                if (targetWeight != null) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(AppleTeal.copy(alpha = 0.5f))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "目标体重",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppleGray1
                    )
                }
            }
        }
    }
}

/**
 * 单条体重记录
 */
@Composable
private fun WeightRecordItem(
    entry: WeightEntryEntity,
    previousEntry: WeightEntryEntity?,
    onClick: () -> Unit
) {
    val change = previousEntry?.let { entry.weight - it.weight }
    val changeColor = change?.let { if (it < 0) AppleTeal else if (it > 0) AppleOrange else AppleGray2 } ?: AppleGray2
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${String.format("%.1f", entry.weight)} kg",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = formatRecordDate(entry.recordedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppleGray1
                )
                
                entry.note?.let { note ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppleGray2
                    )
                }
            }
            
            // 变化指示
            change?.let { c ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        when {
                            c > 0 -> Icons.Rounded.TrendingUp
                            c < 0 -> Icons.Rounded.TrendingDown
                            else -> Icons.Rounded.TrendingFlat
                        },
                        contentDescription = null,
                        tint = changeColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${if (c >= 0) "+" else ""}${String.format("%.1f", c)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = changeColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = AppleGray3,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 空状态
 */
@Composable
private fun EmptyHistoryState(onAddWeight: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.MonitorWeight,
            contentDescription = null,
            tint = AppleGray3,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "暂无体重记录",
            style = MaterialTheme.typography.titleMedium,
            color = AppleGray1
        )
        Text(
            text = "开始记录体重，追踪健康变化",
            style = MaterialTheme.typography.bodySmall,
            color = AppleGray2,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onAddWeight,
            colors = ButtonDefaults.buttonColors(containerColor = AppleBlue),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("添加第一条记录")
        }
    }
}

/**
 * 格式化日期
 */
private fun formatRecordDate(timestamp: Long): String {
    val date = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    val today = LocalDate.now()
    
    return when {
        date == today -> "今天"
        date == today.minusDays(1) -> "昨天"
        date.year == today.year -> date.format(DateTimeFormatter.ofPattern("MM月dd日"))
        else -> date.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))
    }
}
