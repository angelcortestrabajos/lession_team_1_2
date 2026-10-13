package com.example.lession_equipo1.repository

import com.example.lession_equipo1.data.local.dao.HistoryDao
import com.example.lession_equipo1.data.local.entity.HistoryRecordEntity
import kotlinx.coroutines.flow.Flow

class RoomHistoryRepository(private val historyDao: HistoryDao) {

    val allRecords: Flow<List<HistoryRecordEntity>> = historyDao.getAllRecords()

    fun getRecordsForUser(userId: String): Flow<List<HistoryRecordEntity>> {
        return if (userId.isEmpty()) {
            historyDao.getAllRecords()
        } else {
            historyDao.getRecordsByUserId(userId)
        }
    }

    suspend fun insertRecord(record: HistoryRecordEntity): Long {
        return historyDao.insertRecord(record)
    }

    suspend fun getRecordById(id: Long): HistoryRecordEntity? {
        return historyDao.getRecordById(id)
    }

    suspend fun deleteRecord(record: HistoryRecordEntity) {
        historyDao.deleteRecord(record)
    }

    suspend fun deleteRecordById(id: Long) {
        historyDao.deleteRecordById(id)
    }
}
