package com.cumtb.mineplus.util

import java.time.Duration
import java.time.LocalTime

/**
 * Course time status used by the today schedule UI.
 */
enum class CourseTimeStatus {
    /** Course has not started yet. */
    UPCOMING,

    /** Course is the nearest upcoming class within the reminder window. */
    STARTING_SOON,

    /** Current time is within the class time range. */
    IN_PROGRESS,

    /** Course has already ended. */
    FINISHED,

    /** Course time could not be parsed or is invalid. */
    UNKNOWN
}

/** 单节课的基础状态（不包含“全局无进行中才显示即将开始”的规则） */
fun computeBaseCourseTimeStatus(
    now: LocalTime,
    start: String,
    end: String
): CourseTimeStatus {
    val startTime = start.toLocalTimeOrNull() ?: return CourseTimeStatus.UNKNOWN
    val endTime = end.toLocalTimeOrNull() ?: return CourseTimeStatus.UNKNOWN
    if (endTime <= startTime) return CourseTimeStatus.UNKNOWN

    return when {
        now < startTime -> CourseTimeStatus.UPCOMING
        now >= startTime && now < endTime -> CourseTimeStatus.IN_PROGRESS
        else -> CourseTimeStatus.FINISHED
    }
}

/**
 * 根据你的新定义：
 * - “进行中”：now 在 [start, end)
 * - “即将开始”：当且仅当「没有任何进行中的课程」时，且是“未来 30 分钟以内最近将要开始的那节课”
 */
fun computeTodayCourseTimeStatuses(
    now: LocalTime,
    courses: List<Pair<String, String>>, // (start, end)
    startingSoonWithin: Duration = Duration.ofMinutes(30)
): List<CourseTimeStatus> {
    val base = courses.map { (s, e) -> computeBaseCourseTimeStatus(now, s, e) }

    // If anything is in progress, we don't mark starting soon.
    if (base.any { it == CourseTimeStatus.IN_PROGRESS }) return base

    // Find the nearest upcoming course within threshold.
    var bestIndex: Int? = null
    var bestDelta: Duration? = null

    courses.forEachIndexed { index, (s, e) ->
        if (base[index] != CourseTimeStatus.UPCOMING) return@forEachIndexed
        val startTime = s.toLocalTimeOrNull() ?: return@forEachIndexed
        val delta = Duration.between(now, startTime)
        if (delta.isNegative || delta.isZero) return@forEachIndexed
        if (delta > startingSoonWithin) return@forEachIndexed

        if (bestDelta == null || delta < bestDelta) {
            bestDelta = delta
            bestIndex = index
        }
    }

    if (bestIndex == null) return base

    return base.mapIndexed { idx, st ->
        if (idx == bestIndex) CourseTimeStatus.STARTING_SOON else st
    }
}

/** Backward-compatible helper for single course; keeps the old name used by UI. */
fun computeCourseTimeStatus(
    now: LocalTime,
    start: String,
    end: String,
    startingSoonWithin: Duration = Duration.ofMinutes(30)
): CourseTimeStatus {
    // Single-course context can't know if there is another in-progress course,
    // so we treat it as base logic + within-threshold upcoming -> STARTING_SOON.
    val base = computeBaseCourseTimeStatus(now, start, end)
    if (base != CourseTimeStatus.UPCOMING) return base

    val startTime = start.toLocalTimeOrNull() ?: return CourseTimeStatus.UNKNOWN
    val delta = Duration.between(now, startTime)
    return if (!delta.isNegative && !delta.isZero && delta <= startingSoonWithin) {
        CourseTimeStatus.STARTING_SOON
    } else {
        CourseTimeStatus.UPCOMING
    }
}

private fun String.toLocalTimeOrNull(): LocalTime? {
    return try {
        LocalTime.parse(this)
    } catch (_: Exception) {
        null
    }
}
