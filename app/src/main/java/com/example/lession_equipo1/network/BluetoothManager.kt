package com.example.lession_equipo1.network

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager as AndroidBluetoothManager
import android.content.Context
import com.example.lession_equipo1.data.ble.BleConnectionState
import com.example.lession_equipo1.data.ble.BluetoothManager as BleDataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class BluetoothState {
    object Idle : BluetoothState()
    object Scanning : BluetoothState()
    data class DeviceFound(val device: BluetoothDevice) : BluetoothState()
    object Connecting : BluetoothState()
    object Connected : BluetoothState()
    data class Error(val message: String) : BluetoothState()
}

data class DiscoveredBluetoothDevice(
    val device: BluetoothDevice,
    val name: String,
    val isEmgSensor: Boolean
)

class BluetoothController(private val context: Context) {

    private val bleManager = BleDataStoreManager(context.applicationContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val bluetoothAdapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? AndroidBluetoothManager)?.adapter

    private val _state = MutableStateFlow<BluetoothState>(BluetoothState.Idle)
    val state: StateFlow<BluetoothState> = _state.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<DiscoveredBluetoothDevice>>(emptyList())
    val scannedDevices: StateFlow<List<DiscoveredBluetoothDevice>> = _scannedDevices.asStateFlow()

    private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val connectedDevice: StateFlow<BluetoothDevice?> = _connectedDevice.asStateFlow()

    val receivedData: StateFlow<String> = bleManager.receivedData
    val parsedHeartRate: StateFlow<Int?> = bleManager.parsedHeartRate
    val parsedBatteryLevel: StateFlow<Int?> = bleManager.parsedBatteryLevel
    val parsedEmgMv: StateFlow<Float?> = bleManager.parsedEmgMv
    val parsedEmgAdc: StateFlow<Int?> = bleManager.parsedEmgAdc
    val parsedMotionStability: StateFlow<Double?> = bleManager.parsedMotionStability
    val parsedIrTemp: StateFlow<Double?> = bleManager.parsedIrTemp
    val parsedBodyTemp: StateFlow<Double?> = bleManager.parsedBodyTemp
    val parsedSpO2: StateFlow<Int?> = bleManager.parsedSpO2
    val lastTelemetryPacketMs: StateFlow<Long> = bleManager.lastTelemetryPacketMs
    val sensorTimestamps: StateFlow<Map<String, Long>> = bleManager.sensorTimestamps
    val emgSamples = bleManager.emgSamples
    val emgSampleSequence = bleManager.emgSampleSequence
    val supportsEmgCommands = bleManager.supportsEmgCommands
    val commandError = bleManager.commandError

    init {
        scope.launch {
            bleManager.connectionState.collect { connectionState ->
                when (connectionState) {
                    is BleConnectionState.Disconnected -> {
                        _state.value = BluetoothState.Idle
                        _connectedDevice.value = null
                    }
                    is BleConnectionState.Scanning -> _state.value = BluetoothState.Scanning
                    is BleConnectionState.Connecting -> _state.value = BluetoothState.Connecting
                    is BleConnectionState.Connected -> {
                        _state.value = BluetoothState.Connected
                        if (bluetoothAdapter != null && connectionState.deviceAddress.isNotEmpty()) {
                            try {
                                _connectedDevice.value = bluetoothAdapter.getRemoteDevice(connectionState.deviceAddress)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    is BleConnectionState.Reconnecting -> _state.value = BluetoothState.Connecting
                    is BleConnectionState.Error -> _state.value = BluetoothState.Error(connectionState.message)
                }
            }
        }

        scope.launch {
            bleManager.scannedDevices.collect { scannedList ->
                if (bluetoothAdapter != null) {
                    val devices = scannedList.mapNotNull { scanned ->
                        try {
                            DiscoveredBluetoothDevice(
                                device = bluetoothAdapter.getRemoteDevice(scanned.address),
                                name = scanned.name,
                                isEmgSensor = scanned.isEmgSensor
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    _scannedDevices.value = devices
                }
            }
        }
    }

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    @SuppressLint("MissingPermission")
    fun startScan(timeoutMs: Long = 15000L) {
        bleManager.startScan()
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        bleManager.stopScan()
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        bleManager.connect(device.address)
    }

    fun connect(address: String) {
        bleManager.connect(address)
    }

    fun sendCommand(command: String) {
        bleManager.sendCommand(command)
    }

    fun hasPermissions(): Boolean {
        return bleManager.hasPermissions()
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        bleManager.disconnect()
    }

    fun close() {
        disconnect()
        bleManager.unregisterReceiver()
        scope.cancel()
    }
}
