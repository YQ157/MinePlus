package com.cumtb.mineplus.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cumtb.mineplus.data.preference.AppPreferences
import com.cumtb.mineplus.data.preference.ReminderPreferences
import com.cumtb.mineplus.ui.theme.CoursePalettes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val reminderPrefs: ReminderPreferences,
    private val appPrefs: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            reminderPrefs.reminderEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(isReminderEnabled = enabled)
            }
        }
        viewModelScope.launch {
            appPrefs.coursePaletteId.collect { id ->
                _uiState.value = _uiState.value.copy(coursePaletteId = id)
            }
        }
    }

    fun onCoursePaletteSelected(id: CoursePalettes.PaletteId) {
        viewModelScope.launch {
            appPrefs.setCoursePaletteId(id)
        }
    }
}

data class SettingsUiState(
    val isReminderEnabled: Boolean = false,
    val coursePaletteId: CoursePalettes.PaletteId = CoursePalettes.defaultPaletteId,
    val isLoading: Boolean = false
)
