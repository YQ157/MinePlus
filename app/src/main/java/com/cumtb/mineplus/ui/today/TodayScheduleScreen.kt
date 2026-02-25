package com.cumtb.mineplus.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cumtb.mineplus.data.model.CourseSchedule
import com.cumtb.mineplus.ui.today.components.TodayCourseCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScheduleScreen(
    onNavigateToWeek: () -> Unit,
    onRelogin: () -> Unit,
    onNavigateToAbout: () -> Unit,
    viewModel: TodayScheduleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pullState = rememberPullToRefreshState()

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
                // Match Week screen: white/neutral top bar.
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
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
                .padding(horizontal = 12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 顶部标题已移到 TopAppBar，内容从列表开始。

                when {
                    uiState.hasNoData -> {
                        TodayEmptyState(
                            title = "还没有课表数据",
                            actionText = "重新登录",
                            onAction = onRelogin
                        )
                    }

                    uiState.courses.isEmpty() -> {
                        TodayEmptyState(
                            title = "今天没有课",
                            actionText = "查看周课表",
                            onAction = onNavigateToWeek
                        )
                    }

                    else -> {
                        TodayCourseList(courses = uiState.courses)
                    }
                }
            }

            if (pullState.isRefreshing && !uiState.isLoading) {
                LaunchedEffect(Unit) {
                    viewModel.onRefresh()
                }
            }

            PullToRefreshContainer(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp),
                state = pullState
            )
        }
    }
}

@Composable
private fun TodayCourseList(courses: List<CourseSchedule>) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items = courses, key = { it.id }) { course ->
            TodayCourseCard(course = course)
        }
    }
}

@Composable
private fun TodayEmptyState(
    title: String,
    actionText: String,
    onAction: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            TextButton(
                onClick = onAction,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(actionText)
            }
        }
    }
}
