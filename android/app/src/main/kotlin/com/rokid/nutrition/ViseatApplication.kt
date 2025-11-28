package com.rokid.nutrition

import android.app.Application
import android.util.Log
import com.rokid.nutrition.bluetooth.BluetoothSender
import com.rokid.nutrition.bluetooth.BluetoothReceiver

/**
 * VISEAT Application
 * 
 * 管理应用级别的单例，确保 CXR 连接在 Activity 重启后仍然保持
 */
class ViseatApplication : Application() {
    
    companion object {
        private const val TAG = "ViseatApp"
        
        // 应用级别单例
        lateinit var bluetoothSender: BluetoothSender
            private set
        lateinit var bluetoothReceiver: BluetoothReceiver
            private set
        
        private var isInitialized = false
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application onCreate")
        
        if (!isInitialized) {
            Log.d(TAG, "初始化蓝牙管理器（Application 级别单例）")
            bluetoothSender = BluetoothSender()
            bluetoothReceiver = BluetoothReceiver()
            bluetoothSender.initialize()
            bluetoothReceiver.initialize()
            isInitialized = true
            Log.d(TAG, "蓝牙管理器初始化完成")
        } else {
            Log.d(TAG, "蓝牙管理器已初始化，跳过")
        }
    }
}
