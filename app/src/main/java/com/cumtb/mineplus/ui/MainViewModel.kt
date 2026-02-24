package com.cumtb.mineplus.ui

import android.webkit.CookieManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cumtb.mineplus.data.preference.AppPreferences
import com.cumtb.mineplus.data.preference.CredentialStorage
import com.cumtb.mineplus.data.repository.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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

    fun testFetchData(onLoginSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.refreshAllData(onLoginSuccess)
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