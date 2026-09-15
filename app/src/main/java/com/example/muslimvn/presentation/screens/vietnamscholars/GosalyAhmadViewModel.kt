package com.example.muslimvn.presentation.screens.vietnamscholars

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muslimvn.domain.models.YoutubeVideo
import com.example.muslimvn.domain.repository.YoutubeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GosalyAhmadUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val videos: List<YoutubeVideo> = emptyList(),
    val hasNextPage: Boolean = true,
    val error: String? = null,
    val isGridMode: Boolean = true,
    val selectedTab: Int = 0, // 0: Video, 1: Tài liệu
    val documents: List<String> = emptyList()
)

@HiltViewModel
class GosalyAhmadViewModel @Inject constructor(
    private val youtubeRepository: YoutubeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GosalyAhmadUiState())
    val uiState = _uiState.asStateFlow()

    init {
        fetchYoutubeVideos()
    }

    fun setSelectedTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun toggleViewMode() {
        _uiState.update { it.copy(isGridMode = !it.isGridMode) }
    }

    private fun fetchYoutubeVideos() {
        val url = "https://www.youtube.com/@gosalyahmad-Unofficial"
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            youtubeRepository.getVideosByChannel(url).collect { result ->
                result.onSuccess { videos ->
                    _uiState.update { it.copy(isLoading = false, videos = videos) }
                }.onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            }
        }
    }

    fun loadMoreVideos() {
        val currentState = _uiState.value
        if (currentState.isLoadingMore || !currentState.hasNextPage) return

        val url = "https://www.youtube.com/@gosalyahmad-Unofficial"
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val result = youtubeRepository.loadMoreVideos(url)
            result.onSuccess { newVideos ->
                _uiState.update { state ->
                    state.copy(
                        isLoadingMore = false,
                        videos = state.videos + newVideos,
                        hasNextPage = newVideos.isNotEmpty()
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }
}
