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
    val displayMode: QuranDisplayMode = QuranDisplayMode.BOTH
)

@HiltViewModel
class SurahDetailViewModel @Inject constructor(
    private val getSurahDetailUseCase: GetSurahDetailUseCase,
    private val toggleBookmarkUseCase: ToggleBookmarkUseCase,
    private val audioPlayerManager: AudioPlayerManager,
    private val quranPreferences: QuranPreferences,
    private val quranRepository: com.example.muslimvn.domain.repository.QuranRepository,
    private val translator: com.example.muslimvn.data.util.TafsirTranslator,
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

    private val _tafsirState = MutableStateFlow<TafsirState>(TafsirState.Idle)
    val tafsirState = _tafsirState.asStateFlow()

    private val _translationState = MutableStateFlow<TranslationState>(TranslationState.Idle)
    val translationState = _translationState.asStateFlow()

    private val _currentTafsirAyah = MutableStateFlow<com.example.muslimvn.domain.models.Ayah?>(null)
    val currentTafsirAyah = _currentTafsirAyah.asStateFlow()

    private val _playlist = MutableStateFlow<List<com.example.muslimvn.domain.models.Ayah>>(emptyList())
    val playlist = _playlist.asStateFlow()

    private val _currentTiming = MutableStateFlow<com.example.muslimvn.domain.models.VerseTiming?>(null)
    val playingWordIndex: StateFlow<Int?> = combine(positionMs, _currentTiming) { pos, timing ->
        timing?.segments?.find { pos in it.startTimeMs..it.endTimeMs }?.wordIndex
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val quranSettings = combine(
        quranPreferences.reciterIdentifier,
        quranPreferences.fontSize,
        quranPreferences.displayMode
    ) { reciter, fontSize, displayMode ->
        QuranUiSettings(reciter, fontSize, displayMode)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QuranUiSettings())

    init {
        loadSurahDetail()
        observeMediaTiming()
        prefetchTiming()
        observeTranslationProgress()
    }

    private fun observeTranslationProgress() {
        translator.isDownloading
            .onEach { isDownloading ->
                if (isDownloading) {
                    _translationState.value = TranslationState.DownloadingModel
                }
            }
            .launchIn(viewModelScope)
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
        val mediaId = "${surah.number}:$ayahNumber"
        
        // Nếu đang phát đúng câu này rồi -> Chỉ toggle Play/Pause
        if (currentMediaId.value == mediaId) {
            if (isPlaying.value) audioPlayerManager.pause() else audioPlayerManager.resume()
            return
        }

        // Nếu chuyển sang câu mới -> Nạp playlist mới (Gapless)
        val items = currentState.surahDetail.ayahs.map { ayah ->
            com.example.muslimvn.data.util.AudioPlayItem(
                url = QuranAudioUrlBuilder.buildAyahUrl(surah.number, ayah.ayahNumber, settings.reciterIdentifier),
                mediaId = "${surah.number}:${ayah.ayahNumber}",
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

    fun loadTafsir(ayah: com.example.muslimvn.domain.models.Ayah) {
        val verseKey = "${ayah.surahId}:${ayah.ayahNumber}"
        _currentTafsirAyah.value = ayah
        _tafsirState.value = TafsirState.Loading
        viewModelScope.launch {
            val tafsir = quranRepository.getTafsir(verseKey)
            if (tafsir != null) {
                _tafsirState.value = TafsirState.Success(tafsir)
            } else {
                _tafsirState.value = TafsirState.Error("Could not load Tafsir")
            }
        }
    }

    fun clearTafsir() {
        _tafsirState.value = TafsirState.Idle
        _currentTafsirAyah.value = null
        _translationState.value = TranslationState.Idle
    }

    fun translateCurrentTafsir() {
        val currentState = _tafsirState.value
        if (currentState !is TafsirState.Success) return
        
        val tafsir = currentState.tafsir
        // Nếu đã có bản dịch rồi thì không dịch lại
        if (tafsir.translatedText != null) return
        
        viewModelScope.launch {
            _translationState.value = TranslationState.Translating
            val result = quranRepository.translateTafsir(tafsir.verseKey, tafsir.text)
            if (result != null) {
                _tafsirState.value = TafsirState.Success(tafsir.copy(translatedText = result))
                _translationState.value = TranslationState.Success
            } else {
                _translationState.value = TranslationState.Error("Translation failed")
            }
        }
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

sealed class TafsirState {
    object Idle : TafsirState()
    object Loading : TafsirState()
    data class Success(val tafsir: com.example.muslimvn.domain.models.Tafsir) : TafsirState()
    data class Error(val message: String) : TafsirState()
}

sealed class TranslationState {
    object Idle : TranslationState()
    object DownloadingModel : TranslationState()
    object Translating : TranslationState()
    object Success : TranslationState()
    data class Error(val message: String) : TranslationState()
}
