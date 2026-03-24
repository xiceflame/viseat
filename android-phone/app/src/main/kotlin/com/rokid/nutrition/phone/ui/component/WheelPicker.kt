package com.rokid.nutrition.phone.ui.component

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rokid.nutrition.phone.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt

// ==================== 刻度尺选择器 (RulerPicker) ====================

/**
 * 水平刻度尺选择器
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RulerPicker(
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    step: Float = 1f,
    modifier: Modifier = Modifier,
    lineColor: Color = AppleGray3,
    highlightColor: Color = AppleTeal
) {
    val density = LocalDensity.current
    val itemWidth = 12.dp
    
    val totalSteps = ((range.endInclusive - range.start) / step).roundToInt()
    val initialIndex = ((value - range.start) / step).roundToInt()
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val view = LocalView.current

    // 监听滚动，更新值
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val centerIndex = listState.firstVisibleItemIndex
            val newValue = range.start + centerIndex * step
            if (abs(newValue - value) > 0.01f) {
                onValueChange(newValue)
                view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val centerPadding = maxWidth / 2
        
        LazyRow(
            state = listState,
            flingBehavior = snapFlingBehavior,
            contentPadding = PaddingValues(horizontal = centerPadding),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            items(totalSteps + 1) { index ->
                val currentValue = range.start + index * step
                val isMajor = index % 5 == 0
                val isTen = index % 10 == 0
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(itemWidth)
                ) {
                    if (isTen) {
                        Text(
                            text = currentValue.roundToInt().toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (abs(currentValue - value) < step / 2) highlightColor else AppleGray2,
                            fontWeight = if (abs(currentValue - value) < step / 2) FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    } else {
                        Box(modifier = Modifier.height(16.dp))
                    }
                    
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(if (isMajor) 24.dp else 12.dp)
                            .background(
                                color = if (abs(currentValue - value) < step / 2) highlightColor else lineColor,
                                shape = RoundedCornerShape(1.dp)
                            )
                    )
                }
            }
        }
        
        // 中心指示器
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .width(2.dp)
                .height(32.dp)
                .background(highlightColor, RoundedCornerShape(1.dp))
        )
    }
}

// ==================== 基础滚轮选择器 ====================

/**
 * 基础滚轮选择器组件 (类似 iOS Picker)
 * 
 * @param items 选项列表
 * @param selectedIndex 当前选中的索引
 * @param onSelectedIndexChange 选中索引变化回调
 * @param modifier Modifier
 * @param visibleItemCount 可见项数量（奇数）
 * @param itemHeight 每项高度
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleItemCount: Int = 5,
    itemHeight: Dp = 44.dp
) {
    val view = LocalView.current
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedIndex
    )
    val coroutineScope = rememberCoroutineScope()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val isDragged by listState.interactionSource.collectIsDraggedAsState()
    
    val centerOffset = visibleItemCount / 2
    
    // 监听滚动位置，实时更新选中项
    LaunchedEffect(listState, isDragged) {
        snapshotFlow { listState.layoutInfo }
            .collect { layoutInfo ->
                // 关键点：只有在用户手动拖拽或惯性滚动时（有交互源），才反馈回外部
                // animateScrollToItem 不会触发 interactionSource 的相关事件
                if (isDragged || listState.isScrollInProgress) {
                    // 其实更简单的方法是：只要正在进行动画（即外部驱动的滚动），我们就保持沉默
                    // 但是 animateScrollToItem 也会让 isScrollInProgress 为 true
                    // 这里的修复思路是：只有当 listState 的中心项确实变了，且是由用户触发的时才回调
                    
                    val viewportCenter = (layoutInfo.viewportEndOffset + layoutInfo.viewportStartOffset) / 2
                    val closestItem = layoutInfo.visibleItemsInfo.minByOrNull { 
                        abs((it.offset + it.size / 2) - viewportCenter)
                    }
                    
                    if (closestItem != null) {
                        val index = closestItem.index
                        if (index in items.indices && index != selectedIndex) {
                            // 核心修复：如果是拖拽状态，我们才实时更新外部值
                            // 如果只是惯性滚动中，我们也更新，但要排除掉程序触发的动画
                            if (isDragged) {
                                onSelectedIndexChange(index)
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            }
                        }
                    }
                }
            }
    }
    
    // 监听滚动停止，最终确认位置
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress && !isDragged) {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = (layoutInfo.viewportEndOffset + layoutInfo.viewportStartOffset) / 2
            val closestItem = layoutInfo.visibleItemsInfo.minByOrNull { 
                abs((it.offset + it.size / 2) - viewportCenter)
            }
            if (closestItem != null && closestItem.index != selectedIndex) {
                onSelectedIndexChange(closestItem.index)
            }
        }
    }
    
    // 外部更新 selectedIndex 时同步滚动
    LaunchedEffect(selectedIndex) {
        val currentVisible = listState.firstVisibleItemIndex
        if (currentVisible != selectedIndex) {
            listState.animateScrollToItem(selectedIndex)
        }
    }
    
    Box(
        modifier = modifier
            .height(itemHeight * visibleItemCount)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // 选中项背景 (Apple Style)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .padding(horizontal = 8.dp)
                .background(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp)
                )
        )
        
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = itemHeight * centerOffset)
        ) {
            itemsIndexed(items) { index, item ->
                val isSelected = index == selectedIndex
                
                // 计算距离中心的偏移量，用于动画效果
                val itemOffset = remember { derivedStateOf { 
                    val layoutInfo = listState.layoutInfo
                    val visibleItemInfo = layoutInfo.visibleItemsInfo.find { it.index == index }
                    if (visibleItemInfo != null) {
                        val viewportCenter = (layoutInfo.viewportEndOffset + layoutInfo.viewportStartOffset) / 2
                        val itemCenter = visibleItemInfo.offset + visibleItemInfo.size / 2
                        (itemCenter - viewportCenter).toFloat() / itemHeight.value
                    } else {
                        0f
                    }
                } }

                val alpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else (1f - (abs(itemOffset.value) * 0.3f)).coerceIn(0.2f, 0.6f),
                    label = "alpha"
                )
                
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.05f else (1f - (abs(itemOffset.value) * 0.05f)).coerceIn(0.9f, 1f),
                    label = "scale"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .alpha(alpha)
                        .scale(scale),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) AppleTeal else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        // 遮罩渐变 (Apple Style)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        0.0f to MaterialTheme.colorScheme.surface,
                        0.2f to MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        0.45f to Color.Transparent,
                        0.55f to Color.Transparent,
                        0.8f to MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        1.0f to MaterialTheme.colorScheme.surface
                    )
                )
        )
    }
}

// ==================== 身高刻度尺选择器 ====================

/**
 * 身高刻度尺选择器
 * 范围: 100-220cm
 */
@Composable
fun HeightRulerPicker(
    selectedHeight: Float,
    onHeightChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    RulerPicker(
        value = selectedHeight,
        onValueChange = onHeightChange,
        range = 100f..220f,
        step = 1f,
        modifier = modifier
    )
}

// ==================== 体重刻度尺选择器 ====================

/**
 * 体重刻度尺选择器
 * 范围: 30-200kg, 步进 0.1kg
 */
@Composable
fun WeightRulerPicker(
    selectedWeight: Float,
    onWeightChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    RulerPicker(
        value = selectedWeight,
        onValueChange = onWeightChange,
        range = 30f..200f,
        step = 0.1f,
        modifier = modifier
    )
}

// ==================== 年龄刻度尺选择器 ====================

/**
 * 年龄刻度尺选择器
 * 范围: 10-100岁
 */
@Composable
fun AgeRulerPicker(
    selectedAge: Int,
    onAgeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    RulerPicker(
        value = selectedAge.toFloat(),
        onValueChange = { onAgeChange(it.roundToInt()) },
        range = 10f..100f,
        step = 1f,
        modifier = modifier
    )
}

// ==================== 目标体重刻度尺选择器 ====================

/**
 * 目标体重刻度尺选择器
 */
@Composable
fun TargetWeightRulerPicker(
    currentWeight: Float,
    selectedTargetWeight: Float,
    onTargetWeightChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val minWeight = (currentWeight - 50f).coerceAtLeast(30f)
    val maxWeight = (currentWeight + 50f).coerceAtMost(200f)
    
    RulerPicker(
        value = selectedTargetWeight,
        onValueChange = onTargetWeightChange,
        range = minWeight..maxWeight,
        step = 0.1f,
        modifier = modifier
    )
}

/**
 * 身高滚轮选择器 (类似 iOS)
 * 范围: 100-220cm
 */
@Composable
fun HeightWheelPicker(
    selectedHeight: Float,
    onHeightChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val heights = remember { (100..220).map { "$it" } }
    val selectedIndex = (selectedHeight.roundToInt() - 100).coerceIn(0, heights.size - 1)
    
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        WheelPicker(
            items = heights,
            selectedIndex = selectedIndex,
            onSelectedIndexChange = { index ->
                onHeightChange((100 + index).toFloat())
            },
            modifier = Modifier.width(100.dp),
            visibleItemCount = 5
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "cm",
            style = MaterialTheme.typography.titleLarge,
            color = AppleGray1,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 体重滚轮选择器 (类似 iOS)
 * 范围: 30-200kg, 步进: 1kg (引导页使用整数)
 */
@Composable
fun WeightWheelPicker(
    selectedWeight: Float,
    onWeightChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val weights = remember {
        (30..200).map { "$it" }
    }
    
    val selectedIndex = (selectedWeight.roundToInt().coerceIn(30, 200) - 30).coerceIn(0, weights.size - 1)
    
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        WheelPicker(
            items = weights,
            selectedIndex = selectedIndex,
            onSelectedIndexChange = { index ->
                onWeightChange((30 + index).toFloat())
            },
            modifier = Modifier.width(100.dp),
            visibleItemCount = 5
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "kg",
            style = MaterialTheme.typography.titleLarge,
            color = AppleGray1,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 目标体重滚轮选择器 (类似 iOS)
 * 步进: 1kg (引导页使用整数)
 */
@Composable
fun TargetWeightWheelPicker(
    currentWeight: Float,
    selectedTargetWeight: Float,
    onTargetWeightChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val minWeight = (currentWeight.roundToInt() - 50).coerceAtLeast(30)
    val maxWeight = (currentWeight.roundToInt() + 50).coerceAtMost(200)
    
    val weights = remember(minWeight, maxWeight) {
        (minWeight..maxWeight).map { "$it" }
    }
    
    val selectedIndex = (selectedTargetWeight.roundToInt().coerceIn(minWeight, maxWeight) - minWeight).coerceIn(0, weights.size - 1)
    
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        WheelPicker(
            items = weights,
            selectedIndex = selectedIndex,
            onSelectedIndexChange = { index ->
                onTargetWeightChange((minWeight + index).toFloat())
            },
            modifier = Modifier.width(100.dp),
            visibleItemCount = 5
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "kg",
            style = MaterialTheme.typography.titleLarge,
            color = AppleGray1,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 日期滚轮选择器 (类似 iOS)
 * 包含年、月、日三列
 */
@Composable
fun DateWheelPicker(
    selectedYear: Int,
    selectedMonth: Int,
    selectedDay: Int,
    onDateChange: (year: Int, month: Int, day: Int) -> Unit,
    modifier: Modifier = Modifier,
    minYear: Int = 1920,
    maxYear: Int = Calendar.getInstance().get(Calendar.YEAR)
) {
    val years = remember(minYear, maxYear) { (minYear..maxYear).map { "$it" } }
    val months = remember { (1..12).map { "$it" } }
    
    val daysInMonth = remember(selectedYear, selectedMonth) {
        getDaysInMonth(selectedYear, selectedMonth)
    }
    val days = remember(daysInMonth) { (1..daysInMonth).map { "$it" } }
    
    val yearIndex = (selectedYear - minYear).coerceIn(0, years.size - 1)
    val monthIndex = (selectedMonth - 1).coerceIn(0, 11)
    val dayIndex = (selectedDay - 1).coerceIn(0, days.size - 1)
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 年份
        WheelPicker(
            items = years,
            selectedIndex = yearIndex,
            onSelectedIndexChange = { index ->
                val newYear = minYear + index
                val newDaysInMonth = getDaysInMonth(newYear, selectedMonth)
                val newDay = selectedDay.coerceAtMost(newDaysInMonth)
                onDateChange(newYear, selectedMonth, newDay)
            },
            modifier = Modifier.weight(1.2f),
            visibleItemCount = 5
        )
        Text("年", style = MaterialTheme.typography.titleMedium, color = AppleGray1, modifier = Modifier.padding(horizontal = 4.dp))
        
        // 月份
        WheelPicker(
            items = months,
            selectedIndex = monthIndex,
            onSelectedIndexChange = { index ->
                val newMonth = index + 1
                val newDaysInMonth = getDaysInMonth(selectedYear, newMonth)
                val newDay = selectedDay.coerceAtMost(newDaysInMonth)
                onDateChange(selectedYear, newMonth, newDay)
            },
            modifier = Modifier.weight(1f),
            visibleItemCount = 5
        )
        Text("月", style = MaterialTheme.typography.titleMedium, color = AppleGray1, modifier = Modifier.padding(horizontal = 4.dp))
        
        // 日期
        WheelPicker(
            items = days,
            selectedIndex = dayIndex,
            onSelectedIndexChange = { index ->
                onDateChange(selectedYear, selectedMonth, index + 1)
            },
            modifier = Modifier.weight(1f),
            visibleItemCount = 5
        )
        Text("日", style = MaterialTheme.typography.titleMedium, color = AppleGray1, modifier = Modifier.padding(horizontal = 4.dp))
    }
}

/**
 * 获取指定年月的天数
 */
private fun getDaysInMonth(year: Int, month: Int): Int {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
    }
    return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
}

/**
 * 年龄滚轮选择器 (类似 iOS)
 * 范围: 10-100岁
 */
@Composable
fun AgeWheelPicker(
    selectedAge: Int,
    onAgeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val ages = remember { (10..100).map { "$it" } }
    val selectedIndex = (selectedAge - 10).coerceIn(0, ages.size - 1)
    
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        WheelPicker(
            items = ages,
            selectedIndex = selectedIndex,
            onSelectedIndexChange = { index ->
                onAgeChange(10 + index)
            },
            modifier = Modifier.width(100.dp),
            visibleItemCount = 5
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "岁",
            style = MaterialTheme.typography.titleLarge,
            color = AppleGray1,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 简单列表选择器（扁平风格）
 * 
 * @param items 选项列表
 * @param selectedIndex 当前选中的索引
 * @param onSelectedIndexChange 选中索引变化回调
 * @param modifier Modifier
 * @param itemHeight 每项高度
 * @param visibleItemCount 可见项数量
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SimpleValuePicker(
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 48.dp,
    visibleItemCount: Int = 5
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (selectedIndex - visibleItemCount / 2).coerceAtLeast(0)
    )
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val view = LocalView.current
    
    // 监听滚动位置，实时更新选中项
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .collect { layoutInfo ->
                // 计算列表中心位置
                val viewportCenter = (layoutInfo.viewportEndOffset + layoutInfo.viewportStartOffset) / 2
                
                // 找到距离中心最近的项
                val closestItem = layoutInfo.visibleItemsInfo.minByOrNull { 
                    abs((it.offset + it.size / 2) - viewportCenter)
                }
                
                if (closestItem != null) {
                    val index = closestItem.index
                    if (index in items.indices && index != selectedIndex) {
                         onSelectedIndexChange(index)
                         view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    }
                }
            }
    }
    
    // 外部更新 selectedIndex 时同步滚动
    LaunchedEffect(selectedIndex) {
        // 只有当不在滚动时才强制同步，避免冲突
        if (!listState.isScrollInProgress) {
            val targetIndex = (selectedIndex - visibleItemCount / 2).coerceAtLeast(0)
            // 如果当前显示位置偏差太大，则滚动
            if (abs(listState.firstVisibleItemIndex - targetIndex) > 1) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    Box(
        modifier = modifier
            .height(itemHeight * visibleItemCount)
            .fillMaxWidth()
    ) {
        // 选中项高亮背景
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(itemHeight)
                .background(
                    color = AppleGray6,
                    shape = RoundedCornerShape(8.dp)
                )
        )
        
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = itemHeight * (visibleItemCount / 2))
        ) {
            itemsIndexed(items) { index, item ->
                val isSelected = index == selectedIndex
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) AppleTeal else AppleGray1,
                        fontSize = if (isSelected) 20.sp else 16.sp
                    )
                }
            }
        }
    }
}
