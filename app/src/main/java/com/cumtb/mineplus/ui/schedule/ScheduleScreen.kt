package com.cumtb.mineplus.ui.schedule

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cumtb.mineplus.data.model.CourseSchedule
import com.cumtb.mineplus.ui.theme.CoursePalettes
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

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

    // 预取：当前/前一周/后一周，避免左右页首次露出时触发 DB 查询导致卡顿
    val schedulesThisWeek by viewModel.schedulesForWeekFlow(selectedWeek).collectAsState(initial = emptyList())
    val schedulesPrevWeek by viewModel.schedulesForWeekFlow((selectedWeek - 1).coerceAtLeast(1)).collectAsState(initial = emptyList())
    val schedulesNextWeek by viewModel.schedulesForWeekFlow((selectedWeek + 1).coerceAtMost(maxWeek.coerceAtLeast(1))).collectAsState(initial = emptyList())

    // Pager: page 0 对应第 1 周
    val pagerState = rememberPagerState(
        initialPage = (selectedWeek - 1).coerceAtLeast(0),
        pageCount = { maxWeek.coerceAtLeast(1) }
    )

    // external selectedWeek -> pager
    LaunchedEffect(selectedWeek, maxWeek) {
        val target = (selectedWeek - 1).coerceIn(0, (maxWeek - 1).coerceAtLeast(0))
        if (pagerState.currentPage != target) {
            // 外部周次变更（自动定位/下拉刷新后修正）时直接对齐页面
            pagerState.scrollToPage(target)
        }
    }

    // pager settled -> selectedWeek (avoid syncing on every drag frame)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val week = page + 1
            if (week != selectedWeek) {
                viewModel.onWeekSelected(week)
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

    var menuExpanded by remember { mutableStateOf(false) }
    var weekMenuExpanded by remember { mutableStateOf(false) }

    val menuIconSize = 48.dp
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    // Reserve the same space as the action icon so the title stays centered.
                    Spacer(modifier = Modifier.size(menuIconSize))
                },
                title = {
                    // 标题显示当前周，点击弹出周次选择（周次切换的滑动过程放到主体 Pager）
                    Text(
                        text = "第 $selectedWeek 周",
                        style = MaterialTheme.typography.titleMedium,
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
                                text = { Text("第 $week 周") },
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
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(menuIconSize)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Menu"
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("关于") },
                            onClick = {
                                menuExpanded = false
                                onNavigateToAbout()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("重新登录") },
                            onClick = {
                                menuExpanded = false
                                onRelogin()
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
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
                key = { page -> page }
            ) { page ->
                val week = page + 1
                val weekSchedules: List<CourseSchedule> = when (week) {
                    selectedWeek -> schedulesThisWeek
                    selectedWeek - 1 -> schedulesPrevWeek
                    selectedWeek + 1 -> schedulesNextWeek
                    else -> emptyList()
                }

                key(page) {
                    ScheduleWeekPage(
                        week = week,
                        schedules = weekSchedules,
                        semesterStartDate = semesterStartDate
                    )
                }
            }

            // 触发刷新：当手势进入 refreshing 状态
            if (pullState.isRefreshing && !isLoading) {
                LaunchedEffect(Unit) {
                    viewModel.onRefreshTriggered()
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
    semesterStartDate: LocalDate?
) {
    // 为每一周缓存一个竖向滚动位置，避免侧页首次出现时创建 ScrollState 引发抖动
    val sharedVScrollState: ScrollState = rememberSaveable(week, saver = ScrollState.Saver) {
        ScrollState(initial = 0)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        WeekHeaderRow(
            selectedWeek = week,
            semesterStartDate = semesterStartDate
        )

        Row(modifier = Modifier.fillMaxSize()) {
            TimeSidebar(scrollState = sharedVScrollState)
            Box(modifier = Modifier.weight(1f)) {
                ScheduleContent(
                    schedules = schedules,
                    scrollState = sharedVScrollState
                )
            }
        }
    }
}

@Composable
private fun WeekHeaderRow(
    selectedWeek: Int,
    semesterStartDate: LocalDate?
) {
    // (grid lines are drawn in ScheduleContent; header just shows labels)
    val days = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    val formatter = remember { DateTimeFormatter.ofPattern("MM-dd") }

    // 以“学期开始日期”为第 1 周周一，算出当前周周一
    val mondayOfWeek: LocalDate? = remember(selectedWeek, semesterStartDate) {
        semesterStartDate?.plusDays(((selectedWeek - 1).coerceAtLeast(0) * 7).toLong())
    }

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
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        days.forEachIndexed { index, day ->
            val dateText = mondayOfWeek
                ?.plusDays(index.toLong())
                ?.format(formatter)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (dateText != null) "$day\n$dateText" else day,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Clip,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun TimeSidebar(scrollState: androidx.compose.foundation.ScrollState) {
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
            .padding(vertical = 4.dp),
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
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
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
fun ScheduleContent(
    schedules: List<CourseSchedule>,
    scrollState: androidx.compose.foundation.ScrollState
) {
    val gridLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Explicitly use BoxWithConstraints scope values (maxWidth/maxHeight)
        val dayWidth = this.maxWidth / 7
        val cellHeight = ScheduleDimens.CellHeight

        // Grid background: draw lines in one Canvas (less layout work than 12 bordered Boxes)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(cellHeight * 12)
        ) {
            val stroke = 0.5.dp.toPx()
            val w = size.width
            val h = size.height

            // horizontal lines
            val rowH = h / 12f
            for (i in 0..12) {
                val y = rowH * i
                drawLine(
                    color = gridLineColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = stroke
                )
            }
            // vertical lines (7 days)
            val colW = w / 7f
            for (i in 0..7) {
                val x = colW * i
                drawLine(
                    color = gridLineColor,
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = stroke
                )
            }
        }

        schedules.forEach { course ->
            val xOffset = dayWidth * (course.dayOfWeek - 1)
            val yOffset = cellHeight * (course.startNode - 1)
            val cardHeight = cellHeight * course.step

            CourseCard(
                course = course,
                modifier = Modifier
                    .width(dayWidth)
                    .height(cardHeight)
                    .absoluteOffset(x = xOffset, y = yOffset)
                    .padding(1.dp)
            )
        }
    }
}

@Composable
fun CourseCard(
    course: CourseSchedule,
    modifier: Modifier = Modifier
) {
    // 根据 colorIndex 取色
    val bgColor = CoursePalettes.Macaron.getOrElse(course.colorIndex) { Color.Gray }

    // 课程色板是“自定义色”，不在 Material colorScheme 里，contentColorFor() 推导不稳定。
    // 用亮度做一个确定性选择：浅色底 -> 黑字；深色底 -> 白字。
    val textColor = if (bgColor.luminance() > 0.6f) Color(0xFF111111) else Color.White

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(2.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = course.courseName,
            fontSize = ScheduleDimens.CourseCardFontSize,
            color = textColor,
            fontWeight = FontWeight.Bold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = course.room.replace("教学楼", ""), // 简单去重，让显示更短
            fontSize = ScheduleDimens.RoomFontSize,
            color = textColor.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}