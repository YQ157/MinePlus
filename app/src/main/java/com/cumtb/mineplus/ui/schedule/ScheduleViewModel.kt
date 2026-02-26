package com.cumtb.mineplus.ui.schedule

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cumtb.mineplus.data.database.CourseDao
import com.cumtb.mineplus.data.model.CourseSchedule
import com.cumtb.mineplus.data.preference.AppPreferences
import com.cumtb.mineplus.data.repository.CourseRepository
import com.cumtb.mineplus.ui.theme.CoursePalettes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

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
    // NOTE: legacy scheduleFlow is no longer used; ScheduleScreen uses schedulesByWeek aggregation.

    /** 学期开始日期（ISO-8601 字符串解析为 LocalDate），用于 UI 渲染表头日期 */
    val semesterStartDate: StateFlow<LocalDate?> = prefs.semesterStartDate
        .map { dateStr ->
            if (dateStr.isNullOrBlank()) return@map null
            try {
                LocalDate.parse(dateStr)
            } catch (_: Exception) {
                null
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // NOTE: legacy weekDates is no longer used.

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
            if (_isLoading.value) return@launch

            _isLoading.value = true
            try {
                repository.refreshAllData(onLoginSuccess = {})
                Log.d("MinePlus", "刷新成功")
                if (source == RefreshSource.User) {
                    _events.tryEmit(UiEvent.RefreshSuccess)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("MinePlus", "刷新失败", e)
                if (source == RefreshSource.User) {
                    _events.tryEmit(UiEvent.RefreshFailed(e.toUserRefreshMessage()))
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun Throwable.toUserRefreshMessage(): String {
        return when (this) {
            is UnknownHostException -> "网络不可用，请检查网络后重试"
            is SocketTimeoutException -> "网络连接超时，请稍后重试"
            is IOException -> "网络异常，请稍后重试"
            else -> "刷新失败，请重新登录"
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

    /** UI 选周入口：统一做边界保护。 */
    fun onWeekSelected(week: Int) {
        val max = _maxWeek.value.coerceAtLeast(1)
        _selectedWeek.value = week.coerceIn(1, max)
    }

    /** ScheduleScreen 调用入口：避免符号缓存导致的 onWeekSelected 解析异常。 */
    fun onSelectedWeekChanged(week: Int) {
        onWeekSelected(week)
    }

    /** UI 声明“当前需要的周集合”（通常 prev/current/next）。 */
    private val activeWeeks = MutableStateFlow<Set<Int>>(emptySet())

    // --- 按周热缓存（用于 pager 相邻页丝滑露出） ---
    private val weekStateCache = mutableMapOf<Int, StateFlow<List<CourseSchedule>>>()
    private val lastWeekValueCache = mutableMapOf<Int, List<CourseSchedule>>()

    /**
     * A hot StateFlow for a given week.
     * - Caches per-week flows to avoid cold-start when a page first becomes visible.
     * - Uses last known value as initialValue to reduce empty->data layout thrash.
     */
    fun schedulesForWeekState(week: Int): StateFlow<List<CourseSchedule>> {
        val max = _maxWeek.value.coerceAtLeast(1)
        val safeWeek = week.coerceIn(1, max)

        return weekStateCache.getOrPut(safeWeek) {
            courseDao.getSchedulesByWeek(safeWeek)
                .onEach { list -> lastWeekValueCache[safeWeek] = list }
                .stateIn(
                    scope = viewModelScope,
                    // Keep it warm for a while to cover quick swipes between adjacent weeks.
                    started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 60_000),
                    initialValue = lastWeekValueCache[safeWeek] ?: emptyList()
                )
        }
    }

    // --- 预取 ---
    private var prefetchJob: Job? = null

    /** Best-effort prefetch of a single week (used by swipe-threshold prefetch). */
    fun prefetchWeek(week: Int) {
        val max = _maxWeek.value.coerceAtLeast(1)
        val safeWeek = week.coerceIn(1, max)
        // Ensure the hot StateFlow is created, and trigger a first emission.
        schedulesForWeekState(safeWeek)
        viewModelScope.launch {
            courseDao.getSchedulesByWeek(safeWeek)
                .take(1)
                .catch { /* best-effort */ }
                .collect()
        }
    }

    /**
     * Prefetch schedules around a given week (typically current/prev/next).
     *
     * This is a best-effort, UI-independent optimization: it warms up Room/SQLite work
     * so the adjacent pager pages are less likely to stutter on first reveal.
     */
    fun prefetchWeeksAround(week: Int) {
        val max = _maxWeek.value.coerceAtLeast(1)
        val safeWeek = week.coerceIn(1, max)
        val weeks = listOf(safeWeek - 1, safeWeek, safeWeek + 1)
            .map { it.coerceIn(1, max) }
            .distinct()

        // Cancel previous prefetch to avoid piling up collectors during fast flings.
        prefetchJob?.cancel()
        prefetchJob = viewModelScope.launch {
            // Short-lived collection is enough to trigger relevant DB work.
            // If the DAO flow is backed by Room invalidation, first emission should come quickly.
            val jobs = weeks.map { w ->
                launch {
                    courseDao.getSchedulesByWeek(w)
                        .take(1)
                        .catch { /* best-effort: ignore */ }
                        .collect()
                }
            }
            // Give the children a tiny window; then cancel anything still running.
            delay(300)
            jobs.forEach { it.cancel() }
        }
    }

    /** VM 输出：当前激活周的课表 Map。
     * 注意：CourseSchedule 本身没有 week 字段，所以这里基于 DAO 的 getSchedulesByWeek(week) 聚合。
     */
    val schedulesByWeek: StateFlow<Map<Int, List<CourseSchedule>>> =
        combine(activeWeeks, _maxWeek) { weeks, max ->
            weeks
                .map { it.coerceIn(1, max.coerceAtLeast(1)) }
                .toSet()
        }
            .flatMapLatest { weeks ->
                if (weeks.isEmpty()) {
                    flowOf(emptyMap())
                } else {
                    combine(
                        weeks.map { w ->
                            // 复用现有的 hot per-week StateFlow，避免“首次露出”冷启动。
                            schedulesForWeekState(w)
                                .map { list ->
                                    // 这里做轻量的去重/排序，放到 Default 上做。
                                    val normalized = list
                                        .distinctBy { it.id }
                                        .sortedWith(
                                            compareBy<CourseSchedule> { it.dayOfWeek }
                                                .thenBy { it.startNode }
                                                .thenBy { it.step }
                                                .thenBy { it.courseName }
                                        )
                                    w to normalized
                                }
                        }
                    ) { pairs ->
                        pairs.toMap()
                    }
                }
            }
            .flowOn(Dispatchers.Default)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 60_000),
                initialValue = emptyMap()
            )

    /** UI 调用：声明当前希望 VM 准备/缓存的周集合（通常 prev/current/next）。 */
    fun activateWeeks(weeks: Set<Int>) {
        val max = _maxWeek.value.coerceAtLeast(1)
        val normalized = weeks
            .map { it.coerceIn(1, max) }
            .toSet()
        if (normalized != activeWeeks.value) {
            activeWeeks.value = normalized
        }
    }

    private var warmUpJob: Job? = null

    /**
     * App 启动后预热（best-effort）。目的：让第一次点开“周课表”不需要再冷启动 Room 查询。
     *
     * 策略：
     * - 延迟一小段时间，避开冷启动首帧的关键路径
     * - 只预热「当前周 + 前后各 1 周」
     * - 复用现有 per-week hot cache + prefetchWeeksAround
     */
    fun warmUpAfterAppStart(delayMillis: Long = 800L) {
        // 去重：避免多处入口反复触发
        if (warmUpJob?.isActive == true) return

        warmUpJob = viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            // 让出首帧/首屏动画时间
            delay(delayMillis)

            val max = _maxWeek.value.coerceAtLeast(1)
            val week = _selectedWeek.value.coerceIn(1, max)

            // 1) 预取 Room（短收集触发 SQLite）
            prefetchWeeksAround(week)

            // 2) 同时激活 schedulesByWeek 的聚合（会触发 Default 上的排序去重），
            //    让 Week 页首次打开直接命中 Map。
            activateWeeks(
                setOf(
                    (week - 1).coerceIn(1, max),
                    week,
                    (week + 1).coerceIn(1, max)
                )
            )
        }
    }

    /** Persisted course card palette (used by CourseCard/Course overlay). */
    val coursePaletteId: StateFlow<CoursePalettes.PaletteId> = prefs.coursePaletteId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CoursePalettes.defaultPaletteId)

    fun onCoursePaletteSelected(id: CoursePalettes.PaletteId) {
        viewModelScope.launch {
            prefs.setCoursePaletteId(id)
        }
    }

    init {
        observeSemesterInfo()
    }
}
