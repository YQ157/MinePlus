package com.cumtb.mineplus.ui.splash

import android.webkit.CookieManager
import androidx.lifecycle.ViewModel
import com.cumtb.mineplus.api.PersistentCookieStore
import com.cumtb.mineplus.api.SchoolApiConfig
import com.cumtb.mineplus.data.preference.AppPreferences
import com.cumtb.mineplus.data.preference.CredentialStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import okhttp3.HttpUrl.Companion.toHttpUrl
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val prefs: AppPreferences,
    private val credentialStorage: CredentialStorage,
    private val persistentCookieStore: PersistentCookieStore
) : ViewModel() {

    suspend fun shouldGoSchedule(): Boolean {
        val remember = prefs.rememberPassword.first()
        if (remember && credentialStorage.hasCredentials()) return true

        return hasCachedCookie()
    }

    private fun hasCachedCookie(): Boolean {
        val baseUrl = SchoolApiConfig.BASE_URL.toHttpUrl()
        if (persistentCookieStore.hasCookiesForHost(baseUrl.host)) return true

        SchoolApiConfig.COOKIE_PROBE_URLS.forEach { urlText ->
            val url = urlText.toHttpUrl()
            val cookieHeader = runCatching {
                CookieManager.getInstance().getCookie(urlText)
            }.getOrNull()

            if (!cookieHeader.isNullOrBlank()) {
                persistentCookieStore.saveFromHeader(url, cookieHeader)
                return true
            }
        }

        return false
    }
}
