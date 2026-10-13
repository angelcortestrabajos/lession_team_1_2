package com.example.lession_equipo1.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lession_equipo1.repository.UserPreferencesRepository
import kotlinx.coroutines.launch

data class AppStrings(
    val home: String = "Inicio",
    val stats: String = "Estadísticas",
    val workout: String = "Entrenamiento",
    val alerts: String = "Alertas",
    val profile: String = "Perfil",
    val welcome: String = "¡Hola",
    val defaultUser: String = "usuario",
    val welcomeGeneral: String = "¡Hola usuario!",
    val bioSignals: String = "Señales biométricas",
    val heartRate: String = "Ritmo cardíaco",
    val bodyTemp: String = "Temperatura corporal",
    val bloodPressure: String = "Presión arterial",
    val muscleFatigue: String = "Fatiga muscular",
    val currentFreq: String = "Frecuencia actual",
    val currentTemp: String = "Temperatura actual",
    val currentPress: String = "Presión actual",
    val currentCarga: String = "Carga muscular acumulada",
    val injuryRisk: String = "Riesgo de lesión",
    val optimal: String = "Óptimo",
    val caution: String = "Precaución",
    val risk: String = "Riesgo",
    val myProfile: String = "Mi perfil",
    val muscleFatigueMap: String = "Mapa de Fatiga Muscular",
    val rest: String = "Descanso",
    val moderate: String = "Moderado",
    val high: String = "Alto",
    val weight: String = "Peso",
    val height: String = "Estatura",
    val pulse: String = "Pulse",
    val sport: String = "Deporte",
    val logout: String = "Cerrar Sesión",
    val deleteAccount: String = "Eliminar Cuenta",
    val deleteConfirmTitle: String = "¿Eliminar cuenta?",
    val deleteConfirmDesc: String = "Esta acción eliminará todos tus datos y tu cuenta permanentemente.",
    val deleteButton: String = "Eliminar datos y cuenta",
    val cancel: String = "Cancelar",
    val settings: String = "Configuración",
    val language: String = "Idioma",
    val changeLanguage: String = "Cambiar a Inglés",
    val back: String = "Volver",
    val login: String = "Ingresar",
    val register: String = "Registrarse",
    val forgotPassword: String = "¿Olvidaste tu contraseña?",
    val userName: String = "Usuario",
    val password: String = "Contraseña",
    val name: String = "Nombre",
    val sendLink: String = "Enviar enlace",
    val resetPassword: String = "Restablecer contraseña",
    val verifyEmail: String = "Por favor, verifica tu correo electrónico para activar tu cuenta.",
    val verifyTitle: String = "Verificación enviada",
    val updateBodyData: String = "Realizar formulario de parámetros",
    val theme: String = "Tema",
    val switchToLight: String = "Cambiar a Tema Claro",
    val switchToDark: String = "Cambiar a Tema Oscuro"
)

val EnStrings = AppStrings(
    home = "Home",
    stats = "Statistics",
    workout = "Workout",
    alerts = "Alerts",
    profile = "Profile",
    welcome = "Hello",
    defaultUser = "user",
    welcomeGeneral = "Hello user!",
    bioSignals = "Biometric signals",
    heartRate = "Heart rate",
    bodyTemp = "Body temperature",
    bloodPressure = "Blood pressure",
    muscleFatigue = "Muscle fatigue",
    currentFreq = "Current frequency",
    currentTemp = "Current temperature",
    currentPress = "Current pressure",
    currentCarga = "Accumulated muscle load",
    injuryRisk = "Injury risk",
    optimal = "Optimal",
    caution = "Caution",
    risk = "Risk",
    myProfile = "My profile",
    muscleFatigueMap = "Muscle Fatigue Map",
    rest = "Rest",
    moderate = "Moderate",
    high = "High",
    weight = "Weight",
    height = "Height",
    pulse = "Pulse",
    sport = "Sport",
    logout = "Log Out",
    deleteAccount = "Delete Account",
    deleteConfirmTitle = "Delete account?",
    deleteConfirmDesc = "This action will permanently delete all your data and account.",
    deleteButton = "Delete data and account",
    cancel = "Cancel",
    settings = "Settings",
    language = "Language",
    changeLanguage = "Switch to Spanish",
    back = "Back",
    login = "Login",
    register = "Register",
    forgotPassword = "Forgot password?",
    userName = "User name",
    password = "Password",
    name = "Name",
    sendLink = "Send link",
    resetPassword = "Reset password",
    verifyEmail = "Please check your email to activate your account.",
    verifyTitle = "Verification sent",
    updateBodyData = "Update body parameters",
    theme = "Theme",
    switchToLight = "Switch to Light Theme",
    switchToDark = "Switch to Dark Theme"
)

val LocalStrings = compositionLocalOf { AppStrings() }

@Composable
@ReadOnlyComposable
fun stringResource(): AppStrings {
    return LocalStrings.current
}

class AppConfigViewModel(private val repository: UserPreferencesRepository) : ViewModel() {
    var isEnglish by mutableStateOf(false)
        private set

    var isDarkMode by mutableStateOf(true)
        private set

    init {
        viewModelScope.launch {
            repository.isEnglishFlow.collect { savedIsEnglish ->
                isEnglish = savedIsEnglish
            }
        }
        viewModelScope.launch {
            repository.isDarkModeFlow.collect { savedIsDarkMode ->
                isDarkMode = savedIsDarkMode
            }
        }
    }

    fun toggleLanguage() {
        val newValue = !isEnglish
        isEnglish = newValue
        viewModelScope.launch {
            repository.setEnglish(newValue)
        }
    }

    fun toggleTheme() {
        val newValue = !isDarkMode
        isDarkMode = newValue
        viewModelScope.launch {
            repository.setDarkMode(newValue)
        }
    }

    val strings: AppStrings
        get() = if (isEnglish) EnStrings else AppStrings()

    val colors: AppColors
        get() = if (isDarkMode) DarkAppColors else LightAppColors
}
