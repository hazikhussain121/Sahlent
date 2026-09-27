package com.sahlent.ui.home

// viewmodel for home screen - handles prayer times, tasbih, settings

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sahlent.data.datastore.SettingsDataStore
import com.sahlent.data.model.DailyPrayers
import com.sahlent.data.model.Prayer
import com.sahlent.data.repository.PrayerRepository
import com.sahlent.util.LocationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val prayers: DailyPrayers? = null,
    val nextPrayer: Prayer? = null,
    val countdownText: String = "",
    val error: String? = null,
    val locationName: String = "Loading...",
    val autoSilentEnabled: Boolean = true,
    val tasbihCount: Int = 0,
    val tasbihTarget: Int = 33,
    val tasbihTotalCount: Long = 0,
    val showTargetReached: Boolean = false,
    val hasDndPermission: Boolean = false,
    val hasLocationPermission: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val prayerRepository: PrayerRepository,
    private val settingsDataStore: SettingsDataStore,
    private val locationHelper: LocationHelper,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
        checkPermissions()
        startCountdownTimer()
        // FIXME: should probably handle timezone changes at some point
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            settingsDataStore.autoSilentEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(autoSilentEnabled = enabled)
            }
        }
        
        viewModelScope.launch {
            settingsDataStore.tasbihCount.collect { count ->
                _uiState.value = _uiState.value.copy(tasbihCount = count)
            }
        }
        
        viewModelScope.launch {
            settingsDataStore.tasbihTarget.collect { target ->
                _uiState.value = _uiState.value.copy(tasbihTarget = target)
            }
        }
        
        viewModelScope.launch {
            settingsDataStore.tasbihTotalCount.collect { total ->
                _uiState.value = _uiState.value.copy(tasbihTotalCount = total)
            }
        }
    }
    
    fun checkPermissions() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val hasDnd = notificationManager.isNotificationPolicyAccessGranted
        _uiState.value = _uiState.value.copy(hasDndPermission = hasDnd)
    }
    
    fun loadPrayerTimes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val location = locationHelper.getLastLocation()
                // Log.d("HomeVM", "got location: $location")
                if (location != null) {
                    val method = settingsDataStore.calculationMethod.first()
                    val madhab = settingsDataStore.madhab.first()
                    val result = prayerRepository.getPrayerTimes(location.latitude, location.longitude, method, madhab)
                    
                    result.fold(
                        onSuccess = { prayers ->
                            val nextPrayer = prayers.getNextPrayer(System.currentTimeMillis())
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                prayers = prayers,
                                nextPrayer = nextPrayer,
                                locationName = locationHelper.getLocationName(location.latitude, location.longitude) ?: "Unknown Location"
                            )
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = error.message ?: "Failed to load prayer times"
                            )
                        }
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Location unavailable. Please enable location services."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "An error occurred"
                )
            }
        }
    }
    
    // countdown timer - updates every second
    // TODO: maybe pause when app is in background to save battery?
    private fun startCountdownTimer() {
        viewModelScope.launch {
            while (true) {
                updateCountdown()
                delay(1000) // 1 sec
            }
        }
    }
    
    private fun updateCountdown() {
        val nextPrayer = _uiState.value.nextPrayer ?: return
        val now = System.currentTimeMillis()
        val diff = nextPrayer.timeInMillis - now
        
        if (diff <= 0) {
            // Prayer time passed, reload
            loadPrayerTimes()
            return
        }
        
        val hours = (diff / (1000 * 60 * 60)).toInt()
        val minutes = ((diff / (1000 * 60)) % 60).toInt()
        val seconds = ((diff / 1000) % 60).toInt()
        
        val countdownText = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        _uiState.value = _uiState.value.copy(countdownText = countdownText)
    }
    
    fun toggleAutoSilent() {
        viewModelScope.launch {
            val newValue = !_uiState.value.autoSilentEnabled
            settingsDataStore.setAutoSilentEnabled(newValue)
        }
    }
    
    fun incrementTasbih() {
        viewModelScope.launch {
            val newCount = settingsDataStore.incrementTasbih()
            val target = _uiState.value.tasbihTarget
            
            // show celebration when target reached
            if (newCount == target) {
                _uiState.value = _uiState.value.copy(showTargetReached = true)
                // maybe add sound effect here later?
            }
        }
    }
    
    fun resetTasbih() {
        viewModelScope.launch {
            settingsDataStore.setTasbihCount(0)
        }
    }
    
    fun dismissTargetReached() {
        _uiState.value = _uiState.value.copy(showTargetReached = false)
    }
    
    fun setTasbihTarget(target: Int) {
        viewModelScope.launch {
            settingsDataStore.setTasbihTarget(target)
        }
    }
    
    fun onLocationPermissionGranted() {
        _uiState.value = _uiState.value.copy(hasLocationPermission = true)
        loadPrayerTimes()
    }
}
