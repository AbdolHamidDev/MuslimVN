package com.example.muslimvn.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import com.example.muslimvn.presentation.components.RoadmapCategory
import com.example.muslimvn.presentation.components.RoadmapItem

object RoadmapData {
    fun getIbadahCategory(
        onPrayerTimesClick: () -> Unit,
        onQiblaClick: () -> Unit,
        onHijriCalendarClick: () -> Unit,
        onNamesOfAllahClick: () -> Unit,
        onFeatureClick: (String) -> Unit
    ) = RoadmapCategory(
        title = "1. Ibadah",
        items = listOf(
            RoadmapItem("Giờ cầu nguyện", Icons.Default.Schedule) { onPrayerTimesClick() },
            RoadmapItem("Qibla", Icons.Default.Explore) { onQiblaClick() },
            RoadmapItem("99 Danh xưng", Icons.Default.AutoAwesome) { onNamesOfAllahClick() },
            RoadmapItem("Azkar", Icons.AutoMirrored.Filled.FormatListBulleted) { onFeatureClick("Azkar") },
            RoadmapItem("Lịch Hijri", Icons.Default.Event) { onHijriCalendarClick() },
        )
    )

    fun getKnowledgeCategory(
        onPodcastClick: () -> Unit
    ) = RoadmapCategory(
        title = "2. Kiến thức",
        items = listOf(
            RoadmapItem("Podcast học giả", Icons.Default.Podcasts) { onPodcastClick() },
        )
    )

    fun getUtilitiesCategory(
        onZakatClick: () -> Unit
    ) = RoadmapCategory(
        title = "3. Tiện ích",
        items = listOf(
            RoadmapItem("Zakat calc", Icons.Default.Calculate) { onZakatClick() },
        )
    )

    fun getLocalCategory() = RoadmapCategory(
        title = "4. Local Việt Nam",
        items = emptyList()
    )
}
