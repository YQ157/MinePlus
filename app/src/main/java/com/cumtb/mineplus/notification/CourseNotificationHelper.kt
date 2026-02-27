package com.cumtb.mineplus.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cumtb.mineplus.MainActivity
import com.cumtb.mineplus.R

/**
 * 课程通知帮助类
 * 负责创建和显示课程相关通知
 */
class CourseNotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID_COURSE_REMINDER = "course_reminder_channel"
        private const val CHANNEL_NAME_COURSE_REMINDER = "课前提醒"
        private const val CHANNEL_DESCRIPTION_COURSE_REMINDER = "课程开始前提醒通知"

        private const val NOTIFICATION_GROUP_COURSE = "course_reminders"
    }

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createNotificationChannels()
    }

    /**
     * 创建通知渠道（Android 8.0+）
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_COURSE_REMINDER,
                CHANNEL_NAME_COURSE_REMINDER,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION_COURSE_REMINDER
                // 需求：不要声音不要振动，但要“浮窗”（heads-up）。
                // heads-up 主要由 IMPORTANCE_HIGH 决定；声音/振动由 channel 决定。
                setSound(null, null)
                enableVibration(false)
                vibrationPattern = null
                enableLights(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 显示课程提醒通知
     */
    fun showCourseReminder(
        courseId: Int,
        title: String,
        content: String
    ) {
        try {
            // 创建点击通知时的Intent
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                // 可以添加额外参数来导航到特定页面
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                courseId, // 使用courseId作为requestCode确保唯一性
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 构建通知
            val builder = NotificationCompat.Builder(context, CHANNEL_ID_COURSE_REMINDER)
                .setSmallIcon(R.drawable.ic_notification) // 需要添加通知图标
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setGroup(NOTIFICATION_GROUP_COURSE)
                // 需求：通知静音（channel 已静音；这里再明确一次，避免旧机型/兼容层差异）
                .setSilent(true)

            // 不使用 DEFAULT_ALL/DEFAULT_VIBRATE，避免声音与震动
            // endReminder 与否对“静音”无差别保留参数仅用于内容/标题区分

            // 显示通知
            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                notificationManager.notify(courseId, builder.build())
            } else {
                // 通知权限未开启
            }

        } catch (e: Exception) {
            // best-effort：不要让通知异常影响主流程
        }
    }

    /**
     * 显示课程取消通知
     */
    fun showCourseCancelled(
        courseId: Int,
        courseName: String,
        teacherName: String
    ) {
        try {
            val title = "课程已取消"
            val content = "$courseName ($teacherName) 已取消"

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                courseId + 10000, // 避免与提醒通知冲突
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID_COURSE_REMINDER)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setGroup(NOTIFICATION_GROUP_COURSE)

            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                notificationManager.notify(courseId + 10000, builder.build())
            }

        } catch (e: Exception) {
        }
    }

    /**
     * 取消特定课程的通知
     */
    fun cancelCourseNotification(courseId: Int) {
        try {
            notificationManager.cancel(courseId)
            notificationManager.cancel(courseId + 10000) // 同时取消可能的取消通知
        } catch (e: Exception) {
        }
    }

    /**
     * 取消所有课程相关通知
     */
    fun cancelAllCourseNotifications() {
        try {
            // Android 8.0以下不支持通知组，需要逐个取消
            // 这里简单实现，实际应用中可能需要维护已发送通知的ID列表
        } catch (e: Exception) {
        }
    }
}