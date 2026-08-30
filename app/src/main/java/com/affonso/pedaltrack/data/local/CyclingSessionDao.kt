package com.affonso.pedaltrack.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CyclingSessionDao {
    @Insert
    suspend fun insert(session: CyclingSessionEntity): Long

    @Update
    suspend fun update(session: CyclingSessionEntity)

    @Query("DELETE FROM cycling_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM cycling_sessions ORDER BY startTime DESC")
    fun observeAll(): Flow<List<CyclingSessionEntity>>

    @Query("SELECT * FROM cycling_sessions ORDER BY startTime DESC")
    suspend fun getAll(): List<CyclingSessionEntity>

    @Query("SELECT healthConnectSessionId FROM cycling_sessions")
    suspend fun getAllHealthConnectIds(): List<String>

    @Query("SELECT * FROM cycling_sessions WHERE id = :id")
    suspend fun getById(id: Long): CyclingSessionEntity?
}
