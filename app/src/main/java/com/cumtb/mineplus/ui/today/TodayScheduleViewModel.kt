package com.cumtb.mineplus.ui.today

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cumtb.mineplus.data.database.CourseDao
import com.cumtb.mineplus.data.model.CourseSchedule
import com.cumtb.mineplus.data.preference.AppPreferences
import com.cumtb.mineplus.data.repository.CourseRepository
import com.cumtb.mineplus.ui.theme.CoursePalettes
import com.cumtb.mineplus.util.minuteAlignedTickerFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayScheduleViewModel @Inject constructor(
    private val repository: CourseRepository,
    private val courseDao: CourseDao,
    private val prefs: AppPreferences
) : ViewModel() {

    data class UiState(
        val title: String,
        val courses: List<CourseSchedule>,
        val isLoading: Boolean,
        /** 没有任何课表数据/学期信息（迭代1用于提示“重新登录”） */
        val hasNoData: Boolean,
        /** 用于在 UI 层推导“进行中/即将开始”等时间状态；通过 ticker 自动刷新 */
        val now: LocalDateTime
    )

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

    private val isLoadingFlow = MutableStateFlow(false)

    private val clock: Clock = Clock.systemDefaultZone()
    private val zoneId: ZoneId = ZoneId.systemDefault()

    private val nowFlow: StateFlow<LocalDateTime> = minuteAlignedTickerFlow(clock = clock)
        .map { instant -> LocalDateTime.ofInstant(instant, zoneId) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocalDateTime.now(clock))

    /** 今日日期：从 ticker 推导，避免 ViewModel 初始化后跨天不更新 */
    private val todayFlow: StateFlow<LocalDate> = nowFlow
        .map { it.toLocalDate() }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocalDate.now(clock))

    /** 今日周几(1..7)：从今日日期推导 */
    private val todayWeekdayFlow: StateFlow<Int> = todayFlow
        .map { it.dayOfWeek.value }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocalDate.now(clock).dayOfWeek.value)

    private val titleFlow: StateFlow<String> = combine(todayFlow, todayWeekdayFlow) { today, weekday ->
        val dateText = today.format(DateTimeFormatter.ofPattern("M月d日"))
        val weekText = when (weekday) {
            1 -> "周一"
            2 -> "周二"
            3 -> "周三"
            4 -> "周四"
            5 -> "周五"
            6 -> "周六"
            else -> "周日"
        }
        "$dateText  $weekText"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private val currentWeekFlow: StateFlow<Int?> = combine(semesterStartDateFlow, todayFlow) { start, today ->
        if (start == null) return@combine null
        val daysDiff = ChronoUnit.DAYS.between(start, today)
        ((daysDiff / 7) + 1).toInt().coerceAtLeast(1)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val todayCoursesFlow: StateFlow<List<CourseSchedule>?> = combine(currentWeekFlow, todayWeekdayFlow) { week, weekday ->
        week to weekday
    }.flatMapLatest { (week, weekday) ->
        if (week == null) {
            kotlinx.coroutines.flow.flowOf(null)
        } else {
            courseDao.getSchedulesByWeek(week)
                .map { weekCourses ->
                    weekCourses
                        .filter { it.dayOfWeek == weekday }
                        .sortedBy { it.startNode }
                }
                .map<List<CourseSchedule>, List<CourseSchedule>?> { it }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Persisted course card palette id, shared with Schedule screen. */
    val coursePaletteId: StateFlow<CoursePalettes.PaletteId> = prefs.coursePaletteId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CoursePalettes.defaultPaletteId)

    val uiState: StateFlow<UiState> = combine(
        isLoadingFlow,
        semesterStartDateFlow,
        todayCoursesFlow,
        nowFlow,
        titleFlow
    ) { loading, startDate, coursesOrNull, now, title ->
        val courses = coursesOrNull ?: emptyList()

        // Lightweight debug log when key pieces change.
        Log.d(
            "MinePlus",
            "TodayUiState: loading=$loading, hasStartDate=${startDate != null}, title=$title, courses=${courses.size}, now=$now"
        )

        UiState(
            title = title,
            courses = courses,
            isLoading = loading,
            hasNoData = startDate == null,
            now = now
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        UiState(
            title = "",
            courses = emptyList(),
            isLoading = false,
            hasNoData = true,
            now = LocalDateTime.now(clock)
        )
    )

    enum class RefreshSource {
        User
    }

    sealed interface UiEvent {
        data object RefreshSuccess : UiEvent
        data class RefreshFailed(val message: String) : UiEvent
    }

    private val _events = MutableSharedFlow<UiEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events = _events.asSharedFlow()

    fun onRefreshTriggered(source: RefreshSource = RefreshSource.User) {
        viewModelScope.launch {
            // 双重保险：避免并发刷新导致重复请求/重复提示
            if (isLoadingFlow.value) return@launch

            Log.d("MinePlus", "TodayRefresh: triggered (source=$source)")
            isLoadingFlow.value = true
            try {
                Log.d("MinePlus", "TodayRefresh: calling repository.refreshAllData()")
                repository.refreshAllData(onLoginSuccess = {})
                Log.d("MinePlus", "TodayRefresh: repository.refreshAllData() finished")
                if (source == RefreshSource.User) {
                    _events.tryEmit(UiEvent.RefreshSuccess)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("MinePlus", "TodayRefresh: failed", e)
                if (source == RefreshSource.User) {
                    _events.tryEmit(UiEvent.RefreshFailed(e.toUserRefreshMessage()))
                }
            } finally {
                isLoadingFlow.value = false
                Log.d("MinePlus", "TodayRefresh: end (loading=false)")
            }
        }
    }

    private fun Throwable.toUserRefreshMessage(): String {
        return when (this) {
            is UnknownHostException -> "网络不可用，请检查网络后重试"
            is SocketTimeoutException -> "网络连接超时，请稍后重试"
            is IOException -> "网络异常，请稍后重试"
            else -> "刷新失败，请稍后重试"
        }
    }

    /** @deprecated Use [onRefreshTriggered] to align with ScheduleScreen refresh semantics. */
    @Deprecated("Use onRefreshTriggered()", ReplaceWith("onRefreshTriggered()"))
    fun onRefresh() = onRefreshTriggered()

    fun onCoursePaletteSelected(id: CoursePalettes.PaletteId) {
        viewModelScope.launch {
            prefs.setCoursePaletteId(id)
        }
    }
}
