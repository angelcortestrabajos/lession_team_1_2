package com.example.lession_equipo1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lession_equipo1.model.WorkoutHistory
import com.example.lession_equipo1.repository.FirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WorkoutHistoryViewModel(private val repository: FirestoreRepository) : ViewModel() {
    private val _history = MutableStateFlow<List<WorkoutHistory>>(emptyList())
    val history: StateFlow<List<WorkoutHistory>> = _history.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadHistory(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getWorkoutHistory(userId).collect { list ->
                _history.value = list
                _isLoading.value = false
            }
        }
    }

    fun saveWorkout(workout: WorkoutHistory) {
        viewModelScope.launch {
            repository.saveWorkoutHistory(workout)
            loadHistory(workout.userId)
        }
    }

    fun deleteWorkout(userId: String, workoutId: String) {
        viewModelScope.launch {
            repository.deleteWorkout(userId, workoutId)
            loadHistory(userId)
        }
    }
    
    fun toggleFavorite(workout: WorkoutHistory) {
        saveWorkout(workout.copy(isFavorite = !workout.isFavorite))
    }
    
    fun updateNotes(workout: WorkoutHistory, newNotes: String) {
        saveWorkout(workout.copy(notes = newNotes))
    }
}
