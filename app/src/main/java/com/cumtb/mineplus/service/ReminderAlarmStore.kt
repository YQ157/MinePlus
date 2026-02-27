package com.cumtb.mineplus.service

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.reminderAlarmDataStore: DataStore<Preferences> by preferencesDataStore(name = "reminder_alarm_store")

/**
 * 持久化维护“已设置的闹钟列表”。
 *
 * 存储内容是 alarmId 的集合（字符串），alarmId 必须能稳定地反推出 requestCode。
 */
@Singleton
class ReminderAlarmStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_ALARM_IDS = stringSetPreferencesKey("alarm_ids")
    }

    val alarmIds: Flow<Set<String>> = context.reminderAlarmDataStore.data
        .map { prefs -> prefs[KEY_ALARM_IDS] ?: emptySet() }

    suspend fun setAlarmIds(ids: Set<String>) {
        context.reminderAlarmDataStore.edit { prefs ->
            prefs[KEY_ALARM_IDS] = ids
        }
    }

    suspend fun clear() {
        setAlarmIds(emptySet())
    }
}

