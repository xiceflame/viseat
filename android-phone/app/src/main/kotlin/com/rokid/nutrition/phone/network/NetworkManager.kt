package com.rokid.nutrition.phone.network

import android.util.Log
import com.rokid.nutrition.phone.Config
import com.rokid.nutrition.phone.network.model.*
import com.rokid.nutrition.phone.util.DebugLogger
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 网络管理器
 * 
 * 封装 Retrofit API 调用，提供重试逻辑
 */
class NetworkManager {
    
    companion object {
        private const val TAG = "NetworkManager"
        
        @Volatile
        private var instance: NetworkManager? = null
        
        fun getInstance(): NetworkManager {
            return instance ?: synchronized(this) {
                instance ?: NetworkManager().also { instance = it }
            }
        }
    }
    
    private val api: ApiService
    
    // 公开 API 服务供其他组件使用
    val apiService: ApiService get() = api
    
    init {
        // 自定义拦截器，记录请求和响应到 DebugLogger
        val debugInterceptor = Interceptor { chain ->
            val request = chain.request()
            
            // 记录请求
            val requestLog = buildString {
                appendLine(">>> REQUEST: ${request.method} ${request.url}")
                request.headers.forEach { (name, value) ->
                    appendLine("  $name: $value")
                }
                request.body?.let { body ->
                    val buffer = Buffer()
                    body.writeTo(buffer)
                    val bodyStr = buffer.readUtf8()
                    if (bodyStr.length < 1000) {  // 只记录小于 1KB 的请求体
                        appendLine("  Body: $bodyStr")
                    } else {
                        appendLine("  Body: (${bodyStr.length} bytes)")
                    }
                }
            }
            DebugLogger.network(TAG, requestLog)
            
            // 执行请求
            val response: Response
            try {
                response = chain.proceed(request)
            } catch (e: Exception) {
                DebugLogger.e(TAG, "请求失败: ${e.message}", e)
                throw e
            }
            
            // 记录响应
            val responseBody = response.peekBody(10 * 1024)  // 最多读取 10KB
            val responseLog = buildString {
                appendLine("<<< RESPONSE: ${response.code} ${response.message}")
                appendLine("  URL: ${request.url}")
                val bodyStr = responseBody.string()
                if (bodyStr.length < 2000) {
                    appendLine("  Body: $bodyStr")
                } else {
                    appendLine("  Body: (${bodyStr.length} bytes)")
                }
            }
            DebugLogger.network(TAG, responseLog)
            
            response
        }
        
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(Config.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(Config.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(Config.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(debugInterceptor)
            .build()
        
        // 配置 Gson：使用 @SerializedName 注解，不序列化 null 值
        val gson = GsonBuilder()
            .setLenient()
            .serializeNulls()  // 序列化 null 值（后端 FastAPI 可以处理）
            .create()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(Config.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        
        api = retrofit.create(ApiService::class.java)
    }

    
    /**
     * 上传图片并分析
     */
    suspend fun uploadAndAnalyze(
        imageData: ByteArray,
        userProfile: UserProfilePayload? = null
    ): Result<VisionAnalyzeResponse> {
        return uploadAndAnalyzeWithProgress(imageData, userProfile, {}, {})
    }
    
    /**
     * 上传图片并分析（带进度回调）
     * 
     * @param imageData 图片数据
     * @param userProfile 用户档案
     * @param onUploadComplete 上传完成回调（参数为图片URL）
     * @param onAnalyzeComplete 分析完成回调
     */
    suspend fun uploadAndAnalyzeWithProgress(
        imageData: ByteArray,
        userProfile: UserProfilePayload? = null,
        onUploadComplete: (imageUrl: String) -> Unit,
        onAnalyzeComplete: () -> Unit
    ): Result<VisionAnalyzeResponse> {
        return withRetry {
            // 1. 上传图片
            val requestBody = imageData.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData(
                "file",
                "image_${System.currentTimeMillis()}.jpg",
                requestBody
            )
            DebugLogger.i(TAG, "开始上传图片，大小: ${imageData.size} bytes")
            val uploadResponse = api.uploadImage(part)
            DebugLogger.i(TAG, "图片上传成功: ${uploadResponse.url}")
            
            // 2. 分析图片 - 需要拼接完整 URL，因为 Qwen-VL 需要可访问的完整 URL
            val fullImageUrl = if (uploadResponse.url.startsWith("http")) {
                uploadResponse.url
            } else {
                "${Config.API_BASE_URL}${uploadResponse.url}"
            }
            DebugLogger.i(TAG, "完整图片 URL: $fullImageUrl")
            
            // 通知上传完成，传递图片URL
            onUploadComplete(fullImageUrl)
            
            // 手动构建 JSON 字符串，完全绕过 Gson 序列化
            val jsonBody = """{"image_url": "$fullImageUrl"}"""
            DebugLogger.i(TAG, "发送分析请求 JSON: $jsonBody")
            
            val analyzeRequestBody = jsonBody.toRequestBody("application/json".toMediaTypeOrNull())
            val analyzeResponse = api.analyzeVision(analyzeRequestBody)
            DebugLogger.i(TAG, "图片分析成功: ${analyzeResponse.rawLlm.foods.size} 种食物")
            
            // 通知分析完成
            onAnalyzeComplete()
            
            analyzeResponse
        }
    }
    
    /**
     * 开始用餐会话
     */
    suspend fun startMeal(
        userId: String,
        mealType: String,
        visionResult: VisionAnalyzeResponse
    ): Result<MealStartResponse> {
        return withRetry {
            val snapshot = SnapshotPayload(
                imageUrl = "",  // 已在 vision/analyze 中处理
                foods = visionResult.snapshot.foods,
                nutrition = visionResult.snapshot.nutrition
            )
            api.startMealSession(userId, mealType, snapshot)
        }
    }
    
    /**
     * 更新用餐会话
     */
    suspend fun updateMeal(
        sessionId: String,
        visionResult: VisionAnalyzeResponse
    ): Result<MealUpdateResponse> {
        return withRetry {
            val snapshot = SnapshotPayload(
                imageUrl = "",
                foods = visionResult.snapshot.foods,
                nutrition = visionResult.snapshot.nutrition
            )
            api.updateMealSession(sessionId, snapshot)
        }
    }
    
    /**
     * 结束用餐会话
     * 
     * @param sessionId 会话ID
     * @param mealContext 用餐上下文（可选）
     * @param dailyContext 今日上下文（可选）
     * @param userProfile 用户档案（可选）
     * @param finalSnapshot 最终快照（可选）
     */
    suspend fun endMeal(
        sessionId: String,
        mealContext: MealContextPayload? = null,
        dailyContext: DailyContextPayload? = null,
        userProfile: UserProfilePayload? = null,
        finalSnapshot: SnapshotPayload? = null
    ): Result<MealEndResponse> {
        return withRetry {
            val request = MealEndRequest(
                sessionId = sessionId,
                mealContext = mealContext,
                dailyContext = dailyContext,
                userProfile = userProfile,
                finalSnapshot = finalSnapshot
            )
            DebugLogger.i(TAG, "结束用餐会话: sessionId=$sessionId, hasMealContext=${mealContext != null}, hasDailyContext=${dailyContext != null}")
            api.endMealSession(request)
        }
    }
    
    /**
     * 获取会话详情
     */
    suspend fun getMealSession(sessionId: String): Result<MealSessionDetailResponse> {
        return withRetry {
            api.getMealSession(sessionId)
        }
    }
    
    /**
     * 获取会话列表
     */
    suspend fun getMealSessions(
        userId: String,
        limit: Int = 20,
        offset: Int = 0
    ): Result<MealSessionsListResponse> {
        return withRetry {
            api.getMealSessions(userId, limit, offset)
        }
    }
    
    /**
     * 营养对话
     */
    suspend fun chatNutrition(
        question: String,
        sessionId: String? = null,
        userProfile: UserProfilePayload? = null
    ): Result<ChatNutritionResponse> {
        return withRetry {
            val request = ChatNutritionRequest(question, sessionId, userProfile)
            api.chatNutrition(request)
        }
    }
    
    /**
     * 用餐更新分析（带容错）
     * 
     * @param imageUrl 图片 URL
     * @param baselineFoods 基线食物列表
     * @return 更新结果，包含 status: accept/skip/adjust
     */
    suspend fun analyzeMealUpdate(
        imageUrl: String,
        baselineFoods: List<BaselineFood>
    ): Result<MealUpdateAnalyzeResponse> {
        return withRetry {
            val request = MealUpdateAnalyzeRequest(
                imageUrl = imageUrl,
                baselineFoods = baselineFoods
            )
            api.analyzeMealUpdate(request)
        }
    }
    
    /**
     * 上传图片并进行用餐更新分析（带进度回调）
     */
    suspend fun uploadAndAnalyzeMealUpdate(
        imageData: ByteArray,
        baselineFoods: List<BaselineFood>,
        onUploadComplete: () -> Unit,
        onAnalyzeComplete: () -> Unit
    ): Result<MealUpdateAnalyzeResponse> {
        return withRetry {
            // 1. 上传图片
            val requestBody = imageData.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData(
                "file",
                "image_${System.currentTimeMillis()}.jpg",
                requestBody
            )
            DebugLogger.i(TAG, "开始上传图片（用餐更新），大小: ${imageData.size} bytes")
            val uploadResponse = api.uploadImage(part)
            DebugLogger.i(TAG, "图片上传成功: ${uploadResponse.url}")
            
            onUploadComplete()
            
            // 2. 用餐更新分析
            val fullImageUrl = if (uploadResponse.url.startsWith("http")) {
                uploadResponse.url
            } else {
                "${Config.API_BASE_URL}${uploadResponse.url}"
            }
            
            val request = MealUpdateAnalyzeRequest(
                imageUrl = fullImageUrl,
                baselineFoods = baselineFoods
            )
            val response = api.analyzeMealUpdate(request)
            DebugLogger.i(TAG, "用餐更新分析完成: status=${response.status}")
            
            onAnalyzeComplete()
            
            response
        }
    }

    
    /**
     * 用户注册/登录
     * 
     * @param deviceId 设备ID
     * @param deviceModel 设备型号
     * @param appVersion 应用版本
     */
    suspend fun registerUser(
        deviceId: String,
        deviceModel: String? = null,
        appVersion: String? = null
    ): Result<UserRegisterResponse> {
        return withRetry {
            val request = UserRegisterRequest(
                deviceId = deviceId,
                deviceType = "phone",
                deviceModel = deviceModel ?: android.os.Build.MODEL,
                appVersion = appVersion ?: Config.APP_VERSION
            )
            DebugLogger.i(TAG, "注册用户: deviceId=$deviceId")
            val response = api.registerUser(request)
            DebugLogger.i(TAG, "注册成功: userId=${response.userId}, isNewUser=${response.isNewUser}")
            response
        }
    }
    
    /**
     * 获取用户档案（通过 device_id）
     */
    suspend fun getUserProfile(deviceId: String): Result<UserProfileResponse> {
        return withRetry {
            DebugLogger.i(TAG, "获取用户档案: deviceId=$deviceId")
            api.getUserProfile(deviceId)
        }
    }
    
    /**
     * 更新用户档案（通过 user_id）
     */
    suspend fun updateUserProfile(
        userId: String,
        age: Int? = null,
        gender: String? = null,
        height: Float? = null,
        weight: Float? = null,
        activityLevel: String? = null,
        healthGoal: String? = null,
        targetWeight: Float? = null,
        healthConditions: List<String>? = null,
        dietaryPreferences: List<String>? = null
    ): Result<UserProfileUpdateResponse> {
        return withRetry {
            val request = UserProfileUpdateRequest(
                age = age,
                gender = gender,
                height = height,
                weight = weight,
                activityLevel = activityLevel,
                healthGoal = healthGoal,
                targetWeight = targetWeight,
                healthConditions = healthConditions,
                dietaryPreferences = dietaryPreferences
            )
            DebugLogger.i(TAG, "更新用户档案: userId=$userId")
            api.updateUserProfile(userId, request)
        }
    }
    
    /**
     * 获取用户统计数据
     */
    suspend fun getUserStats(userId: String, days: Int = 7): Result<UserStatsResponse> {
        return withRetry {
            DebugLogger.i(TAG, "获取用户统计: userId=$userId, days=$days")
            api.getUserStats(userId, days)
        }
    }
    
    /**
     * 获取用户用餐历史
     */
    suspend fun getUserMeals(
        userId: String,
        limit: Int = 20,
        offset: Int = 0
    ): Result<UserMealsResponse> {
        return withRetry {
            DebugLogger.i(TAG, "获取用餐历史: userId=$userId, limit=$limit, offset=$offset")
            api.getUserMeals(userId, limit, offset)
        }
    }
    
    /**
     * 删除用餐会话
     * 
     * @param sessionId 会话ID
     */
    suspend fun deleteMealSession(sessionId: String): Result<DeleteMealResponse> {
        return withRetry {
            DebugLogger.i(TAG, "删除用餐会话: sessionId=$sessionId")
            api.deleteMealSession(sessionId)
        }
    }
    
    /**
     * 带重试的网络请求
     * 
     * 重试策略：3次重试，指数退避（1s, 2s, 4s）
     */
    private suspend fun <T> withRetry(block: suspend () -> T): Result<T> {
        var currentDelay = Config.INITIAL_RETRY_DELAY_MS
        
        repeat(Config.MAX_RETRY_COUNT) { attempt ->
            try {
                val result = block()
                return Result.success(result)
            } catch (e: Exception) {
                Log.w(TAG, "请求失败 (尝试 ${attempt + 1}/${Config.MAX_RETRY_COUNT}): ${e.message}")
                
                if (attempt == Config.MAX_RETRY_COUNT - 1) {
                    Log.e(TAG, "请求最终失败", e)
                    return Result.failure(e)
                }
                
                delay(currentDelay)
                currentDelay = (currentDelay * 2).coerceAtMost(Config.MAX_RETRY_DELAY_MS)
            }
        }
        
        return Result.failure(IllegalStateException("重试次数已用尽"))
    }
}

/**
 * 网络错误类型
 */
sealed class NetworkError : Exception() {
    object NoConnection : NetworkError()
    object Timeout : NetworkError()
    data class ServerError(val code: Int, override val message: String) : NetworkError()
    data class ParseError(override val cause: Throwable) : NetworkError()
}
