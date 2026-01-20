package com.cumtb.mineplus.ui.splash

import androidx.lifecycle.ViewModel
import com.cumtb.mineplus.data.preference.AppPreferences
import com.cumtb.mineplus.data.preference.CredentialStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val prefs: AppPreferences,
    private val credentialStorage: CredentialStorage
) : ViewModel() {

    suspend fun shouldGoSchedule(): Boolean {
        val remember = prefs.rememberPassword.first()
        return remember && credentialStorage.hasCredentials()
    }
}
