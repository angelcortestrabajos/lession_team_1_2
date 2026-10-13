package com.example.lession_equipo1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lession_equipo1.model.GoalCategory
import com.example.lession_equipo1.model.TrainingGoal
import com.example.lession_equipo1.ui.CustomTextField
import com.example.lession_equipo1.ui.LocalAppColors
import com.example.lession_equipo1.ui.LocalStrings
import com.example.lession_equipo1.viewmodel.GoalViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditGoalScreen(
    userId: String,
    viewModel: GoalViewModel,
    onBack: () -> Unit
) {
    val colors = LocalAppColors.current
    val strings = LocalStrings.current

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(GoalCategory.DURATION) }
    var targetValue by remember { mutableStateOf("") }
    var deadlineDays by remember { mutableStateOf("7") }

    val categories = listOf(
        GoalCategory.WORKOUT_COUNT to "Entrenamientos",
        GoalCategory.DURATION to "Duración (min)",
        GoalCategory.CALORIES to "Calorías",
        GoalCategory.DISTANCE to "Distancia (km)",
        GoalCategory.HEART_RATE to "Frecuencia Cardíaca",
        GoalCategory.FATIGUE_REDUCTION to "Reducción Fatiga",
        GoalCategory.RISK_CONTROL to "Control de Riesgo"
    )

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Objetivo", color = colors.text) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colors.text)
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
            Text("Detalles del Objetivo", color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            CustomTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = "Ej: Completar 5 entrenamientos",
                backgroundColor = colors.cardBackground
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            CustomTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = "Descripción (opcional)",
                backgroundColor = colors.cardBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Categoría", color = colors.subText, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            
            var expanded by remember { mutableStateOf(false) }
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
                        Text(categories.find { it.first == category }?.second ?: "", color = colors.text)
                        Icon(Icons.Default.ArrowDropDown, null, tint = colors.accent)
                    }
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(colors.surface)
                ) {
                    categories.forEach { (cat, label) ->
                        DropdownMenuItem(
                            text = { Text(label, color = colors.text) },
                            onClick = {
                                category = cat
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Valor Meta", color = colors.subText, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    CustomTextField(
                        value = targetValue,
                        onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) targetValue = it },
                        placeholder = "Ej: 300",
                        backgroundColor = colors.cardBackground,
                        keyboardType = KeyboardType.Number
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Días Límite", color = colors.subText, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    CustomTextField(
                        value = deadlineDays,
                        onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) deadlineDays = it },
                        placeholder = "Ej: 7",
                        backgroundColor = colors.cardBackground,
                        keyboardType = KeyboardType.Number
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    val days = deadlineDays.toIntOrNull() ?: 7
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.DAY_OF_YEAR, days)
                    
                    val goal = TrainingGoal(
                        userId = userId,
                        title = title,
                        description = description,
                        category = category,
                        targetValue = targetValue.toDoubleOrNull() ?: 0.0,
                        unit = categories.find { it.first == category }?.second?.substringAfter("(")?.substringBefore(")") ?: "",
                        deadline = calendar.timeInMillis
                    )
                    viewModel.saveGoal(goal)
                    onBack()
                },
                enabled = title.isNotEmpty() && targetValue.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Crear Objetivo", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
