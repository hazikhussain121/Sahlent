package com.sahlent.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sahlent.data.datastore.SettingsDataStore
import com.sahlent.data.model.CalculationMethod
import com.sahlent.data.model.Madhab
import com.sahlent.data.model.PrayerName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val calculationMethod: CalculationMethod = CalculationMethod.MWL,
    val madhab: Madhab = Madhab.SHAFII,
    val silentDurationMinutes: Int = 20,
    val prayerStartDelayMinutes: Int = 0,
    val prayerSettings: Map<PrayerName, Boolean> = mapOf(
        PrayerName.FAJR to true,
        PrayerName.DHUHR to true,
        PrayerName.ASR to true,
        PrayerName.MAGHRIB to true,
        PrayerName.ISHA to true
    )
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            settingsDataStore.calculationMethod.collect { method ->
                _uiState.value = _uiState.value.copy(calculationMethod = method)
            }
        }
        
        viewModelScope.launch {
            settingsDataStore.madhab.collect { madhab ->
                _uiState.value = _uiState.value.copy(madhab = madhab)
            }
        }
        
        viewModelScope.launch {
            settingsDataStore.silentDurationMinutes.collect { minutes ->
                _uiState.value = _uiState.value.copy(silentDurationMinutes = minutes)
            }
        }
        
        viewModelScope.launch {
            settingsDataStore.prayerStartDelayMinutes.collect { minutes ->
                _uiState.value = _uiState.value.copy(prayerStartDelayMinutes = minutes)
            }
        }
        
        // Load individual prayer settings
        listOf(PrayerName.FAJR, PrayerName.DHUHR, PrayerName.ASR, PrayerName.MAGHRIB, PrayerName.ISHA).forEach { prayer ->
            viewModelScope.launch {
                settingsDataStore.isPrayerSilentEnabled(prayer).collect { enabled ->
                    val currentSettings = _uiState.value.prayerSettings.toMutableMap()
                    currentSettings[prayer] = enabled
                    _uiState.value = _uiState.value.copy(prayerSettings = currentSettings)
                }
            }
        }
    }
    
    fun setCalculationMethod(method: CalculationMethod) {
        viewModelScope.launch {
            settingsDataStore.setCalculationMethod(method)
        }
    }
    
    fun setMadhab(madhab: Madhab) {
        viewModelScope.launch {
            settingsDataStore.setMadhab(madhab)
        }
    }
    
    fun setSilentDuration(minutes: Int) {
        viewModelScope.launch {
            settingsDataStore.setSilentDurationMinutes(minutes)
        }
    }
    
    fun setPrayerStartDelay(minutes: Int) {
        viewModelScope.launch {
            settingsDataStore.setPrayerStartDelayMinutes(minutes)
        }
    }
    
    fun setPrayerSilentEnabled(prayer: PrayerName, enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setPrayerSilentEnabled(prayer, enabled)
        }
    }
}
