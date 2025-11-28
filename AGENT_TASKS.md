# Agent 详细任务清单

> 三个 Agent 并行开发，每个 Agent 独立负责一端

---

## 🎯 产品目标摘要

### 核心流程
```
开始进餐 → 自动监测(5分钟/次) → 结束进餐 → 整体评价
```

### 三阶段输出
| 阶段 | 输出 |
|------|------|
| 开始 | 当餐热量/营养 + 1-2句建议（结合用户档案） |
| 监测 | 无感更新进食量 |
| 结束 | 整体评价 + 营养饼图 + 今日总摄入占比 |

### 个性化建议示例
```
用户档案: 45岁 + 轻度脂肪肝
→ 开始建议: "建议低油且蓬菜为主"
→ 结束评价: "本餐红烧肉热量较高，建议下餐减少油腓食物"
```

---

## Agent 1：手机端应用（新建）

### 📋 开发进度

> **更新时间**: 2025-11-25

**已完成**:
- ✅ 需求文档 (`.kiro/specs/phone-app/requirements.md`)
- ✅ 设计文档 (`.kiro/specs/phone-app/design.md`)
- ✅ 任务列表 (`.kiro/specs/phone-app/tasks.md`)

**设计要点**:
- 使用 CXR-M SDK (`com.rokid.cxr:client-m:1.0.3`) 实现蓝牙通信
- 消息协议与眼镜端 `android/BLUETOOTH_PROTOCOL.md` 完全一致
- 数据库结构与后端 `backend/app/models.py` 对应
- API 响应格式与后端 `backend/app/main.py` 一致

**下一步**: 开始执行任务 1.1 - 创建 Android 项目结构

---

### 手机端核心功能

| 功能模块 | 说明 | 优先级 |
|----------|------|--------|
| **蓝牙通信** | 与眼镜收发图片/结果 | P0 |
| **服务器通信** | 调用后端 API | P0 |
| **用户档案** | BMI计算、健康状况、饮食习惯(初次注册时收集，保存到数据库) | P1 |
| **统计页面** | 饮食爱好排行(当月)、时间规律(用餐时长/时间)、热量摄入(当天/当周) | P1 |
| **设备管理** | Rokid眼镜连接/配对/设置 | P1 |
| **未来扩展** | 手机/手表运动数据 → 基础消耗热量 | P2 |

### 📁 项目路径: `android-phone/`

### Day 1: 项目初始化

```bash
# 创建目录结构
android-phone/
├── app/
│   ├── src/main/
│   │   ├── kotlin/com/rokid/nutrition/phone/
│   │   │   ├── MainActivity.kt
│   │   │   ├── bluetooth/
│   │   │   │   ├── BluetoothManager.kt
│   │   │   │   └── GlassesConnectionService.kt
│   │   │   ├── network/
│   │   │   │   ├── NetworkManager.kt
│   │   │   │   └── ApiService.kt
│   │   │   ├── database/
│   │   │   │   ├── AppDatabase.kt
│   │   │   │   ├── MealSessionDao.kt
│   │   │   │   └── entities/
│   │   │   ├── ui/
│   │   │   │   ├── screens/
│   │   │   │   └── components/
│   │   │   └── model/
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
└── settings.gradle.kts
```

#### Task 1.1: 创建 settings.gradle.kts
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.rokid.com/repository/maven-public/") }
    }
}
rootProject.name = "RokidNutritionPhone"
include(":app")
```

#### Task 1.2: 创建 app/build.gradle.kts
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.rokid.nutrition.phone"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.rokid.nutrition.phone"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    // CXR-M SDK
    implementation("com.rokid.ai.cxr:cxr-api:1.0.3")
    
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
```

#### Task 1.3: 创建 AndroidManifest.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <uses-permission android:name="android.permission.BLUETOOTH"/>
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT"/>
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN"/>
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE"/>
    
    <application
        android:name=".NutritionPhoneApp"
        android:label="营养助手"
        android:icon="@mipmap/ic_launcher"
        android:theme="@style/Theme.NutritionPhone"
        android:usesCleartextTraffic="true">
        
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>
        
        <service
            android:name=".bluetooth.GlassesConnectionService"
            android:foregroundServiceType="connectedDevice"
            android:exported="false"/>
            
    </application>
</manifest>
```

### Day 1-2: 蓝牙服务

#### Task 2.1: BluetoothManager.kt
```kotlin
package com.rokid.nutrition.phone.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import com.rokid.cxrm.api.CxrApi
import com.rokid.cxrm.api.callback.*
import com.rokid.cxrm.api.model.Caps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BluetoothManager(private val context: Context) {
    
    companion object {
        private const val TAG = "BluetoothManager"
        private const val SERVICE_UUID = "00009100-0000-1000-8000-00805f9b34fb"
        private const val CLIENT_SECRET = "your-secret" // TODO: 从 Rokid 平台获取
    }
    
    private val cxrApi = CxrApi.getInstance()
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState
    
    private val _receivedImage = MutableStateFlow<ByteArray?>(null)
    val receivedImage: StateFlow<ByteArray?> = _receivedImage
    
    private val _receivedCommand = MutableStateFlow<String?>(null)
    val receivedCommand: StateFlow<String?> = _receivedCommand
    
    private val bluetoothCallback = object : BluetoothStatusCallback {
        override fun onConnectionInfo(uuid: String?, mac: String?, p2: String?, p3: Int) {
            Log.d(TAG, "连接信息: uuid=$uuid, mac=$mac")
            if (uuid != null && mac != null) {
                connectWithLicense(uuid, mac)
            }
        }
        
        override fun onConnected() {
            Log.d(TAG, "蓝牙已连接")
            _connectionState.value = ConnectionState.Connected
            setupListeners()
        }
        
        override fun onDisconnected() {
            Log.d(TAG, "蓝牙已断开")
            _connectionState.value = ConnectionState.Disconnected
        }
        
        override fun onFailed(errorCode: ValueUtil.CxrBluetoothErrorCode?) {
            Log.e(TAG, "连接失败: $errorCode")
            _connectionState.value = ConnectionState.Error(errorCode?.name ?: "Unknown")
        }
    }
    
    fun connect(device: BluetoothDevice) {
        _connectionState.value = ConnectionState.Connecting
        cxrApi.initBluetooth(context, device, bluetoothCallback)
    }
    
    private fun connectWithLicense(uuid: String, mac: String) {
        val license = context.assets.open("rokid_license.lc").readBytes()
        cxrApi.connectBluetooth(context, uuid, mac, bluetoothCallback, license, CLIENT_SECRET)
    }
    
    private fun setupListeners() {
        // 接收眼镜消息
        cxrApi.setCustomCmdListener { cmdName, caps ->
            Log.d(TAG, "收到消息: $cmdName")
            handleMessage(cmdName, caps)
        }
    }
    
    private fun handleMessage(cmdName: String?, caps: Caps?) {
        caps ?: return
        when (cmdName) {
            "nutrition_image" -> {
                // TODO: 处理图片数据
                Log.d(TAG, "收到图片")
            }
            "nutrition_command" -> {
                val command = caps.at(1).string
                Log.d(TAG, "收到指令: $command")
                _receivedCommand.value = command
            }
        }
    }
    
    fun sendResult(result: NutritionResult) {
        val caps = Caps().apply {
            write("nutrition_result")
            write(result.foodName)
            write(result.calories.toFloat())
            write(result.protein.toFloat())
            write(result.carbs.toFloat())
            write(result.fat.toFloat())
            write(result.suggestion)
        }
        cxrApi.sendCustomCmd("nutrition_result", caps)
    }
    
    fun showARView(result: NutritionResult) {
        // TODO: 构建 CustomView JSON
    }
    
    fun disconnect() {
        cxrApi.deinitBluetooth()
    }
    
    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        object Connected : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }
}

data class NutritionResult(
    val foodName: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val suggestion: String
)
```

### Day 2-3: 网络层

#### Task 3.1: ApiService.kt
```kotlin
package com.rokid.nutrition.phone.network

import okhttp3.MultipartBody
import retrofit2.http.*

/**
 * FastAPI 后端 API 接口
 * 生产地址: http://viseat.cn
 */
interface ApiService {
    
    @GET("/health")
    suspend fun healthCheck(): HealthResponse
    
    // 图片上传 - 返回相对路径，需拼接基址
    @Multipart
    @POST("/api/v1/upload")
    suspend fun uploadImage(@Part file: MultipartBody.Part): UploadResponse
    
    // 核心: Qwen-VL 多食材拆解 + 营养计算
    // 非食物图片返回 HTTP 400
    @POST("/api/v1/vision/analyze")
    suspend fun analyzeVision(@Body request: VisionAnalyzeRequest): VisionAnalyzeResponse
    
    // 营养聚合计算
    @POST("/api/v1/nutrition/aggregate")
    suspend fun aggregateNutrition(@Body request: SnapshotPayload): NutritionAggregateResponse
    
    // 查询单个食物营养 (每100g)
    @GET("/api/v1/nutrition/lookup")
    suspend fun lookupNutrition(@Query("name") name: String): NutritionLookupResponse
    
    // 条码查询 (Open Food Facts)
    @GET("/api/v1/nutrition/barcode")
    suspend fun lookupBarcode(@Query("barcode") barcode: String): NutritionLookupResponse
    
    // 用餐会话管理
    @POST("/api/v1/meal/start")
    suspend fun startMealSession(
        @Query("user_id") userId: String = "default_user",
        @Query("meal_type") mealType: String = "lunch",
        @Query("auto_interval") autoInterval: Int = 300,
        @Body snapshot: SnapshotPayload
    ): MealStartResponse
    
    @POST("/api/v1/meal/update")
    suspend fun updateMealSession(
        @Query("session_id") sessionId: String,
        @Body snapshot: SnapshotPayload  // “当前剩余”快照
    ): MealUpdateResponse
    
    @POST("/api/v1/meal/end")
    suspend fun endMealSession(
        @Query("session_id") sessionId: String,
        @Body finalSnapshot: SnapshotPayload?
    ): MealEndResponse
    
    @GET("/api/v1/meal/session/{session_id}")
    suspend fun getMealSession(@Path("session_id") sessionId: String): MealSession
    
    @GET("/api/v1/meal/sessions")
    suspend fun getMealSessions(
        @Query("user_id") userId: String,
        @Query("status") status: String? = null
    ): List<MealSession>
    
    // AI 营养建议对话
    @POST("/api/v1/chat/nutrition")
    suspend fun chatNutrition(@Body request: ChatNutritionRequest): ChatNutritionResponse
}

// === 数据类 === //

data class HealthResponse(val status: String, val version: String?)

data class UploadResponse(
    val url: String,       // 相对路径，如 /uploads/xxx.jpg
    val filename: String
)

data class VisionAnalyzeRequest(val image_url: String)  // 必须是公网可访问 URL

/**
 * 后端 /api/v1/vision/analyze 返回结构
 * 
 * 示例响应:
 * {
 *   "raw_llm": {
 *     "is_food": true,
 *     "foods": [
 *       {"dish_name": "红烧肉", "cooking_method": "braise", "ingredients": [{"name_en": "pork", "weight_g": 150}]},
 *       {"dish_name": "米饭", "cooking_method": "steam", "ingredients": [{"name_en": "rice", "weight_g": 200}]}
 *     ]
 *   },
 *   "snapshot": {
 *     "foods": [...],
 *     "nutrition": {"calories": 650, "protein": 25, "carbs": 80, "fat": 28}
 *   }
 * }
 * 
 * 字段说明:
 * - cooking_method: 英文烹饪方式 (raw/steam/boil/braise/stir-fry/deep-fry)
 *   - raw (生食) → 1.0x
 *   - steam/boil (清蒸/水煮) → 1.0x
 *   - braise/stew (红烧/炖) → 1.2x
 *   - stir-fry/pan-fry (炒/煎) → 1.3x
 *   - deep-fry (油炸) → 2.0x
 * - name_en: 英文食材名，用于数据库查询 (如 pork, rice, chicken breast)
 * 
 * 手机端处理流程:
 * 1. 从 raw_llm.foods[].dish_name 提取菜品名称
 * 2. 组合成简洁描述（如 "红烧肉 · 米饭" 或 "红烧肉等3道菜"）
 * 3. 从 snapshot.nutrition 提取营养数据
 * 4. 调用 /api/v1/chat/nutrition 获取 LLM 建议
 * 5. 通过蓝牙发送给眼镜
 */
data class VisionAnalyzeResponse(
    val raw_llm: RawLlm,
    val snapshot: Snapshot
)

data class Snapshot(
    val foods: List<SimpleFoodItem>,
    val nutrition: NutritionTotal
)

data class RawLlm(
    val is_food: Boolean,            // false 表示非食物图片
    val foods: List<FoodItem>?       // 菜品列表
)

data class FoodItem(
    val dish_name: String,           // 中文菜名（用于显示），如"红烧肉"
    val cooking_method: String,      // 英文烹饪方式: raw/steam/boil/braise/stir-fry/deep-fry
    val ingredients: List<Ingredient>,
    val total_weight_g: Double,
    val confidence: Double
)

data class Ingredient(
    val name_en: String,             // 英文食材名（用于数据库查询），如 "pork", "rice", "chicken breast"
    val weight_g: Double,
    val confidence: Double
)

data class NutritionTotal(
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

data class SnapshotPayload(
    val foods: List<SimpleFoodItem>,
    val nutrition: NutritionTotal?
)

data class SimpleFoodItem(
    val name: String,
    val weight_g: Double
)

data class MealStartResponse(
    val session_id: String,
    val status: String,
    val initial_kcal: Double
)

data class MealUpdateResponse(
    val session_id: String,
    val current_remaining_kcal: Double,
    val total_served_kcal: Double,
    val consumed_kcal: Double,
    val suggestion: String?
)

data class MealEndResponse(
    val session_id: String,
    val status: String,
    val total_consumed_kcal: Double,
    val consumption_ratio: Double,
    val duration_minutes: Int,
    val report: String?
)

data class MealSession(
    val session_id: String,
    val user_id: String,
    val status: String,              // "active", "ended"
    val start_time: String,
    val end_time: String?,
    val total_consumed_kcal: Double?
)

data class NutritionAggregateResponse(
    val foods: List<FoodNutrition>,
    val total: NutritionTotal
)

data class FoodNutrition(
    val name: String,
    val weight_g: Double,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

data class NutritionLookupResponse(
    val name: String,
    val calories_per_100g: Double,
    val protein_per_100g: Double,
    val carbs_per_100g: Double,
    val fat_per_100g: Double
)

data class ChatNutritionRequest(
    val message: String,
    val session_id: String?,
    val context: Map<String, Any>?
)

data class ChatNutritionResponse(
    val reply: String,
    val suggestions: List<String>?
)
```

#### Task 3.2: NetworkManager.kt
```kotlin
package com.rokid.nutrition.phone.network

import android.content.Context
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class NetworkManager(context: Context) {
    
    companion object {
        // 生产后端地址
        private const val BASE_URL = "http://viseat.cn"
        private const val TIMEOUT_SECONDS = 30L
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val api: ApiService = retrofit.create(ApiService::class.java)
    
    /**
     * 完整流程: 上传图片 → 视觉分析
     */
    suspend fun uploadAndAnalyze(imageData: ByteArray): VisionAnalyzeResponse {
        // 1. 上传图片
        val requestBody = imageData.toRequestBody("image/jpeg".toMediaType())
        val part = MultipartBody.Part.createFormData("file", "photo.jpg", requestBody)
        val uploadResult = api.uploadImage(part)
        
        // 2. 拼接完整 URL (Qwen-VL 需要公网可访问的 URL)
        val imageUrl = "$BASE_URL${uploadResult.url}"
        
        // 3. 视觉分析
        return api.analyzeVision(VisionAnalyzeRequest(imageUrl))
    }
    
    /**
     * 从 VLM 结果中提取菜品描述（发送给眼镜显示）
     * 
     * 示例:
     * - 单道菜: "红烧肉"
     * - 两道菜: "红烧肉 · 米饭"
     * - 多道菜: "红烧肉等3道菜"
     */
    fun extractDishDescription(visionResult: VisionAnalyzeResponse): String {
        val dishNames = visionResult.raw_llm.foods?.map { it.dish_name } ?: emptyList()
        return when {
            dishNames.isEmpty() -> "未识别到菜品"
            dishNames.size == 1 -> dishNames[0]
            dishNames.size == 2 -> "${dishNames[0]} · ${dishNames[1]}"
            else -> "${dishNames[0]}等${dishNames.size}道菜"
        }
    }
    
    /**
     * 开始用餐会话
     */
    suspend fun startMeal(visionResult: VisionAnalyzeResponse): MealStartResponse {
        val snapshot = SnapshotPayload(
            foods = visionResult.snapshot.foods,
            nutrition = visionResult.snapshot.nutrition
        )
        return api.startMealSession(snapshot = snapshot)
    }
}
```

---

## Agent 2：眼镜端改造

### 📁 项目路径: `android/`

### Day 1: 启用 CXR-S SDK

#### Task 1.1: 修改 RokidManager.kt - 取消注释，启用 SDK
```kotlin
// 将所有注释的 cxrBridge 调用取消注释
private val cxrBridge = CXRServiceBridge()

private val statusListener = object : CXRServiceBridge.StatusListener {
    override fun onConnected(name: String, type: Int) {
        Log.d(TAG, "手机已连接: $name")
        connectionListener?.invoke(true, name, type)
        setupMessageSubscriptions()
    }
    
    override fun onDisconnected() {
        Log.d(TAG, "手机已断开")
        connectionListener?.invoke(false, null, null)
    }
    
    override fun onARTCStatus(health: Float, reset: Boolean) {
        artcStatusListener?.invoke(health, reset)
    }
}
```

### Day 1-2: 新增蓝牙发送功能

#### Task 2.1: 新建 BluetoothSender.kt
```kotlin
package com.rokid.nutrition

import android.graphics.Bitmap
import android.util.Log
import com.rokid.cxr.service.bridge.CXRServiceBridge
import com.rokid.cxr.service.bridge.Caps
import java.io.ByteArrayOutputStream

class BluetoothSender(private val cxrBridge: CXRServiceBridge) {
    
    companion object {
        private const val TAG = "BluetoothSender"
        const val MSG_IMAGE = "nutrition_image"
        const val MSG_COMMAND = "nutrition_command"
    }
    
    /**
     * 发送图片到手机
     */
    fun sendImage(bitmap: Bitmap, quality: Int = 85): Boolean {
        return try {
            val data = bitmap.toJpegByteArray(quality)
            val caps = Caps().apply {
                write(MSG_IMAGE)
                write("jpeg")
                writeInt32(data.size)
                write(System.currentTimeMillis())
            }
            val result = cxrBridge.sendMessage(MSG_IMAGE, caps, data, 0, data.size)
            Log.d(TAG, "发送图片: ${data.size} bytes, result=$result")
            result == 0
        } catch (e: Exception) {
            Log.e(TAG, "发送图片失败", e)
            false
        }
    }
    
    /**
     * 发送指令到手机
     */
    fun sendCommand(command: String): Boolean {
        return try {
            val caps = Caps().apply {
                write(MSG_COMMAND)
                write(command)
                write(System.currentTimeMillis())
            }
            val result = cxrBridge.sendMessage(MSG_COMMAND, caps)
            Log.d(TAG, "发送指令: $command, result=$result")
            result == 0
        } catch (e: Exception) {
            Log.e(TAG, "发送指令失败", e)
            false
        }
    }
    
    private fun Bitmap.toJpegByteArray(quality: Int): ByteArray {
        val stream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }
}
```

### Day 2: 改造 MainActivity 拍照流程

#### Task 3.1: 修改拍照回调
```kotlin
// MainActivity.kt 中修改 onPhotoTaken

private lateinit var bluetoothSender: BluetoothSender

override fun onCreate(...) {
    // 初始化蓝牙发送器
    bluetoothSender = BluetoothSender(rokidManager.getCxrBridge())
}

private fun onPhotoTaken(bitmap: Bitmap) {
    // 移除: networkManager.uploadAndAnalyze(...)
    
    // 新增: 通过蓝牙发送
    uiState.analysisStatus.value = "正在发送到手机..."
    
    lifecycleScope.launch(Dispatchers.IO) {
        val success = bluetoothSender.sendImage(bitmap)
        withContext(Dispatchers.Main) {
            if (success) {
                uiState.analysisStatus.value = "等待识别结果..."
            } else {
                uiState.analysisStatus.value = "发送失败，请检查连接"
            }
        }
    }
}
```

### Day 2-3: 接收结果并显示

#### Task 4.1: 在 RokidManager 中添加结果订阅
```kotlin
// RokidManager.kt

var nutritionResultListener: ((NutritionResult) -> Unit)? = null

private fun setupMessageSubscriptions() {
    cxrBridge.subscribe("nutrition_result", object : CXRServiceBridge.MsgCallback {
        override fun onReceive(name: String, args: Caps, data: ByteArray?) {
            val result = NutritionResult(
                foodName = args.at(1).string,
                calories = args.at(2).float.toDouble(),
                protein = args.at(3).float.toDouble(),
                carbs = args.at(4).float.toDouble(),
                fat = args.at(5).float.toDouble(),
                suggestion = args.at(6).string
            )
            nutritionResultListener?.invoke(result)
        }
    })
}

data class NutritionResult(
    val foodName: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val suggestion: String
)
```

---

## Agent 3：后端数据流验证 + 个性化建议

### ⭐ 重点任务：验证数据流准确性

```
VLM识别餐食种类 → 食品数据库查询原材料 → 估计热量运算 → LLM生成建议
```

### 后端现状说明

**已完成功能**:
- FastAPI 服务已部署到 `http://viseat.cn`
- Qwen-VL 多食材拆解 + 营养计算
- 用餐会话管理 (start/update/end)
- 动态基线策略（自动识别加菜）
- 烹饪方式加权系数

**营养数据库**:
- USDA FoodData Central (MVP)
- foodstruct_nutritional_facts.csv
- Open Food Facts (条码查询)

### Day 1: 数据流验证

#### Task 1.1: 验证 VLM 识别准确性
```bash
# 测试多种菜品
curl -X POST "http://viseat.cn/api/v1/vision/analyze" \
  -H "Content-Type: application/json" \
  -d '{"image_url": "http://viseat.cn/uploads/test_food.jpg"}'

# 检查返回的 dish_name 和 ingredients 是否准确
```

#### Task 1.2: 验证数据库查询
```python
# 检查 ingredients.name_en 能否正确查询到 foodstruct 数据库
# 例如: "Pork belly" → calories_per_100g = 518
```

#### Task 1.3: 验证热量计算逻辑
```
输入: Pork belly 150g, cooking_method=Red-braise
计算: 518 * 1.5 * 0.5 = 777 kcal (Red-braise 系数 1.5)
```

### Day 2: 个性化建议 API

#### Task 2.1: 支持用户档案作为 RAG 输入
```python
# POST /api/v1/chat/nutrition 请求体增加 user_profile
{
    "message": "分析本餐",
    "session_id": "xxx",
    "user_profile": {
        "age": 45,
        "bmi": 26.5,
        "health_conditions": ["轻度脂肪肝"],
        "dietary_preferences": ["低油"]
    }
}

# 响应应包含个性化建议
{
    "reply": "您有轻度脂肪肝，本餐红烧肉热量较高，建议以蓬菜为主"
}
```

### Day 2-3: 结束报告优化

#### Task 3.1: 生成营养饼图数据
```python
# meal/end 响应增加饼图数据
{
    "nutrition_breakdown": {
        "protein_percent": 15,
        "carbs_percent": 55,
        "fat_percent": 30
    }
}
```

#### Task 3.2: 今日总摄入统计
```python
# 新增 API: GET /api/v1/stats/daily?user_id=xxx&date=2025-11-25
{
    "total_calories": 1850,
    "target_calories": 2000,
    "meals": [
        {"meal_type": "breakfast", "calories": 450},
        {"meal_type": "lunch", "calories": 780},
        {"meal_type": "dinner", "calories": 620}
    ]
}
```

### Day 3: 测试数据 & 联调支持

#### Task 2.1: 创建测试响应数据 test_data/mock_responses.json
```json
{
  "vision_analyze_success": {
    "foods": [
      {
        "dish_name": "红烧肉",
        "cooking_method": "Red-braise",
        "ingredients": [
          {"name_en": "Pork belly", "weight_g": 150, "confidence": 0.9},
          {"name_en": "Sugar", "weight_g": 10, "confidence": 0.7}
        ],
        "total_weight_g": 160,
        "confidence": 0.85
      },
      {
        "dish_name": "米饭",
        "cooking_method": "Steam",
        "ingredients": [
          {"name_en": "White rice", "weight_g": 200, "confidence": 0.95}
        ],
        "total_weight_g": 200,
        "confidence": 0.95
      }
    ],
    "total": {
      "calories": 780,
      "protein": 28,
      "carbs": 85,
      "fat": 35
    },
    "raw_llm": {
      "is_food": true,
      "suggestion": "红烧肉热量较高，建议搭配蓬菜平衡营养"
    }
  },
  "vision_analyze_no_food": {
    "detail": "未检测到食物，请拍摄清晰的食物照片"
  },
  "meal_start_success": {
    "session_id": "sess_abc123",
    "status": "active",
    "initial_kcal": 780
  },
  "meal_update_success": {
    "session_id": "sess_abc123",
    "current_remaining_kcal": 400,
    "total_served_kcal": 780,
    "consumed_kcal": 380,
    "suggestion": "已摄入 380 千卡，进度良好"
  },
  "meal_end_success": {
    "session_id": "sess_abc123",
    "status": "ended",
    "total_consumed_kcal": 620,
    "consumption_ratio": 0.79,
    "duration_minutes": 25,
    "report": "本餐共摄入 620 千卡，用餐时长 25 分钟，进食比例 79%"
  }
}
```

### Day 2-3: 联调支持

#### Task 3.1: 创建调试脚本 scripts/test_api.sh
```bash
#!/bin/bash
BASE_URL=${1:-"http://viseat.cn"}

echo "=== 1. 健康检查 ==="
curl -s "$BASE_URL/health" | jq

echo -e "\n=== 2. 上传图片测试 ==="
UPLOAD_RESULT=$(curl -s -X POST "$BASE_URL/api/v1/upload" \
  -F "file=@test_food.jpg")
echo "$UPLOAD_RESULT" | jq
IMAGE_URL="$BASE_URL$(echo $UPLOAD_RESULT | jq -r '.url')"
echo "图片 URL: $IMAGE_URL"

echo -e "\n=== 3. 视觉分析测试 ==="
VISION_RESULT=$(curl -s -X POST "$BASE_URL/api/v1/vision/analyze" \
  -H "Content-Type: application/json" \
  -d "{\"image_url\": \"$IMAGE_URL\"}")
echo "$VISION_RESULT" | jq

echo -e "\n=== 4. 开始用餐会话 ==="
SESSION_RESULT=$(curl -s -X POST "$BASE_URL/api/v1/meal/start?user_id=test&meal_type=lunch" \
  -H "Content-Type: application/json" \
  -d '{"foods": [{"name": "rice", "weight_g": 200}], "nutrition": {"calories": 260}}')
echo "$SESSION_RESULT" | jq
SESSION_ID=$(echo $SESSION_RESULT | jq -r '.session_id')

echo -e "\n=== 5. 更新会话 ==="
curl -s -X POST "$BASE_URL/api/v1/meal/update?session_id=$SESSION_ID" \
  -H "Content-Type: application/json" \
  -d '{"foods": [{"name": "rice", "weight_g": 100}], "nutrition": {"calories": 130}}' | jq

echo -e "\n=== 6. 结束会话 ==="
curl -s -X POST "$BASE_URL/api/v1/meal/end?session_id=$SESSION_ID" \
  -H "Content-Type: application/json" \
  -d '{}' | jq

echo -e "\n=== 7. AI 营养建议 ==="
curl -s -X POST "$BASE_URL/api/v1/chat/nutrition" \
  -H "Content-Type: application/json" \
  -d '{"message": "我今天吃了红烧肉和米饭，健康吗？"}' | jq
```

#### Task 3.2: 后端 API 响应格式确认

确保后端返回格式与手机端数据类匹配：

```python
# /api/v1/vision/analyze 响应结构
{
    "foods": [
        {
            "dish_name": str,          # 菜名
            "cooking_method": str,     # Raw/Steam/Boil/Stir-fry/Deep-fry/Red-braise
            "ingredients": [
                {
                    "name_en": str,    # 英文食材名
                    "weight_g": float,
                    "confidence": float
                }
            ],
            "total_weight_g": float,
            "confidence": float
        }
    ],
    "total": {
        "calories": float,
        "protein": float,
        "carbs": float,
        "fat": float
    },
    "raw_llm": {
        "is_food": bool,
        "suggestion": str | null
    }
}
```

---

## 🔗 同步约定

### 消息名称常量（三端必须一致）
```
nutrition_image    - 眼镜→手机: 图片数据
nutrition_command  - 眼镜→手机: 用户指令
nutrition_result   - 手机→眼镜: 识别结果
session_status     - 手机→眼镜: 会话状态
```

### 每日同步检查
- 09:00 - 三个 Agent 同步当日任务
- 18:00 - 汇报进度，协调问题

### 联调顺序
1. 先确保蓝牙连接正常（Agent 1 + 2）
2. 再测试消息收发（Agent 1 + 2）
3. 最后接入后端（Agent 1 + 3）

---

## 🚨 待办任务：用餐总结数据补充

> **创建时间**: 2025-11-25
> **优先级**: 高
> **涉及**: 后端 Agent + 手机端 Agent

### 背景

眼镜端用餐结束时需要显示**营养饼状图**（蛋白质/碳水/脂肪占比），但当前后端 `end_meal_session` 接口只返回热量数据，缺少营养成分总量。

### 需要修改的位置

#### 1. 后端修改 (`backend/app/main.py`)

**文件**: `backend/app/main.py`
**函数**: `calculate_dynamic_stats()` (第 813 行)
**位置**: 第 882-888 行

**当前返回**:
```python
return {
    "total_served_kcal": round(total_served_kcal, 1),
    "current_remaining_kcal": round(current_remaining_kcal, 1),
    "total_consumed_kcal": round(total_consumed_kcal, 1),
    "consumption_ratio": round(consumption_ratio, 3),
    "duration_minutes": round(duration_minutes, 1)
}
```

**需要添加**:
```python
return {
    "total_served_kcal": round(total_served_kcal, 1),
    "current_remaining_kcal": round(current_remaining_kcal, 1),
    "total_consumed_kcal": round(total_consumed_kcal, 1),
    "consumption_ratio": round(consumption_ratio, 3),
    "duration_minutes": round(duration_minutes, 1),
    # === 新增：营养成分总量 ===
    "total_protein_g": round(total_protein_g, 1),
    "total_carbs_g": round(total_carbs_g, 1),
    "total_fat_g": round(total_fat_g, 1)
}
```

**实现思路**:
- 在遍历 `snapshots` 时，同时累加每种食物的蛋白质、碳水、脂肪
- 需要从 `MealSnapshot.raw_json["nutrition"]` 或 `SnapshotFood` 中提取营养数据
- 如果 `SnapshotFood` 表没有营养字段，需要先扩展表结构

---

**文件**: `backend/app/main.py`
**函数**: `end_meal_session()` (第 1006 行)
**位置**: 第 1068-1076 行

**当前返回**:
```python
final_stats = {
    "total_served": stats["total_served_kcal"],
    "total_consumed": stats["total_consumed_kcal"],
    "consumption_ratio": stats["consumption_ratio"],
    "duration_minutes": stats["duration_minutes"],
    ...
}
```

**需要添加**:
```python
final_stats = {
    "total_served": stats["total_served_kcal"],
    "total_consumed": stats["total_consumed_kcal"],
    "consumption_ratio": stats["consumption_ratio"],
    "duration_minutes": stats["duration_minutes"],
    # === 新增 ===
    "total_protein": stats["total_protein_g"],
    "total_carbs": stats["total_carbs_g"],
    "total_fat": stats["total_fat_g"],
    ...
}
```

---

#### 2. 手机端修改 (`android-phone/`)

**文件**: `MealEndResponse` 数据类 (参考 AGENT_TASKS.md 第 522 行)

**当前定义**:
```kotlin
data class MealEndResponse(
    val session_id: String,
    val status: String,
    val total_consumed_kcal: Double,
    val consumption_ratio: Double,
    val duration_minutes: Int,
    val report: String?
)
```

**需要修改为**:
```kotlin
data class MealEndResponse(
    val session_id: String,
    val status: String,
    val total_consumed_kcal: Double,
    val consumption_ratio: Double,
    val duration_minutes: Int,
    val report: String?,
    // === 新增：营养成分总量 ===
    val total_protein: Double,
    val total_carbs: Double,
    val total_fat: Double
)
```

---

**文件**: `SessionStatus` 蓝牙消息 (参考 `android/app/.../BluetoothReceiver.kt` 第 220 行)

**当前格式** (手机→眼镜):
```
[0] String: 会话ID
[1] String: 状态 ("active" | "ended")
[2] Float:  总摄入热量 (kcal)
[3] String: 消息文本
```

**需要修改为**:
```
[0] String: 会话ID
[1] String: 状态 ("active" | "ended")
[2] Float:  总摄入热量 (kcal)
[3] String: 消息文本
[4] Float:  总蛋白质 (g)     // 新增
[5] Float:  总碳水化合物 (g)  // 新增
[6] Float:  总脂肪 (g)       // 新增
[7] Int:    用餐时长 (分钟)   // 新增
```

---

#### 3. 眼镜端已准备好接收 (`android/`)

眼镜端 `SessionStatus` 数据类需要同步更新：

**文件**: `android/app/src/main/kotlin/com/rokid/nutrition/bluetooth/BluetoothReceiver.kt`

**当前定义** (第 220 行):
```kotlin
data class SessionStatus(
    val sessionId: String,
    val status: String,
    val totalConsumed: Double,
    val message: String
)
```

**需要修改为**:
```kotlin
data class SessionStatus(
    val sessionId: String,
    val status: String,
    val totalConsumed: Double,
    val message: String,
    // === 新增 ===
    val totalProtein: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val totalFat: Double = 0.0,
    val durationMinutes: Int = 0
)
```

---

### 验收标准

1. ✅ 后端 `POST /api/v1/meal/end` 返回 `total_protein`, `total_carbs`, `total_fat`
2. ✅ 手机端正确解析并通过蓝牙发送给眼镜
3. ✅ 眼镜端用餐结束时显示营养饼状图（三色环形图）

### 临时方案（眼镜端已实现）

在后端/手机端完成修改前，眼镜端使用**本地累加**方式计算营养总量：
- 每次收到 `NutritionResult` 时累加 `protein`, `carbs`, `fat`
- 用餐结束时使用本地累加值显示饼状图

此方案的局限性：
- 如果中途重启应用，累加数据会丢失
- 无法获取后端计算的精确消耗量（只有上菜量）
