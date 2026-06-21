package com.cumtb.mineplus.data.preference

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.cumtb.mineplus.ui.theme.CoursePalettes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// 定义 DataStore 扩展属性，名字叫 "app_prefs"
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

@Singleton
class AppPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    // 定义 Keys
    companion object {
        val KEY_SEMESTER_START_DATE = stringPreferencesKey("semester_start_date")
        val KEY_TOTAL_WEEKS = intPreferencesKey("total_weeks")
        val KEY_REMEMBER_PASSWORD = booleanPreferencesKey("remember_password")
        val KEY_STD_PERSON_ID = longPreferencesKey("std_person_id")
        val KEY_GRADE_DATA_ID = longPreferencesKey("grade_data_id")
        val KEY_GRADE_FETCHED_AT = longPreferencesKey("grade_fetched_at")

        // Course card palette
        val KEY_COURSE_PALETTE = stringPreferencesKey("course_palette")
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

    val stdPersonId: Flow<Long?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_STD_PERSON_ID]
        }

    val gradeDataId: Flow<Long?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_GRADE_DATA_ID]
        }

    val gradeFetchedAt: Flow<Long?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_GRADE_FETCHED_AT]
        }

    /** Selected course palette id (persisted). Defaults to [CoursePalettes.defaultPaletteId]. */
    val coursePaletteId: Flow<CoursePalettes.PaletteId> = context.dataStore.data
        .map { preferences ->
            CoursePalettes.paletteIdFromStorageKey(preferences[KEY_COURSE_PALETTE])
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

    suspend fun saveStdPersonId(stdPersonId: Long) {
        context.dataStore.edit { preferences ->
            preferences[KEY_STD_PERSON_ID] = stdPersonId
        }
    }

    suspend fun saveGradeDataId(gradeDataId: Long) {
        context.dataStore.edit { preferences ->
            preferences[KEY_GRADE_DATA_ID] = gradeDataId
        }
    }

    suspend fun saveGradeFetchedAt(fetchedAt: Long) {
        context.dataStore.edit { preferences ->
            preferences[KEY_GRADE_FETCHED_AT] = fetchedAt
        }
    }

    suspend fun clearGradeSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_GRADE_DATA_ID)
            preferences.remove(KEY_GRADE_FETCHED_AT)
        }
    }

    suspend fun clearAccountSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_STD_PERSON_ID)
            preferences.remove(KEY_GRADE_DATA_ID)
            preferences.remove(KEY_GRADE_FETCHED_AT)
        }
    }

    suspend fun setCoursePaletteId(id: CoursePalettes.PaletteId) {
        context.dataStore.edit { preferences ->
            preferences[KEY_COURSE_PALETTE] = id.storageKey
        }
    }
}
