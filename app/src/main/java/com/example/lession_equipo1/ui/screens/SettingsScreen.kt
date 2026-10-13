package com.example.lession_equipo1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lession_equipo1.ui.AppConfigViewModel
import com.example.lession_equipo1.ui.LocalAppColors
import com.example.lession_equipo1.ui.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AppConfigViewModel,
    onBack: () -> Unit
) {
    val strings = stringResource()
    val colors = LocalAppColors.current

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
        ) {
            Text(
                text = strings.language,
                color = colors.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
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

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = strings.theme,
                color = colors.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
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
        }
    }
}
