package com.cumtb.mineplus.ui.schedule

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cumtb.mineplus.data.database.CourseDao
import com.cumtb.mineplus.data.model.CourseSchedule
import com.cumtb.mineplus.data.preference.AppPreferences
import com.cumtb.mineplus.data.repository.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val repository: CourseRepository,
    private val courseDao: CourseDao,
    private val prefs: AppPreferences
) : ViewModel() {

    // --- 状态流 ---

    // 1. 当前选中的周 (UI 听这个来显示 "第 X 周")
    private val _selectedWeek = MutableStateFlow(1)
    val selectedWeek = _selectedWeek.asStateFlow()

    // 2. 总周数 (UI 听这个来决定周次选择器显示到多少)
    private val _maxWeek = MutableStateFlow(20) // 默认给个20兜底
    val maxWeek = _maxWeek.asStateFlow()

    // 3. 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // 4. 核心数据流：当 selectedWeek 变化时，自动去数据库查课
//    val scheduleFlow: StateFlow<List<CourseSchedule>> = MutableStateFlow(
//        listOf(
//            // 1. 周一 第1-2节 (高数) - 红色
//            CourseSchedule(
//                id = 1, dayOfWeek = 1, startNode = 1, step = 2,
//                room = "教1-201", rawStartTime = "08:00", rawEndTime = "09:35", date = "",
//                courseName = "高等数学A(1)", teacher = "张三", colorIndex = 0
//            ),
//            // 2. 周一 第3-4节 (大英) - 紫色
//            CourseSchedule(
//                id = 2, dayOfWeek = 1, startNode = 3, step = 2,
//                room = "教3-405", rawStartTime = "10:05", rawEndTime = "11:40", date = "",
//                courseName = "大学英语(3)", teacher = "李四", colorIndex = 1
//            ),
//            // 3. 周二 第3-5节 (实验课，跨3节) - 蓝色
//            CourseSchedule(
//                id = 3, dayOfWeek = 2, startNode = 3, step = 3,
//                room = "机房B", rawStartTime = "10:05", rawEndTime = "12:30", date = "",
//                courseName = "Python程序设计实验", teacher = "王五", colorIndex = 3
//            ),
//            // 4. 周三 第1-2节 (体育) - 绿色
//            CourseSchedule(
//                id = 4, dayOfWeek = 3, startNode = 1, step = 2,
//                room = "田径场", rawStartTime = "08:00", rawEndTime = "09:35", date = "",
//                courseName = "体育(篮球)", teacher = "赵六", colorIndex = 6
//            ),
//            // 5. 周五 第6-9节 (超长课) - 橙色
//            CourseSchedule(
//                id = 5, dayOfWeek = 5, startNode = 6, step = 4,
//                room = "教4-101", rawStartTime = "14:00", rawEndTime = "17:30", date = "",
//                courseName = "毛泽东思想和中国特色社会主义理论体系概论", teacher = "钱七", colorIndex = 11
//            )
//        )
//    ).asStateFlow()
//    @OptIn(ExperimentalCoroutinesApi::class)
    val scheduleFlow: StateFlow<List<CourseSchedule>> = _selectedWeek
        .flatMapLatest { week ->
            courseDao.getSchedulesByWeek(week)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 5. 表头日期流 (Mon 1/19, Tue 1/20...)
    // 我们从 scheduleFlow 里随便拿一节课的 date，倒推这一周的周一日期
    val weekDates: StateFlow<List<LocalDate>> = scheduleFlow
        .map { schedules ->
            if (schedules.isEmpty()) {
                // 如果这周没课，就没法从数据反推日期，暂时返回空或者根据 selectedWeek 估算（如果做了开学日期配置）
                // 这里为了简单，如果没数据就返回空列表，UI 显示通用表头
                emptyList()
            } else {
                // 随便找一节课，比如 "2025-05-29" (周四)
                val sample = schedules.first()
                try {
                    val date = LocalDate.parse(sample.date) // 解析日期
                    val dayOfWeek = sample.dayOfWeek // 4
                    // 倒推周一：日期 - (周几 - 1) 天
                    val monday = date.minusDays((dayOfWeek - 1).toLong())

                    // 生成周一到周日的日期列表
                    List(7) { i -> monday.plusDays(i.toLong()) }
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    init {
        // 🚀 启动时，监听 DataStore 自动计算当前周
        observeSemesterInfo()
    }

    // --- 用户意图 (Actions) ---

    fun onWeekSelected(week: Int) {
        val max = _maxWeek.value
        _selectedWeek.value = week.coerceIn(1, max.coerceAtLeast(1))
    }

    fun onRefreshTriggered() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.refreshAllData(onLoginSuccess = {})
                // 刷新后可能当前周的数据变了，或者 maxWeek 变了，StateFlow 会自动通知 UI
                Log.d("MinePlus", "刷新成功")
            } catch (e: Exception) {
                Log.e("MinePlus", "刷新失败", e)
                // 这里应该发射一个 error event 给 UI 显示 Toast，先略过
            } finally {
                _isLoading.value = false
            }
        }
    }

    // --- 内部逻辑 ---

    private fun observeSemesterInfo() {
        viewModelScope.launch {
            combine(prefs.semesterStartDate, prefs.totalWeeks) { startDateStr, totalWeeks ->
                _maxWeek.value = totalWeeks.coerceAtLeast(1)

                if (startDateStr != null) {
                    try {
                        val start = LocalDate.parse(startDateStr)
                        val today = LocalDate.now()

                        val daysDiff = ChronoUnit.DAYS.between(start, today)
                        val currentWeek = (daysDiff / 7) + 1

                        Log.d("MinePlus", "📅 自动定位: 开学=$start, 今天=$today, 算出第 $currentWeek 周")
                        onWeekSelected(currentWeek.toInt())
                    } catch (e: Exception) {
                        Log.w("MinePlus", "semesterStartDate 解析失败: $startDateStr", e)
                    }
                }
            }.collect()
        }
    }
}
