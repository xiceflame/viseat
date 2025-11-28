# Design Document: Rokid Nutrition Phone App

## Overview

手机端应用是 Rokid 营养助手系统的网络中枢，负责连接 AR 眼镜与后端服务。应用使用 Kotlin + Jetpack Compose 开发，采用 MVVM 架构，通过 CXR-M SDK (com.rokid.cxr:client-m:1.0.3) 实现蓝牙通信，通过 Retrofit 实现网络请求。

### 核心职责
- **蓝牙通信**: 使用 CXR-M SDK 连接眼镜，通过自定义协议 (CustomCmd/Caps) 收发数据
- **网络中枢**: 调用后端 API 进行图片上传和营养分析
- **数据管理**: 本地存储用户档案、用餐历史、统计数据
- **UI 展示**: 提供统计页面、历史记录、设备管理界面

### CXR-M SDK 核心能力
基于 SDK 示例项目 (CXRMSamples1.0.3) 分析，SDK 提供以下核心功能：

| 模块 | 功能 | 关键 API |
|------|------|----------|
| 蓝牙连接 | BLE 扫描、配对、连接 | `CxrApi.initBluetooth()`, `connectBluetooth()` |
| 自定义协议 | 双向数据传输 | `sendCustomCmd()`, `setCustomCmdListener()` |
| 拍照控制 | 远程触发眼镜拍照 | `takeGlassPhotoGlobal()` |
| 设备信息 | 获取眼镜状态 | `getGlassInfo()`, `setBatteryLevelUpdateListener()` |
| 自定义视图 | 在眼镜上显示 UI | `openCustomView()`, `updateCustomView()` |

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Phone App (Android)                       │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │   UI Layer  │  │   UI Layer  │  │   UI Layer  │              │
│  │  (Compose)  │  │  (Compose)  │  │  (Compose)  │              │
│  │  HomeScreen │  │ StatsScreen │  │DeviceScreen │              │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘              │
│         │                │                │                      │
│  ┌──────▼────────────────▼────────────────▼──────┐              │
│  │              ViewModel Layer                   │              │
│  │  HomeViewModel | StatsViewModel | DeviceVM    │              │
│  └──────────────────────┬────────────────────────┘              │
│                         │                                        │
│  ┌──────────────────────▼────────────────────────┐              │
│  │              Repository Layer                  │              │
│  │  BluetoothRepo | NetworkRepo | LocalRepo      │              │
│  └───────┬──────────────┬──────────────┬─────────┘              │
│          │              │              │                         │
│  ┌───────▼───────┐ ┌────▼────┐ ┌───────▼───────┐                │
│  │ CxrApi        │ │ ApiSvc  │ │  Room DB      │                │
│  │ (CXR-M SDK)   │ │(Retrofit)│ │ (SQLite)     │                │
│  └───────┬───────┘ └────┬────┘ └───────────────┘                │
└──────────┼──────────────┼───────────────────────────────────────┘
           │              │
    ┌──────▼──────┐ ┌─────▼─────┐
    │ Rokid Glass │ │  Backend  │
    │ (Bluetooth) │ │ (FastAPI) │
    └─────────────┘ └───────────┘
```

## Components and Interfaces

### 1. Bluetooth Module (基于 CXR-M SDK)

#### 常量定义（与眼镜端 Config.kt 保持一致）
```kotlin
object Config {
    // ==================== 蓝牙权限 ====================
    val BLUETOOTH_PERMISSIONS = mutableListOf(
        android.Manifest.permission.BLUETOOTH,
        android.Manifest.permission.BLUETOOTH_ADMIN,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(android.Manifest.permission.BLUETOOTH_CONNECT)
            add(android.Manifest.permission.BLUETOOTH_SCAN)
        }
    }.toTypedArray()

    // ==================== 蓝牙配置 ====================
    
    /** Rokid 蓝牙服务 UUID（与眼镜端一致） */
    const val SERVICE_UUID = "00009100-0000-1000-8000-00805f9b34fb"
    
    /** 从 Rokid 开发者平台获取的客户端密钥 */
    const val CLIENT_SECRET = "your-client-secret-from-rokid-platform"

    // ==================== 消息协议（与眼镜端 Config.MsgName 一致） ====================
    
    object MsgName {
        const val IMAGE = "nutrition_image"           // 眼镜→手机: 图片数据
        const val COMMAND = "nutrition_command"       // 眼镜→手机: 用户指令
        const val RESULT = "nutrition_result"         // 手机→眼镜: 识别结果
        const val SESSION_STATUS = "session_status"   // 手机→眼镜: 会话状态
    }

    /** 指令类型（与眼镜端 Config.CommandType 一致） */
    object CommandType {
        const val START_MEAL = "start_meal"
        const val END_MEAL = "end_meal"
        const val TAKE_PHOTO = "take_photo"
    }

    // ==================== 后端配置 ====================
    
    /** 后端 API 地址 */
    const val API_BASE_URL = "http://viseat.cn"
    
    /** 网络超时（秒） */
    const val NETWORK_TIMEOUT_SECONDS = 30L
}
```

#### BluetoothManager (封装 CxrApi)
```kotlin
class BluetoothManager(private val context: Context) {
    
    private val cxrApi = CxrApi.getInstance()
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _receivedImage = MutableSharedFlow<ImageData>()
    val receivedImage: SharedFlow<ImageData> = _receivedImage.asSharedFlow()
    
    private val _receivedCommand = MutableSharedFlow<String>()
    val receivedCommand: SharedFlow<String> = _receivedCommand.asSharedFlow()
    
    // 保存的设备信息（用于重连）
    private var recordedUuid: String? = null
    private var recordedMacAddress: String? = null
    
    // 蓝牙状态回调
    private val bluetoothCallback = object : BluetoothStatusCallback {
        override fun onConnectionInfo(uuid: String?, macAddress: String?, p2: String?, p3: Int) {
            // 收到连接信息，保存并尝试连接
            uuid?.let { recordedUuid = it }
            macAddress?.let { recordedMacAddress = it }
            connectWithLicense()
        }
        
        override fun onConnected() {
            _connectionState.value = ConnectionState.Connected
            setupCustomCmdListener()
        }
        
        override fun onDisconnected() {
            _connectionState.value = ConnectionState.Disconnected
        }
        
        override fun onFailed(errorCode: ValueUtil.CxrBluetoothErrorCode?) {
            _connectionState.value = ConnectionState.Error(errorCode?.name ?: "Unknown")
        }
    }
    
    /**
     * 初始化蓝牙连接（首次配对）
     */
    fun initBluetooth(device: BluetoothDevice) {
        _connectionState.value = ConnectionState.Connecting
        cxrApi.initBluetooth(context, device, bluetoothCallback)
    }
    
    /**
     * 使用 License 连接（已配对设备重连）
     */
    private fun connectWithLicense() {
        val uuid = recordedUuid ?: return
        val mac = recordedMacAddress ?: return
        val license = readLicenseFile()
        val secret = CONSTANT.CLIENT_SECRET.replace("-", "")
        
        cxrApi.connectBluetooth(context, uuid, mac, bluetoothCallback, license, secret)
    }
    
    /**
     * 设置自定义协议监听器
     */
    private fun setupCustomCmdListener() {
        cxrApi.setCustomCmdListener { cmdName, caps ->
            when (cmdName) {
                Config.MsgName.IMAGE -> handleImageReceived(caps)
                Config.MsgName.COMMAND -> handleCommandReceived(caps)
            }
        }
    }
    
    /**
     * 处理接收到的图片数据
     * 
     * 眼镜端发送格式 (参考 android/BLUETOOTH_PROTOCOL.md):
     * Caps 结构:
     * [0] String: 消息类型 = "nutrition_image"
     * [1] String: 图片格式 = "jpeg"
     * [2] Int32:  数据大小 (bytes)
     * [3] Long:   时间戳 (毫秒)
     * [4] Boolean: 是否主动拍照 (true=用户主动拍照，评估应更仔细)
     * Binary Data: JPEG 图片数据（通过 sendMessage 的 data 参数传输）
     */
    private fun handleImageReceived(caps: Caps?, binaryData: ByteArray?) {
        caps ?: return
        binaryData ?: return
        
        val format = caps.at(1).string           // "jpeg"
        val dataSize = caps.at(2).int            // 数据大小
        val timestamp = caps.at(3).double.toLong() // 时间戳
        val isManualCapture = caps.at(4).int != 0  // 是否主动拍照
        
        _receivedImage.tryEmit(ImageData(
            data = binaryData,
            format = format,
            timestamp = timestamp,
            isManualCapture = isManualCapture
        ))
    }
    
    /**
     * 处理接收到的指令
     * 
     * 眼镜端发送格式:
     * Caps 结构:
     * [0] String: 消息类型 = "nutrition_command"
     * [1] String: 指令类型 = "start_meal" | "end_meal"
     * [2] Long:   时间戳 (毫秒)
     */
    private fun handleCommandReceived(caps: Caps?) {
        caps ?: return
        val commandType = caps.at(1).string  // "start_meal" | "end_meal"
        _receivedCommand.tryEmit(commandType)
    }
    
    /**
     * 发送营养结果到眼镜
     * 
     * 参考 android/BLUETOOTH_PROTOCOL.md:
     * Caps 结构:
     * [0] String: 菜品描述（如 "红烧肉 · 米饭"）
     * [1] Float:  总热量 (kcal)
     * [2] Float:  蛋白质 (g)
     * [3] Float:  碳水化合物 (g)
     * [4] Float:  脂肪 (g)
     * [5] String: LLM 建议文本
     * 
     * 注意：菜品描述需要从 VLM 结果中提取 dish_name 并组合：
     * - 单道菜: "红烧肉"
     * - 两道菜: "红烧肉 · 米饭"
     * - 三道及以上: "红烧肉等3道菜"
     */
    fun sendNutritionResult(result: NutritionResult) {
        val caps = Caps().apply {
            write(result.foodName)           // [0] 菜品描述
            write(result.calories.toFloat()) // [1] 热量
            write(result.protein.toFloat())  // [2] 蛋白质
            write(result.carbs.toFloat())    // [3] 碳水
            write(result.fat.toFloat())      // [4] 脂肪
            write(result.suggestion)         // [5] 建议
        }
        cxrApi.sendCustomCmd(Config.MsgName.RESULT, caps)
    }
    
    /**
     * 从 VLM 响应中提取菜品描述
     */
    fun formatFoodName(foods: List<FoodItem>): String {
        return when {
            foods.isEmpty() -> "未识别到食物"
            foods.size == 1 -> foods[0].dish_name
            foods.size == 2 -> "${foods[0].dish_name} · ${foods[1].dish_name}"
            else -> "${foods[0].dish_name}等${foods.size}道菜"
        }
    }
    
    /**
     * 发送会话状态到眼镜
     * 
     * 参考 android/BLUETOOTH_PROTOCOL.md:
     * Caps 结构:
     * [0] String: 会话ID
     * [1] String: 状态 ("active" | "ended")
     * [2] Float:  总摄入热量 (kcal)
     * [3] String: 消息文本
     */
    fun sendSessionStatus(sessionId: String, status: String, totalConsumed: Double, message: String) {
        val caps = Caps().apply {
            write(sessionId)                 // [0] 会话ID
            write(status)                    // [1] 状态
            write(totalConsumed.toFloat())   // [2] 总摄入热量
            write(message)                   // [3] 消息
        }
        cxrApi.sendCustomCmd(Config.MsgName.SESSION_STATUS, caps)
    }
    
    /**
     * 打开自定义视图（AR 显示）
     */
    fun openARView(result: NutritionResult) {
        val viewJson = buildNutritionViewJson(result)
        cxrApi.openCustomView(viewJson)
    }
    
    /**
     * 更新自定义视图
     */
    fun updateARView(result: NutritionResult) {
        val updateJson = buildNutritionUpdateJson(result)
        cxrApi.updateCustomView(updateJson)
    }
    
    /**
     * 关闭自定义视图
     */
    fun closeARView() {
        cxrApi.closeCustomView()
    }
    
    /**
     * 远程触发眼镜拍照
     */
    fun takeGlassPhoto(width: Int = 1920, height: Int = 1080, quality: Int = 85, 
                       callback: (ByteArray?) -> Unit) {
        cxrApi.takeGlassPhotoGlobal(width, height, quality) { status, imageData ->
            if (status == ValueUtil.CxrStatus.RESPONSE_SUCCEED) {
                callback(imageData)
            } else {
                callback(null)
            }
        }
    }
    
    /**
     * 获取眼镜设备信息
     */
    fun getGlassInfo(callback: (GlassInfo?) -> Unit) {
        cxrApi.getGlassInfo { status, glassInfo ->
            if (status == ValueUtil.CxrStatus.RESPONSE_SUCCEED) {
                callback(glassInfo)
            } else {
                callback(null)
            }
        }
    }
    
    /**
     * 设置电量监听
     */
    fun setBatteryListener(listener: (Int, Boolean) -> Unit) {
        cxrApi.setBatteryLevelUpdateListener { level, isCharging ->
            listener(level, isCharging)
        }
    }
    
    /**
     * 断开连接
     */
    fun disconnect() {
        cxrApi.deinitBluetooth()
        _connectionState.value = ConnectionState.Disconnected
    }
    
    /**
     * 检查连接状态
     */
    fun isConnected(): Boolean = cxrApi.isBluetoothConnected
    
    private fun readLicenseFile(): ByteArray {
        return context.resources.openRawResource(R.raw.rokid_license).readBytes()
    }
    
    private fun buildNutritionViewJson(result: NutritionResult): String {
        // 构建 CustomView JSON
        return """
        {
            "type": "LinearLayout",
            "props": {
                "id": "root",
                "layout_width": "match_parent",
                "layout_height": "wrap_content",
                "backgroundColor": "#CC000000",
                "orientation": "vertical",
                "paddingStart": "16dp",
                "paddingEnd": "16dp",
                "paddingTop": "8dp",
                "paddingBottom": "8dp"
            },
            "children": [
                {
                    "type": "TextView",
                    "props": {
                        "id": "foodName",
                        "text": "${result.foodName}",
                        "textColor": "#FFFFFF",
                        "textSize": "20sp",
                        "textStyle": "bold"
                    }
                },
                {
                    "type": "TextView",
                    "props": {
                        "id": "calories",
                        "text": "热量: ${result.calories.toInt()} kcal",
                        "textColor": "#FF6B6B",
                        "textSize": "16sp"
                    }
                },
                {
                    "type": "TextView",
                    "props": {
                        "id": "macros",
                        "text": "蛋白质: ${result.protein.toInt()}g | 碳水: ${result.carbs.toInt()}g | 脂肪: ${result.fat.toInt()}g",
                        "textColor": "#AAAAAA",
                        "textSize": "14sp"
                    }
                },
                {
                    "type": "TextView",
                    "props": {
                        "id": "suggestion",
                        "text": "${result.suggestion}",
                        "textColor": "#4CAF50",
                        "textSize": "14sp",
                        "marginTop": "8dp"
                    }
                }
            ]
        }
        """.trimIndent()
    }
    
    private fun buildNutritionUpdateJson(result: NutritionResult): String {
        return """
        {
            "updateList": [
                {"id": "foodName", "props": {"text": "${result.foodName}"}},
                {"id": "calories", "props": {"text": "热量: ${result.calories.toInt()} kcal"}},
                {"id": "macros", "props": {"text": "蛋白质: ${result.protein.toInt()}g | 碳水: ${result.carbs.toInt()}g | 脂肪: ${result.fat.toInt()}g"}},
                {"id": "suggestion", "props": {"text": "${result.suggestion}"}}
            ]
        }
        """.trimIndent()
    }
}

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

data class ImageData(
    val data: ByteArray,
    val format: String,
    val timestamp: Long,
    val isManualCapture: Boolean = false  // 是否用户主动拍照（影响评估仔细度）
)
```

#### GlassesConnectionService (前台服务)
```kotlin
class GlassesConnectionService : Service() {
    
    private lateinit var bluetoothManager: BluetoothManager
    
    override fun onCreate() {
        super.onCreate()
        bluetoothManager = BluetoothManager(this)
        startForeground(NOTIFICATION_ID, createNotification())
    }
    
    override fun onBind(intent: Intent?): IBinder? = LocalBinder()
    
    inner class LocalBinder : Binder() {
        fun getBluetoothManager(): BluetoothManager = bluetoothManager
    }
    
    private fun createNotification(): Notification {
        // 创建前台服务通知
        val channel = NotificationChannel(
            CHANNEL_ID, "眼镜连接服务",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("营养助手")
            .setContentText("正在与眼镜保持连接")
            .setSmallIcon(R.drawable.ic_glasses)
            .build()
    }
    
    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "glasses_connection"
    }
}
```

### 2. Network Module

#### ApiService
```kotlin
interface ApiService {
    @Multipart
    @POST("/api/v1/upload")
    suspend fun uploadImage(@Part file: MultipartBody.Part): UploadResponse
    
    @POST("/api/v1/vision/analyze")
    suspend fun analyzeVision(@Body request: VisionAnalyzeRequest): VisionAnalyzeResponse
    
    @POST("/api/v1/meal/start")
    suspend fun startMealSession(
        @Query("user_id") userId: String,
        @Query("meal_type") mealType: String,
        @Body snapshot: SnapshotPayload
    ): MealStartResponse
    
    @POST("/api/v1/meal/update")
    suspend fun updateMealSession(
        @Query("session_id") sessionId: String,
        @Body snapshot: SnapshotPayload
    ): MealUpdateResponse
    
    @POST("/api/v1/meal/end")
    suspend fun endMealSession(
        @Query("session_id") sessionId: String,
        @Body finalSnapshot: SnapshotPayload?
    ): MealEndResponse
    
    @POST("/api/v1/chat/nutrition")
    suspend fun chatNutrition(@Body request: ChatNutritionRequest): ChatNutritionResponse
}
```

#### NetworkManager
```kotlin
class NetworkManager(private val api: ApiService) {
    suspend fun uploadAndAnalyze(imageData: ByteArray): Result<VisionAnalyzeResponse>
    suspend fun startMeal(visionResult: VisionAnalyzeResponse, userProfile: UserProfile): Result<MealStartResponse>
    suspend fun updateMeal(sessionId: String, snapshot: SnapshotPayload): Result<MealUpdateResponse>
    suspend fun endMeal(sessionId: String, finalSnapshot: SnapshotPayload?): Result<MealEndResponse>
}
```

### 3. Database Module

手机端需要本地存储用餐数据，与后端数据库结构对应但简化。

#### Room Entities（与后端 models.py 对应）
```kotlin
@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: String = "default",
    val age: Int,
    val height: Float,  // cm
    val weight: Float,  // kg
    val bmi: Float,
    val healthConditions: String,  // JSON array: ["脂肪肝", "高血压"]
    val dietaryPreferences: String,  // JSON array: ["低油", "低糖"]
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * 用餐会话（对应后端 MealSession）
 */
@Entity(tableName = "meal_sessions")
data class MealSessionEntity(
    @PrimaryKey val sessionId: String,
    val userId: String,
    val mealType: String,  // breakfast/lunch/dinner/snack
    val status: String,    // active/completed
    val startTime: Long,
    val endTime: Long?,
    val autoCaptureInterval: Int = 300,  // 秒
    // 最终统计（会话结束时填充）
    val totalServedKcal: Double?,
    val totalConsumedKcal: Double?,
    val consumptionRatio: Double?,
    val durationMinutes: Double?,
    val report: String?,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * 用餐快照（对应后端 MealSnapshot）
 */
@Entity(
    tableName = "meal_snapshots",
    foreignKeys = [ForeignKey(
        entity = MealSessionEntity::class,
        parentColumns = ["sessionId"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class MealSnapshotEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val imageUrl: String,
    val localImagePath: String?,  // 本地缓存路径
    val capturedAt: Long,
    val model: String = "qwen3-vl-plus",
    val rawJson: String?,  // 后端返回的原始 JSON
    val totalKcal: Double
)

/**
 * 快照中的食物（对应后端 SnapshotFood）
 */
@Entity(
    tableName = "snapshot_foods",
    foreignKeys = [ForeignKey(
        entity = MealSnapshotEntity::class,
        parentColumns = ["id"],
        childColumns = ["snapshotId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class SnapshotFoodEntity(
    @PrimaryKey val id: String,
    val snapshotId: String,
    val name: String,
    val chineseName: String?,
    val weightG: Double,
    val caloriesKcal: Double,
    val proteinG: Double?,
    val carbsG: Double?,
    val fatG: Double?,
    val confidence: Double,
    val cookingMethod: String?
)
```

#### DAOs
```kotlin
@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE id = :id")
    suspend fun getProfile(id: String = "default"): UserProfileEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: UserProfileEntity)
    
    @Query("SELECT EXISTS(SELECT 1 FROM user_profiles WHERE id = 'default')")
    suspend fun hasProfile(): Boolean
}

@Dao
interface MealSessionDao {
    @Query("SELECT * FROM meal_sessions ORDER BY startTime DESC LIMIT :limit")
    fun getRecentSessions(limit: Int = 20): Flow<List<MealSessionEntity>>
    
    @Query("SELECT * FROM meal_sessions WHERE sessionId = :sessionId")
    suspend fun getSession(sessionId: String): MealSessionEntity?
    
    @Query("SELECT * FROM meal_sessions WHERE status = 'active' LIMIT 1")
    suspend fun getActiveSession(): MealSessionEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(session: MealSessionEntity)
    
    @Query("UPDATE meal_sessions SET status = :status, endTime = :endTime, totalServedKcal = :totalServed, totalConsumedKcal = :totalConsumed, consumptionRatio = :ratio, durationMinutes = :duration, report = :report, updatedAt = :updatedAt WHERE sessionId = :sessionId")
    suspend fun endSession(
        sessionId: String, 
        status: String, 
        endTime: Long, 
        totalServed: Double, 
        totalConsumed: Double, 
        ratio: Double, 
        duration: Double, 
        report: String?,
        updatedAt: Long
    )
    
    // 统计查询
    @Query("SELECT SUM(totalConsumedKcal) FROM meal_sessions WHERE startTime >= :startOfDay AND startTime < :endOfDay AND status = 'completed'")
    suspend fun getDailyCalories(startOfDay: Long, endOfDay: Long): Double?
    
    @Query("SELECT * FROM meal_sessions WHERE startTime >= :startTime AND startTime < :endTime AND status = 'completed' ORDER BY startTime ASC")
    suspend fun getSessionsInRange(startTime: Long, endTime: Long): List<MealSessionEntity>
}

@Dao
interface MealSnapshotDao {
    @Query("SELECT * FROM meal_snapshots WHERE sessionId = :sessionId ORDER BY capturedAt ASC")
    suspend fun getSnapshotsForSession(sessionId: String): List<MealSnapshotEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSnapshot(snapshot: MealSnapshotEntity)
    
    @Query("SELECT * FROM meal_snapshots WHERE sessionId = :sessionId ORDER BY capturedAt DESC LIMIT 1")
    suspend fun getLatestSnapshot(sessionId: String): MealSnapshotEntity?
}

@Dao
interface SnapshotFoodDao {
    @Query("SELECT * FROM snapshot_foods WHERE snapshotId = :snapshotId")
    suspend fun getFoodsForSnapshot(snapshotId: String): List<SnapshotFoodEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFoods(foods: List<SnapshotFoodEntity>)
}
```

#### AppDatabase
```kotlin
@Database(
    entities = [
        UserProfileEntity::class,
        MealSessionEntity::class,
        MealSnapshotEntity::class,
        SnapshotFoodEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun mealSessionDao(): MealSessionDao
    abstract fun mealSnapshotDao(): MealSnapshotDao
    abstract fun snapshotFoodDao(): SnapshotFoodDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nutrition_phone.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
```

### 4. UI Module

#### Screens
- **HomeScreen**: 主页，显示连接状态、当前会话、快捷操作
- **StatsScreen**: 统计页面，显示每日/每周热量图表
- **HistoryScreen**: 历史记录，列表展示过往用餐
- **ProfileScreen**: 用户档案编辑
- **DeviceScreen**: 设备管理，蓝牙配对/设置

#### ViewModels
```kotlin
class HomeViewModel(
    private val bluetoothManager: BluetoothManager,
    private val networkManager: NetworkManager,
    private val sessionRepository: MealSessionRepository
) : ViewModel() {
    val connectionState: StateFlow<ConnectionState>
    val currentSession: StateFlow<MealSession?>
    val analysisState: StateFlow<AnalysisState>
    
    fun onImageReceived(imageData: ImageData)
    fun onCommandReceived(command: String)
    fun startMeal()
    fun endMeal()
}

class StatsViewModel(
    private val sessionRepository: MealSessionRepository
) : ViewModel() {
    val dailyCalories: StateFlow<Double>
    val weeklyData: StateFlow<List<DailyStats>>
    val monthlyTrends: StateFlow<List<MonthlyTrend>>
}
```

## Data Models

### API Response Models（与后端 main.py 返回格式一致）

#### 图片上传响应
```kotlin
data class UploadResponse(
    val filename: String,
    val url: String,      // 相对路径，如 "/uploads/xxx.jpg"
    val message: String
)
```

#### 视觉分析响应
```kotlin
/**
 * POST /api/v1/vision/analyze 响应
 * 
 * 注意：如果图片中没有食物，后端返回 HTTP 400
 */
data class VisionAnalyzeResponse(
    val raw_llm: RawLlm,
    val snapshot: SnapshotData
)

data class RawLlm(
    val is_food: Boolean,
    val foods: List<FoodItem>
)

data class FoodItem(
    val dish_name: String,
    val cooking_method: String,
    val ingredients: List<Ingredient>,
    val total_weight_g: Double,
    val confidence: Double
)

data class Ingredient(
    val name_en: String,
    val weight_g: Double,
    val confidence: Double
)

data class SnapshotData(
    val foods: List<FoodInput>,
    val nutrition: NutritionTotal
)

data class FoodInput(
    val name: String,
    val weight_g: Double,
    val cooking_method: String?
)

data class NutritionTotal(
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)
```

#### 用餐会话响应
```kotlin
/**
 * POST /api/v1/meal/start 响应
 */
data class MealStartResponse(
    val session_id: String,
    val initial_nutrition: NutritionTotal,
    val auto_capture_interval: Int,
    val status: String  // "started"
)

/**
 * POST /api/v1/meal/update 响应
 */
data class MealUpdateResponse(
    val session_id: String,
    val current_status: CurrentStatus,
    val since_last_update: SinceLastUpdate,
    val advice: String,
    val snapshots_count: Int
)

data class CurrentStatus(
    val remaining_calories: Double,
    val consumed_total: Double,
    val consumption_ratio: Double,
    val duration_minutes: Double
)

data class SinceLastUpdate(
    val net_calories_change: Double,
    val time_elapsed_minutes: Double
)

/**
 * POST /api/v1/meal/end 响应
 */
data class MealEndResponse(
    val session_id: String,
    val final_stats: FinalStats,
    val report: String,
    val message: String
)

data class FinalStats(
    val total_served: Double,
    val total_consumed: Double,
    val consumption_ratio: Double,
    val duration_minutes: Double,
    val average_eating_speed: Double,
    val waste_ratio: Double,
    val snapshots_count: Int
)

/**
 * GET /api/v1/meal/session/{session_id} 响应
 */
data class MealSessionDetailResponse(
    val session: SessionInfo,
    val progress: ProgressInfo
)

data class SessionInfo(
    val id: String,
    val user_id: String,
    val status: String,
    val start_time: String,
    val end_time: String?,
    val auto_capture_interval: Int
)

data class ProgressInfo(
    val total_served_calories: Double,
    val current_calories: Double,
    val total_consumed: Double,
    val consumption_ratio: Double,
    val duration_minutes: Double
)

/**
 * GET /api/v1/meal/sessions 响应
 */
data class MealSessionsListResponse(
    val sessions: List<SessionSummary>,
    val total: Int
)

data class SessionSummary(
    val session_id: String,
    val start_time: String,
    val end_time: String?,
    val status: String,
    val initial_calories: Double,
    val current_calories: Double,
    val snapshots_count: Int
)
```

#### 营养对话响应
```kotlin
/**
 * POST /api/v1/chat/nutrition 响应
 */
data class ChatNutritionResponse(
    val answer: String,
    val suggested_actions: List<String>
)
```

### Domain Models
```kotlin
data class NutritionResult(
    val foodName: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val suggestion: String,
    val cookingMethod: String? = null,
    val confidence: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

data class UserProfile(
    val age: Int,
    val height: Float,
    val weight: Float,
    val bmi: Float,
    val healthConditions: List<String>,
    val dietaryPreferences: List<String>
)

data class MealSession(
    val sessionId: String,
    val mealType: String,
    val status: SessionStatus,
    val startTime: Long,
    val endTime: Long?,
    val totalCalories: Double?,
    val consumptionRatio: Double?,
    val durationMinutes: Int?
)

enum class SessionStatus {
    ACTIVE, ENDED
}
```

### Bluetooth Protocol Models
```kotlin
// 消息类型常量
object MsgName {
    const val IMAGE = "nutrition_image"
    const val COMMAND = "nutrition_command"
    const val RESULT = "nutrition_result"
    const val STATUS = "session_status"
}

object CommandType {
    const val START_MEAL = "start_meal"
    const val END_MEAL = "end_meal"
    const val TAKE_PHOTO = "take_photo"
}
```



## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

Based on the prework analysis, the following correctness properties have been identified:

### Property 1: Connection State Consistency
*For any* Bluetooth connection state change, the UI state SHALL reflect the actual connection status within 100ms.
**Validates: Requirements 1.3**

### Property 2: Image Validation
*For any* received byte array claiming to be an image, the validation function SHALL correctly identify valid JPEG format and reject invalid data.
**Validates: Requirements 2.2**

### Property 3: API Response Parsing Round-Trip
*For any* valid VisionAnalyzeResponse JSON from the backend, parsing into domain objects and serializing back SHALL produce equivalent JSON structure.
**Validates: Requirements 3.3**

### Property 4: Retry Logic Correctness
*For any* failed network request, the retry mechanism SHALL attempt exactly 3 retries with exponential backoff (delays of 1s, 2s, 4s).
**Validates: Requirements 3.5**

### Property 5: Result Transmission Completeness
*For any* NutritionResult, the Caps-formatted message SHALL contain all required fields: foodName, calories, protein, carbs, fat, and suggestion.
**Validates: Requirements 4.2**

### Property 6: AR View JSON Validity
*For any* NutritionResult, the generated CustomView JSON SHALL be valid JSON and contain required UI elements (root layout, food name text, calorie text).
**Validates: Requirements 4.3**

### Property 7: Session Lifecycle Integrity
*For any* meal session, the state transitions SHALL follow: CREATED → ACTIVE → ENDED, and session ID SHALL remain constant throughout.
**Validates: Requirements 5.1, 5.3, 5.4**

### Property 8: BMI Calculation Accuracy
*For any* valid height (cm) and weight (kg), BMI SHALL be calculated as weight / (height/100)² with precision to 1 decimal place.
**Validates: Requirements 6.2**

### Property 9: Data Persistence Round-Trip
*For any* MealSession saved to Room database, retrieving by session ID SHALL return an equivalent object.
**Validates: Requirements 8.1, 8.2, 8.3**

### Property 10: Statistics Aggregation Correctness
*For any* set of meal sessions within a date range, the daily calorie sum SHALL equal the sum of individual session calories.
**Validates: Requirements 7.1, 7.5**

### Property 11: Error Message Localization
*For any* error condition, the displayed error message SHALL be non-empty and in Chinese characters.
**Validates: Requirements 10.1**

### Property 12: Offline Queue Ordering
*For any* operations queued during offline mode, replay SHALL maintain FIFO order when network is restored.
**Validates: Requirements 10.2**

## Error Handling

### Network Errors
```kotlin
sealed class NetworkError {
    object NoConnection : NetworkError()
    object Timeout : NetworkError()
    data class ServerError(val code: Int, val message: String) : NetworkError()
    data class ParseError(val cause: Throwable) : NetworkError()
}

class NetworkErrorHandler {
    fun handle(error: NetworkError): UserMessage {
        return when (error) {
            is NetworkError.NoConnection -> UserMessage("网络连接失败，请检查网络设置")
            is NetworkError.Timeout -> UserMessage("请求超时，请稍后重试")
            is NetworkError.ServerError -> UserMessage("服务器错误 (${error.code})，请稍后重试")
            is NetworkError.ParseError -> UserMessage("数据解析失败，请重试")
        }
    }
}
```

### Bluetooth Errors
```kotlin
sealed class BluetoothError {
    object Disabled : BluetoothError()
    object NotPaired : BluetoothError()
    object ConnectionLost : BluetoothError()
    data class TransmissionFailed(val cause: Throwable) : BluetoothError()
}

class BluetoothErrorHandler {
    fun handle(error: BluetoothError): UserMessage {
        return when (error) {
            is BluetoothError.Disabled -> UserMessage("请开启蓝牙功能")
            is BluetoothError.NotPaired -> UserMessage("请先配对 Rokid 眼镜")
            is BluetoothError.ConnectionLost -> UserMessage("蓝牙连接已断开，正在重连...")
            is BluetoothError.TransmissionFailed -> UserMessage("数据传输失败，请重试")
        }
    }
}
```

### Retry Strategy
```kotlin
class RetryStrategy(
    private val maxRetries: Int = 3,
    private val initialDelayMs: Long = 1000,
    private val maxDelayMs: Long = 8000,
    private val factor: Double = 2.0
) {
    suspend fun <T> execute(block: suspend () -> T): Result<T> {
        var currentDelay = initialDelayMs
        repeat(maxRetries) { attempt ->
            try {
                return Result.success(block())
            } catch (e: Exception) {
                if (attempt == maxRetries - 1) {
                    return Result.failure(e)
                }
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
            }
        }
        return Result.failure(IllegalStateException("Retry exhausted"))
    }
}
```

## Testing Strategy

### Unit Testing Framework
- **Framework**: JUnit 5 + MockK
- **Coverage Target**: 80% for business logic

### Property-Based Testing Framework
- **Framework**: Kotest Property Testing
- **Minimum Iterations**: 100 per property

### Test Categories

#### 1. Unit Tests
- ViewModel state management
- Repository data transformations
- Utility functions (BMI calculation, date formatting)
- Error handling logic

#### 2. Property-Based Tests
Each correctness property MUST be implemented as a property-based test using Kotest.

**Test Annotation Format**:
```kotlin
// **Feature: phone-app, Property 3: API Response Parsing Round-Trip**
@Test
fun `parsing and serializing VisionAnalyzeResponse should be equivalent`() {
    checkAll(Arb.visionAnalyzeResponse()) { response ->
        val json = gson.toJson(response)
        val parsed = gson.fromJson(json, VisionAnalyzeResponse::class.java)
        parsed shouldBe response
    }
}
```

#### 3. Integration Tests
- Bluetooth communication flow (with mock SDK)
- Network request/response cycle (with MockWebServer)
- Database operations (with in-memory Room)

### Test File Structure
```
app/src/test/kotlin/com/rokid/nutrition/phone/
├── bluetooth/
│   └── BluetoothManagerTest.kt
├── network/
│   ├── ApiServiceTest.kt
│   └── NetworkManagerTest.kt
├── database/
│   ├── UserProfileDaoTest.kt
│   └── MealSessionDaoTest.kt
├── viewmodel/
│   ├── HomeViewModelTest.kt
│   └── StatsViewModelTest.kt
├── property/
│   ├── ConnectionStatePropertyTest.kt
│   ├── ImageValidationPropertyTest.kt
│   ├── ApiParsingPropertyTest.kt
│   ├── RetryLogicPropertyTest.kt
│   ├── ResultTransmissionPropertyTest.kt
│   ├── ARViewJsonPropertyTest.kt
│   ├── SessionLifecyclePropertyTest.kt
│   ├── BMICalculationPropertyTest.kt
│   ├── DataPersistencePropertyTest.kt
│   ├── StatisticsAggregationPropertyTest.kt
│   ├── ErrorMessagePropertyTest.kt
│   └── OfflineQueuePropertyTest.kt
└── util/
    └── TestGenerators.kt
```

### Generators for Property Tests
```kotlin
object TestGenerators {
    fun Arb.Companion.nutritionResult(): Arb<NutritionResult> = arbitrary {
        NutritionResult(
            foodName = Arb.string(1..50).bind(),
            calories = Arb.double(0.0..5000.0).bind(),
            protein = Arb.double(0.0..500.0).bind(),
            carbs = Arb.double(0.0..500.0).bind(),
            fat = Arb.double(0.0..500.0).bind(),
            suggestion = Arb.string(0..200).bind()
        )
    }
    
    fun Arb.Companion.userProfile(): Arb<UserProfile> = arbitrary {
        val height = Arb.float(100f..250f).bind()
        val weight = Arb.float(30f..200f).bind()
        UserProfile(
            age = Arb.int(1..120).bind(),
            height = height,
            weight = weight,
            bmi = weight / ((height / 100f) * (height / 100f)),
            healthConditions = Arb.list(Arb.string(1..20), 0..5).bind(),
            dietaryPreferences = Arb.list(Arb.string(1..20), 0..5).bind()
        )
    }
    
    fun Arb.Companion.visionAnalyzeResponse(): Arb<VisionAnalyzeResponse> = arbitrary {
        VisionAnalyzeResponse(
            foods = Arb.list(Arb.foodItem(), 1..5).bind(),
            total = Arb.nutritionTotal().bind(),
            raw_llm = Arb.rawLlm().orNull().bind()
        )
    }
}
```
