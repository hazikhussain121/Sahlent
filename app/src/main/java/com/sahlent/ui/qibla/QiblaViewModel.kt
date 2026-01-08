package com.sahlent.ui.qibla

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sahlent.data.repository.PrayerRepository
import com.sahlent.util.CompassSensor
import com.sahlent.util.LocationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QiblaUiState(
    val isLoading: Boolean = true,
    val qiblaDirection: Float = 0f,
    val compassBearing: Float = 0f,
    val needleRotation: Float = 0f,
    val error: String? = null,
    val isCompassAvailable: Boolean = true,
    val showCalibrationHint: Boolean = false
)

@HiltViewModel
class QiblaViewModel @Inject constructor(
    private val compassSensor: CompassSensor,
    private val locationHelper: LocationHelper,
    private val prayerRepository: PrayerRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(QiblaUiState())
    val uiState: StateFlow<QiblaUiState> = _uiState.asStateFlow()
    
    init {
        checkCompassAvailability()
        loadQiblaDirection()
        startCompass()
    }
    
    private fun checkCompassAvailability() {
        _uiState.value = _uiState.value.copy(
            isCompassAvailable = compassSensor.isAvailable
        )
    }
    
    private fun loadQiblaDirection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val location = locationHelper.getLastLocation()
                if (location != null) {
                    // Try API first
                    val result = prayerRepository.getQiblaDirection(location.latitude, location.longitude)
                    
                    result.fold(
                        onSuccess = { direction ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                qiblaDirection = direction.toFloat()
                            )
                        },
                        onFailure = {
                            // Fallback to local calculation
                            val direction = CompassSensor.calculateQiblaDirection(
                                location.latitude,
                                location.longitude
                            )
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                qiblaDirection = direction.toFloat()
                            )
                        }
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Location unavailable"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    private fun startCompass() {
        viewModelScope.launch {
            compassSensor.getBearing().collect { bearing ->
                val qiblaDirection = _uiState.value.qiblaDirection
                
                // Calculate needle rotation to point to Qibla
                // The needle should rotate opposite to device rotation
                val needleRotation = qiblaDirection - bearing
                
                _uiState.value = _uiState.value.copy(
                    compassBearing = bearing,
                    needleRotation = needleRotation
                )
            }
        }
    }
    
    fun retry() {
        loadQiblaDirection()
    }
    
    fun dismissCalibrationHint() {
        _uiState.value = _uiState.value.copy(showCalibrationHint = false)
    }
}
