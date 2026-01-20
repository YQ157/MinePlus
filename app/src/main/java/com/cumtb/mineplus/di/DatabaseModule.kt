package com.cumtb.mineplus.di

import android.content.Context
import androidx.room.Room
import com.cumtb.mineplus.data.database.AppDatabase
import com.cumtb.mineplus.data.database.CourseDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // 安装在单例组件中，全 App 生命周期有效
object DatabaseModule {

    // 1. 告诉 Hilt 如何创建数据库实例
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "mine_plus.db" // 数据库名称
        )
            .fallbackToDestructiveMigration() // 开发阶段很有用：改了表结构直接重建，防止崩毁
            .build()
    }

    // 2. 告诉 Hilt 如何获取 CourseDao (这就是报错缺失的部分！)
    @Provides
    fun provideCourseDao(database: AppDatabase): CourseDao {
        return database.courseDao()
    }
}