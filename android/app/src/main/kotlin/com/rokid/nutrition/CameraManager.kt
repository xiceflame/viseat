package com.rokid.nutrition

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
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
                // 使用更高的拍摄分辨率，后续会裁剪和压缩
                 imageCapture = ImageCapture.Builder()
                     .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                     .setTargetResolution(android.util.Size(Config.IMAGE_OUTPUT_WIDTH, Config.IMAGE_OUTPUT_HEIGHT))
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
     *
     * 处理流程：
     * 1. 读取 EXIF 旋转信息并校正方向
     * 2. 中心裁剪（聚焦眼前食物，保留 85% 区域）
     * 3. 缩放到目标分辨率（16:11 比例）
     *
     * 优化目标：
     * - 输出分辨率: 16:11
     * - 文件大小: 150-500KB
     * - 食物占画面: 50%+
     */
    private fun compressImage(imageFile: File): Bitmap? {
        return try {
            // 读取原始图片
            var originalBitmap = BitmapFactory.decodeFile(imageFile.absolutePath) ?: return null
            Log.d(TAG, "原始图片: ${originalBitmap.width}x${originalBitmap.height}")

            // 步骤0：读取 EXIF 旋转信息并校正方向
            val rotatedBitmap = rotateImageIfRequired(originalBitmap, imageFile.absolutePath)
            if (rotatedBitmap != originalBitmap) {
                originalBitmap.recycle()
                originalBitmap = rotatedBitmap
            }
            Log.d(TAG, "旋转校正后: ${originalBitmap.width}x${originalBitmap.height}")

             val croppedBitmap = cropPreviewWindow(originalBitmap)
             Log.d(TAG, "裁切窗后: ${croppedBitmap.width}x${croppedBitmap.height}")
            
            // 回收原始 Bitmap
            if (croppedBitmap != originalBitmap) {
                originalBitmap.recycle()
            }
            
             val targetWidth = Config.IMAGE_OUTPUT_WIDTH
             val targetHeight = Config.IMAGE_OUTPUT_HEIGHT

             val scaleWidth = targetWidth.toFloat() / croppedBitmap.width
             val scaleHeight = targetHeight.toFloat() / croppedBitmap.height
             val scale = maxOf(scaleWidth, scaleHeight)

             val scaledWidth = (croppedBitmap.width * scale).toInt()
             val scaledHeight = (croppedBitmap.height * scale).toInt()

             val scaledBitmap = if (scale != 1f) {
                 Bitmap.createScaledBitmap(croppedBitmap, scaledWidth, scaledHeight, true)
             } else {
                 croppedBitmap
             }

             Log.d(TAG, "缩放后: ${scaledBitmap.width}x${scaledBitmap.height}")

             if (scaledBitmap != croppedBitmap) {
                 croppedBitmap.recycle()
             }

             val finalBitmap = if (scaledBitmap.width != targetWidth || scaledBitmap.height != targetHeight) {
                 val x = maxOf(0, (scaledBitmap.width - targetWidth) / 2)
                 val y = maxOf(0, (scaledBitmap.height - targetHeight) / 2)
                 val cropWidth = minOf(targetWidth, scaledBitmap.width)
                 val cropHeight = minOf(targetHeight, scaledBitmap.height)
                 Bitmap.createBitmap(scaledBitmap, x, y, cropWidth, cropHeight)
             } else {
                 scaledBitmap
             }

             Log.d(TAG, "最终图片: ${finalBitmap.width}x${finalBitmap.height}")

             if (finalBitmap != scaledBitmap) {
                 scaledBitmap.recycle()
             }

             finalBitmap
        } catch (e: Exception) {
            Log.e(TAG, "图片压缩失败", e)
            null
        }
    }
    
     private fun cropPreviewWindow(bitmap: Bitmap): Bitmap {
         val width = bitmap.width
         val height = bitmap.height

         val cx = (Config.PREVIEW_CROP_CX * width).coerceIn(0f, width.toFloat())
         val cy = (Config.PREVIEW_CROP_CY * height).coerceIn(0f, height.toFloat())

         val initialW = Config.PREVIEW_CROP_WIDTH_RATIO * width
         val initialH = initialW * (11f / 16f)

         val maxHalfW = minOf(cx, width - cx)
         val maxHalfH = minOf(cy, height - cy)

         val scaleW = if (initialW <= 0f) 1f else (2f * maxHalfW / initialW).coerceAtMost(1f)
         val scaleH = if (initialH <= 0f) 1f else (2f * maxHalfH / initialH).coerceAtMost(1f)
         val scale = minOf(1f, scaleW, scaleH)

         val cropW = (initialW * scale).toInt().coerceAtLeast(1)
         val cropH = (initialH * scale).toInt().coerceAtLeast(1)

         val left = (cx - cropW / 2f).toInt().coerceIn(0, width - cropW)
         val top = (cy - cropH / 2f).toInt().coerceIn(0, height - cropH)

         return Bitmap.createBitmap(bitmap, left, top, cropW, cropH)
     }


    /**
     * 根据 EXIF 信息旋转图片
     */
    private fun rotateImageIfRequired(bitmap: Bitmap, imagePath: String): Bitmap {
        val exif = ExifInterface(imagePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        val rotationDegrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        return if (rotationDegrees != 0f) {
            val matrix = Matrix().apply { postRotate(rotationDegrees) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
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
