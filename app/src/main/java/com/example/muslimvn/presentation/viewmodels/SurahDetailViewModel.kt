package com.example.muslimvn.presentation.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muslimvn.data.preferences.QuranDisplayMode
import com.example.muslimvn.data.preferences.QuranPreferences
import com.example.muslimvn.data.util.AudioPlayerManager
import com.example.muslimvn.data.util.QuranAudioUrlBuilder
import com.example.muslimvn.domain.usecases.GetSurahDetailUseCase
import com.example.muslimvn.domain.usecases.SurahDetail
import com.example.muslimvn.domain.usecases.ToggleBookmarkUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuranUiSettings(
    val reciterIdentifier: String = "Alafasy_128kbps",
    val fontSize: Float = 18f,
    val displayMode: QuranDisplayMode = QuranDisplayMode.BOTH,
    val hapticEnabled: Boolean = true
)

@HiltViewModel
class SurahDetailViewModel @Inject constructor(
    private val getSurahDetailUseCase: GetSurahDetailUseCase,
    private val toggleBookmarkUseCase: ToggleBookmarkUseCase,
    private val audioPlayerManager: AudioPlayerManager,
    private val quranPreferences: QuranPreferences,
    private val quranRepository: com.example.muslimvn.domain.repository.QuranRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val surahNumber: Int = checkNotNull(savedStateHandle["surahNumber"])

    private val _state = MutableStateFlow<SurahDetailState>(SurahDetailState.Loading)
    val state = _state.asStateFlow()

    val isPlaying = audioPlayerManager.isPlaying
    val isBuffering = audioPlayerManager.isBuffering
    val currentMediaId = audioPlayerManager.currentMediaId
    val positionMs = audioPlayerManager.positionMs

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _syncProgress = MutableStateFlow(0f)
    val syncProgress = _syncProgress.asStateFlow()

    private val _playlist = MutableStateFlow<List<com.example.muslimvn.domain.models.Ayah>>(emptyList())
    val playlist = _playlist.asStateFlow()

    private val _currentTiming = MutableStateFlow<com.example.muslimvn.domain.models.VerseTiming?>(null)
    val playingWordIndex: StateFlow<Int?> = combine(positionMs, _currentTiming) { pos, timing ->
        timing?.segments?.find { pos in it.startTimeMs..it.endTimeMs }?.wordIndex
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val quranSettings = combine(
        quranPreferences.reciterIdentifier,
        quranPreferences.fontSize,
        quranPreferences.displayMode,
        quranPreferences.hapticEnabled
    ) { reciter, fontSize, displayMode, haptic ->
        QuranUiSettings(reciter, fontSize, displayMode, haptic)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QuranUiSettings())

    init {
        loadSurahDetail()
        observeMediaTiming()
        prefetchTiming()
    }

    private fun prefetchTiming() {
        viewModelScope.launch {
            val settings = quranSettings.filter { it.reciterIdentifier.isNotEmpty() }.first()
            val reciter = com.example.muslimvn.domain.models.availableReciters.find { 
                it.identifier == settings.reciterIdentifier 
            } ?: com.example.muslimvn.domain.models.availableReciters[0]
            
            _isSyncing.value = true
            quranRepository.prefetchSurahTiming(surahNumber, reciter.quranComId) { progress ->
                _syncProgress.value = progress
            }
            _isSyncing.value = false
        }
    }

    private fun observeMediaTiming() {
        viewModelScope.launch {
            currentMediaId.collect { id ->
                if (id != null && id.contains(":")) {
                    val settings = quranSettings.value
                    val reciter = com.example.muslimvn.domain.models.availableReciters.find { 
                        it.identifier == settings.reciterIdentifier 
                    } ?: com.example.muslimvn.domain.models.availableReciters[0]
                    
                    _currentTiming.value = quranRepository.getVerseTiming(id, reciter.quranComId)
                } else {
                    _currentTiming.value = null
                }
            }
        }
    }

    fun retry() {
        _state.value = SurahDetailState.Loading
        loadSurahDetail()
        prefetchTiming()
    }

    private fun loadSurahDetail() {
        getSurahDetailUseCase(surahNumber)
            .onEach { surahDetail ->
                if (surahDetail != null) {
                    _state.value = SurahDetailState.Success(surahDetail)
                    _playlist.value = surahDetail.ayahs
                } else {
                    _state.value = SurahDetailState.Error("Surah not found")
                }
            }
            .launchIn(viewModelScope)
    }

    fun toggleBookmark(ayahId: Int, isBookmarked: Boolean) {
        viewModelScope.launch {
            toggleBookmarkUseCase(ayahId, isBookmarked)
            val current = _playlist.value
            _playlist.value = current.map { if (it.id == ayahId) it.copy(isBookmarked = isBookmarked) else it }
        }
    }

    fun playAyah(ayahNumber: Int) {
        val currentState = _state.value
        if (currentState !is SurahDetailState.Success) return
        
        val surah = currentState.surahDetail.surah
        val settings = quranSettings.value
        val mediaId = "$surahNumber:$ayahNumber"
        
        if (currentMediaId.value == mediaId) {
            if (isPlaying.value) audioPlayerManager.pause() else audioPlayerManager.resume()
            return
        }

        val items = currentState.surahDetail.ayahs.map { ayah ->
            com.example.muslimvn.data.util.AudioPlayItem(
                url = QuranAudioUrlBuilder.buildAyahUrl(surahNumber, ayah.ayahNumber, settings.reciterIdentifier),
                mediaId = "$surahNumber:${ayah.ayahNumber}",
                title = "${surah.nameVietnamese} - Câu ${ayah.ayahNumber}",
                artist = "Quran Recitation",
                artworkPath = "icon/quran.png"
            )
        }
        
        audioPlayerManager.playList(items, startIndex = ayahNumber - 1)
    }

    fun playContinuous(startAyahNumber: Int) {
        playAyah(startAyahNumber)
    }

    fun stopAudio() {
        audioPlayerManager.stop()
    }

    override fun onCleared() {
        super.onCleared()
    }
}

sealed class SurahDetailState {
    object Loading : SurahDetailState()
    data class Success(val surahDetail: SurahDetail) : SurahDetailState()
    data class Error(val message: String) : SurahDetailState()
}
