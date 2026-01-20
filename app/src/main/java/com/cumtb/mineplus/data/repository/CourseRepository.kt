package com.cumtb.mineplus.data.repository

import android.util.Log
import com.cumtb.mineplus.data.api.SchoolApi
import com.cumtb.mineplus.data.database.CourseDao
import com.cumtb.mineplus.data.database.CourseEntity
import com.cumtb.mineplus.data.database.ScheduleEntity
import com.cumtb.mineplus.data.model.DatumRequest
import com.cumtb.mineplus.data.preference.AppPreferences
import com.cumtb.mineplus.data.scraper.HtmlParser
import com.cumtb.mineplus.util.TimeMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CourseRepository @Inject constructor(
    private val api: SchoolApi,
    private val courseDao: CourseDao,
    private val prefs: AppPreferences,
) {

    /**
     * 🚀 核心方法：刷新所有课表数据
     * 这个方法会执行完整的 ETL (Extract, Transform, Load) 流程
     */
    suspend fun refreshAllData(onLoginSuccess: () -> Unit) = withContext(Dispatchers.IO) {
        try {
            Log.d("MinePlus", "1️⃣ 开始获取 HTML 以解析 ID...")
            val htmlBody = api.getCoursePageHtml().string()
            val semesterId = HtmlParser.parseSemesterId(htmlBody)
            val personId = HtmlParser.parseStdPersonId(htmlBody) // 👈 解析 personId

            Log.d("MinePlus", "   解析结果: semesterId=$semesterId, personId=$personId")

            if (personId == -1L) throw Exception("无法获取用户ID (stdPersonId)")

            // --- 阶段 A: 获取课程清单 (get-data) ---
            Log.d("MinePlus", "2️⃣ 请求课程清单 (get-data)...")
            val courseRes = api.getScheduleData(281)

            // 🔥🔥🔥 新增：计算并保存学期时间信息 🔥🔥🔥
            val currentWeek = courseRes.currentWeek ?: 1
            val maxWeek = courseRes.weekIndices?.maxOrNull() ?: 20

            // 核心算法：反推开学第一周的周一
            // 公式：开学日 = 今天 - ((currentWeek - 1) * 7) - (今天周几 - 1)
            val today = LocalDate.now()
            val dayOfWeekVal = today.dayOfWeek.value // Mon=1, Sun=7

            // 1. 先回到本周的周一
            val mondayOfCurrentWeek = today.minusDays(dayOfWeekVal - 1L)
            // 2. 再回退 (currentWeek - 1) 周
            val semesterStartDate = mondayOfCurrentWeek.minusWeeks(currentWeek - 1L)

            Log.d("MinePlus", "📅 时间校准: API周次=$currentWeek, 倒推开学日=$semesterStartDate")

            // 保存到 DataStore
            prefs.saveSemesterInfo(semesterStartDate.toString(), maxWeek)


            val allLessons = courseRes.lessons ?: emptyList()
            if (allLessons.isEmpty()) {
                Log.w("MinePlus", "⚠️ 课程列表为空")
                return@withContext
            }

            // 提取所有 lessonId 用于下一步请求
            val lessonIds = allLessons.map { it.id.toLong() }
            Log.d("MinePlus", "   获取到 ${lessonIds.size} 门课程")

            // --- 阶段 B: 获取排课详情 (datum) ---
            Log.d("MinePlus", "3️⃣ 请求详细排课数据 (datum)...")
            val datumReq = DatumRequest(
                lessonIds = lessonIds,
                stdPersonId = personId
            )
            val scheduleRes = api.getScheduleDatum(datumReq)
            Log.d("MinePlus", scheduleRes.result?.scheduleList?.size.toString())
            // --- 阶段 C: 数据清洗与转换 (Transform) ---
            Log.d("MinePlus", "4️⃣ 开始处理数据 & 分配颜色...")

            // 1. 🎨 颜色分配 (发牌算法)
            // 先按 ID 排序，保证每次分配的颜色一致
            val sortedLessonIds = lessonIds.sorted()
            // 建立映射表: lessonId -> colorIndex (0~14)
            val colorMap = sortedLessonIds.mapIndexed { index, id ->
                id to (index % 15) // 假设我们有 15 种颜色
            }.toMap()

            // 2. 构建 CourseEntity 列表
            val courseEntities = allLessons.map { json ->
                val id = json.id.toLong()
                CourseEntity(
                    lessonId = id,
                    name = json.course?.nameZh ?: "未知课程",
                    teacher = json.teacherAssignmentList?.firstOrNull()?.person?.nameZh ?: "",
                    credit = json.course?.credits ?: 0f,
                    colorIndex = colorMap[id] ?: 0, // 👈 写入颜色编号
                    rawScheduleText = null
                )
            }

            // 3. 构建 ScheduleEntity 列表
            val scheduleEntities = scheduleRes.result?.scheduleList?.map { item ->
                // 🕒 时间映射: 800 -> 1
                val startNode = TimeMapper.mapStartTimeToNode(item.startTime)

                ScheduleEntity(
                    lessonId = item.lessonId,
                    weekIndex = item.weekIndex,
                    dayOfWeek = item.weekday,
                    startNode = startNode,
                    step = item.periods, // 持续节次
                    room = item.room?.nameZh ?: "",
                    rawStartTime = formatTime(item.startTime), // 800 -> "08:00"
                    rawEndTime = formatTime(item.endTime),     // 1130 -> "11:30"
                    date = item.date ?: ""
                )
            } ?: emptyList()

            // --- 阶段 D: 存入数据库 (Load) ---
            Log.d("MinePlus", "5️⃣ 写入数据库...")
            // 这里的 clearAll 需要在 CourseDao 里实现，建议使用 @Transaction 保证原子性
            courseDao.clearAll() // 清空旧数据
            courseDao.insertCourses(courseEntities)
            courseDao.insertSchedules(scheduleEntities)

            Log.d("MinePlus", "🎉 数据刷新完成！存入 ${scheduleEntities.size} 条日程。")

            // ⚠️ 这里会触发导航/UI 行为，必须切回主线程
            withContext(Dispatchers.Main) {
                onLoginSuccess()
            }

        } catch (e: Exception) {
            Log.e("MinePlus", "❌ 数据刷新失败", e)
            throw e // 抛出给 ViewModel 处理 UI 状态
        }
    }

    // 辅助工具: 800 -> "08:00"
    private fun formatTime(time: Int): String {
        val t = time.toString().padStart(4, '0') // 补齐前导0
        return "${t.substring(0, 2)}:${t.substring(2)}"
    }
}