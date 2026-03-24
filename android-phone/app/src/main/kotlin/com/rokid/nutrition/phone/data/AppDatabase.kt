package com.rokid.nutrition.phone.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import com.rokid.nutrition.phone.data.dao.*
import com.rokid.nutrition.phone.data.entity.*

/**
 * 应用数据库
 */
@Database(
    entities = [
        UserProfileEntity::class,
        MealSessionEntity::class,
        MealSnapshotEntity::class,
        SnapshotFoodEntity::class,
        SyncQueueEntity::class,
        WeightEntryEntity::class
    ],
    version = 6,  // 添加增强版用户档案字段和体重记录表
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun userProfileDao(): UserProfileDao
    abstract fun mealSessionDao(): MealSessionDao
    abstract fun mealSnapshotDao(): MealSnapshotDao
    abstract fun snapshotFoodDao(): SnapshotFoodDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun weightEntryDao(): WeightEntryDao
    
    companion object {
        private const val DATABASE_NAME = "nutrition_phone.db"
        
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration()  // 开发阶段使用，生产环境应使用迁移
                .build().also { INSTANCE = it }
            }
        }
    }
}


