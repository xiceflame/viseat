package com.rokid.nutrition.phone.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rokid.nutrition.phone.domain.*
import com.rokid.nutrition.phone.repository.UserManager
import com.rokid.nutrition.phone.repository.UserProfile
import com.rokid.nutrition.phone.repository.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Onboarding 步骤
 */
enum class OnboardingStep {
    WELCOME,
    GOAL,
    BODY_DATA,
    TARGET_WEIGHT,
    ACTIVITY,
    DIETARY,
    SUMMARY
}

/**
 * Onboarding 状态
 */
data class OnboardingState(
    val currentStep: OnboardingStep = OnboardingStep.WELCOME,
    
    // 步骤数据
    val nickname: String = "",  // 用户昵称/称呼
    val healthGoal: HealthGoal? = null,
    val gender: Gender? = null,
    val birthYear: Int = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) - 27, // 默认 27 岁
    val birthMonth: Int = 1,
    val birthDay: Int = 1,
    val height: Float = 175f, // 默认 175cm
    val weight: Float = 70f,  // 默认 70kg
    val targetWeight: Float? = null,
    val targetDate: LocalDate? = null,
    val activityLevel: ActivityLevel? = null,
    val dietType: DietType = DietType.OMNIVORE,
    val allergens: Set<Allergen> = emptySet(),
    
    // 计算结果
    val bmi: Float? = null,
    val age: Int? = null,
    val weeklyRate: Float? = null,
    val isWeeklyRateSafe: Boolean = true,
    val nutritionGoals: NutritionGoals? = null,
    
    // 状态
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val currentStepIndex: Int
        get() = when (currentStep) {
            OnboardingStep.WELCOME -> 0
            OnboardingStep.GOAL -> 1
            OnboardingStep.BODY_DATA -> 2
            OnboardingStep.TARGET_WEIGHT -> 3
            OnboardingStep.ACTIVITY -> if (healthGoal == HealthGoal.MAINTAIN) 3 else 4
            OnboardingStep.DIETARY -> if (healthGoal == HealthGoal.MAINTAIN) 4 else 5
            OnboardingStep.SUMMARY -> if (healthGoal == HealthGoal.MAINTAIN) 5 else 6
        }
    
    val totalSteps: Int
        get() = if (healthGoal == HealthGoal.MAINTAIN) 5 else 6
    
    val birthDate: LocalDate
        get() = LocalDate.of(birthYear, birthMonth, birthDay)
}

/**
 * Onboarding ViewModel
 */
class OnboardingViewModel(
    private val userProfileRepository: UserProfileRepository,
    private val userManager: UserManager? = null
) : ViewModel() {
    
    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()
    
    /**
     * 开始引导
     */
    fun startOnboarding() {
        _state.update { it.copy(currentStep = OnboardingStep.GOAL) }
    }
    
    /**
     * 下一步
     */
    fun nextStep() {
        _state.update { currentState ->
            val nextStep = when (currentState.currentStep) {
                OnboardingStep.WELCOME -> OnboardingStep.GOAL
                OnboardingStep.GOAL -> OnboardingStep.BODY_DATA
                OnboardingStep.BODY_DATA -> {
                    // 如果是维持体重，跳过目标体重步骤
                    if (currentState.healthGoal == HealthGoal.MAINTAIN) {
                        OnboardingStep.ACTIVITY
                    } else {
                        OnboardingStep.TARGET_WEIGHT
                    }
                }
                OnboardingStep.TARGET_WEIGHT -> OnboardingStep.ACTIVITY
                OnboardingStep.ACTIVITY -> OnboardingStep.DIETARY
                OnboardingStep.DIETARY -> {
                    // 计算营养目标
                    calculateNutritionGoals()
                    OnboardingStep.SUMMARY
                }
                OnboardingStep.SUMMARY -> OnboardingStep.SUMMARY
            }
            currentState.copy(currentStep = nextStep)
        }
    }
    
    /**
     * 上一步
     */
    fun previousStep() {
        _state.update { currentState ->
            val prevStep = when (currentState.currentStep) {
                OnboardingStep.WELCOME -> OnboardingStep.WELCOME
                OnboardingStep.GOAL -> OnboardingStep.WELCOME
                OnboardingStep.BODY_DATA -> OnboardingStep.GOAL
                OnboardingStep.TARGET_WEIGHT -> OnboardingStep.BODY_DATA
                OnboardingStep.ACTIVITY -> {
                    if (currentState.healthGoal == HealthGoal.MAINTAIN) {
                        OnboardingStep.BODY_DATA
                    } else {
                        OnboardingStep.TARGET_WEIGHT
                    }
                }
                OnboardingStep.DIETARY -> OnboardingStep.ACTIVITY
                OnboardingStep.SUMMARY -> OnboardingStep.DIETARY
            }
            currentState.copy(currentStep = prevStep)
        }
    }
    
    /**
     * 设置昵称
     */
    fun setNickname(nickname: String) {
        _state.update { it.copy(nickname = nickname) }
    }
    
    /**
     * 设置健康目标
     */
    fun setHealthGoal(goal: HealthGoal) {
        _state.update { it.copy(healthGoal = goal) }
    }
    
    /**
     * 设置性别
     */
    fun setGender(gender: Gender) {
        _state.update { currentState ->
            // 根据性别设置默认的身高和体重
            val defaultHeight = if (gender == Gender.MALE) 175f else 162f
            val defaultWeight = if (gender == Gender.MALE) 70f else 52f
            
            val newState = currentState.copy(
                gender = gender,
                height = defaultHeight,
                weight = defaultWeight,
                targetWeight = defaultWeight // 目标体重也同步更新为该性别的默认体重
            )
            updateBMI(newState)
        }
    }
    
    /**
     * 设置出生日期
     */
    fun setBirthDate(year: Int, month: Int, day: Int) {
        _state.update { 
            val newState = it.copy(
                birthYear = year,
                birthMonth = month,
                birthDay = day,
                age = NutritionCalculator.calculateAge(LocalDate.of(year, month, day))
            )
            newState
        }
    }
    
    /**
     * 设置身高
     */
    fun setHeight(height: Float) {
        _state.update { 
            val newState = it.copy(height = height)
            updateBMI(newState)
        }
    }
    
    /**
     * 设置体重
     */
    fun setWeight(weight: Float) {
        _state.update { 
            val newState = it.copy(weight = weight, targetWeight = it.targetWeight ?: weight)
            updateBMI(newState)
        }
    }
    
    /**
     * 设置目标体重
     */
    fun setTargetWeight(weight: Float) {
        _state.update { 
            val newState = it.copy(targetWeight = weight)
            updateWeeklyRate(newState)
        }
    }
    
    /**
     * 设置目标日期
     */
    fun setTargetDate(date: LocalDate) {
        _state.update { 
            val newState = it.copy(targetDate = date)
            updateWeeklyRate(newState)
        }
    }
    
    /**
     * 设置活动量
     */
    fun setActivityLevel(level: ActivityLevel) {
        _state.update { it.copy(activityLevel = level) }
    }
    
    /**
     * 设置饮食类型
     */
    fun setDietType(type: DietType) {
        _state.update { it.copy(dietType = type) }
    }
    
    /**
     * 切换过敏原
     */
    fun toggleAllergen(allergen: Allergen) {
        _state.update { currentState ->
            val newAllergens = if (allergen in currentState.allergens) {
                currentState.allergens - allergen
            } else {
                currentState.allergens + allergen
            }
            currentState.copy(allergens = newAllergens)
        }
    }
    
    /**
     * 清除所有过敏原
     */
    fun clearAllergens() {
        _state.update { it.copy(allergens = emptySet()) }
    }
    
    /**
     * 更新 BMI
     */
    private fun updateBMI(state: OnboardingState): OnboardingState {
        val bmi = if (state.height > 0 && state.weight > 0) {
            NutritionCalculator.calculateBMI(state.height, state.weight)
        } else null
        return state.copy(bmi = bmi)
    }
    
    /**
     * 更新每周速率
     */
    private fun updateWeeklyRate(state: OnboardingState): OnboardingState {
        val weeklyRate = if (state.targetWeight != null && state.targetDate != null) {
            NutritionCalculator.calculateWeeklyRate(state.weight, state.targetWeight, state.targetDate)
        } else null
        
        val isSafe = weeklyRate?.let { rate ->
            state.healthGoal?.let { goal ->
                NutritionCalculator.isWeeklyRateSafe(rate, goal)
            }
        } ?: true
        
        return state.copy(weeklyRate = weeklyRate, isWeeklyRateSafe = isSafe)
    }
    
    /**
     * 计算营养目标
     */
    private fun calculateNutritionGoals() {
        val currentState = _state.value
        
        if (currentState.gender != null && currentState.activityLevel != null && currentState.healthGoal != null) {
            val age = currentState.age ?: NutritionCalculator.calculateAge(currentState.birthDate)
            
            val goals = NutritionCalculator.calculateNutritionGoals(
                gender = currentState.gender,
                weightKg = currentState.weight,
                heightCm = currentState.height,
                age = age,
                activityLevel = currentState.activityLevel,
                healthGoal = currentState.healthGoal,
                dietType = currentState.dietType
            )
            
            _state.update { it.copy(nutritionGoals = goals, age = age) }
        }
    }
    
    /**
     * 完成引导并保存档案
     */
    fun completeOnboarding(onComplete: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            try {
                val currentState = _state.value
                val age = currentState.age ?: NutritionCalculator.calculateAge(currentState.birthDate)
                
                val profile = UserProfile(
                    nickname = currentState.nickname.ifBlank { null },
                    age = age,
                    gender = currentState.gender?.value ?: "male",
                    birthDate = currentState.birthDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    height = currentState.height,
                    weight = currentState.weight,
                    bmi = currentState.bmi ?: 0f,
                    activityLevel = currentState.activityLevel?.value ?: "moderate",
                    healthGoal = currentState.healthGoal?.value ?: "maintain",
                    targetWeight = currentState.targetWeight,
                    targetDate = currentState.targetDate?.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    dietType = currentState.dietType.value,
                    allergens = currentState.allergens.map { it.value },
                    healthConditions = emptyList(),
                    dietaryPreferences = emptyList(),
                    isOnboardingCompleted = true
                )
                
                userProfileRepository.saveProfile(profile)
                
                // 标记用户已在本设备完成引导
                userManager?.setOnboardingCompletedLocally(true)
                
                _state.update { it.copy(isLoading = false) }
                onComplete()
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
    
    /**
     * 跳过引导
     */
    fun skipOnboarding(onSkip: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            try {
                // 保存默认档案
                userProfileRepository.saveDefaultProfile()
                
                // 标记用户已在本设备完成引导（跳过也算完成）
                userManager?.setOnboardingCompletedLocally(true)
                
                _state.update { it.copy(isLoading = false) }
                onSkip()
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
