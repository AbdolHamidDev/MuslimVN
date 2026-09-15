package com.example.muslimvn.presentation.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoutubePlayerScreen(
    onBackClick: () -> Unit
) {
    YoutubePlayerDetailScreen(onBackClick = onBackClick)
}
