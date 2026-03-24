package com.rokid.nutrition.phone.bluetooth

/**
 * 蓝牙连接状态
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Scanning : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

/**
 * 图片类型（与眼镜端 BluetoothSender.ImageType 一致）
 */
object ImageType {
    const val AUTO_CAPTURE = 0      // 自动拍照（用餐监测中）
    const val MANUAL_CAPTURE = 1    // 手动拍照（普通识别）
    const val END_MEAL_CAPTURE = 2  // 结束用餐拍照（需要基线对比并结束会话）
}

/**
 * 接收到的图片数据
 */
data class ImageData(
    val data: ByteArray,
    val format: String,
    val timestamp: Long,
    val isManualCapture: Boolean = false,
    val imageType: Int = ImageType.MANUAL_CAPTURE  // 图片类型
) {
    /** 是否是结束用餐的照片 */
    val isEndMealCapture: Boolean get() = imageType == ImageType.END_MEAL_CAPTURE
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ImageData
        return data.contentEquals(other.data) &&
                format == other.format &&
                timestamp == other.timestamp &&
                isManualCapture == other.isManualCapture
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + format.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + isManualCapture.hashCode()
        return result
    }
}

/**
 * 营养结果（发送到眼镜）
 * 
 * category 字段用于眼镜端判断是否进入用餐监测：
 * - "meal": 正餐，进入用餐监测模式
 * - "snack": 零食
 * - "beverage": 饮料
 * - "dessert": 甜点
 * - "fruit": 水果
 */
data class NutritionResult(
    val foodName: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val suggestion: String,
    val category: String = "meal",  // 食物类型，眼镜端根据此字段判断是否进入用餐监测
    val cookingMethod: String? = null,
    val confidence: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 食物项（用于格式化菜品名称）
 */
data class FoodItem(
    val dishName: String,
    val cookingMethod: String? = null,
    val weightG: Double = 0.0,
    val confidence: Double = 0.0
)

/**
 * 眼镜设备信息
 */
data class GlassInfo(
    val name: String,
    val firmwareVersion: String,
    val serialNumber: String,
    val batteryLevel: Int = 0,
    val isCharging: Boolean = false
)
