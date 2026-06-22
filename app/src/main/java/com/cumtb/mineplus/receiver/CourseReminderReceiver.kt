package com.cumtb.mineplus.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.cumtb.mineplus.notification.CourseNotificationHelper

/**
 * 课程提醒广播接收器
 * 接收闹钟触发的提醒并显示通知
 */
class CourseReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_COURSE_REMINDER = "com.cumtb.mineplus.COURSE_REMINDER"
        const val EXTRA_COURSE_ID = "course_id"
        const val EXTRA_COURSE_NAME = "course_name"
        const val EXTRA_TEACHER_NAME = "teacher_name"
        const val EXTRA_ROOM = "room"
        const val EXTRA_START_TIME = "start_time"
        const val EXTRA_END_TIME = "end_time"
        const val EXTRA_MINUTES_UNTIL_START = "minutes_until_start"

        private const val TAG = "CourseReminderReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            Log.d(TAG, "收到课程提醒广播，Action: ${intent.action}")

            if (intent.action != ACTION_COURSE_REMINDER) {
                Log.w(TAG, "收到未知动作的广播: ${intent.action}")
                return
            }

            // 提取课程信息
            val courseId = intent.getIntExtra(EXTRA_COURSE_ID, -1)
            val courseName = intent.getStringExtra(EXTRA_COURSE_NAME) ?: "未知课程"
            val teacherName = intent.getStringExtra(EXTRA_TEACHER_NAME) ?: "未知教师"
            val room = intent.getStringExtra(EXTRA_ROOM) ?: "未知教室"
            val startTime = intent.getStringExtra(EXTRA_START_TIME) ?: ""
            val endTime = intent.getStringExtra(EXTRA_END_TIME) ?: ""
            val minutesUntilStart = intent.getIntExtra(EXTRA_MINUTES_UNTIL_START, 0)

            if (courseId == -1) {
                Log.w(TAG, "无效的课程ID")
                return
            }

            Log.d(TAG, "处理课程提醒 - ID: $courseId, 课程: $courseName, 教师: $teacherName, 地点: $room, 时间: $startTime-$endTime")

            // 显示通知
            showCourseNotification(
                context = context,
                courseId = courseId,
                courseName = courseName,
                teacherName = teacherName,
                room = room,
                startTime = startTime,
                endTime = endTime,
                minutesUntilStart = minutesUntilStart
            )

        } catch (e: Exception) {
            Log.e(TAG, "处理课程提醒失败", e)
        }
    }

    private fun showCourseNotification(
        context: Context,
        courseId: Int,
        courseName: String,
        teacherName: String,
        room: String,
        startTime: String,
        endTime: String,
        minutesUntilStart: Int
    ) {
        try {
            val notificationHelper = CourseNotificationHelper(context)

            // 标题只放课程名，避免教室挤占标题展示空间。
            val title = courseName

            // 内容：第一行教室；第二行时间和教师。
            val content = buildString {
                append("教室: $room\n")
                append("时间: $startTime-$endTime  教师: $teacherName")
            }

            notificationHelper.showCourseReminder(
                courseId = courseId,
                title = title,
                content = content
            )

            Log.d(TAG, "已显示课程提醒通知: $courseName")

        } catch (e: Exception) {
            Log.e(TAG, "显示通知失败", e)
        }
    }
}
