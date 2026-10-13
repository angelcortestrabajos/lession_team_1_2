package com.example.lession_equipo1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lession_equipo1.data.local.entity.HistoryRecordEntity
import com.example.lession_equipo1.model.HistoryEntry
import com.example.lession_equipo1.repository.FirestoreRepository
import com.example.lession_equipo1.repository.RoomHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val firestoreRepository: FirestoreRepository,
    private val roomHistoryRepository: RoomHistoryRepository
) : ViewModel() {

    private val _localRecords = MutableStateFlow<List<HistoryRecordEntity>>(emptyList())
    val localRecords: StateFlow<List<HistoryRecordEntity>> = _localRecords.asStateFlow()

    private val _firebaseRecords = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val firebaseRecords: StateFlow<List<HistoryEntry>> = _firebaseRecords.asStateFlow()

    private val _selectedRecord = MutableStateFlow<HistoryRecordEntity?>(null)
    val selectedRecord: StateFlow<HistoryRecordEntity?> = _selectedRecord.asStateFlow()

    fun loadHistory(uid: String) {
        viewModelScope.launch {
            roomHistoryRepository.getRecordsForUser(uid).collect { list ->
                _localRecords.value = list
            }
        }
    }

    fun addRecord(record: HistoryRecordEntity) {
        viewModelScope.launch {
            roomHistoryRepository.insertRecord(record)
        }
    }

    fun loadFirebaseHistory(uid: String) {
        viewModelScope.launch {
            firestoreRepository.getHistory(uid).collect { records ->
                _firebaseRecords.value = records
            }
        }
    }

    fun deleteFirebaseRecord(uid: String, recordId: String) {
        viewModelScope.launch {
            firestoreRepository.deleteMeasurement(uid, recordId)
        }
    }

    fun deleteRecord(record: HistoryRecordEntity) {
        viewModelScope.launch {
            roomHistoryRepository.deleteRecord(record)
        }
    }

    fun selectRecord(record: HistoryRecordEntity?) {
        _selectedRecord.value = record
    }
}
