package com.example.lession_equipo1.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_records")
data class HistoryRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "",
    val heartRate: Int = 0,
    val temperature: Double = 36.5,
    val bpSystolic: Int = 120,
    val bpDiastolic: Int = 80,
    val fatigue: Double = 0.0,
    val maxMuscle: Double = 0.0,
    val stability: Double = 90.0,
    val riskLevel: Double = 0.0,
    val riskState: String = "Óptimo",
    val activityType: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)
