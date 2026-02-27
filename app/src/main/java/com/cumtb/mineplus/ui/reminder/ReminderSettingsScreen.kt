package com.cumtb.mineplus.ui.reminder

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.cumtb.mineplus.ui.theme.Dimens
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSettingsScreen(
    navController: NavController,
    viewModel: ReminderSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 页面回到前台时自动刷新一次（用户从系统设置返回时体验更好）
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, uiState.isReminderEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionsNow()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "课前提醒",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
            
            // 添加顶部分割线
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                thickness = 0.5.dp
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.screenPaddingCompact)
        ) {
            // 主开关
            SettingItemWithSwitch(
                title = "课前提醒",
                description = "开启后将在每节课前15分钟提醒您",
                icon = { Icon(imageVector = Icons.Filled.Alarm, contentDescription = null) },
                checked = uiState.isReminderEnabled,
                onCheckedChange = { viewModel.toggleReminder(it) }
            )
            
            Spacer(modifier = Modifier.height(Dimens.screenPaddingCompact))
            
            // 权限状态显示
            if (uiState.isReminderEnabled) {
                Text(
                    text = "权限状态",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(Dimens.tiny2))
                
                // 自启动：无法可靠检测 -> 仅引导
                PermissionStatusCard(
                    permissionName = "自启动权限",
                    description = "建议在系统设置中允许应用开机/后台自启动（无法自动检测）",
                    isGranted = null,
                    onActionClick = { viewModel.requestAutoStartPermission() }
                )
                
                Spacer(modifier = Modifier.height(Dimens.small2))
                
                // 电池优化白名单
                PermissionStatusCard(
                    permissionName = "电池优化白名单",
                    description = "防止系统限制后台活动",
                    isGranted = uiState.hasBatteryOptimizationExemption,
                    onActionClick = { viewModel.requestBatteryOptimizationExemption() }
                )
                
                Spacer(modifier = Modifier.height(Dimens.small2))
                
                // 后台限制：不可统一可靠检测 -> 仅引导
                PermissionStatusCard(
                    permissionName = "后台运行设置",
                    description = "建议关闭系统对本应用的后台限制/省电策略（无法自动检测）",
                    isGranted = null,
                    onActionClick = { viewModel.requestBackgroundPermission() }
                )

                Spacer(modifier = Modifier.height(Dimens.small2))

                // 通知权限（Android 13+）
                PermissionStatusCard(
                    permissionName = "通知权限",
                    description = "允许发送课前提醒通知",
                    isGranted = uiState.hasNotificationPermission,
                    onActionClick = { viewModel.requestNotificationPermission() }
                )

                Spacer(modifier = Modifier.height(Dimens.small2))

                // 精确闹钟（Android 12+）
                PermissionStatusCard(
                    permissionName = "精确闹钟权限",
                    description = "保证提醒尽量准点（Android 12+）",
                    isGranted = uiState.hasExactAlarmPermission,
                    onActionClick = { viewModel.requestExactAlarmPermission() }
                )
                
                Spacer(modifier = Modifier.height(Dimens.small2))
                
                // 锁屏显示：无法可靠检测 -> 引导到通知设置 / 锁屏通知
                PermissionStatusCard(
                    permissionName = "锁屏显示（通知）",
                    description = "在系统通知设置中开启锁屏显示（无法自动检测）",
                    isGranted = null,
                    onActionClick = { viewModel.requestLockScreenDisplayPermission() }
                )
                
                Spacer(modifier = Modifier.height(Dimens.medium))
                
                // 温馨提示
                InfoCard(
                    title = "💡 温馨提示",
                    content = """
                        • 建议优先开启：通知权限、精确闹钟、电池优化白名单。
                        • “自启动 / 后台运行 / 锁屏显示”在部分机型属于厂商私有开关，应用无法可靠检测是否开启，因此这里仅提供入口引导。
                    """.trimIndent()
                )
            }
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.medium)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            Spacer(modifier = Modifier.height(Dimens.small2))
            
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun SettingItemWithSwitch(
    title: String,
    description: String,
    icon: @Composable (() -> Unit)? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.small2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            icon()
            Spacer(modifier = Modifier.width(Dimens.small2))
        }
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun PermissionStatusCard(
    permissionName: String,
    description: String = "",
    isGranted: Boolean?,
    actionText: String = "去设置",
    onActionClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.screenPaddingCompact),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = permissionName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (isGranted != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isGranted) "已授权" else "未授权",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isGranted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
            
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(text = actionText)
            }
        }
    }
}