package com.cumtb.mineplus.data.model

import com.google.gson.annotations.SerializedName

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

    /**
     * 课时类型标记：
     * - theory=true：计入加权
     * - practice=true：不计入加权
     * 按约定二者不应同时为 true；若后端缺失字段则为 null。
     *
     * ⚠️ 说明：根据不同学校/接口版本，这两个字段可能出现在 lesson 顶层或 course 下。
     * 这里先在 lesson 顶层保留一份，保证解析不丢；同时 CourseInfo 里也保留同名字段。
     */
    @SerializedName("theory")
    val theory: Boolean? = null,
    @SerializedName("practice")
    val practice: Boolean? = null,

    val course: CourseInfo?,
    val teacherAssignmentList: List<TeacherAssignment>?,
    // 原始文本，虽然我们不用它来排课，但存起来备查也好
    val scheduleText: ScheduleTextJson?
)

data class CourseInfo(
    val nameZh: String?, // 课程名
    val credits: Float?,  // 学分

    // get-data 示例里 theory/practice 在 course 对象里
    @SerializedName("theory")
    val theory: Boolean? = null,
    @SerializedName("practice")
    val practice: Boolean? = null,
)

/**
 * 把 theory/practice 映射成 UI 友好的词。
 * 优先使用 course 下的字段（如果有），没有则回退 lesson 顶层字段。
 */
fun LessonJson.weightedCalcLabel(): String {
    val t = this.course?.theory ?: this.theory
    val p = this.course?.practice ?: this.practice

    return when {
        t == true && p != true -> "理论"
        p == true && t != true -> "实践"
        t == true && p == true -> "理论+实践" // 异常兜底：理论/实践同时为 true
        else -> "未知"
    }
}

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