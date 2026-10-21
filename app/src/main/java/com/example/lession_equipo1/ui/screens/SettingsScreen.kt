package com.example.lession_equipo1.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lession_equipo1.BiometricViewModel
import com.example.lession_equipo1.network.BluetoothState
import com.example.lession_equipo1.ui.AppConfigViewModel
import com.example.lession_equipo1.ui.LocalAppColors
import com.example.lession_equipo1.ui.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AppConfigViewModel,
    biometricViewModel: BiometricViewModel? = null,
    userId: String = "",
    onBluetoothClick: () -> Unit = {},
    onBack: () -> Unit
) {
    val strings = stringResource()
    val colors = LocalAppColors.current
    val bluetoothStateState = biometricViewModel
        ?.bluetoothState
        ?.collectAsStateWithLifecycle()
    val bluetoothState = bluetoothStateState?.value ?: BluetoothState.Idle
    val connectionLabel = when (val state = bluetoothState) {
        is BluetoothState.Connected -> "Conectado"
        is BluetoothState.Connecting -> "Conectando"
        is BluetoothState.Scanning -> "Buscando dispositivos"
        is BluetoothState.Error -> "Error de conexión: ${state.message}"
        else -> "Desconectado"
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text(strings.settings, color = colors.text) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back, tint = colors.text)
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
            // Sección 1: Conexión BLE y Dispositivos
            Text(
                text = "Conectividad & Sensores",
                color = colors.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                shape = RoundedCornerShape(16.dp),
                onClick = onBluetoothClick
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFF00BFFF).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.BluetoothConnected, contentDescription = null, tint = Color(0xFF00BFFF))
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Conectar sensores BLE",
                            color = colors.text,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Buscar dispositivos BLE con perfiles de frecuencia cardíaca o EMG",
                            color = colors.subText,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Estado: $connectionLabel",
                            color = when (bluetoothState) {
                                is BluetoothState.Connected -> Color(0xFF2E7D32)
                                is BluetoothState.Connecting, is BluetoothState.Scanning -> Color(0xFFF57F17)
                                is BluetoothState.Error -> Color(0xFFC62828)
                                else -> colors.subText
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.subText)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Sección 2: Control de Simulación
            if (biometricViewModel != null) {
                val isSimulating = biometricViewModel.isSimulating
                val isBleConnected = bluetoothState is BluetoothState.Connected

                Text(
                    text = "Control de Simulación Biométrica",
                    color = colors.text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSimulating) Color(0xFF1B5E20).copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSimulating) Color.Green else Color.Red.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Sensors,
                                    contentDescription = null,
                                    tint = if (isSimulating) Color.Green else Color.Red
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (isSimulating) "Estado: Simulación Activa" else "Estado: Simulación Detenida",
                                    color = if (isSimulating) Color.Green else Color.Red,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isSimulating) {
                                    "La simulación genera mediciones de demostración mientras la app está activa."
                            } else {
                                "La simulación está completamente pausada. No se generan ni envían datos simulados."
                            },
                            color = colors.subText,
                            fontSize = 12.sp
                        )

                        if (isBleConnected) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Sensor BLE conectado: Las lecturas reales del sensor prevalecen automáticamente.",
                                color = colors.accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { biometricViewModel.stopSimulation() },
                                enabled = isSimulating,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Red.copy(alpha = 0.85f),
                                    contentColor = Color.White,
                                    disabledContainerColor = Color.Gray.copy(alpha = 0.2f),
                                    disabledContentColor = Color.Gray
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Detener", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            Button(
                                onClick = { biometricViewModel.startSimulation(userId) },
                                enabled = !isSimulating && (
                                    bluetoothState is BluetoothState.Idle ||
                                        bluetoothState is BluetoothState.Error
                                    ),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.accent,
                                    contentColor = Color.Black,
                                    disabledContainerColor = Color.Gray.copy(alpha = 0.2f),
                                    disabledContentColor = Color.Gray
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Iniciar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }

            // Sección 3: Idioma
            Text(
                text = strings.language,
                color = colors.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                shape = RoundedCornerShape(16.dp),
                onClick = { viewModel.toggleLanguage() }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = colors.accent)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = if (viewModel.isEnglish) "English" else "Español",
                            color = colors.text,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.changeLanguage,
                            color = colors.subText,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Sección 4: Tema
            Text(
                text = strings.theme,
                color = colors.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                shape = RoundedCornerShape(16.dp),
                onClick = { viewModel.toggleTheme() }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (viewModel.isDarkMode) Icons.Default.BrightnessHigh else Icons.Default.Brightness4,
                        contentDescription = null,
                        tint = colors.accent
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = if (viewModel.isDarkMode) "Dark Mode" else "Light Mode",
                            color = colors.text,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (viewModel.isDarkMode) strings.switchToLight else strings.switchToDark,
                            color = colors.subText,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
