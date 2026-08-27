package com.example.muslimvn.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import com.example.muslimvn.presentation.components.RoadmapCategory
import com.example.muslimvn.presentation.components.RoadmapItem

object RoadmapData {
    fun getIbadahCategory(
        onQuranClick: () -> Unit,
        onPrayerTimesClick: () -> Unit,
        onQiblaClick: () -> Unit,
        onHijriCalendarClick: () -> Unit,
        onFeatureClick: (String) -> Unit
    ) = RoadmapCategory(
        title = "1. Ibadah",
        items = listOf(
            RoadmapItem("Quran offline", Icons.AutoMirrored.Filled.MenuBook) { onQuranClick() },
            RoadmapItem("Giờ cầu nguyện", Icons.Default.Schedule) { onPrayerTimesClick() },
            RoadmapItem("Qibla", Icons.Default.Explore) { onQiblaClick() },
            RoadmapItem("Azkar", Icons.Default.FormatListBulleted) { onFeatureClick("Azkar") },
            RoadmapItem("Dua", Icons.Default.VolunteerActivism) { onFeatureClick("Dua") },
            RoadmapItem("Tasbih", Icons.Default.RadioButtonChecked) { onFeatureClick("Tasbih") },
            RoadmapItem("Ramadan", Icons.Default.WbTwilight) { onFeatureClick("Ramadan") },
            RoadmapItem("Lịch Hijri", Icons.Default.Event) { onHijriCalendarClick() },
        )
    )

    fun getKnowledgeCategory(
        onZakatClick: () -> Unit,
        onFeatureClick: (String) -> Unit
    ) = RoadmapCategory(
        title = "2. Kiến thức",
        items = listOf(
            RoadmapItem("Hadith", Icons.Default.AutoStories) { onFeatureClick("Hadith") },
            RoadmapItem("Fiqh cơ bản", Icons.Default.Gavel) { onFeatureClick("Fiqh cơ bản") },
            RoadmapItem("Bài học Islam", Icons.Default.School) { onFeatureClick("Các bài học Islam") },
            RoadmapItem("H.dẫn Salah", Icons.Default.AccessibilityNew) { onFeatureClick("Hướng dẫn Salah") },
            RoadmapItem("Wudu/Ghusl", Icons.Default.WaterDrop) { onFeatureClick("Wudu/Ghusl") },
            RoadmapItem("Zakat", Icons.Default.Savings) { onZakatClick() },
            RoadmapItem("Người mới", Icons.Default.PersonAdd) { onFeatureClick("Nội dung cho người mới tìm hiểu Islam") },
        )
    )

    fun getUtilitiesCategory(
        onZakatClick: () -> Unit,
        onFeatureClick: (String) -> Unit
    ) = RoadmapCategory(
        title = "3. Tiện ích",
        items = listOf(
            RoadmapItem("Zakat calc", Icons.Default.Calculate) { onZakatClick() },
            RoadmapItem("Fasting track", Icons.Default.Fastfood) { onFeatureClick("Fasting tracker") },
            RoadmapItem("Prayer track", Icons.Default.CheckBox) { onFeatureClick("Prayer tracker") },
            RoadmapItem("Dhikr count", Icons.Default.AddCircle) { onFeatureClick("Dhikr counter") },
            RoadmapItem("Islamic date", Icons.Default.CompareArrows) { onFeatureClick("Islamic date converter") },
            RoadmapItem("Ramadan CD", Icons.Default.Timer) { onFeatureClick("Ramadan countdown") },
            RoadmapItem("Prayer Notif", Icons.Default.NotificationsActive) { onFeatureClick("Prayer notification") },
            RoadmapItem("Widget", Icons.Default.Widgets) { onFeatureClick("Widget Android") },
        )
    )

    fun getLocalCategory(
        onFeatureClick: (String) -> Unit
    ) = RoadmapCategory(
        title = "4. Local Việt Nam",
        items = listOf(
            RoadmapItem("Masjid", Icons.Default.Place) { onFeatureClick("Danh sách Masjid") },
            RoadmapItem("Halal", Icons.Default.Restaurant) { onFeatureClick("Địa điểm Halal") },
            RoadmapItem("Cộng đồng VN", Icons.Default.People) { onFeatureClick("Thông tin cộng đồng Muslim Việt Nam") },
            RoadmapItem("Tài liệu Việt", Icons.AutoMirrored.Filled.LibraryBooks) { onFeatureClick("Các nguồn tài liệu tiếng Việt được tuyển chọn") },
        )
    )
}
