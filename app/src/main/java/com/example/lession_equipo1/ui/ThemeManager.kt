package com.example.lession_equipo1.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppColors(
    val background: Color,
    val surface: Color,
    val text: Color,
    val subText: Color,
    val accent: Color,
    val cardBackground: Color,
    val isDark: Boolean
)

val DarkAppColors = AppColors(
    background = Color(0xFF0D0B26), // Deep blue/violet
    surface = Color(0xFF1B183D),    // Deep violet surface
    text = Color.White,
    subText = Color(0xFFB4A7EB),   // Light violet subtext
    accent = Color(0xFFE1F541),    // Yellow/lime accent
    cardBackground = Color(0xFF1B183D), // Slightly lighter violet for cards
    isDark = true
)

val LightAppColors = AppColors(
    background = Color(0xFFF5F3FF),
    surface = Color.White,
    text = Color(0xFF1A1251),
    subText = Color(0xFF4A4477),
    accent = Color(0xFFE1F541),
    cardBackground = Color(0xFFEDE7F6),
    isDark = false
)

val LocalAppColors = compositionLocalOf { DarkAppColors }
