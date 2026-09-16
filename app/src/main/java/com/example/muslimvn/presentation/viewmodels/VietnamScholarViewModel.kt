package com.example.muslimvn.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muslimvn.domain.models.YoutubeVideo
import com.example.muslimvn.domain.repository.YoutubeRepository
import com.example.muslimvn.presentation.components.vietnamScholarsList
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VietnamScholarUiState(
    val isLoading: Boolean = false,
    val videos: List<YoutubeVideo> = emptyList(),
    val error: String? = null,
    val isGridMode: Boolean = false
)

@HiltViewModel(assistedFactory = VietnamScholarViewModel.Factory::class)
class VietnamScholarViewModel @AssistedInject constructor(
    private val youtubeRepository: YoutubeRepository,
    @Assisted scholarId: String
) : ViewModel() {
    @AssistedFactory
    interface Factory { fun create(scholarId: String): VietnamScholarViewModel }
    private val scholar = vietnamScholarsList.find { it.id == scholarId }

    private val _uiState = MutableStateFlow(VietnamScholarUiState())
    val uiState = _uiState.asStateFlow()

    fun toggleViewMode() {
        _uiState.update { it.copy(isGridMode = !it.isGridMode) }
    }

    init {
        fetchVideos()
    }

    private fun fetchVideos() {
        val url = scholar?.youtubeUrl ?: return
        
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
}
