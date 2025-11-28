package com.rokid.nutrition.phone.network

import com.rokid.nutrition.phone.network.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

/**
 * 后端 API 接口定义
 */
interface ApiService {
    
    /**
     * 用户注册/登录
     * POST /api/v1/user/register
     */
    @POST("/api/v1/user/register")
    suspend fun registerUser(@Body request: UserRegisterRequest): UserRegisterResponse
    
    /**
     * 获取用户档案
     * GET /api/v1/user/profile?device_id=xxx
     */
    @GET("/api/v1/user/profile")
    suspend fun getUserProfile(@Query("device_id") deviceId: String): UserProfileResponse
    
    /**
     * 更新用户档案
     * PUT /api/v1/user/profile?user_id=xxx
     */
    @PUT("/api/v1/user/profile")
    suspend fun updateUserProfile(
        @Query("user_id") userId: String,
        @Body request: UserProfileUpdateRequest
    ): UserProfileUpdateResponse
    
    /**
     * 获取用户统计数据
     * GET /api/v1/user/{user_id}/stats
     */
    @GET("/api/v1/user/{user_id}/stats")
    suspend fun getUserStats(
        @Path("user_id") userId: String,
        @Query("days") days: Int = 7
    ): UserStatsResponse
    
    /**
     * 获取用户用餐历史
     * GET /api/v1/user/{user_id}/meals
     */
    @GET("/api/v1/user/{user_id}/meals")
    suspend fun getUserMeals(
        @Path("user_id") userId: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): UserMealsResponse
    
    /**
     * 上传图片
     * POST /api/v1/upload
     */
    @Multipart
    @POST("/api/v1/upload")
    suspend fun uploadImage(@Part file: MultipartBody.Part): UploadResponse
    
    /**
     * 视觉分析
     * POST /api/v1/vision/analyze
     * 使用 RequestBody 直接发送 JSON 字符串
     */
    @POST("/api/v1/vision/analyze")
    suspend fun analyzeVision(@Body request: RequestBody): VisionAnalyzeResponse
    
    /**
     * 开始用餐会话
     * POST /api/v1/meal/start
     */
    @POST("/api/v1/meal/start")
    suspend fun startMealSession(
        @Query("user_id") userId: String,
        @Query("meal_type") mealType: String,
        @Body snapshot: SnapshotPayload
    ): MealStartResponse
    
    /**
     * 更新用餐会话
     * POST /api/v1/meal/update
     */
    @POST("/api/v1/meal/update")
    suspend fun updateMealSession(
        @Query("session_id") sessionId: String,
        @Body snapshot: SnapshotPayload
    ): MealUpdateResponse
    
    /**
     * 结束用餐会话
     * POST /api/v1/meal/end
     * 
     * 支持传递完整上下文数据：meal_context, daily_context, user_profile
     */
    @POST("/api/v1/meal/end")
    suspend fun endMealSession(
        @Body request: MealEndRequest
    ): MealEndResponse
    
    /**
     * 获取会话详情
     * GET /api/v1/meal/session/{session_id}
     */
    @GET("/api/v1/meal/session/{session_id}")
    suspend fun getMealSession(
        @Path("session_id") sessionId: String
    ): MealSessionDetailResponse
    
    /**
     * 获取会话列表
     * GET /api/v1/meal/sessions
     */
    @GET("/api/v1/meal/sessions")
    suspend fun getMealSessions(
        @Query("user_id") userId: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): MealSessionsListResponse
    
    /**
     * 营养对话
     * POST /api/v1/chat/nutrition
     */
    @POST("/api/v1/chat/nutrition")
    suspend fun chatNutrition(@Body request: ChatNutritionRequest): ChatNutritionResponse
    
    /**
     * 用餐更新分析（带容错）
     * POST /api/v1/vision/analyze_meal_update
     * 
     * 返回 status: "accept" | "skip" | "adjust"
     * - accept: 正常更新
     * - skip: 拍摄问题，静默跳过
     * - adjust: 加菜，需要更新基线
     */
    @POST("/api/v1/vision/analyze_meal_update")
    suspend fun analyzeMealUpdate(@Body request: MealUpdateAnalyzeRequest): MealUpdateAnalyzeResponse
    
    /**
     * 更新食物数据
     * PUT /api/v1/meal/food/{food_id}
     * 
     * 用于同步用户编辑的食物营养数据
     */
    @PUT("/api/v1/meal/food/{food_id}")
    suspend fun updateFood(@Body request: UpdateFoodRequest): UpdateFoodResponse
    
    /**
     * 删除用餐会话
     * DELETE /api/v1/meal/session/{session_id}
     */
    @DELETE("/api/v1/meal/session/{session_id}")
    suspend fun deleteMealSession(@Path("session_id") sessionId: String): DeleteMealResponse
}
