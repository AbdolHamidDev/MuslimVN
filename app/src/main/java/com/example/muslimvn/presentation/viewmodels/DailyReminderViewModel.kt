package com.example.muslimvn.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muslimvn.domain.models.Hadith
import com.example.muslimvn.domain.repository.HadithRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DailyReminderUiState(
    val stories: List<Hadith> = emptyList(),
    val isInitialLoading: Boolean = true,
    val isLoadingMore: Boolean = false
)

@HiltViewModel
class DailyReminderViewModel @Inject constructor(
    private val repository: HadithRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(DailyReminderUiState(stories = repository.cachedHadiths(), isInitialLoading = repository.cachedHadiths().isEmpty()))
    val uiState: StateFlow<DailyReminderUiState> = _uiState.asStateFlow()

    init { loadMore() }

    fun loadMore() {
        if (_uiState.value.isLoadingMore) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            repository.loadNextBatch().onSuccess { batch ->
                _uiState.update { state ->
                    state.copy(
                        stories = (state.stories + batch).distinctBy { it.id },
                        isInitialLoading = false,
                        isLoadingMore = false
                    )
                }
            }.onFailure {
                // Existing cache remains visible. A first-load failure simply hides the section.
                _uiState.update { it.copy(isInitialLoading = false, isLoadingMore = false) }
            }
        }
    }

    fun preloadIfNeeded(index: Int) {
        if (index >= _uiState.value.stories.lastIndex - PRELOAD_THRESHOLD) loadMore()
    }

    private companion object { const val PRELOAD_THRESHOLD = 5 }
}
