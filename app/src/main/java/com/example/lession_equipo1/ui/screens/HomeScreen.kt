package com.example.lession_equipo1.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.lession_equipo1.BiometricViewModel
import com.example.lession_equipo1.model.DataSourceState
import com.example.lession_equipo1.ui.LocalAppColors
import com.example.lession_equipo1.ui.LocalStrings
import com.example.lession_equipo1.viewmodel.AuthViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: BiometricViewModel,
    authViewModel: AuthViewModel,
    isDataMissing: Boolean,
    onBluetoothClick: () -> Unit,
    onCompleteProfileClick: () -> Unit
) {
    val user by authViewModel.currentUser.collectAsStateWithLifecycle()
    val riskLevel = viewModel.riskLevel
    val latestTelemetryMs by viewModel.lastTelemetryPacketMs.collectAsStateWithLifecycle()
    val dataSourceState by viewModel.dataSourceState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val colors = LocalAppColors.current
    var showDialog by remember(isDataMissing) { mutableStateOf(isDataMissing) }
    val lastNotification by viewModel.notifications.collectAsStateWithLifecycle(initialValue = "")

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Perfil Incompleto", fontWeight = FontWeight.Bold)
                    IconButton(onClick = { showDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }
            },
            text = { Text("Para calcular tu riesgo de lesión con precisión, necesitamos tus datos corporales.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        onCompleteProfileClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) {
                    Text("Completar ahora", color = Color.Black)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                user?.photoUrl?.let {
                    AsyncImage(
                        model = it,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                    )
                } ?: Icon(Icons.Default.AccountCircle, null, tint = colors.text, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(8.dp))
                val displayName = if (user?.name?.isNotEmpty() == true && user?.name != "Usuario") user?.name else strings.defaultUser
                Text("${strings.welcome} $displayName!", color = colors.text, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                val bleState by viewModel.bluetoothState.collectAsStateWithLifecycle()
                val bleBattery by viewModel.parsedBatteryLevel.collectAsStateWithLifecycle()
                val isConnected = bleState is com.example.lession_equipo1.network.BluetoothState.Connected

                IconButton(onClick = onBluetoothClick) {
                    Icon(
                        if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                        if (isConnected) "Bluetooth conectado" else "Abrir conexión Bluetooth",
                        tint = if (isConnected) Color(0xFF00FFCC) else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Icon(
                    Icons.Default.BatteryFull,
                    "Batería BLE",
                    tint = if (isConnected && bleBattery != null) colors.accent else colors.subText,
                    modifier = Modifier.size(20.dp)
                )
                val batteryText = if (isConnected && bleBattery != null) "$bleBattery%" else "--"
                Text(batteryText, color = colors.text, fontSize = 12.sp)
            }
        }

        if (latestTelemetryMs > 0L && dataSourceState != DataSourceState.REAL_SENSORS_ACTIVE) {
            Text(
                "La transmisión BLE se detuvo; se conserva la última lectura recibida.",
                color = colors.subText,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
        }

        if (lastNotification.isNotEmpty()) {
            val isCritical = lastNotification.contains("¡CRÍTICO!") || lastNotification.contains("¡RIESGO ALTO!")
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCritical) Color.Red.copy(alpha = 0.2f) else Color.Yellow.copy(alpha = 0.2f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isCritical) Color.Red else Color.Yellow)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isCritical) Icons.Default.Error else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isCritical) Color.Red else Color.Yellow
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(lastNotification, color = colors.text, fontSize = 12.sp)
                }
            }
        }

        // Indicador del Origen y Estado Real de Datos
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = dataSourceState.getColor().copy(alpha = 0.15f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, dataSourceState.getColor())
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when(dataSourceState) {
                        DataSourceState.REAL_SENSORS_ACTIVE -> Icons.Default.Sensors
                        DataSourceState.SIMULATION_ACTIVE -> Icons.Default.PlayCircle
                        DataSourceState.CONNECTED_NO_DATA -> Icons.Default.BluetoothConnected
                        DataSourceState.COMMUNICATION_ERROR -> Icons.Default.ErrorOutline
                        DataSourceState.SENSORS_DISCONNECTED -> Icons.Default.SensorsOff
                    },
                    contentDescription = null,
                    tint = dataSourceState.getColor()
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Origen de datos: ${dataSourceState.getLabel()}",
                        color = dataSourceState.getColor(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    if (dataSourceState == DataSourceState.REAL_SENSORS_ACTIVE) {
                        Text(
                            text = "Se recibieron mediciones válidas desde Bluetooth LE.",
                            color = colors.subText,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = latestTelemetryMs.takeIf { it > 0L }?.let {
                            "Último dato BLE: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(it))}"
                        } ?: "Esperando mediciones BLE",
                        color = colors.subText,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        if (viewModel.hasRiskMeasurements) {
            NewRiskGauge(riskLevel)
            Spacer(modifier = Modifier.height(12.dp))
            RecommendationMessage(riskLevel)
        } else {
            Text(
                "Esperando mediciones para estimar el riesgo",
                color = colors.subText,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text(strings.bioSignals, color = colors.text, fontSize = 18.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
        Spacer(modifier = Modifier.height(16.dp))

        BiometricCard(
            strings.heartRate,
            if (viewModel.heartRate > 0.0) viewModel.heartRate.toInt().toString() else "Sin datos",
            if (viewModel.heartRate > 0.0) "BPM" else "",
            Icons.Default.Favorite,
            Color.Red,
            strings.currentFreq
        )
        if (viewModel.spO2 > 0) {
            BiometricCard("Saturación O2 (SpO2)", "${viewModel.spO2}", "%", Icons.Default.WaterDrop, Color.Cyan, "Pulsioxímetro MAX30102")
        }
        BiometricCard(
            strings.bodyTemp,
            if (viewModel.temperature > 0.0) "%.1f".format(viewModel.temperature) else "Sin datos",
            if (viewModel.temperature > 0.0) "°C" else "",
            Icons.Default.Thermostat,
            Color.Yellow,
            strings.currentTemp
        )
        BiometricCard(
            strings.bloodPressure,
            if (viewModel.bloodPressureHigh > 0 && viewModel.bloodPressureLow > 0) {
                "${viewModel.bloodPressureHigh}/${viewModel.bloodPressureLow}"
            } else {
                "Sin datos"
            },
            if (viewModel.bloodPressureHigh > 0 && viewModel.bloodPressureLow > 0) "mmHg" else "",
            Icons.Default.WaterDrop,
            Color.Red,
            strings.currentPress
        )
        BiometricCard(
            strings.muscleFatigue,
            if (viewModel.isSimulating) {
                viewModel.muscles.asSequence().map { it.fatigue }.average().toInt().toString()
            } else {
                "Sin datos"
            },
            if (viewModel.isSimulating) "%" else "",
            Icons.Default.FitnessCenter,
            Color.Green,
            strings.currentCarga
        )

        val bluetoothState by viewModel.bluetoothState.collectAsStateWithLifecycle()
        val supportsEmg by viewModel.supportsEmgCommands.collectAsStateWithLifecycle()
        val emgSession = viewModel.emgSession
        val isSimulating = viewModel.isSimulating
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    when {
                        bluetoothState is com.example.lession_equipo1.network.BluetoothState.Connected && supportsEmg ->
                            "Dispositivo BLE conectado · módulo EMG compatible"
                        bluetoothState is com.example.lession_equipo1.network.BluetoothState.Connected ->
                            "Dispositivo BLE conectado · esperando mediciones"
                        bluetoothState is com.example.lession_equipo1.network.BluetoothState.Connecting ->
                            "Conectando con el sensor BLE"
                        bluetoothState is com.example.lession_equipo1.network.BluetoothState.Scanning ->
                            "Buscando sensores BLE"
                        bluetoothState is com.example.lession_equipo1.network.BluetoothState.Error ->
                            "Error BLE: ${(bluetoothState as com.example.lession_equipo1.network.BluetoothState.Error).message}"
                        isSimulating -> "Simulación activa"
                        else -> "Dispositivo BLE desconectado"
                    },
                    color = colors.text,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "EMG: ${emgSession.latestSample?.let { "%.1f".format(it.valueMv) } ?: "--"} mV · " +
                        "ADC: ${emgSession.latestSample?.adcRaw?.toString() ?: "--"} · " +
                        "Esfuerzo: ${emgSession.performancePercent?.let { "%.0f".format(it) } ?: "--"}%",
                    color = colors.subText,
                    fontSize = 13.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (isSimulating) viewModel.stopSimulation()
                            else viewModel.startSimulation(user?.uid.orEmpty())
                        },
                        enabled = bluetoothState is com.example.lession_equipo1.network.BluetoothState.Idle ||
                            bluetoothState is com.example.lession_equipo1.network.BluetoothState.Error,
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onBluetoothClick,
                        enabled = bluetoothState is com.example.lession_equipo1.network.BluetoothState.Connected &&
                            supportsEmg,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Calibrar")
                    }
                    Text(
                        "Reposo: ${emgSession.referenceRestMv?.let { "%.1f".format(it) } ?: "--"} · " +
                            "Umbral: ${emgSession.referenceMaxMv?.let { "%.1f".format(it) } ?: "--"} mV",
                        color = colors.subText,
                        modifier = Modifier.weight(1f).align(Alignment.CenterVertically)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Descargo / Aviso Médico Explicativo de los Algoritmos Heurísticos
        Surface(
            color = colors.cardBackground.copy(alpha = 0.6f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = colors.subText, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Aviso: El riesgo de lesión y la fatiga muscular son estimaciones algorítmicas heurísticas calculadas a partir de las métricas de los sensores. No constituyen un diagnóstico médico ni predicciones clínicamente validadas.",
                    color = colors.subText,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp)) // Padding for bottom bar
    }
}

@Composable
fun NewRiskGauge(risk: Float) {
    val strings = LocalStrings.current
    val colors = LocalAppColors.current
    val status = when {
        risk < 40 -> strings.optimal to Color.Green
        risk < 75 -> strings.caution to Color.Yellow
        else -> strings.risk to Color.Red
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(280.dp)) {
        Canvas(modifier = Modifier.size(220.dp)) {
            val strokeWidth = 16.dp.toPx()
            // Background arc
            drawArc(
                color = Color.White.copy(alpha = 0.1f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            // Multi-color arc segments
            // Green segment (0-40%)
            drawArc(
                color = Color.Green,
                startAngle = 135f,
                sweepAngle = 270f * 0.4f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            // Yellow segment (40-75%)
            drawArc(
                color = Color.Yellow,
                startAngle = 135f + 270f * 0.4f,
                sweepAngle = 270f * 0.35f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            // Red segment (75-100%)
            drawArc(
                color = Color.Red,
                startAngle = 135f + 270f * 0.75f,
                sweepAngle = 270f * 0.25f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            
            // Current value indicator (needle or small circle on the arc)
            // For now just the progress on top
            drawArc(
                color = Color.White,
                startAngle = 135f,
                sweepAngle = 270f * (risk / 100f),
                useCenter = false,
                style = Stroke(width = strokeWidth / 2, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(status.first, color = status.second, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("${risk.toInt()}%", color = colors.text, fontSize = 56.sp, fontWeight = FontWeight.Black)
            Text(strings.injuryRisk, color = colors.subText, fontSize = 16.sp)
        }
    }
}

@Composable
fun RecommendationMessage(risk: Float) {
    val colors = LocalAppColors.current
    val message = when {
        risk == 0f -> "Simulación detenida / Sin mediciones activas. Inicia la simulación o conecta un sensor real."
        risk < 40 -> "Tu estado es excelente. Puedes continuar con tu entrenamiento de alta intensidad."
        risk < 75 -> "Atención: Se detecta fatiga moderada. Considera reducir la carga de trabajo."
        else -> "¡PELIGRO! Riesgo de lesión inminente. Detén la actividad y descansa inmediatamente."
    }
    
    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            color = colors.text,
            fontSize = 14.sp,
            modifier = Modifier.padding(16.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun BiometricCard(label: String, value: String, unit: String, icon: ImageVector, iconColor: Color, subLabel: String) {
    val colors = LocalAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = iconColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(label, color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text(subLabel, color = colors.subText, fontSize = 12.sp)
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, color = colors.text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Text(unit, color = colors.subText, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
    }
}
