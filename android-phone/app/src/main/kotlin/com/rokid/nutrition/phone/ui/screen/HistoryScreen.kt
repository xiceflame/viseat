package com.rokid.nutrition.phone.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rokid.nutrition.phone.data.entity.MealSessionEntity
import java.text.SimpleDateFormat
import java.util.*

/**
 * 带照片的用餐会话数据
 */
data class MealSessionWithPhoto(
    val session: MealSessionEntity,
    val photoUri: String? = null
)

/**
 * 历史记录页面
 */
@Composable
fun HistoryScreen(
    sessions: List<MealSessionEntity>,
    onSessionClick: (MealSessionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "用餐历史",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无用餐记录",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sessions) { session ->
                    HistoryItem(
                        session = session,
                        onClick = { onSessionClick(session) }
                    )
                }
            }
        }
    }
}

/**
 * 带照片的历史记录页面
 */
@Composable
fun HistoryScreenWithPhotos(
    sessions: List<MealSessionWithPhoto>,
    onSessionClick: (MealSessionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "用餐历史",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无用餐记录",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sessions) { sessionWithPhoto ->
                    HistoryItemWithPhoto(
                        sessionWithPhoto = sessionWithPhoto,
                        onClick = { onSessionClick(sessionWithPhoto.session) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryItem(
    session: MealSessionEntity,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    val mealTypeText = when (session.mealType) {
        "breakfast" -> "早餐"
        "lunch" -> "午餐"
        "dinner" -> "晚餐"
        "snack" -> "加餐"
        else -> session.mealType
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = mealTypeText,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                Text(
                    text = dateFormat.format(Date(session.startTime)),
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                if (session.durationMinutes != null) {
                    Text(
                        text = "${session.durationMinutes.toInt()} 分钟",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${session.totalConsumedKcal?.toInt() ?: 0}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "千卡",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * 带照片的历史记录项
 */
@Composable
private fun HistoryItemWithPhoto(
    sessionWithPhoto: MealSessionWithPhoto,
    onClick: () -> Unit
) {
    val session = sessionWithPhoto.session
    val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    val mealTypeText = when (session.mealType) {
        "breakfast" -> "早餐"
        "lunch" -> "午餐"
        "dinner" -> "晚餐"
        "snack" -> "加餐"
        else -> session.mealType
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 照片缩略图
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (sessionWithPhoto.photoUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(sessionWithPhoto.photoUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "食物照片",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationZ = -90f  // 逆时针旋转90度
                            }
                    )
                } else {
                    // 占位图标
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mealTypeText,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                Text(
                    text = dateFormat.format(Date(session.startTime)),
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                if (session.durationMinutes != null) {
                    Text(
                        text = "${session.durationMinutes.toInt()} 分钟",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
            
            // 热量
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${session.totalConsumedKcal?.toInt() ?: 0}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "千卡",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}
