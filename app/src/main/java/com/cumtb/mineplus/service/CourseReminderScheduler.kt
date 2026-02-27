package com.cumtb.mineplus.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.cumtb.mineplus.data.database.CourseDao
import com.cumtb.mineplus.data.model.CourseSchedule
import com.cumtb.mineplus.receiver.CourseReminderReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 课前提醒调度器
 * 负责计算提醒时间、设置闹钟、管理提醒任务
 */
@Singleton
class CourseReminderScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val courseDao: CourseDao,
    private val alarmStore: ReminderAlarmStore
) {

    companion object {
        private const val TAG = "CourseReminderScheduler"
        private const val REMINDER_MINUTES_BEFORE = 15L // 课前15分钟提醒

        // 仅保留课前提醒
        private const val ALARM_ID_PREFIX_CLASS = "class_" // 正常课前提醒

        private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * 为接下来3天的所有课程设置提醒
     * 每次APP启动时调用
     */
    suspend fun scheduleRemindersForNextThreeDays() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始为接下来3天设置课前提醒...")
            Log.d(TAG, "当前时间: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}")

            val today = LocalDate.now()
            val endDate = today.plusDays(2) // 包含今天在内共3天

            // 获取近3天内所有周次
            val weekIndices = getWeekIndicesForDateRange(today, endDate)
            if (weekIndices.isEmpty()) {
                Log.w(TAG, "未找到有效的周次数据，日期范围: $today 到 $endDate")
                return@withContext
            }

            // 取出未来 3 天内相关课程列表
            val allCourses = mutableListOf<CourseSchedule>()
            weekIndices.forEach { weekIndex ->
                try {
                    val weekCourses = courseDao.getSchedulesByWeek(weekIndex)
                        .firstOrNull()
                        .orEmpty()
                    allCourses.addAll(weekCourses)
                } catch (e: Exception) {
                    Log.w(TAG, "获取第${weekIndex}周课程失败", e)
                }
            }

            // 过滤出日期范围内的课程
            val filteredCourses = allCourses.filter { course ->
                try {
                    val courseDate = LocalDate.parse(course.date, DATE_FMT)
                    courseDate >= today && courseDate <= endDate
                } catch (_: Exception) {
                    false
                }
            }

            // 目标集合：本次应该存在的 alarmId
            val targetAlarmIds = filteredCourses
                .map { buildAlarmId(course = it) }
                .toSet()

            // 已存在集合：上次调度写入的 alarmId
            val existingAlarmIds = alarmStore.alarmIds.first()

            // 1) 取消已存在但本次不需要的闹钟（课程被删/日期滑出窗口）
            val toCancel = existingAlarmIds - targetAlarmIds
            toCancel.forEach { alarmId ->
                cancelAlarmById(alarmId)
            }

            // 2) 为目标集合重设闹钟（即使已存在也覆盖，确保时间正确）
            filteredCourses.forEach { course ->
                scheduleReminderForCourse(course)
            }

            // 3) 持久化写入目标集合
            alarmStore.setAlarmIds(targetAlarmIds)

            Log.d(TAG, "课前提醒调度完成：target=${targetAlarmIds.size}, cancel=${toCancel.size}")

        } catch (e: Exception) {
            Log.e(TAG, "设置课前提醒失败", e)
        }
    }

    /**
     * 取消所有课前提醒：以持久化列表为准逐个取消，并清空存储。
     */
    suspend fun cancelAllReminders() = withContext(Dispatchers.IO) {
        try {
            val existing = alarmStore.alarmIds.first()
            existing.forEach { alarmId ->
                cancelAlarmById(alarmId)
            }
            alarmStore.clear()
            Log.d(TAG, "已取消并清空所有课前提醒: ${existing.size}")
        } catch (e: Exception) {
            Log.w(TAG, "取消课前提醒失败（best-effort）", e)
        }
    }

    /**
     * 为单门课程设置提醒
     */
    private suspend fun scheduleReminderForCourse(course: CourseSchedule) {
        try {
            val courseDate = LocalDate.parse(course.date, DATE_FMT)
            val courseStartTime = LocalTime.parse(course.rawStartTime, TIME_FMT)
            val courseEndTime = LocalTime.parse(course.rawEndTime, TIME_FMT)

            val courseStartDateTime = LocalDateTime.of(courseDate, courseStartTime)
            val courseEndDateTime = LocalDateTime.of(courseDate, courseEndTime)

            // 计算提醒时间
            val reminderTime = calculateReminderTime(course)

            if (reminderTime == null) {
                Log.d(TAG, "课程 ${course.courseName} 无需设置提醒（时间已过或被挤压到上课后）")
                return
            }

            // 设置闹钟
            setAlarmForCourse(course, reminderTime, courseStartDateTime, courseEndDateTime)

            Log.d(TAG, "已为课程 ${course.courseName} 设置提醒: ${reminderTime.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))}")

        } catch (e: Exception) {
            Log.e(TAG, "为课程 ${course.courseName} 设置提醒失败", e)
        }
    }

    /**
     * 计算课程的提醒时间
     * 规则：默认课前15分钟；如果该时间点处于其他课程上课区间内，则顺延到该冲突课程的下课时刻。
     * 说明：按业务约束，这里只会顺延一次，且不会顺延到目标课的上课开始。
     */
    private suspend fun calculateReminderTime(course: CourseSchedule): LocalDateTime? {
        val courseDate = LocalDate.parse(course.date, DATE_FMT)
        val courseStartTime = LocalTime.parse(course.rawStartTime, TIME_FMT)
        val courseStartDateTime = LocalDateTime.of(courseDate, courseStartTime)

        val now = LocalDateTime.now()
        if (now >= courseStartDateTime) return null

        val defaultReminderTime = courseStartDateTime.minusMinutes(REMINDER_MINUTES_BEFORE)

        val dayCourses = getCoursesForDate(courseDate)
            .filter { it.id != course.id }

        // 只检查一次冲突：默认提醒时刻是否落在任一课程进行区间内
        val conflict = dayCourses.firstOrNull { c ->
            try {
                val s = LocalDateTime.of(courseDate, LocalTime.parse(c.rawStartTime, TIME_FMT))
                val e = LocalDateTime.of(courseDate, LocalTime.parse(c.rawEndTime, TIME_FMT))
                defaultReminderTime >= s && defaultReminderTime < e
            } catch (_: Exception) {
                false
            }
        }

        return if (conflict != null) {
            // 顺延到冲突课下课时刻
            val end = LocalTime.parse(conflict.rawEndTime, TIME_FMT)
            LocalDateTime.of(courseDate, end)
        } else {
            defaultReminderTime
        }
    }

    private suspend fun getCoursesForDate(date: LocalDate): List<CourseSchedule> {
        val weekIndex = getWeekIndexForDate(date) ?: return emptyList()
        val weekday = date.dayOfWeek.value

        var courses: List<CourseSchedule> = emptyList()
        try {
            val list: List<CourseSchedule> = courseDao.getSchedulesByWeek(weekIndex)
                .firstOrNull()
                .orEmpty()
            courses = list.filter { it.dayOfWeek == weekday && it.date == date.format(DATE_FMT) }
        } catch (e: Exception) {
            Log.w(TAG, "获取第${weekIndex}周 星期${weekday} 课程失败", e)
        }

        return courses
    }

    /**
     * 设置闹钟
     */
    private fun setAlarmForCourse(
        course: CourseSchedule,
        reminderTime: LocalDateTime,
        courseStartTime: LocalDateTime,
        courseEndTime: LocalDateTime
    ) {
        val now = LocalDateTime.now()
        if (reminderTime <= now) {
            Log.d(TAG, "提醒时间已过，跳过设置: ${course.courseName}")
            return
        }

        val alarmId = buildAlarmId(course = course)
        val requestCode = alarmId.hashCode()

        val intent = Intent(context, CourseReminderReceiver::class.java).apply {
            action = CourseReminderReceiver.ACTION_COURSE_REMINDER
            putExtra(CourseReminderReceiver.EXTRA_COURSE_ID, course.id)
            putExtra(CourseReminderReceiver.EXTRA_COURSE_NAME, course.courseName)
            putExtra(CourseReminderReceiver.EXTRA_TEACHER_NAME, course.teacher)
            putExtra(CourseReminderReceiver.EXTRA_ROOM, course.room)
            putExtra(CourseReminderReceiver.EXTRA_START_TIME, course.rawStartTime)
            putExtra(CourseReminderReceiver.EXTRA_END_TIME, course.rawEndTime)

            val minutesUntilStart = java.time.Duration.between(now, courseStartTime).toMinutes()
            putExtra(CourseReminderReceiver.EXTRA_MINUTES_UNTIL_START, minutesUntilStart.toInt())
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTimeMillis = reminderTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+：根据系统能力决定是否能用精确闹钟
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                } else {
                    // 降级：仍尽量允许 idle，但不保证精确
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            }

            Log.d(TAG, "闹钟设置成功: $alarmId at ${reminderTime.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))}")

        } catch (e: SecurityException) {
            Log.e(TAG, "设置闹钟失败（权限/系统限制），降级到普通闹钟: $alarmId", e)
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )
        }
    }

    private fun buildAlarmId(course: CourseSchedule): String {
        return "${ALARM_ID_PREFIX_CLASS}${course.id}_${course.date}"
    }

    private fun cancelAlarmById(alarmId: String) {
        try {
            val requestCode = alarmId.hashCode()
            val intent = Intent(context, CourseReminderReceiver::class.java).apply {
                action = CourseReminderReceiver.ACTION_COURSE_REMINDER
            }
            val pi = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pi)
        } catch (e: Exception) {
            Log.w(TAG, "取消闹钟失败: $alarmId", e)
        }
    }

    /**
     * 根据日期范围获取周次列表
     */
    private suspend fun getWeekIndicesForDateRange(startDate: LocalDate, endDate: LocalDate): Set<Int> {
        val weekIndices = mutableSetOf<Int>()

        var currentDate = startDate
        while (currentDate <= endDate) {
            val weekIndex = getWeekIndexForDate(currentDate)
            if (weekIndex != null) {
                weekIndices.add(weekIndex)
            }
            currentDate = currentDate.plusDays(1)
        }

        return weekIndices
    }

    /**
     * 根据日期获取周次
     */
    private suspend fun getWeekIndexForDate(date: LocalDate): Int? {
        return try {
            courseDao.getUpcomingWeekIndex(date.toString())
        } catch (e: Exception) {
            Log.w(TAG, "获取日期 $date 的周次失败", e)
            null
        }
    }
}