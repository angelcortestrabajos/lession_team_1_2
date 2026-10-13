package com.example.lession_equipo1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lession_equipo1.model.IntensityLevel
import com.example.lession_equipo1.model.WorkoutHistory
import com.example.lession_equipo1.ui.LocalAppColors
import com.example.lession_equipo1.viewmodel.WorkoutHistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    workout: WorkoutHistory,
    viewModel: WorkoutHistoryViewModel,
    onBack: () -> Unit
) {
    val colors = LocalAppColors.current
    var notes by remember { mutableStateOf(workout.notes) }
    val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy - HH:mm", Locale.getDefault())
    val dateStr = sdf.format(Date(workout.startTime))

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Entrenamiento", color = colors.text) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colors.text)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.deleteWorkout(workout.userId, workout.id); onBack() }) {
                        Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.7f))
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(colors.accent.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when(workout.activityType) {
                            "Ciclismo" -> Icons.AutoMirrored.Filled.DirectionsBike
                            "Correr" -> Icons.AutoMirrored.Filled.DirectionsRun
                            else -> Icons.Default.FitnessCenter
                        },
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(workout.activityType, color = colors.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(dateStr, color = colors.subText, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Metrics Grid
            Text("Métricas Registradas", color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                DetailMetricCard("Duración", "${workout.duration / 60000}m", Icons.Default.Timer, Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                
                val intensityLabel = workout.intensity.getLabel()
                val intensityColor = workout.intensity.getColor()
                Card(
                    modifier = Modifier.height(80.dp).weight(1f),
                    colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, null, tint = intensityColor, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Intensidad", color = colors.subText, fontSize = 10.sp)
                            Text(intensityLabel, color = intensityColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                DetailMetricCard("Calorías", "${workout.calories} kcal", Icons.Default.LocalFireDepartment, Modifier.weight(1f))
                if (workout.distance > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    DetailMetricCard("Distancia", "${workout.distance} km", Icons.Default.Route, Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Biometría y Riesgo", color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    BiometricSummaryRow("Ritmo Cardíaco (Prom)", "${workout.hrAvg} BPM", Color.Red)
                    BiometricSummaryRow("Riesgo Promedio", "${workout.riskAvg.toInt()}%", colors.accent)
                    BiometricSummaryRow("Fatiga Final", "${workout.fatigueEnd.toInt()}%", Color.Yellow)
                }
            }

            if (workout.summary.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Recomendación", color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Surface(color = colors.surface, shape = RoundedCornerShape(12.dp)) {
                    Text(workout.summary, color = colors.text, modifier = Modifier.padding(16.dp), fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Notas Personales", color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = notes,
                onValueChange = { 
                    notes = it
                    viewModel.updateNotes(workout, it)
                },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.subText.copy(alpha = 0.3f),
                    focusedTextColor = colors.text,
                    unfocusedTextColor = colors.text
                ),
                placeholder = { Text("Escribe cómo te sentiste...", color = colors.subText) },
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Button(
                onClick = { /* Share summary */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = colors.cardBackground, contentColor = colors.text),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compartir Resumen")
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun DetailMetricCard(label: String, value: String, icon: ImageVector, modifier: Modifier) {
    val colors = LocalAppColors.current
    Card(
        modifier = modifier.height(80.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = colors.accent, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(label, color = colors.subText, fontSize = 10.sp)
                Text(value, color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun BiometricSummaryRow(label: String, value: String, dotColor: Color) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(dotColor, RoundedCornerShape(2.dp)))
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, color = colors.text)
        }
        Text(value, color = colors.text, fontWeight = FontWeight.Bold)
    }
}
