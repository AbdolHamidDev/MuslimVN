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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val prayerTimes: PrayerTimes? = null,
    val reminders: Map<String, PrayerReminder> = emptyMap(),
    val featuredScholars: List<Scholar> = emptyList(),
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
        refreshPrayerTimes()
        startCountdownTimer()
        observeReminders()
        loadFeaturedScholars()
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

    /**
     * Đọc trạng thái quyền THỰC TẾ từ hệ thống và đồng bộ vào UI state.
     *
     * Trước đây [HomeUiState.isLocationPermissionGranted] chỉ được đặt trong callback
     * của hộp thoại xin quyền nên mỗi lần mở lại app nó luôn reset về `false` dù quyền
     * đã được cấp — khiến card "Cấp quyền" hiện vĩnh viễn. Hàm này là nguồn sự thật.
     *
     * @return true nếu quyền vị trí VỪA chuyển từ chưa-cấp → đã-cấp (ví dụ người dùng
     *         bật thủ công trong Cài đặt rồi quay lại app) — lúc đó nên tính lại
     *         giờ cầu nguyện theo vị trí thật bằng [refreshPrayerTimes].
     */
    fun syncPermissionState(): Boolean {
        val locationGranted = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        // POST_NOTIFICATIONS chỉ tồn tại từ Android 13 (API 33) trở lên
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

    fun refreshPrayerTimes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val times = getPrayerTimesUseCase()
                _uiState.update { it.copy(prayerTimes = times, isLoading = false) }
                
                // Đặt báo thức trong một khối try-catch riêng để không làm hỏng luồng UI
                try {
                    adhanScheduler.scheduleNextWithSettings(times, _uiState.value.reminders)
                } catch (e: Exception) {
                    // Chỉ ghi log hoặc thông báo nhẹ, không làm hiện màn hình lỗi chính
                    android.util.Log.e("HomeViewModel", "Alarm scheduling failed", e)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage ?: "Unknown error", isLoading = false) }
            }
        }
    }

    private fun startCountdownTimer() {
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                // Tính lại mốc cầu nguyện tiếp theo mỗi phút (ví dụ khi đã qua Isha,
                // mốc tiếp theo là Fajr ngày mai). Phần giây đếm ngược do UI tự tick
                // từng giây — không gọi lại use case mỗi giây.
                if (_uiState.value.prayerTimes != null) {
                    refreshPrayerTimes()
                }
            }
        }
    }
}
