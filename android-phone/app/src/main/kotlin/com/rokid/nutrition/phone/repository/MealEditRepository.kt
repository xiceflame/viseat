package com.rokid.nutrition.phone.repository

import com.rokid.nutrition.phone.data.dao.MealSnapshotDao
import com.rokid.nutrition.phone.data.dao.SnapshotFoodDao
import com.rokid.nutrition.phone.data.dao.SyncQueueDao
import com.rokid.nutrition.phone.data.entity.MealSnapshotEntity
import com.rokid.nutrition.phone.data.entity.SnapshotFoodEntity
import com.rokid.nutrition.phone.data.entity.SyncQueueEntity
import com.rokid.nutrition.phone.util.FoodItemUpdates
import com.rokid.nutrition.phone.util.FoodItemValidator
import com.rokid.nutrition.phone.util.NutritionCalculator
import com.rokid.nutrition.phone.util.NutritionValues
import com.google.gson.Gson
import java.util.UUID

/**
 * 快照与食物的组合数据
 */
data class MealSnapshotWithFoods(
    val snapshot: MealSnapshotEntity,
    val foods: List<SnapshotFoodEntity>
)

/**
 * 用餐编辑仓库
 * 
 * 处理饮食数据的编辑和持久化
 */
class MealEditRepository(
    private val snapshotDao: MealSnapshotDao,
    private val foodDao: SnapshotFoodDao,
    private val syncQueueDao: SyncQueueDao
) {
    private val gson = Gson()
    
    /**
     * 获取快照及其食物数据
     */
    suspend fun getMealSnapshot(snapshotId: String): MealSnapshotWithFoods? {
        val snapshots = snapshotDao.getSnapshotsForSession(snapshotId)
        val snapshot = snapshots.firstOrNull { it.id == snapshotId } 
            ?: return null
        val foods = foodDao.getFoodsForSnapshot(snapshotId)
        return MealSnapshotWithFoods(snapshot, foods)
    }
    
    /**
     * 根据 sessionId 获取最新快照
     */
    suspend fun getLatestSnapshotForSession(sessionId: String): MealSnapshotWithFoods? {
        val snapshot = snapshotDao.getLatestSnapshot(sessionId) ?: return null
        val foods = foodDao.getFoodsForSnapshot(snapshot.id)
        return MealSnapshotWithFoods(snapshot, foods)
    }
    
    /**
     * 获取会话的所有快照
     */
    suspend fun getSnapshotsForSession(sessionId: String): List<MealSnapshotWithFoods> {
        val snapshots = snapshotDao.getSnapshotsForSession(sessionId)
        return snapshots.map { snapshot ->
            val foods = foodDao.getFoodsForSnapshot(snapshot.id)
            MealSnapshotWithFoods(snapshot, foods)
        }
    }
    
    /**
     * 更新食物项
     * 
     * @param foodId 食物ID
     * @param updates 更新数据
     * @return 操作结果
     */
    suspend fun updateFoodItem(foodId: String, updates: FoodItemUpdates): Result<SnapshotFoodEntity> {
        // 验证输入
        val validation = FoodItemValidator.validate(updates)
        if (!validation.isValid) {
            return Result.failure(IllegalArgumentException(validation.errors.values.first()))
        }
        
        // 获取当前食物数据
        val currentFood = foodDao.getFoodById(foodId)
            ?: return Result.failure(IllegalArgumentException("食物不存在: $foodId"))
        
        // 计算新值
        val newFood = calculateUpdatedFood(currentFood, updates)
        
        // 保存到数据库
        foodDao.saveFood(newFood)
        
        // 添加到同步队列
        enqueueSyncOperation(newFood)
        
        return Result.success(newFood)
    }
    
    /**
     * 计算更新后的食物数据
     */
    private fun calculateUpdatedFood(
        current: SnapshotFoodEntity,
        updates: FoodItemUpdates
    ): SnapshotFoodEntity {
        val now = System.currentTimeMillis()
        
        // 确保原始值已保存
        val originalWeight = current.originalWeightG ?: current.weightG
        val originalCalories = current.originalCaloriesKcal ?: current.caloriesKcal
        val originalProtein = current.originalProteinG ?: current.proteinG
        val originalCarbs = current.originalCarbsG ?: current.carbsG
        val originalFat = current.originalFatG ?: current.fatG
        
        // 如果需要按比例重新计算
        if (updates.recalculateFromWeight && updates.weightG != null) {
            val originalNutrition = NutritionValues(
                calories = originalCalories,
                protein = originalProtein ?: 0.0,
                carbs = originalCarbs ?: 0.0,
                fat = originalFat ?: 0.0
            )
            
            val newNutrition = NutritionCalculator.recalculateProportionally(
                original = originalNutrition,
                originalWeight = originalWeight,
                newWeight = updates.weightG
            )
            
            return current.copy(
                originalWeightG = originalWeight,
                originalCaloriesKcal = originalCalories,
                originalProteinG = originalProtein,
                originalCarbsG = originalCarbs,
                originalFatG = originalFat,
                weightG = updates.weightG,
                caloriesKcal = newNutrition.calories,
                proteinG = newNutrition.protein,
                carbsG = newNutrition.carbs,
                fatG = newNutrition.fat,
                isEdited = true,
                editedAt = now
            )
        }
        
        // 手动覆盖模式
        return current.copy(
            originalWeightG = originalWeight,
            originalCaloriesKcal = originalCalories,
            originalProteinG = originalProtein,
            originalCarbsG = originalCarbs,
            originalFatG = originalFat,
            weightG = updates.weightG ?: current.weightG,
            caloriesKcal = updates.caloriesKcal ?: current.caloriesKcal,
            proteinG = updates.proteinG ?: current.proteinG,
            carbsG = updates.carbsG ?: current.carbsG,
            fatG = updates.fatG ?: current.fatG,
            isEdited = true,
            editedAt = now
        )
    }
    
    /**
     * 添加同步操作到队列
     */
    private suspend fun enqueueSyncOperation(food: SnapshotFoodEntity) {
        val payload = mapOf(
            "food_id" to food.id,
            "weight_g" to food.weightG,
            "calories_kcal" to food.caloriesKcal,
            "protein_g" to food.proteinG,
            "carbs_g" to food.carbsG,
            "fat_g" to food.fatG,
            "edited_at" to food.editedAt
        )
        
        val operation = SyncQueueEntity(
            id = UUID.randomUUID().toString(),
            operationType = "update_food",
            targetId = food.id,
            payload = gson.toJson(payload),
            createdAt = System.currentTimeMillis()
        )
        
        syncQueueDao.insert(operation)
    }
    
    /**
     * 获取待同步操作数量
     */
    suspend fun getPendingSyncCount(): Int {
        return syncQueueDao.getPendingCount()
    }
    
    /**
     * 恢复食物到原始值
     */
    suspend fun restoreToOriginal(foodId: String): Result<SnapshotFoodEntity> {
        val currentFood = foodDao.getFoodById(foodId)
            ?: return Result.failure(IllegalArgumentException("食物不存在: $foodId"))
        
        // 如果没有原始值，说明从未编辑过
        if (currentFood.originalWeightG == null) {
            return Result.success(currentFood)
        }
        
        val restoredFood = currentFood.copy(
            weightG = currentFood.originalWeightG,
            caloriesKcal = currentFood.originalCaloriesKcal ?: currentFood.caloriesKcal,
            proteinG = currentFood.originalProteinG,
            carbsG = currentFood.originalCarbsG,
            fatG = currentFood.originalFatG,
            isEdited = false,
            editedAt = null
        )
        
        foodDao.saveFood(restoredFood)
        return Result.success(restoredFood)
    }
    
    /**
     * 删除食物项
     * 
     * @param foodId 食物ID
     * @return 操作结果
     */
    suspend fun deleteFoodItem(foodId: String): Result<Unit> {
        return try {
            // 获取食物数据（用于同步队列）
            val food = foodDao.getFoodById(foodId)
            
            // 从数据库删除
            foodDao.deleteFood(foodId)
            
            // 添加删除操作到同步队列
            if (food != null) {
                enqueueDeleteOperation(food)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 添加删除操作到同步队列
     */
    private suspend fun enqueueDeleteOperation(food: SnapshotFoodEntity) {
        val payload = mapOf(
            "food_id" to food.id,
            "snapshot_id" to food.snapshotId,
            "deleted_at" to System.currentTimeMillis()
        )
        
        val operation = SyncQueueEntity(
            id = UUID.randomUUID().toString(),
            operationType = "delete_food",
            targetId = food.id,
            payload = gson.toJson(payload),
            createdAt = System.currentTimeMillis()
        )
        
        syncQueueDao.insert(operation)
    }
}
