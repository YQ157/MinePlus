package com.cumtb.mineplus.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "schedules",
    // 外键关联：如果 Course 被删了，对应的 Schedule 也会自动删除
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["lessonId"],
            childColumns = ["lessonId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    // 建立索引，加速查询 (我们经常按“周”查询)
    indices = [Index(value = ["weekIndex"]), Index(value = ["lessonId"])]
)
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val lessonId: Long, // 外键，指向 CourseEntity

    // --- 定位信息 ---
    val weekIndex: Int, // 第几周 (1-20)
    val dayOfWeek: Int, // 星期几 (1-7)

    // --- 🕒 核心：经过 TimeMapper 映射后的逻辑节次 ---
    val startNode: Int, // 例如 1 (代表第1节)
    val step: Int,      // 例如 4 (占4个格子)

    // --- 展示信息 ---
    val room: String,
    val rawStartTime: String, // "08:00" (用于卡片上显示)
    val rawEndTime: String,   // "11:30"
    val date: String          // "2025-05-29"
)