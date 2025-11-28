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
}

/**
 * 食物详情 ViewModel
 */
class FoodDetailViewModel(
    private val mealEditRepository: MealEditRepository,
    private val photoStorageRepository: PhotoStorageRepository,
    private val syncManager: SyncManager,
    private val dailyNutritionTracker: DailyNutritionTracker? = null
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
                    val photoSource = photoStorageRepository.getPhotoSource(snapshotWithFoods.snapshot.id)
                    val photoUri = when (photoSource) {
                        is PhotoSource.Local -> photoSource.path
                        is PhotoSource.Remote -> photoSource.url
                        null -> snapshotWithFoods.snapshot.imageUrl.takeIf { it.isNotBlank() }
                    }
                    android.util.Log.d("FoodDetailViewModel", "Photo URI: $photoUri")
                    
                    // 保存快照时间戳
                    currentSnapshotTimestamp = snapshotWithFoods.snapshot.capturedAt
                    
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
}
