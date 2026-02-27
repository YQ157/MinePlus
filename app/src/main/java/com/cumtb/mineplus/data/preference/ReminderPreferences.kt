package com.cumtb.mineplus.data.preference

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "reminder_settings")

@Singleton
class ReminderPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        val KEY_REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
    }

    val reminderEnabled: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_REMINDER_ENABLED] ?: false }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_REMINDER_ENABLED] = enabled
        }
    }
}

