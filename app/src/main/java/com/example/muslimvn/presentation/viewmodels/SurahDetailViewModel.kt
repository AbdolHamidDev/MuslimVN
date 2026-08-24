package com.example.muslimvn.presentation.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muslimvn.data.util.AudioPlayerManager
import com.example.muslimvn.data.util.QuranAudioUrlBuilder
import com.example.muslimvn.domain.usecases.GetSurahDetailUseCase
import com.example.muslimvn.domain.usecases.SurahDetail
import com.example.muslimvn.domain.usecases.ToggleBookmarkUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SurahDetailViewModel @Inject constructor(
    private val getSurahDetailUseCase: GetSurahDetailUseCase,
    private val toggleBookmarkUseCase: ToggleBookmarkUseCase,
    private val audioPlayerManager: AudioPlayerManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val surahNumber: Int = checkNotNull(savedStateHandle["surahNumber"])

    private val _state = MutableStateFlow<SurahDetailState>(SurahDetailState.Loading)
    val state = _state.asStateFlow()

    val isPlaying = audioPlayerManager.isPlaying
    val isBuffering = audioPlayerManager.isBuffering
    val currentMediaId = audioPlayerManager.currentMediaId

    init {
        loadSurahDetail()
    }

    /** Tải lại chi tiết surah sau khi gặp lỗi (gọi từ nút "Thử lại"). */
    fun retry() {
        _state.value = SurahDetailState.Loading
        loadSurahDetail()
    }

    private fun loadSurahDetail() {
        getSurahDetailUseCase(surahNumber)
            .onEach { surahDetail ->
                if (surahDetail != null) {
                    _state.value = SurahDetailState.Success(surahDetail)
                } else {
                    _state.value = SurahDetailState.Error("Surah not found")
                }
            }
            .launchIn(viewModelScope)
    }

    fun toggleBookmark(ayahId: Int, isBookmarked: Boolean) {
        viewModelScope.launch {
            toggleBookmarkUseCase(ayahId, isBookmarked)
        }
    }

    fun playAyah(ayahNumber: Int) {
        val url = QuranAudioUrlBuilder.buildAyahUrl(surahNumber, ayahNumber)
        val mediaId = "$surahNumber:$ayahNumber"
        
        if (currentMediaId.value == mediaId) {
            if (isPlaying.value) {
                audioPlayerManager.pause()
            } else {
                audioPlayerManager.resume()
            }
        } else {
            audioPlayerManager.play(url, mediaId) {
                // Individual play doesn't auto-advance by default unless requested
            }
        }
    }

    fun playContinuous(startAyahNumber: Int) {
        playAyahRecursive(startAyahNumber)
    }

    private fun playAyahRecursive(ayahNumber: Int) {
        val currentState = _state.value
        if (currentState is SurahDetailState.Success) {
            val totalAyahs = currentState.surahDetail.surah.totalAyahs
            if (ayahNumber <= totalAyahs) {
                val url = QuranAudioUrlBuilder.buildAyahUrl(surahNumber, ayahNumber)
                val mediaId = "$surahNumber:$ayahNumber"
                audioPlayerManager.play(url, mediaId) {
                    playAyahRecursive(ayahNumber + 1)
                }
            } else {
                audioPlayerManager.stop()
            }
        }
    }

    fun stopAudio() {
        audioPlayerManager.stop()
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayerManager.release()
    }
}

sealed class SurahDetailState {
    object Loading : SurahDetailState()
    data class Success(val surahDetail: SurahDetail) : SurahDetailState()
    data class Error(val message: String) : SurahDetailState()
}
