package com.rokid.nutrition.phone

import android.Manifest
import android.os.Build

/**
 * 手机端应用配置（网络中枢）
 * 
 * 手机端负责蓝牙通信、API 调用、数据存储
 */
object Config {

    // ==================== 应用配置 ====================

    const val APP_NAME = "Rokid 营养助手"
    const val APP_VERSION = "1.0.0"

    // ==================== 蓝牙权限 ====================
    
    val BLUETOOTH_PERMISSIONS = mutableListOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_SCAN)
        }
    }.toTypedArray()

    // ==================== 蓝牙配置 ====================
    
    /** Rokid 蓝牙服务 UUID（与眼镜端一致） */
    const val SERVICE_UUID = "00009100-0000-1000-8000-00805f9b34fb"
    
    /** 从 Rokid 开发者平台获取的客户端密钥 */
    const val CLIENT_SECRET = "c7cba481-c9da-11f0-961e-043f72fdb9c8"

    // ==================== 消息协议（与眼镜端 Config.MsgName 一致） ====================
    
    object MsgName {
        /** 眼镜→手机: 图片数据 */
        const val IMAGE = "nutrition_image"
        /** 眼镜→手机: 用户指令 */
        const val COMMAND = "nutrition_command"
        /** 手机→眼镜: 识别结果 */
        const val RESULT = "nutrition_result"
        /** 手机→眼镜: 会话状态 */
        const val SESSION_STATUS = "session_status"
        /** 手机→眼镜: 处理阶段状态 */
        const val PROCESSING_PHASE = "processing_phase"
        /** 手机→眼镜: 用餐总结 */
        const val MEAL_SUMMARY = "meal_summary"
    }


    /** 指令类型（与眼镜端 Config.CommandType 一致） */
    object CommandType {
        const val START_MEAL = "start_meal"
        const val END_MEAL = "end_meal"
        const val TAKE_PHOTO = "take_photo"
    }

    // ==================== 后端配置 ====================
    
    /** 后端 API 地址 */
    const val API_BASE_URL = "https://viseat.cn"
    
    /** 网络超时（秒） */
    const val NETWORK_TIMEOUT_SECONDS = 30L
    
    /** 最大重试次数 */
    const val MAX_RETRY_COUNT = 3
    
    /** 初始重试延迟（毫秒） */
    const val INITIAL_RETRY_DELAY_MS = 1000L
    
    /** 最大重试延迟（毫秒） */
    const val MAX_RETRY_DELAY_MS = 8000L

    // ==================== 会话配置 ====================
    
    /** 默认自动拍照间隔（秒） */
    const val DEFAULT_AUTO_CAPTURE_INTERVAL = 300

    // ==================== 通知配置 ====================
    
    const val NOTIFICATION_CHANNEL_ID = "glasses_connection"
    const val NOTIFICATION_ID = 1001

    // ==================== 日志配置 ====================

    const val DEBUG_LOG_ENABLED = true
    const val LOG_TAG = "NutritionPhone"
}
