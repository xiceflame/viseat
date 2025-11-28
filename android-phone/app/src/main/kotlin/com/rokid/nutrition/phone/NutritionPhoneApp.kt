package com.rokid.nutrition.phone

import android.app.Application
import android.util.Log
import com.rokid.nutrition.phone.data.AppDatabase

/**
 * 应用入口类
 * 
 * 负责初始化全局依赖
 */
class NutritionPhoneApp : Application() {
    
    companion object {
        private const val TAG = "NutritionPhoneApp"
        
        lateinit var instance: NutritionPhoneApp
            private set
    }
    
    // 懒加载数据库实例
    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        Log.d(TAG, "应用启动: ${Config.APP_NAME} v${Config.APP_VERSION}")
    }
}
