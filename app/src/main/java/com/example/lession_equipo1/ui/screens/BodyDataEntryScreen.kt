package com.example.lession_equipo1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lession_equipo1.ui.CustomTextField
import com.example.lession_equipo1.ui.LocalAppColors
import com.example.lession_equipo1.ui.LocalStrings
import com.example.lession_equipo1.viewmodel.AuthState
import com.example.lession_equipo1.viewmodel.AuthViewModel

@Composable
fun BodyDataEntryScreen(
    viewModel: AuthViewModel,
    onSuccess: () -> Unit
) {
    val strings = LocalStrings.current
    val colors = LocalAppColors.current
    val state by viewModel.state.collectAsState()

    var age by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("M") }
    var sport by remember { mutableStateOf("") }
    
    var showError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.resetState()
    }

    LaunchedEffect(state) {
        if (state is AuthState.Success) {
            onSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (colors.isDark) {
                        listOf(Color(0xFF2B1663), Color(0xFF120826))
                    } else {
                        listOf(colors.background, Color.White)
                    }
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = if (strings.home == "Home") "Corporal Data" else "Datos Corporales",
                color = if (colors.isDark) colors.accent else colors.text,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Por favor, ingresa tus parámetros físicos. Son obligatorios para el cálculo de salud.",
                color = colors.subText,
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (showError) {
                Text(
                    text = "Todos los campos son obligatorios",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            CustomTextField(
                value = age,
                onValueChange = { if (it.all { c -> c.isDigit() }) age = it },
                placeholder = "Edad",
                backgroundColor = colors.cardBackground,
                keyboardType = KeyboardType.Number
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                CustomTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    placeholder = "Peso (kg)",
                    backgroundColor = colors.cardBackground,
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Decimal
                )
                Spacer(modifier = Modifier.width(8.dp))
                CustomTextField(
                    value = height,
                    onValueChange = { height = it },
                    placeholder = "Altura (cm)",
                    backgroundColor = colors.cardBackground,
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Decimal
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Género", color = colors.subText, fontSize = 14.sp)
                    Row {
                        FilterChip(
                            selected = gender == "M",
                            onClick = { gender = "M" },
                            label = { Text("M") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = gender == "F",
                            onClick = { gender = "F" },
                            label = { Text("F") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            CustomTextField(
                value = sport,
                onValueChange = { sport = it },
                placeholder = "Deporte principal",
                backgroundColor = colors.cardBackground
            )

            Spacer(modifier = Modifier.height(40.dp))

            if (state is AuthState.Loading) {
                CircularProgressIndicator(color = colors.accent)
            } else {
                Button(
                    onClick = {
                        val parsedAge = age.toIntOrNull()
                        val parsedWeight = weight.toDoubleOrNull()
                        val parsedHeight = height.toDoubleOrNull()

                        if (parsedAge != null && parsedAge in 1..120
                            && parsedWeight != null && parsedWeight in 1.0..500.0
                            && parsedHeight != null && parsedHeight in 1.0..300.0
                            && sport.isNotBlank()
                        ) {
                            showError = false
                            viewModel.updateBodyData(
                                age = parsedAge,
                                weight = parsedWeight,
                                height = parsedHeight,
                                gender = gender,
                                sport = sport.trim()
                            )
                        } else {
                            showError = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.8f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) {
                    Text(
                        text = "Guardar y Continuar",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (state is AuthState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = (state as AuthState.Error).message,
                    color = Color.Red,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
