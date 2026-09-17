package com.example.muslimvn.presentation.viewmodels

import android.hardware.GeomagneticField
import android.hardware.SensorManager
import android.location.Location
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
    private val locationRepository: LocationRepository,
    private val settingsRepository: com.example.muslimvn.domain.repository.SettingsRepository
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
            _uiState.update { it.copy(isLoading = true) }
            
            // Try Live GPS first
            var location = locationRepository.getCurrentLocation()
            
            // Fallback to Cache
            if (location == null) {
                val cached = settingsRepository.getLastLocation().first()
                if (cached != null) {
                    location = Location("cache").apply {
                        latitude = cached.first
                        longitude = cached.second
                    }
                }
            }
            
            // Final fallback to Hardcoded
            val finalLocation = location ?: createDefaultLocation()
            val isUsingDefault = finalLocation.latitude == DEFAULT_LATITUDE && finalLocation.longitude == DEFAULT_LONGITUDE

            val bearing = QiblaUtils.calculateQiblaBearing(finalLocation.latitude, finalLocation.longitude).toFloat()
            val distance = QiblaUtils.calculateDistanceToMecca(finalLocation.latitude, finalLocation.longitude)
            
            // Calculate Magnetic Declination
            val geoField = GeomagneticField(
                finalLocation.latitude.toFloat(),
                finalLocation.longitude.toFloat(),
                finalLocation.altitude.toFloat(),
                System.currentTimeMillis()
            )
            val declination = geoField.declination
            
            // Adjusted bearing for Magnetic North
            val adjustedBearing = (bearing - declination + 360f) % 360f

            _uiState.update { 
                it.copy(
                    qiblaBearing = adjustedBearing,
                    distanceToMecca = distance,
                    userLatitude = finalLocation.latitude,
                    userLongitude = finalLocation.longitude,
                    isUsingDefaultLocation = isUsingDefault,
                    isLoading = false
                )
            }
        }
    }

    private fun createDefaultLocation(): Location {
        return Location("fallback").apply {
            latitude = DEFAULT_LATITUDE
            longitude = DEFAULT_LONGITUDE
            altitude = 0.0
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
        val isUsingDefaultLocation: Boolean = false,
        val error: String? = null,
        val sensorAccuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_HIGH
    )

    companion object {
        const val DEFAULT_LATITUDE = 10.7005
        const val DEFAULT_LONGITUDE = 105.1147
    }
}
