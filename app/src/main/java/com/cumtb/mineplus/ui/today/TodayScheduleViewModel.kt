package com.cumtb.mineplus.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cumtb.mineplus.data.database.CourseDao
import com.cumtb.mineplus.data.model.CourseSchedule
import com.cumtb.mineplus.data.preference.AppPreferences
import com.cumtb.mineplus.data.repository.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class TodayScheduleViewModel @Inject constructor(
    private val repository: CourseRepository,
    private val courseDao: CourseDao,
    prefs: AppPreferences
) : ViewModel() {

    data class UiState(
        val title: String,
        val courses: List<CourseSchedule>,
        val isLoading: Boolean,
        /** 没有任何课表数据/学期信息（迭代1用于提示“重新登录”） */
        val hasNoData: Boolean
    )

    private val today = LocalDate.now()
    private val todayWeekday = today.dayOfWeek.value // 1..7

    private val semesterStartDateFlow: StateFlow<LocalDate?> = prefs.semesterStartDate
        .map { it?.takeIf(String::isNotBlank) }
        .map { str ->
            try {
                str?.let(LocalDate::parse)
            } catch (_: Exception) {
                null
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val title: String = run {
        val dateText = today.format(DateTimeFormatter.ofPattern("M月d日"))
        val weekText = when (todayWeekday) {
            1 -> "周一"
            2 -> "周二"
            3 -> "周三"
            4 -> "周四"
            5 -> "周五"
            6 -> "周六"
            else -> "周日"
        }
        "$dateText  $weekText"
    }

    private val isLoadingFlow = MutableStateFlow(false)

    private val currentWeekFlow: StateFlow<Int?> = semesterStartDateFlow
        .map { start ->
            if (start == null) return@map null
            val daysDiff = ChronoUnit.DAYS.between(start, today)
            ((daysDiff / 7) + 1).toInt().coerceAtLeast(1)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val todayCoursesFlow: StateFlow<List<CourseSchedule>?> = currentWeekFlow
        .flatMapLatest { week ->
            if (week == null) {
                kotlinx.coroutines.flow.flowOf(null)
            } else {
                courseDao.getSchedulesByWeek(week)
                    .map { weekCourses ->
                        weekCourses
                            .filter { it.dayOfWeek == todayWeekday }
                            .sortedBy { it.startNode }
                    }
                    .map<List<CourseSchedule>, List<CourseSchedule>?> { it }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val uiState: StateFlow<UiState> = combine(
        isLoadingFlow,
        semesterStartDateFlow,
        todayCoursesFlow
    ) { loading, startDate, coursesOrNull ->
        UiState(
            title = title,
            courses = coursesOrNull ?: emptyList(),
            isLoading = loading,
            hasNoData = startDate == null
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        UiState(title = title, courses = emptyList(), isLoading = false, hasNoData = true)
    )

    fun onRefresh() {
        viewModelScope.launch {
            isLoadingFlow.value = true
            try {
                repository.refreshAllData(onLoginSuccess = {})
            } finally {
                isLoadingFlow.value = false
            }
        }
    }
}
