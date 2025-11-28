package com.rokid.nutrition.phone.data.dao

import androidx.room.*
import com.rokid.nutrition.phone.data.entity.*
import kotlinx.coroutines.flow.Flow

/**
 * 用户档案 DAO
 */
@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE id = :id")
    suspend fun getProfile(id: String = "default"): UserProfileEntity?
    
    @Query("SELECT * FROM user_profiles WHERE id = :id")
    fun getProfileFlow(id: String = "default"): Flow<UserProfileEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: UserProfileEntity)
    
    @Query("SELECT EXISTS(SELECT 1 FROM user_profiles WHERE id = 'default')")
    suspend fun hasProfile(): Boolean
}

/**
 * 用餐会话 DAO
 */
@Dao
interface MealSessionDao {
    @Query("SELECT * FROM meal_sessions ORDER BY startTime DESC LIMIT :limit")
    fun getRecentSessions(limit: Int = 20): Flow<List<MealSessionEntity>>
    
    @Query("SELECT * FROM meal_sessions WHERE sessionId = :sessionId")
    suspend fun getSession(sessionId: String): MealSessionEntity?
    
    @Query("SELECT * FROM meal_sessions WHERE status = 'active' LIMIT 1")
    suspend fun getActiveSession(): MealSessionEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(session: MealSessionEntity)
    
    @Query("""
        UPDATE meal_sessions SET 
            status = :status, 
            endTime = :endTime, 
            totalServedKcal = :totalServed, 
            totalConsumedKcal = :totalConsumed, 
            consumptionRatio = :ratio, 
            durationMinutes = :duration, 
            report = :report,
            updatedAt = :updatedAt 
        WHERE sessionId = :sessionId
    """)
    suspend fun endSession(
        sessionId: String,
        status: String,
        endTime: Long,
        totalServed: Double,
        totalConsumed: Double,
        ratio: Double,
        duration: Double,
        report: String?,
        updatedAt: Long
    )
    
    // 统计查询
    @Query("""
        SELECT COALESCE(SUM(totalConsumedKcal), 0.0) 
        FROM meal_sessions 
        WHERE startTime >= :startOfDay AND startTime < :endOfDay AND status = 'completed'
    """)
    suspend fun getDailyCalories(startOfDay: Long, endOfDay: Long): Double
    
    @Query("""
        SELECT * FROM meal_sessions 
        WHERE startTime >= :startTime AND startTime < :endTime AND status = 'completed' 
        ORDER BY startTime ASC
    """)
    suspend fun getSessionsInRange(startTime: Long, endTime: Long): List<MealSessionEntity>
    
    @Query("DELETE FROM meal_sessions WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)
}


/**
 * 用餐快照 DAO
 */
@Dao
interface MealSnapshotDao {
    @Query("SELECT * FROM meal_snapshots WHERE sessionId = :sessionId ORDER BY capturedAt ASC")
    suspend fun getSnapshotsForSession(sessionId: String): List<MealSnapshotEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSnapshot(snapshot: MealSnapshotEntity)
    
    @Query("SELECT * FROM meal_snapshots WHERE sessionId = :sessionId ORDER BY capturedAt DESC LIMIT 1")
    suspend fun getLatestSnapshot(sessionId: String): MealSnapshotEntity?
    
    @Query("DELETE FROM meal_snapshots WHERE sessionId = :sessionId")
    suspend fun deleteSnapshotsForSession(sessionId: String)
    
    @Query("SELECT id FROM meal_snapshots WHERE sessionId = :sessionId")
    suspend fun getSnapshotIdsForSession(sessionId: String): List<String>
}

/**
 * 快照食物 DAO
 */
@Dao
interface SnapshotFoodDao {
    @Query("SELECT * FROM snapshot_foods WHERE snapshotId = :snapshotId")
    suspend fun getFoodsForSnapshot(snapshotId: String): List<SnapshotFoodEntity>
    
    @Query("SELECT * FROM snapshot_foods WHERE id = :foodId")
    suspend fun getFoodById(foodId: String): SnapshotFoodEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFoods(foods: List<SnapshotFoodEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFood(food: SnapshotFoodEntity)
    
    @Query("DELETE FROM snapshot_foods WHERE snapshotId = :snapshotId")
    suspend fun deleteFoodsForSnapshot(snapshotId: String)
    
    @Query("""
        UPDATE snapshot_foods SET 
            weightG = :weightG,
            caloriesKcal = :caloriesKcal,
            proteinG = :proteinG,
            carbsG = :carbsG,
            fatG = :fatG,
            isEdited = 1,
            editedAt = :editedAt
        WHERE id = :foodId
    """)
    suspend fun updateFoodNutrition(
        foodId: String,
        weightG: Double,
        caloriesKcal: Double,
        proteinG: Double?,
        carbsG: Double?,
        fatG: Double?,
        editedAt: Long
    )
    
    @Query("DELETE FROM snapshot_foods WHERE id = :foodId")
    suspend fun deleteFood(foodId: String)
}

/**
 * 同步队列 DAO
 */
@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE status = 'pending' ORDER BY createdAt ASC")
    suspend fun getPendingOperations(): List<SyncQueueEntity>
    
    @Query("SELECT * FROM sync_queue WHERE status = 'pending' ORDER BY createdAt ASC LIMIT 1")
    suspend fun getNextPendingOperation(): SyncQueueEntity?
    
    @Query("SELECT * FROM sync_queue WHERE targetId = :targetId AND status = 'pending'")
    suspend fun getPendingForTarget(targetId: String): List<SyncQueueEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(operation: SyncQueueEntity)
    
    @Query("UPDATE sync_queue SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)
    
    @Query("UPDATE sync_queue SET status = 'failed', lastError = :error, retryCount = retryCount + 1 WHERE id = :id")
    suspend fun markFailed(id: String, error: String)
    
    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun delete(id: String)
    
    @Query("DELETE FROM sync_queue WHERE status = 'completed'")
    suspend fun deleteCompleted()
    
    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'pending'")
    suspend fun getPendingCount(): Int
    
    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'pending'")
    fun getPendingCountFlow(): Flow<Int>
}
