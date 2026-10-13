package com.example.lession_equipo1.network

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

sealed class BluetoothState {
    object Idle : BluetoothState()
    object Scanning : BluetoothState()
    data class DeviceFound(val device: BluetoothDevice) : BluetoothState()
    object Connecting : BluetoothState()
    object Connected : BluetoothState()
    data class Error(val message: String) : BluetoothState()
}

class BluetoothController(private val context: Context) {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _state = MutableStateFlow<BluetoothState>(BluetoothState.Idle)
    val state = _state.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val scannedDevices = _scannedDevices.asStateFlow()

    private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val connectedDevice = _connectedDevice.asStateFlow()

    private val _receivedData = MutableStateFlow("")
    val receivedData = _receivedData.asStateFlow()

    private var gatt: BluetoothGatt? = null

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            if (device != null) {
                val currentList = _scannedDevices.value
                if (currentList.none { it.address == device.address }) {
                    _scannedDevices.value = currentList + device
                }
                _state.value = BluetoothState.DeviceFound(device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            _state.value = BluetoothState.Error("Error al escanear (Código $errorCode)")
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (adapter == null) {
            _state.value = BluetoothState.Error("El dispositivo no tiene soporte para Bluetooth")
            return
        }
        if (!adapter.isEnabled) {
            _state.value = BluetoothState.Error("El Bluetooth está desactivado. Por favor, actívalo.")
            return
        }
        if (!hasPermissions()) {
            _state.value = BluetoothState.Error("Permisos de Bluetooth faltantes")
            return
        }

        _scannedDevices.value = emptyList()
        _state.value = BluetoothState.Scanning
        adapter.bluetoothLeScanner?.startScan(scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        adapter?.bluetoothLeScanner?.stopScan(scanCallback)
        if ((_state.value is BluetoothState.Scanning) || (_state.value is BluetoothState.DeviceFound)) {
            _state.value = BluetoothState.Idle
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        stopScan()
        _connectedDevice.value = device
        _state.value = BluetoothState.Connecting
        @Suppress("DEPRECATION")
        gatt = device.connectGatt(context, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _state.value = BluetoothState.Connected
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _state.value = BluetoothState.Idle
                _connectedDevice.value = null
                gatt.close()
                this@BluetoothController.gatt = null
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Habilitar notificaciones para características con soporte
                for (service in gatt.services) {
                    for (characteristic in service.characteristics) {
                        val properties = characteristic.properties
                        if ((properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0 ||
                            (properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                        ) {
                            gatt.setCharacteristicNotification(characteristic, true)
                            val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                            val descriptor = characteristic.getDescriptor(cccdUuid)
                            if (descriptor != null) {
                                @Suppress("DEPRECATION")
                                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                @Suppress("DEPRECATION")
                                gatt.writeDescriptor(descriptor)
                            }
                        }
                    }
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            @Suppress("DEPRECATION")
            val dataBytes = characteristic.value
            val dataStr = dataBytes?.decodeToString() ?: ""
            _receivedData.value = dataStr
        }
    }

    fun hasPermissions(): Boolean {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        _connectedDevice.value = null
        _state.value = BluetoothState.Idle
    }
}

