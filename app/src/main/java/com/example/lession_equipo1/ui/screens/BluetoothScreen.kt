package com.example.lession_equipo1.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lession_equipo1.BiometricViewModel
import com.example.lession_equipo1.network.BluetoothState
import com.example.lession_equipo1.ui.LocalAppColors

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothScreen(viewModel: BiometricViewModel, onBack: () -> Unit) {
    val state by viewModel.bluetoothState.collectAsState()
    val scannedDevices by viewModel.scannedDevices.collectAsState()
    val connectedDevice by viewModel.connectedDevice.collectAsState()
    val receivedData by viewModel.receivedData.collectAsState()
    val colors = LocalAppColors.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val allGranted = permissionsMap.values.all { it }
        if (allGranted) {
            viewModel.startBluetoothScan()
        }
    }

    fun checkAndStartScan() {
        if (!viewModel.hasBluetoothPermissions()) {
            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            } else {
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            }
            permissionLauncher.launch(permissions)
        } else {
            viewModel.startBluetoothScan()
        }
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Gestión de Sensores", color = colors.text) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = colors.text)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Busca y conecta tus bandas biométricas para iniciar el monitoreo en tiempo real.",
                color = colors.subText,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Dynamic Banner Status Card
            when (state) {
                is BluetoothState.Connected -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20).copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, Color.Green)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Green)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("¡Sensor Conectado!", color = Color.Green, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    val devName = connectedDevice?.name ?: "Banda Biométrica Lession"
                                    val devAddress = connectedDevice?.address ?: ""
                                    Text(devName, color = colors.text, fontSize = 14.sp)
                                    if (devAddress.isNotEmpty()) {
                                        Text("MAC: $devAddress", color = colors.subText, fontSize = 12.sp)
                                    }
                                }
                            }
                            if (receivedData.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Datos recibidos: $receivedData", color = colors.accent, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.disconnectBluetooth() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f), contentColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.LinkOff, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Desconectar Sensor")
                            }
                        }
                    }
                }

                is BluetoothState.Connecting -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(color = colors.accent, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Conectando al sensor...", color = colors.text, fontWeight = FontWeight.Bold)
                                connectedDevice?.name?.let {
                                    Text(it, color = colors.subText, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                is BluetoothState.Error -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, Color.Red)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color.Red)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    (state as BluetoothState.Error).message,
                                    color = Color.Red,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { checkAndStartScan() },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.Black),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reintentar")
                            }
                        }
                    }
                }

                else -> {}
            }

            // Scan Action Controls
            if (state is BluetoothState.Scanning) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = colors.accent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Buscando dispositivos cercanos...", color = colors.text, fontSize = 14.sp)
                    }
                    OutlinedButton(
                        onClick = { viewModel.stopBluetoothScan() },
                        border = BorderStroke(1.dp, colors.subText)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, tint = colors.text)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Detener", color = colors.text)
                    }
                }
            } else if (state !is BluetoothState.Connected && state !is BluetoothState.Connecting) {
                Button(
                    onClick = { checkAndStartScan() },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buscar Sensores", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Discovered Devices List Section
            if (scannedDevices.isNotEmpty()) {
                Text(
                    "Dispositivos Encontrados (${scannedDevices.size})",
                    color = colors.text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 12.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(scannedDevices) { device ->
                        DeviceItemCard(
                            device = device,
                            onConnect = { viewModel.connectToDevice(device) }
                        )
                    }
                }
            } else if (state is BluetoothState.Scanning) {
                Spacer(modifier = Modifier.height(32.dp))
                Icon(Icons.AutoMirrored.Filled.BluetoothSearching, contentDescription = null, tint = colors.accent, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Escaneando señales Bluetooth LE...", color = colors.subText, fontSize = 13.sp)
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun DeviceItemCard(
    device: BluetoothDevice,
    onConnect: () -> Unit
) {
    val colors = LocalAppColors.current
    val name = device.name?.takeIf { it.isNotBlank() } ?: "Dispositivo BLE Sin Nombre"
    val address = device.address ?: "Dirección Desconocida"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Bluetooth, contentDescription = null, tint = colors.accent)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(name, color = colors.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(address, color = colors.subText, fontSize = 12.sp)
                }
            }
            Button(
                onClick = onConnect,
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Conectar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
