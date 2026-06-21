package com.cumtb.mineplus.ui

import android.util.Log
import android.webkit.CookieManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cumtb.mineplus.data.preference.AppPreferences
import com.cumtb.mineplus.data.preference.CredentialStorage
import com.cumtb.mineplus.data.repository.CourseRepository
import com.cumtb.mineplus.data.repository.GradeRepository
import com.cumtb.mineplus.service.CourseReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: CourseRepository,
    private val gradeRepository: GradeRepository,
    private val prefs: AppPreferences,
    private val credentialStorage: CredentialStorage,
    private val reminderScheduler: CourseReminderScheduler,
    private val reminderPrefs: com.cumtb.mineplus.data.preference.ReminderPreferences
) : ViewModel() {

    data class SavedCredentialsState(
        val rememberPassword: Boolean,
        val username: String,
        val password: String
    )

    suspend fun loadSavedCredentials(): SavedCredentialsState {
        val remember = prefs.rememberPassword.first()
        return SavedCredentialsState(
            rememberPassword = remember,
            username = if (remember) credentialStorage.getUsername().orEmpty() else "",
            password = if (remember) credentialStorage.getPassword().orEmpty() else ""
        )
    }

    suspend fun hasRememberedCredentials(): Boolean {
        val remember = prefs.rememberPassword.first()
        return remember && credentialStorage.hasCredentials()
    }

    suspend fun persistCredentials(
        rememberPassword: Boolean,
        username: String,
        password: String
    ) {
        prefs.setRememberPassword(rememberPassword)
        if (rememberPassword) {
            credentialStorage.save(username, password)
        } else {
            credentialStorage.clear()
        }
    }

    /**
     * 手动 WebView 登录成功时使用：允许保留“记住密码”开关和用户名，但不保存密码。
     */
    suspend fun persistUsernameOnlyWhenRememberEnabled(
        rememberPassword: Boolean,
        username: String
    ) {
        prefs.setRememberPassword(rememberPassword)
        if (rememberPassword) {
            credentialStorage.saveUsernameOnly(username)
        } else {
            credentialStorage.clear()
        }
    }

    /**
     * 登录成功后触发：
     * - 先尝试做一次“首次同步”（可设置超时），成功就直接带数据进入；
     * - 失败/超时也要进入 MainScreen，但此时数据库为空（或保持旧数据），UI 显示空态；
     * - 全程不允许因为异常导致闪退。
     */
    fun onLoginSuccessFetchThenNavigate(
        onNavigateToMain: () -> Unit,
        initialSyncTimeoutMs: Long = 10_000L
    ) {
        viewModelScope.launch {
            try {
                prefs.clearAccountSession()
                gradeRepository.clearLocalCache()
                withTimeout(initialSyncTimeoutMs) {
                    repository.refreshAllData()
                }
                // 数据刷新成功后重新调度提醒
                rescheduleCourseReminders()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("MinePlus", "❌ 登录后首次数据同步失败（将进入主界面但数据为空）", e)
            } finally {
                onNavigateToMain()
            }
        }
    }

    // 旧方法保留（可能被其他地方调用），但登录页不再使用它。
    fun onLoginSuccessAndNavigate(onNavigateToMain: () -> Unit) {
        onNavigateToMain()
        viewModelScope.launch {
            try {
                prefs.clearAccountSession()
                gradeRepository.clearLocalCache()
                repository.refreshAllData()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("MinePlus", "❌ 登录后首次数据拉取失败（将进入主界面但数据为空）", e)
            }
        }
    }

    fun testFetchData(onLoginSuccess: () -> Unit) {
        viewModelScope.launch {
            prefs.clearAccountSession()
            gradeRepository.clearLocalCache()
            repository.refreshAllData()
            onLoginSuccess()
        }
    }

    fun clearSession(onCompleted: () -> Unit = {}) {
        viewModelScope.launch {
            prefs.clearAccountSession()
            gradeRepository.clearLocalCache()
            val cm = CookieManager.getInstance()
            cm.removeAllCookies(null)
            cm.flush()
            onCompleted()
        }
    }

    fun logout(onCompleted: () -> Unit = {}) {
        viewModelScope.launch {
            // 1) Clear local remembered credentials
            prefs.setRememberPassword(false)
            credentialStorage.clear()
            prefs.clearAccountSession()
            gradeRepository.clearLocalCache()

            // 2) Clear WebView/OkHttp cookies (this app's CookieJar reads from CookieManager)
            val cm = CookieManager.getInstance()
            cm.removeAllCookies(null)
            cm.flush()

            onCompleted()
        }
    }

    /**
     * 应用启动时调度课前提醒
     * 每次打开APP时调用，设置接下来3天的课前提醒
     */
    fun scheduleCourseReminders() {
        viewModelScope.launch {
            try {
                if (!reminderPrefs.reminderEnabled.first()) return@launch
                reminderScheduler.scheduleRemindersForNextThreeDays()
            } catch (_: Exception) {
                // 静默处理，不影响主流程
            }
        }
    }

    /**
     * 当课程数据发生变化时重新调度提醒
     * 例如：刷新课表、添加/删除课程后调用
     */
    fun rescheduleCourseReminders() {
        viewModelScope.launch {
            try {
                if (!reminderPrefs.reminderEnabled.first()) return@launch
                reminderScheduler.cancelAllReminders()
                reminderScheduler.scheduleRemindersForNextThreeDays()
            } catch (_: Exception) {
                // 静默处理
            }
        }
    }
}
