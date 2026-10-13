package com.example.lession_equipo1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lession_equipo1.ui.CustomTextField
import com.example.lession_equipo1.ui.LocalAppColors
import com.example.lession_equipo1.ui.LocalStrings
import com.example.lession_equipo1.viewmodel.AuthState
import com.example.lession_equipo1.viewmodel.AuthViewModel

@Composable
fun ForgotPasswordScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    val state by viewModel.state.collectAsState()
    val strings = LocalStrings.current
    val colors = LocalAppColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (colors.isDark) {
                        listOf(Color(0xFF2B1663), Color(0xFF120826))
                    } else {
                        listOf(colors.background, Color.White)
                    },
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.Start),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back, tint = colors.text)
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = if (strings.home == "Home") "Recover Password" else "Recuperar Contraseña",
                color = if (colors.isDark) colors.accent else colors.text,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (strings.home == "Home") "Enter your email and we'll send you a link to reset your password." else "Ingresa tu correo electrónico y te enviaremos un enlace para restablecer tu contraseña.",
                color = colors.subText,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            Spacer(modifier = Modifier.height(40.dp))

            CustomTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = strings.userName,
                backgroundColor = colors.cardBackground,
            )

            Spacer(modifier = Modifier.height(32.dp))

            when (state) {
                is AuthState.Loading -> {
                    CircularProgressIndicator(color = colors.accent)
                }
                is AuthState.PasswordResetSent -> {
                    Text(
                        text = if (strings.home == "Home") "Link sent successfully. Check your inbox." else "Enlace enviado con éxito. Revisa tu bandeja de entrada.",
                        color = Color.Green,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(0.6f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                    ) {
                        Text(if (strings.home == "Home") "Back to Login" else "Volver al Login", color = Color.Black)
                    }
                }
                else -> {
                    Button(
                        onClick = { viewModel.sendPasswordReset(email) },
                        enabled = email.contains("@") && email.contains("."),
                        modifier = Modifier.fillMaxWidth(0.6f).height(50.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                    ) {
                        Text(strings.sendLink, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (state is AuthState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    (state as AuthState.Error).message,
                    color = Color.Red,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
