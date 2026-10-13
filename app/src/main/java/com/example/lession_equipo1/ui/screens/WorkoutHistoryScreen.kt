package com.example.lession_equipo1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
fun WorkoutHistoryScreen(
    userId: String,
    viewModel: WorkoutHistoryViewModel,
    onWorkoutClick: (WorkoutHistory) -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalAppColors.current
    val history by viewModel.history.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Todos") }
    var selectedIntensityFilter by remember { mutableStateOf<IntensityLevel?>(null) }
    var sortBy by remember { mutableStateOf("Fecha") }

    LaunchedEffect(userId) {
        viewModel.loadHistory(userId)
    }

    val filteredHistory = remember(history, searchQuery, selectedFilter, selectedIntensityFilter, sortBy) {
        val now = System.currentTimeMillis()
        val dayMillis = 24 * 60 * 60 * 1000L
        val weekMillis = 7 * dayMillis
        val monthMillis = 30 * dayMillis

        history.filter { workout ->
            val matchesSearch = workout.activityType.contains(searchQuery, ignoreCase = true) ||
                                workout.notes.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "Favoritos" -> workout.isFavorite
                "Hoy" -> now - workout.startTime < dayMillis
                "Semana" -> now - workout.startTime < weekMillis
                "Mes" -> now - workout.startTime < monthMillis
                "Ciclismo", "Correr", "Gimnasio" -> workout.activityType == selectedFilter
                else -> true
            }
            val matchesIntensity = selectedIntensityFilter == null || workout.intensity == selectedIntensityFilter
            matchesSearch && matchesFilter && matchesIntensity
        }.sortedByDescending { 
            when (sortBy) {
                "Duración" -> it.duration.toFloat()
                "Calorías" -> it.calories.toFloat()
                "Riesgo" -> it.riskAvg
                else -> it.startTime.toFloat()
            }
        }
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Historial de Entrenamientos", color = colors.text) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colors.text)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Search and Filters
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar entrenamiento...", color = colors.subText) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = colors.accent) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.subText.copy(alpha = 0.3f),
                        focusedTextColor = colors.text,
                        unfocusedTextColor = colors.text
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                ScrollableTabRow(
                    selectedTabIndex = 0,
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = {},
                    edgePadding = 0.dp
                ) {
                    listOf("Todos", "Favoritos", "Hoy", "Semana", "Mes", "Ciclismo", "Correr").forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) },
                            modifier = Modifier.padding(end = 8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.accent,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Intensidad:", color = colors.subText, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    ScrollableTabRow(
                        selectedTabIndex = 0,
                        containerColor = Color.Transparent,
                        divider = {},
                        indicator = {},
                        edgePadding = 0.dp
                    ) {
                        IntensityLevel.entries.filter { it != IntensityLevel.NONE }.forEach { level ->
                            FilterChip(
                                selected = selectedIntensityFilter == level,
                                onClick = { selectedIntensityFilter = if (selectedIntensityFilter == level) null else level },
                                label = { Text(level.getLabel()) },
                                modifier = Modifier.padding(end = 8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = level.getColor(),
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Ordenar por:", color = colors.subText, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    listOf("Fecha", "Duración", "Calorías", "Riesgo").forEach { sort ->
                        TextButton(
                            onClick = { sortBy = sort },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = sort,
                                color = if (sortBy == sort) colors.accent else colors.subText,
                                fontSize = 12.sp,
                                fontWeight = if (sortBy == sort) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.accent)
                }
            } else if (filteredHistory.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.History, null, tint = colors.subText, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No se encontraron entrenamientos", color = colors.subText)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredHistory, key = { it.id }) { workout ->
                        WorkoutHistoryCard(
                            workout = workout,
                            onClick = { onWorkoutClick(workout) },
                            onFavoriteToggle = { viewModel.toggleFavorite(workout) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHistoryCard(
    workout: WorkoutHistory,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    val colors = LocalAppColors.current
    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    val dateStr = sdf.format(Date(workout.startTime))
    val durationMin = workout.duration / 60000
    
    val riskColor = when {
        workout.riskAvg < 40 -> Color.Green
        workout.riskAvg < 75 -> Color.Yellow
        else -> Color.Red
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(colors.accent.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when(workout.activityType) {
                        "Ciclismo" -> Icons.AutoMirrored.Filled.DirectionsBike
                        "Correr" -> Icons.AutoMirrored.Filled.DirectionsRun
                        "Caminar" -> Icons.AutoMirrored.Filled.DirectionsWalk
                        else -> Icons.Default.FitnessCenter
                    },
                    contentDescription = null,
                    tint = colors.accent
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(workout.activityType, color = colors.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(dateStr, color = colors.subText, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(6.dp).background(riskColor, CircleShape))
                    if (workout.intensity != IntensityLevel.NONE) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = workout.intensity.getLabel(),
                            color = workout.intensity.getColor(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text("${durationMin}m", color = colors.text, fontWeight = FontWeight.Bold)
                Text("${workout.calories} kcal", color = colors.subText, fontSize = 12.sp)
            }
            
            IconButton(onClick = onFavoriteToggle) {
                Icon(
                    imageVector = if (workout.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (workout.isFavorite) Color.Red else colors.subText
                )
            }
        }
    }
}
