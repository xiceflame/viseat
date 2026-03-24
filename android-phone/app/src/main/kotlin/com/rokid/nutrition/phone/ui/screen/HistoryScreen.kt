package com.rokid.nutrition.phone.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.rokid.nutrition.phone.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * 食物项（带分类）
 */
data class FoodItemWithCategory(
    val name: String,
    val category: String,  // meal/snack/beverage/dessert/fruit
    val calories: Double = 0.0
)

/**
 * 带照片的用餐会话数据
 */
data class MealSessionWithPhoto(
    val session: MealSessionEntity,
    val photoUri: String? = null,
    val foods: List<FoodItemWithCategory> = emptyList()  // 识别到的食物列表
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
            .background(PremiumGradients.pageBackground())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "用餐历史",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.RestaurantMenu,
                        contentDescription = null,
                        tint = AppleGray3,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无用餐记录",
                        style = MaterialTheme.typography.bodyLarge,
                        color = AppleGray1
                    )
                }
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
            .background(PremiumGradients.pageBackground())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "用餐历史",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.RestaurantMenu,
                        contentDescription = null,
                        tint = AppleGray3,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无用餐记录",
                        style = MaterialTheme.typography.bodyLarge,
                        color = AppleGray1
                    )
                }
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
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppleTeal.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Restaurant,
                        contentDescription = null,
                        tint = AppleTeal,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = mealTypeText,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dateFormat.format(Date(session.startTime)),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppleGray1
                    )
                    if (session.durationMinutes != null) {
                        Text(
                            text = "${session.durationMinutes.toInt()} 分钟",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppleGray2
                        )
                    }
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${session.totalConsumedKcal?.toInt() ?: 0}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = CalorieRed
                )
                Text(
                    text = "kcal",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppleGray1
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
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 照片缩略图
            Card(
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppleGray5),
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
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Restaurant,
                            contentDescription = null,
                            tint = AppleGray2,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            // 信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mealTypeText,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateFormat.format(Date(session.startTime)),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppleGray1
                )
                if (session.durationMinutes != null) {
                    Text(
                        text = "${session.durationMinutes.toInt()} 分钟",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppleGray2
                    )
                }
            }
            
            // 热量
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${session.totalConsumedKcal?.toInt() ?: 0}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = CalorieRed
                )
                Text(
                    text = "kcal",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppleGray1
                )
            }
        }
    }
}
