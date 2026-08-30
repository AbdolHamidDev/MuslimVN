package com.example.muslimvn.presentation.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muslimvn.data.preferences.QuranDisplayMode
import com.example.muslimvn.data.preferences.QuranPreferences
import com.example.muslimvn.data.preferences.QuranViewMode
import com.example.muslimvn.data.util.AudioPlayerManager
import com.example.muslimvn.data.util.QuranAudioUrlBuilder
import com.example.muslimvn.domain.usecases.GetSurahDetailUseCase
import com.example.muslimvn.domain.usecases.SurahDetail
import com.example.muslimvn.domain.usecases.ToggleBookmarkUseCase
import com.example.muslimvn.domain.usecases.UpdateTrackerQuranUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuranUiSettings(
    val reciterIdentifier: String = "Alafasy_128kbps",
    val fontSize: Float = 24f,
    val displayMode: QuranDisplayMode = QuranDisplayMode.BOTH,
    val viewMode: QuranViewMode = QuranViewMode.LIST
)

@HiltViewModel
class SurahDetailViewModel @Inject constructor(
    private val getSurahDetailUseCase: com.example.muslimvn.domain.usecases.GetSurahDetailUseCase,
    private val toggleBookmarkUseCase: com.example.muslimvn.domain.usecases.ToggleBookmarkUseCase,
    private val updateTrackerQuranUseCase: com.example.muslimvn.domain.usecases.UpdateTrackerQuranUseCase,
    private val getQuranAudioPlaylistUseCase: com.example.muslimvn.domain.usecases.GetQuranAudioPlaylistUseCase,
    private val audioPlayerManager: AudioPlayerManager,
    private val quranPreferences: QuranPreferences,
    private val quranRepository: com.example.muslimvn.domain.repository.QuranRepository,
    private val translator: com.example.muslimvn.data.util.TafsirTranslator,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val initialSurahNumber: Int = checkNotNull(savedStateHandle["surahNumber"])
    private val startAyah: Int = savedStateHandle["startAyah"] ?: 1

    private val _surahNumberFlow = MutableStateFlow(initialSurahNumber)
    private val _refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val state: StateFlow<SurahDetailState> = combine(_surahNumberFlow, _refreshTrigger) { number, _ -> number }
        .flatMapLatest { number ->
            getSurahDetailUseCase(number).map { surahDetail ->
                if (surahDetail != null) {
                    _playlist.value = surahDetail.ayahs
                    currentSurahName = surahDetail.surah.nameVietnamese
                    
                    // Cập nhật tracker khi đổi Surah (Chỉ khi load thành công)
                    val aNum = if (number == initialSurahNumber) startAyah else 1
                    updateTrackerForAyah(number, aNum, surahDetail.surah.nameVietnamese, surahDetail.surah.totalAyahs)
                    
                    SurahDetailState.Success(surahDetail)
                } else {
                    SurahDetailState.Error("Surah not found")
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SurahDetailState.Loading)

    private var currentSurahName: String = ""

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

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress = _downloadProgress.asStateFlow()

    private val _downloadedCount = MutableStateFlow(0)
    val downloadedCount = _downloadedCount.asStateFlow()

    private val _currentTiming = MutableStateFlow<com.example.muslimvn.domain.models.VerseTiming?>(null)
    val playingWordIndex: StateFlow<Int?> = combine(positionMs, _currentTiming) { pos, timing ->
        timing?.segments?.find { pos in it.startTimeMs..it.endTimeMs }?.wordIndex
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val quranSettings = combine(
        quranPreferences.reciterIdentifier,
        quranPreferences.fontSize,
        quranPreferences.displayMode,
        quranPreferences.viewMode
    ) { reciter, fontSize, displayMode, viewMode ->
        QuranUiSettings(reciter, fontSize, displayMode, viewMode)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QuranUiSettings())

    private val _currentMushafPage = MutableStateFlow(1)
    val currentMushafPage = _currentMushafPage.asStateFlow()

    // Tọa độ highlight cho Mushaf
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val ayahHighlightCoordinates: StateFlow<List<List<Int>>> = currentMediaId
        .flatMapLatest { id ->
            if (id != null && id.contains(":")) {
                // Tự động lật trang nếu câu đang phát thuộc trang khác
                syncMushafPageWithAudio(id)
                flow { emit(quranRepository.getAyahCoordinates(id)) }
            } else {
                flow { emit(emptyList<List<Int>>()) }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun syncMushafPageWithAudio(verseKey: String) {
        viewModelScope.launch {
            val parts = verseKey.split(":")
            val s = parts[0].toIntOrNull() ?: return@launch
            val a = parts[1].toIntOrNull() ?: return@launch
            
            val targetPage = quranRepository.getPageForAyah(s, a)
            if (targetPage != _currentMushafPage.value) {
                _currentMushafPage.value = targetPage
            }
        }
    }

    private var lastVisibleAyah: Int = startAyah

    init {
        observeMediaTiming()
        observeSurahAndReciterForTiming()
        observeTranslationProgress()
        observeDownloadStatus()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun observeDownloadStatus() {
        viewModelScope.launch {
            combine(_surahNumberFlow, quranSettings.map { it.reciterIdentifier }.distinctUntilChanged()) { sNum, reciterId ->
                sNum to reciterId
            }
                .flatMapLatest { (sNum, reciterIdentifier) ->
                    val reciter = com.example.muslimvn.domain.models.availableReciters.find {
                        it.identifier == reciterIdentifier
                    } ?: com.example.muslimvn.domain.models.availableReciters[0]
                    
                    quranRepository.getDownloadedAyahsCount(sNum, reciter.quranComId)
                }
                .collect { count ->
                    _downloadedCount.value = count
                    val total = (state.value as? SurahDetailState.Success)?.surahDetail?.surah?.totalAyahs ?: 0
                    if (total > 0) {
                        _downloadProgress.value = count.toFloat() / total
                    }
                }
        }
    }

    fun downloadSurah() {
        viewModelScope.launch {
            val settings = quranSettings.value
            val reciter = com.example.muslimvn.domain.models.availableReciters.find {
                it.identifier == settings.reciterIdentifier
            } ?: com.example.muslimvn.domain.models.availableReciters[0]
            
            quranRepository.startSurahDownload(_surahNumberFlow.value, reciter.quranComId)
        }
    }

    fun getStartAyah(): Int = startAyah

    private fun observeTranslationProgress() {
        translator.isDownloading
            .onEach { isDownloading ->
                if (isDownloading) {
                    _translationState.value = TranslationState.DownloadingModel
                }
            }
            .launchIn(viewModelScope)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun observeSurahAndReciterForTiming() {
        combine(_surahNumberFlow, quranSettings.map { it.reciterIdentifier }.distinctUntilChanged()) { sNum, reciterId ->
            sNum to reciterId
        }
        .onEach { (sNum, reciterId) ->
            if (reciterId.isNotEmpty()) {
                prefetchTiming(sNum, reciterId)
            }
        }
        .launchIn(viewModelScope)
    }

    private fun prefetchTiming(sNum: Int, reciterId: String) {
        viewModelScope.launch {
            val reciter = com.example.muslimvn.domain.models.availableReciters.find { 
                it.identifier == reciterId 
            } ?: com.example.muslimvn.domain.models.availableReciters[0]
            
            _isSyncing.value = true
            quranRepository.prefetchSurahTiming(sNum, reciter.quranComId) { progress ->
                _syncProgress.value = progress
            }
            _isSyncing.value = false
        }
    }

    private fun observeMediaTiming() {
        viewModelScope.launch {
            combine(currentMediaId, quranSettings.map { it.reciterIdentifier }.distinctUntilChanged()) { id, _ -> id }
                .collect { id ->
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
        _refreshTrigger.tryEmit(Unit)
    }

    private fun calculateProgress(ayah: Int, total: Int): Float {
        return if (total > 0) ayah.toFloat() / total.toFloat() else 0f
    }

    private fun updateTrackerForAyah(sNum: Int, aNum: Int, sName: String, total: Int) {
        viewModelScope.launch {
            updateTrackerQuranUseCase(
                surahName = sName,
                surahNumber = sNum,
                ayahNumber = aNum,
                progress = calculateProgress(aNum, total)
            )
        }
    }

    fun updateLastReadAyah(ayahNumber: Int) {
        updateLastReadAyah(_surahNumberFlow.value, ayahNumber)
    }

    fun updateLastReadAyah(sNum: Int, aNum: Int) {
        lastVisibleAyah = aNum
        val currentState = state.value
        if (currentState !is SurahDetailState.Success) return
        
        val currentSurah = currentState.surahDetail.surah
        // Chỉ cập nhật nếu đúng Surah đang hiển thị (hoặc nếu ta cho phép cập nhật chéo)
        val sName = if (sNum == currentSurah.number) currentSurah.nameVietnamese else ""
        val total = if (sNum == currentSurah.number) currentSurah.totalAyahs else 0
        
        if (sName.isNotEmpty()) {
            updateTrackerForAyah(sNum, aNum, sName, total)
        }
    }

    fun toggleBookmark(ayahId: Int, isBookmarked: Boolean) {
        viewModelScope.launch {
            toggleBookmarkUseCase(ayahId, isBookmarked)
            val current = _playlist.value
            _playlist.value = current.map { if (it.id == ayahId) it.copy(isBookmarked = isBookmarked) else it }
        }
    }

    fun playAyah(ayahNumber: Int, forceReload: Boolean = false, startPlaying: Boolean = true) {
        viewModelScope.launch {
            val currentState = state.value
            if (currentState !is SurahDetailState.Success) return@launch
            
            val surah = currentState.surahDetail.surah
            val settings = quranSettings.value
            val mediaId = "${surah.number}:$ayahNumber"
            
            // Nếu đang phát đúng câu này rồi và không bắt buộc nạp lại -> Chỉ toggle Play/Pause
            if (!forceReload && currentMediaId.value == mediaId) {
                if (isPlaying.value) audioPlayerManager.pause() else audioPlayerManager.resume()
                return@launch
            }

            // Nếu buộc nạp lại khi đang phát, dừng tạm thời để chờ nạp
            if (forceReload && isPlaying.value) {
                audioPlayerManager.pause()
            }

            // Hiển thị trạng thái chuẩn bị (nếu cần)
            _isSyncing.value = true

            // Sử dụng UseCase để lấy playlist chuẩn (Gapless)
            val items = getQuranAudioPlaylistUseCase(surah.number, settings.reciterIdentifier)
            
            _isSyncing.value = false
            if (items.isNotEmpty()) {
                audioPlayerManager.playList(items, startIndex = ayahNumber - 1, playWhenReady = startPlaying)
            }
        }
    }

    fun playContinuous(startAyahNumber: Int) {
        playAyah(startAyahNumber)
    }

    fun pauseAudio() {
        audioPlayerManager.pause()
    }

    fun resumeAudio() {
        audioPlayerManager.resume()
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

    fun toggleViewMode() {
        viewModelScope.launch {
            val settings = quranSettings.value
            val newMode = if (settings.viewMode == QuranViewMode.LIST) QuranViewMode.MUSHAF else QuranViewMode.LIST
            
            if (newMode == QuranViewMode.MUSHAF) {
                // Sync List -> Mushaf
                val ayahToSync = getAyahToSync()
                val page = quranRepository.getPageForAyah(_surahNumberFlow.value, ayahToSync)
                _currentMushafPage.value = page
            } else {
                // Sync Mushaf -> List (sẽ được xử lý ở Screen qua LaunchedEffect hoặc method này trả về target)
            }
            
            quranPreferences.saveViewMode(newMode)
        }
    }

    private fun getAyahToSync(): Int {
        val playingId = currentMediaId.value
        val sNum = _surahNumberFlow.value
        if (playingId != null && playingId.startsWith("$sNum:")) {
            return playingId.split(":")[1].toIntOrNull() ?: lastVisibleAyah
        }
        return lastVisibleAyah
    }

    fun onMushafPageChanged(page: Int) {
        if (_currentMushafPage.value != page) {
            _currentMushafPage.value = page
            // Tùy chọn: Cập nhật tracker và Surah khi lật trang Mushaf
            viewModelScope.launch {
                val ayahs = quranRepository.getAyahsForPage(page)
                if (ayahs.isNotEmpty()) {
                    val parts = ayahs[0].split(":")
                    val sNum = parts[0].toIntOrNull() ?: return@launch
                    val aNum = parts[1].toIntOrNull() ?: 1
                    
                    if (sNum != _surahNumberFlow.value) {
                        _surahNumberFlow.value = sNum
                    }
                    updateLastReadAyah(sNum, aNum)
                }
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
