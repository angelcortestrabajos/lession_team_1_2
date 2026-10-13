package com.example.lession_equipo1.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lession_equipo1.data.local.entity.HistoryRecordEntity
import com.example.lession_equipo1.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

import com.example.lession_equipo1.ui.LocalAppColors
import com.example.lession_equipo1.ui.LocalStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    uid: String,
    viewModel: HistoryViewModel,
    onBack: () -> Unit,
) {
    val records by viewModel.localRecords.collectAsState()
    var selectedRecord by remember { mutableStateOf<HistoryRecordEntity?>(null) }
    val strings = LocalStrings.current
    val colors = LocalAppColors.current

    LaunchedEffect(uid) {
        viewModel.loadHistory(uid)
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Historial", color = colors.text) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back, tint = colors.text)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                ),
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (records.isEmpty()) {
                item {
                    Text(
                        "Aún no hay registros guardados.",
                        color = colors.subText,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                items(records, key = { it.id }) { record ->
                    HistoryItem(
                        record = record,
                        onClick = { selectedRecord = record },
                        onDelete = { viewModel.deleteRecord(record) }
                    )
                }
            }
        }
    }

    selectedRecord?.let { record ->
        HistoryRecordDetailsDialog(record, strings) { selectedRecord = null }
    }
}

@Composable
fun HistoryItem(
    record: HistoryRecordEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalAppColors.current
    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(record.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(dateStr, color = colors.subText, fontSize = 12.sp)
                Text("Riesgo: ${record.riskLevel.toInt()}% · Pulso: ${record.heartRate} bpm", color = colors.text)
                Text("Fatiga: ${record.fatigue.toInt()}% · ${record.riskState}", color = colors.subText)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar registro", tint = Color.Red)
            }
        }
    }
}

@Composable
private fun HistoryRecordDetailsDialog(
    record: HistoryRecordEntity,
    strings: com.example.lession_equipo1.ui.AppStrings,
    onDismiss: () -> Unit
) {
    val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(record.timestamp))
    val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(record.timestamp))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detalle del registro") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Fecha: $date")
                Text("Hora: $time")
                Text("${strings.heartRate}: ${record.heartRate} bpm")
                Text("Temperatura: %.1f °C".format(Locale.getDefault(), record.temperature))
                Text("Presión: ${record.bpSystolic}/${record.bpDiastolic} mmHg")
                Text("${strings.muscleFatigue}: ${record.fatigue.toInt()}%")
                Text("Máxima fatiga muscular: ${record.maxMuscle.toInt()}%")
                Text("Estabilidad: ${record.stability.toInt()}%")
                Text("${strings.risk}: ${record.riskLevel.toInt()}% (${record.riskState})")
                if (record.activityType.isNotBlank()) Text("Actividad: ${record.activityType}")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}
