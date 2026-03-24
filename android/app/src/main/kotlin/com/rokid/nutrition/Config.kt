package com.rokid.nutrition

/**
 * 眼镜端应用配置（瘦客户端）
 * 
 * 眼镜端不直接联网，通过蓝牙与手机通信
 */
object Config {

    // ==================== 应用配置 ====================

    const val APP_NAME = "Rokid 营养助手"
    const val APP_VERSION = "3.0.0"

    // ==================== 相机配置 ====================

    const val CAMERA_RESOLUTION = 1920
    
    const val IMAGE_OUTPUT_WIDTH = 1920
    const val IMAGE_OUTPUT_HEIGHT = 1320

    const val IMAGE_MAX_BYTES = 1_000_000

    const val IMAGE_FALLBACK_WIDTH = 1600
    const val IMAGE_FALLBACK_HEIGHT = 1100

    const val PREVIEW_CROP_CX = 0.5f
    const val PREVIEW_CROP_CY = 0.48f
    const val PREVIEW_CROP_WIDTH_RATIO = 0.60f
    
    @Deprecated("使用 IMAGE_OUTPUT_WIDTH/HEIGHT 代替")
    const val MAX_IMAGE_SIZE = 1280

    const val IMAGE_QUALITY = 88

    const val CENTER_CROP_RATIO = 0.85f

    // ==================== 用餐监测配置 ====================

    const val AUTO_CAPTURE_INTERVAL_MS = 5 * 60 * 1000L

    const val RECOGNITION_TIMEOUT_MS = 20 * 1000L

    const val MAX_RETRY_COUNT = 3

    // ==================== 蓝牙通信配置 ====================

    const val BLUETOOTH_SERVICE_UUID = "00009100-0000-1000-8000-00805f9b34fb"

    /** 消息名称常量 */
    object MsgName {
        const val IMAGE = "nutrition_image"           // 眼镜→手机: 图片
        const val COMMAND = "nutrition_command"       // 眼镜→手机: 指令
        const val RESULT = "nutrition_result"         // 手机→眼镜: 结果
        const val SESSION_STATUS = "session_status"   // 手机→眼镜: 会话状态
        const val PROCESSING_PHASE = "processing_phase"  // 手机→眼镜: 处理阶段
        const val PERSONALIZED_TIP = "personalized_tip"  // 手机→眼镜: 个性化建议
    }
    
    /** 处理阶段代码 */
    object ProcessingPhaseCode {
        const val UPLOADING = 1         // 上传中
        const val ANALYZING = 2         // 识别菜品中
        const val CALCULATING = 3       // 热量计算中
        const val COMPLETE = 4          // 完成
        const val ERROR = 5             // 错误
        const val NOT_FOOD = 6          // 未检测到食物
    }

    /** 指令类型常量 */
    object CommandType {
        const val START_MEAL = "start_meal"
        const val END_MEAL = "end_meal"
        const val TAKE_PHOTO = "take_photo"
    }

    // ==================== Rokid 眼镜配置 ====================

    /** 是否启用侧键事件监听 */
    const val ENABLE_SIDE_KEY = true

    // ==================== 日志配置 ====================

    const val DEBUG_LOG_ENABLED = true
    const val LOG_TAG = "RokidGlasses"
}
