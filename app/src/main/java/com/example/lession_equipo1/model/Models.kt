package com.example.lession_equipo1.model

import androidx.compose.ui.graphics.Color
import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class MuscleData(
    val id: String,
    val name: String,
    val fatigue: Float
)

data class AppUser(
    val uid: String,
    val name: String,
    val email: String,
    val photoUrl: String? = null,
    var age: Int = 0,
    var weight: Double = 0.0,
    var height: Double = 0.0,
    var gender: String = "",
    var sport: String = ""
)

data class UserProfile(
    @get:PropertyName("display_name") @set:PropertyName("display_name") var displayName: String = "",
    var email: String = "",
    @get:PropertyName("photo_url") @set:PropertyName("photo_url") var photoUrl: String = "",
    var age: Int = 0,
    var weight: Double = 0.0,
    var height: Double = 0.0,
    var gender: String = "",
    var sport: String = "",
    @get:PropertyName("created_at") @set:PropertyName("created_at") var createdAt: Timestamp? = null,
    @get:PropertyName("last_login") @set:PropertyName("last_login") var lastLogin: Timestamp? = null
)

data class HistoryEntry(
    var id: String = "",
    @get:PropertyName("user_id") var userId: String = "",
    @get:PropertyName("heart_rate") var heartRate: Int = 0,
    var temperature: Double = 36.5,
    @get:PropertyName("bp_systolic") var bpSystolic: Int = 120,
    @get:PropertyName("bp_diastolic") var bpDiastolic: Int = 80,
    var fatigue: Double = 0.0,
    @get:PropertyName("max_muscle") var maxMuscle: Double = 0.0,
    var stability: Double = 90.0,
    @get:PropertyName("risk_level") var riskLevel: Double = 0.0,
    @get:PropertyName("risk_state") var riskState: String = "Óptimo",
    @get:PropertyName("activity_type") var activityType: String = "",
    var timestamp: Timestamp = Timestamp.now(),
    @get:PropertyName("device_id") var deviceId: String = ""
)

data class LessionNotification(
    val id: String,
    val title: String,
    val message: String,
    val time: Long = System.currentTimeMillis(),
    val type: NotificationType,
    val isRead: Boolean = false,
    val priority: NotificationPriority = NotificationPriority.NORMAL
)

enum class NotificationType {
    BIOMETRIC, WORKOUT, SYSTEM, RISK
}

enum class NotificationPriority {
    LOW, NORMAL, HIGH, CRITICAL
}

enum class GoalStatus {
    ACTIVE, COMPLETED, OVERDUE, CANCELLED
}

enum class IntensityLevel {
    LOW, LOW_MEDIUM, MEDIUM, MEDIUM_HIGH, HIGH, NONE;

    fun getLabel(): String = when(this) {
        LOW -> "Bajo"
        LOW_MEDIUM -> "Bajo-medio"
        MEDIUM -> "Medio"
        MEDIUM_HIGH -> "Medio-alto"
        HIGH -> "Alto"
        NONE -> "Sin clasificar"
    }

    fun getColor(): Color = when(this) {
        LOW -> Color.Green
        LOW_MEDIUM -> Color(0xFF99CC00)
        MEDIUM -> Color.Yellow
        MEDIUM_HIGH -> Color(0xFFFFA500)
        HIGH -> Color.Red
        NONE -> Color.Gray
    }

    fun getDescription(): String = when(this) {
        LOW -> "Actividad suave y de recuperación"
        LOW_MEDIUM -> "Esfuerzo ligero y sostenido"
        MEDIUM -> "Esfuerzo moderado"
        MEDIUM_HIGH -> "Esfuerzo exigente"
        HIGH -> "Esfuerzo máximo o muy exigente"
        NONE -> ""
    }
}

data class WorkoutHistory(
    var id: String = "",
    val userId: String = "",
    val startTime: Long = 0,
    val endTime: Long = 0,
    val duration: Long = 0, // in milliseconds
    val activityType: String = "",
    val intensity: IntensityLevel = IntensityLevel.NONE,
    val calories: Int = 0,
    val distance: Double = 0.0,
    val hrMin: Int = 0,
    val hrMax: Int = 0,
    val hrAvg: Int = 0,
    val riskStart: Float = 0f,
    val riskAvg: Float = 0f,
    val riskEnd: Float = 0f,
    val fatigueStart: Float = 0f,
    val fatigueEnd: Float = 0f,
    val temperatureAvg: Double = 0.0,
    val bloodPressureMax: String = "",
    val summary: String = "",
    val deviceName: String = "",
    val status: String = "Completado",
    val notes: String = "",
    val isFavorite: Boolean = false
)

data class TrainingGoal(
    var id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val category: GoalCategory = GoalCategory.DURATION,
    val targetValue: Double = 0.0,
    var currentValue: Double = 0.0,
    val unit: String = "",
    val startDate: Long = System.currentTimeMillis(),
    val deadline: Long = System.currentTimeMillis(),
    val status: GoalStatus = GoalStatus.ACTIVE
)

enum class GoalCategory {
    WORKOUT_COUNT, DURATION, CALORIES, DISTANCE, HEART_RATE, FATIGUE_REDUCTION, RISK_CONTROL
}
