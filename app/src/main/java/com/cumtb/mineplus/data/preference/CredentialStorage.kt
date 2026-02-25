package com.cumtb.mineplus.data.preference

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialStorage @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)

    fun getPassword(): String? = prefs.getString(KEY_PASSWORD, null)

    fun hasCredentials(): Boolean = !getUsername().isNullOrBlank() && !getPassword().isNullOrBlank()

    fun save(username: String, password: String) {
        prefs.edit {
            putString(KEY_USERNAME, username)
            putString(KEY_PASSWORD, password)
        }
    }

    /**
     * 仅保存用户名，并确保密码不会被持久化。
     * 用于“手动 WebView 登录成功”的兜底流程：允许下次自动填学号，但不保存密码。
     */
    fun saveUsernameOnly(username: String) {
        prefs.edit {
            putString(KEY_USERNAME, username)
            remove(KEY_PASSWORD)
        }
    }

    fun clearPassword() {
        prefs.edit {
            remove(KEY_PASSWORD)
        }
    }

    fun clear() {
        prefs.edit {
            clear()
        }
    }

    private companion object {
        private const val FILE_NAME = "encrypted_credentials"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
    }
}
