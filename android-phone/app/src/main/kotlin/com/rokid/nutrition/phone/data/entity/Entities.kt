package com.rokid.nutrition.phone.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 用户档案
 */
@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: String = "default",
    val nickname: String? = null,  // 用户昵称
    val age: Int,
    val gender: String = "male",  // male/female
    val height: Float,  // cm
    val weight: Float,  // kg
    val bmi: Float,
    val activityLevel: String = "moderate",  // sedentary/light/moderate/active/very_active
    val healthGoal: String = "maintain",  // lose_weight/gain_muscle/maintain
    val targetWeight: Float? = null,  // 目标体重 kg
    val healthConditions: String,  // JSON array: ["脂肪肝", "高血压"]
    val dietaryPreferences: String,  // JSON array: ["低油", "低糖"]
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * 用餐会话
 */
@Entity(tableName = "meal_sessions")
data class MealSessionEntity(
    @PrimaryKey val sessionId: String,
    val userId: String,
    val mealType: String,  // breakfast/lunch/dinner/snack
    val status: String,    // active/completed
    val startTime: Long,
    val endTime: Long?,
    val autoCaptureInterval: Int = 300,
    // 最终统计
    val totalServedKcal: Double?,
    val totalConsumedKcal: Double?,
    val consumptionRatio: Double?,
    val durationMinutes: Double?,
    val report: String?,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * 用餐快照
 */
@Entity(
    tableName = "meal_snapshots",
    foreignKeys = [ForeignKey(
        entity = MealSessionEntity::class,
        parentColumns = ["sessionId"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId")]
)
data class MealSnapshotEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val imageUrl: String,
    val localImagePath: String?,
    val capturedAt: Long,
    val model: String = "qwen3-vl-plus",
    val rawJson: String?,
    val totalKcal: Double,
    val isEdited: Boolean = false,      // 是否被编辑过
    val lastSyncedAt: Long? = null      // 最后同步时间
)


/**
 * 快照中的食物
 */
@Entity(
    tableName = "snapshot_foods",
    foreignKeys = [ForeignKey(
        entity = MealSnapshotEntity::class,
        parentColumns = ["id"],
        childColumns = ["snapshotId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("snapshotId")]
)
data class SnapshotFoodEntity(
    @PrimaryKey val id: String,
    val snapshotId: String,
    val name: String,
    val chineseName: String?,
    // 原始值（AI识别）
    val originalWeightG: Double? = null,
    val originalCaloriesKcal: Double? = null,
    val originalProteinG: Double? = null,
    val originalCarbsG: Double? = null,
    val originalFatG: Double? = null,
    // 当前值（可能被用户编辑）
    val weightG: Double,
    val caloriesKcal: Double,
    val proteinG: Double?,
    val carbsG: Double?,
    val fatG: Double?,
    // 元数据
    val confidence: Double,
    val cookingMethod: String?,
    val isEdited: Boolean = false,
    val editedAt: Long? = null
)

/**
 * 同步队列
 */
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey val id: String,
    val operationType: String,      // "update_food", "update_snapshot"
    val targetId: String,           // foodId 或 snapshotId
    val payload: String,            // JSON 格式的更新数据
    val createdAt: Long,
    val retryCount: Int = 0,
    val lastError: String? = null,
    val status: String = "pending"  // pending, syncing, failed, completed
)
