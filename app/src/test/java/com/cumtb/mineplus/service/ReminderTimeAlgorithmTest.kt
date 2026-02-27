package com.cumtb.mineplus.service

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 额外用例：链式冲突但最终还能落在上课前空档（应返回顺延后的时间）。
 */
class ReminderTimeAlgorithmTest {

    private data class Interval(val start: LocalDateTime, val end: LocalDateTime)

    private fun resolve(defaultReminderTime: LocalDateTime, courseStart: LocalDateTime, intervals: List<Interval>): LocalDateTime? {
        var t = defaultReminderTime
        val sorted = intervals.sortedBy { it.start }
        while (true) {
            val conflict = sorted.firstOrNull { t >= it.start && t < it.end } ?: break
            t = conflict.end
            if (!t.isBefore(courseStart)) return null
        }
        return t
    }

    @Test
    fun chainConflict_butStillBeforeStart_returnsShiftedTime() {
        val date = LocalDate.of(2024, 3, 1)
        val courseStart = LocalDateTime.of(date, LocalTime.of(14, 0))
        val defaultReminder = courseStart.minusMinutes(15) // 13:45

        val result = resolve(
            defaultReminderTime = defaultReminder,
            courseStart = courseStart,
            intervals = listOf(
                Interval(LocalDateTime.of(date, LocalTime.of(13, 30)), LocalDateTime.of(date, LocalTime.of(13, 50)))
            )
        )

        assertEquals(LocalDateTime.of(date, LocalTime.of(13, 50)), result)
    }
}
