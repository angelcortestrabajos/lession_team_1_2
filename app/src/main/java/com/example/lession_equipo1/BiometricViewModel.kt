package com.example.lession_equipo1

import android.bluetooth.BluetoothDevice
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lession_equipo1.data.local.entity.HistoryRecordEntity
import com.example.lession_equipo1.model.DataSourceState
import com.example.lession_equipo1.model.HistoryEntry
import com.example.lession_equipo1.model.EmgSession
import com.example.lession_equipo1.model.MuscleData
import com.example.lession_equipo1.model.PhysicalSensorInfo
import com.example.lession_equipo1.model.SensorConnectionState
import com.example.lession_equipo1.network.BluetoothController
import com.example.lession_equipo1.network.BluetoothState
import com.example.lession_equipo1.repository.FirestoreRepository
import com.example.lession_equipo1.repository.RoomHistoryRepository
import com.example.lession_equipo1.repository.UserPreferencesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import java.io.IOException

enum class EmgCalibrationStage {
    IDLE,
    REST_PROMPT,
    CAPTURING_REST,
    MAXIMUM_PROMPT,
    CAPTURING_MAXIMUM,
    COMPLETE
}

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

    var heartRate by mutableDoubleStateOf(0.0)
    var spO2 by mutableIntStateOf(0)
    var stability by mutableDoubleStateOf(0.0)
    var temperature by mutableDoubleStateOf(0.0)
    var bloodPressureHigh by mutableIntStateOf(0)
    var bloodPressureLow by mutableIntStateOf(0)
    var isSimulating by mutableStateOf(value = false)
    var activeActivity by mutableStateOf("")
    var emgSession by mutableStateOf(EmgSession())
        private set
    var emgCalibrationStage by mutableStateOf(EmgCalibrationStage.IDLE)
        private set
    var emgCalibrationError by mutableStateOf<String?>(null)
        private set
    private var pendingEmgRestMv: Float? = null
    private var emgCalibrationJob: Job? = null
    private var emgMaximumAlertActive = false

    val riskLevel: Float
        get() {
            if (!hasRiskMeasurements) {
                return 0f
            }
            val avgFatigue = _muscles.asSequence().map { it.fatigue }.average().toFloat()
            if (heartRate == 0.0 && stability == 0.0 && avgFatigue == 0f) {
                return 0f
            }
            val hrRisk = if (heartRate > 100.0) {
                ((heartRate - 100.0) / (180.0 - 100.0) * 100.0).coerceIn(0.0, 100.0).toFloat()
            } else 0f
            val stabilityRisk = (100f - stability.toFloat()).coerceAtLeast(0f)

            return ((avgFatigue * 0.45f) + (stabilityRisk * 0.35f) + (hrRisk * 0.20f)).coerceIn(0f, 100f)
        }

    val hasRiskMeasurements: Boolean
        get() = isSimulating

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
    val parsedMotionStability = bluetoothController.parsedMotionStability
    val parsedIrTemp = bluetoothController.parsedIrTemp
    val parsedBodyTemp = bluetoothController.parsedBodyTemp
    val parsedSpO2 = bluetoothController.parsedSpO2
    val lastTelemetryPacketMs = bluetoothController.lastTelemetryPacketMs
    val supportsEmgCommands = bluetoothController.supportsEmgCommands
    val commandError = bluetoothController.commandError

    val dataSourceState: StateFlow<DataSourceState> = combine(
        bluetoothController.state,
        bluetoothController.lastTelemetryPacketMs,
        snapshotFlow { isSimulating },
        flow {
            while (true) {
                emit(System.currentTimeMillis())
                delay(1000L)
            }
        }
    ) { btState, lastPacketMs, simulating, now ->
        val isPacketRecent = lastPacketMs > 0L && (now - lastPacketMs < 10000L)

        when {
            btState is BluetoothState.Error -> DataSourceState.COMMUNICATION_ERROR
            btState is BluetoothState.Connected && isPacketRecent -> DataSourceState.REAL_SENSORS_ACTIVE
            btState is BluetoothState.Connected && !isPacketRecent -> DataSourceState.CONNECTED_NO_DATA
            simulating -> DataSourceState.SIMULATION_ACTIVE
            else -> DataSourceState.SENSORS_DISCONNECTED
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = DataSourceState.SENSORS_DISCONNECTED
    )

    val physicalSensors: StateFlow<List<PhysicalSensorInfo>> = combine(
        bluetoothController.state,
        bluetoothController.connectedDevice,
        bluetoothController.sensorTimestamps,
        bluetoothController.parsedHeartRate,
        bluetoothController.parsedBatteryLevel,
        bluetoothController.parsedEmgMv,
        bluetoothController.parsedMotionStability,
        bluetoothController.parsedIrTemp,
        bluetoothController.parsedBodyTemp,
        flow {
            while (true) {
                emit(Unit)
                delay(1000L)
            }
        }
    ) { args ->
        buildPhysicalSensorsList(
            btState = args[0] as BluetoothState,
            device = args[1] as? BluetoothDevice,
            timestamps = @Suppress("UNCHECKED_CAST") (args[2] as Map<String, Long>),
            hr = args[3] as? Int,
            battery = args[4] as? Int,
            emgMv = args[5] as? Float,
            stabilityVal = args[6] as? Double,
            irTemp = args[7] as? Double,
            bodyTemp = args[8] as? Double
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    init {
        // Escuchar ritmo cardíaco de sensor BLE en tiempo real (MAX30102 / Estándar)
        viewModelScope.launch {
            bluetoothController.parsedHeartRate.collect { realHr ->
                if (realHr != null && realHr > 0) {
                    heartRate = realHr.toDouble()
                    checkThresholds()
                    currentUserId?.let { saveToFirestore(it) }
                }
            }
        }

        // Escuchar estabilidad MPU6050 de sensor BLE en tiempo real
        viewModelScope.launch {
            bluetoothController.parsedMotionStability.collect { realStability ->
                if (realStability != null && realStability in 0.0..100.0) {
                    stability = realStability
                    checkThresholds()
                }
            }
        }

        // Escuchar temperatura infrarroja MLX90614
        viewModelScope.launch {
            bluetoothController.parsedIrTemp.collect { realTemp ->
                if (realTemp != null && realTemp in 20.0..45.0) {
                    temperature = realTemp
                }
            }
        }

        // Escuchar temperatura corporal MAX30205
        viewModelScope.launch {
            bluetoothController.parsedBodyTemp.collect { realTemp ->
                if (realTemp != null && realTemp in 20.0..45.0) {
                    temperature = realTemp
                }
            }
        }

        userPreferencesRepository?.let { preferences ->
            viewModelScope.launch {
                combine(
                    preferences.emgReferenceRestMvFlow,
                    preferences.emgReferenceMaxMvFlow
                ) { rest, maximum ->
                    rest to maximum
                }.collect { (rest, maximum) ->
                    val validCalibration = rest != null && maximum != null &&
                        rest.isFinite() && maximum.isFinite() && maximum > rest
                    emgSession = emgSession.copy(
                        referenceRestMv = rest.takeIf { validCalibration },
                        referenceMaxMv = maximum.takeIf { validCalibration }
                    )
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
                            if (updatedSession.maximumEffortReached) {
                                if (!emgMaximumAlertActive) {
                                    emgMaximumAlertActive = true
                                    _notifications.emit(
                                        "Se alcanzó el umbral de esfuerzo máximo calibrado. Reduce la intensidad o descansa."
                                    )
                                }
                            } else {
                                emgMaximumAlertActive = false
                            }
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

        // Generar actualización inicial e iterar cada 2 segundos
        updateMuscles()

        simulationJob = viewModelScope.launch {
            while (isActive && generation == simulationGeneration && isSimulating && canSimulate()) {
                delay(2000)
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

        resetDashboardMetricsToZero()
    }

    fun resetDashboardMetricsToZero() {
        // Restablece los indicadores a cero solo cuando los sensores BLE no están transmitiendo datos reales
        if (bluetoothState.value !is BluetoothState.Connected) {
            heartRate = 0.0
            stability = 0.0
            temperature = 0.0
            bloodPressureHigh = 0
            bloodPressureLow = 0
            for (i in _muscles.indices) {
                _muscles[i] = _muscles[i].copy(fatigue = 0f)
            }
            resetEmgSession()
        }
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
        emgMaximumAlertActive = false
        emgSession = emgSession.resetSession()
    }

    fun beginEmgCalibration() {
        if (bluetoothState.value !is BluetoothState.Connected ||
            !supportsEmgCommands.value
        ) {
            emgCalibrationError = "Conecta primero el módulo EMG para calibrar."
            return
        }
        pendingEmgRestMv = null
        emgCalibrationError = null
        emgCalibrationStage = EmgCalibrationStage.REST_PROMPT
    }

    fun captureEmgRest() {
        captureEmgCalibrationValue(isRest = true)
    }

    fun captureEmgMaximum() {
        captureEmgCalibrationValue(isRest = false)
    }

    fun closeEmgCalibration() {
        emgCalibrationJob?.cancel()
        emgCalibrationJob = null
        pendingEmgRestMv = null
        emgCalibrationError = null
        emgCalibrationStage = EmgCalibrationStage.IDLE
    }

    private fun captureEmgCalibrationValue(isRest: Boolean) {
        val expectedStage = if (isRest) {
            EmgCalibrationStage.REST_PROMPT
        } else {
            EmgCalibrationStage.MAXIMUM_PROMPT
        }
        if (emgCalibrationStage != expectedStage || emgCalibrationJob?.isActive == true) return
        if (bluetoothState.value !is BluetoothState.Connected || !supportsEmgCommands.value) {
            emgCalibrationError = "El sensor EMG se desconectó. Reconéctalo para continuar."
            return
        }

        val firstSequence = bluetoothController.emgSampleSequence.value
        val captureEpoch = connectionEpoch
        emgCalibrationError = null
        emgCalibrationStage = if (isRest) {
            EmgCalibrationStage.CAPTURING_REST
        } else {
            EmgCalibrationStage.CAPTURING_MAXIMUM
        }
        emgCalibrationJob = viewModelScope.launch {
            val values = withTimeoutOrNull(5_000L) {
                bluetoothController.emgSamples
                    .filter { it.sequence > firstSequence }
                    .map { it.valueMv }
                    .take(20)
                    .toList()
            }
            val promptStage = if (isRest) {
                EmgCalibrationStage.REST_PROMPT
            } else {
                EmgCalibrationStage.MAXIMUM_PROMPT
            }
            if (values == null || values.size < 15) {
                emgCalibrationStage = promptStage
                emgCalibrationError = "No llegaron suficientes muestras EMG válidas. Reintenta con el sensor conectado."
                return@launch
            }
            if (bluetoothState.value !is BluetoothState.Connected || captureEpoch != connectionEpoch) {
                emgCalibrationStage = promptStage
                emgCalibrationError = "La conexión BLE cambió durante la captura. Reintenta."
                return@launch
            }

            val capturedMv = values.average().toFloat()
            if (!capturedMv.isFinite()) {
                emgCalibrationStage = promptStage
                emgCalibrationError = "La captura EMG no produjo un valor válido."
                return@launch
            }

            if (isRest) {
                pendingEmgRestMv = capturedMv
                emgCalibrationStage = EmgCalibrationStage.MAXIMUM_PROMPT
            } else {
                val restMv = pendingEmgRestMv
                if (restMv == null || capturedMv <= restMv) {
                    emgCalibrationStage = EmgCalibrationStage.MAXIMUM_PROMPT
                    emgCalibrationError = "El esfuerzo máximo debe producir una señal mayor que la de reposo. Reintenta."
                    return@launch
                }

                try {
                    userPreferencesRepository?.setEmgCalibration(restMv, capturedMv)
                    emgSession = emgSession.copy(
                        referenceRestMv = restMv,
                        referenceMaxMv = capturedMv
                    )
                    emgMaximumAlertActive = true
                    emgCalibrationStage = EmgCalibrationStage.COMPLETE
                } catch (exception: IOException) {
                    emgCalibrationStage = EmgCalibrationStage.MAXIMUM_PROMPT
                    emgCalibrationError =
                        "No se pudo guardar la calibración EMG: ${exception.localizedMessage}"
                }
            }
        }
    }

    private fun buildPhysicalSensorsList(
        btState: BluetoothState,
        device: BluetoothDevice?,
        timestamps: Map<String, Long>,
        hr: Int?,
        battery: Int?,
        emgMv: Float?,
        stabilityVal: Double?,
        irTemp: Double?,
        bodyTemp: Double?
    ): List<PhysicalSensorInfo> {
        val now = System.currentTimeMillis()
        val isBtConnected = btState is BluetoothState.Connected
        val isBtConnecting = btState is BluetoothState.Connecting
        val isBtError = btState is BluetoothState.Error

        val mainDeviceState = when {
            isBtConnected -> SensorConnectionState.CONNECTED
            isBtConnecting -> SensorConnectionState.CONNECTING
            isBtError -> SensorConnectionState.ERROR
            else -> SensorConnectionState.DISCONNECTED
        }

        val deviceName = device?.name?.takeIf { it.isNotBlank() } ?: "Dispositivo BLE"
        val sensors = mutableListOf(
            PhysicalSensorInfo(
                id = "ble_device",
                name = deviceName,
                description = "Estado de la conexión Bluetooth LE",
                connectionState = mainDeviceState,
                protocol = "Bluetooth LE",
                isMainDevice = true
            )
        )

        fun addObservedSensor(
            id: String,
            name: String,
            description: String,
            reading: String?
        ) {
            val lastSeen = timestamps[id] ?: return
            sensors += PhysicalSensorInfo(
                id = id,
                name = name,
                description = description,
                connectionState = if (isBtConnected && now - lastSeen < 15000L) {
                    SensorConnectionState.CONNECTED
                } else {
                    SensorConnectionState.DISCONNECTED
                },
                protocol = "Bluetooth LE GATT",
                lastSeenMs = lastSeen,
                lastReading = reading
            )
        }

        addObservedSensor(
            id = "heartRate",
            name = "Frecuencia cardíaca BLE",
            description = "Lecturas recibidas del servicio estándar de frecuencia cardíaca",
            reading = hr?.let { "$it BPM" }
        )
        addObservedSensor(
            id = "battery",
            name = "Nivel de batería BLE",
            description = "Nivel informado por el servicio estándar de batería",
            reading = battery?.let { "$it%" }
        )
        addObservedSensor(
            id = "emg",
            name = "Módulo EMG",
            description = "Muestras EMG validadas recibidas por BLE",
            reading = emgMv?.let { "%.1f mV".format(it) }
        )

        return sensors
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
                !hasRiskMeasurements -> "Sin datos"
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
                riskAvailable = hasRiskMeasurements,
                fatigueAvailable = isSimulating,
                temperatureAvailable = temperature in 20.0..45.0,
                bloodPressureAvailable = bloodPressureHigh > 0 && bloodPressureLow > 0,
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
