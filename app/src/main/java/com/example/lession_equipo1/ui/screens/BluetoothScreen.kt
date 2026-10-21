package com.example.lession_equipo1.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lession_equipo1.BiometricViewModel
import com.example.lession_equipo1.network.BluetoothState
import com.example.lession_equipo1.network.DiscoveredBluetoothDevice
import com.example.lession_equipo1.ui.LocalAppColors

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothScreen(viewModel: BiometricViewModel, onBack: () -> Unit) {
    val state by viewModel.bluetoothState.collectAsStateWithLifecycle()
    val scannedDevices by viewModel.scannedDevices.collectAsStateWithLifecycle()
    val connectedDevice by viewModel.connectedDevice.collectAsStateWithLifecycle()
    val receivedData by viewModel.receivedData.collectAsStateWithLifecycle()
    val emgSession = viewModel.emgSession
    val latestEmgSample = emgSession.latestSample
    val supportsEmg by viewModel.supportsEmgCommands.collectAsStateWithLifecycle()
    val commandError by viewModel.commandError.collectAsStateWithLifecycle()
    val colors = LocalAppColors.current

    val context = LocalContext.current

    var isRedLedOn by remember { mutableStateOf(false) }
    var isBlueLedOn by remember { mutableStateOf(false) }
    var isYellowLedOn by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val allGranted = permissionsMap.values.all { it }
        if (allGranted) {
            viewModel.startBluetoothScan()
        } else {
            android.widget.Toast.makeText(
                context,
                "Se necesitan los permisos Bluetooth para buscar y conectar el sensor.",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    val checkAndStartScan: () -> Unit = {
        val permissions = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }.toTypedArray()

        val missingPermissions = permissions.filter { permission ->
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                permission
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(permissions)
        } else {
            viewModel.startBluetoothScan()
        }
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Gestión de Sensores BLE", color = colors.text) },
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
                "Conecta tu módulo ESP32-C3-EMG o banda biométrica para iniciar el monitoreo en tiempo real.",
                color = colors.subText,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val isSimulating = viewModel.isSimulating
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        when {
                            state is BluetoothState.Connected -> "Dispositivo BLE conectado"
                            state is BluetoothState.Connecting -> "Estado del sensor: conectando"
                            state is BluetoothState.Scanning -> "Estado del sensor: buscando"
                            state is BluetoothState.Error -> "Estado del sensor: error"
                            isSimulating -> "Fuente activa: simulación manual"
                            else -> "Sensor desconectado · simulación detenida"
                        },
                        color = colors.text,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (isSimulating) viewModel.stopSimulation()
                                else viewModel.startSimulation()
                            },
                            enabled = state is BluetoothState.Idle || state is BluetoothState.Error,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isSimulating) "Detener simulación" else "Iniciar simulación")
                        }
                        OutlinedButton(
                            onClick = { viewModel.resetEmgSession() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reset")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                if (!viewModel.setEmgMaximumReference()) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Se necesita una muestra EMG real válida para fijar el máximo.",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            enabled = state is BluetoothState.Connected && supportsEmg,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Máximo")
                        }
                        Text(
                            "Muestras ${emgSession.sampleCount} · Promedio ${emgSession.averageMv?.let { "%.1f".format(it) } ?: "--"} mV",
                            color = colors.subText,
                            modifier = Modifier.weight(1f).align(Alignment.CenterVertically)
                        )
                    }
                    Text(
                        "EMG actual: ${latestEmgSample?.let { "%.1f".format(it.valueMv) } ?: "--"} mV · " +
                        "ADC: ${latestEmgSample?.adcRaw?.toString() ?: "--"} · " +
                            "Máximo sesión: ${emgSession.maxSessionMv?.let { "%.1f".format(it) } ?: "--"} mV",
                        color = colors.text,
                        fontSize = 12.sp
                    )
                    viewModel.emgReferenceSaveError?.let { error ->
                        Text(error, color = Color.Red, fontSize = 12.sp)
                    }
                    Text(
                        "Referencia: ${emgSession.referenceMaxMv?.let { "%.1f".format(it) } ?: "--"} mV · " +
                            "Rendimiento: ${emgSession.performancePercent?.let { "%.0f".format(it) } ?: "--"}%",
                        color = colors.subText,
                        fontSize = 12.sp
                    )
                    Text("Muestras EMG recientes (mV)", color = colors.subText, fontSize = 12.sp)
                    Canvas(Modifier.fillMaxWidth().height(56.dp)) {
                        val values = emgSession.sampleValuesMv
                        if (values.size > 1) {
                            val maxValue = values.maxOrNull()?.takeIf { it > 0f } ?: 1f
                            val path = Path()
                            values.forEachIndexed { index, value ->
                                val x = size.width * index / (values.lastIndex)
                                val y = size.height * (1f - value / maxValue)
                                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawPath(path, color = colors.accent, style = Stroke(width = 3.dp.toPx()))
                        }
                    }
                }
            }

            // Tarjeta dinámica de estado
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
                                    Text(
                                        if (supportsEmg) "Sensor EMG conectado" else "Sensor BLE conectado",
                                        color = Color.Green,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    val devName = connectedDevice?.name?.takeIf { it.isNotBlank() } ?: "Dispositivo BLE"
                                    val devAddress = connectedDevice?.address ?: ""
                                    Text(devName, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    if (devAddress.isNotEmpty()) {
                                        Text("MAC: $devAddress", color = colors.subText, fontSize = 12.sp)
                                    }
                                }
                            }

                            if (supportsEmg) {
                            commandError?.let { error ->
                                Text(error, color = Color.Red, fontSize = 12.sp)
                            }
                            // Panel de lecturas EMG si están disponibles
                            if (latestEmgSample != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = Color.Green.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(12.dp))

                                Text("Señal EMG en Tiempo Real:", color = colors.text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Tensión: ${latestEmgSample.valueMv} mV", color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("ADC Raw: ${latestEmgSample.adcRaw} / 4095", color = colors.text, fontSize = 13.sp)
                                }

                                LinearProgressIndicator(
                                    progress = { (latestEmgSample.adcRaw / 4095.0f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .padding(top = 6.dp),
                                    color = colors.accent,
                                    trackColor = colors.surface
                                )
                            } else if (receivedData.contains("\"type\":\"status\"") ||
                                receivedData.contains("\"type\": \"status\"")
                            ) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Datos recibidos: $receivedData", color = colors.accent, fontSize = 12.sp)
                            } else {
                                Text("Esperando una muestra EMG válida.", color = colors.subText, fontSize = 12.sp)
                            }

                            // Panel de Comandos e Interacción LED con ESP32-C3
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Comandos para ESP32-C3:", color = colors.text, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        isRedLedOn = !isRedLedOn
                                        viewModel.sendEmgCommand(if (isRedLedOn) "LED_RED_ON" else "LED_RED_OFF")
                                    },
                                    border = BorderStroke(1.dp, if (isRedLedOn) Color.Red else colors.subText),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Rojo", color = if (isRedLedOn) Color.Red else colors.text, fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        isBlueLedOn = !isBlueLedOn
                                        viewModel.sendEmgCommand(if (isBlueLedOn) "LED_BLUE_ON" else "LED_BLUE_OFF")
                                    },
                                    border = BorderStroke(1.dp, if (isBlueLedOn) Color.Cyan else colors.subText),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Azul", color = if (isBlueLedOn) Color.Cyan else colors.text, fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        isYellowLedOn = !isYellowLedOn
                                        viewModel.sendEmgCommand(if (isYellowLedOn) "LED_YELLOW_ON" else "LED_YELLOW_OFF")
                                    },
                                    border = BorderStroke(1.dp, if (isYellowLedOn) Color.Yellow else colors.subText),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Amarillo", color = if (isYellowLedOn) Color.Yellow else colors.text, fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        isRedLedOn = false
                                        isBlueLedOn = false
                                        isYellowLedOn = false
                                        viewModel.sendEmgCommand("ALL_OFF")
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Apagar LEDs", color = colors.text, fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        isRedLedOn = true
                                        isBlueLedOn = true
                                        isYellowLedOn = true
                                        viewModel.sendEmgCommand("ALL_ON")
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Encender todos", color = colors.text, fontSize = 11.sp)
                                }

                                Button(
                                    onClick = { viewModel.sendEmgCommand("READ_DATA") },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.Black),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Leer EMG", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                            } else {
                                Text(
                                    "Este dispositivo no ofrece la característica EMG del ESP32-C3.",
                                    color = colors.subText,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

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
                                Text("Conectando al sensor BLE...", color = colors.text, fontWeight = FontWeight.Bold)
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
                                Text("Reintentar Escaneo")
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
                        Text("Buscando ESP32-C3-EMG y sensores...", color = colors.text, fontSize = 14.sp)
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
                    Text("Buscar Sensores BLE (ESP32-C3-EMG)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
                            onConnect = { viewModel.connectToDevice(device.device) }
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
    device: DiscoveredBluetoothDevice,
    onConnect: () -> Unit
) {
    val colors = LocalAppColors.current
    val address = device.device.address ?: "Dirección Desconocida"
    val isEmgSensor = device.isEmgSensor
    val displayName = device.name

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEmgSensor) colors.accent.copy(alpha = 0.1f) else colors.cardBackground
        ),
        border = if (isEmgSensor) BorderStroke(1.dp, colors.accent) else null
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
                Icon(
                    imageVector = if (isEmgSensor) Icons.Default.Sensors else Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = colors.accent
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(displayName, color = colors.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        if (isEmgSensor) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = colors.accent,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "EMG",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
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
