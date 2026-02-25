package com.cumtb.mineplus.data.database

import androidx.room.*
import com.cumtb.mineplus.data.model.CourseSchedule
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    // ... 之前的查询方法 ...

    // 插入课程
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<CourseEntity>)

    // 插入日程
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<ScheduleEntity>)

    // 清空表 (注意顺序，先删 Schedule 再删 Course，虽然有 CASCADE 但这样更稳)
    @Query("DELETE FROM schedules")
    suspend fun deleteAllSchedules()

    @Query("DELETE FROM courses")
    suspend fun deleteAllCourses()

    // 事务操作：一键清空并重写
    @Transaction
    suspend fun clearAll() {
        deleteAllSchedules()
        deleteAllCourses()
    }

    /**
     * 🔍 核心查询：根据周次获取完整的课程表数据
     * 使用 INNER JOIN 关联 schedules 和 courses 两张表
     */
    @Query("""
        SELECT 
            -- 1. 选取 schedules 表的字段
            schedules.id,
            schedules.dayOfWeek,
            schedules.startNode,
            schedules.step,
            schedules.room,
            schedules.rawStartTime,
            schedules.rawEndTime,
            schedules.date,
            
            -- 2. 选取 courses 表的字段 (注意起别名对应 CourseSchedule 类)
            courses.name as courseName,
            courses.teacher,
            courses.credit,
            courses.rawScheduleText,
            courses.weightedCalc,
            courses.colorIndex
            
        FROM schedules
        -- 3. 联表条件：通过 lessonId 匹配
        INNER JOIN courses ON schedules.lessonId = courses.lessonId

        -- 4. 过滤条件：只取指定的周
        WHERE schedules.weekIndex = :weekIndex
        
        -- 5. 排序：按周几 -> 第几节 排序
        ORDER BY schedules.dayOfWeek ASC, schedules.startNode ASC
    """)
    fun getSchedulesByWeek(weekIndex: Int): Flow<List<CourseSchedule>>


    /**
     * 补充一个查询：获取当前学期共有多少周 (用于生成周次选择器的最大值)
     * 取 schedules 表里最大的 weekIndex
     */
    @Query("SELECT MAX(weekIndex) FROM schedules")
    suspend fun getMaxWeekIndex(): Int?

    /**
     * 📅 定位当前周
     * 查找 日期 >= 指定日期 的第一条记录，返回它的周次。
     * 这样即使今天是周日（没课），也能定位到下周一所在的周次。
     */
    @Query("SELECT weekIndex FROM schedules WHERE date >= :today ORDER BY date ASC LIMIT 1")
    suspend fun getUpcomingWeekIndex(today: String): Int?
}