package com.rokid.nutrition.phone.util

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 调试日志管理器
 * 
 * 功能：
 * 1. 将日志写入 Download 文件夹
 * 2. 通过 StateFlow 暴露日志供 UI 显示
 */
object DebugLogger {
    
    private const val TAG = "DebugLogger"
    private const val LOG_FILE_NAME = "rokid_nutrition_debug.log"
    private const val MAX_UI_LOGS = 100
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    private var logFile: File? = null
    
    // UI 日志列表
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs
    
    data class LogEntry(
        val timestamp: String,
        val level: String,
        val tag: String,
        val message: String
    ) {
        override fun toString() = "[$timestamp] $level/$tag: $message"
    }
    
    /**
     * 初始化日志文件
     */
    fun init(context: Context) {
        try {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }
            logFile = File(downloadDir, LOG_FILE_NAME)
            
            // 写入启动标记
            val startMsg = "\n\n========== APP STARTED ${dateFormat.format(Date())} ==========\n"
            logFile?.appendText(startMsg)
            
            i(TAG, "日志文件初始化成功: ${logFile?.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "初始化日志文件失败", e)
        }
    }
    
    /**
     * Info 级别日志
     */
    fun i(tag: String, message: String) {
        log("I", tag, message)
        Log.i(tag, message)
    }
    
    /**
     * Debug 级别日志
     */
    fun d(tag: String, message: String) {
        log("D", tag, message)
        Log.d(tag, message)
    }
    
    /**
     * Warning 级别日志
     */
    fun w(tag: String, message: String) {
        log("W", tag, message)
        Log.w(tag, message)
    }
    
    /**
     * Error 级别日志
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val fullMessage = if (throwable != null) {
            "$message\n${throwable.stackTraceToString()}"
        } else {
            message
        }
        log("E", tag, fullMessage)
        Log.e(tag, message, throwable)
    }
    
    /**
     * 网络请求日志（特殊标记）
     */
    fun network(tag: String, message: String) {
        log("NET", tag, message)
        Log.d(tag, "[NET] $message")
    }
    
    private fun log(level: String, tag: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val entry = LogEntry(timestamp, level, tag, message)
        
        // 写入文件
        try {
            logFile?.appendText("$entry\n")
        } catch (e: Exception) {
            Log.e(TAG, "写入日志文件失败", e)
        }
        
        // 更新 UI 日志
        val currentLogs = _logs.value.toMutableList()
        currentLogs.add(0, entry)  // 新日志在前
        if (currentLogs.size > MAX_UI_LOGS) {
            currentLogs.removeAt(currentLogs.size - 1)
        }
        _logs.value = currentLogs
    }
    
    /**
     * 清除 UI 日志
     */
    fun clearUiLogs() {
        _logs.value = emptyList()
    }
    
    /**
     * 获取日志文件路径
     */
    fun getLogFilePath(): String? = logFile?.absolutePath
}
