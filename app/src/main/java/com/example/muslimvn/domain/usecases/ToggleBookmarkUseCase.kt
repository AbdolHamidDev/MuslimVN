package com.example.muslimvn.domain.usecases

import com.example.muslimvn.domain.repository.QuranRepository
import javax.inject.Inject

class ToggleBookmarkUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    suspend operator fun invoke(ayahId: Int, isBookmarked: Boolean) {
        repository.toggleBookmark(ayahId, isBookmarked)
    }
}
