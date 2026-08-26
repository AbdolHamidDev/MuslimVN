package com.example.muslimvn.domain.models

enum class ReminderMode {
    SILENT,
    NOTIFICATION, // Default sound
    ADHAN       // Full Adhan audio
}

data class PrayerReminder(
    val prayerType: String,
    val mode: ReminderMode = ReminderMode.NOTIFICATION,
    val adhanFileName: String? = "Mishary-Alafasi.mp3" // Default adhan
)

data class AllReminders(
    val reminders: Map<String, PrayerReminder>
)
