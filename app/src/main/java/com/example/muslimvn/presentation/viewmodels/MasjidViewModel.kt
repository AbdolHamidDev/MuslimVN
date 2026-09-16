package com.example.muslimvn.presentation.viewmodels

import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muslimvn.domain.models.masjid.Masjid
import com.example.muslimvn.domain.models.masjid.MasjidType
import com.example.muslimvn.domain.repository.LocationRepository
import com.example.muslimvn.domain.repository.MasjidRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MasjidFilterType {
    ALL,
    MASJID,
    TIEU_THANG_DUONG
}

data class MasjidItemUiState(
    val masjid: Masjid,
    val distanceKm: Double? = null
)

data class MasjidListUiState(
    val masjids: List<MasjidItemUiState> = emptyList(),
    val filterType: MasjidFilterType = MasjidFilterType.ALL,
    val searchQuery: String = "",
    val isLocating: Boolean = false,
    val userLocationAddress: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class MasjidViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val masjidRepository: MasjidRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MasjidListUiState())
    val uiState: StateFlow<MasjidListUiState> = _uiState.asStateFlow()

    init {
        loadMasjids("ho-chi-minh")
    }

    fun loadMasjids(provinceId: String = "ho-chi-minh") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val list = masjidRepository.getMasjids(provinceId)
                val items = list.map { MasjidItemUiState(masjid = it) }
                _uiState.update { it.copy(masjids = items, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = e.localizedMessage ?: "Không thể nạp danh sách Masjid"
                    ) 
                }
            }
        }
    }

    fun setFilterType(filterType: MasjidFilterType) {
        _uiState.update { it.copy(filterType = filterType) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun findNearestMasjid() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLocating = true) }
            try {
                val userLocation = locationRepository.getCurrentLocation()
                val currentMasjids = _uiState.value.masjids.map { item ->
                    val mLat = item.masjid.location.latitude
                    val mLng = item.masjid.location.longitude
                    
                    val calculatedDistanceKm = if (userLocation != null && mLat != null && mLng != null) {
                        val results = FloatArray(1)
                        Location.distanceBetween(
                            userLocation.latitude, userLocation.longitude,
                            mLat, mLng,
                            results
                        )
                        (results[0] / 1000.0)
                    } else {
                        item.distanceKm
                    }
                    item.copy(distanceKm = calculatedDistanceKm)
                }

                val sortedMasjids = currentMasjids.sortedBy { it.distanceKm ?: Double.MAX_VALUE }

                _uiState.update { 
                    it.copy(
                        masjids = sortedMasjids,
                        isLocating = false,
                        userLocationAddress = if (userLocation != null) 
                            "Vị trí GPS của bạn (${String.format(java.util.Locale.getDefault(), "%.4f, %.4f", userLocation.latitude, userLocation.longitude)})" 
                        else 
                            "Thành phố Hồ Chí Minh"
                    ) 
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLocating = false) }
            }
        }
    }
}
