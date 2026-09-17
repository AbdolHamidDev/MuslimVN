package com.example.muslimvn.presentation.viewmodels

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muslimvn.core.utils.AdhanScheduler
import com.example.muslimvn.domain.models.PrayerReminder
import com.example.muslimvn.domain.models.PrayerTimes
import com.example.muslimvn.domain.models.Scholar
import com.example.muslimvn.domain.models.masjid.Masjid
import com.example.muslimvn.domain.repository.LocationRepository
import com.example.muslimvn.domain.repository.MasjidRepository
import com.example.muslimvn.domain.repository.PodcastRepository
import com.example.muslimvn.domain.repository.SettingsRepository
import com.example.muslimvn.domain.usecases.GetHijriDateOffsetUseCase
import com.example.muslimvn.domain.usecases.GetPrayerTimesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val prayerTimes: PrayerTimes? = null,
    val reminders: Map<String, PrayerReminder> = emptyMap(),
    val featuredScholars: List<Scholar> = emptyList(),
    val masjids: List<Masjid> = emptyList(),
    val isLocatingMasjid: Boolean = false,
    val userLocationAddress: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isLocationPermissionGranted: Boolean = false,
    val isNotificationPermissionGranted: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val getPrayerTimesUseCase: GetPrayerTimesUseCase,
    private val getHijriDateOffsetUseCase: GetHijriDateOffsetUseCase,
    private val settingsRepository: SettingsRepository,
    private val podcastRepository: PodcastRepository,
    private val masjidRepository: MasjidRepository,
    private val locationRepository: LocationRepository,
    private val adhanScheduler: AdhanScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * Offset ngày Hijri do người dùng điều chỉnh ở màn Lịch Hijri.
     * Header Trang chủ cũng dùng giá trị này để hai nơi luôn hiển thị khớp nhau.
     */
    val hijriDateOffset: StateFlow<Int> = getHijriDateOffsetUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        syncPermissionState()
        loadInitialData()
        startCountdownTimer()
        observeReminders()
        loadFeaturedScholars()
        loadMasjids()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // Step 1: Load from cache first for instant UI
            val cached = settingsRepository.getLastLocation().first()
            if (cached != null) {
                val (lat, lng, address) = cached
                _uiState.update { it.copy(userLocationAddress = address, isLoading = false) }
                // Load prayer times from cache immediately
                val cachedTimes = getPrayerTimesUseCase(lat = lat, lng = lng)
                _uiState.update { it.copy(prayerTimes = cachedTimes) }
            }

            // Step 2: Trigger a background refresh for live data
            refreshPrayerTimes()
        }
    }

    fun loadMasjids(provinceId: String = "ho-chi-minh") {
        viewModelScope.launch {
            try {
                val list = masjidRepository.getMasjids(provinceId)
                _uiState.update { it.copy(masjids = list) }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Failed to load masjids", e)
            }
        }
    }

    fun findNearestMasjid() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLocatingMasjid = true) }
            delay(1000) // Giả lập quét GPS định vị vị trí gần nhất
            _uiState.update { 
                it.copy(
                    isLocatingMasjid = false,
                    userLocationAddress = "Thành phố Hồ Chí Minh"
                ) 
            }
        }
    }

    private fun loadFeaturedScholars() {
        viewModelScope.launch {
            podcastRepository.initializeData()
            podcastRepository.getScholars().collect { list ->
                _uiState.update { it.copy(featuredScholars = list.filter { s -> s.featured }) }
            }
        }
    }

    private fun observeReminders() {
        viewModelScope.launch {
            settingsRepository.getPrayerReminders().collect { reminders ->
                _uiState.update { it.copy(reminders = reminders) }
                // Re-schedule when settings change
                val currentTimes = _uiState.value.prayerTimes
                if (currentTimes != null) {
                    adhanScheduler.scheduleNextWithSettings(currentTimes, reminders)
                }
            }
        }
    }

    fun updateReminder(reminder: PrayerReminder) {
        viewModelScope.launch {
            settingsRepository.updateReminder(reminder)
        }
    }

    fun syncPermissionState(): Boolean {
        val locationGranted = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        val notificationsGranted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                hasPermission(Manifest.permission.POST_NOTIFICATIONS)

        val wasNotGrantedBefore = !_uiState.value.isLocationPermissionGranted
        _uiState.update {
            it.copy(
                isLocationPermissionGranted = locationGranted,
                isNotificationPermissionGranted = notificationsGranted
            )
        }
        return wasNotGrantedBefore && locationGranted
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(appContext, permission) ==
            PackageManager.PERMISSION_GRANTED

    fun refreshPrayerTimes(manualLat: Double? = null, manualLng: Double? = null) {
        viewModelScope.launch {
            val shouldShowLoading = _uiState.value.prayerTimes == null
            if (shouldShowLoading) {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }
            
            try {
                // 1. Get Prayer Times (Highest Priority: Manual/Cache or first attempt at Live GPS)
                val times = if (manualLat != null && manualLng != null) {
                    getPrayerTimesUseCase(lat = manualLat, lng = manualLng)
                } else {
                    getPrayerTimesUseCase()
                }
                
                _uiState.update { it.copy(prayerTimes = times, isLoading = false) }

                // 2. Refresh Location & Address in background to ensure maximum accuracy
                if (_uiState.value.isLocationPermissionGranted) {
                    launch {
                        val location = locationRepository.getCurrentLocation()
                        if (location != null) {
                            // Update address & cache
                            val address = locationRepository.getAddress(location.latitude, location.longitude)
                            _uiState.update { it.copy(userLocationAddress = address) }
                            settingsRepository.saveLastLocation(location.latitude, location.longitude, address)
                            
                            // CRITICAL: Re-calculate prayer times with this confirmed live location
                            // This ensures that even if the first attempt failed/timed out, 
                            // we eventually show the exact times for the user's location.
                            val accurateTimes = getPrayerTimesUseCase(lat = location.latitude, lng = location.longitude)
                            _uiState.update { it.copy(prayerTimes = accurateTimes) }
                            adhanScheduler.scheduleNextWithSettings(accurateTimes, _uiState.value.reminders)
                        }
                    }
                }
                
                // 3. Schedule Notifications for the initial 'times' (updated again in step 2 if location found)
                adhanScheduler.scheduleNextWithSettings(times, _uiState.value.reminders)

            } catch (e: Exception) {
                if (shouldShowLoading) {
                    _uiState.update { it.copy(error = e.localizedMessage ?: "Unknown error", isLoading = false) }
                }
                android.util.Log.e("HomeViewModel", "Refresh failed", e)
            }
        }
    }

    private fun startCountdownTimer() {
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                if (_uiState.value.prayerTimes != null) {
                    refreshPrayerTimes()
                }
            }
        }
    }
}
