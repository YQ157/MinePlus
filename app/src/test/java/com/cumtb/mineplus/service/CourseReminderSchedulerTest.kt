package com.cumtb.mineplus.service

import com.cumtb.mineplus.data.model.CourseSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 课前提醒调度器测试
 */
class CourseReminderSchedulerTest {

    private data class Interval(val start: LocalDateTime, val end: LocalDateTime)

    /**
     * 纯算法（业务规则版）：默认提醒时间 t0，若落在任一课堂区间内，则顺延到该区间 end。
     * 仅顺延一次（不会链式冲突），且按业务约束不会顺延到上课开始。
     */
    private fun resolveReminderTime(
        defaultReminderTime: LocalDateTime,
        courseStart: LocalDateTime,
        intervals: List<Interval>
    ): LocalDateTime? {
        val sorted = intervals.sortedBy { it.start }
        val conflict = sorted.firstOrNull { defaultReminderTime >= it.start && defaultReminderTime < it.end }
            ?: return defaultReminderTime

        // 按业务约束：不会顺延到上课开始；这里仅做安全兜底
        val shifted = conflict.end
        return shifted.takeIf { it.isBefore(courseStart) }
    }

    @Test
    fun testReminderTimeCalculation_normalCase() {
        // 测试正常情况：课前15分钟提醒
        val course = createTestCourse(
            date = "2024-03-01",
            startTime = "10:00",
            endTime = "11:30"
        )

        val now = LocalDateTime.of(2024, 3, 1, 9, 30) // 9:30
        val expectedReminderTime = LocalDateTime.of(2024, 3, 1, 9, 45) // 9:45提醒

        // 这里应该验证计算逻辑...
        // 由于涉及数据库和上下文依赖，实际测试需要mock环境
    }

    @Test
    fun testReminderTimeCalculation_conflictCase() {
        // 测试冲突情况：提醒时间被其他课程占用时的处理
        // 需要mock数据库和课程数据来完整测试
    }

    @Test
    fun testReminderTimeCalculation_pastCourse() {
        // 测试过去课程不应该设置提醒
        val pastCourse = createTestCourse(
            date = "2024-03-01",
            startTime = "10:00",
            endTime = "11:30"
        )

        val now = LocalDateTime.of(2024, 3, 1, 12, 0) // 12:00，课程已结束

        // 过去的课程应该返回null
        // assertNull(calculatedTime)
    }

    private fun createTestCourse(
        date: String,
        startTime: String,
        endTime: String
    ): CourseSchedule {
        return CourseSchedule(
            id = 1,
            dayOfWeek = 5, // 周五
            startNode = 3,
            step = 4,
            room = "教学楼A101",
            rawStartTime = startTime,
            rawEndTime = endTime,
            date = date,
            courseName = "测试课程",
            teacher = "测试教师",
            credit = 2.0f,
            rawScheduleText = null,
            weightedCalc = null,
            colorIndex = 0
        )
    }

    @Test
    fun reminderTime_noConflict_returnsDefault() {
        val date = LocalDate.of(2024, 3, 1)
        val courseStart = LocalDateTime.of(date, LocalTime.of(10, 0))
        val defaultReminder = courseStart.minusMinutes(15)

        val result = resolveReminderTime(
            defaultReminderTime = defaultReminder,
            courseStart = courseStart,
            intervals = listOf(
                Interval(
                    start = LocalDateTime.of(date, LocalTime.of(8, 0)),
                    end = LocalDateTime.of(date, LocalTime.of(8, 50))
                )
            )
        )

        assertEquals(defaultReminder, result)
    }

    @Test
    fun reminderTime_singleConflict_pushToConflictEnd() {
        val date = LocalDate.of(2024, 3, 1)
        val courseStart = LocalDateTime.of(date, LocalTime.of(10, 0))
        val defaultReminder = courseStart.minusMinutes(15) // 09:45

        // 冲突课 09:00-10:00 覆盖 09:45
        val result = resolveReminderTime(
            defaultReminderTime = defaultReminder,
            courseStart = courseStart,
            intervals = listOf(
                Interval(
                    start = LocalDateTime.of(date, LocalTime.of(9, 0)),
                    end = LocalDateTime.of(date, LocalTime.of(9, 50))
                )
            )
        )

        // 推迟到 09:50，此时仍未到上课时间
        assertEquals(LocalDateTime.of(date, LocalTime.of(9, 50)), result)
    }

    @Test
    fun reminderTime_singleConflict_pushToConflictEnd_and_noFurtherChaining() {
        val date = LocalDate.of(2024, 3, 1)
        val courseStart = LocalDateTime.of(date, LocalTime.of(10, 0))
        val defaultReminder = courseStart.minusMinutes(15) // 09:45

        val result = resolveReminderTime(
            defaultReminderTime = defaultReminder,
            courseStart = courseStart,
            intervals = listOf(
                Interval(
                    start = LocalDateTime.of(date, LocalTime.of(9, 0)),
                    end = LocalDateTime.of(date, LocalTime.of(9, 50))
                ),
                // 即使紧接着还有一门课覆盖 09:50，也不会链式顺延
                Interval(
                    start = LocalDateTime.of(date, LocalTime.of(9, 50)),
                    end = LocalDateTime.of(date, LocalTime.of(10, 10))
                )
            )
        )

        assertEquals(LocalDateTime.of(date, LocalTime.of(9, 50)), result)
    }
}