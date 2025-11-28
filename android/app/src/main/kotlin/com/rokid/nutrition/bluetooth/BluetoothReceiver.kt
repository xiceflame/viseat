package com.rokid.nutrition.bluetooth

import android.util.Log
import com.rokid.nutrition.Config

/**
 * CXR-S SDK 消息接收器（眼镜端）
 * 
 * 订阅手机端发来的消息：
 * - 营养识别结果
 * - 会话状态更新
 * 
 * ⚠️ 真机测试说明：
 * 在 Rokid 设备上编译时，需要：
 * 1. 取消注释下方的 SDK imports
 * 2. 取消注释 cxrBridge 实例化
 * 3. 将 USE_REAL_SDK 设为 true
 * 
 * 参考: https://www.cnblogs.com/xiongbatianduxing/p/19143814
 */

// ========== 真机模式已启用 ==========
import com.rokid.cxr.CXRServiceBridge
import com.rokid.cxr.Caps
// ========================================

class BluetoothReceiver {
    
    companion object {
        private const val TAG = "CXRReceiver"
        
        /**
         * ⚠️ 真机测试开关
         * 
         * 开发环境（Mac/PC）：false - 使用模拟模式
         * Rokid 设备：true - 使用真实 SDK（需先取消上方 import 注释）
         */
        private const val USE_REAL_SDK = true
    }
    
    // ========== 真机模式已启用 ==========
    private val cxrBridge = CXRServiceBridge()
    // ========================================
    
    private var resultListener: ((NutritionResult) -> Unit)? = null
    private var sessionStatusListener: ((SessionStatus) -> Unit)? = null
    private var processingPhaseListener: ((Int, String) -> Unit)? = null
    private var takePhotoListener: (() -> Unit)? = null
    
    /**
     * 初始化消息订阅
     */
    fun initialize() {
        if (USE_REAL_SDK) {
            // ========== 真机模式已启用 ==========
            Log.d(TAG, "════════════════════════════════════════")
            Log.d(TAG, "CXR 消息订阅初始化（真实模式）")
            Log.d(TAG, "════════════════════════════════════════")
            
            // 订阅营养结果消息
            cxrBridge.subscribe(Config.MsgName.RESULT, object : CXRServiceBridge.MsgCallback {
                override fun onReceive(name: String, args: Caps, data: ByteArray?) {
                    try {
                        val result = NutritionResult(
                            foodName = args.at(0).getString(),
                            calories = args.at(1).getFloat().toDouble(),
                            protein = args.at(2).getFloat().toDouble(),
                            carbs = args.at(3).getFloat().toDouble(),
                            fat = args.at(4).getFloat().toDouble(),
                            suggestion = args.at(5).getString()
                        )
                        Log.d(TAG, "收到营养结果: ${result.foodName}, ${result.calories}kcal")
                        resultListener?.invoke(result)
                    } catch (e: Exception) {
                        Log.e(TAG, "解析营养结果失败", e)
                    }
                }
            })
            
            // 订阅会话状态消息
            cxrBridge.subscribe(Config.MsgName.SESSION_STATUS, object : CXRServiceBridge.MsgCallback {
                override fun onReceive(name: String, args: Caps, data: ByteArray?) {
                    try {
                        val status = SessionStatus(
                            sessionId = args.at(0).getString(),
                            status = args.at(1).getString(),
                            totalConsumed = args.at(2).getFloat().toDouble(),
                            message = args.at(3).getString()
                        )
                        Log.d(TAG, "收到会话状态: ${status.status}, ${status.totalConsumed}kcal")
                        sessionStatusListener?.invoke(status)
                    } catch (e: Exception) {
                        Log.e(TAG, "解析会话状态失败", e)
                    }
                }
            })
            
            // 订阅处理阶段消息（手机端发送的实时状态）
            cxrBridge.subscribe(Config.MsgName.PROCESSING_PHASE, object : CXRServiceBridge.MsgCallback {
                override fun onReceive(name: String, args: Caps, data: ByteArray?) {
                    try {
                        val phaseCode = args.at(0).getString().toIntOrNull() ?: 0
                        val phaseMessage = args.at(1).getString()
                        Log.d(TAG, "收到处理阶段: code=$phaseCode, message=$phaseMessage")
                        processingPhaseListener?.invoke(phaseCode, phaseMessage)
                    } catch (e: Exception) {
                        Log.e(TAG, "解析处理阶段失败", e)
                    }
                }
            })
            
            // 订阅远程拍照命令（手机端通过自定义消息触发）
            val takePhotoResult = cxrBridge.subscribe("take_photo", object : CXRServiceBridge.MsgCallback {
                override fun onReceive(name: String, args: Caps, data: ByteArray?) {
                    try {
                        Log.d(TAG, "════════════════════════════════════════")
                        Log.d(TAG, "★★★ 收到远程拍照命令 (take_photo) ★★★")
                        Log.d(TAG, "args: $args")
                        Log.d(TAG, "data: ${data?.size ?: 0} bytes")
                        Log.d(TAG, "调用 takePhotoListener...")
                        takePhotoListener?.invoke()
                        Log.d(TAG, "════════════════════════════════════════")
                    } catch (e: Exception) {
                        Log.e(TAG, "处理远程拍照命令失败", e)
                        takePhotoListener?.invoke()
                    }
                }
            })
            Log.d(TAG, "订阅 take_photo 结果: $takePhotoResult")
            
            // 订阅 SDK 内置拍照请求（手机端通过 takeGlassPhoto API 触发）
            // 根据 CXR SDK 文档，眼镜端需要订阅 "take_glass_photo" 消息
            val takeGlassPhotoResult = cxrBridge.subscribe("take_glass_photo", object : CXRServiceBridge.MsgCallback {
                override fun onReceive(name: String, args: Caps, data: ByteArray?) {
                    try {
                        Log.d(TAG, "════════════════════════════════════════")
                        Log.d(TAG, "★★★ 收到 SDK 拍照请求 (take_glass_photo) ★★★")
                        Log.d(TAG, "args: $args")
                        Log.d(TAG, "data: ${data?.size ?: 0} bytes")
                        // 解析参数：width, height, quality
                        val width = try { args.at(0).getInt() } catch (e: Exception) { 1920 }
                        val height = try { args.at(1).getInt() } catch (e: Exception) { 1080 }
                        val quality = try { args.at(2).getInt() } catch (e: Exception) { 85 }
                        Log.d(TAG, "拍照参数: ${width}x${height}, quality=$quality")
                        Log.d(TAG, "调用 takePhotoListener...")
                        takePhotoListener?.invoke()
                        Log.d(TAG, "════════════════════════════════════════")
                    } catch (e: Exception) {
                        Log.e(TAG, "处理 SDK 拍照请求失败", e)
                        // 即使解析失败也尝试拍照
                        takePhotoListener?.invoke()
                    }
                }
            })
            Log.d(TAG, "订阅 take_glass_photo 结果: $takeGlassPhotoResult")
            
            // 也订阅可能的其他拍照消息名称
            val photoRequestResult = cxrBridge.subscribe("photo_request", object : CXRServiceBridge.MsgCallback {
                override fun onReceive(name: String, args: Caps, data: ByteArray?) {
                    Log.d(TAG, "★★★ 收到拍照请求 (photo_request) ★★★")
                    takePhotoListener?.invoke()
                }
            })
            Log.d(TAG, "订阅 photo_request 结果: $photoRequestResult")
            
            // 订阅所有可能的消息名称（调试用）
            val debugMsgNames = listOf("capture", "camera", "photo", "image_request", "remote_capture")
            debugMsgNames.forEach { msgName ->
                val result = cxrBridge.subscribe(msgName, object : CXRServiceBridge.MsgCallback {
                    override fun onReceive(name: String, args: Caps, data: ByteArray?) {
                        Log.d(TAG, "★★★ 收到消息: $name ★★★")
                        takePhotoListener?.invoke()
                    }
                })
                Log.d(TAG, "订阅 $msgName 结果: $result")
            }
            
            Log.d(TAG, "════════════════════════════════════════")
            Log.d(TAG, "消息订阅初始化完成")
            Log.d(TAG, "════════════════════════════════════════")
            // ========================================
        } else {
            // 模拟模式（不再自动生成结果）
            Log.d(TAG, "CXR 消息订阅初始化（模拟模式，未自动结果）")
        }
    }
    
    /**
     * 模拟接收结果（仅用于开发测试）
     */
    private fun simulateResult() {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val mockResult = NutritionResult(
                foodName = "红烧肉 · 米饭",
                calories = 650.0,
                protein = 25.0,
                carbs = 80.0,
                fat = 28.0,
                suggestion = "建议搭配蔬菜"
            )
            Log.d(TAG, "模拟收到营养结果: ${mockResult.foodName}")
            resultListener?.invoke(mockResult)
        }, 3000)
    }
    
    /**
     * 触发模拟结果（用于外部调用）
     */
    fun triggerSimulateResult() {
        simulateResult()
    }
    
    /**
     * 模拟用餐结束状态
     */
    fun triggerSimulateMealEnd(totalCalories: Double) {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val status = SessionStatus(
                sessionId = "mock-session-001",
                status = "ended",
                totalConsumed = totalCalories,
                message = "本餐共摄入${totalCalories.toInt()}千卡"
            )
            Log.d(TAG, "模拟用餐结束: ${status.totalConsumed}kcal")
            sessionStatusListener?.invoke(status)
        }, 1000)
    }
    
    /**
     * 设置营养结果监听器
     */
    fun setResultListener(listener: (NutritionResult) -> Unit) {
        resultListener = listener
    }
    
    /**
     * 设置会话状态监听器
     */
    fun setSessionStatusListener(listener: (SessionStatus) -> Unit) {
        sessionStatusListener = listener
    }
    
    /**
     * 设置处理阶段监听器
     * 
     * @param listener 回调函数，参数为 (phaseCode, phaseMessage)
     *   phaseCode: 1=上传中, 2=识别菜品中, 3=热量计算中, 4=完成, 5=错误, 6=未检测到食物
     */
    fun setProcessingPhaseListener(listener: (Int, String) -> Unit) {
        processingPhaseListener = listener
    }
    
    /**
     * 设置远程拍照监听器
     * 
     * 当手机端发送拍照命令时触发
     */
    fun setTakePhotoListener(listener: () -> Unit) {
        takePhotoListener = listener
    }
    
    /**
     * 释放资源
     */
    fun release() {
        // CXR SDK 没有 unsubscribe 方法，订阅会在进程结束时自动清理
        resultListener = null
        sessionStatusListener = null
        processingPhaseListener = null
        takePhotoListener = null
        Log.d(TAG, "CXR Receiver 已释放")
    }
}

/**
 * 营养结果数据类
 * 
 * 后端 VLM 返回格式:
 * - raw_llm.foods[]: 菜品列表，每个包含 dish_name, cooking_method, ingredients[]
 * - snapshot.nutrition: { calories, protein, carbs, fat }
 * 
 * 手机端处理后发送给眼镜端:
 * [0] String: 菜品描述（如 "红烧肉 · 米饭" 或 "检测到2道菜"）
 * [1] Float:  总热量 (kcal)
 * [2] Float:  蛋白质 (g)
 * [3] Float:  碳水化合物 (g)
 * [4] Float:  脂肪 (g)
 * [5] String: 建议文本（由 LLM 生成）
 * 
 * 注意：foodName 应该是手机端从 VLM 结果中提取的菜品名称组合，
 * 而不是原始的食材列表。例如：
 * - 单道菜: "红烧肉"
 * - 多道菜: "红烧肉 · 米饭" 或 "红烧肉等3道菜"
 */
data class NutritionResult(
    val foodName: String,           // 菜品描述（手机端从 VLM 结果提取）
    val calories: Double,           // 总热量
    val protein: Double,            // 蛋白质
    val carbs: Double,              // 碳水化合物
    val fat: Double,                // 脂肪
    val suggestion: String,         // LLM 建议
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 会话状态数据类
 * 
 * 手机端发送格式:
 * [0] String: 会话ID
 * [1] String: 状态 ("active" | "ended")
 * [2] Float:  总摄入热量 (kcal)
 * [3] String: 消息文本
 * [4] Float:  总蛋白质 (g)      // 待后端/手机端实现
 * [5] Float:  总碳水化合物 (g)   // 待后端/手机端实现
 * [6] Float:  总脂肪 (g)        // 待后端/手机端实现
 * [7] Int:    用餐时长 (分钟)    // 待后端/手机端实现
 */
data class SessionStatus(
    val sessionId: String,
    val status: String,
    val totalConsumed: Double,
    val message: String,
    // === 新增字段（待后端/手机端实现后启用）===
    val totalProtein: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val totalFat: Double = 0.0,
    val durationMinutes: Int = 0
)
