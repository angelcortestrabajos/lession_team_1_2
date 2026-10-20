package com.example.lession_equipo1.data.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import com.example.lession_equipo1.model.EmgSample
import org.json.JSONObject
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong

sealed class BleConnectionState {
    object Disconnected : BleConnectionState()
    object Scanning : BleConnectionState()
    object Connecting : BleConnectionState()
    data class Connected(val deviceName: String, val deviceAddress: String) : BleConnectionState()
    data class Reconnecting(val attempt: Int, val maxAttempts: Int) : BleConnectionState()
    data class Error(val message: String) : BleConnectionState()
}

data class ScannedBleDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val isEmgSensor: Boolean = false
)

class BluetoothManager(private val context: Context) {

    companion object {
        private const val TAG = "BluetoothManager"

        // UUIDs específicos para ESP32-C3-EMG
        val EMG_SERVICE_UUID: UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
        val EMG_CHARACTERISTIC_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")

        // UUIDs estándar
        val HR_SERVICE_UUID: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val HR_MEASUREMENT_CHAR_UUID: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        val BATTERY_SERVICE_UUID: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        val BATTERY_LEVEL_CHAR_UUID: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        const val EMG_DEVICE_NAME = "ESP32-C3-EMG"

        private const val MAX_RECONNECT_ATTEMPTS = 5
        private const val INITIAL_RECONNECT_DELAY_MS = 2000L
        private const val SCAN_TIMEOUT_MS = 15000L
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
        manager?.adapter
    }

    private var bluetoothGatt: BluetoothGatt? = null

    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<ScannedBleDevice>>(emptyList())
    val scannedDevices: StateFlow<List<ScannedBleDevice>> = _scannedDevices.asStateFlow()

    private val _parsedHeartRate = MutableStateFlow<Int?>(null)
    val parsedHeartRate: StateFlow<Int?> = _parsedHeartRate.asStateFlow()

    private val _parsedBatteryLevel = MutableStateFlow<Int?>(null)
    val parsedBatteryLevel: StateFlow<Int?> = _parsedBatteryLevel.asStateFlow()

    private val _parsedEmgMv = MutableStateFlow<Float?>(null)
    val parsedEmgMv: StateFlow<Float?> = _parsedEmgMv.asStateFlow()

    private val _parsedEmgAdc = MutableStateFlow<Int?>(null)
    val parsedEmgAdc: StateFlow<Int?> = _parsedEmgAdc.asStateFlow()

    private val sampleSequence = AtomicLong(0)
    private val _emgSampleSequence = MutableStateFlow(0L)
    val emgSampleSequence: StateFlow<Long> = _emgSampleSequence.asStateFlow()
    private val _emgSamples = MutableSharedFlow<EmgSample>(extraBufferCapacity = 64)
    val emgSamples: SharedFlow<EmgSample> = _emgSamples
    private val _supportsEmgCommands = MutableStateFlow(false)
    val supportsEmgCommands: StateFlow<Boolean> = _supportsEmgCommands.asStateFlow()
    private val _commandError = MutableStateFlow<String?>(null)
    val commandError: StateFlow<String?> = _commandError.asStateFlow()

    private val _receivedData = MutableStateFlow("")
    val receivedData: StateFlow<String> = _receivedData.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var isUserInitiatedDisconnect = false
    private var lastConnectedDeviceAddress: String? = null
    private var reconnectAttempt = 0

    private val descriptorWriteQueue = ConcurrentLinkedQueue<BluetoothGattDescriptor>()
    private var isWritingDescriptor = false
    private data class PendingGattWrite(
        val characteristic: BluetoothGattCharacteristic,
        val value: ByteArray
    )
    private val characteristicWriteQueue = ConcurrentLinkedQueue<PendingGattWrite>()
    private var isWritingCharacteristic = false
    private var emgCharacteristic: BluetoothGattCharacteristic? = null
    private var batteryCharacteristic: BluetoothGattCharacteristic? = null
    private var pendingSetupDescriptors = 0

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                    BluetoothAdapter.STATE_OFF -> {
                        disconnect()
                        _connectionState.value = BleConnectionState.Error("Bluetooth apagado por el sistema")
                    }
                    BluetoothAdapter.STATE_ON -> {
                        Log.d(TAG, "Bluetooth encendido de nuevo")
                    }
                }
            }
        }
    }

    init {
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        ContextCompat.registerReceiver(
            context,
            bluetoothStateReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            _connectionState.value = BleConnectionState.Error("Adaptador Bluetooth no disponible")
            return
        }

        _scannedDevices.value = emptyList()
        _connectionState.value = BleConnectionState.Scanning

        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            _connectionState.value = BleConnectionState.Error("Scanner BLE no disponible")
            return
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner.startScan(null, settings, scanCallback)
            mainHandler.postDelayed({ stopScan() }, SCAN_TIMEOUT_MS)
        } catch (e: SecurityException) {
            _connectionState.value = BleConnectionState.Error("Permisos BLE faltantes: ${e.localizedMessage}")
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (_connectionState.value is BleConnectionState.Scanning) {
            try {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            } catch (e: Exception) {
                Log.e(TAG, "Error al detener escaneo", e)
            }
            _connectionState.value = BleConnectionState.Disconnected
        }
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            val rawName = result.scanRecord?.deviceName ?: device.name
            val address = device.address

            // Determinar si es el sensor ESP32-C3-EMG por nombre o UUID de servicio anunciado
            val serviceUuids = result.scanRecord?.serviceUuids?.map { it.uuid } ?: emptyList()
            val isEmg = rawName == EMG_DEVICE_NAME || serviceUuids.contains(EMG_SERVICE_UUID)

            val name = when {
                !rawName.isNullOrBlank() -> rawName
                isEmg -> EMG_DEVICE_NAME
                else -> "Dispositivo BLE ($address)"
            }

            val currentList = _scannedDevices.value.toMutableList()
            val existingIndex = currentList.indexOfFirst { it.address == address }

            val scannedDevice = ScannedBleDevice(name, address, result.rssi, isEmgSensor = isEmg)
            if (existingIndex >= 0) {
                currentList[existingIndex] = scannedDevice
            } else {
                // Dar prioridad visual a sensores EMG colocándolos al inicio
                if (isEmg) {
                    currentList.add(0, scannedDevice)
                } else {
                    currentList.add(scannedDevice)
                }
            }
            _scannedDevices.value = currentList
        }

        override fun onScanFailed(errorCode: Int) {
            _connectionState.value = BleConnectionState.Error("Falló el escaneo BLE. Código: $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String) {
        stopScan()
        isUserInitiatedDisconnect = false
        lastConnectedDeviceAddress = address
        reconnectAttempt = 0
        try {
            bluetoothGatt?.close()
        } catch (e: SecurityException) {
            Log.w(TAG, "No se pudo liberar la conexión GATT anterior", e)
        } finally {
            bluetoothGatt = null
        }
        emgCharacteristic = null
        batteryCharacteristic = null
        _supportsEmgCommands.value = false
        _commandError.value = null
        descriptorWriteQueue.clear()
        pendingSetupDescriptors = 0
        isWritingDescriptor = false
        characteristicWriteQueue.clear()
        isWritingCharacteristic = false

        val device = bluetoothAdapter?.getRemoteDevice(address)
        if (device == null) {
            _connectionState.value = BleConnectionState.Error("Dispositivo no encontrado")
            return
        }

        _connectionState.value = BleConnectionState.Connecting

        try {
            bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                device.connectGatt(context, false, gattCallback)
            }
        } catch (e: SecurityException) {
            _connectionState.value = BleConnectionState.Error("Error de permisos al conectar: ${e.localizedMessage}")
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        isUserInitiatedDisconnect = true
        lastConnectedDeviceAddress = null
        mainHandler.removeCallbacksAndMessages(null)
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (e: SecurityException) {
            Log.e(TAG, "Error de seguridad al desconectar", e)
        } finally {
            bluetoothGatt = null
            _connectionState.value = BleConnectionState.Disconnected
            _parsedHeartRate.value = null
            _parsedBatteryLevel.value = null
            _parsedEmgMv.value = null
            _parsedEmgAdc.value = null
            _receivedData.value = ""
            emgCharacteristic = null
            batteryCharacteristic = null
            _supportsEmgCommands.value = false
            descriptorWriteQueue.clear()
            isWritingDescriptor = false
            pendingSetupDescriptors = 0
            characteristicWriteQueue.clear()
            isWritingCharacteristic = false
        }
    }

    @SuppressLint("MissingPermission")
    private fun scheduleReconnection() {
        if (isUserInitiatedDisconnect) return
        val address = lastConnectedDeviceAddress ?: return

        if (reconnectAttempt < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempt++
            val delay = INITIAL_RECONNECT_DELAY_MS * (1 shl (reconnectAttempt - 1))
            _connectionState.value = BleConnectionState.Reconnecting(reconnectAttempt, MAX_RECONNECT_ATTEMPTS)

            mainHandler.postDelayed({
                if (!isUserInitiatedDisconnect) {
                    val device = bluetoothAdapter?.getRemoteDevice(address)
                    bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        device?.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
                    } else {
                        device?.connectGatt(context, false, gattCallback)
                    }
                }
            }, delay)
        } else {
            _connectionState.value = BleConnectionState.Error("Reconexión fallida tras $MAX_RECONNECT_ATTEMPTS intentos")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (gatt !== bluetoothGatt) {
                gatt.close()
                return
            }
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    reconnectAttempt = 0
                    bluetoothGatt = gatt
                    _connectionState.value = BleConnectionState.Connecting
                    try {
                        if (!gatt.requestMtu(247) && !gatt.discoverServices()) {
                            failGattSetup(gatt, "No se pudieron descubrir los servicios GATT")
                        }
                    } catch (exception: SecurityException) {
                        failGattSetup(gatt, "Permiso BLE perdido durante la conexión")
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    gatt.close()
                    bluetoothGatt = null
                    emgCharacteristic = null
                    batteryCharacteristic = null
                    _supportsEmgCommands.value = false
                    descriptorWriteQueue.clear()
                    pendingSetupDescriptors = 0
                    isWritingDescriptor = false
                    characteristicWriteQueue.clear()
                    isWritingCharacteristic = false
                    if (!isUserInitiatedDisconnect) {
                        scheduleReconnection()
                    } else {
                        _connectionState.value = BleConnectionState.Disconnected
                    }
                }
            } else {
                gatt.close()
                bluetoothGatt = null
                emgCharacteristic = null
                batteryCharacteristic = null
                _supportsEmgCommands.value = false
                descriptorWriteQueue.clear()
                pendingSetupDescriptors = 0
                isWritingDescriptor = false
                characteristicWriteQueue.clear()
                isWritingCharacteristic = false
                if (!isUserInitiatedDisconnect) {
                    scheduleReconnection()
                } else {
                    _connectionState.value = BleConnectionState.Disconnected
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (gatt !== bluetoothGatt) return
            if (status != BluetoothGatt.GATT_SUCCESS) {
                failGattSetup(gatt, "Falló el descubrimiento de servicios GATT: $status")
                return
            }
            try {
                var hasSupportedSensor = false
                val emg = gatt.getService(EMG_SERVICE_UUID)?.getCharacteristic(EMG_CHARACTERISTIC_UUID)
                if (emg != null) {
                    val canNotify = emg.properties and
                        (BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                    val canWrite = emg.properties and
                        (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
                    if (!canNotify || !canWrite) {
                        failGattSetup(gatt, "La característica EMG no admite notificaciones y escritura")
                        return
                    }
                    emgCharacteristic = emg
                    hasSupportedSensor = true
                    if (!enableNotification(gatt, emg)) {
                        failGattSetup(gatt, "No se pudo habilitar la notificación de la característica EMG")
                        return
                    }
                }
                val heartRate = gatt.getService(HR_SERVICE_UUID)?.getCharacteristic(HR_MEASUREMENT_CHAR_UUID)
                if (heartRate != null) {
                    hasSupportedSensor = true
                    if (!enableNotification(gatt, heartRate) && emg == null) {
                        failGattSetup(gatt, "No se pudo habilitar la notificación de ritmo cardíaco")
                        return
                    }
                }
                if (!hasSupportedSensor) {
                    failGattSetup(gatt, "El dispositivo no ofrece un servicio BLE compatible")
                    return
                }
                batteryCharacteristic =
                    gatt.getService(BATTERY_SERVICE_UUID)?.getCharacteristic(BATTERY_LEVEL_CHAR_UUID)
                completeGattSetupIfReady(gatt)
            } catch (exception: SecurityException) {
                failGattSetup(gatt, "Permiso BLE perdido al configurar la característica")
            }
        }

        @Deprecated("Usado para APIs anteriores a Android 13")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (gatt !== bluetoothGatt) return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                parseCharacteristicData(characteristic)
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (gatt !== bluetoothGatt) return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                parseCharacteristicData(characteristic, value)
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (gatt !== bluetoothGatt) return
            isWritingCharacteristic = false
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _commandError.value = "Falló la escritura del comando BLE (código $status)"
                Log.e(TAG, _commandError.value!!)
            }
            processNextCharacteristicWrite(gatt)
        }

        @Deprecated("Usado para APIs anteriores a Android 13")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (gatt !== bluetoothGatt) return
            parseCharacteristicData(characteristic)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (gatt !== bluetoothGatt) return
            parseCharacteristicData(characteristic, value)
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (gatt !== bluetoothGatt) return
            isWritingDescriptor = false
            if (status != BluetoothGatt.GATT_SUCCESS) {
                failGattSetup(gatt, "Falló la configuración de notificaciones GATT: $status")
                return
            }
            pendingSetupDescriptors--
            processNextDescriptorWrite(gatt)
            if (pendingSetupDescriptors == 0 && descriptorWriteQueue.isEmpty() && !isWritingDescriptor) {
                completeGattSetupIfReady(gatt)
            }
        }

        @SuppressLint("MissingPermission")
        private fun completeGattSetupIfReady(gatt: BluetoothGatt) {
            if (bluetoothGatt !== gatt || _connectionState.value !is BleConnectionState.Connecting) return
            if (pendingSetupDescriptors != 0 || descriptorWriteQueue.isNotEmpty() || isWritingDescriptor) return
            _supportsEmgCommands.value = emgCharacteristic != null
            try {
                batteryCharacteristic?.let { readCharacteristic(it) }
                val deviceName = gatt.device.name ?: "Dispositivo BLE"
                _connectionState.value = BleConnectionState.Connected(deviceName, gatt.device.address)
            } catch (exception: SecurityException) {
                failGattSetup(gatt, "Permiso BLE perdido al finalizar la conexión")
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (gatt !== bluetoothGatt) return
            try {
                if (!gatt.discoverServices()) {
                    failGattSetup(gatt, "No se pudieron descubrir los servicios GATT")
                }
            } catch (exception: SecurityException) {
                failGattSetup(gatt, "Permiso BLE perdido al descubrir servicios")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableNotification(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic): Boolean {
        if (!gatt.setCharacteristicNotification(characteristic, true)) return false
        val descriptor = characteristic.getDescriptor(CCCD_UUID)
        if (descriptor == null) return false
        @Suppress("DEPRECATION")
        descriptor.value = if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) {
            BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        } else {
            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        }
        descriptorWriteQueue.add(descriptor)
        pendingSetupDescriptors++
        processNextDescriptorWrite(gatt)
        return true
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun processNextDescriptorWrite(gatt: BluetoothGatt) {
        if (isWritingDescriptor) return
        val nextDescriptor = descriptorWriteQueue.poll() ?: return
        isWritingDescriptor = true
        val value = nextDescriptor.value ?: BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        val accepted = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(nextDescriptor, value) == BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(nextDescriptor)
            }
        } catch (exception: SecurityException) {
            isWritingDescriptor = false
            failGattSetup(gatt, "Permiso BLE perdido al habilitar notificaciones")
            false
        }
        if (!accepted) {
            isWritingDescriptor = false
            failGattSetup(gatt, "Android rechazó la escritura del descriptor CCCD")
        }
    }

    @SuppressLint("MissingPermission")
    private fun failGattSetup(gatt: BluetoothGatt, message: String) {
        Log.e(TAG, message)
        gatt.close()
        if (bluetoothGatt === gatt) bluetoothGatt = null
        emgCharacteristic = null
        batteryCharacteristic = null
        descriptorWriteQueue.clear()
        characteristicWriteQueue.clear()
        pendingSetupDescriptors = 0
        isWritingDescriptor = false
        isWritingCharacteristic = false
        _supportsEmgCommands.value = false
        _connectionState.value = BleConnectionState.Error(message)
    }

    @SuppressLint("MissingPermission")
    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        try {
            bluetoothGatt?.readCharacteristic(characteristic)
        } catch (exception: SecurityException) {
            _connectionState.value = BleConnectionState.Error("Permiso BLE perdido al leer la característica")
        }
    }

    @SuppressLint("MissingPermission")
    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic, data: ByteArray) {
        val gatt = bluetoothGatt ?: return
        characteristicWriteQueue.add(PendingGattWrite(characteristic, data.copyOf()))
        processNextCharacteristicWrite(gatt)
    }

    @SuppressLint("MissingPermission")
    private fun processNextCharacteristicWrite(gatt: BluetoothGatt) {
        if (isWritingCharacteristic) return
        val pendingWrite = characteristicWriteQueue.poll() ?: return
        val characteristic = pendingWrite.characteristic
        val writeType = if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        } else {
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        }
        isWritingCharacteristic = true
        val accepted = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(characteristic, pendingWrite.value, writeType) == BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                characteristic.value = pendingWrite.value
                characteristic.writeType = writeType
                @Suppress("DEPRECATION")
                gatt.writeCharacteristic(characteristic)
            }
        } catch (exception: SecurityException) {
            isWritingCharacteristic = false
            Log.e(TAG, "Permiso BLE perdido al escribir comando", exception)
            false
        }
        if (!accepted) {
            isWritingCharacteristic = false
            _commandError.value = "Android rechazó la escritura del comando BLE."
            Log.e(TAG, "Android rechazó la escritura del comando BLE")
            processNextCharacteristicWrite(gatt)
        }
    }

    @SuppressLint("MissingPermission")
    fun sendCommand(command: String) {
        if (bluetoothGatt == null || _connectionState.value !is BleConnectionState.Connected) {
            _commandError.value = "El sensor BLE no está conectado."
            return
        }
        val allowedCommands = setOf(
            "READ_DATA", "LED_RED_ON", "LED_RED_OFF", "LED_BLUE_ON", "LED_BLUE_OFF",
            "LED_YELLOW_ON", "LED_YELLOW_OFF", "ALL_OFF", "ALL_ON"
        )
        if (command !in allowedCommands) {
            _commandError.value = "El comando no está soportado por el firmware."
            Log.w(TAG, "Comando BLE ignorado porque no está soportado por el firmware")
            return
        }
        val characteristic = emgCharacteristic
        if (characteristic == null) {
            _commandError.value = "Este dispositivo no ofrece control EMG."
            return
        }
        _commandError.value = null
        writeCharacteristic(characteristic, command.toByteArray(Charsets.UTF_8))
    }

    @Suppress("DEPRECATION")
    private fun parseCharacteristicData(
        characteristic: BluetoothGattCharacteristic,
        rawBytes: ByteArray? = null
    ) {
        val data = rawBytes ?: characteristic.value ?: return
        if (data.isEmpty()) return

        when (characteristic.uuid) {
            // Característica del módulo EMG ESP32-C3
            EMG_CHARACTERISTIC_UUID -> {
                val str = data.decodeToString().trim()
                _receivedData.value = str

                try {
                    val json = JSONObject(str)
                    if (json.optString("type") == "emg") {
                        val sensor = json.optString("sensor")
                        val metric = json.optString("metric")
                        val unit = json.optString("unit")
                        val status = json.optString("status")
                        if (sensor != "emg_module" || metric != "emgEnvelope" || unit != "mV" ||
                            status != "valid" || !json.has("value") || !json.has("adcRaw") ||
                            !json.has("timestamp")
                        ) {
                            Log.w(TAG, "Muestra EMG inválida o incompatible: $str")
                            return
                        }
                        val value = json.getDouble("value")
                        val adc = json.getInt("adcRaw")
                        val timestamp = json.getLong("timestamp")
                        val valueMv = value.toFloat()
                        if (!value.isFinite() || !valueMv.isFinite() || value < 0.0 ||
                            adc !in 0..4095 || timestamp < 0L
                        ) {
                            Log.w(TAG, "Muestra EMG fuera de rango: $str")
                            return
                        }
                        val sequence = sampleSequence.incrementAndGet()
                        val sample = EmgSample(
                            sensor = sensor,
                            metric = metric,
                            valueMv = valueMv,
                            unit = unit,
                            adcRaw = adc,
                            status = status,
                            timestampMs = timestamp,
                            sequence = sequence
                        )
                        _emgSampleSequence.value = sequence
                        if (!_emgSamples.tryEmit(sample)) {
                            Log.e(TAG, "Se descartó una muestra EMG porque el consumidor no pudo procesarla")
                            return
                        }
                        val mv = sample.valueMv
                        _parsedEmgMv.value = mv
                        _parsedEmgAdc.value = adc
                    } else if (json.optString("type") == "status") {
                        val msg = json.optString("message", "ready")
                        Log.d(TAG, "Estado recibido de ESP32-C3: $msg")
                    }
                } catch (e: org.json.JSONException) {
                    Log.w(TAG, "JSON BLE inválido", e)
                } catch (e: RuntimeException) {
                    Log.w(TAG, "No se pudo procesar el mensaje BLE", e)
                }
            }

            // Característica de Ritmo Cardíaco estándar
            HR_MEASUREMENT_CHAR_UUID -> {
                val flags = data[0].toInt()
                val is16Bit = (flags and 0x01) != 0
                val bpm = if (is16Bit && data.size >= 3) {
                    (data[1].toInt() and 0xFF) or ((data[2].toInt() and 0xFF) shl 8)
                } else if (data.size >= 2) {
                    data[1].toInt() and 0xFF
                } else null

                bpm?.let { _parsedHeartRate.value = it }
            }

            // Característica de Batería estándar
            BATTERY_LEVEL_CHAR_UUID -> {
                val battery = data[0].toInt() and 0xFF
                _parsedBatteryLevel.value = battery
            }
        }
    }

    fun hasPermissions(): Boolean {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(android.Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(android.Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }

        return permissions.all {
            androidx.core.content.ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    fun unregisterReceiver() {
        try {
            context.unregisterReceiver(bluetoothStateReceiver)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Receiver ya fue desregistrado", e)
        }
    }
}
