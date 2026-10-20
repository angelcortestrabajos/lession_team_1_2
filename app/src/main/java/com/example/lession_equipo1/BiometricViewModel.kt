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
import com.example.lession_equipo1.model.EmgSession
import com.example.lession_equipo1.model.MuscleData
import com.example.lession_equipo1.network.BluetoothController
import com.example.lession_equipo1.network.BluetoothState
import com.example.lession_equipo1.repository.FirestoreRepository
import com.example.lession_equipo1.repository.RoomHistoryRepository
import com.example.lession_equipo1.repository.UserPreferencesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.IOException

class BiometricViewModel(
    private val firestoreRepository: FirestoreRepository,
    private val bluetoothController: BluetoothController,
    private val roomHistoryRepository: RoomHistoryRepository? = null,
    private val userPreferencesRepository: UserPreferencesRepository? = null,
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
    var emgSession by mutableStateOf(EmgSession())
        private set
    var emgReferenceSaveError by mutableStateOf<String?>(null)
        private set

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
    val parsedHeartRate = bluetoothController.parsedHeartRate
    val parsedBatteryLevel = bluetoothController.parsedBatteryLevel
    val parsedEmgMv = bluetoothController.parsedEmgMv
    val parsedEmgAdc = bluetoothController.parsedEmgAdc
    val supportsEmgCommands = bluetoothController.supportsEmgCommands
    val commandError = bluetoothController.commandError

    init {
        // Escuchar ritmo cardíaco de sensor BLE en tiempo real
        viewModelScope.launch {
            bluetoothController.parsedHeartRate.collect { realHr ->
                if (realHr != null && realHr > 0) {
                    heartRate = realHr.toDouble()
                    checkThresholds()
                    currentUserId?.let { saveToFirestore(it) }
                }
            }
        }

        userPreferencesRepository?.let { preferences ->
            viewModelScope.launch {
                preferences.emgReferenceMaxMvFlow.collect { reference ->
                    emgSession = emgSession.copy(referenceMaxMv = reference)
                }
            }
        }

        // EMG values and session statistics are kept in mV; ADC counts remain raw metadata.
        viewModelScope.launch {
            bluetoothController.emgSamples.collect { sample ->
                if (sample.sequence > lastProcessedEmgSequence) {
                    lastProcessedEmgSequence = sample.sequence
                    if (bluetoothState.value is BluetoothState.Connected) {
                        val updatedSession = emgSession.accept(sample)
                        if (updatedSession !== emgSession) {
                            emgSession = updatedSession
                            latestEmgSampleEpoch = connectionEpoch
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            var wasConnected = false
            bluetoothState.collect { state ->
                val isConnected = state is BluetoothState.Connected
                if (isConnected && !wasConnected) {
                    connectionEpoch++
                    emgSession = emgSession.copy(lastTimestampMs = null)
                }
                wasConnected = isConnected
            }
        }
    }

    fun sendEmgCommand(command: String) {
        bluetoothController.sendCommand(command)
    }

    fun hasBluetoothPermissions(): Boolean {
        return bluetoothController.hasPermissions()
    }

    private var currentUserId: String? = null
    private var simulationJob: Job? = null
    private var simulationSaveJob: Job? = null
    private var simulationGeneration = 0L
    private var lastProcessedEmgSequence = 0L
    private var connectionEpoch = 0L
    private var latestEmgSampleEpoch: Long? = null

    fun setCurrentUser(uid: String) {
        currentUserId = uid
    }

    fun startSimulation(uid: String = "") {
        if (uid.isNotEmpty()) {
            currentUserId = uid
        }
        if (!canSimulate() || simulationJob?.isActive == true) return
        simulationGeneration++
        val generation = simulationGeneration
        isSimulating = true
        simulationJob = viewModelScope.launch {
            while (isActive && generation == simulationGeneration) {
                delay(30000)
                if (isActive && generation == simulationGeneration && isSimulating && canSimulate()) {
                    updateMuscles()
                }
            }
        }
    }

    fun stopSimulation() {
        isSimulating = false
        simulationGeneration++
        simulationJob?.cancel()
        simulationJob = null
        simulationSaveJob?.cancel()
        simulationSaveJob = null
    }

    fun triggerManualSimulation() {
        if (isSimulating && canSimulate()) updateMuscles()
    }

    fun updateMuscleManual(id: String, fatigue: Float) {
        if (!isSimulating || !canSimulate()) return
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
        stopSimulation()
        bluetoothController.connectToDevice(device)
    }

    private fun updateMuscles() {
        if (!isSimulating || !canSimulate()) return
        for (i in _muscles.indices) {
            val current = _muscles[i]
            val nextFatigue = (current.fatigue + 10f).coerceAtMost(100f)
            _muscles[i] = current.copy(fatigue = nextFatigue)
        }

        heartRate = (70..180).random().toDouble()
        stability = (60..98).random().toDouble()
        temperature = 36.0 + (Math.random() * 1.5)
        bloodPressureHigh = (110..150).random()
        bloodPressureLow = (70..95).random()

        currentUserId?.let { uid ->
            simulationSaveJob?.cancel()
            simulationSaveJob = saveToFirestore(uid, simulationGeneration)
        }
        checkThresholds()
    }

    fun resetEmgSession() {
        lastProcessedEmgSequence = bluetoothController.emgSampleSequence.value
        latestEmgSampleEpoch = null
        emgSession = emgSession.resetSession()
    }

    fun setEmgMaximumReference(): Boolean {
        if (bluetoothState.value !is BluetoothState.Connected ||
            !supportsEmgCommands.value ||
            latestEmgSampleEpoch != connectionEpoch
        ) return false
        val currentValue = emgSession.latestSample?.valueMv ?: return false
        if (currentValue <= 0f || !currentValue.isFinite()) return false
        emgReferenceSaveError = null
        emgSession = emgSession.copy(referenceMaxMv = currentValue)
        userPreferencesRepository?.let { preferences ->
            viewModelScope.launch {
                try {
                    preferences.setEmgReferenceMaxMv(currentValue)
                    emgReferenceSaveError = null
                } catch (exception: IOException) {
                    emgReferenceSaveError = "No se pudo guardar la referencia máxima: ${exception.localizedMessage}"
                }
            }
        }
        return true
    }

    private fun canSimulate(): Boolean =
        bluetoothState.value is BluetoothState.Idle || bluetoothState.value is BluetoothState.Error

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

    private fun saveToFirestore(uid: String, simulationToken: Long? = null): Job =
        viewModelScope.launch {
            if (simulationToken != null &&
                (!isSimulating || simulationToken != simulationGeneration)
            ) return@launch
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
                deviceId = if (simulationToken == null) "BLE" else "SIMULATION",
                timestamp = com.google.firebase.Timestamp.now()
            )
            if (simulationToken != null &&
                (!isSimulating || simulationToken != simulationGeneration)
            ) return@launch
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
                    timestamp = entry.timestamp.toDate().time,
                    notes = if (simulationToken == null) "" else "Fuente: simulación manual"
                )
            )
            if (simulationToken != null &&
                (!isSimulating || simulationToken != simulationGeneration)
            ) return@launch
            firestoreRepository.saveMeasurement(uid, entry)
        }

    override fun onCleared() {
        stopSimulation()
        bluetoothController.close()
        super.onCleared()
    }
}
