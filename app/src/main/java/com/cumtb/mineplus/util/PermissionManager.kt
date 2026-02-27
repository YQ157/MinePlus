package com.cumtb.mineplus.util

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import com.hjq.device.compat.DeviceOs
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionLists
import android.content.pm.PackageManager

/**
 * 权限管理工具类
 * 使用 XXPermissions 库来处理各种权限跳转和检测
 */
class PermissionManager(private val context: Context) {

    /**
     * 检查自启动权限状态
     *
     * 说明：自启动（开机自启 / 后台自启）在大多数厂商 ROM 中属于私有开关，
     * Android 没有统一、可靠的检测 API，因此只能引导用户去设置页面手动确认。
     */
    fun checkAutoStartPermission(): Boolean {
        return try {
            // 无法可靠检测，默认认为未开启（让 UI 引导用户设置）
            false
        } catch (e: Exception) {
            Log.w("PermissionManager", "检查自启动权限失败", e)
            false
        }
    }

    /**
     * 检查电池优化白名单状态（忽略电池优化）
     */
    fun checkBatteryOptimizationExemption(): Boolean {
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } catch (e: Exception) {
            Log.w("PermissionManager", "检查电池优化状态失败", e)
            false
        }
    }

    /**
     * 检查后台运行/后台限制状态
     *
     * 说明：这里检测的是 AOSP 标准的“后台受限（Background Restricted）”。
     * 厂商自定义的省电策略无法完全覆盖，所以只能做到“部分可靠”。
     */
    fun checkBackgroundPermission(): Boolean {
        return try {
            // 后台限制（AOSP 标准）在 Android 9+ 才有，但不同编译 SDK 的 android.jar 可能不包含该 API。
            // 为了保证编译通过，这里用反射读取 ActivityManager#isBackgroundRestricted。
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val method = am.javaClass.getMethod("isBackgroundRestricted")
                val restricted = method.invoke(am) as? Boolean
                // restricted == true 表示被限制 => 不满足后台运行；null/false 视作未限制
                restricted != true
            } else {
                true
            }
        } catch (e: Throwable) {
            Log.w("PermissionManager", "检查后台权限失败", e)
            // 检测失败时不阻塞功能，默认认为满足（避免误伤）
            true
        }
    }

    /**
     * 检查锁屏显示权限（非悬浮窗）
     *
     * 说明：
     * - “锁屏显示”通常依赖【通知】在锁屏上的展示能力（以及用户的锁屏通知设置），并不是一个 Android 标准权限。
     * - 各 ROM 里的“锁屏显示/后台弹出界面”等开关属于厂商私有能力，无法可靠检测。
     *
     * 因此这里返回 true（不阻塞功能），配合 requestLockScreenDisplayPermission() 引导用户去系统设置确认。
     */
    fun checkLockScreenPermission(): Boolean {
        return true
    }

    /**
     * Android 13+：通知运行时权限
     */
    fun checkNotificationPermission(): Boolean {
        return try {
            XXPermissions.isGrantedPermission(
                context,
                PermissionLists.getPostNotificationsPermission()
            )
        } catch (e: Throwable) {
            Log.w("PermissionManager", "检查通知权限失败", e)
            // Android 13 以下没有该运行时权限，检测失败时默认不阻塞
            true
        }
    }

    fun requestNotificationPermission(onResult: ((granted: Boolean) -> Unit)? = null) {
        try {
            XXPermissions.with(context)
                .permission(PermissionLists.getPostNotificationsPermission())
                .request(object : OnPermissionCallback {
                    override fun onResult(
                        grantedList: List<com.hjq.permissions.permission.base.IPermission>,
                        deniedList: List<com.hjq.permissions.permission.base.IPermission>
                    ) {
                        onResult?.invoke(deniedList.isEmpty())
                    }
                })
        } catch (e: Throwable) {
            Log.e("PermissionManager", "请求通知权限失败", e)
            // 跳转到通知设置页让用户手动开启
            try {
                startNotificationSettings()
            } catch (_: Throwable) {
                fallbackToAppDetailsSettings()
            }
            onResult?.invoke(false)
        }
    }

    /**
     * Android 12+：精确闹钟（特殊权限）
     */
    fun checkExactAlarmPermission(): Boolean {
        return try {
            XXPermissions.isGrantedPermission(
                context,
                PermissionLists.getScheduleExactAlarmPermission()
            )
        } catch (e: Throwable) {
            Log.w("PermissionManager", "检查精确闹钟权限失败", e)
            true
        }
    }

    fun requestExactAlarmPermission() {
        try {
            XXPermissions.startPermissionActivity(
                context,
                PermissionLists.getScheduleExactAlarmPermission()
            )
        } catch (e: Throwable) {
            Log.e("PermissionManager", "跳转精确闹钟设置失败", e)
            fallbackToAppDetailsSettings()
        }
    }

    /**
     * 跳转到自启动权限设置页面
     *
     * 说明：自启动没有统一设置页，这里只能尽量跳到“权限设置/应用详情”让用户继续操作。
     */
    fun requestAutoStartPermission() {
        Log.d("PermissionManager", "请求自启动权限（引导）")
        try {
            // 让框架根据 ROM 做权限设置页跳转兜底
            XXPermissions.startPermissionActivity(context)
        } catch (e: Exception) {
            Log.e("PermissionManager", "启动自启动权限设置失败", e)
            fallbackToAppDetailsSettings()
        }
    }

    /**
     * 跳转到电池优化设置页面（忽略电池优化）
     */
    fun requestBatteryOptimizationExemption() {
        Log.d("PermissionManager", "请求电池优化白名单权限")

        // 1) vivo（OriginOS/FuntouchOS）优先跳到它更常用的“耗电管理/后台高耗电”入口
        if (DeviceOs.isOriginOs() || DeviceOs.isFuntouchOs()) {
            if (startVivoPowerManagerPage()) {
                return
            }
        }

        // 2) 通用：走 XXPermissions（会尝试 ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS 等）
        try {
            XXPermissions.startPermissionActivity(
                context,
                PermissionLists.getRequestIgnoreBatteryOptimizationsPermission()
            )
            return
        } catch (e: Exception) {
            Log.e("PermissionManager", "启动电池优化设置失败，使用系统 fallback", e)
        }

        // 3) 最后兜底
        fallbackToRequestIgnoreBatteryOptimizations()
    }

    private fun startVivoPowerManagerPage(): Boolean {
        val candidates = listOf(
            // iManager - 耗电管理（不同版本包名/Activity 可能不同，做多候选兜底）
            ComponentName("com.vivo.abe", "com.vivo.applicationbehaviorengine.ui.ExcessivePowerManagerActivity"),
            ComponentName("com.iqoo.powersaving", "com.iqoo.powersaving.PowerSavingManagerActivity"),
            ComponentName("com.vivo.abeui", "com.vivo.abeui.highpower.HighPowerMainActivity"),
            ComponentName("com.vivo.abeui", "com.vivo.abeui.highpower.ExcessivePowerActivity"),
            ComponentName("com.vivo.abe", "com.vivo.applicationbehaviorengine.ui.HighPowerDetailActivity")
        )

        for (component in candidates) {
            try {
                val intent = Intent().setComponent(component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                val pm: PackageManager = context.packageManager
                val resolved = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                if (resolved != null) {
                    context.startActivity(intent)
                    Log.d("PermissionManager", "跳转 vivo 电量/后台高耗电页: $component")
                    return true
                }
            } catch (_: Exception) {
                // try next
            }
        }

        // 候选都不可用：跳应用详情（用户仍可在“电量/后台”里继续找）
        return false
    }

    /**
     * 跳转到后台相关设置页面
     */
    fun requestBackgroundPermission() {
        Log.d("PermissionManager", "请求后台运行权限（引导）")
        try {
            // 优先跳应用详情页（不少 ROM 的“电池/后台限制”入口在这里）
            XXPermissions.startPermissionActivity(context)
        } catch (e: Exception) {
            Log.e("PermissionManager", "启动后台权限设置失败", e)
            fallbackToAppDetailsSettings()
        }
    }

    /**
     * 跳转到锁屏显示相关设置（优先通知设置页）
     */
    fun requestOverlayPermission() {
        // 兼容旧调用：原 UI/VM 使用 requestOverlayPermission 作为“锁屏显示”按钮。
        // 现在改为引导用户去通知设置/应用详情页确认“锁屏通知”相关选项。
        Log.d("PermissionManager", "请求锁屏显示权限（引导到通知设置）")
        try {
            startNotificationSettings()
        } catch (e: Exception) {
            Log.e("PermissionManager", "启动通知设置失败", e)
            fallbackToAppDetailsSettings()
        }
    }

    /**
     * （可选）如果你们确实需要“任意界面上层显示浮层”，再用这个检测/跳转。
     */
    fun checkOverlayPermission(): Boolean {
        return try {
            Settings.canDrawOverlays(context)
        } catch (_: Exception) {
            false
        }
    }

    fun requestOverlayWindowPermission() {
        Log.d("PermissionManager", "请求悬浮窗权限")
        try {
            XXPermissions.startPermissionActivity(
                context,
                PermissionLists.getSystemAlertWindowPermission()
            )
        } catch (e: Exception) {
            Log.e("PermissionManager", "启动悬浮窗设置失败，使用系统 fallback", e)
            fallbackToManageOverlayPermission()
        }
    }

    private fun startNotificationSettings() {
        val intent: Intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", context.packageName, null))
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun fallbackToRequestIgnoreBatteryOptimizations() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("PermissionManager", "fallback: REQUEST_IGNORE_BATTERY_OPTIMIZATIONS 失败", e)
            // 再降级到列表页或应用详情
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                Log.e("PermissionManager", "fallback: IGNORE_BATTERY_OPTIMIZATION_SETTINGS 也失败", e2)
                fallbackToAppDetailsSettings()
            }
        }
    }

    private fun fallbackToManageOverlayPermission() {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("PermissionManager", "fallback: MANAGE_OVERLAY_PERMISSION 失败", e)
            fallbackToAppDetailsSettings()
        }
    }

    /**
     * Fallback 到应用详情设置页面
     */
    private fun fallbackToAppDetailsSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.d("PermissionManager", "fallback 到应用详情设置页面")
        } catch (e: Exception) {
            Log.e("PermissionManager", "fallback 启动也失败", e)
        }
    }

    /**
     * 检查所有课前提醒所需权限是否都已获得
     */
    fun checkAllReminderPermissions(): Boolean {
        return checkAutoStartPermission() &&
            checkBatteryOptimizationExemption() &&
            checkBackgroundPermission() &&
            checkLockScreenPermission()
    }

    /**
     * 获取缺失的权限列表
     */
    fun getMissingPermissions(): List<String> {
        val missing = mutableListOf<String>()
        if (!checkAutoStartPermission()) missing.add("自启动权限")
        if (!checkBatteryOptimizationExemption()) missing.add("电池优化白名单")
        if (!checkBackgroundPermission()) missing.add("后台运行权限")
        if (!checkLockScreenPermission()) missing.add("锁屏显示权限")
        return missing
    }
}