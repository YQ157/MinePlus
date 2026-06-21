package com.cumtb.mineplus.ui.schedule

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cumtb.mineplus.data.model.CourseSchedule
import com.cumtb.mineplus.ui.components.GlassOverflowMenu
import com.cumtb.mineplus.ui.components.GlassOverflowMenuItem
import com.cumtb.mineplus.ui.theme.CoursePalettes
import com.cumtb.mineplus.ui.theme.Dimens
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    onNavigateToAbout: () -> Unit,
    onRelogin: () -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    // 当前周
    val selectedWeek by viewModel.selectedWeek.collectAsState()
    val maxWeek by viewModel.maxWeek.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val semesterStartDate by viewModel.semesterStartDate.collectAsState()

    // 只订阅一次：按周聚合后的 Map
    val schedulesByWeek by viewModel.schedulesByWeek.collectAsState()

    // Pager: page 0 对应第 1 周
    val pagerState = rememberPagerState(
        initialPage = (selectedWeek - 1).coerceAtLeast(0),
        pageCount = { maxWeek.coerceAtLeast(1) }
    )

    // B: external selectedWeek -> pager（仅在外部强制改周且当前不在滑动时对齐，避免来回打架）
    LaunchedEffect(selectedWeek, maxWeek) {
        val target = (selectedWeek - 1).coerceIn(0, (maxWeek - 1).coerceAtLeast(0))
        if (!pagerState.isScrollInProgress && pagerState.currentPage != target) {
            // 外部周次变更（自动定位/下拉刷新后修正）时对齐页面
            pagerState.scrollToPage(target)
        }
    }

    // B+D: pager settled -> selectedWeek（单一入口回写） + 预取
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                val week = page + 1
                if (week != selectedWeek) {
                    viewModel.onSelectedWeekChanged(week)
                }
                viewModel.prefetchWeeksAround(week)
            }
    }

    // 2) 预取前移：拖拽接近边界就预取相邻周（去重），让“刚露出相邻周”更平滑
    var lastPrefetchWeek by remember { mutableIntStateOf(-1) }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage to pagerState.currentPageOffsetFraction }
            .collect { (page, offset) ->
                val threshold = 0.35f
                if (abs(offset) < threshold) return@collect

                val currentWeek = page + 1
                val targetWeek = if (offset > 0f) currentWeek + 1 else currentWeek - 1
                if (targetWeek != lastPrefetchWeek) {
                    lastPrefetchWeek = targetWeek
                    viewModel.prefetchWeek(targetWeek)
                }
            }
    }

    val pullState = rememberPullToRefreshState()

    // 当 ViewModel loading 结束时，收起下拉控件
    LaunchedEffect(isLoading) {
        if (!isLoading) {
            pullState.endRefresh()
        }
    }

    var weekMenuExpanded by remember { mutableStateOf(false) }

    // Keep the TopAppBar compact. 48.dp tends to make the bar feel too tall.
    val menuIconSize = 36.dp
    val scope = rememberCoroutineScope()

    // When the pager moves, tell VM which weeks we care about (prev/current/next).
    LaunchedEffect(pagerState, maxWeek) {
        fun weeksAround(page: Int): Set<Int> {
            val safeMax = maxWeek.coerceAtLeast(1)
            val currentWeek = (page + 1).coerceIn(1, safeMax)
            return setOf(
                (currentWeek - 1).coerceIn(1, safeMax),
                currentWeek,
                (currentWeek + 1).coerceIn(1, safeMax)
            )
        }

        // prime
        viewModel.activateWeeks(weeksAround(pagerState.currentPage))

        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                viewModel.activateWeeks(weeksAround(page))
            }
    }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ScheduleViewModel.UiEvent.RefreshFailed -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                else -> Unit
            }
        }
    }

    val selectedPaletteId by viewModel.coursePaletteId.collectAsState()
    val coursePalette = remember(selectedPaletteId) { CoursePalettes.colorsFor(selectedPaletteId) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                // Keep just a tiny gap to the status bar.
                windowInsets = WindowInsets.statusBars,
                navigationIcon = {
                    // Reserve the same space as the action icon so the title stays centered.
                    Spacer(modifier = Modifier.size(menuIconSize))
                },
                title = {
                    // 标题显示当前周，点击弹出周次选择（周次切换的滑动过程放到主体 Pager）
                    Text(
                        text = "第 $selectedWeek 周",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { weekMenuExpanded = true },
                        textAlign = TextAlign.Center
                    )

                    DropdownMenu(
                        expanded = weekMenuExpanded,
                        onDismissRequest = { weekMenuExpanded = false }
                    ) {
                        val safeMax = maxWeek.coerceAtLeast(1)
                        for (week in 1..safeMax) {
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        text = "第 $week 周",
                                        style = MaterialTheme.typography.bodyMedium
                                    ) 
                                },
                                onClick = {
                                    weekMenuExpanded = false
                                    scope.launch {
                                        pagerState.animateScrollToPage(week - 1)
                                    }
                                }
                            )
                        }
                    }
                },
                actions = {
                    GlassOverflowMenu(
                        iconSize = menuIconSize,
                        items = listOf(
                            GlassOverflowMenuItem(
                                text = "关于",
                                icon = Icons.Filled.Info,
                                onClick = onNavigateToAbout
                            ),
                            GlassOverflowMenuItem(
                                text = "重新登录",
                                icon = Icons.AutoMirrored.Filled.Logout,
                                onClick = onRelogin
                            )
                        )
                    )
                },
                // Match Today screen: white/neutral top bar with top divider.
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
            
            // 添加顶部极细分割线
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                thickness = 0.5.dp
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .nestedScroll(pullState.nestedScrollConnection)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { page -> page },
                beyondViewportPageCount = 1
            ) { page ->
                val week = page + 1
                val weekSchedules = schedulesByWeek[week].orEmpty()

                ScheduleWeekPage(
                    week = week,
                    schedules = weekSchedules,
                    semesterStartDate = semesterStartDate,
                    coursePalette = coursePalette
                )
            }

            // 触发刷新：当手势进入 refreshing 状态
            if (pullState.isRefreshing && !isLoading) {
                LaunchedEffect(pullState.isRefreshing) {
                    viewModel.onRefreshTriggered(ScheduleViewModel.RefreshSource.User)
                }
            }

            PullToRefreshContainer(
                modifier = Modifier.align(Alignment.TopCenter),
                state = pullState
            )
        }
    }
}

@Composable
private fun ScheduleWeekPage(
    week: Int,
    schedules: List<CourseSchedule>,
    semesterStartDate: LocalDate?,
    coursePalette: List<Color>
) {
    // 为每一周缓存一个竖向滚动位置，避免侧页首次出现时创建 ScrollState 引发抖动
    val sharedVScrollState: ScrollState = rememberSaveable(week, saver = ScrollState.Saver) {
        ScrollState(initial = 0)
    }

    // 计算“今天”是否在当前周内：不在则不高亮
    val todayColumnIndex: Int? = remember(week, semesterStartDate) {
        val start = semesterStartDate ?: return@remember null
        val today = LocalDate.now()
        val daysFromStart = java.time.temporal.ChronoUnit.DAYS.between(start, today).toInt()
        if (daysFromStart < 0) return@remember null

        val todayWeek = (daysFromStart / 7) + 1
        if (todayWeek != week) return@remember null

        // LocalDate.dayOfWeek.value: 1..7 (Mon..Sun)
        today.dayOfWeek.value - 1
    }

    Column(modifier = Modifier.fillMaxSize()) {
        WeekHeaderRow(
            selectedWeek = week,
            semesterStartDate = semesterStartDate,
            highlightedDayIndex = todayColumnIndex
        )

        Row(modifier = Modifier.fillMaxSize()) {
            TimeSidebar(scrollState = sharedVScrollState)
            Box(modifier = Modifier.weight(1f)) {
                // --- overlay expand state (single expanded) ---
                var expandedAnchor by remember(week) { mutableStateOf<CourseOverlayAnchor?>(null) }
                var overlayHostSize by remember(week) { mutableStateOf<IntSize?>(null) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coords ->
                            val newSize = coords.size
                            if (overlayHostSize != newSize) {
                                overlayHostSize = newSize
                            }
                        }
                ) {
                    ScheduleContent(
                        schedules = schedules,
                        scrollState = sharedVScrollState,
                        highlightedDayIndex = todayColumnIndex,
                        coursePalette = coursePalette,
                        onCourseClick = { course, boundsInHost ->
                             expandedAnchor = if (expandedAnchor?.courseId == course.id) {
                                null
                            } else {
                                CourseOverlayAnchor(
                                    courseId = course.id,
                                    course = course,
                                    boundsInRoot = boundsInHost
                                )
                            }
                        }
                    )

                    CourseOverlayHost(
                        expandedAnchor = expandedAnchor,
                        viewportSize = overlayHostSize,
                        onDismissRequest = { expandedAnchor = null },
                        modifier = Modifier.fillMaxSize(),
                        coursePalette = coursePalette
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekHeaderRow(
    selectedWeek: Int,
    semesterStartDate: LocalDate?,
    highlightedDayIndex: Int?
) {
    // (grid lines are drawn in ScheduleContent; header just shows labels)
    val days = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    val formatter = remember { DateTimeFormatter.ofPattern("MM-dd") }

    // 以“学期开始日期”为第 1 周周一，算出当前周周一
    val mondayOfWeek: LocalDate? = remember(selectedWeek, semesterStartDate) {
        semesterStartDate?.plusDays(((selectedWeek - 1).coerceAtLeast(0) * 7).toLong())
    }

    // Today highlight tuning:
    // - fill 轻一点，避免把文字背景染得太重
    // - border 清晰一点，保证深色/浅色都能一眼看出
    val todayFillColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.16f)
    val todayBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.70f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ScheduleDimens.HeaderHeight)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // 左上角占位：与左侧时间轴同宽，保证表头与网格对齐
        Box(
            modifier = Modifier
                .width(ScheduleDimens.SidebarWidth)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "时间",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        days.forEachIndexed { index, day ->
            val dateText = mondayOfWeek
                ?.plusDays(index.toLong())
                ?.format(formatter)

            val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (highlightedDayIndex == index) highlightColor else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (dateText != null) "$day\n$dateText" else day,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Clip,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun TimeSidebar(scrollState: ScrollState) {
    val timeLabels = remember {
        listOf(
            "08:00",
            "08:55",
            "10:10",
            "11:05",
            "14:00",
            "14:55",
            "16:10",
            "17:05",
            "19:00",
            "19:55",
            "20:50",
            "21:45"
        )
    }

    Column(
        modifier = Modifier
            .width(ScheduleDimens.SidebarWidth)
            .verticalScroll(scrollState)
            .padding(vertical = Dimens.tiny),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 生成 1 到 12 的刻度（节次 + 时间）
        for (i in 1..12) {
            Box(
                modifier = Modifier.height(ScheduleDimens.CellHeight),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$i",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = timeLabels.getOrNull(i - 1) ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
internal fun ScheduleContent(
    schedules: List<CourseSchedule>,
    scrollState: ScrollState,
    highlightedDayIndex: Int?,
    coursePalette: List<Color>,
    onCourseClick: (CourseSchedule, Rect) -> Unit = { _, _ -> }
) {
    val gridLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val density = androidx.compose.ui.platform.LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .onSizeChanged { containerSize = it }
    ) {
        if (containerSize.width <= 0) return@Box

        val dayWidth = with(density) { (containerSize.width / 7f).toDp() }
        val cellHeight = ScheduleDimens.CellHeight

        // 今天列：仅保留淡底高亮（去掉边框）
        if (highlightedDayIndex != null) {
            val fillColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)

            Box(
                modifier = Modifier
                    .absoluteOffset(x = dayWidth * highlightedDayIndex)
                    .width(dayWidth)
                    .height(cellHeight * 12)
                    .background(fillColor)
            )
        }

        GridBackground(
            gridLineColor = gridLineColor,
            modifier = Modifier
                .fillMaxWidth()
                .height(cellHeight * 12)
        )

        val courseBounds = remember(schedules, dayWidth, cellHeight) {
            schedules.map { course ->
                val xOffset = dayWidth * (course.dayOfWeek - 1)
                val yOffset = cellHeight * (course.startNode - 1)
                val cardHeight = cellHeight * course.step

                val bounds = Rect(
                    left = xOffset.value,
                    top = yOffset.value,
                    right = (xOffset + dayWidth).value,
                    bottom = (yOffset + cardHeight).value
                )

                CourseLayout(
                    course = course,
                    xOffset = xOffset,
                    yOffset = yOffset,
                    cardHeight = cardHeight,
                    bounds = bounds
                )
            }
        }

        courseBounds.forEach { item ->
            CourseCard(
                course = item.course,
                coursePalette = coursePalette,
                modifier = Modifier
                    .width(dayWidth)
                    .height(item.cardHeight)
                    .absoluteOffset(x = item.xOffset, y = item.yOffset)
                    .padding(Dimens.dividerThickness),
                onClick = { c, _ -> onCourseClick(c, item.bounds) }
            )
        }
    }
}

private data class CourseLayout(
    val course: CourseSchedule,
    val xOffset: androidx.compose.ui.unit.Dp,
    val yOffset: androidx.compose.ui.unit.Dp,
    val cardHeight: androidx.compose.ui.unit.Dp,
    val bounds: Rect
)

@Composable
private fun GridBackground(
    gridLineColor: Color,
    modifier: Modifier = Modifier
) {
    // Use drawWithCache so Path/metrics are only recalculated when size or inputs change.
    Box(
        modifier = modifier.drawWithCache {
            val strokeWidth = 0.5.dp.toPx()
            val w = size.width
            val h = size.height
            val rowH = h / 12f
            val colW = w / 7f

            onDrawBehind {
                // horizontal lines
                for (i in 0..12) {
                    val y = rowH * i
                    drawLine(
                        color = gridLineColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = strokeWidth
                    )
                }
                // vertical lines
                for (i in 0..7) {
                    val x = colW * i
                    drawLine(
                        color = gridLineColor,
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = strokeWidth
                    )
                }
            }
        }
    )
}

@Composable
internal fun CourseCard(
    course: CourseSchedule,
    coursePalette: List<Color>,
    modifier: Modifier = Modifier,
    onClick: (CourseSchedule, LayoutCoordinates?) -> Unit = { _, _ -> }
) {
    // 根据 colorIndex 取色
    val bgColor = coursePalette.getOrElse(course.colorIndex) { Color.Gray }

    // 课程色板是“自定义色”，不在 Material colorScheme 里，contentColorFor() 推导不稳定。
    // 用亮度做一个确定性选择：浅色底 -> 黑字；深色底 -> 白字。
    val textColor = if (bgColor.luminance() > 0.6f) Color(0xFF111111) else Color.White

    // C: 固定使用更小的教室字号，避免每张卡片测量宽度并写入状态导致的额外重组/卡顿。
    // 在 labelSmall 基础上再小一点点。
    val roomBaseStyle = MaterialTheme.typography.labelSmall
    val roomStyleToUse = remember(roomBaseStyle) {
        val newSize = (roomBaseStyle.fontSize.value - 1f).coerceAtLeast(9f)
        roomBaseStyle.copy(fontSize = newSize.sp)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .clickable {
                onClick(course, null)
            }
            .padding(Dimens.tiny),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = course.courseName,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Dimens.textSpacing))

        Text(
            text = course.room.replace("教学楼", ""), // 简单去重，让显示更短
            style = roomStyleToUse,
            color = textColor.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
        )
    }
}
