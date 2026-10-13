package com.example.lession_equipo1.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lession_equipo1.model.HistoryEntry
import com.example.lession_equipo1.ui.LocalAppColors
import com.example.lession_equipo1.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun FirebaseHistoryScreen(
    userId: String,
    viewModel: HistoryViewModel,
    onBack: () -> Unit
) {
    val colors = LocalAppColors.current
    val records by viewModel.firebaseRecords.collectAsState()
    var selectedRecord by remember { mutableStateOf<HistoryEntry?>(null) }

    LaunchedEffect(userId) {
        viewModel.loadFirebaseHistory(userId)
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Historial Firebase", color = colors.text) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = colors.text
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay registros guardados", color = colors.subText)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(records, key = { it.id }) { record ->
                    FirebaseHistoryItem(
                        record = record,
                        onClick = { selectedRecord = record },
                        onDelete = {
                            viewModel.deleteFirebaseRecord(userId, record.id)
                        }
                    )
                }
            }
        }
    }

    selectedRecord?.let { record ->
        FirebaseHistoryDetailsDialog(
            record = record,
            onDismiss = { selectedRecord = null }
        )
    }
}

@Composable
private fun FirebaseHistoryItem(
    record: HistoryEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalAppColors.current
    val date = SimpleDateFormat(
        "dd/MM/yyyy HH:mm:ss",
        Locale.getDefault()
    ).format(record.timestamp.toDate())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(date, color = colors.subText, fontWeight = FontWeight.Bold)
                Text("Pulso: ${record.heartRate} bpm", color = colors.text)
                Text("Fatiga: ${record.fatigue.toInt()}%", color = colors.text)
                Text("Riesgo: ${record.riskLevel.toInt()}% · ${record.riskState}", color = colors.subText)
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar registro",
                    tint = Color.Red
                )
            }
        }
    }
}

@Composable
private fun FirebaseHistoryDetailsDialog(
    record: HistoryEntry,
    onDismiss: () -> Unit
) {
    val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        .format(record.timestamp.toDate())
    val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        .format(record.timestamp.toDate())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detalle del registro") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Fecha: $date")
                Text("Hora: $time")
                Text("Frecuencia cardíaca: ${record.heartRate} bpm")
                Text("Temperatura: ${record.temperature} °C")
                Text("Presión: ${record.bpSystolic}/${record.bpDiastolic} mmHg")
                Text("Fatiga: ${record.fatigue.toInt()}%")
                Text("Máxima fatiga: ${record.maxMuscle.toInt()}%")
                Text("Estabilidad: ${record.stability.toInt()}%")
                Text("Riesgo: ${record.riskLevel.toInt()}%")
                Text("Estado: ${record.riskState}")
                if (record.activityType.isNotBlank()) {
                    Text("Actividad: ${record.activityType}")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}
