package com.example.muslimvn.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muslimvn.domain.models.AllahName
import com.example.muslimvn.domain.repository.NameAllahRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel của màn hình 99 Danh Xưng Allah (Asmaul Husna).
 *
 * - Tải toàn bộ danh xưng từ [NameAllahRepository] một lần (repo tự cache).
 * - Lọc thời gian thực theo chữ Ả Rập / phiên âm / ý nghĩa tiếng Việt / số thứ tự.
 * - Quản lý danh xưng đang được chọn để hiển thị ModalBottomSheet chi tiết.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class NamesOfAllahViewModel @Inject constructor(
    private val nameAllahRepository: NameAllahRepository
) : ViewModel() {

    private val _allNames = MutableStateFlow<List<AllahName>>(emptyList())

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isError = MutableStateFlow(false)
    val isError: StateFlow<Boolean> = _isError.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedName = MutableStateFlow<AllahName?>(null)
    val selectedName: StateFlow<AllahName?> = _selectedName.asStateFlow()

    /** Danh xưng sau khi lọc theo từ khoá tìm kiếm. */
    val namesList: StateFlow<List<AllahName>> = combine(
        _allNames,
        _searchQuery
    ) { names, query -> names.filterByQuery(query) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = emptyList()
        )

    init {
        loadNames()
    }

    /** Tải lại dữ liệu; cũng được dùng cho nút "Thử lại" khi parse lỗi. */
    fun retryLoading() = loadNames()

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    /** Chọn một danh xưng → mở bottom sheet chi tiết. */
    fun onNameSelected(name: AllahName) {
        _selectedName.value = name
    }

    /** Đóng bottom sheet chi tiết. */
    fun dismissDetail() {
        _selectedName.value = null
    }

    private fun loadNames() {
        viewModelScope.launch {
            _isLoading.value = true
            _isError.value = false
            val result = runCatching { nameAllahRepository.getAllNames() }
            
            if (result.isSuccess) {
                val data = result.getOrThrow()
                _allNames.value = data
                _isError.value = data.isEmpty() // Nếu data rỗng thật sự thì coi như lỗi hoặc trống
            } else {
                _isError.value = true
            }
            _isLoading.value = false
        }
    }

    /**
     * Lọc không phân biệt hoa/thường, dấu tiếng Việt và khoảng trắng — người dùng
     * gõ "vua chua" vẫn khớp "Vua Chúa". Số thứ tự cũng được tìm trực tiếp ("99").
     */
    private fun List<AllahName>.filterByQuery(query: String): List<AllahName> {
        val normalizedQuery = normalizeForSearch(query)
        if (normalizedQuery.isEmpty()) return this

        return filter { name ->
            normalizeForSearch(name.transliteration).contains(normalizedQuery) ||
                normalizeForSearch(name.meaningVietnamese).contains(normalizedQuery) ||
                normalizeForSearch(name.nameArabic).contains(normalizedQuery) ||
                name.number.toString().contains(normalizedQuery)
        }
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 200L
        private const val STOP_TIMEOUT_MS = 5_000L

        private val DIACRITIC_MARKS = Regex("\\p{Mn}+")
        private val NON_ALPHANUMERIC = Regex("[^\\p{L}\\p{N}]+")

        /** Bỏ dấu (tiếng Việt & harakat Ả Rập), dấu câu, khoảng trắng và về chữ thường. */
        fun normalizeForSearch(input: String): String =
            Normalizer.normalize(input.lowercase(Locale.ROOT), Normalizer.Form.NFD)
                .replace(DIACRITIC_MARKS, "")
                .replace('đ', 'd')
                .replace(NON_ALPHANUMERIC, "")
    }
}
