package com.example.lession_equipo1.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lession_equipo1.data.local.entity.HistoryRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: HistoryRecordEntity): Long

    @Query("SELECT * FROM history_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<HistoryRecordEntity>>

    @Query("SELECT * FROM history_records WHERE userId = :userId ORDER BY timestamp DESC")
    fun getRecordsByUserId(userId: String): Flow<List<HistoryRecordEntity>>

    @Query("SELECT * FROM history_records WHERE id = :id")
    suspend fun getRecordById(id: Long): HistoryRecordEntity?

    @Delete
    suspend fun deleteRecord(record: HistoryRecordEntity)

    @Query("DELETE FROM history_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)

    @Query("DELETE FROM history_records")
    suspend fun deleteAllRecords()
}
