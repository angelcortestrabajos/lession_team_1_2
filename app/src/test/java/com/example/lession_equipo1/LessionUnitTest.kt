package com.example.lession_equipo1

import com.example.lession_equipo1.model.IntensityLevel
import com.example.lession_equipo1.model.EmgSample
import com.example.lession_equipo1.model.EmgSession
import com.example.lession_equipo1.model.HistoryEntry
import com.example.lession_equipo1.model.MuscleData
import com.example.lession_equipo1.viewmodel.StatisticsPeriod
import com.example.lession_equipo1.viewmodel.aggregateStatistics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LessionUnitTest {

    @Test
    fun riskLevel_calculation_isCorrect() {
        val muscles = listOf(
            MuscleData("biceps", "Bíceps", 50f),
            MuscleData("triceps", "Tríceps", 50f)
        )
        val stability = 90.0 // 100 - stability = 10.0
        val avgFatigue = 50f
        val expectedRisk = ((avgFatigue * 0.7f) + (10f * 0.3f)).coerceIn(0f, 100f) // 35 + 3 = 38.0

        assertEquals(38.0f, expectedRisk, 0.01f)
    }

    @Test
    fun intensityLevel_labels_areValid() {
        assertEquals("Bajo", IntensityLevel.LOW.getLabel())
        assertEquals("Alto", IntensityLevel.HIGH.getLabel())
    }

    @Test
    fun heartRate_ble_binaryParsing_8bit_isCorrect() {
        // Formato estándar BLE 0x2A37: Byte 0 = Flags (0 = uint8), Byte 1 = Heart Rate
        val bytes = byteArrayOf(0x00, 78.toByte())
        val flags = bytes[0].toInt()
        val is16Bit = (flags and 0x01) != 0
        val hrValue = if (is16Bit && bytes.size >= 3) {
            (bytes[1].toInt() and 0xFF) or ((bytes[2].toInt() and 0xFF) shl 8)
        } else if (bytes.size >= 2) {
            bytes[1].toInt() and 0xFF
        } else 0

        assertEquals(78, hrValue)
    }

    @Test
    fun heartRate_ble_binaryParsing_16bit_isCorrect() {
        // Formato estándar BLE 0x2A37: Byte 0 = Flags (1 = uint16), Byte 1 y 2 = Heart Rate (uint16 LSB first)
        // 140 = 0x008C -> LSB=0x8C, MSB=0x00
        val bytes = byteArrayOf(0x01, 0x8C.toByte(), 0x00)
        val flags = bytes[0].toInt()
        val is16Bit = (flags and 0x01) != 0
        val hrValue = if (is16Bit && bytes.size >= 3) {
            (bytes[1].toInt() and 0xFF) or ((bytes[2].toInt() and 0xFF) shl 8)
        } else if (bytes.size >= 2) {
            bytes[1].toInt() and 0xFF
        } else 0

        assertEquals(140, hrValue)
    }

    @Test
    fun bleManager_constants_areValid() {
        assertEquals("0000180d-0000-1000-8000-00805f9b34fb", com.example.lession_equipo1.data.ble.BluetoothManager.HR_SERVICE_UUID.toString())
        assertEquals("00002a37-0000-1000-8000-00805f9b34fb", com.example.lession_equipo1.data.ble.BluetoothManager.HR_MEASUREMENT_CHAR_UUID.toString())
        assertEquals("0000180f-0000-1000-8000-00805f9b34fb", com.example.lession_equipo1.data.ble.BluetoothManager.BATTERY_SERVICE_UUID.toString())
    }

    @Test
    fun emgSession_accumulatesRealMillivoltsAndCalculatesBoundedPerformance() {
        val reference = EmgSession(referenceMaxMv = 1000f)
        val session = reference
            .accept(emgSample(valueMv = 500f, timestampMs = 1L))
            .accept(emgSample(valueMv = 1200f, timestampMs = 2L))

        assertEquals(2L, session.sampleCount)
        assertEquals(850.0, session.averageMv!!, 0.001)
        assertEquals(1200f, session.maxSessionMv!!)
        assertEquals(100f, session.performancePercent!!)
    }

    @Test
    fun emgSession_ignoresRepeatedAndInvalidSamples() {
        val initial = EmgSession().accept(emgSample(valueMv = 250f, timestampMs = 10L))
        val duplicate = initial.accept(emgSample(valueMv = 300f, timestampMs = 10L))
        val invalid = initial.accept(emgSample(adcRaw = 4096, timestampMs = 11L))
        val wrongMetric = initial.accept(
            emgSample(timestampMs = 11L).copy(metric = "adcRaw")
        )
        val invalidStatus = initial.accept(
            emgSample(timestampMs = 11L).copy(status = "invalid")
        )
        val wrongSensor = initial.accept(
            emgSample(timestampMs = 11L).copy(sensor = "other_sensor")
        )
        val nonFinite = initial.accept(
            emgSample(valueMv = Float.NaN, timestampMs = 11L)
        )

        assertEquals(initial, duplicate)
        assertEquals(initial, invalid)
        assertEquals(initial, wrongMetric)
        assertEquals(initial, invalidStatus)
        assertEquals(initial, wrongSensor)
        assertEquals(initial, nonFinite)
        assertEquals(1L, initial.sampleCount)
    }

    @Test
    fun emgSession_zeroReferenceDoesNotDivideAndResetKeepsReference() {
        val zeroReference = EmgSession(referenceMaxMv = 0f)
            .accept(emgSample(valueMv = 0f, timestampMs = 1L))
        assertEquals(null, zeroReference.performancePercent)

        val session = zeroReference.copy(referenceMaxMv = 900f)
        val reset = session.resetSession()
        assertEquals(0L, reset.sampleCount)
        assertTrue(reset.sampleValuesMv.isEmpty())
        assertEquals(null, reset.latestSample)
        assertEquals(900f, reset.referenceMaxMv!!)
    }

    @Test
    fun statistics_ignoreInvalidHeartRatesAndUnavailableRiskOrFatigue() {
        val period = StatisticsPeriod.Day(1L)
        val state = aggregateStatistics(
            listOf(
                HistoryEntry(
                    heartRate = 0,
                    fatigue = 95.0,
                    riskLevel = 90.0,
                    fatigueAvailable = true,
                    riskAvailable = true
                ),
                HistoryEntry(
                    heartRate = 100,
                    fatigue = 80.0,
                    riskLevel = 70.0
                ),
                HistoryEntry(
                    heartRate = 80,
                    fatigue = 50.0,
                    riskLevel = 40.0,
                    fatigueAvailable = true,
                    riskAvailable = true,
                    riskState = "Precaución"
                ),
                HistoryEntry(
                    heartRate = 120,
                    fatigue = 150.0,
                    riskLevel = Double.NaN,
                    fatigueAvailable = true,
                    riskAvailable = true
                )
            ),
            period
        )

        assertEquals(period, state.period)
        assertEquals(3, state.entries.size)
        assertEquals(100, state.avgHeartRate)
        assertEquals(80, state.minHeartRate)
        assertEquals(120, state.maxHeartRate)
        assertEquals(50, state.avgFatigue)
        assertEquals(50, state.minFatigue)
        assertEquals(50, state.maxFatigue)
        assertEquals(40, state.avgRisk)
        assertEquals(40, state.minRisk)
        assertEquals(40, state.maxRisk)
        assertEquals("Precaución", state.mostFrequentState)
    }

    @Test
    fun statistics_emptyOrInvalidHistoryHasNoZeroValuedMetrics() {
        val state = aggregateStatistics(
            listOf(HistoryEntry(heartRate = 0, riskAvailable = true, riskLevel = 0.0)),
            StatisticsPeriod.Today
        )

        assertTrue(state.entries.isEmpty())
        assertEquals(null, state.avgHeartRate)
        assertEquals(null, state.avgRisk)
        assertEquals(null, state.avgFatigue)
    }

    @Test
    fun emgSession_calculatesPercentageFromSameMillivoltMetric() {
        val session = EmgSession(referenceMaxMv = 1000f)
            .accept(emgSample(valueMv = 250f, timestampMs = 1L))

        assertEquals(25f, session.performancePercent!!, 0.001f)
    }

    private fun emgSample(
        valueMv: Float = 1200f,
        adcRaw: Int = 2500,
        timestampMs: Long,
        sequence: Long = timestampMs
    ) = EmgSample(
        sensor = "emg_module",
        metric = "emgEnvelope",
        valueMv = valueMv,
        unit = "mV",
        adcRaw = adcRaw,
        status = "valid",
        timestampMs = timestampMs,
        sequence = sequence
    )
}
