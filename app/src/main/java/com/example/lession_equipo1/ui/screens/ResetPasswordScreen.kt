package com.example.lession_equipo1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun ResetPasswordScreen(
    oobCode: String,
    viewModel: AuthViewModel,
    onSuccess: () -> Unit,
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val state by viewModel.state.collectAsState()
    val strings = LocalStrings.current
    val colors = LocalAppColors.current

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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            Text(
                text = strings.resetPassword,
                color = if (colors.isDark) colors.accent else colors.text,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (strings.home == "Home") "Set your new access password." else "Establece tu nueva contraseña de acceso.",
                color = colors.subText,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            CustomTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                placeholder = strings.password,
                backgroundColor = colors.cardBackground,
                isPassword = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            CustomTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                placeholder = if (strings.home == "Home") "Confirm Password" else "Confirmar Contraseña",
                backgroundColor = colors.cardBackground,
                isPassword = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (state is AuthState.Loading) {
                CircularProgressIndicator(color = colors.accent)
            } else {
                Button(
                    onClick = { 
                        viewModel.confirmReset(oobCode, newPassword)
                    },
                    enabled = (newPassword.length >= 6) && (newPassword == confirmPassword),
                    modifier = Modifier.fillMaxWidth(0.6f).height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) {
                    Text(
                        text = if (strings.home == "Home") "Change Password" else "Cambiar Contraseña",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && newPassword != confirmPassword) {
                Text(
                    text = if (strings.home == "Home") "Passwords do not match" else "Las contraseñas no coinciden",
                    color = Color.Red,
                    fontSize = 12.sp
                )
            }

            if (state is AuthState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    (state as AuthState.Error).message,
                    color = Color.Red,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
