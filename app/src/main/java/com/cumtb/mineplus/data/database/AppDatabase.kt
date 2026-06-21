package com.cumtb.mineplus.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * 🗄️ 数据库核心类
 * 定义了包含哪些表 (entities) 和版本号 (version)
 */
@Database(
    entities = [CourseEntity::class, ScheduleEntity::class, GradeEntity::class],
    version = 4,
    exportSchema = false // 不导出 Schema 文件，避免编译警告
)
abstract class AppDatabase : RoomDatabase() {

    // 暴露 DAO，让外界（Hilt 里的 DatabaseModule）可以通过 database.courseDao() 拿到它
    abstract fun courseDao(): CourseDao

    abstract fun gradeDao(): GradeDao
}
