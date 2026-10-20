package com.example.lession_equipo1.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.lession_equipo1.BiometricViewModel
import com.example.lession_equipo1.network.BluetoothController
import com.example.lession_equipo1.data.local.database.AppDatabase
import com.example.lession_equipo1.repository.AuthRepository
import com.example.lession_equipo1.repository.FirestoreRepository
import com.example.lession_equipo1.repository.RoomHistoryRepository
import com.example.lession_equipo1.repository.UserPreferencesRepository
import com.example.lession_equipo1.ui.AppConfigViewModel

class ViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val authRepository = AuthRepository(context)
    private val firestoreRepository = FirestoreRepository()
    private val bluetoothController = BluetoothController(context)
    private val roomHistoryRepository = RoomHistoryRepository(
        AppDatabase.getDatabase(context.applicationContext).historyDao()
    )
    private val userPreferencesRepository = UserPreferencesRepository(context.applicationContext)
    private val appConfigViewModel = AppConfigViewModel(userPreferencesRepository)
    private val notificationViewModel = NotificationViewModel()
    private val goalViewModel = GoalViewModel(firestoreRepository)
    private val workoutHistoryViewModel = WorkoutHistoryViewModel(firestoreRepository)
    private val statisticsViewModel = StatisticsViewModel(firestoreRepository)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel(authRepository, firestoreRepository) as T
            }
            modelClass.isAssignableFrom(BiometricViewModel::class.java) -> {
                BiometricViewModel(
                    firestoreRepository,
                    bluetoothController,
                    roomHistoryRepository,
                    userPreferencesRepository
                ) as T
            }
            modelClass.isAssignableFrom(HistoryViewModel::class.java) -> {
                HistoryViewModel(firestoreRepository, roomHistoryRepository) as T
            }
            modelClass.isAssignableFrom(AppConfigViewModel::class.java) -> {
                appConfigViewModel as T
            }
            modelClass.isAssignableFrom(NotificationViewModel::class.java) -> {
                notificationViewModel as T
            }
            modelClass.isAssignableFrom(GoalViewModel::class.java) -> {
                goalViewModel as T
            }
            modelClass.isAssignableFrom(WorkoutHistoryViewModel::class.java) -> {
                workoutHistoryViewModel as T
            }
            modelClass.isAssignableFrom(StatisticsViewModel::class.java) -> {
                statisticsViewModel as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
