package com.rokid.nutrition

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Rokid 眼镜管理器（眼镜端）
 * 
 * 职责：
 * - TTS 语音播报
 * - AR 内容显示
 * 
 * 注：蓝牙通信由 BluetoothSender/Receiver 负责
 */
class RokidManager(private val context: Context) {
    
    companion object {
        private const val TAG = "RokidManager"
        
        @Volatile
        private var instance: RokidManager? = null
        
        fun getInstance(context: Context): RokidManager {
            return instance ?: synchronized(this) {
                instance ?: RokidManager(context).also { instance = it }
            }
        }
    }
    
    private var isInitialized = false
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    
    /**
     * 初始化
     */
    fun initialize() {
        initTts()
        isInitialized = true
        Log.d(TAG, "RokidManager 初始化完成")
    }
    
    /**
     * 初始化 TTS 引擎
     */
    private fun initTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.CHINESE
                isTtsReady = true
                Log.d(TAG, "TTS 初始化成功")
            } else {
                Log.e(TAG, "TTS 初始化失败: $status")
            }
        }
    }
    
    /**
     * TTS 语音播报
     */
    fun speak(text: String) {
        if (!isTtsReady) {
            Log.w(TAG, "TTS 未就绪，跳过播报: $text")
            return
        }
        
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "rokid_tts_${System.currentTimeMillis()}")
        Log.d(TAG, "TTS 播报: $text")
    }
    
    /**
     * 停止语音播报
     */
    fun stopSpeaking() {
        tts?.stop()
    }
    
    /**
     * 显示 AR 内容
     * TODO: 集成 Rokid AR SDK
     */
    fun showARContent(content: ARContent) {
        Log.d(TAG, "显示 AR 内容: ${content.title}")
        // TODO: 实际的 AR 显示实现
    }
    
    /**
     * 隐藏 AR 内容
     */
    fun hideARContent() {
        Log.d(TAG, "隐藏 AR 内容")
        // TODO: 实际的 AR 隐藏实现
    }
    
    /**
     * 释放资源
     */
    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isTtsReady = false
        isInitialized = false
        Log.d(TAG, "RokidManager 已释放")
    }
    
    /**
     * AR 内容数据类
     */
    data class ARContent(
        val title: String,
        val subtitle: String? = null,
        val items: List<String> = emptyList(),
        val position: ARPosition = ARPosition.CENTER
    )
    
    /**
     * AR 显示位置
     */
    enum class ARPosition {
        TOP, CENTER, BOTTOM
    }
}
