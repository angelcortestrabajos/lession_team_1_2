package com.example.lession_equipo1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lession_equipo1.model.HistoryEntry
import com.example.lession_equipo1.repository.FirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

sealed class StatisticsPeriod {
    data class Day(val time: Long) : StatisticsPeriod()
    object Last7Days : StatisticsPeriod()
    object CurrentMonth : StatisticsPeriod()
    data class Custom(val start: Long, val end: Long) : StatisticsPeriod()
    
    companion object {
        val Today = Day(System.currentTimeMillis())
        val Yesterday = Day(System.currentTimeMillis() - 24 * 60 * 60 * 1000L)
    }
}

data class StatisticsState(
    val isLoading: Boolean = false,
    val entries: List<HistoryEntry> = emptyList(),
    val period: StatisticsPeriod = StatisticsPeriod.Today,
    val avgHeartRate: Int = 0,
    val avgFatigue: Int = 0,
    val avgRisk: Int = 0,
    val mostFrequentState: String = "Óptimo",
    val trendMessage: String = ""
)

class StatisticsViewModel(private val repository: FirestoreRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(StatisticsState())
    val uiState: StateFlow<StatisticsState> = _uiState.asStateFlow()

    fun setPeriod(period: StatisticsPeriod, userId: String) {
        _uiState.value = _uiState.value.copy(period = period)
        loadData(userId)
    }

    fun navigateDay(delta: Int, userId: String) {
        val current = _uiState.value.period
        if (current is StatisticsPeriod.Day) {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = current.time
            calendar.add(Calendar.DAY_OF_YEAR, delta)
            setPeriod(StatisticsPeriod.Day(calendar.timeInMillis), userId)
        }
    }

    fun loadData(userId: String) {
        if (userId.isEmpty()) return
        val range = getRangeFromPeriod(_uiState.value.period)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.getHistoryByRange(userId, range.first, range.second).collect { entries ->
                processEntries(entries)
            }
        }
    }

    private fun processEntries(entries: List<HistoryEntry>) {
        if (entries.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                entries = emptyList(),
                avgHeartRate = 0,
                avgFatigue = 0,
                avgRisk = 0,
                trendMessage = "Aún no hay mediciones registradas para esta fecha."
            )
            return
        }

        val avgHr = entries.map { it.heartRate }.average().toInt()
        val avgFatigue = entries.map { it.fatigue }.average().toInt()
        val avgRisk = entries.map { it.riskLevel }.average().toInt()
        
        val stateCounts = entries.groupingBy { it.riskState }.eachCount()
        val mostFrequent = stateCounts.maxByOrNull { it.value }?.key ?: "Óptimo"

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            entries = entries,
            avgHeartRate = avgHr,
            avgFatigue = avgFatigue,
            avgRisk = avgRisk,
            mostFrequentState = mostFrequent,
            trendMessage = generateTrendMessage(avgHr, avgFatigue)
        )
    }

    private fun generateTrendMessage(hr: Int, fatigue: Int): String {
        return "Tu frecuencia cardíaca promedio es de $hr BPM con una fatiga muscular del $fatigue%."
    }

    private fun getRangeFromPeriod(period: StatisticsPeriod): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        
        when (period) {
            is StatisticsPeriod.Day -> {
                calendar.timeInMillis = period.time
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val start = calendar.timeInMillis
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                return Pair(start, calendar.timeInMillis)
            }
            is StatisticsPeriod.Last7Days -> {
                val end = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                return Pair(calendar.timeInMillis, end)
            }
            is StatisticsPeriod.CurrentMonth -> {
                val end = calendar.timeInMillis
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                return Pair(calendar.timeInMillis, end)
            }
            is StatisticsPeriod.Custom -> return Pair(period.start, period.end)
        }
    }
}
