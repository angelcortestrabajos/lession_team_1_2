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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.lession_equipo1.BiometricViewModel
import com.example.lession_equipo1.ui.LocalAppColors
import com.example.lession_equipo1.ui.LocalStrings
import com.example.lession_equipo1.viewmodel.AuthViewModel

@Composable
fun HomeScreen(
    viewModel: BiometricViewModel,
    authViewModel: AuthViewModel,
    isDataMissing: Boolean,
    onBluetoothClick: () -> Unit,
    onCompleteProfileClick: () -> Unit
) {
    val user by authViewModel.currentUser.collectAsState()
    val riskLevel = viewModel.riskLevel
    val strings = LocalStrings.current
    val colors = LocalAppColors.current
    val context = LocalContext.current
    var showDialog by remember(isDataMissing) { mutableStateOf(isDataMissing) }
    val lastNotification by viewModel.notifications.collectAsState(initial = "")

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
                val bleState by viewModel.bluetoothState.collectAsState()
                val bleBattery by viewModel.parsedBatteryLevel.collectAsState()
                val isConnected = bleState is com.example.lession_equipo1.network.BluetoothState.Connected

                IconButton(onClick = onBluetoothClick) {
                    Icon(
                        Icons.Default.BluetoothConnected,
                        null,
                        tint = if (isConnected) Color(0xFF00FFCC) else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Icon(Icons.Default.BatteryFull, null, tint = colors.accent, modifier = Modifier.size(20.dp))
                val batteryText = if (isConnected && bleBattery != null) "$bleBattery%" else "--"
                Text(batteryText, color = colors.text, fontSize = 12.sp)
            }
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

        Spacer(modifier = Modifier.height(24.dp))
        
        NewRiskGauge(riskLevel)
        
        Spacer(modifier = Modifier.height(12.dp))
        
        RecommendationMessage(riskLevel)

        Spacer(modifier = Modifier.height(24.dp))
        Text(strings.bioSignals, color = colors.text, fontSize = 18.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
        Spacer(modifier = Modifier.height(16.dp))

        BiometricCard(strings.heartRate, viewModel.heartRate.toInt().toString(), "BPM", Icons.Default.Favorite, Color.Red, strings.currentFreq)
        BiometricCard(strings.bodyTemp, "%.1f".format(viewModel.temperature), "°C", Icons.Default.Thermostat, Color.Yellow, strings.currentTemp)
        BiometricCard(strings.bloodPressure, "${viewModel.bloodPressureHigh}/${viewModel.bloodPressureLow}", "mmHg", Icons.Default.WaterDrop, Color.Red, strings.currentPress)
        BiometricCard(strings.muscleFatigue, viewModel.muscles.asSequence().map { it.fatigue }.average().toInt().toString(), "%", Icons.Default.FitnessCenter, Color.Green, strings.currentCarga)

        val bluetoothState by viewModel.bluetoothState.collectAsState()
        val supportsEmg by viewModel.supportsEmgCommands.collectAsState()
        val emgSession = viewModel.emgSession
        val isSimulating = viewModel.isSimulating
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    when {
                        bluetoothState is com.example.lession_equipo1.network.BluetoothState.Connected ->
                            "Sensor EMG conectado"
                        bluetoothState is com.example.lession_equipo1.network.BluetoothState.Connecting ->
                            "Conectando con el sensor BLE"
                        bluetoothState is com.example.lession_equipo1.network.BluetoothState.Scanning ->
                            "Buscando sensores BLE"
                        bluetoothState is com.example.lession_equipo1.network.BluetoothState.Error ->
                            "Error BLE: ${(bluetoothState as com.example.lession_equipo1.network.BluetoothState.Error).message}"
                        isSimulating -> "Simulación activa"
                        else -> "Sensor desconectado · simulación detenida"
                    },
                    color = colors.text,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "EMG: ${emgSession.latestSample?.let { "%.1f".format(it.valueMv) } ?: "--"} mV · " +
                        "ADC: ${emgSession.latestSample?.adcRaw?.toString() ?: "--"} · " +
                        "Rendimiento: ${emgSession.performancePercent?.let { "%.0f".format(it) } ?: "--"}%",
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
                    viewModel.emgReferenceSaveError?.let { error ->
                        Text(error, color = Color.Red, fontSize = 12.sp)
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
                        onClick = {
                            if (!viewModel.setEmgMaximumReference()) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Se necesita una muestra EMG real válida para fijar el máximo.",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        enabled = bluetoothState is com.example.lession_equipo1.network.BluetoothState.Connected &&
                            supportsEmg,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Máximo")
                    }
                    Text(
                        "Referencia: ${emgSession.referenceMaxMv?.let { "%.1f".format(it) } ?: "--"} mV",
                        color = colors.subText,
                        modifier = Modifier.weight(1f).align(Alignment.CenterVertically)
                    )
                }
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
