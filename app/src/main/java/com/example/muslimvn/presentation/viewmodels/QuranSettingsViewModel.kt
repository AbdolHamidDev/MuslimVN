package com.example.muslimvn.presentation.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muslimvn.data.preferences.QuranDisplayMode
import com.example.muslimvn.data.preferences.QuranPreferences
import com.example.muslimvn.data.preferences.QuranViewMode
import com.example.muslimvn.domain.models.DownloadStatus
import com.example.muslimvn.domain.models.Reciter
import com.example.muslimvn.domain.repository.QuranRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class QuranSettingsViewModel @Inject constructor(
    private val preferences: QuranPreferences,
    private val quranRepository: QuranRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val surahNumber: Int = savedStateHandle["surahNumber"] ?: -1

    private val _showDownloadConfirmDialog = MutableStateFlow<Reciter?>(null)
    val showDownloadConfirmDialog: StateFlow<Reciter?> = _showDownloadConfirmDialog.asStateFlow()

    private val _showClearConfirmDialog = MutableStateFlow<Reciter?>(null)
    val showClearConfirmDialog: StateFlow<Reciter?> = _showClearConfirmDialog.asStateFlow()

    private val _showMushafDownloadConfirm = MutableStateFlow(false)
    val showMushafDownloadConfirm = _showMushafDownloadConfirm.asStateFlow()

    private val _showMushafClearConfirm = MutableStateFlow(false)
    val showMushafClearConfirm = _showMushafClearConfirm.asStateFlow()

    val reciterIdentifier: StateFlow<String> = preferences.reciterIdentifier
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Alafasy_128kbps")

    val hasSelectedReciter: StateFlow<Boolean> = preferences.hasSelectedReciter
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val fontSize: StateFlow<Float> = preferences.fontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 18f)

    val displayMode: StateFlow<QuranDisplayMode> = preferences.displayMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QuranDisplayMode.BOTH)

    val viewMode: StateFlow<QuranViewMode> = preferences.viewMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QuranViewMode.LIST)

    val mushafDownloadProgress = quranRepository.getMushafDownloadProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isMushafOffline = quranRepository.isMushafOffline()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val mushafDownloadStatus = quranRepository.getMushafDownloadStatus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DownloadStatus.IDLE)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getDownloadProgress(reciterId: Int): Flow<Float?> {
        // If we are in surah detail, show surah progress
        if (surahNumber != -1) {
            return quranRepository.getDownloadedAyahsCount(surahNumber, reciterId)
                .flatMapLatest { count ->
                    flow {
                        val surah = quranRepository.getSurahByNumber(surahNumber)
                        val total = surah?.totalAyahs ?: 0
                        if (total > 0) {
                            emit(count.toFloat() / total)
                        } else {
                            emit(0f)
                        }
                    }
                }
        }
        // Otherwise show full quran progress
        return quranRepository.getFullQuranDownloadProgress(reciterId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getDownloadStatus(reciterId: Int): Flow<DownloadStatus> {
        if (surahNumber == -1) {
            return quranRepository.getDownloadStatus(reciterId)
        }
        return quranRepository.getSurahDownloadStatus(surahNumber, reciterId)
    }

    fun onReciterSelected(reciter: Reciter) {
        viewModelScope.launch {
            preferences.saveReciterIdentifier(reciter.identifier)
        }
    }

    fun onDownloadIconClick(reciter: Reciter) {
        _showDownloadConfirmDialog.value = reciter
    }

    fun dismissDownloadConfirmDialog() {
        _showDownloadConfirmDialog.value = null
    }

    fun startFullQuranDownload(reciter: Reciter) {
        dismissDownloadConfirmDialog()
        quranRepository.startFullQuranDownload(reciter.quranComId, reciter.name)
    }

    fun pauseDownload(reciter: Reciter) {
        quranRepository.pauseDownload(reciter.quranComId)
    }

    fun onClearClick(reciter: Reciter) {
        _showClearConfirmDialog.value = reciter
    }

    fun dismissClearConfirmDialog() {
        _showClearConfirmDialog.value = null
    }

    fun confirmClearAudio(reciter: Reciter) {
        dismissClearConfirmDialog()
        if (surahNumber != -1) {
            quranRepository.clearSurahAudio(surahNumber, reciter.quranComId)
        } else {
            quranRepository.clearDownloadedAudio(reciter.quranComId)
        }
    }

    fun startDownload(reciter: Reciter) {
        if (surahNumber == -1) {
            onDownloadIconClick(reciter)
            return
        }
        viewModelScope.launch {
            quranRepository.startSurahDownload(surahNumber, reciter.quranComId)
        }
    }

    fun onFontSizeChanged(size: Float) {
        viewModelScope.launch {
            preferences.saveFontSize(size)
        }
    }

    fun onDisplayModeChanged(mode: QuranDisplayMode) {
        viewModelScope.launch {
            preferences.saveDisplayMode(mode)
        }
    }

    fun onViewModeChanged(mode: QuranViewMode) {
        viewModelScope.launch {
            preferences.saveViewMode(mode)
        }
    }

    fun onMushafDownloadClick() {
        _showMushafDownloadConfirm.value = true
    }

    fun dismissMushafDownloadConfirm() {
        _showMushafDownloadConfirm.value = false
    }

    fun startMushafDownload() {
        dismissMushafDownloadConfirm()
        quranRepository.startMushafDownload()
    }

    fun pauseMushafDownload() {
        quranRepository.pauseMushafDownload()
    }

    fun onMushafClearClick() {
        _showMushafClearConfirm.value = true
    }

    fun dismissMushafClearConfirm() {
        _showMushafClearConfirm.value = false
    }

    fun clearMushafData() {
        dismissMushafClearConfirm()
        quranRepository.clearMushafData()
    }
}
