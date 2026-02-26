package com.cumtb.mineplus.ui.today

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random
import androidx.hilt.navigation.compose.hiltViewModel
import com.cumtb.mineplus.data.model.CourseSchedule
import com.cumtb.mineplus.ui.today.components.TodayCourseCard
import java.time.LocalTime
import com.cumtb.mineplus.util.computeTodayCourseTimeStatuses
import com.cumtb.mineplus.ui.theme.CoursePalettes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScheduleScreen(
    onNavigateToWeek: () -> Unit,
    onRelogin: () -> Unit,
    onNavigateToAbout: () -> Unit,
    viewModel: TodayScheduleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedPaletteId by viewModel.coursePaletteId.collectAsState()
    val pullState = rememberPullToRefreshState()

    val context = LocalContext.current

    // Bump this when the user triggers a pull-to-refresh so the empty-state random label can re-roll.
    var refreshNonce by remember { mutableStateOf(0) }

    // 仅失败提示：Toast
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TodayScheduleViewModel.UiEvent.RefreshFailed -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                else -> Unit
            }
        }
    }

    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) pullState.endRefresh()
    }

    var menuExpanded by remember { mutableStateOf(false) }
    val menuIconSize = 36.dp

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
                    Text(
                        text = uiState.title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
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
                        // Course palette selector (English)
                        CoursePalettes.PaletteId.entries.forEach { id ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (id == selectedPaletteId) "✓ ${id.englishName}" else id.englishName,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.onCoursePaletteSelected(id)
                                }
                            )
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            thickness = 0.5.dp
                        )

                        DropdownMenuItem(
                            text = { Text(text = "关于", style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                menuExpanded = false
                                onNavigateToAbout()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(text = "重新登录", style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                menuExpanded = false
                                onRelogin()
                            }
                        )
                    }
                },
                // Match Week screen: white/neutral top bar with top divider.
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
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .nestedScroll(pullState.nestedScrollConnection)
                .padding(horizontal = 12.dp)
                .clipToBounds()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 顶部标题已移到 TopAppBar，内容从列表开始。

                when {
                    uiState.hasNoData -> {
                        TodayEmptyStateScrollHost(
                            title = "还没有课表数据",
                            actionText = "重新登录",
                            onAction = onRelogin,
                            refreshNonce = refreshNonce
                        )
                    }

                    uiState.courses.isEmpty() -> {
                        TodayEmptyStateScrollHost(
                            title = "", // 不显示“今天没有课”这行副标题
                            actionText = "查看周课表",
                            onAction = onNavigateToWeek,
                            refreshNonce = refreshNonce
                        )
                    }

                    else -> {
                        TodayCourseList(courses = uiState.courses, now = uiState.now.toLocalTime())
                    }
                }
            }

            if (pullState.isRefreshing && !uiState.isLoading) {
                // 用 refreshing 作为 key，避免因重组多次启动同一个 Unit effect
                LaunchedEffect(pullState.isRefreshing) {
                    // User explicitly started a refresh gesture: re-roll the empty-state random message.
                    refreshNonce++
                    viewModel.onRefreshTriggered(TodayScheduleViewModel.RefreshSource.User)
                }
            }

            PullToRefreshContainer(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    // Move it up slightly so idle state won't peek.
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) {
                            placeable.placeRelative(0, (-placeable.height / 2))
                        }
                    },
                state = pullState
            )
        }
    }
}

@Composable
private fun TodayCourseList(courses: List<CourseSchedule>, now: LocalTime) {
    val statuses = remember(courses, now) {
        computeTodayCourseTimeStatuses(
            now = now,
            courses = courses.map { it.rawStartTime to it.rawEndTime }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val minBlankSpace = 200.dp  // 固定值替代maxHeight

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = courses.withIndex().toList(),
                key = { it.value.id }
            ) { indexed ->
                val idx = indexed.index
                val course = indexed.value
                val status = statuses.getOrNull(idx) ?: com.cumtb.mineplus.util.CourseTimeStatus.UNKNOWN
                TodayCourseCard(course = course, status = status)
            }

            item(key = "bottom_filler") {
                Spacer(modifier = Modifier.height(minBlankSpace))
            }
        }
    }
}

// 无课状态的文案数组
private val NO_CLASS_MESSAGES = listOf(
    "今日无课，合法摸鱼 🎣",
    "难得空闲，去吃点什么呢？ 🍜",
    "难得的空闲，把时间还给自己 ⏳",
    "今日无课，宜：发呆、晒太阳 ☀️",
    "系统建议立即启动\"躺平\"模式 🛌",
    "今日无课，要不要去图书馆？ 📚",
    "自由时间已到账 💰"
)

@Composable
private fun TodayEmptyState(
    title: String,
    actionText: String,
    onAction: () -> Unit,
    refreshNonce: Int
) {
    // 随机选择一条文案
    val randomMessage = remember(title, refreshNonce) {
        NO_CLASS_MESSAGES.random(Random(System.currentTimeMillis()))
    }

    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                // 注意：不要在 LazyColumn 的 item 里再用 fillMaxHeight()，否则会把 item 撑到异常尺寸，影响视觉居中
                .padding(horizontal = 24.dp)
        ) {
            // 主标题（随机标签）
            Text(
                text = randomMessage,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 1.5.sp,
                    letterSpacing = 0.8.sp
                ),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )

            // 副标题（固定文案）
            if (title.isNotBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp
                    ),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    maxLines = 1
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 按钮容器 - 确保按钮和文字水平对齐
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                LaunchedEffect(interactionSource) {
                    interactionSource.interactions.collect { interaction ->
                        when (interaction) {
                            is androidx.compose.foundation.interaction.PressInteraction.Press -> isPressed = true
                            is androidx.compose.foundation.interaction.PressInteraction.Release -> isPressed = false
                            is androidx.compose.foundation.interaction.PressInteraction.Cancel -> isPressed = false
                        }
                    }
                }

                val gap = 2.dp
                val measurer = androidx.compose.ui.text.rememberTextMeasurer()
                val textStyle = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 17.sp
                )

                // A calmer, more mature blue that adapts to light/dark (and dynamic color when enabled).
                val actionColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)

                // 宽度测量：用于把“查看周课表”这段文字的几何中心严格放在屏幕水平中心。
                val density = androidx.compose.ui.platform.LocalDensity.current
                val textWidthPx = remember(actionText, textStyle, measurer) {
                    measurer.measure(text = actionText, style = textStyle, maxLines = 1).size.width
                }
                val arrowWidthPx = remember(textStyle, measurer) {
                    measurer.measure(text = ">", style = textStyle, maxLines = 1).size.width
                }
                val gapPx = with(density) { gap.roundToPx() }

                val textWidthDp = with(density) { textWidthPx.toDp() }
                // 左侧补偿：相当于给 group 增加一个“看不见的左箭头槽”，让文字中心不被右箭头拉偏。
                val leftCompensationDp = with(density) { (arrowWidthPx + gapPx).toDp() }

                TextButton(
                    onClick = onAction,
                    modifier = Modifier
                        .scale(if (isPressed) 0.95f else 1.0f)
                        .padding(vertical = 8.dp, horizontal = 0.dp),
                    interactionSource = interactionSource,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = actionColor)
                ) {
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 左侧占位：把文字中心往右推半个箭头宽度，从而让文字本身居中在屏幕中线。
                        Spacer(modifier = Modifier.width(leftCompensationDp))

                        Box(modifier = Modifier.width(textWidthDp)) {
                            Text(
                                text = actionText,
                                style = textStyle,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Text(
                            text = ">",
                            style = textStyle,
                            modifier = Modifier.padding(start = gap)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayEmptyStateScrollHost(
    title: String,
    actionText: String,
    onAction: () -> Unit,
    refreshNonce: Int
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val minBlankSpace = 200.dp  // 固定值替代maxHeight

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item(key = "empty_state") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillParentMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    TodayEmptyState(
                        title = title,
                        actionText = actionText,
                        onAction = onAction,
                        refreshNonce = refreshNonce
                    )
                }
            }
            // Add filler so the whole screen is part of the scroll container.
            item(key = "bottom_filler") {
                Spacer(modifier = Modifier.height(minBlankSpace))
            }
        }
    }
}
