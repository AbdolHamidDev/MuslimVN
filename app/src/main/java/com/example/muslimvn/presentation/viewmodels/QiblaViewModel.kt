package com.example.muslimvn.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muslimvn.data.sensor.CompassSensorManager
import com.example.muslimvn.domain.repository.LocationRepository
import com.example.muslimvn.domain.util.QiblaUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class QiblaViewModel @Inject constructor(
    private val sensorManager: CompassSensorManager,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QiblaUiState())
    val uiState: StateFlow<QiblaUiState> = _uiState.asStateFlow()

    init {
        loadLocationAndCalculateQibla()
        observeCompass()
    }

    private fun loadLocationAndCalculateQibla() {
        viewModelScope.launch {
            val location = locationRepository.getCurrentLocation()
            if (location != null) {
                val bearing = QiblaUtils.calculateQiblaBearing(location.latitude, location.longitude).toFloat()
                val distance = QiblaUtils.calculateDistanceToMecca(location.latitude, location.longitude)
                
                _uiState.update { 
                    it.copy(
                        qiblaBearing = bearing,
                        distanceToMecca = distance,
                        userLatitude = location.latitude,
                        userLongitude = location.longitude,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(error = "Could not get location", isLoading = false) }
            }
        }
    }

    private fun observeCompass() {
        sensorManager.azimuthFlow
            .onEach { azimuth ->
                _uiState.update { state ->
                    val isFacing = abs(azimuth - state.qiblaBearing) < 3.0 || 
                                   abs(azimuth - state.qiblaBearing) > 357.0
                    state.copy(
                        currentAzimuth = azimuth,
                        isFacingQibla = isFacing
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    data class QiblaUiState(
        val currentAzimuth: Float = 0f,
        val qiblaBearing: Float = 0f,
        val isFacingQibla: Boolean = false,
        val distanceToMecca: Double = 0.0,
        val userLatitude: Double = 0.0,
        val userLongitude: Double = 0.0,
        val isLoading: Boolean = true,
        val error: String? = null
    )
}
