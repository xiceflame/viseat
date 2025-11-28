package com.rokid.nutrition.phone.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.rokid.nutrition.phone.network.model.*

/**
 * 用餐会话管理器
 * 
 * 负责：
 * - 会话状态管理（基线数据、容错回退、加菜处理）
 * - 状态持久化（防止 App 被杀后丢失）
 * - 判断是否应该激活用餐监测
 */
class MealSessionManager(context: Context) {
    
    companion object {
        private const val TAG = "MealSessionManager"
        private const val PREFS_NAME = "meal_session"
        private const val KEY_SESSION_STATE = "session_state"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    private var _state: MealSessionState? = null
    val state: MealSessionState? get() = _state
    
    val isActive: Boolean get() = _state?.status == "active"
    
    init {
        // 从本地恢复会话状态
        restoreState()
    }
    
    /**
     * 开始用餐会话
     */
    fun startSession(
        sessionId: String,
        baselineFoods: List<BaselineFood>,
        baselineImageUrl: String,
        baselineNutrition: NutritionTotal
    ) {
        _state = MealSessionState(
            sessionId = sessionId,
            status = "active",
            baselineFoods = baselineFoods,
            baselineImageUrl = baselineImageUrl,
            baselineNutrition = baselineNutrition,
            lastValidFoods = baselineFoods,
            lastValidNutrition = baselineNutrition,
            currentFoods = baselineFoods,
            currentNutrition = baselineNutrition
        )
        saveState()
        Log.d(TAG, "会话已开始: $sessionId")
    }
    
    /**
     * 处理用餐更新响应
     */
    fun handleUpdateResponse(response: MealUpdateAnalyzeResponse) {
        val currentState = _state ?: return
        
        when (response.status) {
            "accept" -> {
                // 正常：更新状态
                val newFoods = response.rawLlm.foods.map { it.toBaselineFood() }
                currentState.lastValidFoods = newFoods
                currentState.lastValidNutrition = response.snapshot.nutrition
                currentState.currentFoods = newFoods
                currentState.currentNutrition = response.snapshot.nutrition
                Log.d(TAG, "更新接受: ${response.message}")
            }
            "skip" -> {
                // 拍摄问题：保持上次数据，静默跳过
                Log.w(TAG, "跳过本次更新: ${response.message}")
                // currentFoods 保持不变
            }
            "adjust" -> {
                // 加菜：更新基线
                response.comparison?.adjustments?.let { handleAdjustments(it) }
                val newFoods = response.rawLlm.foods.map { it.toBaselineFood() }
                currentState.currentFoods = newFoods
                currentState.currentNutrition = response.snapshot.nutrition
                Log.d(TAG, "加菜调整: ${response.message}")
            }
        }
        saveState()
    }
    
    /**
     * 处理加菜调整
     */
    private fun handleAdjustments(adjustments: List<Adjustment>) {
        val currentState = _state ?: return
        
        for (adj in adjustments) {
            when (adj.action) {
                "add_new" -> {
                    // 新增食材到基线
                    val newFood = BaselineFood(
                        dishName = "新增菜品",
                        dishNameCn = "新增菜品",
                        ingredients = listOf(BaselineIngredient(adj.ingredient, adj.weight)),
                        totalWeightG = adj.weight
                    )
                    currentState.baselineFoods = currentState.baselineFoods + newFood
                    // 更新基线热量（简化：按 1kcal/g 估算）
                    currentState.baselineNutrition = currentState.baselineNutrition.copy(
                        calories = currentState.baselineNutrition.calories + adj.weight
                    )
                    Log.d(TAG, "新增食材: ${adj.ingredient}, ${adj.weight}g")
                }
            }
        }
    }
    
    /**
     * 结束会话
     */
    fun endSession() {
        _state?.status = "ended"
        saveState()
        Log.d(TAG, "会话已结束")
    }
    
    /**
     * 清除会话
     */
    fun clearSession() {
        _state = null
        prefs.edit().remove(KEY_SESSION_STATE).apply()
        Log.d(TAG, "会话已清除")
    }
    
    /**
     * 获取基线食物列表
     */
    fun getBaselineFoods(): List<BaselineFood> = _state?.baselineFoods ?: emptyList()
    
    /**
     * 计算已消耗营养
     */
    fun getTotalConsumed(): NutritionTotal? {
        val state = _state ?: return null
        return NutritionTotal(
            calories = state.baselineNutrition.calories - state.currentNutrition.calories,
            protein = state.baselineNutrition.protein - state.currentNutrition.protein,
            carbs = state.baselineNutrition.carbs - state.currentNutrition.carbs,
            fat = state.baselineNutrition.fat - state.currentNutrition.fat
        )
    }
    
    private fun saveState() {
        _state?.let {
            val json = gson.toJson(it)
            prefs.edit().putString(KEY_SESSION_STATE, json).apply()
        }
    }
    
    private fun restoreState() {
        val json = prefs.getString(KEY_SESSION_STATE, null)
        if (json != null) {
            try {
                _state = gson.fromJson(json, MealSessionState::class.java)
                Log.d(TAG, "恢复会话状态: ${_state?.sessionId}")
            } catch (e: Exception) {
                Log.e(TAG, "恢复会话状态失败", e)
            }
        }
    }
}

/**
 * 会话状态
 */
data class MealSessionState(
    val sessionId: String,
    var status: String,  // "active" | "ended"
    // 基线数据（首次识别，加菜时更新）
    var baselineFoods: List<BaselineFood>,
    var baselineImageUrl: String,
    var baselineNutrition: NutritionTotal,
    // 上次有效识别（容错回退用）
    var lastValidFoods: List<BaselineFood>,
    var lastValidNutrition: NutritionTotal,
    // 当前剩余
    var currentFoods: List<BaselineFood>,
    var currentNutrition: NutritionTotal
)

/**
 * FoodItemResponse 转 BaselineFood
 */
fun FoodItemResponse.toBaselineFood(): BaselineFood {
    return BaselineFood(
        dishName = dishName,
        dishNameCn = dishNameCn ?: dishName,
        ingredients = ingredients.map { 
            BaselineIngredient(it.nameEn, it.weightG.toFloat(), it.confidence.toFloat()) 
        },
        totalWeightG = totalWeightG.toFloat()
    )
}
