package com.cumtb.mineplus.ui.reminder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cumtb.mineplus.data.preference.ReminderPreferences
import com.cumtb.mineplus.util.PermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReminderSettingsViewModel @Inject constructor(
    application: Application,
    private val reminderPrefs: ReminderPreferences,
    private val reminderScheduler: com.cumtb.mineplus.service.CourseReminderScheduler
) : AndroidViewModel(application) {

    private val permissionManager = PermissionManager(application)

    private val _uiState = MutableStateFlow(ReminderSettingsUiState())
    val uiState: StateFlow<ReminderSettingsUiState> = _uiState.asStateFlow()

    init {
        loadReminderSettings()
    }

    fun toggleReminder(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isReminderEnabled = enabled)
            saveReminderSetting(enabled)

            if (enabled) {
                refreshPermissions()
                // 立即调度一次（best-effort）
                try {
                    reminderScheduler.cancelAllReminders()
                    reminderScheduler.scheduleRemindersForNextThreeDays()
                } catch (_: Exception) {
                }
            } else {
                // 关闭时取消并清空持久化列表
                try {
                    reminderScheduler.cancelAllReminders()
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun loadReminderSettings() {
        viewModelScope.launch {
            try {
                val enabled = reminderPrefs.reminderEnabled.first()
                _uiState.value = _uiState.value.copy(isReminderEnabled = enabled)
                if (enabled) {
                    refreshPermissions()
                }
            } catch (e: Exception) {
                // 默认情况下不启用提醒
            }
        }
    }

    private suspend fun saveReminderSetting(enabled: Boolean) {
        try {
            reminderPrefs.setReminderEnabled(enabled)
        } catch (_: Exception) {
            // 保存失败，但不影响UI操作
        }
    }

    /**
     * 刷新所有权限状态
     */
    private fun refreshPermissions() {
        viewModelScope.launch {
            val currentState = _uiState.value
            _uiState.value = currentState.copy(
                // 自启动/锁屏显示/后台运行：无法可靠检测，UI 仅做引导，不显示“已授权”
                hasAutoStartPermission = false,
                hasLockScreenDisplayPermission = false,
                hasBackgroundPermission = false,
                hasBatteryOptimizationExemption = permissionManager.checkBatteryOptimizationExemption(),
                hasNotificationPermission = permissionManager.checkNotificationPermission(),
                hasExactAlarmPermission = permissionManager.checkExactAlarmPermission()
            )
        }
    }

    /**
     * 给 UI 使用：当页面回到前台时立即刷新一次权限状态。
     */
    fun refreshPermissionsNow() {
        if (_uiState.value.isReminderEnabled) {
            refreshPermissions()
        }
    }

    fun requestAutoStartPermission() {
        permissionManager.requestAutoStartPermission()
    }

    fun requestBatteryOptimizationExemption() {
        permissionManager.requestBatteryOptimizationExemption()
        viewModelScope.launch {
            kotlinx.coroutines.delay(1200)
            refreshPermissions()
        }
    }

    fun requestBackgroundPermission() {
        permissionManager.requestBackgroundPermission()
        viewModelScope.launch {
            kotlinx.coroutines.delay(1200)
            refreshPermissions()
        }
    }

    fun requestLockScreenDisplayPermission() {
        // 锁屏显示：引导到通知设置页 / 应用设置
        permissionManager.requestOverlayPermission()
    }

    fun requestNotificationPermission() {
        permissionManager.requestNotificationPermission {
            // 立即刷新一次
            refreshPermissions()
        }
    }

    fun requestExactAlarmPermission() {
        permissionManager.requestExactAlarmPermission()
        viewModelScope.launch {
            kotlinx.coroutines.delay(1200)
            refreshPermissions()
        }
    }
}

data class ReminderSettingsUiState(
    val isReminderEnabled: Boolean = false,
    // 无法可靠检测的项（只用于 UI 引导，不用于显示“已授权”）
    val hasAutoStartPermission: Boolean = false,
    val hasLockScreenDisplayPermission: Boolean = false,
    // 可检测项
    val hasBatteryOptimizationExemption: Boolean = false,
    val hasBackgroundPermission: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val hasExactAlarmPermission: Boolean = false,
    val isLoading: Boolean = false
)