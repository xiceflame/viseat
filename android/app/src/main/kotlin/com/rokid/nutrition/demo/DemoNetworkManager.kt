package com.rokid.nutrition.demo

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 演示模式网络管理器
 * 
 * 直接调用后端 API 进行图片分析，用于演示模式
 */
class DemoNetworkManager {
    
    companion object {
        private const val TAG = "DemoNetworkManager"
        
        // 后端 API 地址
        // 注意：模拟器 DNS 可能无法解析域名，使用 IP 地址
        // viseat.cn -> 166.88.100.113
        private const val API_BASE_URL = "https://166.88.100.113"
        
        @Volatile
        private var instance: DemoNetworkManager? = null
        
        fun getInstance(): DemoNetworkManager {
            return instance ?: synchronized(this) {
                instance ?: DemoNetworkManager().also { instance = it }
            }
        }
    }
    
    private val client: OkHttpClient
    
    init {
        // 创建信任所有证书的 SSL 配置（仅用于演示模式）
        val trustAllCerts = arrayOf<javax.net.ssl.TrustManager>(object : javax.net.ssl.X509TrustManager {
            override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
        })
        
        val sslContext = javax.net.ssl.SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        
        client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as javax.net.ssl.X509TrustManager)
            .hostnameVerifier { _, _ -> true }  // 信任所有主机名
            .build()
    }
    
    // 保存最后一次 API 原始响应，供手机端模拟使用
    var lastRawResponse: String? = null
        private set
    
    // 保存所有分析结果的列表
    private val _allResponses = mutableListOf<AnalyzeResponseRecord>()
    val allResponses: List<AnalyzeResponseRecord> get() = _allResponses.toList()
    
    /**
     * 清空保存的响应
     */
    fun clearResponses() {
        _allResponses.clear()
        lastRawResponse = null
    }
    
    /**
     * 上传图片
     * 
     * @return 图片 URL
     */
    suspend fun uploadImage(imageData: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始上传图片，大小: ${imageData.size} bytes")
            
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    "demo_image_${System.currentTimeMillis()}.jpg",
                    imageData.toRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                .build()
            
            val request = Request.Builder()
                .url("$API_BASE_URL/api/v1/upload")
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            if (!response.isSuccessful) {
                throw Exception("Upload failed: ${response.code} - $responseBody")
            }
            
            val json = JSONObject(responseBody)
            val imageUrl = json.getString("url")
            
            // 拼接完整 URL - 使用域名（供 Qwen-VL 访问）
            val fullUrl = if (imageUrl.startsWith("http")) {
                // 如果返回的是 IP 地址，替换为域名
                imageUrl.replace("166.88.100.113", "viseat.cn")
            } else {
                // 使用域名拼接，而不是 IP
                "https://viseat.cn$imageUrl"
            }
            
            Log.d(TAG, "图片上传成功: $fullUrl")
            Result.success(fullUrl)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "图片上传失败: 无法解析主机名，请检查网络连接", e)
            Result.failure(Exception("网络错误: 无法连接服务器"))
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "图片上传失败: 连接超时", e)
            Result.failure(Exception("网络错误: 连接超时"))
        } catch (e: java.io.IOException) {
            Log.e(TAG, "图片上传失败: IO错误 - ${e.message}", e)
            Result.failure(Exception("网络错误: ${e.message}"))
        } catch (e: Exception) {
            Log.e(TAG, "图片上传失败: ${e.javaClass.simpleName} - ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 分析图片
     * 
     * @param imageUrl 图片 URL
     * @param mode 用餐监测模式: "start" | "progress" | "end"，单图识别传 null
     * @param sessionId 用餐会话 ID，用餐监测模式需要传入
     * @return 分析结果
     */
    suspend fun analyzeImage(
        imageUrl: String, 
        mode: String? = null,
        sessionId: String? = null
    ): Result<AnalyzeResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始分析图片: $imageUrl, mode=$mode, sessionId=$sessionId")
            
            val jsonBody = JSONObject().apply {
                put("image_url", imageUrl)
                mode?.let { put("mode", it) }
                sessionId?.let { put("session_id", it) }
            }
            
            val request = Request.Builder()
                .url("$API_BASE_URL/api/v1/vision/analyze")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            if (!response.isSuccessful) {
                throw Exception("Analyze failed: ${response.code} - $responseBody")
            }
            
            Log.d(TAG, "API 返回原始数据: $responseBody")
            
            // 保存原始响应供手机端模拟使用
            lastRawResponse = responseBody
            
            val json = JSONObject(responseBody)
            val result = parseAnalyzeResult(json)
            
            // 保存到记录列表
            _allResponses.add(AnalyzeResponseRecord(
                timestamp = System.currentTimeMillis(),
                imageUrl = imageUrl,
                rawResponse = responseBody,
                foodName = result.foodName,
                calories = result.calories
            ))
            
            Log.d(TAG, "解析结果: foodName=${result.foodName}, calories=${result.calories}, protein=${result.protein}, carbs=${result.carbs}, fat=${result.fat}")
            Log.d(TAG, "suggestion=${result.suggestion}")
            Log.d(TAG, "已保存 ${_allResponses.size} 条分析记录")
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "图片分析失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 上传并分析图片（一步完成）
     */
    suspend fun uploadAndAnalyze(
        imageData: ByteArray,
        onUploadComplete: (String) -> Unit,
        onAnalyzeStart: () -> Unit
    ): Result<AnalyzeResult> {
        // 1. 上传图片
        val uploadResult = uploadImage(imageData)
        if (uploadResult.isFailure) {
            return Result.failure(uploadResult.exceptionOrNull() ?: Exception("Upload failed"))
        }
        
        val imageUrl = uploadResult.getOrThrow()
        onUploadComplete(imageUrl)
        
        // 2. 分析图片
        onAnalyzeStart()
        return analyzeImage(imageUrl)
    }
    
    /**
     * 解析分析结果
     * 
     * API 返回格式：
     * {
     *   "raw_llm": {
     *     "is_food": true,
     *     "foods": [{"dish_name": "...", "dish_name_cn": "可乐", ...}],
     *     "suggestion": "..."
     *   },
     *   "snapshot": {
     *     "nutrition": {"calories": 150, "protein": 0, "carbs": 39, "fat": 0}
     *   },
     *   "suggestion": "..."
     * }
     */
    private fun parseAnalyzeResult(json: JSONObject): AnalyzeResult {
        val rawLlm = json.optJSONObject("raw_llm")
        val snapshot = json.optJSONObject("snapshot")
        val nutrition = snapshot?.optJSONObject("nutrition")
        val foods = rawLlm?.optJSONArray("foods") ?: JSONArray()
        
        // 是否是食物
        val isFood = rawLlm?.optBoolean("is_food", false) ?: false
        
        // 获取食物名称列表
        val foodNames = mutableListOf<String>()
        for (i in 0 until foods.length()) {
            val food = foods.getJSONObject(i)
            // 优先使用中文名
            val name = food.optString("dish_name_cn", "").takeIf { it.isNotBlank() }
                ?: food.optString("dish_name", "未知食物")
            foodNames.add(name)
        }
        
        // 合并食物名称
        val foodName = if (foodNames.isNotEmpty()) {
            foodNames.joinToString("、")
        } else {
            "未知食物"
        }
        
        // 从 snapshot.nutrition 获取营养数据
        val calories = nutrition?.optDouble("calories", 0.0)?.toInt() ?: 0
        val protein = nutrition?.optDouble("protein", 0.0)?.toInt() ?: 0
        val carbs = nutrition?.optDouble("carbs", 0.0)?.toInt() ?: 0
        val fat = nutrition?.optDouble("fat", 0.0)?.toInt() ?: 0
        
        // 获取建议（优先顶层，其次 raw_llm）
        val suggestion = json.optString("suggestion", "").takeIf { it.isNotBlank() }
            ?: rawLlm?.optString("suggestion", "") ?: ""
        
        Log.d(TAG, "解析: isFood=$isFood, foodName=$foodName, calories=$calories")
        
        return AnalyzeResult(
            isFood = isFood,
            foodName = foodName,
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat,
            suggestion = suggestion,
            allFoods = (0 until foods.length()).map { i ->
                val food = foods.getJSONObject(i)
                val name = food.optString("dish_name_cn", "").takeIf { it.isNotBlank() }
                    ?: food.optString("dish_name", "")
                FoodItem(
                    name = name,
                    calories = 0,  // 单个食物的热量在 API 中没有单独返回
                    protein = 0,
                    carbs = 0,
                    fat = 0
                )
            }
        )
    }
}

/**
 * 分析结果
 */
data class AnalyzeResult(
    val isFood: Boolean,
    val foodName: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val suggestion: String,
    val allFoods: List<FoodItem> = emptyList()
)

/**
 * 食物项
 */
data class FoodItem(
    val name: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int
)

/**
 * 分析响应记录 - 用于保存完整的 API 响应供手机端模拟使用
 */
data class AnalyzeResponseRecord(
    val timestamp: Long,
    val imageUrl: String,
    val rawResponse: String,  // 完整的 API 原始 JSON 响应
    val foodName: String,
    val calories: Int
) {
    /**
     * 格式化为可读的时间
     */
    fun getFormattedTime(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
}
