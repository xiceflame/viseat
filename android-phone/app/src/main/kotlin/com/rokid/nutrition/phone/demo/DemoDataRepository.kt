package com.rokid.nutrition.phone.demo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.gson.Gson
import com.rokid.nutrition.phone.network.model.VisionAnalyzeResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 演示数据仓库
 * 
 * 负责加载和解析模拟数据，包括：
 * - 单图识别模式的食物数据（可乐、薯片）
 * - 用餐监测模式的三阶段数据
 */
class DemoDataRepository(private val context: Context) {
    
    companion object {
        private const val TAG = "DemoDataRepository"
        private const val DEMO_ASSETS_PATH = "demo"
    }
    
    private val gson = Gson()
    
    // 缓存已加载的数据
    private val responseCache = mutableMapOf<String, VisionAnalyzeResponse>()
    private val imageCache = mutableMapOf<String, Bitmap>()
    
    /**
     * 加载单图识别的模拟数据
     * @param foodType 食物类型: "coke" | "chips"
     * @return VisionAnalyzeResponse 或 null
     */
    suspend fun loadSingleImageData(foodType: String): VisionAnalyzeResponse? = withContext(Dispatchers.IO) {
        val fileName = "${foodType}_response.json"
        loadResponseFromAssets(fileName)
    }
    
    /**
     * 加载用餐监测阶段的模拟数据
     * @param phase 阶段: "start" | "middle" | "end"
     * @return VisionAnalyzeResponse 或 null
     */
    suspend fun loadMealPhaseData(phase: String): VisionAnalyzeResponse? = withContext(Dispatchers.IO) {
        val fileName = "meal_${phase}_response.json"
        loadResponseFromAssets(fileName)
    }
    
    /**
     * 加载图片资源
     * @param imageName 图片文件名（不含路径）
     * @return Bitmap 或 null
     */
    fun loadDemoImage(imageName: String): Bitmap? {
        // 检查缓存
        imageCache[imageName]?.let { return it }
        
        return try {
            val inputStream = context.assets.open("$DEMO_ASSETS_PATH/$imageName")
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            // 缓存
            bitmap?.let { imageCache[imageName] = it }
            Log.d(TAG, "图片加载成功: $imageName")
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "图片加载失败: $imageName", e)
            null
        }
    }
    
    /**
     * 检查模拟数据是否可用
     */
    fun isDemoDataAvailable(): DemoDataStatus {
        return DemoDataStatus(
            cokeAvailable = isAssetExists("coke_response.json") && isAssetExists("coke.jpg"),
            chipsAvailable = isAssetExists("chips_response.json") && isAssetExists("chips.jpg"),
            mealStartAvailable = isAssetExists("meal_start_response.json") && isAssetExists("meal_start.jpg"),
            mealMiddleAvailable = isAssetExists("meal_middle_response.json") && isAssetExists("meal_middle.jpg"),
            mealEndAvailable = isAssetExists("meal_end_response.json") && isAssetExists("meal_end.jpg")
        )
    }
    
    /**
     * 获取单图识别的图片名称
     */
    fun getSingleImageFileName(foodType: String): String {
        return "$foodType.jpg"
    }
    
    /**
     * 获取用餐阶段的图片名称
     */
    fun getMealPhaseImageFileName(phase: String): String {
        return "meal_$phase.jpg"
    }
    
    /**
     * 将demo图片复制到本地存储
     * 
     * @param imageName 图片文件名（如 "coke.jpg"）
     * @return 本地文件路径
     */
    suspend fun copyDemoImageToLocalStorage(imageName: String): String? = withContext(Dispatchers.IO) {
        try {
            val photoDir = java.io.File(context.filesDir, "meal_photos")
            if (!photoDir.exists()) {
                photoDir.mkdirs()
            }
            
            // 使用唯一文件名避免冲突
            val localFileName = "demo_${System.currentTimeMillis()}_$imageName"
            val localFile = java.io.File(photoDir, localFileName)
            
            // 从assets复制到本地
            context.assets.open("$DEMO_ASSETS_PATH/$imageName").use { input ->
                java.io.FileOutputStream(localFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            Log.d(TAG, "Demo图片已复制到本地: ${localFile.absolutePath}")
            localFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "复制demo图片失败: $imageName", e)
            null
        }
    }
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        responseCache.clear()
        imageCache.values.forEach { it.recycle() }
        imageCache.clear()
    }
    
    /**
     * 从assets加载JSON响应
     */
    private fun loadResponseFromAssets(fileName: String): VisionAnalyzeResponse? {
        // 检查缓存
        responseCache[fileName]?.let { return it }
        
        return try {
            val inputStream = context.assets.open("$DEMO_ASSETS_PATH/$fileName")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            inputStream.close()
            
            val response = gson.fromJson(jsonString, VisionAnalyzeResponse::class.java)
            
            // 缓存
            response?.let { responseCache[fileName] = it }
            Log.d(TAG, "JSON加载成功: $fileName")
            response
        } catch (e: Exception) {
            Log.e(TAG, "JSON加载失败: $fileName", e)
            null
        }
    }
    
    /**
     * 检查asset文件是否存在
     */
    private fun isAssetExists(fileName: String): Boolean {
        return try {
            context.assets.open("$DEMO_ASSETS_PATH/$fileName").close()
            true
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * 模拟数据可用状态
 */
data class DemoDataStatus(
    val cokeAvailable: Boolean,
    val chipsAvailable: Boolean,
    val mealStartAvailable: Boolean,
    val mealMiddleAvailable: Boolean,
    val mealEndAvailable: Boolean
) {
    val singleImageModeAvailable: Boolean
        get() = cokeAvailable || chipsAvailable
    
    val mealMonitorModeAvailable: Boolean
        get() = mealStartAvailable && mealMiddleAvailable && mealEndAvailable
}
