package com.rokid.nutrition

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 相机管理器（眼镜端）
 * 
 * 职责：拍照并返回压缩后的 Bitmap
 * 不需要预览界面（眼镜端无屏幕预览需求）
 */
class CameraManager(private val context: Context) {
    
    companion object {
        private const val TAG = "CameraManager"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
    
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var cameraExecutor: ExecutorService
    private var isInitialized = false
    
    init {
        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    
    /**
     * 初始化相机（无预览模式）
     * 眼镜端不需要预览界面，直接初始化拍照功能
     */
    fun initialize(
        lifecycleOwner: LifecycleOwner,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                
                // 仅图像捕获，无预览
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetResolution(android.util.Size(Config.MAX_IMAGE_SIZE, Config.MAX_IMAGE_SIZE))
                    .build()
                
                // 选择后置摄像头（眼镜的主摄像头）
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                // 绑定到生命周期
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    imageCapture
                )
                
                isInitialized = true
                Log.d(TAG, "相机初始化成功（无预览模式）")
                onSuccess()
            } catch (e: Exception) {
                Log.e(TAG, "相机初始化失败", e)
                onError(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }
    
    /**
     * 拍照
     * @param callback 回调函数，返回压缩后的 Bitmap 和错误信息
     */
    fun takePicture(callback: (Bitmap?, String?) -> Unit) {
        if (!isInitialized) {
            Log.e(TAG, "相机未初始化")
            callback(null, "相机未初始化")
            return
        }
        
        val capture = imageCapture ?: run {
            Log.e(TAG, "ImageCapture 未初始化")
            callback(null, "相机未初始化")
            return
        }
        
        // 创建输出文件
        val photoFile = createImageFile()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        
        capture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d(TAG, "照片已保存: ${photoFile.absolutePath}")
                    
                    // 压缩图片
                    val compressedBitmap = compressImage(photoFile)
                    
                    // 删除临时文件
                    photoFile.delete()
                    
                    callback(compressedBitmap, null)
                }
                
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "拍照失败", exc)
                    callback(null, "拍照失败: ${exc.message}")
                }
            }
        )
    }
    
    /**
     * 压缩图片到指定分辨率和质量
     */
    private fun compressImage(imageFile: File): Bitmap? {
        return try {
            // 读取原始图片
            val originalBitmap = BitmapFactory.decodeFile(imageFile.absolutePath) ?: return null
            
            // 计算缩放比例
            val maxSize = Config.MAX_IMAGE_SIZE.toFloat()
            val scale = minOf(
                maxSize / originalBitmap.width,
                maxSize / originalBitmap.height,
                1f  // 不放大
            )
            
            if (scale >= 1f) {
                // 不需要缩放
                Log.d(TAG, "图片无需缩放: ${originalBitmap.width}x${originalBitmap.height}")
                return originalBitmap
            }
            
            // 缩放图片
            val scaledWidth = (originalBitmap.width * scale).toInt()
            val scaledHeight = (originalBitmap.height * scale).toInt()
            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)
            
            Log.d(TAG, "图片压缩完成: ${scaledWidth}x${scaledHeight}")
            
            // 回收原始 Bitmap
            if (scaledBitmap != originalBitmap) {
                originalBitmap.recycle()
            }
            
            scaledBitmap
        } catch (e: Exception) {
            Log.e(TAG, "图片压缩失败", e)
            null
        }
    }
    
    /**
     * 创建临时图片文件
     */
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(Date())
        return File.createTempFile("IMG_$timeStamp", ".jpg", context.cacheDir)
    }
    
    /**
     * 释放资源
     */
    fun release() {
        try {
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
            isInitialized = false
            Log.d(TAG, "相机管理器已释放")
        } catch (e: Exception) {
            Log.e(TAG, "释放相机资源失败", e)
        }
    }
}
