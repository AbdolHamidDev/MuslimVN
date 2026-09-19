package com.example.muslimvn.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muslimvn.data.local.entities.AzkarEntity
import com.example.muslimvn.domain.repository.AzkarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AzkarViewModel @Inject constructor(
    private val repository: AzkarRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val categories: StateFlow<List<String>> = repository.getCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val azkarList: StateFlow<List<AzkarEntity>> = combine(
        _selectedCategory,
        _searchQuery
    ) { category, query ->
        category to query
    }.flatMapLatest { (category, query) ->
        if (query.trim().isNotEmpty()) {
            // TÌM KIẾM TOÀN CỤC: Bỏ qua danh mục đang chọn để quét trên toàn bộ dữ liệu
            repository.getAllAzkar().map { list ->
                list.filter { 
                    it.title.contains(query, ignoreCase = true) || 
                    it.contentVietnamese.contains(query, ignoreCase = true) ||
                    it.contentArabic.contains(query)
                }
            }
        } else {
            // LỌC THEO DANH MỤC: Chỉ chạy khi thanh tìm kiếm trống
            if (category == null) {
                repository.getAllAzkar()
            } else {
                repository.getAzkarByCategory(category)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.preloadAzkarIfNeeded()
        }
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavorite(azkar: AzkarEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(azkar.id, !azkar.isFavorite)
        }
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.preloadAzkarIfNeeded()
            } catch (_: Exception) {
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
