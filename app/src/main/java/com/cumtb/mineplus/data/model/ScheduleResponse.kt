package com.cumtb.mineplus.data.model

// 对应 datum 接口的根对象
// 根据你提供的 JSON，它是一个对象，里面包含 scheduleList 和 lessonList
data class ScheduleResponse(
    val result: ScheduleData?
)

data class ScheduleData(
    val scheduleList: List<ScheduleItemJson>?,
    val lessonList: List<LessonJson>?
)

// 每一条具体的排课记录 (原子化数据)
data class ScheduleItemJson(
    val lessonId: Long,
    val date: String?,      // "2025-05-29"
    val weekday: Int,       // 4 (周四)
    val weekIndex: Int,     // 15 (第15周)
    val startTime: Int,     // 800 (8:00)
    val endTime: Int,       // 1130 (11:30)
    val periods: Int,       // 4 (持续4节)
    val room: RoomJson?     // 教室信息
)

data class RoomJson(
    val nameZh: String?     // "教106"
)