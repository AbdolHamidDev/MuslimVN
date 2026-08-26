package com.example.muslimvn.domain.models

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val bio: String = "",
    val madhab: String = "Shafi'i", // Default madhab for Vietnam
    val prayerCalculationMethod: Int = -1, // -1 means use default
    val location: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
