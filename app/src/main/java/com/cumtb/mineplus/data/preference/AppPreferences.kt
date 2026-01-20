package com.cumtb.mineplus.data.preference

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// 定义 DataStore 扩展属性，名字叫 "app_prefs"
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // 定义 Keys
    companion object {
        val KEY_SEMESTER_START_DATE = stringPreferencesKey("semester_start_date")
        val KEY_TOTAL_WEEKS = intPreferencesKey("total_weeks")
        val KEY_REMEMBER_PASSWORD = booleanPreferencesKey("remember_password")
    }

    // --- 读取数据 (Flow) ---

    // 获取开学日期 (例如 "2025-08-25")
    val semesterStartDate: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_SEMESTER_START_DATE]
        }

    // 获取本学期总周数 (默认为 20)
    val totalWeeks: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_TOTAL_WEEKS] ?: 20
        }

    val rememberPassword: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_REMEMBER_PASSWORD] ?: false
        }

    // --- 写入数据 ---

    suspend fun saveSemesterInfo(startDate: String, totalWeeks: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SEMESTER_START_DATE] = startDate
            preferences[KEY_TOTAL_WEEKS] = totalWeeks
        }
    }

    suspend fun setRememberPassword(remember: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_REMEMBER_PASSWORD] = remember
        }
    }
}