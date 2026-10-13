package com.example.lession_equipo1

import android.bluetooth.BluetoothDevice
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lession_equipo1.data.local.entity.HistoryRecordEntity
import com.example.lession_equipo1.model.HistoryEntry
import com.example.lession_equipo1.model.MuscleData
import com.example.lession_equipo1.network.BluetoothController
import com.example.lession_equipo1.repository.FirestoreRepository
import com.example.lession_equipo1.repository.RoomHistoryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class BiometricViewModel(
    private val firestoreRepository: FirestoreRepository,
    private val bluetoothController: BluetoothController,
    private val roomHistoryRepository: RoomHistoryRepository? = null,
) : ViewModel() {
    private val _muscles = mutableStateListOf(
        MuscleData("chest", "Pecho", 5f),
        MuscleData("biceps_l", "Bíceps Izquierdo", 10f),
        MuscleData("biceps_r", "Bíceps Derecho", 8f),
        MuscleData("thigh_l", "Muslo Izquierdo", 2f),
        MuscleData("thigh_r", "Muslo Derecho", 4f),
    )
    val muscles: List<MuscleData> = _muscles

    var heartRate by mutableDoubleStateOf(75.0)
    var stability by mutableDoubleStateOf(90.0)
    var temperature by mutableDoubleStateOf(36.5)
    var bloodPressureHigh by mutableIntStateOf(120)
    var bloodPressureLow by mutableIntStateOf(80)
    var isSimulating by mutableStateOf(value = false)
    var activeActivity by mutableStateOf("")

    val riskLevel: Float
        get() {
            val avgFatigue = _muscles.asSequence().map { it.fatigue }.average().toFloat()
            return ((avgFatigue * 0.7f) + ((100 - stability.toFloat()) * 0.3f)).coerceIn(0f, 100f)
        }
    
    private val _notifications = MutableSharedFlow<String>()
    val notifications = _notifications.asSharedFlow()

    val bluetoothState = bluetoothController.state
    val scannedDevices = bluetoothController.scannedDevices
    val connectedDevice = bluetoothController.connectedDevice
    val receivedData = bluetoothController.receivedData

    fun hasBluetoothPermissions(): Boolean {
        return bluetoothController.hasPermissions()
    }

    private var currentUserId: String? = null
    private var simulationJob: Job? = null

    fun startSimulation(uid: String) {
        currentUserId = uid
        if (simulationJob != null) return
        
        simulationJob = viewModelScope.launch {
            isSimulating = true
            while (true) {
                delay(30000)
                updateMuscles()
            }
        }
    }

    fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
        isSimulating = false
    }

    fun triggerManualSimulation() {
        updateMuscles()
    }

    fun updateMuscleManual(id: String, fatigue: Float) {
        val index = _muscles.indexOfFirst { it.id == id }
        if (index != -1) {
            _muscles[index] = _muscles[index].copy(fatigue = fatigue.coerceIn(0f, 100f))
            checkThresholds()
        }
    }

    fun startBluetoothScan() {
        bluetoothController.startScan()
    }

    fun stopBluetoothScan() {
        bluetoothController.stopScan()
    }

    fun disconnectBluetooth() {
        bluetoothController.disconnect()
    }

    fun connectToDevice(device: BluetoothDevice) {
        bluetoothController.connectToDevice(device)
    }

    private fun updateMuscles() {
        for (i in _muscles.indices) {
            val current = _muscles[i]
            val nextFatigue = (current.fatigue + 10f).coerceAtMost(100f)
            _muscles[i] = current.copy(fatigue = nextFatigue)
        }

        // Variar otros datos aleatoriamente
        heartRate = (70..180).random().toDouble()
        stability = (60..98).random().toDouble()
        temperature = 36.0 + (Math.random() * 1.5)
        bloodPressureHigh = (110..150).random()
        bloodPressureLow = (70..95).random()

        currentUserId?.let { saveToFirestore(it) }
        checkThresholds()
    }

    private fun checkThresholds() {
        viewModelScope.launch {
            // 1. Detección de Fatiga Muscular
            _muscles.forEach { muscle ->
                if (muscle.fatigue >= 90f) {
                    _notifications.emit("¡CRÍTICO! Límite superado en ${muscle.name} (${muscle.fatigue.toInt()}%)")
                } else if (muscle.fatigue >= 75f) {
                    _notifications.emit("ALERTA PREVENTIVA: Proximidad al límite en ${muscle.name} (${muscle.fatigue.toInt()}%)")
                }
            }

            // 2. Detección de Ritmo Cardíaco
            if (heartRate >= 160.0) {
                _notifications.emit("¡CRÍTICO! Frecuencia cardíaca muy alta (${heartRate.toInt()} BPM)")
            } else if (heartRate >= 140.0) {
                _notifications.emit("ALERTA PREVENTIVA: Ritmo cardíaco elevado (${heartRate.toInt()} BPM)")
            }

            // 3. Detección de Riesgo de Lesión
            if (riskLevel >= 80f) {
                _notifications.emit("¡RIESGO ALTO! Nivel de riesgo en ${riskLevel.toInt()}%. ¡Reduce la intensidad!")
            } else if (riskLevel >= 65f) {
                _notifications.emit("ALERTA PREVENTIVA: Riesgo moderado (${riskLevel.toInt()}%). Mantén precaución.")
            }
        }
    }

    private fun saveToFirestore(uid: String) {
        viewModelScope.launch {
            val risk = riskLevel
            val state = when {
                risk < 40 -> "Óptimo"
                risk < 75 -> "Precaución"
                else -> "Riesgo"
            }
            
            val entry = HistoryEntry(
                userId = uid,
                heartRate = heartRate.toInt(),
                temperature = temperature,
                bpSystolic = bloodPressureHigh,
                bpDiastolic = bloodPressureLow,
                fatigue = _muscles.asSequence().map { it.fatigue.toDouble() }.average(),
                maxMuscle = _muscles.maxOf { it.fatigue }.toDouble(),
                stability = stability,
                riskLevel = risk.toDouble(),
                riskState = state,
                activityType = activeActivity,
                timestamp = com.google.firebase.Timestamp.now()
            )
            roomHistoryRepository?.insertRecord(
                HistoryRecordEntity(
                    userId = uid,
                    heartRate = entry.heartRate,
                    temperature = entry.temperature,
                    bpSystolic = entry.bpSystolic,
                    bpDiastolic = entry.bpDiastolic,
                    fatigue = entry.fatigue,
                    maxMuscle = entry.maxMuscle,
                    stability = entry.stability,
                    riskLevel = entry.riskLevel,
                    riskState = entry.riskState,
                    activityType = entry.activityType,
                    timestamp = entry.timestamp.toDate().time
                )
            )
            firestoreRepository.saveMeasurement(uid, entry)
        }
    }
}
