package com.example.muslimvn.domain.models

data class AdhanAudio(
    val name: String,
    val fileName: String,
    val isFajrSpecific: Boolean = false
)

val availableAdhans = listOf(
    AdhanAudio("Mishary Rashid Alafasy", "Mishary-Alafasi.mp3"),
    AdhanAudio("Nasser Al-Qatami", "Nasser-Alqatami.mp3"),
    AdhanAudio("Mansoor Az-Zahrani", "Mansoor-Az-Zahrani.mp3"),
    AdhanAudio("Ahmed El-Kourdi", "Ahmed-El-Kourdi.mp3"),
    AdhanAudio("Hamad Daghriry", "hamad_daghriry.mp3"),
    AdhanAudio("Rabeh Ibn Darah", "Rabeh-Ibn-Darah-Al-Jazairi.mp3"),
    AdhanAudio("Fajr Adhan (Đặc biệt cho Fajr)", "Fajaz_Azan.mp3", isFajrSpecific = true)
)
