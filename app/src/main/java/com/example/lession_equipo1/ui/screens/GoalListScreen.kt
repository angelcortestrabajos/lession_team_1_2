package com.example.lession_equipo1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lession_equipo1.model.GoalCategory
import com.example.lession_equipo1.model.GoalStatus
import com.example.lession_equipo1.model.TrainingGoal
import com.example.lession_equipo1.ui.LocalAppColors
import com.example.lession_equipo1.ui.LocalStrings
import com.example.lession_equipo1.viewmodel.GoalViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalListScreen(
    userId: String,
    viewModel: GoalViewModel,
    onAddGoalClick: () -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalAppColors.current
    val strings = LocalStrings.current
    val goals by viewModel.goals.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var selectedFilter by remember { mutableStateOf("Todos") }

    LaunchedEffect(userId) {
        viewModel.loadGoals(userId)
    }

    val filteredGoals = remember(goals, selectedFilter) {
        when (selectedFilter) {
            "Activos" -> goals.filter { it.status == GoalStatus.ACTIVE }
            "Completados" -> goals.filter { it.status == GoalStatus.COMPLETED }
            "Vencidos" -> goals.filter { it.status == GoalStatus.OVERDUE }
            else -> goals
        }
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Mis Objetivos", color = colors.text) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colors.text)
                    }
                },
                actions = {
                    IconButton(onClick = onAddGoalClick) {
                        Icon(Icons.Default.Add, "Agregar", tint = colors.accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Filters
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Todos", "Activos", "Completados", "Vencidos").forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.accent,
                            selectedLabelColor = Color.Black,
                            containerColor = colors.cardBackground,
                            labelColor = colors.text
                        )
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.accent)
                }
            } else if (filteredGoals.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Flag, null, tint = colors.subText, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No hay objetivos en esta categoría", color = colors.subText)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onAddGoalClick,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                        ) {
                            Text("Crear mi primer objetivo", color = Color.Black)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredGoals, key = { it.id }) { goal ->
                        GoalCard(
                            goal = goal,
                            onCompleteClick = { viewModel.markAsCompleted(goal) },
                            onDeleteClick = { viewModel.deleteGoal(userId, goal.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GoalCard(
    goal: TrainingGoal,
    onCompleteClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val progress = (goal.currentValue / goal.targetValue).coerceIn(0.0, 1.0).toFloat()
    val statusColor = when (goal.status) {
        GoalStatus.COMPLETED -> Color.Green
        GoalStatus.OVERDUE -> Color.Red
        else -> colors.accent
    }

    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val deadlineStr = sdf.format(Date(goal.deadline))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(goal.title, color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(goal.category.name.replace("_", " "), color = colors.subText, fontSize = 12.sp)
                }
                
                Row {
                    if (goal.status == GoalStatus.ACTIVE) {
                        IconButton(onClick = onCompleteClick) {
                            Icon(Icons.Default.CheckCircle, "Completar", tint = Color.Green)
                        }
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Default.Delete, "Eliminar", tint = Color.Red.copy(alpha = 0.6f))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${goal.currentValue.toInt()} / ${goal.targetValue.toInt()} ${goal.unit}", color = colors.text, fontSize = 14.sp)
                Text("${(progress * 100).toInt()}%", color = statusColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = statusColor,
                trackColor = statusColor.copy(alpha = 0.1f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Event, null, tint = colors.subText, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Límite: $deadlineStr", color = colors.subText, fontSize = 12.sp)
                
                if (goal.status == GoalStatus.COMPLETED) {
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .background(Color.Green.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("COMPLETADO", color = Color.Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
