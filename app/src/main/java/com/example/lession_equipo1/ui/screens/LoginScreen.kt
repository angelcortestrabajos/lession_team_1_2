package com.example.lession_equipo1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lession_equipo1.ui.CustomTextField
import com.example.lession_equipo1.ui.LessionLogoGraphic
import com.example.lession_equipo1.ui.LocalAppColors
import com.example.lession_equipo1.ui.LocalStrings
import com.example.lession_equipo1.viewmodel.AuthState
import com.example.lession_equipo1.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onForgotPasswordClick: () -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    val strings = LocalStrings.current
    val colors = LocalAppColors.current

    var isRegister by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    LaunchedEffect(user) {
        if (user != null) {
            onLoginSuccess()
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
                    },
                ),
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            LessionLogoGraphic()

            Spacer(modifier = Modifier.height(30.dp))

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .border(4.dp, colors.accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.fillMaxSize(0.7f)
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (colors.isDark) Color(0x66000000) else Color.White.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (colors.isDark) 0.dp else 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (isRegister) strings.register else strings.login,
                        color = if (colors.isDark) colors.accent else colors.text,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (isRegister) {
                        CustomTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = strings.name,
                            backgroundColor = colors.cardBackground
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    CustomTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = strings.userName,
                        backgroundColor = colors.cardBackground
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CustomTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = strings.password,
                        backgroundColor = colors.cardBackground,
                        isPassword = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (state is AuthState.Loading) {
                        CircularProgressIndicator(color = colors.accent)
                    } else if (state is AuthState.VerificationEmailSent) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f)),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(strings.verifyTitle, color = Color.Green, fontWeight = FontWeight.Bold)
                                Text(strings.verifyEmail, color = colors.text, fontSize = 12.sp, textAlign = TextAlign.Center)
                                Button(
                                    onClick = { viewModel.resetState() },
                                    modifier = Modifier.padding(top = 8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                                ) {
                                    Text("OK", color = Color.Black)
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                if (isRegister) {
                                    viewModel.registerWithEmail(email, password, name)
                                } else {
                                    viewModel.loginWithEmail(email, password)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(50.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                        ) {
                            Text(
                                if (isRegister) strings.register else strings.login,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!isRegister) {
                        TextButton(onClick = onForgotPasswordClick) {
                            Text(strings.forgotPassword, color = colors.text)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    val switchText = if (isRegister) {
                        if (strings.home == "Home") "Already have an account? Login" else "¿Ya tienes cuenta? Inicia sesión"
                    } else {
                        if (strings.home == "Home") "Don't have an account? Register" else "¿No tienes cuenta? Regístrate"
                    }
                    
                    TextButton(
                        onClick = {
                            isRegister = !isRegister
                            viewModel.resetState()
                        },
                    ) {
                        Text(switchText, color = colors.text)
                    }
                    
                    IconButton(onClick = { viewModel.signIn(context) }) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_add),
                            contentDescription = "Google",
                            tint = colors.text
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { viewModel.loginAnonymously() },
                        modifier = Modifier.fillMaxWidth(0.6f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (colors.isDark) colors.accent else colors.text),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (colors.isDark) colors.accent else colors.text)
                    ) {
                        Text("Acceso Rápido (Invitado)", fontSize = 12.sp)
                    }
                }
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
