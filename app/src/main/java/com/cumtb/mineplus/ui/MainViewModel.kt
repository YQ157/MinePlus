package com.cumtb.mineplus.ui

import android.util.Log
import android.webkit.CookieManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cumtb.mineplus.data.preference.AppPreferences
import com.cumtb.mineplus.data.preference.CredentialStorage
import com.cumtb.mineplus.data.repository.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: CourseRepository,
    private val prefs: AppPreferences,
    private val credentialStorage: CredentialStorage
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
     * 1) 立即进入 MainScreen（哪怕后续拉取数据失败，也不闪退）
     * 2) 后台尝试刷新课表数据；失败时仅记录日志，数据库保持为空/旧数据
     */
    fun onLoginSuccessAndNavigate(onNavigateToMain: () -> Unit) {
        // 先导航，避免被网络/解析失败阻塞或导致崩溃
        onNavigateToMain()

        // 再后台拉取数据（best-effort）
        viewModelScope.launch {
            try {
                repository.refreshAllData(onLoginSuccess = {})
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("MinePlus", "❌ 登录后首次数据拉取失败（将进入主界面但数据为空）", e)
                // swallow: do not crash
            }
        }
    }

    fun testFetchData(onLoginSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.refreshAllData(onLoginSuccess)
        }
    }

    fun clearSession(onCompleted: () -> Unit = {}) {
        viewModelScope.launch {
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

            // 2) Clear WebView/OkHttp cookies (this app's CookieJar reads from CookieManager)
            val cm = CookieManager.getInstance()
            cm.removeAllCookies(null)
            cm.flush()

            onCompleted()
        }
    }
}