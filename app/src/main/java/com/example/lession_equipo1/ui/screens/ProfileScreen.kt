package com.example.lession_equipo1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.lession_equipo1.BiometricViewModel
import com.example.lession_equipo1.model.AppUser
import com.example.lession_equipo1.ui.LocalAppColors
import com.example.lession_equipo1.ui.LocalStrings
import com.example.lession_equipo1.viewmodel.AuthState
import com.example.lession_equipo1.viewmodel.AuthViewModel

@Composable
fun ProfileScreen(
    viewModel: BiometricViewModel,
    authViewModel: AuthViewModel,
    isDataMissing: Boolean,
    onSettingsClick: () -> Unit,
    onUpdateBodyDataClick: () -> Unit
) {
    val user by authViewModel.currentUser.collectAsState()
    val strings = LocalStrings.current
    val colors = LocalAppColors.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(strings.deleteConfirmTitle) },
            text = { Text(strings.deleteConfirmDesc) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        authViewModel.deleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(strings.deleteButton)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    user?.let { profileUser ->
        if (showEditProfileDialog) {
            EditProfileDialog(
                user = profileUser,
                authViewModel = authViewModel,
                onDismiss = {
                    showEditProfileDialog = false
                    authViewModel.resetProfileUpdateState()
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, null, tint = colors.text)
            }
            Text(
                strings.myProfile,
                color = colors.text,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = {
                    authViewModel.resetProfileUpdateState()
                    showEditProfileDialog = true
                }
            ) {
                Icon(Icons.Default.Edit, "Editar perfil", tint = colors.text)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(contentAlignment = Alignment.BottomEnd) {
            user?.photoUrl?.let { photoUrl ->
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "Foto de perfil",
                    modifier = Modifier.size(100.dp).clip(CircleShape)
                )
            } ?: Icon(
                Icons.Default.AccountCircle,
                contentDescription = null,
                tint = colors.text,
                modifier = Modifier.size(100.dp)
            )
            Surface(
                color = colors.accent,
                shape = CircleShape,
                modifier = Modifier.size(32.dp).offset(x = (-4).dp, y = (-4).dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar perfil",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            user?.name ?: strings.defaultUser,
            color = colors.text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            if (user?.age != 0) "Atleta - ${user?.age} años" else "Atleta Amateur",
            color = colors.subText,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.BluetoothConnected,
                null,
                tint = Color.Cyan,
                modifier = Modifier.size(16.dp)
            )
            Text(" Conectado", color = Color.Cyan, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                Icons.Default.BatteryFull,
                null,
                tint = colors.accent,
                modifier = Modifier.size(16.dp)
            )
            Text(" 85%", color = colors.text, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onUpdateBodyDataClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDataMissing) Color.Red else colors.cardBackground,
                contentColor = if (isDataMissing) Color.White else colors.text
            ),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isDataMissing) Color.Transparent else colors.subText.copy(alpha = 0.2f)
            )
        ) {
            Text("Configurar parámetros corporales", fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = {
                authViewModel.resetProfileUpdateState()
                showEditProfileDialog = true
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Edit, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Editar perfil")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "Métricas Físicas",
            color = colors.text,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            ProfileMetricCard(
                strings.weight,
                user?.weight?.toInt()?.toString() ?: "0",
                "kg",
                Icons.Default.MonitorWeight,
                Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            ProfileMetricCard(
                strings.height,
                user?.height?.toInt()?.toString() ?: "0",
                "cm",
                Icons.Default.Height,
                Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            ProfileMetricCard(
                strings.pulse,
                viewModel.heartRate.toInt().toString(),
                "BPM",
                Icons.Default.Favorite,
                Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            ProfileMetricCard(
                strings.sport,
                user?.sport ?: "-",
                "",
                Icons.AutoMirrored.Filled.DirectionsRun,
                Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        ProfileSectionTitle("Preferencias")
        ProfileOptionItem("Notificaciones", Icons.Default.Notifications)
        ProfileOptionItem("Unidades (Métrico)", Icons.Default.Straighten)
        ProfileOptionItem("Idioma", Icons.Default.Language)

        Spacer(modifier = Modifier.height(32.dp))

        ProfileSectionTitle("Cuenta")
        Button(
            onClick = { authViewModel.logout() },
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.cardBackground,
                contentColor = colors.text
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(strings.logout)
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = { showDeleteDialog = true }) {
            Text(strings.deleteAccount, color = Color.Red.copy(alpha = 0.7f), fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun EditProfileDialog(
    user: AppUser,
    authViewModel: AuthViewModel,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current
    val profileState by authViewModel.profileUpdateState.collectAsState()

    var name by remember(user.uid) { mutableStateOf(user.name) }
    var age by remember(user.uid) {
        mutableStateOf(if (user.age > 0) user.age.toString() else "")
    }
    var weight by remember(user.uid) {
        mutableStateOf(if (user.weight > 0) user.weight.toString() else "")
    }
    var height by remember(user.uid) {
        mutableStateOf(if (user.height > 0) user.height.toString() else "")
    }
    var gender by remember(user.uid) { mutableStateOf(user.gender) }
    var sport by remember(user.uid) { mutableStateOf(user.sport) }
    val isSaving = profileState is AuthState.Loading

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar perfil", color = colors.text) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProfileEditField(name, "Nombre", onValueChange = { name = it })
                ProfileEditField(
                    age,
                    "Edad",
                    KeyboardType.Number
                ) { age = it.filter(Char::isDigit) }
                ProfileEditField(
                    weight,
                    "Peso (kg)",
                    KeyboardType.Decimal
                ) { weight = it }
                ProfileEditField(
                    height,
                    "Altura (cm)",
                    KeyboardType.Decimal
                ) { height = it }
                ProfileEditField(gender, "Género", onValueChange = { gender = it })
                ProfileEditField(sport, "Deporte o actividad", onValueChange = { sport = it })

                when (profileState) {
                    is AuthState.Error -> Text(
                        (profileState as AuthState.Error).message,
                        color = Color.Red
                    )
                    AuthState.Success -> Text(
                        "Perfil guardado correctamente.",
                        color = Color(0xFF2E7D32)
                    )
                    else -> Unit
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving,
                onClick = {
                    val parsedAge = age.toIntOrNull()
                    val parsedWeight = weight.replace(',', '.').toDoubleOrNull()
                    val parsedHeight = height.replace(',', '.').toDoubleOrNull()

                    if (parsedAge == null || parsedWeight == null || parsedHeight == null) {
                        authViewModel.setProfileValidationError(
                            "Completa edad, peso y altura con valores válidos."
                        )
                    } else {
                        authViewModel.updateProfile(
                            name = name,
                            age = parsedAge,
                            weight = parsedWeight,
                            height = parsedHeight,
                            gender = gender,
                            sport = sport
                        )
                    }
                }
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Guardar")
                }
            }
        },
        dismissButton = {
            TextButton(enabled = !isSaving, onClick = onDismiss) {
                Text("Cerrar")
            }
        },
        containerColor = colors.cardBackground
    )
}

@Composable
private fun ProfileEditField(
    value: String,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    val colors = LocalAppColors.current

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = colors.text,
            unfocusedTextColor = colors.text,
            focusedBorderColor = colors.accent,
            unfocusedBorderColor = colors.subText,
            focusedLabelColor = colors.accent,
            unfocusedLabelColor = colors.subText,
            cursorColor = colors.accent
        )
    )
}

@Composable
fun ProfileSectionTitle(title: String) {
    val colors = LocalAppColors.current
    Text(
        text = title,
        color = colors.subText,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    )
}

@Composable
fun ProfileOptionItem(label: String, icon: ImageVector) {
    val colors = LocalAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = colors.accent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text(label, color = colors.text, fontSize = 16.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = colors.subText)
        }
    }
}

@Composable
fun ProfileMetricCard(
    label: String,
    value: String,
    unit: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    Card(
        modifier = modifier.height(90.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = colors.accent.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = colors.accent, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(label, color = colors.subText, fontSize = 10.sp)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(value, color = colors.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(
                        " $unit",
                        color = colors.subText,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}
