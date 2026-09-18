package com.example.muslimvn.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muslimvn.domain.models.Surah
import com.example.muslimvn.domain.usecases.GetSurahsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class QuranViewModel @Inject constructor(
    private val getSurahsUseCase: GetSurahsUseCase,
    private val quranPreferences: com.example.muslimvn.data.preferences.QuranPreferences,
    private val audioPlayerManager: com.example.muslimvn.data.util.AudioPlayerManager
) : ViewModel() {

    val currentMediaId = audioPlayerManager.currentMediaId
    val isPlaying = audioPlayerManager.isPlaying
    
    val hasSelectedReciter = quranPreferences.hasSelectedReciter
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val quranSettings = combine(
        quranPreferences.reciterIdentifier,
        quranPreferences.fontSize,
        quranPreferences.displayMode
    ) { reciter, fontSize, displayMode ->
        com.example.muslimvn.presentation.viewmodels.QuranUiSettings(reciter, fontSize, displayMode)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.muslimvn.presentation.viewmodels.QuranUiSettings())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val surahs: StateFlow<List<Surah>> = _searchQuery
        .debounce(300L)
        .flatMapLatest { query ->
            getSurahsUseCase(query)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun togglePlayPause() {
        if (isPlaying.value) {
            audioPlayerManager.pause()
        } else {
            audioPlayerManager.resume()
        }
    }

    fun onReciterSelected(reciter: com.example.muslimvn.domain.models.Reciter) {
        viewModelScope.launch {
            quranPreferences.saveReciterIdentifier(reciter.identifier)
            // Lưu xong Coordinator sẽ tự động reload nếu đang phát. 
            // Nếu chưa phát thì không cần start tự động ở đây để tránh gây phiền hà.
        }
    }

    fun onDismissReciterSelection() {
        val defaultReciter = com.example.muslimvn.domain.models.availableReciters.firstOrNull { 
            it.name.contains("Mishary", ignoreCase = true) 
        } ?: com.example.muslimvn.domain.models.availableReciters[0]
        onReciterSelected(defaultReciter)
    }
}
