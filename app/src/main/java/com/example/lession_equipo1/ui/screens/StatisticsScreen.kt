package com.example.lession_equipo1.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lession_equipo1.BiometricViewModel
import com.example.lession_equipo1.model.HistoryEntry
import com.example.lession_equipo1.ui.LocalAppColors
import com.example.lession_equipo1.ui.LocalStrings
import com.example.lession_equipo1.viewmodel.StatisticsPeriod
import com.example.lession_equipo1.viewmodel.StatisticsViewModel
import com.example.lession_equipo1.viewmodel.WorkoutHistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatisticsScreen(
    biometricViewModel: BiometricViewModel,
    historyViewModel: WorkoutHistoryViewModel,
    statsViewModel: StatisticsViewModel,
    userId: String
) {
    val colors = LocalAppColors.current
    val strings = LocalStrings.current
    val state by statsViewModel.uiState.collectAsState()
    
    val risk = biometricViewModel.riskLevel
    val stateColor = when {
        risk < 40 -> Color.Green
        risk < 75 -> Color.Yellow
        else -> Color.Red
    }

    LaunchedEffect(userId) {
        statsViewModel.loadData(userId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = strings.stats,
            color = colors.text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Time Filters
        PeriodSelector(state.period) { statsViewModel.setPeriod(it, userId) }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Date Navigation for Daily View
        if (state.period is StatisticsPeriod.Day) {
            val date = (state.period as StatisticsPeriod.Day).time
            val sdf = SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault())
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { statsViewModel.navigateDay(-1, userId) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colors.accent)
                }
                Text(
                    text = sdf.format(Date(date)),
                    color = colors.text,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = { statsViewModel.navigateDay(1, userId) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = colors.accent)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Dynamic Summary Banner
        SummaryBanner(state.mostFrequentState, state.trendMessage)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Summary Cards
        Row(modifier = Modifier.fillMaxWidth()) {
            SummaryCard("Riesgo Promedio", "${state.avgRisk}%", getStatusColor(state.avgRisk.toFloat()), Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            SummaryCard("FC Promedio", "${state.avgHeartRate}", Color.Red, Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            SummaryCard("Fatiga Media", "${state.avgFatigue}%", Color.Yellow, Modifier.weight(1f))
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        if (state.isLoading) {
            CircularProgressIndicator(color = colors.accent)
        } else if (state.entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No hay mediciones para este periodo", color = colors.subText)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { /* Nav to start monitoring */ }, colors = ButtonDefaults.buttonColors(containerColor = colors.accent)) {
                        Text("Iniciar Monitoreo", color = Color.Black)
                    }
                }
            }
        } else {
            // Charts Section
            var selectedMetric by remember { mutableStateOf("Riesgo") }
            MetricToggle(selectedMetric) { selectedMetric = it }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val chartData = remember(state.entries, selectedMetric) {
                when (selectedMetric) {
                    "Riesgo" -> state.entries.map { it.riskLevel.toFloat() }
                    "Pulso" -> state.entries.map { it.heartRate.toFloat() }
                    "Temp" -> state.entries.map { it.temperature.toFloat() }
                    else -> state.entries.map { it.fatigue.toFloat() }
                }
            }
            
            val chartColor = when (selectedMetric) {
                "Riesgo" -> stateColor
                "Pulso" -> Color.Red
                "Temp" -> Color.Yellow
                else -> Color.Green
            }

            ChartCard(selectedMetric, chartData, chartColor)
            
            Spacer(modifier = Modifier.height(24.dp))

            // Objectives Summary
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Cumplimiento de Objetivos", color = colors.text, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    ObjectiveItem("Control de Riesgo", if (state.avgRisk < 40) 1.0f else 0.5f, if (state.avgRisk < 40) Color.Green else Color.Red)
                    ObjectiveItem("Frecuencia Cardíaca", 0.85f, Color.Red)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Historical List
            Text("Historial de Datos", color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
            
            state.entries.reversed().take(20).forEach { entry ->
                HistoricalEntryItem(entry)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun PeriodSelector(current: StatisticsPeriod, onSelect: (StatisticsPeriod) -> Unit) {
    val colors = LocalAppColors.current
    val periods = listOf(
        "Hoy" to StatisticsPeriod.Today,
        "Ayer" to StatisticsPeriod.Yesterday,
        "7 Días" to StatisticsPeriod.Last7Days,
        "Mes" to StatisticsPeriod.CurrentMonth
    )
    
    ScrollableTabRow(
        selectedTabIndex = 0,
        containerColor = Color.Transparent,
        divider = {},
        indicator = {},
        edgePadding = 0.dp
    ) {
        periods.forEach { (label, period) ->
            FilterChip(
                selected = current == period,
                onClick = { onSelect(period) },
                label = { Text(label) },
                modifier = Modifier.padding(end = 8.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colors.accent,
                    selectedLabelColor = Color.Black
                )
            )
        }
        // Custom range trigger
        FilterChip(
            selected = current is StatisticsPeriod.Custom,
            onClick = { /* Show date picker */ },
            label = { Icon(Icons.Default.DateRange, null, modifier = Modifier.size(16.dp)) },
            colors = FilterChipDefaults.filterChipColors(containerColor = colors.cardBackground)
        )
    }
}

@Composable
fun MetricToggle(selected: String, onSelect: (String) -> Unit) {
    val colors = LocalAppColors.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Riesgo", "Pulso", "Temp", "Fatiga").forEach { metric ->
            FilterChip(
                selected = selected == metric,
                onClick = { onSelect(metric) },
                label = { Text(metric, fontSize = 10.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colors.surface,
                    selectedLabelColor = colors.accent
                )
            )
        }
    }
}

@Composable
fun SummaryBanner(state: String, trend: String) {
    val colors = LocalAppColors.current
    val color = when(state) {
        "Óptimo" -> Color.Green
        "Precaución" -> Color.Yellow
        else -> Color.Red
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Analytics, null, tint = color)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Estado predominante: $state", color = colors.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(trend, color = colors.subText, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun HistoricalEntryItem(entry: HistoryEntry) {
    val colors = LocalAppColors.current
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeStr = sdf.format(entry.timestamp.toDate())
    val color = getStatusColor(entry.riskLevel.toFloat())
    
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                Spacer(modifier = Modifier.width(12.dp))
                Text(timeStr, color = colors.text, fontWeight = FontWeight.Bold)
                if (entry.activityType.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.background(colors.accent.copy(alpha = 0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp)) {
                        Text(entry.activityType, color = colors.accent, fontSize = 10.sp)
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Text("${entry.heartRate} BPM", color = colors.subText, fontSize = 12.sp)
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = colors.subText)
            }
            
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = colors.subText.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MetricDetail("Riesgo", "${entry.riskLevel.toInt()}%")
                        MetricDetail("Fatiga", "${entry.fatigue.toInt()}%")
                        MetricDetail("Temp", "${entry.temperature}°C")
                        MetricDetail("Presión", "${entry.bpSystolic}/${entry.bpDiastolic}")
                    }
                }
            }
        }
    }
}

@Composable
fun MetricDetail(label: String, value: String) {
    val colors = LocalAppColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = colors.subText, fontSize = 10.sp)
        Text(value, color = colors.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

fun getStatusColor(risk: Float): Color = when {
    risk < 40 -> Color.Green
    risk < 75 -> Color.Yellow
    else -> Color.Red
}

@Composable
fun SummaryCard(label: String, value: String, accentColor: Color, modifier: Modifier) {
    val colors = LocalAppColors.current
    Card(
        modifier = modifier.height(90.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(label, color = colors.subText, fontSize = 10.sp)
            Spacer(modifier = Modifier.weight(1f))
            Text(value, color = colors.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(accentColor.copy(alpha = 0.5f)))
        }
    }
}

@Composable
fun ChartCard(title: String, data: List<Float>, color: Color) {
    val colors = LocalAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth().height(220.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxSize()) {
                SimpleLineChart(data, color)
            }
        }
    }
}

@Composable
fun SimpleLineChart(data: List<Float>, color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (data.size < 2) return@Canvas
        val maxVal = data.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        val minVal = data.minOrNull() ?: 0f
        val range = (maxVal - minVal).coerceAtLeast(1f)
        
        val stepX = size.width / (data.size - 1)
        val path = Path()
        
        data.forEachIndexed { i, value ->
            val x = i * stepX
            val y = size.height - ((value - minVal) / range * size.height)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        
        drawPath(path, color, style = Stroke(width = 3.dp.toPx()))
        
        // Fill area under path
        val fillPath = Path().apply {
            addPath(path)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(fillPath, color.copy(alpha = 0.1f))
        
        // Draw points
        data.forEachIndexed { i, value ->
            val x = i * stepX
            val y = size.height - ((value - minVal) / range * size.height)
            drawCircle(color, 4.dp.toPx(), Offset(x, y))
            drawCircle(Color.White, 2.dp.toPx(), Offset(x, y))
        }
    }
}

@Composable
fun ObjectiveItem(label: String, progress: Float, color: Color) {
    val appColors = LocalAppColors.current
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = appColors.subText, fontSize = 12.sp)
            Text("${(progress * 100).toInt()}%", color = appColors.text, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            color = color,
            trackColor = color.copy(alpha = 0.1f)
        )
    }
}
