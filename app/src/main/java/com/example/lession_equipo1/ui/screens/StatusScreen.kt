package com.example.lession_equipo1.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lession_equipo1.BiometricViewModel
import com.example.lession_equipo1.model.MuscleData
import com.example.lession_equipo1.model.PhysicalSensorInfo
import com.example.lession_equipo1.model.SensorConnectionState
import com.example.lession_equipo1.network.BluetoothState
import com.example.lession_equipo1.ui.AppColors
import com.example.lession_equipo1.ui.LocalAppColors
import com.example.lession_equipo1.ui.LocalStrings

@Composable
fun StatusScreen(viewModel: BiometricViewModel) {
    val colors = LocalAppColors.current
    val strings = LocalStrings.current
    val muscles = viewModel.muscles
    val isSimulating = viewModel.isSimulating

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Estado Corporal y Sensores",
            color = colors.text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = if (isSimulating) {
                "Mapa basado en datos de demostración; no representa lecturas físicas."
            } else {
                "Mapa neutral: no hay mediciones de fatiga muscular desde la fuente BLE actual."
            },
            color = colors.subText,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Body Map Front & Back
        Row(
            modifier = Modifier.fillMaxWidth().height(300.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Frontal", color = colors.subText, fontSize = 12.sp)
                Box(modifier = Modifier.size(150.dp, 280.dp)) {
                    BodySilhouette(muscles, isBack = false, hasMeasurements = viewModel.isSimulating)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Posterior", color = colors.subText, fontSize = 12.sp)
                Box(modifier = Modifier.size(150.dp, 280.dp)) {
                    BodySilhouette(muscles, isBack = true, hasMeasurements = viewModel.isSimulating)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Legend
        LegendPanel()
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Active Sensors Panel displaying real physical sensors
        ActiveSensorsPanel(
            viewModel = viewModel,
            onSearchClick = { viewModel.startBluetoothScan() }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Biometric summaries
        Row(modifier = Modifier.fillMaxWidth()) {
            SmallBiometricCard(
                strings.heartRate,
                if (viewModel.heartRate > 0.0) viewModel.heartRate.toInt().toString() else "Sin datos",
                if (viewModel.heartRate > 0.0) "BPM" else "",
                Modifier.weight(1f),
                colors.accent
            )
            Spacer(modifier = Modifier.width(8.dp))
            SmallBiometricCard(
                strings.bodyTemp,
                if (viewModel.temperature > 0.0) "%.1f".format(viewModel.temperature) else "Sin datos",
                if (viewModel.temperature > 0.0) "°C" else "",
                Modifier.weight(1f),
                Color.Yellow
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            SmallBiometricCard(
                strings.muscleFatigue,
                if (viewModel.isSimulating) muscles.asSequence().map { it.fatigue }.average().toInt().toString() else "Sin datos",
                if (viewModel.isSimulating) "%" else "",
                Modifier.weight(1f),
                Color.Green
            )
            Spacer(modifier = Modifier.width(8.dp))
            SmallBiometricCard(
                strings.bloodPressure,
                if (viewModel.bloodPressureHigh > 0 && viewModel.bloodPressureLow > 0) {
                    "${viewModel.bloodPressureHigh}/${viewModel.bloodPressureLow}"
                } else {
                    "Sin datos"
                },
                if (viewModel.bloodPressureHigh > 0 && viewModel.bloodPressureLow > 0) "mmHg" else "",
                Modifier.weight(1f),
                Color.Red
            )
        }
        
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun BodySilhouette(muscles: List<MuscleData>, isBack: Boolean, hasMeasurements: Boolean = true) {
    val colors = LocalAppColors.current
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val headRadius = 25f
        
        // Draw Head
        drawCircle(colors.text, headRadius, Offset(w/2, 40f), style = Stroke(2f))
        
        // Front view muscles
        if (!hasMeasurements) {
            drawRect(colors.subText.copy(alpha = 0.12f), Offset(w / 2 - 35f, 75f), Size(70f, 190f))
        } else if (!isBack) {
            val chestFatigue = muscles.find { it.id == "chest" }?.fatigue ?: 0f
            drawMusclePart(getFatigueColor(chestFatigue), Offset(w/2 - 35f, 75f), Size(70f, 90f))
            
            val bicepLFatigue = muscles.find { it.id == "biceps_l" }?.fatigue ?: 0f
            drawMusclePart(getFatigueColor(bicepLFatigue), Offset(w/2 - 75f, 85f), Size(30f, 80f))
            
            val bicepRFatigue = muscles.find { it.id == "biceps_r" }?.fatigue ?: 0f
            drawMusclePart(getFatigueColor(bicepRFatigue), Offset(w/2 + 45f, 85f), Size(30f, 80f))
            
            val thighLFatigue = muscles.find { it.id == "thigh_l" }?.fatigue ?: 0f
            drawMusclePart(getFatigueColor(thighLFatigue), Offset(w/2 - 35f, 175f), Size(32f, 90f))
            
            val thighRFatigue = muscles.find { it.id == "thigh_r" }?.fatigue ?: 0f
            drawMusclePart(getFatigueColor(thighRFatigue), Offset(w/2 + 3f, 175f), Size(32f, 90f))
        } else {
            // Back view muscles
            drawMusclePart(getFatigueColor(0f), Offset(w/2 - 35f, 75f), Size(70f, 90f))
            drawMusclePart(getFatigueColor(0f), Offset(w/2 - 35f, 175f), Size(32f, 90f))
            drawMusclePart(getFatigueColor(0f), Offset(w/2 + 3f, 175f), Size(32f, 90f))
        }
        
        // Outline
        drawRect(colors.text, Offset(w/2 - 35f, 75f), Size(70f, 90f), style = Stroke(2f))
    }
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMusclePart(color: Color, offset: Offset, size: Size) {
    drawRect(color, offset, size)
}

fun getFatigueColor(fatigue: Float): Color {
    return when {
        fatigue < 30f -> Color.Green.copy(alpha = 0.6f)
        fatigue < 70f -> Color.Yellow.copy(alpha = 0.6f)
        else -> Color.Red.copy(alpha = 0.6f)
    }
}

@Composable
fun LegendPanel() {
    val colors = LocalAppColors.current
    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendItem("Óptimo", Color.Green)
            LegendItem("Moderado", Color.Yellow)
            LegendItem("Fatiga alta", Color.Red)
            LegendItem("Sensor", Color.Cyan)
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = Color.White, fontSize = 10.sp)
    }
}

@Composable
fun ActiveSensorsPanel(
    viewModel: BiometricViewModel,
    onSearchClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val bluetoothState by viewModel.bluetoothState.collectAsStateWithLifecycle()
    val physicalSensors by viewModel.physicalSensors.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Estado Real de Sensores Físicos",
                color = colors.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            if (bluetoothState is BluetoothState.Connected) {
                TextButton(onClick = { viewModel.disconnectBluetooth() }) {
                    Text("Desconectar", color = Color.Red, fontSize = 12.sp)
                }
            } else if (bluetoothState is BluetoothState.Scanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = colors.accent,
                    strokeWidth = 2.dp
                )
            } else {
                TextButton(onClick = onSearchClick) {
                    Text("Buscar BLE", color = colors.accent, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Render each physical sensor in order
        physicalSensors.forEach { sensor ->
            PhysicalSensorItemCard(sensor = sensor, colors = colors)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (bluetoothState !is BluetoothState.Connected && bluetoothState !is BluetoothState.Scanning) {
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onSearchClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Buscar Dispositivo Principal (ESP32)", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PhysicalSensorItemCard(sensor: PhysicalSensorInfo, colors: AppColors) {
    val state = sensor.connectionState
    val isConnected = state == SensorConnectionState.CONNECTED
    val isMain = sensor.isMainDevice

    val sensorIcon = when (sensor.id) {
        "esp32" -> Icons.Default.Memory
        "max30102" -> Icons.Default.Favorite
        "mpu6050" -> Icons.Default.DirectionsRun
        "mlx90614" -> Icons.Default.Thermostat
        "max30205" -> Icons.Default.DeviceThermostat
        "myoware" -> Icons.Default.FitnessCenter
        else -> Icons.Default.Sensors
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isMain && isConnected) colors.accent.copy(alpha = 0.12f) else colors.cardBackground
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isMain) BorderStroke(1.dp, if (isConnected) colors.accent else colors.subText.copy(alpha = 0.3f)) else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = state.getColor().copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(sensorIcon, contentDescription = null, tint = state.getColor(), modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        sensor.name,
                        color = colors.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (isMain) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = colors.accent,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "MAESTRO",
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    sensor.description,
                    color = colors.subText,
                    fontSize = 11.sp
                )
                if (sensor.lastReading != null) {
                    Text(
                        "Lectura real: ${sensor.lastReading}",
                        color = colors.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // State Badge
            Surface(
                color = state.getColor().copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, state.getColor())
            ) {
                Text(
                    text = state.getLabel(),
                    color = state.getColor(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
fun SmallBiometricCard(label: String, value: String, unit: String, modifier: Modifier, accentColor: Color) {
    val colors = LocalAppColors.current
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, color = colors.subText, fontSize = 12.sp)
            Spacer(modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, color = colors.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Text(unit, color = colors.subText, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { 0.5f },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = accentColor,
                trackColor = accentColor.copy(alpha = 0.2f)
            )
        }
    }
}
