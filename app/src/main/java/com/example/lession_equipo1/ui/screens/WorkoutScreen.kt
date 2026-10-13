package com.example.lession_equipo1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lession_equipo1.BiometricViewModel
import com.example.lession_equipo1.model.GoalCategory
import com.example.lession_equipo1.model.IntensityLevel
import com.example.lession_equipo1.model.WorkoutHistory
import com.example.lession_equipo1.ui.LocalAppColors
import com.example.lession_equipo1.ui.LocalStrings
import com.example.lession_equipo1.viewmodel.GoalViewModel
import com.example.lession_equipo1.viewmodel.WorkoutHistoryViewModel

@Composable
fun WorkoutScreen(
    viewModel: BiometricViewModel,
    goalViewModel: GoalViewModel,
    historyViewModel: WorkoutHistoryViewModel,
    userId: String,
    onManageGoalsClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val strings = LocalStrings.current
    
    var selectedActivity by remember { mutableStateOf("Cardio") }
    var duration by remember { mutableStateOf("30 min") }
    var selectedIntensity by remember { mutableStateOf<IntensityLevel?>(null) }
    var isMonitoring by remember { mutableStateOf(false) }
    var startTime by remember { mutableLongStateOf(0L) }
    var showValidationError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.workout,
                color = colors.text,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            Row {
                IconButton(onClick = onHistoryClick) {
                    Icon(Icons.Default.History, null, tint = colors.accent)
                }
                TextButton(onClick = onManageGoalsClick) {
                    Icon(Icons.Default.Flag, null, tint = colors.accent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Objetivos", color = colors.accent)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Selectors
        WorkoutSelector("Actividad", selectedActivity, listOf("Cardio", "Fuerza", "Yoga", "HIIT", "Ciclismo", "Correr")) { selectedActivity = it }
        Spacer(modifier = Modifier.height(16.dp))
        WorkoutSelector("Duración Estimada", duration, listOf("15 min", "30 min", "45 min", "60 min")) { duration = it }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Intensity Selector
        Text("Intensidad (Obligatorio)", color = if (showValidationError && selectedIntensity == null) Color.Red else colors.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))
        
        IntensitySelector(
            selectedIntensity = selectedIntensity,
            onIntensitySelect = { 
                selectedIntensity = it
                showValidationError = false
            }
        )
        
        if (showValidationError && selectedIntensity == null) {
            Text(
                text = "Selecciona la intensidad del entrenamiento",
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Objetivos de Entrenamiento", color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(16.dp))
        
        ObjectiveToggle("Reducir riesgo de lesión", "Ajusta la intensidad si el riesgo supera el 60%", true)
        ObjectiveToggle("Aumentar carga muscular", "Mantener fatiga entre 40% y 70%", false)
        ObjectiveToggle("Controlar frecuencia cardíaca", "Zona 2 (120-140 BPM)", true)
        
        Spacer(modifier = Modifier.height(40.dp))
        
        if (!isMonitoring) {
            Button(
                onClick = { 
                    if (selectedIntensity != null) {
                        isMonitoring = true
                        startTime = System.currentTimeMillis()
                        viewModel.activeActivity = selectedActivity
                    } else {
                        showValidationError = true
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Iniciar monitoreo en tiempo real", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        } else {
            Button(
                onClick = { 
                    isMonitoring = false
                    viewModel.activeActivity = ""
                    val endTime = System.currentTimeMillis()
                    val actualDuration = endTime - startTime
                    
                    // Update goals
                    goalViewModel.updateProgress(userId, GoalCategory.WORKOUT_COUNT, 1.0)
                    val durationMin = actualDuration / 60000.0
                    goalViewModel.updateProgress(userId, GoalCategory.DURATION, durationMin)
                    
                    // Save history
                    val workout = WorkoutHistory(
                        userId = userId,
                        startTime = startTime,
                        endTime = endTime,
                        duration = actualDuration,
                        activityType = selectedActivity,
                        intensity = selectedIntensity ?: IntensityLevel.NONE,
                        calories = (durationMin * 8).toInt(), // Simplified calculation
                        hrAvg = viewModel.heartRate.toInt(),
                        riskAvg = viewModel.riskLevel,
                        fatigueEnd = viewModel.muscles.asSequence().map { it.fatigue }.average().toFloat(),
                        summary = "Buen trabajo. Tu riesgo promedio fue controlado."
                    )
                    historyViewModel.saveWorkout(workout)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Finalizar Entrenamiento", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("Monitoreo activo...", color = colors.accent, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun IntensitySelector(selectedIntensity: IntensityLevel?, onIntensitySelect: (IntensityLevel) -> Unit) {
    val colors = LocalAppColors.current
    val intensities = IntensityLevel.entries.filter { it != IntensityLevel.NONE }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        intensities.forEach { level ->
            val label = level.getLabel()
            val desc = level.getDescription()
            val isSelected = selectedIntensity == level
            val accentColor = level.getColor()
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isSelected) 2.dp else 0.dp,
                        color = if (isSelected) accentColor else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) accentColor.copy(alpha = 0.15f) else colors.cardBackground
                ),
                onClick = { onIntensitySelect(level) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(accentColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(label, color = if (isSelected) accentColor else colors.text, fontWeight = FontWeight.Bold)
                        Text(desc, color = colors.subText, fontSize = 11.sp)
                    }
                    if (isSelected) {
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.Check, null, tint = accentColor)
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutSelector(label: String, value: String, options: List<String>, onSelect: (String) -> Unit) {
    val colors = LocalAppColors.current
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(label, color = colors.subText, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Box {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                onClick = { expanded = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(value, color = colors.text, fontWeight = FontWeight.Medium)
                    Icon(Icons.Default.ArrowDropDown, null, tint = colors.accent)
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(colors.surface)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, color = colors.text) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ObjectiveToggle(title: String, description: String, initialValue: Boolean) {
    val colors = LocalAppColors.current
    var checked by remember { mutableStateOf(initialValue) }
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = colors.text, fontWeight = FontWeight.Medium)
                Text(description, color = colors.subText, fontSize = 12.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = { checked = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = colors.accent,
                    uncheckedThumbColor = colors.subText,
                    uncheckedTrackColor = colors.surface
                )
            )
        }
    }
}

@Composable
fun AlertConfigItem(label: String, initialValue: Boolean) {
    val colors = LocalAppColors.current
    var checked by remember { mutableStateOf(initialValue) }
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = colors.text)
        Checkbox(
            checked = checked,
            onCheckedChange = { checked = it },
            colors = CheckboxDefaults.colors(
                checkedColor = colors.accent,
                uncheckedColor = colors.subText,
                checkmarkColor = Color.Black
            )
        )
    }
}
