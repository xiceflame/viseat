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
 * 接收到的图片数据
 */
data class ImageData(
    val data: ByteArray,
    val format: String,
    val timestamp: Long,
    val isManualCapture: Boolean = false
) {
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
 */
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
