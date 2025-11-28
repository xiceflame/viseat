package com.rokid.nutrition.phone.repository

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.rokid.nutrition.phone.data.dao.MealSnapshotDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * 照片存储仓库
 * 
 * 处理照片的存储、加载和下载
 */
class PhotoStorageRepository(
    private val context: Context,
    private val snapshotDao: MealSnapshotDao
) {
    companion object {
        private const val TAG = "PhotoStorageRepo"
        private const val PHOTO_DIR = "meal_photos"
    }
    
    /**
     * 获取照片 URI
     * 
     * 优先返回本地路径，如果本地不存在则返回云端 URL
     * 
     * @param snapshotId 快照ID
     * @return 照片 URI（本地路径或云端 URL）
     */
    suspend fun getPhotoUri(snapshotId: String): String? {
        val snapshots = snapshotDao.getSnapshotsForSession(snapshotId)
        val snapshot = snapshots.firstOrNull { it.id == snapshotId }
            ?: snapshotDao.getLatestSnapshot(snapshotId)
            ?: return null
        
        // 优先使用本地路径
        snapshot.localImagePath?.let { localPath ->
            val file = File(localPath)
            if (file.exists()) {
                Log.d(TAG, "使用本地照片: $localPath")
                return localPath
            }
        }
        
        // 回退到云端 URL
        if (snapshot.imageUrl.isNotBlank()) {
            Log.d(TAG, "使用云端照片: ${snapshot.imageUrl}")
            return snapshot.imageUrl
        }
        
        return null
    }
    
    /**
     * 获取照片源（区分本地和云端）
     */
    suspend fun getPhotoSource(snapshotId: String): PhotoSource? {
        val snapshots = snapshotDao.getSnapshotsForSession(snapshotId)
        val snapshot = snapshots.firstOrNull { it.id == snapshotId }
            ?: snapshotDao.getLatestSnapshot(snapshotId)
            ?: return null
        
        // 检查本地路径
        snapshot.localImagePath?.let { localPath ->
            val file = File(localPath)
            if (file.exists()) {
                return PhotoSource.Local(localPath)
            }
        }
        
        // 回退到云端 URL
        if (snapshot.imageUrl.isNotBlank()) {
            return PhotoSource.Remote(snapshot.imageUrl)
        }
        
        return null
    }
    
    /**
     * 保存照片到相册
     * 
     * @param imageUri 图片 URI（本地路径或云端 URL）
     * @return 保存后的 URI
     */
    suspend fun savePhotoToGallery(imageUri: String): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val bitmap = loadBitmap(imageUri)
                ?: return@withContext Result.failure(Exception("无法加载图片"))
            
            val savedUri = saveBitmapToGallery(bitmap)
                ?: return@withContext Result.failure(Exception("保存到相册失败"))
            
            Log.d(TAG, "照片已保存到相册: $savedUri")
            Result.success(savedUri)
        } catch (e: Exception) {
            Log.e(TAG, "保存照片失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 从云端下载照片到本地
     * 
     * @param imageUrl 云端图片 URL
     * @return 本地文件路径
     */
    suspend fun downloadPhotoFromCloud(imageUrl: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val bitmap = loadBitmapFromUrl(imageUrl)
                ?: return@withContext Result.failure(Exception("无法从云端加载图片"))
            
            val localPath = saveBitmapToLocalStorage(bitmap)
            Log.d(TAG, "照片已下载到本地: $localPath")
            Result.success(localPath)
        } catch (e: Exception) {
            Log.e(TAG, "下载照片失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 保存照片到应用本地存储
     */
    suspend fun savePhotoToLocalStorage(bitmap: Bitmap, filename: String): String = withContext(Dispatchers.IO) {
        val photoDir = File(context.filesDir, PHOTO_DIR)
        if (!photoDir.exists()) {
            photoDir.mkdirs()
        }
        
        val file = File(photoDir, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        
        file.absolutePath
    }
    
    // ==================== 私有方法 ====================
    
    private fun loadBitmap(uri: String): Bitmap? {
        return try {
            if (uri.startsWith("http://") || uri.startsWith("https://")) {
                loadBitmapFromUrl(uri)
            } else {
                BitmapFactory.decodeFile(uri)
            }
        } catch (e: Exception) {
            Log.e(TAG, "加载图片失败: $uri", e)
            null
        }
    }
    
    private fun loadBitmapFromUrl(url: String): Bitmap? {
        return try {
            val connection = URL(url).openConnection()
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.getInputStream().use { input ->
                BitmapFactory.decodeStream(input)
            }
        } catch (e: Exception) {
            Log.e(TAG, "从 URL 加载图片失败: $url", e)
            null
        }
    }
    
    private fun saveBitmapToGallery(bitmap: Bitmap): Uri? {
        val filename = "meal_${System.currentTimeMillis()}.jpg"
        
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/NutritionApp")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: return null
        
        try {
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
            
            return uri
        } catch (e: Exception) {
            Log.e(TAG, "保存到相册失败", e)
            resolver.delete(uri, null, null)
            return null
        }
    }
    
    private fun saveBitmapToLocalStorage(bitmap: Bitmap): String {
        val photoDir = File(context.filesDir, PHOTO_DIR)
        if (!photoDir.exists()) {
            photoDir.mkdirs()
        }
        
        val filename = "downloaded_${System.currentTimeMillis()}.jpg"
        val file = File(photoDir, filename)
        
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        
        return file.absolutePath
    }
}

/**
 * 照片来源
 */
sealed class PhotoSource {
    data class Local(val path: String) : PhotoSource()
    data class Remote(val url: String) : PhotoSource()
}
