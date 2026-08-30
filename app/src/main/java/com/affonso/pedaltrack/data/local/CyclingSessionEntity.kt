package com.affonso.pedaltrack.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "cycling_sessions",
    indices = [Index(value = ["healthConnectSessionId"], unique = true)]
)
data class CyclingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val healthConnectSessionId: String,
    val startTime: Instant,
    val endTime: Instant,
    val durationMin: Int,
    val calories: Double?,
    val avgHeartRate: Int?,
    val km: Double,
    val carga: String?,
    val createdAt: Instant
)
