package com.example.lession_equipo1.data.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothManager as AndroidBluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.lession_equipo1.model.EmgSample
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicLong
import java.util.UUID

sealed class BleConnectionState {
    data object Disconnected : BleConnectionState()
    data object Scanning : BleConnectionState()
    data object Connecting : BleConnectionState()
    data class Connected(val deviceAddress: String) : BleConnectionState()
    data object Reconnecting : BleConnectionState()
    data class Error(val message: String) : BleConnectionState()
}

data class ScannedBleDevice(
    val address: String,
    val name: String,
    val isEmgSensor: Boolean = false
)

class BluetoothManager(context: Context) {

    companion object {
        val EMG_SERVICE_UUID: UUID =
            UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
        val EMG_CHARACTERISTIC_UUID: UUID =
            UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
        val HR_SERVICE_UUID: UUID =
            UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val HR_MEASUREMENT_CHAR_UUID: UUID =
            UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        val BATTERY_SERVICE_UUID: UUID =
            UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        val BATTERY_LEVEL_CHAR_UUID: UUID =
            UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
        val CCCD_UUID: UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        const val EMG_DEVICE_NAME = "ESP32-C3-EMG"
        private const val MAX_RECONNECT_ATTEMPTS = 5
    }

    private data class PendingCharacteristicWrite(
        val characteristic: BluetoothGattCharacteristic,
        val value: ByteArray
    )

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private val adapter: BluetoothAdapter? =
        (appContext.getSystemService(Context.BLUETOOTH_SERVICE)
                as? AndroidBluetoothManager)?.adapter

    private val _connectionState =
        MutableStateFlow<BleConnectionState>(
            BleConnectionState.Disconnected
        )
    val connectionState: StateFlow<BleConnectionState> =
        _connectionState.asStateFlow()

    private val _scannedDevices =
        MutableStateFlow<List<ScannedBleDevice>>(emptyList())
    val scannedDevices: StateFlow<List<ScannedBleDevice>> =
        _scannedDevices.asStateFlow()

    private val _receivedData = MutableStateFlow("")
    val receivedData: StateFlow<String> = _receivedData.asStateFlow()

    private val _parsedHeartRate = MutableStateFlow<Int?>(null)
    val parsedHeartRate: StateFlow<Int?> = _parsedHeartRate.asStateFlow()

    private val _parsedBatteryLevel = MutableStateFlow<Int?>(null)
    val parsedBatteryLevel: StateFlow<Int?> =
        _parsedBatteryLevel.asStateFlow()

    private val _parsedEmgMv = MutableStateFlow<Float?>(null)
    val parsedEmgMv: StateFlow<Float?> = _parsedEmgMv.asStateFlow()

    private val _parsedEmgAdc = MutableStateFlow<Int?>(null)
    val parsedEmgAdc: StateFlow<Int?> = _parsedEmgAdc.asStateFlow()

    private val _parsedMotionStability = MutableStateFlow<Double?>(null)
    val parsedMotionStability: StateFlow<Double?> =
        _parsedMotionStability.asStateFlow()

    private val _parsedIrTemp = MutableStateFlow<Double?>(null)
    val parsedIrTemp: StateFlow<Double?> = _parsedIrTemp.asStateFlow()

    private val _parsedBodyTemp = MutableStateFlow<Double?>(null)
    val parsedBodyTemp: StateFlow<Double?> =
        _parsedBodyTemp.asStateFlow()

    private val _parsedSpO2 = MutableStateFlow<Int?>(null)
    val parsedSpO2: StateFlow<Int?> = _parsedSpO2.asStateFlow()

    private val _lastTelemetryPacketMs = MutableStateFlow(0L)
    val lastTelemetryPacketMs: StateFlow<Long> =
        _lastTelemetryPacketMs.asStateFlow()

    private val _sensorTimestamps =
        MutableStateFlow<Map<String, Long>>(emptyMap())
    val sensorTimestamps: StateFlow<Map<String, Long>> =
        _sensorTimestamps.asStateFlow()

    private val sampleSequence = AtomicLong(0L)
    private val _emgSamples = MutableSharedFlow<EmgSample>(extraBufferCapacity = 64)
    val emgSamples: SharedFlow<EmgSample> = _emgSamples

    private val _emgSampleSequence = MutableStateFlow(0L)
    val emgSampleSequence: StateFlow<Long> =
        _emgSampleSequence.asStateFlow()

    private val _supportsEmgCommands = MutableStateFlow(false)
    val supportsEmgCommands: StateFlow<Boolean> =
        _supportsEmgCommands.asStateFlow()

    private val _commandError = MutableStateFlow<String?>(null)
    val commandError: StateFlow<String?> = _commandError.asStateFlow()

    private var gatt: BluetoothGatt? = null
    private var scanning = false
    private var userInitiatedDisconnect = true
    private var lastConnectedAddress: String? = null
    private var reconnectAttempt = 0
    private var scanTimeout: Runnable? = null
    private var reconnectRunnable: Runnable? = null
    private var emgCharacteristic: BluetoothGattCharacteristic? = null
    private var batteryCharacteristic: BluetoothGattCharacteristic? = null
    private var batteryReadRequested = false
    private val descriptorQueue = ArrayDeque<BluetoothGattDescriptor>()
    private var isWritingDescriptor = false
    private val characteristicWriteQueue = ArrayDeque<PendingCharacteristicWrite>()
    private var isWritingCharacteristic = false

    private val scanResults = mutableMapOf<String, ScannedBleDevice>()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(
            callbackType: Int,
            result: ScanResult
        ) {
            val device = result.device

            @SuppressLint("MissingPermission")
            val name = try {
                result.scanRecord?.deviceName
                    ?: device.name
                    ?: "Dispositivo sin nombre"
            } catch (_: SecurityException) {
                "Dispositivo Bluetooth"
            }

            synchronized(scanResults) {
                val isEmg = name == EMG_DEVICE_NAME ||
                    result.scanRecord?.serviceUuids?.any {
                        it.uuid == EMG_SERVICE_UUID
                    } == true
                scanResults[device.address] = ScannedBleDevice(
                    address = device.address,
                    name = name,
                    isEmgSensor = isEmg
                )
                _scannedDevices.value = scanResults.values.toList()
            }
        }

        override fun onScanFailed(errorCode: Int) {
            scanning = false
            _connectionState.value =
                BleConnectionState.Error(
                    "Falló el escaneo Bluetooth. Código: $errorCode"
                )
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(
            bluetoothGatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {
            if (this@BluetoothManager.gatt !== bluetoothGatt) {
                bluetoothGatt.close()
                return
            }

            if (status != BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value =
                    BleConnectionState.Error(
                        "Error de conexión GATT: $status"
                    )
                bluetoothGatt.close()
                this@BluetoothManager.gatt = null
                scheduleReconnection()
                return
            }

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    reconnectAttempt = 0
                    _connectionState.value =
                        BleConnectionState.Connected(
                            bluetoothGatt.device.address
                        )

                    if (hasPermissions()) {
                        try {
                            if (!bluetoothGatt.requestMtu(247)) {
                                bluetoothGatt.discoverServices()
                            }
                        } catch (_: SecurityException) {
                            _connectionState.value = BleConnectionState.Error(
                                "Se perdió el permiso Bluetooth durante la conexión."
                            )
                        }
                    }
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    this@BluetoothManager.gatt = null
                    bluetoothGatt.close()
                    emgCharacteristic = null
                    _supportsEmgCommands.value = false
                    descriptorQueue.clear()
                    isWritingDescriptor = false
                    characteristicWriteQueue.clear()
                    isWritingCharacteristic = false
                    if (userInitiatedDisconnect) {
                        _connectionState.value = BleConnectionState.Disconnected
                    } else {
                        scheduleReconnection()
                    }
                }
            }
        }

        override fun onServicesDiscovered(
            bluetoothGatt: BluetoothGatt,
            status: Int
        ) {
            if (this@BluetoothManager.gatt !== bluetoothGatt) return
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value =
                    BleConnectionState.Error(
                        "No se pudieron consultar los servicios BLE."
                    )
                return
            }

            emgCharacteristic = bluetoothGatt
                .getService(EMG_SERVICE_UUID)
                ?.getCharacteristic(EMG_CHARACTERISTIC_UUID)
                ?.takeIf { characteristic ->
                    val hasWrite = characteristic.properties and (
                        BluetoothGattCharacteristic.PROPERTY_WRITE or
                            BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
                    ) != 0
                    val hasUpdates = characteristic.properties and (
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY or
                            BluetoothGattCharacteristic.PROPERTY_INDICATE
                    ) != 0
                    hasWrite && hasUpdates
                }
            _supportsEmgCommands.value = emgCharacteristic != null

            val characteristics = listOfNotNull(
                bluetoothGatt.getService(EMG_SERVICE_UUID)
                    ?.getCharacteristic(EMG_CHARACTERISTIC_UUID),
                bluetoothGatt.getService(HR_SERVICE_UUID)
                    ?.getCharacteristic(HR_MEASUREMENT_CHAR_UUID),
                bluetoothGatt.getService(BATTERY_SERVICE_UUID)
                    ?.getCharacteristic(BATTERY_LEVEL_CHAR_UUID)
            )
            characteristics.forEach { characteristic ->
                enableNotifications(bluetoothGatt, characteristic)
            }
            batteryCharacteristic = bluetoothGatt
                .getService(BATTERY_SERVICE_UUID)
                ?.getCharacteristic(BATTERY_LEVEL_CHAR_UUID)
            if (
                batteryCharacteristic?.properties
                    ?.and(BluetoothGattCharacteristic.PROPERTY_READ) != 0
            ) {
                writeNextDescriptor(bluetoothGatt)
            }
        }

        @Deprecated("Compatibilidad con versiones anteriores de Android")
        override fun onCharacteristicChanged(
            bluetoothGatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (this@BluetoothManager.gatt !== bluetoothGatt) return
            processCharacteristic(
                characteristic.uuid,
                characteristic.value ?: byteArrayOf()
            )
        }

        override fun onCharacteristicChanged(
            bluetoothGatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (this@BluetoothManager.gatt !== bluetoothGatt) return
            processCharacteristic(characteristic.uuid, value)
        }

        @Deprecated("Compatibilidad con versiones anteriores de Android")
        override fun onCharacteristicRead(
            bluetoothGatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (this@BluetoothManager.gatt === bluetoothGatt && status == BluetoothGatt.GATT_SUCCESS) {
                processCharacteristic(
                    characteristic.uuid,
                    characteristic.value ?: byteArrayOf()
                )
            }
        }

        override fun onCharacteristicRead(
            bluetoothGatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (this@BluetoothManager.gatt === bluetoothGatt && status == BluetoothGatt.GATT_SUCCESS) {
                processCharacteristic(characteristic.uuid, value)
            }
        }

        override fun onDescriptorWrite(
            bluetoothGatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (this@BluetoothManager.gatt !== bluetoothGatt) return
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _commandError.value =
                    "No se pudo activar una notificación BLE."
            }
            isWritingDescriptor = false
            writeNextDescriptor(bluetoothGatt)
        }

        override fun onCharacteristicWrite(
            bluetoothGatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (this@BluetoothManager.gatt !== bluetoothGatt) return
            isWritingCharacteristic = false
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _commandError.value = "Falló la escritura del comando BLE (código $status)."
            }
            writeNextCharacteristic(bluetoothGatt)
        }

        override fun onMtuChanged(bluetoothGatt: BluetoothGatt, mtu: Int, status: Int) {
            if (this@BluetoothManager.gatt !== bluetoothGatt) return
            try {
                if (!bluetoothGatt.discoverServices()) {
                    _connectionState.value = BleConnectionState.Error(
                        "No se pudieron descubrir los servicios BLE."
                    )
                }
            } catch (_: SecurityException) {
                _connectionState.value = BleConnectionState.Error(
                    "Se perdió el permiso Bluetooth al consultar los servicios."
                )
            }
        }
    }

    fun hasPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return appContext.checkSelfPermission(
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED &&
                    appContext.checkSelfPermission(
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
        }

        return appContext.checkSelfPermission(
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isBluetoothEnabled(): Boolean {
        return adapter?.isEnabled == true
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (!hasPermissions()) {
            _connectionState.value =
                BleConnectionState.Error(
                    "Faltan permisos de Bluetooth."
                )
            return
        }

        val scanner = adapter?.bluetoothLeScanner

        if (adapter == null || !adapter.isEnabled || scanner == null) {
            _connectionState.value =
                BleConnectionState.Error(
                    "Activá Bluetooth en el teléfono."
                )
            return
        }

        stopScan()
        synchronized(scanResults) {
            scanResults.clear()
            _scannedDevices.value = emptyList()
        }

        try {
            scanner.startScan(scanCallback)
            scanning = true
            _connectionState.value = BleConnectionState.Scanning

            val timeout = Runnable {
                stopScan()
                if (_connectionState.value is BleConnectionState.Scanning) {
                    _connectionState.value =
                        BleConnectionState.Disconnected
                }
            }
            scanTimeout = timeout
            mainHandler.postDelayed(timeout, 15000L)
        } catch (e: SecurityException) {
            _connectionState.value =
                BleConnectionState.Error(
                    "Android denegó el permiso Bluetooth."
                )
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        scanTimeout?.let(mainHandler::removeCallbacks)
        scanTimeout = null

        try {
            if (scanning) {
                adapter?.bluetoothLeScanner?.stopScan(scanCallback)
            }
        } catch (_: SecurityException) {
            _connectionState.value = BleConnectionState.Error(
                "Se perdió el permiso para detener el escaneo Bluetooth."
            )
        }

        scanning = false
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String) {
        if (!hasPermissions()) {
            _connectionState.value =
                BleConnectionState.Error(
                    "Faltan permisos para conectar por Bluetooth."
                )
            return
        }

        val bluetoothAdapter = adapter

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _connectionState.value =
                BleConnectionState.Error("Bluetooth está desactivado.")
            return
        }

        stopScan()

        try {
            gatt?.close()
            gatt = null
            reconnectRunnable?.let(mainHandler::removeCallbacks)
            reconnectRunnable = null
            userInitiatedDisconnect = false
            lastConnectedAddress = address
            reconnectAttempt = 0
            emgCharacteristic = null
            batteryCharacteristic = null
            batteryReadRequested = false
            descriptorQueue.clear()
            isWritingDescriptor = false
            characteristicWriteQueue.clear()
            isWritingCharacteristic = false
            _supportsEmgCommands.value = false
            _parsedHeartRate.value = null
            _parsedBatteryLevel.value = null
            _parsedEmgMv.value = null
            _parsedEmgAdc.value = null
            _sensorTimestamps.value = emptyMap()
            _lastTelemetryPacketMs.value = 0L

            val device = bluetoothAdapter.getRemoteDevice(address)
            _connectionState.value = BleConnectionState.Connecting

            gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                device.connectGatt(
                    appContext,
                    false,
                    gattCallback,
                    BluetoothDevice.TRANSPORT_LE
                )
            } else {
                device.connectGatt(
                    appContext,
                    false,
                    gattCallback
                )
            }

            if (gatt == null) {
                _connectionState.value =
                    BleConnectionState.Error(
                        "No se pudo iniciar la conexión."
                    )
            }
        } catch (e: Exception) {
            _connectionState.value =
                BleConnectionState.Error(
                    "Error al conectar: ${e.message ?: "desconocido"}"
                )
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableNotifications(
        bluetoothGatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ) {
        val supportsUpdates = characteristic.properties and (
            BluetoothGattCharacteristic.PROPERTY_NOTIFY or
                BluetoothGattCharacteristic.PROPERTY_INDICATE
        ) != 0
        if (!supportsUpdates || !bluetoothGatt.setCharacteristicNotification(characteristic, true)) return

        val descriptor = characteristic.getDescriptor(
            CCCD_UUID
        ) ?: return

        descriptor.value = if (
            characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0
        ) {
            BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        } else {
            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        }
        descriptorQueue.addLast(descriptor)
        writeNextDescriptor(bluetoothGatt)
    }

    @SuppressLint("MissingPermission")
    private fun writeNextCharacteristic(bluetoothGatt: BluetoothGatt) {
        if (isWritingCharacteristic) return
        val pending = characteristicWriteQueue.removeFirstOrNull() ?: return
        val characteristic = pending.characteristic
        val writeType = if (
            characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0
        ) {
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        } else {
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        }
        isWritingCharacteristic = true
        val accepted = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bluetoothGatt.writeCharacteristic(
                    characteristic,
                    pending.value,
                    writeType
                ) == android.bluetooth.BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                characteristic.value = pending.value
                @Suppress("DEPRECATION")
                characteristic.writeType = writeType
                @Suppress("DEPRECATION")
                bluetoothGatt.writeCharacteristic(characteristic)
            }
        } catch (_: SecurityException) {
            _commandError.value = "Se perdió el permiso para enviar comandos Bluetooth."
            false
        }
        if (!accepted) {
            isWritingCharacteristic = false
            _commandError.value = "Android rechazó el comando Bluetooth."
            writeNextCharacteristic(bluetoothGatt)
        }
    }

    @SuppressLint("MissingPermission")
    private fun writeNextDescriptor(bluetoothGatt: BluetoothGatt) {
        if (isWritingDescriptor) return
        val descriptor = descriptorQueue.removeFirstOrNull()
        if (descriptor == null) {
            readBatteryLevel(bluetoothGatt)
            return
        }
        isWritingDescriptor = true
        val accepted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bluetoothGatt.writeDescriptor(
                descriptor,
                descriptor.value ?: BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            ) == android.bluetooth.BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            bluetoothGatt.writeDescriptor(descriptor)
        }
        if (!accepted) {
            isWritingDescriptor = false
            _commandError.value = "Android rechazó la configuración de una notificación BLE."
            writeNextDescriptor(bluetoothGatt)
        }
    }

    @SuppressLint("MissingPermission")
    private fun readBatteryLevel(bluetoothGatt: BluetoothGatt) {
        val characteristic = batteryCharacteristic ?: return
        if (batteryReadRequested || !hasPermissions()) return
        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ == 0) return
        batteryReadRequested = try {
            @Suppress("DEPRECATION")
            bluetoothGatt.readCharacteristic(characteristic)
        } catch (_: SecurityException) {
            _commandError.value = "Se perdió el permiso para leer la batería BLE."
            false
        }
    }

    private fun processCharacteristic(
        uuid: UUID,
        value: ByteArray
    ) {
        if (value.isEmpty()) return

        val now = System.currentTimeMillis()

        when (uuid) {
            HR_MEASUREMENT_CHAR_UUID -> {
                val flags = value[0].toInt() and 0xFF
                val heartRate = if ((flags and 0x01) == 0) {
                    if (value.size >= 2) {
                        value[1].toInt() and 0xFF
                    } else null
                } else {
                    if (value.size >= 3) {
                        (value[1].toInt() and 0xFF) or
                                ((value[2].toInt() and 0xFF) shl 8)
                    } else null
                }

                if (heartRate != null && heartRate in 1..300) {
                    _parsedHeartRate.value = heartRate
                    _sensorTimestamps.value =
                        _sensorTimestamps.value + ("heartRate" to now)
                    _lastTelemetryPacketMs.value = now
                }
            }

            BATTERY_LEVEL_CHAR_UUID -> {
                val battery = value[0].toInt() and 0xFF
                if (battery <= 100) {
                    _parsedBatteryLevel.value = battery

                    _sensorTimestamps.value =
                        _sensorTimestamps.value + ("battery" to now)
                }
            }

            EMG_CHARACTERISTIC_UUID -> {
                val raw = value.decodeToString().trim()
                _receivedData.value = raw
                try {
                    val json = JSONObject(raw)
                    if (json.optString("type") != "emg") return
                    val sensor = json.optString("sensor")
                    val metric = json.optString("metric")
                    val unit = json.optString("unit")
                    val status = json.optString("status")
                    if (
                        sensor != "emg_module" ||
                        metric != "emgSignal" ||
                        unit != "mV" ||
                        status != "valid" ||
                        !json.has("value") ||
                        !json.has("adcRaw") ||
                        !json.has("timestamp")
                    ) return

                    val valueMv = json.getDouble("value").toFloat()
                    val adcRaw = json.getInt("adcRaw")
                    val timestampMs = json.getLong("timestamp")
                    if (
                        !valueMv.isFinite() || valueMv < 0f ||
                        adcRaw !in 0..4095 || timestampMs < 0L
                    ) return

                    val sample = EmgSample(
                        sensor = sensor,
                        metric = metric,
                        valueMv = valueMv,
                        unit = unit,
                        adcRaw = adcRaw,
                        status = status,
                        timestampMs = timestampMs,
                        sequence = sampleSequence.incrementAndGet()
                    )
                    if (_emgSamples.tryEmit(sample)) {
                        _emgSampleSequence.value = sample.sequence
                        _parsedEmgMv.value = valueMv
                        _parsedEmgAdc.value = adcRaw
                        _sensorTimestamps.value =
                            _sensorTimestamps.value + ("emg" to now)
                        _lastTelemetryPacketMs.value = now
                    }
                } catch (_: JSONException) {
                    _commandError.value = "Se recibió un paquete EMG con formato JSON inválido."
                }
            }
        }
        _receivedData.value = "UUID: $uuid\nDatos: " +
            value.joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }
    }

    fun sendCommand(command: String) {
        val allowedCommands = setOf(
            "READ_DATA", "LED_RED_ON", "LED_RED_OFF", "LED_BLUE_ON",
            "LED_BLUE_OFF", "LED_GREEN_ON", "LED_GREEN_OFF", "ALL_OFF", "ALL_ON"
        )
        val characteristic = emgCharacteristic
        val bluetoothGatt = gatt
        if (bluetoothGatt == null || _connectionState.value !is BleConnectionState.Connected) {
            _commandError.value = "El sensor BLE no está conectado."
        } else if (command !in allowedCommands) {
            _commandError.value = "El comando no está soportado por el firmware."
        } else if (characteristic == null) {
            _commandError.value = "Este dispositivo no ofrece control EMG."
        } else {
            _commandError.value = null
            characteristicWriteQueue.addLast(
                PendingCharacteristicWrite(
                    characteristic,
                    command.toByteArray(Charsets.UTF_8)
                )
            )
            writeNextCharacteristic(bluetoothGatt)
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        stopScan()
        userInitiatedDisconnect = true
        lastConnectedAddress = null
        reconnectRunnable?.let(mainHandler::removeCallbacks)
        reconnectRunnable = null

        try {
            gatt?.disconnect()
            gatt?.close()
        } catch (_: SecurityException) {
            // El permiso pudo revocarse.
        }

        gatt = null
        emgCharacteristic = null
        batteryCharacteristic = null
        batteryReadRequested = false
        descriptorQueue.clear()
        isWritingDescriptor = false
        characteristicWriteQueue.clear()
        isWritingCharacteristic = false
        _supportsEmgCommands.value = false
        _connectionState.value = BleConnectionState.Disconnected
    }

    fun unregisterReceiver() {
        // Esta implementación no registra receptores.
    }

    @SuppressLint("MissingPermission")
    private fun scheduleReconnection() {
        if (userInitiatedDisconnect) return
        val address = lastConnectedAddress ?: return
        if (!hasPermissions()) {
            _connectionState.value = BleConnectionState.Error(
                "Se perdió el permiso Bluetooth; no se puede reconectar."
            )
            return
        }
        if (reconnectAttempt >= MAX_RECONNECT_ATTEMPTS) {
            _connectionState.value = BleConnectionState.Error(
                "No se pudo reconectar al dispositivo Bluetooth."
            )
            return
        }

        reconnectAttempt += 1
        _connectionState.value = BleConnectionState.Reconnecting
        val delayMs = 1000L shl (reconnectAttempt - 1)
        reconnectRunnable = Runnable {
            if (userInitiatedDisconnect || gatt != null) return@Runnable
            try {
                val device = adapter?.getRemoteDevice(address)
                if (device == null || adapter?.isEnabled != true) {
                    scheduleReconnection()
                    return@Runnable
                }
                gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    device.connectGatt(
                        appContext,
                        false,
                        gattCallback,
                        BluetoothDevice.TRANSPORT_LE
                    )
                } else {
                    device.connectGatt(appContext, false, gattCallback)
                }
                if (gatt == null) scheduleReconnection()
            } catch (_: SecurityException) {
                _connectionState.value = BleConnectionState.Error(
                    "Android denegó el permiso para reconectar por Bluetooth."
                )
            } catch (_: IllegalArgumentException) {
                _connectionState.value = BleConnectionState.Error(
                    "El dispositivo Bluetooth guardado ya no está disponible."
                )
            }
        }
        mainHandler.postDelayed(reconnectRunnable!!, delayMs)
    }

}