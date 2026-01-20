package com.cumtb.mineplus.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.snapshotFlow
import com.cumtb.mineplus.data.model.CourseSchedule
import com.cumtb.mineplus.ui.theme.CoursePalettes
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.input.nestedscroll.nestedScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val schedules by viewModel.scheduleFlow.collectAsState()
    val selectedWeek by viewModel.selectedWeek.collectAsState()
    val maxWeek by viewModel.maxWeek.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Pager：page 0 对应第 1 周
    val pagerState = rememberPagerState(
        initialPage = (selectedWeek - 1).coerceAtLeast(0),
        pageCount = { maxWeek.coerceAtLeast(1) }
    )

    // selectedWeek -> pager
    LaunchedEffect(selectedWeek, maxWeek) {
        val target = (selectedWeek - 1).coerceIn(0, (maxWeek - 1).coerceAtLeast(0))
        if (pagerState.currentPage != target) {
            pagerState.scrollToPage(target)
        }
    }

    // pager -> selectedWeek
    LaunchedEffect(pagerState, selectedWeek) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .nestedScroll(pullState.nestedScrollConnection)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- 顶部：周次左右滑 ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .statusBarsPadding()
                    .height(50.dp),
                contentAlignment = Alignment.Center
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) { page ->
                    Text(
                        text = "第 ${page + 1} 周",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // --- 主体：课表 ---
            Row(modifier = Modifier.fillMaxSize()) {
                TimeSidebar()
                Box(modifier = Modifier.weight(1f)) {
                    ScheduleContent(schedules = schedules)
                }
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

@Composable
fun TimeSidebar() {
    Column(
        modifier = Modifier
            .width(ScheduleDimens.SidebarWidth)
            .verticalScroll(rememberScrollState()) // 注意：这个滚动要和右边同步，暂时先各自滚动，后面优化
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 生成 1 到 12 的刻度
        for (i in 1..12) {
            Box(
                modifier = Modifier.height(ScheduleDimens.CellHeight),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "$i", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    // 这里其实可以根据 i 查具体的上课时间 (8:00)，暂时省略
                }
            }
        }
    }
}

@Composable
fun ScheduleContent(schedules: List<CourseSchedule>) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Column {
            repeat(12) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ScheduleDimens.CellHeight)
                        .border(width = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                )
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val dayWidth = maxWidth / 7

            schedules.forEach { course ->
                val xOffset = dayWidth * (course.dayOfWeek - 1)
                val yOffset = ScheduleDimens.CellHeight * (course.startNode - 1)
                val cardHeight = ScheduleDimens.CellHeight * course.step

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
}

@Composable
fun CourseCard(
    course: CourseSchedule,
    modifier: Modifier = Modifier
) {
    // 根据 colorIndex 取色
    val bgColor = CoursePalettes.Macaron.getOrElse(course.colorIndex) { Color.Gray }

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
            color = Color.White,
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
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}