package com.cumtb.mineplus.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cumtb.mineplus.ui.theme.Dimens
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cumtb.mineplus.R
import com.cumtb.mineplus.ui.schedule.ScheduleScreen
import com.cumtb.mineplus.ui.schedule.ScheduleViewModel
import com.cumtb.mineplus.ui.today.TodayScheduleScreen
import com.cumtb.mineplus.ui.grade.GradeScreen
import com.cumtb.mineplus.ui.service.ServiceScreen
import com.cumtb.mineplus.ui.settings.SettingsScreen
import com.cumtb.mineplus.ui.reminder.ReminderSettingsScreen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Icon
import androidx.compose.foundation.isSystemInDarkTheme

private object MainRoutes {
    const val Today = "main/today"
    const val Week = "main/week"
    const val Grades = "main/grades"
    const val More = "main/more"
}

private object ServiceRoutes {
    const val Service = "service"
    const val Settings = "settings"
    const val ReminderSettings = "reminder_settings"
}

private data class MainBottomItem(
    val route: String,
    val label: String,
    val selectedIcon: @Composable () -> Unit,
    val unselectedIcon: @Composable () -> Unit
)

@Composable
fun MainScreen(
    onNavigateToAbout: () -> Unit,
    onRelogin: () -> Unit,
    onNavigateToLogin: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val items = listOf(
        MainBottomItem(
            route = MainRoutes.Today,
            label = "今日",
            selectedIcon = { Icon(painter = painterResource(id = R.drawable.clock_time_five), contentDescription = null) },
            unselectedIcon = { Icon(painter = painterResource(id = R.drawable.clock_time_five_outline), contentDescription = null) }
        ),
        MainBottomItem(
            route = MainRoutes.Week,
            label = "课表",
            selectedIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
            unselectedIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) }
        ),
        MainBottomItem(
            route = MainRoutes.Grades,
            label = "成绩",
            selectedIcon = { Icon(Icons.Filled.School, contentDescription = null) },
            unselectedIcon = { Icon(Icons.Outlined.School, contentDescription = null) }
        ),
        MainBottomItem(
            route = MainRoutes.More,
            label = "服务",
            selectedIcon = { Icon(painter = painterResource(id = R.drawable.apps_box), contentDescription = null) },
            unselectedIcon = { Icon(Icons.Filled.Apps, contentDescription = null) }
        )
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val navigateToMainTab: (String) -> Unit = remember(navController, currentRoute) {
        { route: String ->
            if (currentRoute == route) return@remember
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    // App 进入主界面后就提前预热“周课表”，避免第一次点开才触发 Room 冷启动。
    // 放在这里比放在 ScheduleScreen 更早，但仍然已经过了 Splash/Login 的关键路径。
    val scheduleViewModel: ScheduleViewModel = hiltViewModel()
    LaunchedEffect(Unit) {
        scheduleViewModel.warmUpAfterAppStart()
    }

    Scaffold(
        bottomBar = {
            // Tunables for bottom bar density.
            val navBarHeight: Dp = 72.dp
            // Keep the nav content anchored in the same visual position even if the bar height changes.
            val contentDownOffset: Dp = 8.dp

            Box(modifier = Modifier.fillMaxWidth()) {
                val dividerColor = if (isSystemInDarkTheme()) {
                    // dark theme: use a light divider
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                } else {
                    // light theme: use a darker divider
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                }

                // Draw the bar first...
                NavigationBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(navBarHeight),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    windowInsets = NavigationBarDefaults.windowInsets
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = contentDownOffset)
                            .padding(horizontal = Dimens.screenPaddingCompact),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        items.forEach { item ->
                            val selected = currentRoute == item.route

                            val itemColors = NavigationBarItemDefaults.colors(
                                // Hide the default selected indicator "bubble".
                                indicatorColor = MaterialTheme.colorScheme.surface,
                                selectedIconColor = MaterialTheme.colorScheme.onSurface,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val dotSize = 4.dp
                            val dotTopSpacing = 4.dp
                            val labelUpOffset: Dp = 8.dp

                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToMainTab(item.route) },
                                icon = {
                                    // The icon inherits tint from NavigationBarItem colors.
                                    if (selected) item.selectedIcon() else item.unselectedIcon()
                                },
                                label = {
                                    Column(
                                        modifier = Modifier.offset(y = -labelUpOffset),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(item.label)
                                        Spacer(modifier = Modifier.height(dotTopSpacing))

                                        if (selected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(dotSize)
                                                    .background(
                                                        color = itemColors.selectedIconColor,
                                                        shape = CircleShape
                                                    )
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.size(dotSize))
                                        }
                                    }
                                },
                                alwaysShowLabel = true,
                                colors = itemColors
                            )
                        }
                    }
                }

                // ...then overlay the top divider so it can't be covered.
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = dividerColor,
                    thickness = 0.5.dp
                )
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
                        onNavigateToWeek = { navigateToMainTab(MainRoutes.Week) },
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
                    GradeScreen()
                }

                composable(MainRoutes.More) {
                    ServiceScreen(
                        navController = navController
                    )
                }
                
                composable(ServiceRoutes.Settings) {
                    SettingsScreen(
                        navController = navController
                    )
                }
                
                composable(ServiceRoutes.ReminderSettings) {
                    ReminderSettingsScreen(
                        navController = navController
                    )
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
