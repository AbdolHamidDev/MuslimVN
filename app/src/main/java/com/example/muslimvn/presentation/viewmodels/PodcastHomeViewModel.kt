package com.example.muslimvn.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muslimvn.domain.models.PodcastCategory
import com.example.muslimvn.domain.models.Scholar
import com.example.muslimvn.domain.repository.PodcastRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel trang chủ Podcast: nạp học giả từ Room (offline-first), seed nếu DB trống,
 * tự động đồng bộ ngầm thông tin/ảnh mới từ Muslim Central API/RSS,
 * và lọc theo phân loại được chọn.
 */
@HiltViewModel
class PodcastHomeViewModel @Inject constructor(
    private val podcastRepository: PodcastRepository
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = true,
        val loadError: Boolean = false,
        val categories: List<PodcastCategory> = emptyList(),
        /** null = chip "Tất cả". */
        val selectedCategoryId: String? = null,
        val featuredScholars: List<Scholar> = emptyList(),
        /** Danh sách đã lọc theo [selectedCategoryId] — dùng cho lưới danh sách chính. */
        val scholars: List<Scholar> = emptyList()
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    /** Cache danh sách đầy đủ để lọc lại tức thì khi đổi chip mà không cần đợi Flow mới. */
    private var allScholars: List<Scholar> = emptyList()

    init {
        viewModelScope.launch {
            // Seed một lần (no-op nếu đã có dữ liệu); lỗi asset hiếm gặp -> báo lỗi UI.
            runCatching { podcastRepository.initializeData() }
                .onFailure { _uiState.update { it.copy(isLoading = false, loadError = true) } }

            _uiState.update { it.copy(categories = podcastRepository.getCategories()) }

            // Chạy đồng bộ tự động từ xa ngầm để nạp thông tin/ảnh học giả mới
            launch {
                runCatching { podcastRepository.syncMuslimCentralDirectory() }
            }

            podcastRepository.getScholars().collect { list ->
                allScholars = list
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        loadError = false,
                        featuredScholars = list.filter { it.featured },
                        scholars = filterByCategory(list, state.selectedCategoryId)
                    )
                }
            }
        }
    }

    /** Chọn/bỏ chọn chip phân loại; id == null nghĩa là "Tất cả". */
    fun selectCategory(categoryId: String?) {
        _uiState.update { state ->
            state.copy(
                selectedCategoryId = categoryId,
                scholars = filterByCategory(allScholars, categoryId)
            )
        }
    }

    fun retryLoading() {
        _uiState.update { it.copy(isLoading = true, loadError = false) }
        viewModelScope.launch {
            runCatching { podcastRepository.initializeData() }
                .onFailure { _uiState.update { s -> s.copy(isLoading = false, loadError = true) } }
            runCatching { podcastRepository.syncMuslimCentralDirectory() }
        }
    }

    private fun filterByCategory(scholars: List<Scholar>, categoryId: String?): List<Scholar> =
        if (categoryId.isNullOrBlank()) scholars
        else scholars.filter { scholar -> categoryId in scholar.tags }
}
