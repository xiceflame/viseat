package com.rokid.nutrition.phone.util

import com.rokid.nutrition.phone.bluetooth.ConnectionState
import com.rokid.nutrition.phone.network.NetworkError
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * 错误处理器
 * 
 * 将各种错误转换为用户友好的中文消息
 */
object ErrorHandler {
    
    /**
     * 处理网络错误
     */
    fun handleNetworkError(error: Throwable): String {
        return when (error) {
            is NetworkError.NoConnection -> "网络连接失败，请检查网络设置"
            is NetworkError.Timeout -> "请求超时，请稍后重试"
            is NetworkError.ServerError -> "服务器错误 (${error.code})，请稍后重试"
            is NetworkError.ParseError -> "数据解析失败，请重试"
            is UnknownHostException -> "无法连接到服务器，请检查网络"
            is SocketTimeoutException -> "连接超时，请稍后重试"
            else -> "网络请求失败: ${error.message ?: "未知错误"}"
        }
    }
    
    /**
     * 处理蓝牙错误
     */
    fun handleBluetoothError(state: ConnectionState): String {
        return when (state) {
            is ConnectionState.Error -> when (state.message) {
                "BLUETOOTH_DISABLED" -> "请开启蓝牙功能"
                "DEVICE_NOT_FOUND" -> "未找到眼镜设备"
                "CONNECTION_FAILED" -> "连接失败，请重试"
                "PAIRING_FAILED" -> "配对失败，请重新配对"
                else -> "蓝牙错误: ${state.message}"
            }
            is ConnectionState.Disconnected -> "蓝牙连接已断开"
            else -> ""
        }
    }
    
    /**
     * 处理通用错误
     */
    fun handleError(error: Throwable): String {
        return error.message ?: "发生未知错误"
    }
}

/**
 * 蓝牙错误类型
 */
sealed class BluetoothError : Exception() {
    object Disabled : BluetoothError()
    object NotPaired : BluetoothError()
    object ConnectionLost : BluetoothError()
    data class TransmissionFailed(override val cause: Throwable) : BluetoothError()
}
