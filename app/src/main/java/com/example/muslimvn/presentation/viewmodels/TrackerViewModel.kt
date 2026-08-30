package com.example.muslimvn.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muslimvn.data.local.entities.TrackerEntity
import com.example.muslimvn.domain.repository.TrackerRepository
import com.example.muslimvn.domain.usecases.GetPrayerTimesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class TrackerViewModel @Inject constructor(
    private val getPrayerTimesUseCase: GetPrayerTimesUseCase,
    private val trackerRepository: TrackerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrackerUiState())
    val uiState = _uiState.asStateFlow()

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var loadJob: Job? = null
    private val selectedDateFlow = MutableStateFlow(Calendar.getInstance().time)

    init {
        initializeWeeklyDays()
        observeSelectedDate()
        loadWeeklyProgress()
    }

    private fun observeSelectedDate() {
        viewModelScope.launch {
            selectedDateFlow.collect { date ->
                loadDataForDate(date)
            }
        }
    }

    private fun loadWeeklyProgress() {
        viewModelScope.launch {
            val daysWithProgress = _uiState.value.weeklyDays.map { day ->
                val dateKey = dateFormatter.format(day.date)
                val entity = trackerRepository.getTrackerByDate(dateKey)
                val completedCount = if (entity != null) {
                    listOf(
                        entity.fajrCompleted,
                        entity.dhuhrCompleted,
                        entity.asrCompleted,
                        entity.maghribCompleted,
                        entity.ishaCompleted
                    ).count { it }
                } else 0
                day.copy(completionProgress = completedCount.toFloat() / 5f)
            }
            _uiState.update { it.copy(weeklyDays = daysWithProgress) }
        }
    }

    private fun initializeWeeklyDays() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val today = calendar.time
        val days = mutableListOf<DaySelection>()
        
        // Tạo 7 ngày gần nhất (từ 6 ngày trước đến hôm nay)
        val tempCal = calendar.clone() as Calendar
        tempCal.add(Calendar.DAY_OF_YEAR, -6)
        for (i in 0..6) {
            val date = tempCal.time
            days.add(DaySelection(date = date, isSelected = dateFormatter.format(date) == dateFormatter.format(today)))
            tempCal.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        _uiState.update { it.copy(weeklyDays = days) }
    }

    fun selectDate(date: Date) {
        _uiState.update { state ->
            state.copy(
                weeklyDays = state.weeklyDays.map { 
                    it.copy(isSelected = dateFormatter.format(it.date) == dateFormatter.format(date)) 
                }
            )
        }
        selectedDateFlow.value = date
    }

    private fun loadDataForDate(date: Date) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val dateKey = dateFormatter.format(date)
            val prayerTimes = getPrayerTimesUseCase(date)
            
            trackerRepository.getTrackerByDateFlow(dateKey).collect { savedEntity ->
                val prayers = if (savedEntity != null) {
                    listOf(
                        PrayerTrackerState("Fajr", prayerTimes.fajr, isCompleted = savedEntity.fajrCompleted, isJamaah = savedEntity.fajrJamaah, hasSunnahBefore = true, isSunnahBeforeCompleted = savedEntity.fajrSunnahBefore),
                        PrayerTrackerState("Dhuhr", prayerTimes.dhuhr, isCompleted = savedEntity.dhuhrCompleted, isJamaah = savedEntity.dhuhrJamaah, hasSunnahBefore = true, isSunnahBeforeCompleted = savedEntity.dhuhrSunnahBefore, hasSunnahAfter = true, isSunnahAfterCompleted = savedEntity.dhuhrSunnahAfter),
                        PrayerTrackerState("Asr", prayerTimes.asr, isCompleted = savedEntity.asrCompleted, isJamaah = savedEntity.asrJamaah, hasSunnahBefore = true, isSunnahBeforeCompleted = savedEntity.asrSunnahBefore),
                        PrayerTrackerState("Maghrib", prayerTimes.maghrib, isCompleted = savedEntity.maghribCompleted, isJamaah = savedEntity.maghribJamaah, hasSunnahAfter = true, isSunnahAfterCompleted = savedEntity.maghribSunnahAfter),
                        PrayerTrackerState("Isha", prayerTimes.isha, isCompleted = savedEntity.ishaCompleted, isJamaah = savedEntity.ishaJamaah, hasSunnahAfter = true, isSunnahAfterCompleted = savedEntity.ishaSunnahAfter)
                    )
                } else {
                    listOf(
                        PrayerTrackerState("Fajr", prayerTimes.fajr, hasSunnahBefore = true),
                        PrayerTrackerState("Dhuhr", prayerTimes.dhuhr, hasSunnahBefore = true, hasSunnahAfter = true),
                        PrayerTrackerState("Asr", prayerTimes.asr, hasSunnahBefore = true),
                        PrayerTrackerState("Maghrib", prayerTimes.maghrib, hasSunnahAfter = true),
                        PrayerTrackerState("Isha", prayerTimes.isha, hasSunnahAfter = true)
                    )
                }
                
                val quran = if (savedEntity != null) {
                    QuranTrackerState(
                        savedEntity.lastSurahName,
                        savedEntity.lastSurahNumber,
                        savedEntity.lastAyahNumber,
                        savedEntity.quranProgress
                    )
                } else QuranTrackerState()

                val azkar = if (savedEntity != null) {
                    AzkarTrackerState(savedEntity.azkarCount)
                } else AzkarTrackerState()

                _uiState.update { it.copy(
                    prayers = prayers, 
                    quran = quran,
                    azkar = azkar,
                    isLoading = false
                ) }
            }
        }
    }

    fun togglePrayer(index: Int) {
        updatePrayer(index) { it.copy(isCompleted = !it.isCompleted) }
    }

    fun toggleJamaah(index: Int) {
        updatePrayer(index) { it.copy(isJamaah = !it.isJamaah) }
    }

    fun toggleSunnahBefore(index: Int) {
        updatePrayer(index) { it.copy(isSunnahBeforeCompleted = !it.isSunnahBeforeCompleted) }
    }

    fun toggleSunnahAfter(index: Int) {
        updatePrayer(index) { it.copy(isSunnahAfterCompleted = !it.isSunnahAfterCompleted) }
    }

    private fun updatePrayer(index: Int, transform: (PrayerTrackerState) -> PrayerTrackerState) {
        _uiState.update { state ->
            val updatedPrayers = state.prayers.toMutableList()
            if (index in updatedPrayers.indices) {
                updatedPrayers[index] = transform(updatedPrayers[index])
            }
            saveCurrentState(state.copy(prayers = updatedPrayers))
            state.copy(prayers = updatedPrayers)
        }
    }

    fun incrementAzkar() {
        _uiState.update { 
            val newState = it.copy(azkar = it.azkar.copy(count = it.azkar.count + 1))
            saveCurrentState(newState)
            newState
        }
    }

    fun resetAzkar() {
        _uiState.update { 
            val newState = it.copy(azkar = it.azkar.copy(count = 0))
            saveCurrentState(newState)
            newState
        }
    }

    private fun saveCurrentState(state: TrackerUiState) {
        val selectedDate = state.weeklyDays.find { it.isSelected }?.date ?: return
        val dateKey = dateFormatter.format(selectedDate)
        
        viewModelScope.launch {
            val entity = TrackerEntity(
                date = dateKey,
                fajrCompleted = state.prayers[0].isCompleted,
                fajrJamaah = state.prayers[0].isJamaah,
                fajrSunnahBefore = state.prayers[0].isSunnahBeforeCompleted,
                dhuhrCompleted = state.prayers[1].isCompleted,
                dhuhrJamaah = state.prayers[1].isJamaah,
                dhuhrSunnahBefore = state.prayers[1].isSunnahBeforeCompleted,
                dhuhrSunnahAfter = state.prayers[1].isSunnahAfterCompleted,
                asrCompleted = state.prayers[2].isCompleted,
                asrJamaah = state.prayers[2].isJamaah,
                asrSunnahBefore = state.prayers[2].isSunnahBeforeCompleted,
                maghribCompleted = state.prayers[3].isCompleted,
                maghribJamaah = state.prayers[3].isJamaah,
                maghribSunnahAfter = state.prayers[3].isSunnahAfterCompleted,
                ishaCompleted = state.prayers[4].isCompleted,
                ishaJamaah = state.prayers[4].isJamaah,
                ishaSunnahAfter = state.prayers[4].isSunnahAfterCompleted,
                lastSurahName = state.quran.lastSurahName,
                lastSurahNumber = state.quran.lastSurahNumber,
                lastAyahNumber = state.quran.lastAyahNumber,
                quranProgress = state.quran.progress,
                azkarCount = state.azkar.count
            )
            trackerRepository.saveTracker(entity)
            loadWeeklyProgress()
        }
    }
}
