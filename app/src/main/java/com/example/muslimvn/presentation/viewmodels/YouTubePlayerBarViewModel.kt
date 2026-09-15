package com.example.muslimvn.presentation.viewmodels

import androidx.lifecycle.ViewModel
import com.example.muslimvn.data.util.YouTubePlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class YouTubePlayerBarViewModel @Inject constructor(
    val playerManager: YouTubePlayerManager
) : ViewModel()
