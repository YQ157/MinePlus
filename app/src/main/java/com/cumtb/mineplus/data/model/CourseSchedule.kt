package com.cumtb.mineplus.data.model

/**
 * 🎨 UI 专用聚合模型
 * 包含了 Schedule 的时间信息 + Course 的名称/颜色信息
 */
data class CourseSchedule(
    // --- 来自 schedules 表 ---
    val id: Int,           // 日程的唯一ID (用于 LazyColumn 的 key)
    val dayOfWeek: Int,    // 周几 (1-7)
    val startNode: Int,    // 第几节开始 (1-12)
    val step: Int,         // 持续几节
    val room: String,      // 教室
    val rawStartTime: String, // "08:00"
    val rawEndTime: String,   // "11:30"
    val date: String,         // "2025-xx-xx"

    // --- 来自 courses 表 ---
    val courseName: String, // 课程名 (在 SQL 里起别名映射过来)
    val teacher: String,
    val colorIndex: Int     // 0-14 的颜色索引
)