package com.rokid.nutrition.phone.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rokid.nutrition.phone.data.entity.SnapshotFoodEntity
import com.rokid.nutrition.phone.repository.DailyNutritionTracker
import com.rokid.nutrition.phone.repository.MealEditRepository
import com.rokid.nutrition.phone.repository.MealSnapshotWithFoods
import com.rokid.nutrition.phone.repository.PhotoSource
import com.rokid.nutrition.phone.repository.PhotoStorageRepository
import com.rokid.nutrition.phone.sync.SyncManager
import com.rokid.nutrition.phone.sync.SyncStatus
import com.rokid.nutrition.phone.util.FoodItemUpdates
import com.rokid.nutrition.phone.util.FoodItemValidator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 可编辑的食物项
 */
data class EditableFoodItem(
    val id: String,
    val name: String,
    val chineseName: String?,
    val weightG: Double,
    val caloriesKcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val originalWeightG: Double,
    val isEdited: Boolean,
    val confidence: Double,
    val cookingMethod: String?
) {
    companion object {
        fun fromEntity(entity: SnapshotFoodEntity): EditableFoodItem {
            return EditableFoodItem(
                id = entity.id,
                name = entity.name,
                chineseName = entity.chineseName,
                weightG = entity.weightG,
                caloriesKcal = entity.caloriesKcal,
                proteinG = entity.proteinG ?: 0.0,
                carbsG = entity.carbsG ?: 0.0,
                fatG = entity.fatG ?: 0.0,
                originalWeightG = entity.originalWeightG ?: entity.weightG,
                isEdited = entity.isEdited,
                confidence = entity.confidence,
                cookingMethod = entity.cookingMethod
            )
        }
    }
}

/**
 * 食物详情页面 UI 状态
 */
data class FoodDetailUiState(
    val snapshot: MealSnapshotWithFoods? = null,
    val foods: List<EditableFoodItem> = emptyList(),
    val photoUri: String? = null,
    val photoSource: PhotoSource? = null,
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val editingFoodId: String? = null,
    val editingFood: EditableFoodItem? = null,
    val syncStatus: SyncStatus = SyncStatus.Synced,
    val pendingSyncCount: Int = 0,
    val validationErrors: Map<String, String> = emptyMap(),
    val error: String? = null,
    val saveSuccess: Boolean = false,
    val downloadSuccess: Boolean = false
)

/**
 * 食物数据变化事件
 */
sealed class FoodDataChangeEvent {
    data class FoodUpdated(
        val foodId: String,
        val oldCalories: Double,
        val newCalories: Double,
        val oldProtein: Double,
        val newProtein: Double,
        val oldCarbs: Double,
        val newCarbs: Double,
        val oldFat: Double,
        val newFat: Double,
        val mealTimestamp: Long
    ) : FoodDataChangeEvent()
    
    data class FoodDeleted(
        val foodId: String,
        val calories: Double,
        val protein: Double,
        val carbs: Double,
        val fat: Double,
        val mealTimestamp: Long
    ) : FoodDataChangeEvent()
    
    data class FoodAdded(
        val foodId: String,
        val calories: Double,
        val protein: Double,
        val carbs: Double,
        val fat: Double,
        val mealTimestamp: Long
    ) : FoodDataChangeEvent()
}

/**
 * 食物详情 ViewModel
 */
class FoodDetailViewModel(
    private val mealEditRepository: MealEditRepository,
    private val photoStorageRepository: PhotoStorageRepository,
    private val syncManager: SyncManager,
    private val dailyNutritionTracker: DailyNutritionTracker? = null,
    private val networkManager: com.rokid.nutrition.phone.network.NetworkManager? = null
) : ViewModel() {
    
    companion object {
        private const val TAG = "FoodDetailViewModel"
    }
    
    private val _uiState = MutableStateFlow(FoodDetailUiState())
    val uiState: StateFlow<FoodDetailUiState> = _uiState.asStateFlow()
    
    // 数据变化事件流（供外部监听）
    private val _dataChangeEvent = MutableSharedFlow<FoodDataChangeEvent>()
    val dataChangeEvent: SharedFlow<FoodDataChangeEvent> = _dataChangeEvent.asSharedFlow()
    
    // 编辑前的原始值（用于取消时恢复）
    private var originalEditingFood: EditableFoodItem? = null
    
    // 当前快照的时间戳（用于判断是否是今天的数据）
    private var currentSnapshotTimestamp: Long = 0L
    
    // 当前快照ID（用于新增食物）
    private var currentSnapshotId: String? = null
    
    init {
        // 观察同步状态
        viewModelScope.launch {
            syncManager.observeSyncStatus().collect { status ->
                _uiState.update { it.copy(syncStatus = status) }
            }
        }
        
        viewModelScope.launch {
            syncManager.observePendingCount().collect { count ->
                _uiState.update { it.copy(pendingSyncCount = count) }
            }
        }
    }
    
    /**
     * 加载快照数据
     */
    fun loadMealSnapshot(sessionId: String) {
        android.util.Log.d("FoodDetailViewModel", "loadMealSnapshot called with sessionId: $sessionId")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                android.util.Log.d("FoodDetailViewModel", "Fetching snapshot for session: $sessionId")
                val snapshotWithFoods = mealEditRepository.getLatestSnapshotForSession(sessionId)
                android.util.Log.d("FoodDetailViewModel", "Snapshot result: ${snapshotWithFoods != null}")
                
                if (snapshotWithFoods != null) {
                    android.util.Log.d("FoodDetailViewModel", "Snapshot found: ${snapshotWithFoods.snapshot.id}, foods: ${snapshotWithFoods.foods.size}")
                    // 使用 sessionId 查询照片源，因为 localImagePath 是保存在 snapshot 中的
                    val photoSource = photoStorageRepository.getPhotoSource(snapshotWithFoods.snapshot.sessionId)
                    val photoUri = when (photoSource) {
                        is PhotoSource.Local -> photoSource.path
                        is PhotoSource.Remote -> photoSource.url
                        null -> snapshotWithFoods.snapshot.imageUrl.takeIf { it.isNotBlank() }
                    }
                    android.util.Log.d("FoodDetailViewModel", "Photo URI: $photoUri")
                    
                    // 保存快照时间戳和ID
                    currentSnapshotTimestamp = snapshotWithFoods.snapshot.capturedAt
                    currentSnapshotId = snapshotWithFoods.snapshot.id
                    
                    _uiState.update { 
                        it.copy(
                            snapshot = snapshotWithFoods,
                            foods = snapshotWithFoods.foods.map { food -> EditableFoodItem.fromEntity(food) },
                            photoUri = photoUri,
                            photoSource = photoSource,
                            isLoading = false
                        )
                    }
                } else {
                    android.util.Log.w("FoodDetailViewModel", "No snapshot found for session: $sessionId")
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = "未找到快照数据"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "加载失败"
                    )
                }
            }
        }
    }
    
    /**
     * 开始编辑食物项
     */
    fun startEditing(foodId: String) {
        val food = _uiState.value.foods.find { it.id == foodId } ?: return
        originalEditingFood = food
        _uiState.update { 
            it.copy(
                isEditing = true,
                editingFoodId = foodId,
                editingFood = food,
                validationErrors = emptyMap()
            )
        }
    }
    
    /**
     * 更新编辑中的食物数据
     */
    fun updateEditingFood(updates: FoodItemUpdates) {
        val currentFood = _uiState.value.editingFood ?: return
        
        // 验证输入
        val validation = FoodItemValidator.validate(updates)
        if (!validation.isValid) {
            _uiState.update { it.copy(validationErrors = validation.errors) }
            return
        }
        
        // 更新编辑中的食物
        val updatedFood = currentFood.copy(
            weightG = updates.weightG ?: currentFood.weightG,
            caloriesKcal = updates.caloriesKcal ?: currentFood.caloriesKcal,
            proteinG = updates.proteinG ?: currentFood.proteinG,
            carbsG = updates.carbsG ?: currentFood.carbsG,
            fatG = updates.fatG ?: currentFood.fatG
        )
        
        _uiState.update { 
            it.copy(
                editingFood = updatedFood,
                validationErrors = emptyMap()
            )
        }
    }
    
    /**
     * 保存编辑的更改
     */
    fun saveChanges() {
        val editingFood = _uiState.value.editingFood ?: return
        val foodId = _uiState.value.editingFoodId ?: return
        val originalFood = originalEditingFood ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, saveSuccess = false) }
            
            val updates = FoodItemUpdates(
                weightG = editingFood.weightG,
                caloriesKcal = editingFood.caloriesKcal,
                proteinG = editingFood.proteinG,
                carbsG = editingFood.carbsG,
                fatG = editingFood.fatG
            )
            
            val result = mealEditRepository.updateFoodItem(foodId, updates)
            
            result.fold(
                onSuccess = { updatedEntity ->
                    // 计算热量差值并更新 DailyNutritionTracker
                    val caloriesDiff = editingFood.caloriesKcal - originalFood.caloriesKcal
                    val proteinDiff = editingFood.proteinG - originalFood.proteinG
                    val carbsDiff = editingFood.carbsG - originalFood.carbsG
                    val fatDiff = editingFood.fatG - originalFood.fatG
                    
                    Log.d(TAG, "食物更新: 热量差值=$caloriesDiff, 蛋白质差值=$proteinDiff")
                    
                    // 更新今日营养统计（如果是今天的数据）
                    if (caloriesDiff != 0.0 || proteinDiff != 0.0 || carbsDiff != 0.0 || fatDiff != 0.0) {
                        dailyNutritionTracker?.let { tracker ->
                            if (caloriesDiff > 0) {
                                // 增加了热量
                                tracker.updateDailyTotals(
                                    com.rokid.nutrition.phone.network.model.NutritionTotal(
                                        calories = caloriesDiff,
                                        protein = proteinDiff.coerceAtLeast(0.0),
                                        carbs = carbsDiff.coerceAtLeast(0.0),
                                        fat = fatDiff.coerceAtLeast(0.0)
                                    )
                                )
                            } else {
                                // 减少了热量
                                tracker.subtractNutrition(
                                    calories = -caloriesDiff,
                                    protein = (-proteinDiff).coerceAtLeast(0.0),
                                    carbs = (-carbsDiff).coerceAtLeast(0.0),
                                    fat = (-fatDiff).coerceAtLeast(0.0),
                                    mealTimestamp = currentSnapshotTimestamp
                                )
                            }
                        }
                        
                        // 发送数据变化事件
                        _dataChangeEvent.emit(
                            FoodDataChangeEvent.FoodUpdated(
                                foodId = foodId,
                                oldCalories = originalFood.caloriesKcal,
                                newCalories = editingFood.caloriesKcal,
                                oldProtein = originalFood.proteinG,
                                newProtein = editingFood.proteinG,
                                oldCarbs = originalFood.carbsG,
                                newCarbs = editingFood.carbsG,
                                oldFat = originalFood.fatG,
                                newFat = editingFood.fatG,
                                mealTimestamp = currentSnapshotTimestamp
                            )
                        )
                    }
                    
                    // 更新本地列表
                    val updatedFoods = _uiState.value.foods.map { food ->
                        if (food.id == foodId) EditableFoodItem.fromEntity(updatedEntity)
                        else food
                    }
                    
                    _uiState.update { 
                        it.copy(
                            foods = updatedFoods,
                            isLoading = false,
                            isEditing = false,
                            editingFoodId = null,
                            editingFood = null,
                            saveSuccess = true
                        )
                    }
                    
                    // 清除原始值
                    originalEditingFood = null
                    
                    // 尝试同步
                    syncManager.processPendingOperations()
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
                }
            )
        }
    }
    
    /**
     * 取消编辑
     */
    fun cancelEdit() {
        _uiState.update { 
            it.copy(
                isEditing = false,
                editingFoodId = null,
                editingFood = null,
                validationErrors = emptyMap()
            )
        }
        originalEditingFood = null
    }
    
    /**
     * 下载照片到相册
     */
    fun downloadPhoto() {
        val photoUri = _uiState.value.photoUri ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, downloadSuccess = false) }
            
            val result = photoStorageRepository.savePhotoToGallery(photoUri)
            
            result.fold(
                onSuccess = {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            downloadSuccess = true
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = "下载失败: ${error.message}"
                        )
                    }
                }
            )
        }
    }
    
    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    /**
     * 清除保存成功状态
     */
    fun clearSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }
    
    /**
     * 清除下载成功状态
     */
    fun clearDownloadSuccess() {
        _uiState.update { it.copy(downloadSuccess = false) }
    }
    
    /**
     * 手动触发同步
     */
    fun triggerSync() {
        viewModelScope.launch {
            syncManager.processPendingOperations()
        }
    }
    
    /**
     * 删除食物项
     */
    fun deleteFood(foodId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // 先获取要删除的食物数据（用于更新统计）
                val foodToDelete = _uiState.value.foods.find { it.id == foodId }
                
                val result = mealEditRepository.deleteFoodItem(foodId)
                
                result.fold(
                    onSuccess = {
                        // 更新 DailyNutritionTracker
                        foodToDelete?.let { food ->
                            Log.d(TAG, "删除食物: ${food.chineseName ?: food.name}, 热量=${food.caloriesKcal}")
                            
                            dailyNutritionTracker?.subtractNutrition(
                                calories = food.caloriesKcal,
                                protein = food.proteinG,
                                carbs = food.carbsG,
                                fat = food.fatG,
                                mealTimestamp = currentSnapshotTimestamp
                            )
                            
                            // 发送数据变化事件
                            _dataChangeEvent.emit(
                                FoodDataChangeEvent.FoodDeleted(
                                    foodId = foodId,
                                    calories = food.caloriesKcal,
                                    protein = food.proteinG,
                                    carbs = food.carbsG,
                                    fat = food.fatG,
                                    mealTimestamp = currentSnapshotTimestamp
                                )
                            )
                        }
                        
                        // 从本地列表中移除
                        val updatedFoods = _uiState.value.foods.filter { it.id != foodId }
                        
                        _uiState.update { 
                            it.copy(
                                foods = updatedFoods,
                                isLoading = false,
                                isEditing = false,
                                editingFoodId = null,
                                editingFood = null,
                                saveSuccess = true
                            )
                        }
                        
                        // 尝试同步
                        syncManager.processPendingOperations()
                    },
                    onFailure = { error ->
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                error = "删除失败: ${error.message}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "删除失败: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * 新增食物
     * 
     * @param foodName 食物名称
     * @param weightG 重量(克)
     */
    fun addFood(foodName: String, weightG: Double) {
        val snapshotId = currentSnapshotId ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // 调用 chatNutrition API 获取营养数据
                val nutritionData = queryNutritionFromApi(foodName, weightG)
                
                val (calories, protein, carbs, fat, category) = nutritionData
                
                // 生成新的食物 ID
                val newFoodId = "${snapshotId}_manual_${System.currentTimeMillis()}"
                
                // 创建新的食物实体
                val newFood = SnapshotFoodEntity(
                    id = newFoodId,
                    snapshotId = snapshotId,
                    name = foodName,
                    chineseName = foodName,
                    category = category,
                    weightG = weightG,
                    caloriesKcal = calories,
                    proteinG = protein,
                    carbsG = carbs,
                    fatG = fat,
                    confidence = 0.9,
                    cookingMethod = null,
                    originalWeightG = weightG,
                    isEdited = false
                )
                
                // 保存到数据库
                val result = mealEditRepository.addFoodItem(snapshotId, newFood)
                
                result.fold(
                    onSuccess = { savedFood ->
                        // 更新今日营养统计
                        dailyNutritionTracker?.updateDailyTotals(
                            com.rokid.nutrition.phone.network.model.NutritionTotal(
                                calories = calories,
                                protein = protein,
                                carbs = carbs,
                                fat = fat
                            )
                        )
                        
                        // 发送数据变化事件
                        _dataChangeEvent.emit(
                            FoodDataChangeEvent.FoodAdded(
                                foodId = newFoodId,
                                calories = calories,
                                protein = protein,
                                carbs = carbs,
                                fat = fat,
                                mealTimestamp = currentSnapshotTimestamp
                            )
                        )
                        
                        // 更新本地列表
                        val updatedFoods = _uiState.value.foods + EditableFoodItem.fromEntity(savedFood)
                        
                        _uiState.update { 
                            it.copy(
                                foods = updatedFoods,
                                isLoading = false,
                                saveSuccess = true
                            )
                        }
                        
                        Log.d(TAG, "新增食物成功: $foodName, ${weightG}g, ${calories}kcal (来自API)")
                    },
                    onFailure = { error ->
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                error = "添加失败: ${error.message}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "添加失败: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * 从 chatNutrition API 查询营养数据
     * 
     * @return (calories, protein, carbs, fat, category)
     */
    private suspend fun queryNutritionFromApi(foodName: String, weightG: Double): NutritionQueryResult {
        // 构建查询问题
        val question = "${weightG.toInt()}克${foodName}的热量、蛋白质、碳水化合物和脂肪分别是多少？请用数字回答，格式：热量X千卡，蛋白质Xg，碳水Xg，脂肪Xg"
        
        // 调用 API
        val result = networkManager?.chatNutrition(question)
        
        if (result?.isSuccess == true) {
            val response = result.getOrNull()
            val answer = response?.answer ?: ""
            Log.d(TAG, "chatNutrition 响应: $answer")
            
            // 解析响应文本
            return parseNutritionFromText(answer, foodName, weightG)
        }
        
        // API 调用失败，使用估算值
        Log.w(TAG, "chatNutrition API 调用失败，使用估算值")
        return estimateNutrition(foodName, weightG)
    }
    
    /**
     * 解析文本中的营养数据
     */
    private fun parseNutritionFromText(text: String, foodName: String, weightG: Double): NutritionQueryResult {
        try {
            // 提取热量 (支持多种格式: "热量150千卡", "150kcal", "150大卡")
            val caloriesRegex = """热量[：:]*\s*(\d+(?:\.\d+)?)\s*(?:千卡|大卡|kcal)?|(\d+(?:\.\d+)?)\s*(?:千卡|大卡|kcal)""".toRegex(RegexOption.IGNORE_CASE)
            val caloriesMatch = caloriesRegex.find(text)
            val calories = caloriesMatch?.groupValues?.filterNot { it.isEmpty() }?.lastOrNull()?.toDoubleOrNull()
            
            // 提取蛋白质
            val proteinRegex = """蛋白质[：:]*\s*(\d+(?:\.\d+)?)\s*g?|蛋白[：:]*\s*(\d+(?:\.\d+)?)\s*g?""".toRegex(RegexOption.IGNORE_CASE)
            val proteinMatch = proteinRegex.find(text)
            val protein = proteinMatch?.groupValues?.filterNot { it.isEmpty() }?.lastOrNull()?.toDoubleOrNull()
            
            // 提取碳水化合物
            val carbsRegex = """碳水[化合物]*[：:]*\s*(\d+(?:\.\d+)?)\s*g?""".toRegex(RegexOption.IGNORE_CASE)
            val carbsMatch = carbsRegex.find(text)
            val carbs = carbsMatch?.groupValues?.filterNot { it.isEmpty() }?.lastOrNull()?.toDoubleOrNull()
            
            // 提取脂肪
            val fatRegex = """脂肪[：:]*\s*(\d+(?:\.\d+)?)\s*g?""".toRegex(RegexOption.IGNORE_CASE)
            val fatMatch = fatRegex.find(text)
            val fat = fatMatch?.groupValues?.filterNot { it.isEmpty() }?.lastOrNull()?.toDoubleOrNull()
            
            // 判断食物分类
            val category = inferFoodCategory(foodName)
            
            // 如果成功解析到热量，使用解析结果
            if (calories != null && calories > 0) {
                Log.d(TAG, "解析成功: calories=$calories, protein=$protein, carbs=$carbs, fat=$fat")
                return NutritionQueryResult(
                    calories = calories,
                    protein = protein ?: (calories * 0.1 / 4),  // 估算: 10% 热量来自蛋白质
                    carbs = carbs ?: (calories * 0.5 / 4),       // 估算: 50% 热量来自碳水
                    fat = fat ?: (calories * 0.3 / 9),           // 估算: 30% 热量来自脂肪
                    category = category
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析营养数据失败: ${e.message}")
        }
        
        // 解析失败，使用估算值
        return estimateNutrition(foodName, weightG)
    }
    
    /**
     * 估算营养数据（后备方案）
     */
    private fun estimateNutrition(foodName: String, weightG: Double): NutritionQueryResult {
        // 根据食物名称推断类型并使用不同的估算值
        val (caloriesPer100g, proteinPer100g, carbsPer100g, fatPer100g) = when {
            foodName.contains("米饭") || foodName.contains("白饭") -> listOf(116.0, 2.6, 25.9, 0.3)
            foodName.contains("面") || foodName.contains("面条") -> listOf(137.0, 4.5, 25.0, 0.8)
            foodName.contains("肉") || foodName.contains("鸡") || foodName.contains("猪") || foodName.contains("牛") -> listOf(200.0, 20.0, 0.0, 12.0)
            foodName.contains("鱼") || foodName.contains("虾") -> listOf(100.0, 18.0, 0.0, 2.0)
            foodName.contains("蛋") -> listOf(144.0, 13.0, 1.0, 10.0)
            foodName.contains("奶") || foodName.contains("牛奶") -> listOf(54.0, 3.0, 5.0, 3.0)
            foodName.contains("水果") || foodName.contains("苹果") || foodName.contains("香蕉") || foodName.contains("橙") -> listOf(50.0, 0.5, 12.0, 0.2)
            foodName.contains("蔬菜") || foodName.contains("菜") -> listOf(25.0, 1.5, 4.0, 0.2)
            foodName.contains("饮料") || foodName.contains("可乐") || foodName.contains("果汁") -> listOf(40.0, 0.0, 10.0, 0.0)
            foodName.contains("饼干") || foodName.contains("零食") || foodName.contains("薯片") -> listOf(480.0, 6.0, 65.0, 22.0)
            foodName.contains("蛋糕") || foodName.contains("甜点") -> listOf(350.0, 5.0, 50.0, 15.0)
            else -> listOf(150.0, 8.0, 15.0, 5.0)  // 通用估算
        }
        
        val category = inferFoodCategory(foodName)
        
        return NutritionQueryResult(
            calories = weightG * caloriesPer100g / 100,
            protein = weightG * proteinPer100g / 100,
            carbs = weightG * carbsPer100g / 100,
            fat = weightG * fatPer100g / 100,
            category = category
        )
    }
    
    /**
     * 推断食物分类
     */
    private fun inferFoodCategory(foodName: String): String {
        return when {
            foodName.contains("饮料") || foodName.contains("可乐") || foodName.contains("果汁") || 
            foodName.contains("茶") || foodName.contains("咖啡") || foodName.contains("奶茶") -> "beverage"
            foodName.contains("水果") || foodName.contains("苹果") || foodName.contains("香蕉") || 
            foodName.contains("橙") || foodName.contains("葡萄") || foodName.contains("西瓜") -> "fruit"
            foodName.contains("饼干") || foodName.contains("零食") || foodName.contains("薯片") || 
            foodName.contains("坚果") || foodName.contains("糖果") -> "snack"
            foodName.contains("蛋糕") || foodName.contains("甜点") || foodName.contains("冰淇淋") || 
            foodName.contains("巧克力") || foodName.contains("布丁") -> "dessert"
            else -> "meal"
        }
    }
}

/**
 * 营养查询结果
 */
private data class NutritionQueryResult(
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val category: String
)
