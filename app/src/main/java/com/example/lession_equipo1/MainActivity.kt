package com.example.lession_equipo1

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.lession_equipo1.network.BluetoothState
import com.example.lession_equipo1.model.*
import com.example.lession_equipo1.ui.*
import com.example.lession_equipo1.ui.theme.Lession_equipo1Theme
import com.example.lession_equipo1.viewmodel.*
import com.example.lession_equipo1.ui.screens.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }

        setContent {
            Lession_equipo1Theme {
                val navController = rememberNavController()
                val factory = ViewModelFactory(LocalContext.current)
                val authViewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)
                val biometricViewModel: BiometricViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)
                val historyViewModel: HistoryViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)
                val appConfigViewModel: AppConfigViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)
                val notificationViewModel: NotificationViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)
                val goalViewModel: GoalViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)
                val workoutHistoryViewModel: WorkoutHistoryViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)
                val statisticsViewModel: StatisticsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)

                val user by authViewModel.currentUser.collectAsState()
                val isDataMissing = user?.let { (it.weight == 0.0) || (it.height == 0.0) || it.sport.isEmpty() } ?: false
                val currentUserId = user?.uid ?: ""

                CompositionLocalProvider(
                    LocalStrings provides appConfigViewModel.strings,
                    LocalAppColors provides appConfigViewModel.colors
                ) {
                    val colors = LocalAppColors.current
                    Scaffold(
                        containerColor = colors.background,
                        topBar = {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentRoute = navBackStackEntry?.destination?.route
                            if (user != null && currentRoute != "login" && currentRoute != "body_data" && currentRoute != "forgot_password") {
                                LessionTopBar(
                                    notificationViewModel = notificationViewModel,
                                    onNotificationsClick = { navController.navigate("notifications") },
                                    onHistoryClick = { navController.navigate("firebase_history") }
                                )
                            }
                        },
                        bottomBar = {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentRoute = navBackStackEntry?.destination?.route
                            if (user != null && currentRoute != "login" && currentRoute != "body_data" && currentRoute != "forgot_password") {
                                BottomNavigationBar(navController)
                            }
                        },
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = if (user == null) "login" else "home",
                            modifier = Modifier.padding(innerPadding),
                        ) {
                            composable("login") {
                                LoginScreen(
                                    viewModel = authViewModel,
                                    onLoginSuccess = {
                                        navController.navigate("home") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    },
                                    onForgotPasswordClick = {
                                        navController.navigate("forgot_password")
                                    },
                                )
                            }
                            composable("body_data") {
                                BodyDataEntryScreen(
                                    viewModel = authViewModel,
                                    onSuccess = {
                                        navController.navigate("home") {
                                            popUpTo("body_data") { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable("home") {
                                val currentUser = user ?: return@composable
                                LaunchedEffect(currentUser.uid) {
                                    biometricViewModel.setCurrentUser(currentUser.uid)
                                }

                                // Bridge biometric updates to goals
                                val currentRisk = biometricViewModel.riskLevel
                                LaunchedEffect(currentRisk) {
                                    goalViewModel.updateProgress(currentUser.uid, GoalCategory.RISK_CONTROL, currentRisk.toDouble())
                                }

                                HomeScreen(
                                    viewModel = biometricViewModel,
                                    authViewModel = authViewModel,
                                    isDataMissing = isDataMissing,
                                    onBluetoothClick = { navController.navigate("bluetooth") },
                                    onCompleteProfileClick = { navController.navigate("body_data") }
                                )
                            }
                            composable("stats") {
                                StatisticsScreen(
                                    biometricViewModel = biometricViewModel,
                                    historyViewModel = workoutHistoryViewModel,
                                    statsViewModel = statisticsViewModel,
                                    userId = currentUserId
                                )
                            }
                            composable("workout") {
                                val currentUser = user ?: return@composable
                                WorkoutScreen(
                                    viewModel = biometricViewModel,
                                    goalViewModel = goalViewModel,
                                    historyViewModel = workoutHistoryViewModel,
                                    userId = currentUser.uid,
                                    onManageGoalsClick = { navController.navigate("goals") },
                                    onHistoryClick = { navController.navigate("workout_history") }
                                )
                            }
                            composable("goals") {
                                val currentUser = user ?: return@composable
                                GoalListScreen(
                                    userId = currentUser.uid,
                                    viewModel = goalViewModel,
                                    onAddGoalClick = { navController.navigate("add_goal") },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("add_goal") {
                                val currentUser = user ?: return@composable
                                AddEditGoalScreen(
                                    userId = currentUser.uid,
                                    viewModel = goalViewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("workout_history") {
                                val currentUser = user ?: return@composable
                                WorkoutHistoryScreen(
                                    userId = currentUser.uid,
                                    viewModel = workoutHistoryViewModel,
                                    onWorkoutClick = { workout ->
                                        navController.navigate("workout_detail/${workout.id}")
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("workout_detail/{workoutId}") { backStackEntry ->
                                val workoutId = backStackEntry.arguments?.getString("workoutId") ?: ""
                                val history by workoutHistoryViewModel.history.collectAsState()
                                val workout = history.find { it.id == workoutId }

                                if (workout != null) {
                                    WorkoutDetailScreen(
                                        workout = workout,
                                        viewModel = workoutHistoryViewModel,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                            }
                            composable("status") {
                                StatusScreen(biometricViewModel)
                            }
                            composable("profile") {
                                ProfileScreen(
                                    viewModel = biometricViewModel,
                                    authViewModel = authViewModel,
                                    isDataMissing = isDataMissing,
                                    onSettingsClick = { navController.navigate("settings") },
                                    onUpdateBodyDataClick = { navController.navigate("body_data") }
                                )
                            }
                            composable("notifications") {
                                NotificationScreen(
                                    viewModel = notificationViewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("settings") {
                                SettingsScreen(
                                    viewModel = appConfigViewModel,
                                    biometricViewModel = biometricViewModel,
                                    userId = currentUserId,
                                    onBluetoothClick = { navController.navigate("bluetooth") },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("history") {
                                val u = user ?: return@composable
                                HistoryScreen(u.uid, historyViewModel, onBack = { navController.popBackStack() })
                            }
                            composable("firebase_history") {
                                val u = user ?: return@composable
                                FirebaseHistoryScreen(
                                    userId = u.uid,
                                    viewModel = historyViewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("bluetooth") {
                                BluetoothScreen(biometricViewModel, onBack = { navController.popBackStack() })
                            }
                            composable("forgot_password") {
                                ForgotPasswordScreen(authViewModel, onBack = { navController.popBackStack() })
                            }
                        }
                    }

                    NotificationHandler(biometricViewModel, authViewModel, notificationViewModel, goalViewModel)
                }
            }
        }
    }

    @Composable
    private fun NotificationHandler(
        biometric: BiometricViewModel,
        auth: AuthViewModel,
        notificationViewModel: NotificationViewModel,
        goalViewModel: GoalViewModel
    ) {
        LaunchedEffect(biometric.notifications) {
            biometric.notifications.collect { message ->
                val type = if (message.contains("¡CRÍTICO!") || message.contains("¡RIESGO ALTO!"))
                    NotificationType.RISK else NotificationType.BIOMETRIC
                val priority = if (type == NotificationType.RISK)
                    NotificationPriority.CRITICAL else NotificationPriority.HIGH

                notificationViewModel.addNotification(
                    title = if (type == NotificationType.RISK) "Alerta de Riesgo" else "Alerta Biométrica",
                    message = message,
                    type = type,
                    priority = priority
                )
                showNotification(if (type == NotificationType.RISK) "Lession Riesgo" else "Alerta Biométrica", message)
            }
        }
        LaunchedEffect(auth.notifications) {
            auth.notifications.collect { message ->
                notificationViewModel.addNotification(
                    title = "Seguridad",
                    message = message,
                    type = NotificationType.SYSTEM,
                    priority = NotificationPriority.NORMAL
                )
                showNotification("Lession Seguridad", message)
            }
        }
        LaunchedEffect(goalViewModel.goalNotifications) {
            goalViewModel.goalNotifications.collect { (title, message) ->
                notificationViewModel.addNotification(
                    title = title,
                    message = message,
                    type = NotificationType.WORKOUT,
                    priority = NotificationPriority.HIGH
                )
                showNotification(title, message)
            }
        }
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "lession_alerts"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Notificaciones", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessionTopBar(
    notificationViewModel: NotificationViewModel,
    onNotificationsClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val notifications by notificationViewModel.notifications.collectAsState()
    val unreadCount = notifications.count { !it.isRead }

    CenterAlignedTopAppBar(
        title = {
            Text(
                "LESSiON",
                color = colors.text,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                letterSpacing = 1.sp
            )
        },
        actions = {
            IconButton(onClick = onHistoryClick) {
                Icon(Icons.Default.History, "Historial", tint = colors.text)
            }
            IconButton(onClick = onNotificationsClick) {
                BadgedBox(badge = {
                    if (unreadCount > 0) {
                        Badge(containerColor = Color.Red) { Text(unreadCount.toString()) }
                    }
                }) {
                    Icon(Icons.Default.Notifications, "Notificaciones", tint = colors.text)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
    )
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val strings = LocalStrings.current
    val colors = LocalAppColors.current
    val items = listOf(
        Triple("home", strings.home, Icons.Default.Home),
        Triple("stats", strings.stats, Icons.Default.BarChart),
        Triple("workout", strings.workout, Icons.AutoMirrored.Filled.DirectionsRun),
        Triple("status", "Estado", Icons.Default.AccessibilityNew),
        Triple("profile", strings.profile, Icons.Default.Person)
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = colors.surface,
        contentColor = colors.accent,
        tonalElevation = 8.dp
    ) {
        items.forEach { (route, label, icon) ->
            val selected = currentRoute == route
            NavigationBarItem(
                icon = {
                    Icon(
                        icon,
                        contentDescription = label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = { Text(label, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                selected = selected,
                onClick = {
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.accent,
                    selectedTextColor = colors.accent,
                    unselectedIconColor = colors.subText,
                    unselectedTextColor = colors.subText,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
