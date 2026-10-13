package com.example.lession_equipo1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lession_equipo1.model.LessionNotification
import com.example.lession_equipo1.model.NotificationType
import com.example.lession_equipo1.ui.LocalAppColors
import com.example.lession_equipo1.viewmodel.NotificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(viewModel: NotificationViewModel, onBack: () -> Unit) {
    val colors = LocalAppColors.current
    val notifications by viewModel.notifications.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    
    val filteredNotifications = remember(notifications, selectedTab) {
        when (selectedTab) {
            1 -> notifications.filter { it.type == NotificationType.BIOMETRIC || it.type == NotificationType.RISK }
            2 -> notifications.filter { it.type == NotificationType.WORKOUT }
            3 -> notifications.filter { it.type == NotificationType.SYSTEM }
            else -> notifications
        }
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Notificaciones", color = colors.text) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colors.text)
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.markAllAsRead() }) {
                        Text("Marcar todo leído", color = colors.accent, fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = colors.background,
                contentColor = colors.accent,
                edgePadding = 16.dp,
                divider = {},
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = colors.accent
                        )
                    }
                }
            ) {
                listOf("Todas", "Biométricas", "Entrenamiento", "Sistema").forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 14.sp) },
                        selectedContentColor = colors.accent,
                        unselectedContentColor = colors.subText
                    )
                }
            }
            
            if (filteredNotifications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay notificaciones", color = colors.subText)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredNotifications, key = { it.id }) { notification ->
                        NotificationItem(
                            notification = notification,
                            onReadClick = { viewModel.markAsRead(notification.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(notification: LessionNotification, onReadClick: () -> Unit) {
    val colors = LocalAppColors.current
    val (icon, color) = when (notification.type) {
        NotificationType.BIOMETRIC -> Icons.Default.Warning to Color.Red
        NotificationType.RISK -> Icons.Default.ReportProblem to Color.Red
        NotificationType.WORKOUT -> Icons.Default.FitnessCenter to Color.Green
        NotificationType.SYSTEM -> Icons.Default.Info to colors.accent
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) colors.cardBackground.copy(alpha = 0.5f) else colors.cardBackground
        ),
        shape = RoundedCornerShape(12.dp),
        onClick = onReadClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (notification.isRead) colors.subText.copy(alpha = 0.1f) else color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = if (notification.isRead) colors.subText else color, modifier = Modifier.size(20.dp))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title, 
                        color = if (notification.isRead) colors.subText else colors.text, 
                        fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold, 
                        fontSize = 14.sp
                    )
                    if (!notification.isRead) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(colors.accent))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.message, 
                    color = colors.subText, 
                    fontSize = 12.sp, 
                    lineHeight = 16.sp,
                    maxLines = 2
                )
            }
        }
    }
}
