package com.example.muslimvn.presentation.viewmodels

import android.hardware.GeomagneticField
import android.hardware.SensorManager
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
        observeCompass()
    }

    fun onPermissionGranted() {
        _uiState.update { it.copy(isPermissionGranted = true) }
        loadLocationAndCalculateQibla()
    }

    fun loadLocationAndCalculateQibla() {
        viewModelScope.launch {
            val location = locationRepository.getCurrentLocation()
            if (location != null) {
                val bearing = QiblaUtils.calculateQiblaBearing(location.latitude, location.longitude).toFloat()
                val distance = QiblaUtils.calculateDistanceToMecca(location.latitude, location.longitude)
                
                // Calculate Magnetic Declination
                val geoField = GeomagneticField(
                    location.latitude.toFloat(),
                    location.longitude.toFloat(),
                    location.altitude.toFloat(),
                    System.currentTimeMillis()
                )
                val declination = geoField.declination
                
                // Adjusted bearing for Magnetic North
                // Qibla bearing is relative to True North. 
                // Magnetic North = True North - Declination
                // So, to point to Mecca using a magnetic compass: 
                // Relative Bearing = True Bearing - Magnetic Declination
                val adjustedBearing = (bearing - declination + 360f) % 360f

                _uiState.update { 
                    it.copy(
                        qiblaBearing = adjustedBearing,
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
        sensorManager.compassDataFlow
            .onEach { data ->
                _uiState.update { state ->
                    val diff = abs(data.azimuth - state.qiblaBearing)
                    val isFacing = diff < 3.0f || diff > 357.0f
                    state.copy(
                        currentAzimuth = data.azimuth,
                        isFacingQibla = isFacing,
                        sensorAccuracy = data.accuracy
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
        val isPermissionGranted: Boolean = false,
        val error: String? = null,
        val sensorAccuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_HIGH
    )
}
