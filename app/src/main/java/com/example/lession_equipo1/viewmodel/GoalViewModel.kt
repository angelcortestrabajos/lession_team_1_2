package com.example.lession_equipo1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lession_equipo1.model.GoalCategory
import com.example.lession_equipo1.model.GoalStatus
import com.example.lession_equipo1.model.TrainingGoal
import com.example.lession_equipo1.repository.FirestoreRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GoalViewModel(private val repository: FirestoreRepository) : ViewModel() {
    private val _goals = MutableStateFlow<List<TrainingGoal>>(emptyList())
    val goals: StateFlow<List<TrainingGoal>> = _goals.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _goalNotifications = MutableSharedFlow<Pair<String, String>>()
    val goalNotifications = _goalNotifications.asSharedFlow()

    fun loadGoals(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getGoals(userId).collect { goalList ->
                _goals.value = goalList
                _isLoading.value = false
            }
        }
    }

    fun saveGoal(goal: TrainingGoal) {
        viewModelScope.launch {
            repository.saveGoal(goal)
            loadGoals(goal.userId)
        }
    }

    fun deleteGoal(userId: String, goalId: String) {
        viewModelScope.launch {
            repository.deleteGoal(userId, goalId)
            loadGoals(userId)
        }
    }

    fun markAsCompleted(goal: TrainingGoal) {
        val updatedGoal = goal.copy(status = GoalStatus.COMPLETED, currentValue = goal.targetValue)
        viewModelScope.launch {
            repository.saveGoal(updatedGoal)
            _goalNotifications.emit("¡Objetivo Cumplido!" to "Has completado: ${goal.title}")
            loadGoals(goal.userId)
        }
    }

    fun updateProgress(userId: String, category: GoalCategory, valueToAdd: Double) {
        val affectedGoals = _goals.value.filter { it.category == category && it.status == GoalStatus.ACTIVE }
        affectedGoals.forEach { goal ->
            val newValue = goal.currentValue + valueToAdd
            val isNewlyCompleted = newValue >= goal.targetValue
            val newStatus = if (isNewlyCompleted) GoalStatus.COMPLETED else GoalStatus.ACTIVE
            
            viewModelScope.launch {
                repository.saveGoal(goal.copy(currentValue = newValue, status = newStatus))
                if (isNewlyCompleted) {
                    _goalNotifications.emit("¡Objetivo Cumplido!" to "Has completado: ${goal.title}")
                }
                loadGoals(userId)
            }
        }
    }
}
