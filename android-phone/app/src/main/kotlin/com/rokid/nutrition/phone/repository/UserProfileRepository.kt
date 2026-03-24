package com.rokid.nutrition.phone.repository

import android.util.Log
import com.google.gson.Gson
import com.rokid.nutrition.phone.data.dao.UserProfileDao
import com.rokid.nutrition.phone.data.entity.UserProfileEntity
import com.rokid.nutrition.phone.network.NetworkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 用户档案仓库
 * 
 * 负责本地存储和后端同步
 */
class UserProfileRepository(
    private val userProfileDao: UserProfileDao,
    private val networkManager: NetworkManager? = null
) {
    companion object {
        private const val TAG = "UserProfileRepository"
    }
    
    private val gson = Gson()
    
    /**
     * 获取用户档案 Flow
     */
    fun getProfileFlow(): Flow<UserProfile?> {
        return userProfileDao.getProfileFlow().map { entity ->
            entity?.toUserProfile()
        }
    }
    
    /**
     * 获取用户档案
     */
    suspend fun getProfile(): UserProfile? {
        return userProfileDao.getProfile()?.toUserProfile()
    }
    
    /**
     * 检查是否有用户档案
     */
    suspend fun hasProfile(): Boolean {
        return userProfileDao.hasProfile()
    }
    
    /**
     * 保存用户档案（本地）
     */
    suspend fun saveProfile(profile: UserProfile) {
        val now = System.currentTimeMillis()
        val entity = UserProfileEntity(
            id = "default",
            nickname = profile.nickname,
            age = profile.age,
            gender = profile.gender,
            birthDate = profile.birthDate,
            height = profile.height,
            weight = profile.weight,
            bmi = calculateBMI(profile.height, profile.weight),
            activityLevel = profile.activityLevel,
            healthGoal = profile.healthGoal,
            targetWeight = profile.targetWeight,
            targetDate = profile.targetDate,
            dietType = profile.dietType,
            allergens = gson.toJson(profile.allergens),
            healthConditions = gson.toJson(profile.healthConditions),
            dietaryPreferences = gson.toJson(profile.dietaryPreferences),
            isOnboardingCompleted = profile.isOnboardingCompleted,
            createdAt = now,
            updatedAt = now
        )
        userProfileDao.saveProfile(entity)
    }
    
    /**
     * 检查是否完成引导
     */
    suspend fun isOnboardingCompleted(): Boolean {
        return userProfileDao.getProfile()?.isOnboardingCompleted ?: false
    }
    
    /**
     * 标记引导完成
     */
    suspend fun markOnboardingCompleted() {
        val profile = getProfile()
        if (profile != null) {
            saveProfile(profile.copy(isOnboardingCompleted = true))
        }
    }
    
    /**
     * 创建默认用户档案（用于跳过引导）
     */
    fun createDefaultProfile(): UserProfile {
        return UserProfile(
            age = 25,
            height = 170f,
            weight = 65f,
            bmi = 22.5f,
            healthConditions = emptyList(),
            dietaryPreferences = emptyList(),
            isOnboardingCompleted = true
        )
    }

    /**
     * 保存默认用户档案（用于跳过引导）
     */
    suspend fun saveDefaultProfile() {
        saveProfile(createDefaultProfile())
    }
    
    /**
     * 保存用户档案并同步到后端
     * 
     * @param profile 用户档案
     * @param userId 用户ID（用于后端同步）
     * @return 包含后端计算的 BMI 和 target_calories
     */
    suspend fun saveProfileAndSync(profile: UserProfile, userId: String): Result<UserProfile> {
        // 1. 先保存到本地
        saveProfile(profile)
        
        // 2. 同步到后端
        if (networkManager != null) {
            try {
                val result = networkManager.updateUserProfile(
                    userId = userId,
                    age = profile.age,
                    gender = profile.gender,
                    height = profile.height,
                    weight = profile.weight,
                    activityLevel = profile.activityLevel,
                    healthGoal = profile.healthGoal,
                    targetWeight = profile.targetWeight,
                    healthConditions = profile.healthConditions,
                    dietaryPreferences = profile.dietaryPreferences,
                    // 新增字段
                    birthDate = profile.birthDate,
                    targetDate = profile.targetDate,
                    dietType = profile.dietType,
                    allergens = profile.allergens
                )
                
                result.fold(
                    onSuccess = { response ->
                        // 使用后端返回的 BMI 和 target_calories 更新本地
                        val updatedProfile = profile.copy(
                            bmi = response.profile.bmi ?: profile.bmi
                        )
                        saveProfile(updatedProfile)
                        Log.d(TAG, "用户档案同步成功: userId=$userId, bmi=${response.profile.bmi}, targetCalories=${response.profile.targetCalories}")
                        return Result.success(updatedProfile)
                    },
                    onFailure = { e ->
                        Log.e(TAG, "用户档案同步失败", e)
                        return Result.failure(e)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "用户档案同步异常", e)
                return Result.failure(e)
            }
        }
        
        return Result.success(profile)
    }
    
    /**
     * 从后端同步用户档案到本地（通过 device_id）
     * 
     * 支持增强版字段：birthDate, targetDate, dietType, allergens
     */
    suspend fun syncFromBackend(deviceId: String): Result<UserProfile?> {
        if (networkManager == null) {
            return Result.failure(IllegalStateException("NetworkManager not available"))
        }
        
        return try {
            val result = networkManager.getUserProfile(deviceId)
            result.fold(
                onSuccess = { response ->
                    val profileData = response.profile
                    if (profileData != null) {
                        val profile = UserProfile(
                            nickname = null,  // 后端暂不支持昵称
                            age = profileData.age ?: 25,
                            gender = profileData.gender ?: "male",
                            birthDate = profileData.birthDate,  // 新增：出生日期
                            height = profileData.height ?: 170f,
                            weight = profileData.weight ?: 65f,
                            bmi = profileData.bmi ?: 0f,
                            activityLevel = profileData.activityLevel ?: "moderate",
                            healthGoal = profileData.healthGoal ?: "maintain",
                            targetWeight = profileData.targetWeight,
                            targetDate = profileData.targetDate,  // 新增：目标日期
                            dietType = profileData.dietType ?: "omnivore",  // 新增：饮食类型
                            allergens = profileData.allergens ?: emptyList(),  // 新增：过敏原
                            healthConditions = profileData.healthConditions ?: emptyList(),
                            dietaryPreferences = profileData.dietaryPreferences ?: emptyList(),
                            isOnboardingCompleted = true  // 从后端恢复的数据视为已完成引导
                        )
                        saveProfile(profile)
                        Log.d(TAG, "从后端同步档案成功: deviceId=$deviceId, birthDate=${profileData.birthDate}, dietType=${profileData.dietType}")
                        Result.success(profile)
                    } else {
                        Log.d(TAG, "后端无档案数据: deviceId=$deviceId")
                        Result.success(null)
                    }
                },
                onFailure = { e ->
                    Log.e(TAG, "从后端同步档案失败", e)
                    Result.failure(e)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "从后端同步档案异常", e)
            Result.failure(e)
        }
    }
    
    /**
     * 计算 BMI
     * BMI = 体重(kg) / 身高(m)²
     */
    fun calculateBMI(heightCm: Float, weightKg: Float): Float {
        if (heightCm <= 0 || weightKg <= 0) return 0f
        val heightM = heightCm / 100f
        return (weightKg / (heightM * heightM)).let {
            // 保留一位小数
            (it * 10).toInt() / 10f
        }
    }
    
    private fun UserProfileEntity.toUserProfile(): UserProfile {
        return UserProfile(
            nickname = nickname,
            age = age,
            gender = gender,
            birthDate = birthDate,
            height = height,
            weight = weight,
            bmi = bmi,
            activityLevel = activityLevel,
            healthGoal = healthGoal,
            targetWeight = targetWeight,
            targetDate = targetDate,
            dietType = dietType,
            allergens = try {
                gson.fromJson(allergens, Array<String>::class.java).toList()
            } catch (e: Exception) {
                emptyList()
            },
            healthConditions = try {
                gson.fromJson(healthConditions, Array<String>::class.java).toList()
            } catch (e: Exception) {
                emptyList()
            },
            dietaryPreferences = try {
                gson.fromJson(dietaryPreferences, Array<String>::class.java).toList()
            } catch (e: Exception) {
                emptyList()
            },
            isOnboardingCompleted = isOnboardingCompleted
        )
    }
}

/**
 * 用户档案领域模型 - 增强版
 */
data class UserProfile(
    val nickname: String? = null,  // 用户昵称
    val age: Int,
    val gender: String = "male",  // male/female
    val birthDate: String? = null,  // 出生日期 yyyy-MM-dd
    val height: Float,  // cm
    val weight: Float,  // kg
    val bmi: Float,
    val activityLevel: String = "moderate",  // sedentary/light/moderate/active/very_active
    val healthGoal: String = "maintain",  // lose_weight/gain_muscle/maintain
    val targetWeight: Float? = null,  // 目标体重 kg
    val targetDate: String? = null,  // 目标日期 yyyy-MM-dd
    val dietType: String = "omnivore",  // omnivore/vegetarian/vegan/low_carb/keto/mediterranean
    val allergens: List<String> = emptyList(),  // ["gluten", "dairy", "nuts"]
    val healthConditions: List<String>,
    val dietaryPreferences: List<String>,
    val isOnboardingCompleted: Boolean = false
) {
    /**
     * 计算每日热量目标 (基于 Mifflin-St Jeor 公式)
     */
    fun calculateDailyCalories(): Int {
        // 基础代谢率 (BMR)
        val bmr = if (gender == "male") {
            10 * weight + 6.25 * height - 5 * age + 5
        } else {
            10 * weight + 6.25 * height - 5 * age - 161
        }
        
        // 活动系数
        val activityMultiplier = when (activityLevel) {
            "sedentary" -> 1.2      // 久坐
            "light" -> 1.375        // 轻度活动
            "moderate" -> 1.55      // 中度活动
            "active" -> 1.725       // 高度活动
            "very_active" -> 1.9    // 极高活动
            else -> 1.55
        }
        
        // 总热量消耗 (TDEE)
        val tdee = bmr * activityMultiplier
        
        // 根据健康目标调整
        return when (healthGoal) {
            "lose_weight" -> (tdee * 0.8).toInt()   // 减重：减少20%
            "gain_muscle" -> (tdee * 1.1).toInt()  // 增肌：增加10%
            else -> tdee.toInt()                    // 维持
        }
    }
}

/**
 * 体重记录领域模型
 */
data class WeightEntry(
    val id: String,
    val weight: Float,
    val note: String? = null,
    val recordedAt: Long
)
