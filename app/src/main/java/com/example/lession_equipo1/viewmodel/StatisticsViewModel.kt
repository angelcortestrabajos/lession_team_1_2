package com.example.lession_equipo1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import com.example.lession_equipo1.model.HistoryEntry
import com.example.lession_equipo1.repository.FirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val avgHeartRate: Int? = null,
    val minHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val avgFatigue: Int? = null,
    val minFatigue: Int? = null,
    val maxFatigue: Int? = null,
    val avgRisk: Int? = null,
    val minRisk: Int? = null,
    val maxRisk: Int? = null,
    val mostFrequentState: String? = null,
    val trendMessage: String = "",
    val errorMessage: String? = null
)

internal fun aggregateStatistics(
    entries: List<HistoryEntry>,
    period: StatisticsPeriod
): StatisticsState {
    val validEntries = entries.filter { it.heartRate in 1..300 }
    if (validEntries.isEmpty()) {
        return StatisticsState(
            period = period,
            trendMessage = "Aún no hay mediciones válidas registradas para esta fecha."
        )
    }

    val avgHr = validEntries.map { it.heartRate }.average().toInt()
    val validFatigue = validEntries.filter {
        it.fatigueAvailable && it.fatigue.isFinite() && it.fatigue in 0.0..100.0
    }
    val validRisk = validEntries.filter {
        it.riskAvailable && it.riskLevel.isFinite() && it.riskLevel in 0.0..100.0
    }
    val avgFatigue = validFatigue.takeIf { it.isNotEmpty() }
        ?.map { it.fatigue }?.average()?.toInt()
    val avgRisk = validRisk.takeIf { it.isNotEmpty() }
        ?.map { it.riskLevel }?.average()?.toInt()
    val fatigueValues = validFatigue.map { it.fatigue.toInt() }
    val riskValues = validRisk.map { it.riskLevel.toInt() }
    val heartRateValues = validEntries.map { it.heartRate }

    val stateCounts = validRisk
        .map { it.riskState }
        .filter { it != "Sin datos" }
        .groupingBy { it }
        .eachCount()
    val mostFrequent = stateCounts.maxByOrNull { it.value }?.key
    val fatigueSummary = avgFatigue?.let { " y una fatiga muscular del $it%" }
        ?: "; no hay fatiga disponible para este periodo"

    return StatisticsState(
        period = period,
        entries = validEntries,
        avgHeartRate = avgHr,
        minHeartRate = heartRateValues.minOrNull(),
        maxHeartRate = heartRateValues.maxOrNull(),
        avgFatigue = avgFatigue,
        minFatigue = fatigueValues.minOrNull(),
        maxFatigue = fatigueValues.maxOrNull(),
        avgRisk = avgRisk,
        minRisk = riskValues.minOrNull(),
        maxRisk = riskValues.maxOrNull(),
        mostFrequentState = mostFrequent,
        trendMessage = "Tu frecuencia cardíaca promedio es de $avgHr BPM$fatigueSummary."
    )
}

class StatisticsViewModel(private val repository: FirestoreRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(StatisticsState())
    val uiState: StateFlow<StatisticsState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    fun setPeriod(period: StatisticsPeriod, userId: String) {
        _uiState.value = _uiState.value.copy(
            period = period,
            isLoading = true,
            entries = emptyList(),
            avgHeartRate = null,
            minHeartRate = null,
            maxHeartRate = null,
            avgFatigue = null,
            minFatigue = null,
            maxFatigue = null,
            avgRisk = null,
            minRisk = null,
            maxRisk = null,
            mostFrequentState = null,
            errorMessage = null
        )
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
        loadJob?.cancel()
        if (userId.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                entries = emptyList(),
                avgHeartRate = null,
                minHeartRate = null,
                maxHeartRate = null,
                avgFatigue = null,
                minFatigue = null,
                maxFatigue = null,
                avgRisk = null,
                minRisk = null,
                maxRisk = null,
                mostFrequentState = null,
                trendMessage = "Inicia sesión para consultar tu historial.",
                errorMessage = null
            )
            return
        }
        val range = getRangeFromPeriod(_uiState.value.period)
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        loadJob = viewModelScope.launch {
            try {
                repository.getHistoryByRange(userId, range.first, range.second).collect { entries ->
                    processEntries(entries)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "No se pudieron cargar las estadísticas: ${exception.localizedMessage ?: "error desconocido"}"
                )
            }
        }
    }

    fun stopLoading() {
        loadJob?.cancel()
        loadJob = null
    }

    private suspend fun processEntries(entries: List<HistoryEntry>) {
        val period = _uiState.value.period
        val nextState = withContext(Dispatchers.Default) {
            aggregateStatistics(entries, period)
        }
        _uiState.value = nextState
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
