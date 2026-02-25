package com.cumtb.mineplus.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey val lessonId: Long, // 直接用学校的 ID 作为主键

    val name: String,
    val teacher: String,
    val credit: Float,

    /**
     * 是否计入加权：
     * - true: 计入加权（theory）
     * - false: 不计入加权（practice）
     * - null: 后端未提供/无法判断
     */
    val weightedCalc: Boolean? = null,

    // 🎨 核心：只存颜色编号 (0~14)，具体颜色由 UI 根据主题决定
    val colorIndex: Int,

    // 原始信息备份 (可选)
    val rawScheduleText: String?
)