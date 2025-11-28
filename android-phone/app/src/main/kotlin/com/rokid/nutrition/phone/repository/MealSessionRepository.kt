package com.rokid.nutrition.phone.repository

import android.util.Log
import com.rokid.nutrition.phone.data.dao.MealSessionDao
import com.rokid.nutrition.phone.data.dao.MealSnapshotDao
import com.rokid.nutrition.phone.data.dao.SnapshotFoodDao
import com.rokid.nutrition.phone.data.entity.MealSessionEntity
import com.rokid.nutrition.phone.data.entity.MealSnapshotEntity
import com.rokid.nutrition.phone.data.entity.SnapshotFoodEntity
import com.rokid.nutrition.phone.network.NetworkManager
import com.rokid.nutrition.phone.network.model.*
import com.rokid.nutrition.phone.util.DebugLogger
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * 用餐会话仓库
 * 
 * 协调网络请求和本地存储
 */
class MealSessionRepository(
    private val sessionDao: MealSessionDao,
    private val snapshotDao: MealSnapshotDao,
    private val foodDao: SnapshotFoodDao,
    private val networkManager: NetworkManager
) {
    companion object {
        private const val TAG = "MealSessionRepository"
    }
    
    /**
     * 获取最近的会话列表
     */
    fun getRecentSessions(limit: Int = 20): Flow<List<MealSessionEntity>> {
        return sessionDao.getRecentSessions(limit)
    }
    
    /**
     * 获取当前活跃会话
     */
    suspend fun getActiveSession(): MealSessionEntity? {
        return sessionDao.getActiveSession()
    }
    
    /**
     * 开始新的用餐会话
     */
    suspend fun startSession(
        userId: String,
        mealType: String,
        visionResult: VisionAnalyzeResponse
    ): Result<MealSessionEntity> {
        return try {
            // 1. 调用后端 API
            val response = networkManager.startMeal(userId, mealType, visionResult)
            
            response.fold(
                onSuccess = { startResponse ->
                    // 2. 保存到本地数据库
                    val now = System.currentTimeMillis()
                    val session = MealSessionEntity(
                        sessionId = startResponse.sessionId,
                        userId = userId,
                        mealType = mealType,
                        status = "active",
                        startTime = now,
                        endTime = null,
                        autoCaptureInterval = startResponse.autoCaptureInterval,
                        totalServedKcal = visionResult.snapshot.nutrition.calories,
                        totalConsumedKcal = null,
                        consumptionRatio = null,
                        durationMinutes = null,
                        report = null,
                        createdAt = now,
                        updatedAt = now
                    )
                    sessionDao.saveSession(session)
                    
                    // 3. 保存初始快照
                    saveSnapshot(startResponse.sessionId, visionResult)
                    
                    Log.d(TAG, "会话已创建: ${startResponse.sessionId}")
                    Result.success(session)
                },
                onFailure = { e ->
                    Log.e(TAG, "创建会话失败", e)
                    Result.failure(e)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "创建会话异常", e)
            Result.failure(e)
        }
    }

    
    /**
     * 更新会话（添加新快照）
     */
    suspend fun updateSession(
        sessionId: String,
        visionResult: VisionAnalyzeResponse
    ): Result<MealUpdateResponse> {
        return try {
            val response = networkManager.updateMeal(sessionId, visionResult)
            
            response.fold(
                onSuccess = { updateResponse ->
                    // 保存快照到本地
                    saveSnapshot(sessionId, visionResult)
                    Log.d(TAG, "会话已更新: $sessionId")
                    Result.success(updateResponse)
                },
                onFailure = { e ->
                    Log.e(TAG, "更新会话失败", e)
                    Result.failure(e)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "更新会话异常", e)
            Result.failure(e)
        }
    }
    
    /**
     * 结束会话
     * 
     * 即使后端失败，也会保存本地记录
     * 
     * @param sessionId 会话ID
     * @param localCalories 本地计算的热量（后端失败时使用）
     * @param mealContext 用餐上下文（可选）
     * @param dailyContext 今日上下文（可选）
     * @param userProfile 用户档案（可选）
     */
    suspend fun endSession(
        sessionId: String, 
        localCalories: Double = 0.0,
        mealContext: com.rokid.nutrition.phone.network.model.MealContextPayload? = null,
        dailyContext: com.rokid.nutrition.phone.network.model.DailyContextPayload? = null,
        userProfile: UserProfilePayload? = null
    ): Result<MealEndResponse> {
        val now = System.currentTimeMillis()
        
        return try {
            val response = networkManager.endMeal(
                sessionId = sessionId,
                mealContext = mealContext,
                dailyContext = dailyContext,
                userProfile = userProfile
            )
            
            response.fold(
                onSuccess = { endResponse ->
                    // 更新本地数据库（使用后端返回的数据）
                    sessionDao.endSession(
                        sessionId = sessionId,
                        status = "completed",
                        endTime = now,
                        totalServed = endResponse.finalStats.totalServed,
                        totalConsumed = endResponse.finalStats.totalConsumed,
                        ratio = endResponse.finalStats.consumptionRatio,
                        duration = endResponse.finalStats.durationMinutes,
                        report = endResponse.report,
                        updatedAt = now
                    )
                    Log.d(TAG, "会话已结束: $sessionId")
                    Result.success(endResponse)
                },
                onFailure = { e ->
                    Log.e(TAG, "结束会话后端失败，保存本地记录", e)
                    // 后端失败时，使用本地数据保存记录
                    saveLocalEndSession(sessionId, now, localCalories)
                    Result.failure(e)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "结束会话异常，保存本地记录", e)
            // 异常时，使用本地数据保存记录
            saveLocalEndSession(sessionId, now, localCalories)
            Result.failure(e)
        }
    }
    
    /**
     * 保存本地结束会话记录（后端失败时使用）
     */
    private suspend fun saveLocalEndSession(sessionId: String, endTime: Long, localCalories: Double) {
        try {
            // 获取会话开始时间计算时长
            val session = sessionDao.getSession(sessionId)
            val durationMinutes = if (session != null) {
                ((endTime - session.startTime) / 60000).toInt()
            } else {
                0
            }
            
            sessionDao.endSession(
                sessionId = sessionId,
                status = "completed",
                endTime = endTime,
                totalServed = localCalories,
                totalConsumed = localCalories,
                ratio = 1.0,
                duration = durationMinutes.toDouble(),
                report = "本地记录",
                updatedAt = endTime
            )
            Log.d(TAG, "本地会话记录已保存: $sessionId, 热量: $localCalories kcal, 时长: $durationMinutes 分钟")
        } catch (e: Exception) {
            Log.e(TAG, "保存本地会话记录失败", e)
        }
    }
    
    /**
     * 保存非正餐会话（零食、饮料等）
     * 
     * 创建一个已完成的会话记录，这样可以在历史列表中显示
     */
    suspend fun saveNonMealSession(
        sessionId: String,
        mealType: String,
        totalCalories: Double
    ) {
        val now = System.currentTimeMillis()
        val session = MealSessionEntity(
            sessionId = sessionId,
            userId = "default_user",
            mealType = mealType,
            status = "completed",  // 直接标记为已完成
            startTime = now,
            endTime = now,
            autoCaptureInterval = 0,
            totalServedKcal = totalCalories,
            totalConsumedKcal = totalCalories,
            consumptionRatio = 1.0,
            durationMinutes = 0.0,
            report = null,
            createdAt = now,
            updatedAt = now
        )
        sessionDao.saveSession(session)
        DebugLogger.i(TAG, "非正餐会话已保存: sessionId=$sessionId, mealType=$mealType, calories=$totalCalories")
    }
    
    /**
     * 保存快照到本地
     * 
     * @param sessionId 会话ID
     * @param visionResult 识别结果
     * @param imageUrl 图片URL（可选，后端返回的URL）
     * @param localImagePath 本地图片路径（可选）
     */
    suspend fun saveSnapshot(
        sessionId: String, 
        visionResult: VisionAnalyzeResponse,
        imageUrl: String? = null,
        localImagePath: String? = null
    ) {
        DebugLogger.i(TAG, "saveSnapshot 开始: sessionId=$sessionId, imageUrl=$imageUrl")
        val snapshotId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        
        // 使用后端返回的图片URL，如果没有则使用传入的参数
        val finalImageUrl = visionResult.snapshot.imageUrl ?: imageUrl ?: ""
        DebugLogger.i(TAG, "最终图片URL: $finalImageUrl (来自response: ${visionResult.snapshot.imageUrl}, 参数: $imageUrl)")
        
        val snapshot = MealSnapshotEntity(
            id = snapshotId,
            sessionId = sessionId,
            imageUrl = finalImageUrl,
            localImagePath = localImagePath,
            capturedAt = now,
            model = "qwen3-vl-plus",
            rawJson = null,
            totalKcal = visionResult.snapshot.nutrition.calories
        )
        snapshotDao.saveSnapshot(snapshot)
        DebugLogger.i(TAG, "快照已保存到数据库: snapshotId=$snapshotId, imageUrl=$finalImageUrl")
        
        // 保存食物列表 - 正确计算每个食物的营养数据
        val totalNutrition = visionResult.snapshot.nutrition
        val totalWeight = visionResult.rawLlm.foods.sumOf { it.totalWeightG }
        
        val foods = visionResult.rawLlm.foods.mapIndexed { index, food ->
            // 按重量比例分配营养数据
            val weightRatio = if (totalWeight > 0) food.totalWeightG / totalWeight else 1.0 / visionResult.rawLlm.foods.size
            
            SnapshotFoodEntity(
                id = "${snapshotId}_$index",
                snapshotId = snapshotId,
                name = food.dishName,
                chineseName = food.dishNameCn ?: food.dishName,
                weightG = food.totalWeightG,
                caloriesKcal = totalNutrition.calories * weightRatio,
                proteinG = totalNutrition.protein * weightRatio,
                carbsG = totalNutrition.carbs * weightRatio,
                fatG = totalNutrition.fat * weightRatio,
                confidence = food.confidence,
                cookingMethod = food.cookingMethod
            )
        }
        foodDao.saveFoods(foods)
        Log.d(TAG, "保存 ${foods.size} 个食物项")
    }
    
    /**
     * 获取会话详情
     */
    suspend fun getSession(sessionId: String): MealSessionEntity? {
        return sessionDao.getSession(sessionId)
    }
    
    /**
     * 获取会话的所有快照
     */
    suspend fun getSnapshots(sessionId: String): List<MealSnapshotEntity> {
        return snapshotDao.getSnapshotsForSession(sessionId)
    }
    
    /**
     * 删除用餐会话及其所有相关数据
     * 
     * 删除顺序：
     * 1. 获取所有快照ID
     * 2. 删除所有快照的食物
     * 3. 删除快照
     * 4. 删除会话
     */
    suspend fun deleteSession(sessionId: String): Result<Unit> {
        return try {
            // 1. 获取所有快照ID
            val snapshotIds = snapshotDao.getSnapshotIdsForSession(sessionId)
            Log.d(TAG, "删除会话 $sessionId，包含 ${snapshotIds.size} 个快照")
            
            // 2. 删除所有快照的食物
            snapshotIds.forEach { snapshotId ->
                foodDao.deleteFoodsForSnapshot(snapshotId)
            }
            
            // 3. 删除快照
            snapshotDao.deleteSnapshotsForSession(sessionId)
            
            // 4. 删除会话
            sessionDao.deleteSession(sessionId)
            
            Log.d(TAG, "会话已删除: $sessionId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "删除会话失败", e)
            Result.failure(e)
        }
    }
}
