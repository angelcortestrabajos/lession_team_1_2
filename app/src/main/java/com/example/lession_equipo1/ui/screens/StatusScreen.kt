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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lession_equipo1.BiometricViewModel
import com.example.lession_equipo1.model.MuscleData
import com.example.lession_equipo1.network.BluetoothState
import com.example.lession_equipo1.ui.LocalAppColors
import com.example.lession_equipo1.ui.LocalStrings

@Composable
fun StatusScreen(viewModel: BiometricViewModel) {
    val colors = LocalAppColors.current
    val strings = LocalStrings.current
    val muscles = viewModel.muscles
    val bluetoothState by viewModel.bluetoothState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Estado Corporal",
            color = colors.text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
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
                    BodySilhouette(muscles, isBack = false)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Posterior", color = colors.subText, fontSize = 12.sp)
                Box(modifier = Modifier.size(150.dp, 280.dp)) {
                    BodySilhouette(muscles, isBack = true)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Legend
        LegendPanel()
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Active Sensors Panel / Search Device
        ActiveSensorsPanel(
            state = bluetoothState,
            onSearchClick = { viewModel.startBluetoothScan() }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Biometric summaries
        Row(modifier = Modifier.fillMaxWidth()) {
            SmallBiometricCard(strings.heartRate, viewModel.heartRate.toInt().toString(), "BPM", Modifier.weight(1f), colors.accent)
            Spacer(modifier = Modifier.width(8.dp))
            SmallBiometricCard(strings.bodyTemp, "%.1f".format(viewModel.temperature), "°C", Modifier.weight(1f), Color.Yellow)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            SmallBiometricCard(strings.muscleFatigue, muscles.asSequence().map { it.fatigue }.average().toInt().toString(), "%", Modifier.weight(1f), Color.Green)
            Spacer(modifier = Modifier.width(8.dp))
            SmallBiometricCard(strings.bloodPressure, "${viewModel.bloodPressureHigh}/${viewModel.bloodPressureLow}", "mmHg", Modifier.weight(1f), Color.Red)
        }
        
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun BodySilhouette(muscles: List<MuscleData>, isBack: Boolean) {
    val colors = LocalAppColors.current
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val headRadius = 25f
        
        // Draw Head
        drawCircle(colors.text, headRadius, Offset(w/2, 40f), style = Stroke(2f))
        
        // Front view muscles
        if (!isBack) {
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
            // Back view muscles (mocked for now as we don't have separate back data yet)
            drawMusclePart(getFatigueColor(10f), Offset(w/2 - 35f, 75f), Size(70f, 90f)) // Back/Lats
            drawMusclePart(getFatigueColor(5f), Offset(w/2 - 35f, 175f), Size(32f, 90f)) // Hamstrings L
            drawMusclePart(getFatigueColor(5f), Offset(w/2 + 3f, 175f), Size(32f, 90f)) // Hamstrings R
        }
        
        // Outline (simplified)
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
fun ActiveSensorsPanel(state: BluetoothState, onSearchClick: () -> Unit) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Sensores Activos", color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))
        
        when (state) {
            is BluetoothState.Connected -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bluetooth, null, tint = colors.accent)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Lession-Band-X1", color = colors.text, fontWeight = FontWeight.Medium)
                            Text("Conectado - Batería 85%", color = colors.subText, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.CheckCircle, null, tint = Color.Green)
                    }
                }
            }
            is BluetoothState.Scanning -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = colors.accent, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Buscando dispositivos...", color = colors.text)
                    }
                }
            }
            is BluetoothState.DeviceFound -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BluetoothSearching, null, tint = colors.accent)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Dispositivo encontrado", color = colors.text)
                    }
                }
            }
            else -> {
                Button(
                    onClick = onSearchClick,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Search, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buscar dispositivo", fontWeight = FontWeight.Bold)
                }
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
