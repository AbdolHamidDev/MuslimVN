package com.example.muslimvn.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.muslimvn.domain.models.Hadith
import com.example.muslimvn.presentation.viewmodels.DailyReminderUiState

/** Home preview artwork; the complete hadith experience remains in the story viewer. */
@Composable
fun DailyReminderSection(state: DailyReminderUiState, onStoryClick: (Hadith) -> Unit) {
    if (state.stories.isEmpty() && !state.isInitialLoading) return
    val enabled = state.stories.isNotEmpty()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = enabled) { state.stories.firstOrNull()?.let(onStoryClick) },
        shape = RoundedCornerShape(20.dp)
    ) {
        AsyncImage(
            model = "images/hadith_card.png".toAndroidAssetUri(),
            contentDescription = "Một lời nhắc dành cho bạn",
            // The supplied PNG has transparent breathing room around the banner; crop it
            // to the actual card ratio so Home shows only the prepared artwork.
            modifier = Modifier.fillMaxWidth().aspectRatio(2.57f),
            contentScale = ContentScale.Crop
        )
    }
}
