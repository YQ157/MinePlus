package com.cumtb.mineplus.data.model

// 对应 get-data 接口
data class CourseResponse(
    val lessons: List<LessonJson>?,     // 课程详情列表
    // schedules 字段我们不需要了，因为我们决定用 datum 接口
    val lessonId2Flag: Map<String, String>?, // 可能用于判断是否不需要排课
    val currentWeek: Int?,      // 当前是第几周 (例如 48)
    val weekIndices: List<Int>? // 周次列表 (例如 [1, 2, ... 20])
)

data class LessonJson(
    val id: String, // lessonId
    val code: String?,
    val course: CourseInfo?,
    val teacherAssignmentList: List<TeacherAssignment>?,
    // 原始文本，虽然我们不用它来排课，但存起来备查也好
    val scheduleText: ScheduleTextJson?
)

data class CourseInfo(
    val nameZh: String?, // 课程名
    val credits: Float?  // 学分
)

data class TeacherAssignment(
    val person: PersonInfo?
)

data class PersonInfo(
    val nameZh: String?
)

data class ScheduleTextJson(
    val dateTimePlacePersonText: TextDetail?
)

data class TextDetail(
    val textZh: String?
)