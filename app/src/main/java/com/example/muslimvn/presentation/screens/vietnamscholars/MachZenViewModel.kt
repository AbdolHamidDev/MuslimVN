package com.example.muslimvn.presentation.screens.vietnamscholars

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muslimvn.data.util.AudioPlayerManager
import com.example.muslimvn.domain.models.ScholarDocument
import com.example.muslimvn.domain.models.YoutubeVideo
import com.example.muslimvn.domain.repository.IslamHouseRepository
import com.example.muslimvn.domain.repository.YoutubeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MachZenUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val videos: List<YoutubeVideo> = emptyList(),
    val hasNextPage: Boolean = true,
    val error: String? = null,
    val isGridMode: Boolean = true, // Mặc định là dạng thẻ 1 cột
    val selectedTab: Int = 0, // 0: Video, 1: Tài liệu
    val documents: List<ScholarDocument> = emptyList(),
    val filteredDocuments: List<ScholarDocument> = emptyList(),
    val selectedDocType: String = "TẤT CẢ",
    val isLoadingDocuments: Boolean = false,
    val isLoadingMoreDocuments: Boolean = false,
    val documentHasNextPage: Boolean = true,
    val documentCurrentPage: Int = 1,
    val documentError: String? = null
)

@HiltViewModel
@androidx.media3.common.util.UnstableApi
class MachZenViewModel @Inject constructor(
    private val youtubeRepository: YoutubeRepository,
    private val islamHouseRepository: IslamHouseRepository,
    private val audioPlayerManager: AudioPlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MachZenUiState())
    val uiState: StateFlow<MachZenUiState> = _uiState.asStateFlow()

    init {
        fetchYoutubeVideos()
        fetchDocuments()
    }

    fun setSelectedTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
        if ((tabIndex == 1 || tabIndex == 2) && _uiState.value.documents.isEmpty() && !_uiState.value.isLoadingDocuments) {
            fetchDocuments()
        }
    }

    fun toggleViewMode() {
        _uiState.update { it.copy(isGridMode = !it.isGridMode) }
    }

    fun setDocumentType(type: String) {
        _uiState.update { state ->
            val filtered = if (type == "TẤT CẢ") {
                state.documents
            } else {
                state.documents.filter { it.fileExtension?.uppercase() == type }
            }
            state.copy(selectedDocType = type, filteredDocuments = filtered)
        }
    }

    private fun fetchYoutubeVideos() {
        val url = "https://www.youtube.com/@islamlavn/videos"
        
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

        val url = "https://www.youtube.com/@islamlavn/videos"
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

    private fun fetchDocuments() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDocuments = true, documentError = null) }
            islamHouseRepository.getAuthorDocuments(authorId = 193689, page = 1).collect { result ->
                result.onSuccess { (documents, hasNext) ->
                    _uiState.update {
                        val currentType = it.selectedDocType
                        val filtered = if (currentType == "TẤT CẢ") documents else documents.filter { d -> d.fileExtension?.uppercase() == currentType }
                        it.copy(
                            isLoadingDocuments = false,
                            documents = documents,
                            filteredDocuments = filtered,
                            documentHasNextPage = hasNext,
                            documentCurrentPage = 1
                        )
                    }
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoadingDocuments = false,
                            documentError = e.message ?: "Lỗi tải tài liệu"
                        )
                    }
                }
            }
        }
    }

    fun loadMoreDocuments() {
        val currentState = _uiState.value
        if (currentState.isLoadingMoreDocuments || !currentState.documentHasNextPage) return

        val nextPage = currentState.documentCurrentPage + 1
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreDocuments = true) }
            val result = islamHouseRepository.loadMoreDocuments(authorId = 193689, page = nextPage)
            result.onSuccess { (newDocs, hasNext) ->
                _uiState.update { state ->
                    val allDocs = state.documents + newDocs
                    val currentType = state.selectedDocType
                    val filtered = if (currentType == "TẤT CẢ") allDocs else allDocs.filter { d -> d.fileExtension?.uppercase() == currentType }
                    state.copy(
                        isLoadingMoreDocuments = false,
                        documents = allDocs,
                        filteredDocuments = filtered,
                        documentHasNextPage = hasNext,
                        documentCurrentPage = nextPage
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isLoadingMoreDocuments = false) }
            }
        }
    }

    fun playAudioDocument(doc: ScholarDocument) {
        if (doc.downloadUrl.isNullOrBlank()) return

        val mp3Docs = uiState.value.documents.filter {
            it.fileExtension?.equals("MP3", ignoreCase = true) == true && !it.downloadUrl.isNullOrBlank()
        }
        val startIndex = mp3Docs.indexOfFirst { it.id == doc.id }.coerceAtLeast(0)

        val items = mp3Docs.map { d ->
            com.example.muslimvn.data.util.AudioPlayItem(
                url = d.downloadUrl.orEmpty(),
                mediaId = "islamhouse_${d.id}",
                title = d.title,
                artist = "Mách Zên",
                artworkPath = "images/featured_scholars_vietnam/mach_zen.webp"
            )
        }

        if (items.isNotEmpty()) {
            audioPlayerManager.playList(
                items = items,
                startIndex = startIndex,
                isPodcast = true
            )
        }
    }
}
