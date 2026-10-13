package com.example.lession_equipo1.viewmodel

import androidx.lifecycle.ViewModel
import com.example.lession_equipo1.model.LessionNotification
import com.example.lession_equipo1.model.NotificationPriority
import com.example.lession_equipo1.model.NotificationType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class NotificationViewModel : ViewModel() {
    private val _notifications = MutableStateFlow<List<LessionNotification>>(emptyList())
    val notifications: StateFlow<List<LessionNotification>> = _notifications.asStateFlow()

    init {
        // Mock data initially
        _notifications.value = listOf(
            LessionNotification(
                id = UUID.randomUUID().toString(),
                title = "Riesgo de lesión alto",
                message = "Se detectó fatiga muscular crítica en el cuádriceps derecho.",
                type = NotificationType.RISK,
                priority = NotificationPriority.CRITICAL
            ),
            LessionNotification(
                id = UUID.randomUUID().toString(),
                title = "Objetivo cumplido",
                message = "¡Felicidades! Has mantenido tu ritmo cardíaco en Zona 2.",
                type = NotificationType.WORKOUT,
                priority = NotificationPriority.NORMAL
            ),
            LessionNotification(
                id = UUID.randomUUID().toString(),
                title = "Sensor desconectado",
                message = "La banda Lession ha perdido la conexión Bluetooth.",
                type = NotificationType.SYSTEM,
                priority = NotificationPriority.HIGH
            )
        )
    }

    fun addNotification(title: String, message: String, type: NotificationType, priority: NotificationPriority = NotificationPriority.NORMAL) {
        val newNotification = LessionNotification(
            id = UUID.randomUUID().toString(),
            title = title,
            message = message,
            type = type,
            priority = priority
        )
        _notifications.update { listOf(newNotification) + it }
    }

    fun markAsRead(id: String) {
        _notifications.update { list ->
            list.map { if (it.id == id) it.copy(isRead = true) else it }
        }
    }

    fun markAllAsRead() {
        _notifications.update { list ->
            list.map { it.copy(isRead = true) }
        }
    }

    fun deleteNotification(id: String) {
        _notifications.update { list ->
            list.filter { it.id != id }
        }
    }
}
