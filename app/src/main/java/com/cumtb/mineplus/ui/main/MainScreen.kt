package com.cumtb.mineplus.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cumtb.mineplus.ui.schedule.ScheduleScreen
import com.cumtb.mineplus.ui.schedule.ScheduleViewModel
import com.cumtb.mineplus.ui.today.TodayScheduleScreen

private object MainRoutes {
    const val Today = "main/today"
    const val Week = "main/week"
    const val Grades = "main/grades"
    const val More = "main/more"
}

private data class MainBottomItem(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit
)

@Composable
fun MainScreen(
    onNavigateToAbout: () -> Unit,
    onRelogin: () -> Unit,
    onNavigateToLogin: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val items = listOf(
        MainBottomItem(MainRoutes.Today, "今日") { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
        MainBottomItem(MainRoutes.Week, "周课表") { Icon(Icons.Filled.DateRange, contentDescription = null) },
        MainBottomItem(MainRoutes.Grades, "成绩") { Icon(Icons.Filled.School, contentDescription = null) },
        MainBottomItem(MainRoutes.More, "其他") { Icon(Icons.Filled.MoreHoriz, contentDescription = null) }
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // App 进入主界面后就提前预热“周课表”，避免第一次点开才触发 Room 冷启动。
    // 放在这里比放在 ScheduleScreen 更早，但仍然已经过了 Splash/Login 的关键路径。
    val scheduleViewModel: ScheduleViewModel = hiltViewModel()
    LaunchedEffect(Unit) {
        scheduleViewModel.warmUpAfterAppStart()
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEach { item ->
                    val selected = currentRoute == item.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = item.icon,
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        // MainScreen has no topBar, but Scaffold's innerPadding can still carry a top inset
        // depending on window inset handling. We only need to offset content for the bottom
        // NavigationBar, so keep the top edge flush.
        val contentPadding = remember(innerPadding) {
            PaddingValues(bottom = innerPadding.calculateBottomPadding())
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = MainRoutes.Today
            ) {
                composable(MainRoutes.Today) {
                    TodayScheduleScreen(
                        onNavigateToWeek = {
                            navController.navigate(MainRoutes.Week) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToAbout = onNavigateToAbout,
                        onRelogin = onNavigateToLogin
                    )
                }

                composable(MainRoutes.Week) {
                    ScheduleScreen(
                        onNavigateToAbout = onNavigateToAbout,
                        onRelogin = onRelogin
                    )
                }

                composable(MainRoutes.Grades) {
                    PlaceholderTabScreen(title = "成绩", description = "暂未开放")
                }

                composable(MainRoutes.More) {
                    PlaceholderTabScreen(title = "其他", description = "暂未开放")
                }
            }
        }
    }
}

@Composable
private fun PlaceholderTabScreen(
    title: String,
    description: String
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "$title\n$description")
    }
}
